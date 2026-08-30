package com.docscan.scanner

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.sqrt

enum class TrackerState {
    SEARCHING,
    DETECTED,
    STABLE,
    LOST
}

data class TrackedQuadState(
    val corners: List<Offset>,
    val state: TrackerState,
    val isStable: Boolean,
    val stableFramesCount: Int,
    val confidence: Float,
    val frameAspectRatio: Float = 3f / 4f
)

/**
 * One Euro Filter — adaptive low-pass filter for real-time signal smoothing.
 *
 * This is the same class of filter used across modern AR / camera-overlay
 * pipelines (MediaPipe, ARCore-style trackers) to get a "glassy smooth but
 * still responsive" feel — heavy smoothing while the tracked point is nearly
 * still (kills micro-jitter from sensor/lighting noise), and low-lag tracking
 * the instant real motion (panning the phone) is detected, because the cutoff
 * frequency adapts to the estimated speed of the signal instead of using one
 * fixed smoothing factor for every situation.
 *
 * Reference: Casiez, Roussel, Vogel — "1€ Filter: A Simple Speed-based
 * Low-pass Filter for Noisy Input in Interactive Systems" (CHI 2012).
 */
private class OneEuroFilter(
    private val minCutoff: Float,
    private val beta: Float,
    private val dCutoff: Float = 1.0f
) {
    private var initialized = false
    private var xPrev = 0f
    private var dxPrev = 0f
    private var tPrevMillis = 0L

    private fun alpha(cutoff: Float, dtSeconds: Float): Float {
        val tau = 1f / (2f * Math.PI.toFloat() * cutoff)
        return 1f / (1f + tau / dtSeconds)
    }

    fun filter(x: Float, tMillis: Long): Float {
        if (!initialized) {
            initialized = true
            xPrev = x
            dxPrev = 0f
            tPrevMillis = tMillis
            return x
        }

        val dt = ((tMillis - tPrevMillis).coerceAtLeast(1L)) / 1000f
        tPrevMillis = tMillis

        // Estimate speed of the signal, itself lightly smoothed.
        val dx = (x - xPrev) / dt
        val aD = alpha(dCutoff, dt)
        val dxHat = dxPrev + aD * (dx - dxPrev)

        // Faster estimated speed -> higher cutoff -> less smoothing -> less lag.
        val cutoff = minCutoff + beta * abs(dxHat)
        val a = alpha(cutoff, dt)
        val xHat = xPrev + a * (x - xPrev)

        xPrev = xHat
        dxPrev = dxHat
        return xHat
    }

    fun reset() {
        initialized = false
        xPrev = 0f
        dxPrev = 0f
        tPrevMillis = 0L
    }
}

/**
 * Intelligent Frame-to-Frame Temporal Quadrilateral Tracker.
 *
 * Responsibilities:
 * 1. Adaptive Smoothing: each corner's x/y is tracked by its own One Euro
 *    Filter, so slow jitter is smoothed away while genuine fast motion
 *    (panning to frame the document) is followed with minimal lag.
 * 2. Outlier Rejection: sudden frame anomalies or lighting flashes while
 *    locked-stable are ignored rather than allowed to corrupt the track.
 * 3. Candidate Continuity: prevents flipping between two similar quads
 *    (e.g. inner vs outer boundary).
 * 4. Dual Hysteresis: strict threshold to lock a new quad (lockThreshold)
 *    vs a generous threshold to retain one (keepThreshold).
 * 5. Lost Grace Period: keeps the last known quad visible for a few frames
 *    during momentary occlusions or motion blur instead of snapping away.
 */
