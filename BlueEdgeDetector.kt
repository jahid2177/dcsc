package com.docscan.scanner

import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// --- নতুন সংযোজন: Document Type ও Premium Result Data Class ---
enum class DocumentType {
    A4_DOCUMENT, ID_CARD, BUSINESS_CARD, RECEIPT, UNKNOWN
}

data class PremiumDetectionResult(
    val corners: List<Offset>,
    val isDocumentDetected: Boolean,
    val frameAspectRatio: Float,
    val confidence: Float,
    val documentType: DocumentType
)

/**
 * ML Kit-style real-time document edge detector for CameraX live preview
 * with Advanced Temporal Tracking and Object Recognition.
 */
object BlueEdgeDetector {

    private const val TAG = "BlueEdgeDetector"

    private const val TARGET_LONG_SIDE = 640
    private const val MIN_LONG_SIDE = 360

    private val EPSILON_VALUES = floatArrayOf(0.015f, 0.022f, 0.030f, 0.038f, 0.048f)

    // --- নতুন সংযোজন: Premium Temporal Document Tracker ---
    object PremiumQuadTracker {
        private var lastCorners: List<Offset>? = null
        private var trackCount = 0
        private const val SMOOTHING_FACTOR = 0.65f
        private const val LOCK_THRESHOLD = 0.08f

        fun updateAndSmooth(newCorners: List<Offset>?, confidence: Float): List<Offset>? {
            if (newCorners == null || confidence < 0.35f) {
                trackCount = 0
                lastCorners = null
                return null
            }

            val currentLast = lastCorners
            if (currentLast == null || isDrasticChange(currentLast, newCorners)) {
                lastCorners = newCorners
                trackCount = 1
                return newCorners
            }

            val smoothed = currentLast.zip(newCorners) { old, new ->
                Offset(
                    x = old.x * SMOOTHING_FACTOR + new.x * (1f - SMOOTHING_FACTOR),
                    y = old.y * SMOOTHING_FACTOR + new.y * (1f - SMOOTHING_FACTOR)
                )
            }

            lastCorners = smoothed
            trackCount++
            return smoothed
        }

        private fun isDrasticChange(old: List<Offset>, new: List<Offset>): Boolean {
            for (i in 0..3) {
                val dx = old[i].x - new[i].x
                val dy = old[i].y - new[i].y
                if (sqrt(dx * dx + dy * dy) > LOCK_THRESHOLD) return true
            }
            return false
        }

        fun defaultCorners(): List<Offset> = listOf(
            Offset(0.1f, 0.1f), Offset(0.9f, 0.1f),
            Offset(0.9f, 0.9f), Offset(0.1f, 0.9f)
        )
    }

    fun idCardFrameCorners(): List<Offset> = listOf(
        Offset(0.08f, 0.22f),
        Offset(0.92f, 0.22f),
        Offset(0.92f, 0.72f),
        Offset(0.08f, 0.72f)
    )

    /**
     * CameraX entry point — আপগ্রেডেড ট্র্যাকিং এবং ক্লাসিফিকেশন সহ।
     */
    fun analyzeImageProxy(imageProxy: ImageProxy): PremiumDetectionResult {
        val rotatedDegrees = imageProxy.imageInfo.rotationDegrees

        val uprightAspect = if (rotatedDegrees == 90 || rotatedDegrees == 270) {
            imageProxy.height.toFloat() / imageProxy.width.toFloat()
        } else {
            imageProxy.width.toFloat() / imageProxy.height.toFloat()
        }

        val detection = try {
            if (imageProxy.format == ImageFormat.YUV_420_888 && imageProxy.planes.isNotEmpty()) {
                detectFromYuv(
                    yBuffer = imageProxy.planes[0].buffer,
                    srcW = imageProxy.width,
                    srcH = imageProxy.height,
                    rowStride = imageProxy.planes[0].rowStride,
                    pixelStride = imageProxy.planes[0].pixelStride,
                    rotationDegrees = rotatedDegrees
                )
            } else {
                detectFromBitmap(imageProxy)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "live detection failed", t)
            null
        }

        val smoothedCorners = PremiumQuadTracker.updateAndSmooth(
            detection?.corners,
            detection?.confidence ?: 0f
        )

        if (smoothedCorners != null && detection != null && detection.confidence >= 0.45f &&
            smoothedCorners.size == 4 &&
            smoothedCorners.all { it.x.isFinite() && it.y.isFinite() }
        ) {
            val rawPts = smoothedCorners.map {
                Point((it.x * imageProxy.width).toDouble(), (it.y * imageProxy.height).toDouble())
            }.toTypedArray()

            val docType = classifyDocumentType(rawPts, imageProxy.width, imageProxy.height)

            return PremiumDetectionResult(
                corners = smoothedCorners,
                isDocumentDetected = true,
                frameAspectRatio = uprightAspect,
                confidence = detection.confidence,
                documentType = docType
            )
        }

        return PremiumDetectionResult(
            corners = PremiumQuadTracker.defaultCorners(),
            isDocumentDetected = false,
            frameAspectRatio = uprightAspect,
            confidence = detection?.confidence ?: 0f,
            documentType = DocumentType.UNKNOWN
        )
    }

