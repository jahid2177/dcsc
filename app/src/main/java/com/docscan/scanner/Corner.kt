package com.docscan.scanner

import org.opencv.core.Mat
import org.opencv.core.Point
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Corner Representation and Refinement Engine.
 */
object Corner {

    /**
     * Orders 4 points into canonical [TL, TR, BR, BL] order.
     * Uses polar-angle sort around centroid, then rotates so the point with
     * smallest (x+y) becomes TL, and ensures clockwise winding.
     */
    fun orderQuad(pts: Array<Point>): Array<Point> {
        require(pts.size == 4) { "Exactly 4 points required for quad ordering" }

        val cx = (pts[0].x + pts[1].x + pts[2].x + pts[3].x) / 4.0
        val cy = (pts[0].y + pts[1].y + pts[2].y + pts[3].y) / 4.0

        // Sort counter-clockwise by polar angle from centroid
        val sorted = pts.sortedBy { p ->
            Math.atan2(p.y - cy, p.x - cx)
        }

        // Find the index of the true Top-Left (minimal x + y)
        var tlIdx = 0
        var minSum = Double.MAX_VALUE
        for (i in sorted.indices) {
            val s = sorted[i].x + sorted[i].y
            if (s < minSum) {
                minSum = s
                tlIdx = i
            }
        }

        // Rotate so TL is first: TL, TR, BR, BL
        val ordered = Array(4) { Point() }
        for (i in 0 until 4) {
            ordered[i] = sorted[(tlIdx + i) % 4]
        }

        // Ensure clockwise winding (screen coords: y increases downward)
        // Cross product of TL→TR and TR→BR should be positive for clockwise
        val cross = (ordered[1].x - ordered[0].x) * (ordered[2].y - ordered[1].y) -
                    (ordered[1].y - ordered[0].y) * (ordered[2].x - ordered[1].x)
        if (cross < 0) {
            // Was counter-clockwise → swap TR and BL to make clockwise
            val tmp = ordered[1]
            ordered[1] = ordered[3]
            ordered[3] = tmp
        }

        return ordered
    }

    /**
     * Local neighborhood refinement: snap each corner to the strongest nearby edge pixel.
     *
     * Performance note: reads the whole edge map into a single ByteArray up front
     * (one JNI call) instead of calling `Mat.get(y, x)` per pixel (one JNI call each).
     * For a typical search window this cuts thousands of per-frame JNI round-trips
     * down to one, which is where most of the live-preview detection latency was
     * going — this is the same class of optimization that makes ML Kit's on-device
     * detector feel instant. Output values are numerically identical to before.
     */
    fun refineCorners(
        quad: Array<Point>,
        edgeMap: Mat,
        searchRadius: Int = 5
    ): Array<Point> {
        val w = edgeMap.width()
        val h = edgeMap.height()
        val channels = edgeMap.channels().coerceAtLeast(1)
        val pixels = ByteArray(w * h * channels)
        edgeMap.get(0, 0, pixels)

        fun valueAt(x: Int, y: Int): Int {
            val idx = (y * w + x) * channels
            return pixels[idx].toInt() and 0xFF
        }

        val refined = Array(4) { Point(quad[it].x, quad[it].y) }

        for (i in 0 until 4) {
            val px = quad[i].x.toInt()
            val py = quad[i].y.toInt()

            var bestX = quad[i].x
            var bestY = quad[i].y
            var maxEnergy = 0.0

            val xMin = max(0, px - searchRadius)
            val xMax = min(w - 1, px + searchRadius)
            val yMin = max(0, py - searchRadius)
            val yMax = min(h - 1, py + searchRadius)

            for (y in yMin..yMax) {
                for (x in xMin..xMax) {
                    val pVal = valueAt(x, y).toDouble()
                    if (pVal > 128.0) {
                        val distSq = (x - px) * (x - px) + (y - py) * (y - py)
                        val energy = pVal / (1.0 + distSq * 0.2)
                        if (energy > maxEnergy) {
                            maxEnergy = energy
                            bestX = x.toDouble()
                            bestY = y.toDouble()
                        }
                    }
                }
            }

            refined[i] = Point(bestX, bestY)
        }

        return refined
    }