class QuadTracker(
    private val historySize: Int = 8,
    private val stabilityVarianceThreshold: Float = 0.022f,
    private val deadbandThreshold: Float = 0.010f,
    private val maxDisplacementPerFrame: Float = 0.080f,
    private val minStableFrames: Int = 2,
    private val lostGraceFrames: Int = 8,
    private val lockThreshold: Float = 0.42f,
    private val keepThreshold: Float = 0.28f,
    private val filterMinCutoff: Float = 0.9f,
    private val filterBeta: Float = 0.45f
) {
    private val history = mutableListOf<List<Offset>>()
    private var currentCorners: List<Offset> = defaultCorners()
    private var lockedStableCorners: List<Offset>? = null
    private var stableFrameCount = 0
    private var lostFrameCount = 0
    private var currentState = TrackerState.SEARCHING
    private var lastConfidence = 0f

    // One filter per axis per corner (TL, TR, BR, BL) x/y = 8 filters total.
    private val filtersX = Array(4) { OneEuroFilter(filterMinCutoff, filterBeta) }
    private val filtersY = Array(4) { OneEuroFilter(filterMinCutoff, filterBeta) }

    /**
     * Process a candidate quad from the current camera frame.
     */
    fun processFrame(
        candidateCorners: List<Offset>,
        isDocumentDetected: Boolean,
        candidateConfidence: Float,
        frameAspectRatio: Float = 3f / 4f
    ): TrackedQuadState {
        val wasDetected = isDocumentDetected && (
            (currentState == TrackerState.SEARCHING && candidateConfidence >= lockThreshold) ||
            (currentState != TrackerState.SEARCHING && candidateConfidence >= keepThreshold)
        )

        val targetCorners = if (wasDetected) candidateCorners else currentCorners
        val now = System.currentTimeMillis()

        val smoothed = mutableListOf<Offset>()
        var totalDistMoved = 0f

        for (i in 0 until 4) {
            val oldPt = currentCorners[i]
            val newPt = targetCorners[i]
            val dist = sqrt((newPt.x - oldPt.x) * (newPt.x - oldPt.x) + (newPt.y - oldPt.y) * (newPt.y - oldPt.y))
            totalDistMoved += dist

            // Deadband: genuinely static input shouldn't drift even a fraction of a
            // pixel due to filter warm-up rounding — snap exactly to the held point.
            if (dist < deadbandThreshold && wasDetected) {
                filtersX[i].filter(oldPt.x, now)
                filtersY[i].filter(oldPt.y, now)
                smoothed.add(oldPt)
                continue
            }

            // Hard outlier rejection: once locked-stable, an enormous one-frame jump
            // is almost always a false-positive flicker (reflection, hand passing by,
            // motion blur spike) rather than real motion — hold position instead of
            // letting a single bad frame corrupt the adaptive filter's speed estimate.
            val isOutlier = wasDetected && dist > 0.30f && stableFrameCount >= minStableFrames
            val inputX = if (isOutlier) oldPt.x else newPt.x
            val inputY = if (isOutlier) oldPt.y else newPt.y

            val filteredX = filtersX[i].filter(inputX, now)
            val filteredY = filtersY[i].filter(inputY, now)

            // Safety clamp: even a filtered response can't teleport more than the
            // configured max step in a single frame.
            val dx = (filteredX - oldPt.x).coerceIn(-maxDisplacementPerFrame, maxDisplacementPerFrame)
            val dy = (filteredY - oldPt.y).coerceIn(-maxDisplacementPerFrame, maxDisplacementPerFrame)

            val clampedX = (oldPt.x + dx).coerceIn(0.01f, 0.99f)
            val clampedY = (oldPt.y + dy).coerceIn(0.01f, 0.99f)

            smoothed.add(Offset(clampedX, clampedY))
        }

        currentCorners = smoothed
        lastConfidence = if (wasDetected) candidateConfidence else (lastConfidence * 0.90f)

        history.add(smoothed)
        if (history.size > historySize) {
            history.removeAt(0)
        }

        // Calculate variance across recent history
        var maxVariance = 0f
        if (history.size >= 3) {
            val latest = history.last()
            for (prev in history) {
                for (i in 0 until 4) {
                    val dX = abs(latest[i].x - prev[i].x)
                    val dY = abs(latest[i].y - prev[i].y)
                    val d = sqrt(dX * dX + dY * dY)
                    if (d > maxVariance) maxVariance = d
                }
            }
        }

        val isStable = wasDetected && history.size >= 3 && maxVariance < stabilityVarianceThreshold

        if (isStable) {
            stableFrameCount++
            lostFrameCount = 0
            if (stableFrameCount >= minStableFrames && lockedStableCorners == null) {
                lockedStableCorners = currentCorners
            }
        } else {
            stableFrameCount = (stableFrameCount - 1).coerceAtLeast(0)
            if (totalDistMoved > 0.08f) {
                lockedStableCorners = null // Unlock upon user intentional camera pan
            }
            if (!wasDetected) {
                lostFrameCount++
            } else {
                lostFrameCount = 0
            }
        }

        // State Machine with Lost Grace Period
        currentState = when {
            stableFrameCount >= minStableFrames -> TrackerState.STABLE
            wasDetected -> TrackerState.DETECTED
            lostFrameCount < lostGraceFrames && currentCorners != defaultCorners() -> {
                // Grace Period: keep overlay visible during quick flash or hand tremor
                TrackerState.DETECTED
            }
            lostFrameCount >= lostGraceFrames && lostFrameCount < lostGraceFrames + 3 -> TrackerState.LOST
            else -> {
                lockedStableCorners = null
                TrackerState.SEARCHING
            }
        }

        val outputCorners = if (currentState == TrackerState.STABLE && lockedStableCorners != null) {
            lockedStableCorners!!
        } else {
            currentCorners
        }

        return TrackedQuadState(
            corners = outputCorners,
            state = currentState,
            isStable = isStable,
            stableFramesCount = stableFrameCount,
            confidence = lastConfidence,
            frameAspectRatio = frameAspectRatio
        )
    }

    fun reset() {
        history.clear()
        currentCorners = defaultCorners()
        lockedStableCorners = null
        stableFrameCount = 0
        lostFrameCount = 0
        currentState = TrackerState.SEARCHING
        lastConfidence = 0f
        for (i in 0 until 4) {
            filtersX[i].reset()
            filtersY[i].reset()
        }
    }

    companion object {
        fun defaultCorners(): List<Offset> = listOf(
            Offset(0.08f, 0.08f),
            Offset(0.92f, 0.08f),
            Offset(0.92f, 0.92f),
            Offset(0.08f, 0.92f)
        )
    }
}
