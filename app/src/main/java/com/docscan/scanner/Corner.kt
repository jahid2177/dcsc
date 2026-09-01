package com.docscan.scanner

import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.core.TermCriteria
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Premium Corner Representation and Advanced Geometric Refinement Engine.
 */
object Corner {

    /**
     * Orders 4 points into canonical [TL, TR, BR, BL] order based on centroid.
     */
    fun orderQuad(pts: Array<Point>): Array<Point> {
        require(pts.size == 4) { "Exactly 4 points required for quad ordering" }

        val cx = (pts[0].x + pts[1].x + pts[2].x + pts[3].x) / 4.0
        val cy = (pts[0].y + pts[1].y + pts[2].y + pts[3].y) / 4.0

        val sorted = pts.sortedBy { p ->
            Math.atan2(p.y - cy, p.x - cx)
        }

        var tlIdx = 0
        var minSum = Double.MAX_VALUE
        for (i in sorted.indices) {
            val s = sorted[i].x + sorted[i].y
            if (s < minSum) {
                minSum = s
                tlIdx = i
            }
        }

        val ordered = Array(4) { Point() }
        for (i in 0 until 4) {
            ordered[i] = sorted[(tlIdx + i) % 4]
        }

        val cross = (ordered[1].x - ordered[0].x) * (ordered[2].y - ordered[1].y) -
                    (ordered[1].y - ordered[0].y) * (ordered[2].x - ordered[1].x)
        if (cross < 0) {
            val tmp = ordered[1]
            ordered[1] = ordered[3]
            ordered[3] = tmp
        }

        return ordered
    }

    /**
     * PREMUIM UPGRADE: Sub-pixel corner refinement.
     * Uses OpenCV's iterative sub-pixel algorithm to find the exact fractional
     * coordinate of the corner, massively improving crop accuracy for text.
     */
    fun refineCornersSubPix(
        quad: Array<Point>,
        grayMap: Mat, // Must be a single-channel image (Grayscale or Edge Map)
        windowSize: Int = 5
    ): Array<Point> {
        val corners2f = MatOfPoint2f(*quad)
        
        // Iteration criteria: Stop after 40 iterations or epsilon < 0.001
        val criteria = TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 40, 0.001)
        