    /**
     * Edge support ratio along the four sides (±searchRadiusPx neighborhood).
     */
    fun calculateEdgeSupport(
        quad: Array<Point>,
        edgeMap: Mat,
        searchRadiusPx: Int = 2
    ): Double {
        val w = edgeMap.width()
        val h = edgeMap.height()
        val channels = edgeMap.channels().coerceAtLeast(1)
        val pixels = ByteArray(w * h * channels)
        edgeMap.get(0, 0, pixels)

        fun valueAt(x: Int, y: Int): Int {
            val idx = (y * w + x) * channels
            return pixels[idx].toInt() and 0xFF
        }

        var totalSampled = 0
        var supportedSamples = 0

        for (side in 0 until 4) {
            val p1 = quad[side]
            val p2 = quad[(side + 1) % 4]
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val length = sqrt(dx * dx + dy * dy)
            val steps = max(8, (length / 2.0).toInt())

            for (s in 0..steps) {
                val t = s.toDouble() / steps
                val sx = (p1.x + t * dx).toInt()
                val sy = (p1.y + t * dy).toInt()

                totalSampled++

                var hit = false
                searchLoop@ for (oy in -searchRadiusPx..searchRadiusPx) {
                    for (ox in -searchRadiusPx..searchRadiusPx) {
                        val cx = sx + ox
                        val cy = sy + oy
                        if (cx in 0 until w && cy in 0 until h) {
                            if (valueAt(cx, cy) > 128) {
                                hit = true
                                break@searchLoop
                            }
                        }
                    }
                }
                if (hit) supportedSamples++
            }
        }

        return if (totalSampled > 0) supportedSamples.toDouble() / totalSampled else 0.0
    }

    /**
     * Rotation-invariant geometry score (parallelism + perpendicularity + convexity).
     */
    fun evaluateGeometry(quad: Array<Point>): GeometricScore {
        val (p0, p1, p2, p3) = quad

        val vTop = Point(p1.x - p0.x, p1.y - p0.y)
        val vRight = Point(p2.x - p1.x, p2.y - p1.y)
        val vBottom = Point(p2.x - p3.x, p2.y - p3.y)
        val vLeft = Point(p3.x - p0.x, p3.y - p0.y)

        val lenTop = sqrt(vTop.x * vTop.x + vTop.y * vTop.y).coerceAtLeast(1.0)
        val lenRight = sqrt(vRight.x * vRight.x + vRight.y * vRight.y).coerceAtLeast(1.0)
        val lenBottom = sqrt(vBottom.x * vBottom.x + vBottom.y * vBottom.y).coerceAtLeast(1.0)
        val lenLeft = sqrt(vLeft.x * vLeft.x + vLeft.y * vLeft.y).coerceAtLeast(1.0)

        val uTop = Point(vTop.x / lenTop, vTop.y / lenTop)
        val uRight = Point(vRight.x / lenRight, vRight.y / lenRight)
        val uBottom = Point(vBottom.x / lenBottom, vBottom.y / lenBottom)
        val uLeft = Point(vLeft.x / lenLeft, vLeft.y / lenLeft)

        val parHoriz = abs(uTop.x * uBottom.x + uTop.y * uBottom.y)
        val parVert = abs(uLeft.x * uRight.x + uLeft.y * uRight.y)
        val parallelismScore = ((parHoriz + parVert) / 2.0).coerceIn(0.0, 1.0)

        val dotTL = abs(uTop.x * uLeft.x + uTop.y * uLeft.y)
        val dotTR = abs(uTop.x * uRight.x + uTop.y * uRight.y)
        val dotBR = abs(uBottom.x * uRight.x + uBottom.y * uRight.y)
        val dotBL = abs(uBottom.x * uLeft.x + uBottom.y * uLeft.y)
        val perpScore = (1.0 - (dotTL + dotTR + dotBR + dotBL) / 4.0).coerceIn(0.0, 1.0)

        val isConvex = isQuadConvex(p0, p1, p2, p3)

        val combinedGeometry = if (isConvex) {
            0.50 * perpScore + 0.50 * parallelismScore
        } else {
            0.0
        }

        return GeometricScore(
            score = combinedGeometry,
            parallelism = parallelismScore,
            perpendicularity = perpScore,
            isConvex = isConvex
        )
    }

    fun isQuadConvex(p1: Point, p2: Point, p3: Point, p4: Point): Boolean {
        fun cross(a: Point, b: Point, c: Point): Double {
            val abX = b.x - a.x
            val abY = b.y - a.y
            val bcX = c.x - b.x
            val bcY = c.y - b.y
            return abX * bcY - abY * bcX
        }

        val cp1 = cross(p1, p2, p3)
        val cp2 = cross(p2, p3, p4)
        val cp3 = cross(p3, p4, p1)
        val cp4 = cross(p4, p1, p2)

        return (cp1 > 0 && cp2 > 0 && cp3 > 0 && cp4 > 0) ||
               (cp1 < 0 && cp2 < 0 && cp3 < 0 && cp4 < 0)
    }
}

data class GeometricScore(
    val score: Double,
    val parallelism: Double,
    val perpendicularity: Double,
    val isConvex: Boolean
)