    /**
     * ডকুমেন্টের ফিজিক্যাল অ্যাসপেক্ট রেশিও বিশ্লেষণ করে অবজেক্ট চেনার চেষ্টা করে।
     */
    private fun classifyDocumentType(orderedPts: Array<Point>, w: Int, h: Int): DocumentType {
        val topLen = dist(orderedPts[0], orderedPts[1])
        val botLen = dist(orderedPts[3], orderedPts[2])
        val leftLen = dist(orderedPts[0], orderedPts[3])
        val rightLen = dist(orderedPts[1], orderedPts[2])

        val avgW = (topLen + botLen) / 2.0
        val avgH = (leftLen + rightLen) / 2.0
        val aspect = max(avgW, avgH) / min(avgW, avgH).coerceAtLeast(1.0)
        val areaRatio = (avgW * avgH) / (w * h).toDouble()

        return when {
            aspect in 1.35..1.65 && areaRatio < 0.25 -> DocumentType.ID_CARD
            aspect in 1.40..1.75 && areaRatio < 0.15 -> DocumentType.BUSINESS_CARD
            aspect in 1.25..1.55 && areaRatio > 0.40 -> DocumentType.A4_DOCUMENT
            aspect > 1.80 && areaRatio > 0.10 -> DocumentType.RECEIPT
            else -> DocumentType.UNKNOWN
        }
    }

    fun analyzeStillBitmap(bitmap: android.graphics.Bitmap): List<Offset> {
        if (bitmap.width < 20 || bitmap.height < 20) {
            return PremiumQuadTracker.defaultCorners()
        }

        val rgba = Mat()
        val gray = Mat()
        val claheMat = Mat()
        val bilateral = Mat()
        val canny = Mat()
        val closed = Mat()

        try {
            org.opencv.android.Utils.bitmapToMat(bitmap, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)

            val clahe = Imgproc.createCLAHE(3.0, Size(8.0, 8.0))
            try {
                clahe.apply(gray, claheMat)
                clahe.collectGarbage()
            } finally {
                clahe.clear()
            }

            Imgproc.bilateralFilter(claheMat, bilateral, 7, 55.0, 55.0)

            var fastCandidate: List<Offset>? = null
            CoreEdgeDetector.detectQuad(bilateral)?.let { pts ->
                val ordered = Corner.orderQuad(pts.toTypedArray())
                val geom = Corner.evaluateGeometry(ordered)
                val qArea = computeQuadAreaNorm(
                    ordered, bitmap.width, bitmap.height
                )
                if (geom.isConvex &&
                    geom.score >= 0.45 &&
                    qArea in 0.10f..0.82f &&
                    isAspectOk(ordered, bitmap.width, bitmap.height)
                ) {
                    fastCandidate = ordered.map {
                        Offset(
                            (it.x / bitmap.width.toDouble()).toFloat().coerceIn(0f, 1f),
                            (it.y / bitmap.height.toDouble()).toFloat().coerceIn(0f, 1f)
                        )
                    }
                }
            }

            val med = computeMedian(bilateral)
            val low = (med * 0.50).coerceIn(28.0, 90.0)
            val high = max(low + 30.0, med * 1.15).coerceIn(75.0, 200.0)
            Imgproc.Canny(bilateral, canny, low, high, 3, true)

            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
            Imgproc.morphologyEx(canny, closed, Imgproc.MORPH_CLOSE, kernel)
            Imgproc.dilate(closed, closed, kernel)
            kernel.release()

            val corners = findBestQuad(
                edgeMap = closed,
                frameW = bitmap.width,
                frameH = bitmap.height
            )

            return corners ?: fastCandidate ?: PremiumQuadTracker.defaultCorners()
        } catch (t: Throwable) {
            Log.w(TAG, "still-image detection failed", t)
            return PremiumQuadTracker.defaultCorners()
        } finally {
            rgba.release(); gray.release(); claheMat.release()
            bilateral.release(); canny.release(); closed.release()
        }
    }