        try {
            Imgproc.cornerSubPix(
                grayMap,
                corners2f,
                Size(windowSize.toDouble(), windowSize.toDouble()),
                Size(-1.0, -1.0),
                criteria
            )
            val result = corners2f.toArray()
            // Fallback to original if sub-pix goes wild (creates NaN or jumps too far)
            for (i in 0..3) {
                val dx = result[i].x - quad[i].x
                val dy = result[i].y - quad[i].y
                if (!result[i].x.isFinite() || !result[i].y.isFinite() || sqrt(dx * dx + dy * dy) > windowSize * 3) {
                    result[i] = quad[i]
                }
            }
            return result
        } catch (e: Exception) {
            return quad // Safe fallback
        } finally {
            corners2f.release()
        }
    }

    private data class LineFit(val point: Point, val direction: Point)

    private fun fitLineTLS(points: List<Point>): LineFit? {
        if (points.size < 3) return null

        var sumX = 0.0
        var sumY = 0.0
        for (p in points) {
            sumX += p.x
            sumY += p.y
        }
        val n = points.size
        val meanX = sumX / n
        val meanY = sumY / n

        var sxx = 0.0
        var syy = 0.0
        var sxy = 0.0
        for (p in points) {
            val dx = p.x - meanX
            val dy = p.y - meanY
            sxx += dx * dx
            syy += dy * dy
            sxy += dx * dy
        }
        if (sxx < 1e-6 && syy < 1e-6) return null

        val angle = 0.5 * atan2(2.0 * sxy, sxx - syy)
        return LineFit(Point(meanX, meanY), Point(cos(angle), sin(angle)))
    }

    private fun intersectLines(l1: LineFit, l2: LineFit): Point? {
        val cross = l1.direction.x * l2.direction.y - l1.direction.y * l2.direction.x
        if (abs(cross) < 1e-6) return null
        val dx = l2.point.x - l1.point.x
        val dy = l2.point.y - l1.point.y
        val t = (dx * l2.direction.y - dy * l2.direction.x) / cross
        val x = l1.point.x + t * l1.direction.x
        val y = l1.point.y + t * l1.direction.y
        if (!x.isFinite() || !y.isFinite()) return null
        return Point(x, y)
    }

    /**
     * Refines rounded corners (e.g., ID cards, leather notebooks) by finding the 
     * intersection of the straight edges, bypassing the physical curve.
     */
    fun refineCornersByEdgeIntersection(
        quad: Array<Point>,
        contour: Array<Point>,
        edgeMap: Mat, // Pass edgeMap to sub-pix refinement
        pixelSnapSearchRadius: Int,
        trimRatio: Double = 0.22,
        minPointsPerSide: Int = 6,
        maxShiftRatio: Double = 0.18
    ): Array<Point> {
        // Use the premium sub-pixel refinement as the base
        val subPixSnapped = refineCornersSubPix(quad, edgeMap, pixelSnapSearchRadius)

        if (contour.size < 4 * minPointsPerSide) {
            return subPixSnapped
        }

        val n = contour.size
        val cornerIndices = IntArray(4) { i ->
            var bestIdx = 0
            var bestDist = Double.MAX_VALUE
            for (j in contour.indices) {
                val dx = contour[j].x - quad[i].x
                val dy = contour[j].y - quad[i].y
                val d = dx * dx + dy * dy
                if (d < bestDist) {
                    bestDist = d
                    bestIdx = j
                }
            }
            bestIdx
        }

        fun arcBetween(from: Int, to: Int): List<Point> {
            val points = ArrayList<Point>()
            var i = from
            while (i != to) {
                points.add(contour[i])
                i = (i + 1) % n
            }
            return points
        }

        val sides = Array(4) { k ->
            val forward = arcBetween(cornerIndices[k], cornerIndices[(k + 1) % 4])
            val backward = arcBetween(cornerIndices[(k + 1) % 4], cornerIndices[k])
            if (forward.size <= backward.size) forward else backward
        }

        val lines = Array(4) { k ->
            val side = sides[k]
            if (side.size < minPointsPerSide) return@Array null
            val trim = (side.size * trimRatio).toInt()
            if (side.size - 2 * trim < minPointsPerSide / 2) return@Array null
            fitLineTLS(side.subList(trim, side.size - trim))
        }

        val avgSideLen = (0 until 4).sumOf { k ->
            val a = quad[k]
            val b = quad[(k + 1) % 4]
            sqrt((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y))
        } / 4.0
        val maxShift = (avgSideLen * maxShiftRatio).coerceAtLeast(pixelSnapSearchRadius * 4.0)

        val result = Array(4) { subPixSnapped[it] }
        for (k in 0 until 4) {
            val prevLine = lines[(k + 3) % 4]
            val nextLine = lines[k]
            if (prevLine == null || nextLine == null) continue

            val intersection = intersectLines(prevLine, nextLine) ?: continue
            val dx = intersection.x - quad[k].x
            val dy = intersection.y - quad[k].y
            val shift = sqrt(dx * dx + dy * dy)
            if (shift <= maxShift) {
                result[k] = intersection
            }
        }

        return result
    }

    /**
     * Advanced Edge support validation.
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
     * PREMIUM UPGRADE: Rotation & 3D Perspective-invariant geometry score.
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

        // 3D Perspective Distortion Check (New)
        // A real rectangle in 3D space will have somewhat similar opposite sides
        // Extreme warping usually means a false positive contour.
        val hDistortion = min(lenTop, lenBottom) / max(lenTop, lenBottom)
        val vDistortion = min(lenLeft, lenRight) / max(lenLeft, lenRight)
        val perspectiveScore = ((hDistortion + vDistortion) / 2.0).coerceIn(0.0, 1.0)

        val isConvex = isQuadConvex(p0, p1, p2, p3)

        // Balanced combination
        val combinedGeometry = if (isConvex) {
            0.40 * perpScore + 0.35 * parallelismScore + 0.25 * perspectiveScore
        } else {
            0.0
        }

        return GeometricScore(
            score = combinedGeometry,
            parallelism = parallelismScore,
            perpendicularity = perpScore,
            perspectiveValid = perspectiveScore, // NEW
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
    val perspectiveValid: Double,
    val isConvex: Boolean
)