    // --------------------------- internals --------------------------- //

    private data class InternalResult(val corners: List<Offset>, val confidence: Float)

    private fun detectFromYuv(
        yBuffer: ByteBuffer,
        srcW: Int,
        srcH: Int,
        rowStride: Int,
        pixelStride: Int,
        rotationDegrees: Int
    ): InternalResult? {
        var rawMat: Mat? = null
        var rotatedMat: Mat? = null
        var scaledMat: Mat? = null
        var claheMat: Mat? = null
        var bilateral: Mat? = null
        var canny: Mat? = null
        var morph: Mat? = null
        var hierarchy: Mat? = null

        try {
            rawMat = Mat(srcH, srcW, CvType.CV_8UC1)
            rawMat.put(0, 0, copyYPlane(yBuffer, srcW, srcH, rowStride, pixelStride))

            rotatedMat = Mat()
            when (rotationDegrees) {
                90 -> Core.rotate(rawMat, rotatedMat, Core.ROTATE_90_CLOCKWISE)
                180 -> Core.rotate(rawMat, rotatedMat, Core.ROTATE_180)
                270 -> Core.rotate(rawMat, rotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE)
                else -> rawMat.copyTo(rotatedMat)
            }

            val rotatedW = rotatedMat!!.width()
            val rotatedH = rotatedMat!!.height()
            val longSide = max(rotatedW, rotatedH).coerceAtLeast(1)
            val scale = (TARGET_LONG_SIDE.toDouble() / longSide)
                .coerceAtMost(1.0)
                .coerceAtLeast(MIN_LONG_SIDE.toDouble() / longSide)
            val wW = max(1, (rotatedW * scale).toInt())
            val wH = max(1, (rotatedH * scale).toInt())

            scaledMat = Mat()
            Imgproc.resize(
                rotatedMat, scaledMat, Size(wW.toDouble(), wH.toDouble()),
                0.0, 0.0, Imgproc.INTER_AREA
            )

            claheMat = Mat()
            val clahe = Imgproc.createCLAHE(3.0, Size(8.0, 8.0))
            try {
                clahe.apply(scaledMat, claheMat)
                clahe.collectGarbage()
            } finally {
                clahe.clear()
            }

            bilateral = Mat()
            Imgproc.bilateralFilter(claheMat, bilateral, 7, 55.0, 55.0)

            var best: InternalResult? = null
            CoreEdgeDetector.detectQuad(bilateral)?.let { pts ->
                val ordered = Corner.orderQuad(pts.toTypedArray())
                val geom = Corner.evaluateGeometry(ordered)
                if (geom.isConvex && geom.score >= 0.45) {
                    val qArea = computeQuadAreaNorm(ordered, wW, wH)
                    if (qArea in 0.12f..0.75f && isAspectOk(ordered, wW, wH)) {
                        val conf = (
                            0.40f * geom.score.toFloat() +
                            0.25f * 0.70f +
                            0.35f * areaScore(qArea)
                        ).coerceIn(0.30f, 0.92f)
                        best = InternalResult(
                            corners = ordered.map {
                                Offset(
                                    (it.x / wW.toDouble()).toFloat().coerceIn(0f, 1f),
                                    (it.y / wH.toDouble()).toFloat().coerceIn(0f, 1f)
                                )
                            },
                            confidence = conf
                        )
                    }
                }
            }

            val median = computeMedian(bilateral)
            val lowThresh = (median * 0.48).coerceIn(25.0, 85.0)
            val highThresh = max(lowThresh + 28.0, median * 1.18).coerceIn(70.0, 190.0)

            canny = Mat()
            Imgproc.Canny(bilateral, canny, lowThresh, highThresh, 3, true)

            morph = Mat()
            val kClose = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(7.0, 7.0))
            Imgproc.morphologyEx(canny, morph, Imgproc.MORPH_CLOSE, kClose)
            Imgproc.dilate(morph, morph, kClose)
            kClose.release()

            hierarchy = Mat()
            val contours = ArrayList<MatOfPoint>()
            Imgproc.findContours(
                morph, contours, hierarchy,
                Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE
            )

            if (contours.isEmpty()) {
                return best
            }

            val frameArea = (wW * wH).toDouble()
            val sorted = contours.sortedByDescending { Imgproc.contourArea(it) }.take(25)
            val config = EdgeDetectionConfig.Default

            fun consider(ordered: Array<Point>, edgeMap: Mat, boost: Float = 1f) {
                val geometry = Corner.evaluateGeometry(ordered)
                if (!geometry.isConvex || geometry.score < 0.48) return
                if (geometry.perpendicularity < 0.40) return

                val qArea = computeQuadAreaNorm(ordered, wW, wH)
                if (qArea < 0.12f || qArea > 0.78f) return
                if (!isAspectOk(ordered, wW, wH, config)) return

                val marginX = wW * 0.04
                val marginY = wH * 0.04
                var borderHits = 0
                for (p in ordered) {
                    if (p.x < marginX || p.x > wW - marginX ||
                        p.y < marginY || p.y > wH - marginY
                    ) borderHits++
                }
                if (borderHits >= 3) return

                val top = dist(ordered[0], ordered[1])
                val bot = dist(ordered[3], ordered[2])
                val left = dist(ordered[0], ordered[3])
                val right = dist(ordered[1], ordered[2])
                val hRatio = min(top, bot) / max(top, bot).coerceAtLeast(1.0)
                val vRatio = min(left, right) / max(left, right).coerceAtLeast(1.0)
                if (hRatio < 0.70 || vRatio < 0.70) return

                val edgeSupport = Corner.calculateEdgeSupport(
                    ordered, edgeMap, searchRadiusPx = 3
                ).toFloat().coerceIn(0f, 1f)
                if (edgeSupport < 0.20f) return

                val xs = ordered.map { it.x.toInt().coerceIn(0, wW - 1) }
                val ys = ordered.map { it.y.toInt().coerceIn(0, wH - 1) }
                val minX = xs.minOrNull()!!; val maxX = xs.maxOrNull()!!
                val minY = ys.minOrNull()!!; val maxY = ys.maxOrNull()!!
                var brightScore = 0.5f
                if (maxX - minX > 8 && maxY - minY > 8) {
                    val roi = org.opencv.core.Rect(minX, minY, maxX - minX, maxY - minY)
                    val patch = Mat(claheMat, roi)
                    val mean = org.opencv.core.MatOfDouble()
                    val stdv = org.opencv.core.MatOfDouble()
                    org.opencv.core.Core.meanStdDev(patch, mean, stdv)
                    val m = mean.get(0, 0)[0]
                    mean.release(); stdv.release()
                    brightScore = when {
                        m >= 160 -> 1.0f
                        m >= 130 -> 0.80f
                        m >= 100 -> 0.50f
                        else -> 0.20f
                    }
                }

                val borderPen = when (borderHits) {
                    0 -> 1.0f; 1 -> 0.92f; 2 -> 0.70f; else -> 0.30f
                }

                val geoScore = geometry.score.toFloat().coerceIn(0f, 1f)
                val conf = (
                    0.28f * geoScore +
                    0.26f * edgeSupport +
                    0.28f * areaScore(qArea) +
                    0.18f * brightScore
                ).coerceIn(0f, 1f) * boost * borderPen

                if (conf < 0.45f) return

                val normalized = ordered.map {
                    Offset(
                        (it.x / wW.toDouble()).toFloat().coerceIn(0f, 1f),
                        (it.y / wH.toDouble()).toFloat().coerceIn(0f, 1f)
                    )
                }
                if (best == null || conf > best!!.confidence) {
                    best = InternalResult(normalized, conf)
                }
            }

            for (contour in sorted) {
                val cArea = Imgproc.contourArea(contour)
                if (cArea / frameArea < 0.10) continue

                val c2f = MatOfPoint2f(*contour.toArray())
                try {
                    val peri = Imgproc.arcLength(c2f, true)
                    for (eps in EPSILON_VALUES) {
                        val approx = MatOfPoint2f()
                        try {
                            Imgproc.approxPolyDP(c2f, approx, eps * peri, true)
                            if (approx.total() != 4L) continue
                            val rawQuad = Corner.orderQuad(approx.toArray())
                            val refined = try {
                                Corner.refineCornersByEdgeIntersection(
                                    quad = rawQuad,
                                    contour = contour.toArray(),
                                    edgeMap = morph!!,
                                    pixelSnapSearchRadius = config.cornerSearchRadiusPx
                                )
                            } catch (_: Throwable) {
                                rawQuad
                            }
                            consider(Corner.orderQuad(refined), morph!!)
                        } finally {
                            approx.release()
                        }
                    }

                    val minRect = Imgproc.minAreaRect(c2f)
                    val boxPts = Array(4) { Point() }
                    minRect.points(boxPts)
                    consider(Corner.orderQuad(boxPts), morph!!, boost = 1.08f)
                } finally {
                    c2f.release()
                }
            }

            contours.forEach { it.release() }
            return best
        } finally {
            rawMat?.release()
            rotatedMat?.release()
            scaledMat?.release()
            claheMat?.release()
            bilateral?.release()
            canny?.release()
            morph?.release()
            hierarchy?.release()
        }
    }

    private fun detectFromBitmap(imageProxy: ImageProxy): InternalResult? {
        val bitmap = try {
            imageProxy.toBitmap()
        } catch (_: Throwable) {
            return null
        }
        val rotated = if (imageProxy.imageInfo.rotationDegrees != 0) {
            val m = android.graphics.Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            }
            val r = android.graphics.Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, m, true
            )
            if (r !== bitmap) bitmap.recycle()
            r
        } else bitmap

        val rgba = Mat()
        val gray = Mat()
        val claheMat = Mat()
        val bilateral = Mat()
        val canny = Mat()
        val morph = Mat()

        try {
            org.opencv.android.Utils.bitmapToMat(rotated, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)

            val clahe = Imgproc.createCLAHE(3.0, Size(8.0, 8.0))
            try {
                clahe.apply(gray, claheMat)
                clahe.collectGarbage()
            } finally {
                clahe.clear()
            }

            Imgproc.bilateralFilter(claheMat, bilateral, 7, 55.0, 55.0)

            CoreEdgeDetector.detectQuad(bilateral)?.let { pts ->
                val ordered = Corner.orderQuad(pts.toTypedArray())
                val geom = Corner.evaluateGeometry(ordered)
                if (geom.isConvex) {
                    val qArea = computeQuadAreaNorm(ordered, rotated.width, rotated.height)
                    if (qArea in 0.04f..0.98f) {
                        return InternalResult(
                            corners = ordered.map {
                                Offset(
                                    (it.x / rotated.width.toDouble()).toFloat().coerceIn(0f, 1f),
                                    (it.y / rotated.height.toDouble()).toFloat().coerceIn(0f, 1f)
                                )
                            },
                            confidence = (0.40f * geom.score.toFloat() + 0.60f * areaScore(qArea))
                                .coerceIn(0.35f, 0.92f)
                        )
                    }
                }
            }

            val median = computeMedian(bilateral)
            val low = (median * 0.48).coerceIn(25.0, 85.0)
            val high = max(low + 28.0, median * 1.18).coerceIn(70.0, 190.0)
            Imgproc.Canny(bilateral, canny, low, high, 3, true)

            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
            Imgproc.morphologyEx(canny, morph, Imgproc.MORPH_CLOSE, kernel)
            Imgproc.dilate(morph, morph, kernel)
            kernel.release()

            val detected = findBestQuad(
                edgeMap = morph,
                frameW = rotated.width,
                frameH = rotated.height
            ) ?: return null

            val qArea = computeQuadAreaPixels(detected, rotated.width, rotated.height) /
                (rotated.width * rotated.height).toDouble()
            return InternalResult(
                corners = detected,
                confidence = qArea.toFloat().coerceIn(0.30f, 0.95f)
            )
        } finally {
            rgba.release(); gray.release(); claheMat.release()
            bilateral.release(); canny.release(); morph.release()
            if (!rotated.isRecycled) rotated.recycle()
        }
    }

    private fun findBestQuad(
        edgeMap: Mat,
        frameW: Int,
        frameH: Int
    ): List<Offset>? {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        try {
            Imgproc.findContours(
                edgeMap, contours, hierarchy,
                Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE
            )
            if (contours.isEmpty()) return null

            val frameArea = (frameW * frameH).toDouble()
            val sorted = contours.sortedByDescending { Imgproc.contourArea(it) }.take(12)

            var best: Array<Point>? = null
            var bestScore = -1.0

            for (c in sorted) {
                val area = Imgproc.contourArea(c)
                if (area / frameArea < 0.035) continue

                val c2f = MatOfPoint2f(*c.toArray())
                try {
                    val peri = Imgproc.arcLength(c2f, true)
                    for (eps in EPSILON_VALUES) {
                        val approx = MatOfPoint2f()
                        try {
                            Imgproc.approxPolyDP(c2f, approx, eps * peri, true)
                            if (approx.total() != 4L) continue
                            val raw = Corner.orderQuad(approx.toArray())
                            val refined = Corner.refineCornersByEdgeIntersection(
                                quad = raw, contour = c.toArray(),
                                edgeMap = edgeMap, pixelSnapSearchRadius = 4
                            )
                            val ordered = Corner.orderQuad(refined)
                            val geom = Corner.evaluateGeometry(ordered)
                            if (!geom.isConvex) continue
                            if (!isAspectOk(ordered, frameW, frameH)) continue

                            val edgeSupport = Corner.calculateEdgeSupport(ordered, edgeMap, 3)
                            val score = geom.score * 0.55 + edgeSupport * 0.45
                            if (score > bestScore) {
                                bestScore = score
                                best = ordered
                            }
                        } finally {
                            approx.release()
                        }
                    }
                } finally {
                    c2f.release()
                }
            }

            if (best == null) return null
            return best.map {
                Offset(
                    (it.x / frameW.toDouble()).toFloat().coerceIn(0f, 1f),
                    (it.y / frameH.toDouble()).toFloat().coerceIn(0f, 1f)
                )
            }
        } finally {
            hierarchy.release()
            contours.forEach { it.release() }
        }
    }

    private fun isAspectOk(
        ordered: Array<Point>,
        w: Int,
        h: Int,
        config: EdgeDetectionConfig = EdgeDetectionConfig.Default
    ): Boolean {
        val topLen = dist(ordered[0], ordered[1])
        val botLen = dist(ordered[3], ordered[2])
        val leftLen = dist(ordered[0], ordered[3])
        val rightLen = dist(ordered[1], ordered[2])
        val physicalAspect = ((topLen + botLen) / 2.0) /
            max((leftLen + rightLen) / 2.0, 1.0)
        return physicalAspect in config.minAspect..config.maxAspect
    }

    private fun areaScore(qArea: Float): Float = when {
        qArea in 0.18f..0.65f -> 1.0f
        qArea in 0.12f..0.18f -> 0.75f
        qArea in 0.65f..0.78f -> 0.55f
        qArea in 0.08f..0.12f -> 0.35f
        else -> 0.12f
    }

    private fun copyYPlane(
        buffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int
    ): ByteArray {
        val out = ByteArray(width * height)
        val limit = buffer.limit()
        var idx = 0
        for (y in 0 until height) {
            val rowStart = y * rowStride
            for (x in 0 until width) {
                val pos = rowStart + x * pixelStride
                out[idx++] = if (pos >= 0 && pos < limit) buffer.get(pos) else 0
            }
        }
        return out
    }

    private fun computeMedian(mat: Mat): Double {
        val hist = IntArray(256)
        val maxSamples = 120_000
        val totalPixels = mat.rows() * mat.cols()
        val step = max(1, sqrt(totalPixels.toDouble() / maxSamples).toInt())
        val sampleW = max(1, mat.cols() / step)
        val sampleH = max(1, mat.rows() / step)
        val buf = ByteArray(sampleW * sampleH)
        var bi = 0
        var y = 0
        while (y < mat.rows()) {
            var x = 0
            while (x < mat.cols()) {
                buf[bi++] = mat.get(y, x)[0].toInt().toByte()
                x += step
            }
            y += step
        }
        for (i in 0 until bi) hist[buf[i].toInt() and 0xFF]++
        val mid = max(1, bi / 2)
        var total = 0
        for (i in hist.indices) {
            total += hist[i]
            if (total >= mid) return i.toDouble()
        }
        return 128.0
    }

    private fun dist(a: Point, b: Point): Double {
        val dx = a.x - b.x; val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun computeQuadAreaNorm(q: Array<Point>, w: Int, h: Int): Float {
        val (p1, p2, p3, p4) = q
        return abs(
            p1.x * p2.y - p2.x * p1.y +
                p2.x * p3.y - p3.x * p2.y +
                p3.x * p4.y - p4.x * p3.y +
                p4.x * p1.y - p1.x * p4.y
        ).toFloat() * 0.5f / (w * h).toFloat()
    }

    private fun computeQuadAreaPixels(corners: List<Offset>, w: Int, h: Int): Double {
        val pts = corners.map { Point((it.x * w).toDouble(), (it.y * h).toDouble()) }
        val (p1, p2, p3, p4) = pts
        return abs(
            p1.x * p2.y - p2.x * p1.y +
                p2.x * p3.y - p3.x * p2.y +
                p3.x * p4.y - p4.x * p3.y +
                p4.x * p1.y - p1.x * p4.y
        ) * 0.5
    }
}
