package com.docscan.util

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import com.docscan.scanner.Corner
import com.docscan.scanner.DocumentRecognition
import com.docscan.scanner.EdgeDetectionConfig
import com.docscan.scanner.QuadTracker
import com.docscan.scanner.TrackerState
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class DetectionState {
    IDLE,
    SEARCHING_DOCUMENT,
    DOCUMENT_DETECTED,
    DOCUMENT_STABLE,
    CAPTURING,
    PROCESSING
}

/**
 * Ultra-Stable, High-Precision Real-time Edge Detector & Document Contour Analyzer.
 */
object EdgeDetector {

    private const val TAG = "EdgeDetector"
    private const val TARGET_SAMPLE_W = 360
    private const val TARGET_SAMPLE_H = 480

    /**
     * Extracts luminance and analyzes corners directly from CameraX ImageProxy.
     */
    fun analyzeImageProxy(imageProxy: ImageProxy): DetectionResult {
        try {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            if (imageProxy.format == ImageFormat.YUV_420_888 && imageProxy.planes.isNotEmpty()) {
                val yPlane = imageProxy.planes[0]
                val yBuffer = yPlane.buffer
                val srcW = imageProxy.width
                val srcH = imageProxy.height
                val rowStride = yPlane.rowStride
                val pixelStride = yPlane.pixelStride

                val isSensorRotated = (rotationDegrees == 90 || rotationDegrees == 270)
                val uprightAspect = if (isSensorRotated) {
                    srcH.toFloat() / srcW.toFloat()
                } else {
                    srcW.toFloat() / srcH.toFloat()
                }

                // 1. OpenCV fast path
                try {
                    val cvResult = detectFromYuvMat(yBuffer, srcW, srcH, rowStride, pixelStride, rotationDegrees)
                    if (cvResult != null && isRealDocumentDetected(cvResult.corners)) {
                        return DetectionResult(
                            corners = cvResult.corners,
                            isDocumentDetected = true,
                            frameAspectRatio = uprightAspect,
                            confidence = cvResult.confidence
                        )
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "OpenCV YUV processing failed, falling back to pure Kotlin CV", e)
                }

                // 2. Pure Kotlin fallback
                val fallbackCorners = detectFromYuvBuffer(
                    buffer = yBuffer,
                    srcW = srcW,
                    srcH = srcH,
                    rowStride = rowStride,
                    pixelStride = pixelStride,
                    rotationDegrees = rotationDegrees
                )

                val isDetected = isRealDocumentDetected(fallbackCorners)
                return DetectionResult(
                    corners = fallbackCorners,
                    isDocumentDetected = isDetected,
                    frameAspectRatio = uprightAspect,
                    confidence = if (isDetected) 0.60f else 0.18f
                )
            } else {
                // RGBA / Bitmap fallback
                val rawBitmap = imageProxy.toBitmap()
                val rotatedBitmap = if (rotationDegrees != 0) {
                    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                    val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                    rawBitmap.recycle()
                    rotated
                } else {
                    rawBitmap
                }

                val corners = detectDocumentCorners(rotatedBitmap)
                val isDetected = isRealDocumentDetected(corners)
                val uprightAspect = rotatedBitmap.width.toFloat() / rotatedBitmap.height.toFloat()
                rotatedBitmap.recycle()

                return DetectionResult(
                    corners = corners,
                    isDocumentDetected = isDetected,
                    frameAspectRatio = uprightAspect,
                    confidence = if (isDetected) 0.70f else 0.18f
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "analyzeImageProxy error", e)
            return DetectionResult(defaultCorners(), false, confidence = 0.1f)
        } finally {
            try {
                imageProxy.close()
            } catch (_: Exception) {}
        }
    }

    private data class InternalDetection(val corners: List<Offset>, val confidence: Float)

    /**
     * Fast OpenCV analysis directly from CameraX YUV buffer.
     * Collects ALL good candidates and returns the highest confidence one.
     */
    private fun detectFromYuvMat(
        yBuffer: ByteBuffer,
        srcW: Int,
        srcH: Int,
        rowStride: Int,
        pixelStride: Int,
        rotationDegrees: Int
    ): InternalDetection? {
        val yBytes = ByteArray(srcW * srcH)
        yBuffer.rewind()

        if (rowStride == srcW && pixelStride == 1) {
            yBuffer.get(yBytes, 0, srcW * srcH)
        } else {
            val bufLimit = yBuffer.limit()
            var outIdx = 0
            for (r in 0 until srcH) {
                val rowStart = r * rowStride
                for (c in 0 until srcW) {
                    val byteIdx = rowStart + c * pixelStride
                    if (byteIdx < bufLimit) {
                        yBytes[outIdx++] = yBuffer.get(byteIdx)
                    }
                }
            }
        }

        val rawMat = Mat(srcH, srcW, CvType.CV_8UC1)
        rawMat.put(0, 0, yBytes)

        val rotatedMat = Mat()
        when (rotationDegrees) {
            90 -> Core.rotate(rawMat, rotatedMat, Core.ROTATE_90_CLOCKWISE)
            180 -> Core.rotate(rawMat, rotatedMat, Core.ROTATE_180)
            270 -> Core.rotate(rawMat, rotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE)
            else -> rawMat.copyTo(rotatedMat)
        }
        rawMat.release()

        val rotW = rotatedMat.width()
        val rotH = rotatedMat.height()

        val longSide = 640.0
        val (wW, wH) = if (rotW >= rotH) {
            longSide.toInt() to max(1, (rotH * longSide / rotW).toInt())
        } else {
            max(1, (rotW * longSide / rotH).toInt()) to longSide.toInt()
        }

        val scaledMat = Mat()
        Imgproc.resize(rotatedMat, scaledMat, Size(wW.toDouble(), wH.toDouble()), 0.0, 0.0, Imgproc.INTER_AREA)
        rotatedMat.release()

        val claheGray = Mat()
        val blurred = Mat()
        val canny = Mat()
        val scharrEdges = Mat()
        val adaptEdges = Mat()
        val fusedEdges = Mat()
        val morphClosed = Mat()

        try {
            val config = EdgeDetectionConfig.Default
            val clahe = Imgproc.createCLAHE(config.claheClipLimit, Size(8.0, 8.0))
            clahe.apply(scaledMat, claheGray)
            clahe.collectGarbage()

            Imgproc.GaussianBlur(claheGray, blurred, Size(5.0, 5.0), 1.2)

            val recognition = DocumentRecognition.evaluate(claheGray, config)

            // Adaptive Canny
            val med = Core.mean(blurred).`val`[0]
            val lowThresh = (med * config.cannyLowFactor + config.cannyLowBias).coerceIn(10.0, 85.0)
            val highThresh = (med * config.cannyHighFactor + config.cannyLowBias * 1.8).coerceIn(35.0, 210.0)
            Imgproc.Canny(blurred, canny, lowThresh, highThresh)

            // Scharr
            val gradX = Mat()
            val gradY = Mat()
            val mag32f = Mat()
            val mag8u = Mat()
            try {
                Imgproc.Scharr(blurred, gradX, CvType.CV_32F, 1, 0)
                Imgproc.Scharr(blurred, gradY, CvType.CV_32F, 0, 1)
                Core.magnitude(gradX, gradY, mag32f)
                Core.convertScaleAbs(mag32f, mag8u)
                val scharrThresh = max(22.0, med * 0.25)
                Imgproc.threshold(mag8u, scharrEdges, scharrThresh, 255.0, Imgproc.THRESH_BINARY)
            } finally {
                gradX.release(); gradY.release(); mag32f.release(); mag8u.release()
            }

            // Adaptive threshold
            val adaptRaw = Mat()
            try {
                Imgproc.adaptiveThreshold(
                    blurred, adaptRaw, 255.0,
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY_INV, 25, 7.0
                )
                val kGrad = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
                Imgproc.morphologyEx(adaptRaw, adaptEdges, Imgproc.MORPH_GRADIENT, kGrad)
                kGrad.release()
            } finally {
                adaptRaw.release()
            }

            // Fuse
            canny.copyTo(fusedEdges)
            Core.bitwise_or(fusedEdges, scharrEdges, fusedEdges)
            Core.bitwise_or(fusedEdges, adaptEdges, fusedEdges)

            val kClose = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
            Imgproc.morphologyEx(fusedEdges, morphClosed, Imgproc.MORPH_CLOSE, kClose, Point(-1.0, -1.0), 2)
            kClose.release()

            val contours = ArrayList<org.opencv.core.MatOfPoint>()
            val hierarchy = Mat()
            try {
                Imgproc.findContours(
                    morphClosed, contours, hierarchy,
                    Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
                )
                if (contours.isEmpty()) return null

                val frameArea = (wW * wH).toDouble()
                val sorted = contours.sortedByDescending { Imgproc.contourArea(it) }.take(12)

                data class Cand(val corners: List<Offset>, val conf: Float)
                val candidates = ArrayList<Cand>()

                for (contour in sorted) {
                    val area = Imgproc.contourArea(contour)
                    if (area < 0.035 * frameArea) continue

                    val c2f = org.opencv.core.MatOfPoint2f(*contour.toArray())
                    val peri = Imgproc.arcLength(c2f, true)

                    for (eps in floatArrayOf(0.012f, 0.018f, 0.025f, 0.032f, 0.040f)) {
                        val approx = org.opencv.core.MatOfPoint2f()
                        try {
                            Imgproc.approxPolyDP(c2f, approx, eps * peri, true)
                            if (approx.total() == 4L) {
                                val pts = approx.toArray()
                                val ordered = Corner.orderQuad(pts)
                                val refined = Corner.refineCorners(ordered, fusedEdges, searchRadius = 4)

                                val qArea = calculateQuadArea(
                                    Offset((refined[0].x / wW).toFloat(), (refined[0].y / wH).toFloat()),
                                    Offset((refined[1].x / wW).toFloat(), (refined[1].y / wH).toFloat()),
                                    Offset((refined[2].x / wW).toFloat(), (refined[2].y / wH).toFloat()),
                                    Offset((refined[3].x / wW).toFloat(), (refined[3].y / wH).toFloat())
                                )

                                if (qArea in 0.045f..0.97f) {
                                    val geom = Corner.evaluateGeometry(refined)
                                    val edgeSupport = Corner.calculateEdgeSupport(refined, fusedEdges, searchRadiusPx = 3)

                                    val geomScore = if (geom.isConvex) {
                                        geom.score.toFloat()
                                    } else {
                                        (geom.score * 0.55).toFloat()
                                    }

                                    val conf = (
                                        0.28f * geomScore +
                                        0.32f * edgeSupport.toFloat() +
                                        0.25f * recognition.confidence +
                                        0.15f * qArea.coerceIn(0.08f, 0.85f)
                                    ).coerceIn(0f, 1f)

                                    if (conf > 0.20f) {
                                        candidates.add(
                                            Cand(
                                                listOf(
                                                    Offset(
                                                        (refined[0].x / wW).toFloat().coerceIn(0f, 1f),
                                                        (refined[0].y / wH).toFloat().coerceIn(0f, 1f)
                                                    ),
                                                    Offset(
                                                        (refined[1].x / wW).toFloat().coerceIn(0f, 1f),
                                                        (refined[1].y / wH).toFloat().coerceIn(0f, 1f)
                                                    ),
                                                    Offset(
                                                        (refined[2].x / wW).toFloat().coerceIn(0f, 1f),
                                                        (refined[2].y / wH).toFloat().coerceIn(0f, 1f)
                                                    ),
                                                    Offset(
                                                        (refined[3].x / wW).toFloat().coerceIn(0f, 1f),
                                                        (refined[3].y / wH).toFloat().coerceIn(0f, 1f)
                                                    )
                                                ),
                                                conf
                                            )
                                        )
                                    }
                                }
                            }
                        } finally {
                            approx.release()
                        }
                    }
                    c2f.release()
                }

                if (candidates.isEmpty()) return null

                val best = candidates.maxByOrNull { it.conf } ?: return null
                return InternalDetection(best.corners, best.conf)
            } finally {
                hierarchy.release()
                contours.forEach { it.release() }
            }
        } finally {
            scaledMat.release()
            claheGray.release()
            blurred.release()
            canny.release()
            scharrEdges.release()
            adaptEdges.release()
            fusedEdges.release()
            morphClosed.release()
        }
    }

    /**
     * Zero-allocation YUV downsampling + pure Kotlin contour detection.
     */
    private fun detectFromYuvBuffer(
        buffer: ByteBuffer,
        srcW: Int,
        srcH: Int,
        rowStride: Int,
        pixelStride: Int,
        rotationDegrees: Int
    ): List<Offset> {
        val isSensorRotated = (rotationDegrees == 90 || rotationDegrees == 270)
        val outW = if (isSensorRotated) TARGET_SAMPLE_W else TARGET_SAMPLE_H
        val outH = if (isSensorRotated) TARGET_SAMPLE_H else TARGET_SAMPLE_W

        val gray = FloatArray(outW * outH)
        buffer.rewind()

        var minLum = 255f
        var maxLum = 0f
        val bufLimit = buffer.limit()

        for (oy in 0 until outH) {
            val rowOffset = oy * outW
            for (ox in 0 until outW) {
                val sx: Int
                val sy: Int
                when (rotationDegrees) {
                    90 -> {
                        sx = ((oy * srcW) / outH).coerceIn(0, srcW - 1)
                        sy = (((outW - 1 - ox) * srcH) / outW).coerceIn(0, srcH - 1)
                    }
                    180 -> {
                        sx = (((outW - 1 - ox) * srcW) / outW).coerceIn(0, srcW - 1)
                        sy = (((outH - 1 - oy) * srcH) / outH).coerceIn(0, srcH - 1)
                    }
                    270 -> {
                        sx = (((outH - 1 - oy) * srcW) / outH).coerceIn(0, srcW - 1)
                        sy = ((ox * srcH) / outW).coerceIn(0, srcH - 1)
                    }
                    else -> {
                        sx = ((ox * srcW) / outW).coerceIn(0, srcW - 1)
                        sy = ((oy * srcH) / outH).coerceIn(0, srcH - 1)
                    }
                }

                val byteIndex = sy * rowStride + sx * pixelStride
                if (byteIndex in 0 until bufLimit) {
                    val lum = (buffer.get(byteIndex).toInt() and 0xFF).toFloat()
                    gray[rowOffset + ox] = lum
                    if (lum < minLum) minLum = lum
                    if (lum > maxLum) maxLum = lum
                }
            }
        }

        return processGrayscaleMatrix(gray, outW, outH, minLum, maxLum)
    }

    fun idCardFrameCorners(): List<Offset> {
        val left = 0.08f
        val right = 0.92f
        val top = 0.22f
        val bottom = 0.72f
        return listOf(
            Offset(left, top),
            Offset(right, top),
            Offset(right, bottom),
            Offset(left, bottom)
        )
    }

    /**
     * Detect document corners from a Bitmap (still image path).
     */
    fun detectDocumentCorners(bitmap: Bitmap): List<Offset> {
        val origW = bitmap.width
        val origH = bitmap.height
        if (origW < 10 || origH < 10) {
            return defaultCorners()
        }

        try {
            val cvResult = com.docscan.scanner.EdgeDetector.detect(bitmap)
            if (cvResult != null) {
                val quad = cvResult.quad
                val tl = cvResult.toFullSpace(quad.tl)
                val tr = cvResult.toFullSpace(quad.tr)
                val br = cvResult.toFullSpace(quad.br)
                val bl = cvResult.toFullSpace(quad.bl)
                val cvCorners = listOf(
                    Offset((tl.x / origW).toFloat().coerceIn(0f, 1f), (tl.y / origH).toFloat().coerceIn(0f, 1f)),
                    Offset((tr.x / origW).toFloat().coerceIn(0f, 1f), (tr.y / origH).toFloat().coerceIn(0f, 1f)),
                    Offset((br.x / origW).toFloat().coerceIn(0f, 1f), (br.y / origH).toFloat().coerceIn(0f, 1f)),
                    Offset((bl.x / origW).toFloat().coerceIn(0f, 1f), (bl.y / origH).toFloat().coerceIn(0f, 1f))
                )
                if (isRealDocumentDetected(cvCorners)) {
                    return cvCorners
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "OpenCV pipeline failed, falling back to Kotlin CV pipeline", e)
        }

        val scale = max(origW.toFloat() / TARGET_SAMPLE_W.toFloat(), origH.toFloat() / TARGET_SAMPLE_H.toFloat())
        val procW = (origW / max(1f, scale)).toInt().coerceAtLeast(60)
        val procH = (origH / max(1f, scale)).toInt().coerceAtLeast(60)

        val scaledBitmap = if (scale > 1.15f) {
            Bitmap.createScaledBitmap(bitmap, procW, procH, true)
        } else {
            bitmap
        }

        val pixels = IntArray(procW * procH)
        scaledBitmap.getPixels(pixels, 0, procW, 0, 0, procW, procH)
        if (scaledBitmap !== bitmap) {
            scaledBitmap.recycle()
        }

        val gray = FloatArray(procW * procH)
        var minLum = 255f
        var maxLum = 0f

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val lum = 0.2126f * r + 0.7152f * g + 0.0722f * b
            gray[i] = lum
            if (lum < minLum) minLum = lum
            if (lum > maxLum) maxLum = lum
        }

        return processGrayscaleMatrix(gray, procW, procH, minLum, maxLum)
    }

    private fun processGrayscaleMatrix(
        gray: FloatArray,
        w: Int,
        h: Int,
        minLum: Float,
        maxLum: Float
    ): List<Offset> {
        val lumRange = max(1f, maxLum - minLum)
        val norm = FloatArray(w * h)
        for (i in gray.indices) {
            norm[i] = ((gray[i] - minLum) / lumRange) * 255f
        }

        // 5x5 Gaussian
        val blurred = FloatArray(w * h)
        val kernel5 = floatArrayOf(
            1f, 4f, 6f, 4f, 1f,
            4f, 16f, 24f, 16f, 4f,
            6f, 24f, 36f, 24f, 6f,
            4f, 16f, 24f, 16f, 4f,
            1f, 4f, 6f, 4f, 1f
        )
        val kernelSum = 256f

        for (y in 2 until h - 2) {
            val rowY = y * w
            for (x in 2 until w - 2) {
                var sum = 0f
                var k = 0
                for (ky in -2..2) {
                    val rowOffset = (y + ky) * w
                    for (kx in -2..2) {
                        sum += norm[rowOffset + (x + kx)] * kernel5[k++]
                    }
                }
                blurred[rowY + x] = sum / kernelSum
            }
        }

        // Scharr gradients
        val gradMag = FloatArray(w * h)
        val gradX = FloatArray(w * h)
        val gradY = FloatArray(w * h)
        var sumGrad = 0.0
        var countGrad = 0

        for (y in 2 until h - 2) {
            val rowY = y * w
            for (x in 2 until w - 2) {
                val gX = (
                    -3f * blurred[(y - 1) * w + (x - 1)] + 3f * blurred[(y - 1) * w + (x + 1)] +
                    -10f * blurred[rowY + (x - 1)] + 10f * blurred[rowY + (x + 1)] +
                    -3f * blurred[(y + 1) * w + (x - 1)] + 3f * blurred[(y + 1) * w + (x + 1)]
                ) / 16f

                val gY = (
                    -3f * blurred[(y - 1) * w + (x - 1)] - 10f * blurred[(y - 1) * w + x] - 3f * blurred[(y - 1) * w + (x + 1)] +
                     3f * blurred[(y + 1) * w + (x - 1)] + 10f * blurred[(y + 1) * w + x] + 3f * blurred[(y + 1) * w + (x + 1)]
                ) / 16f

                val mag = sqrt(gX * gX + gY * gY)
                gradMag[rowY + x] = mag
                gradX[rowY + x] = gX
                gradY[rowY + x] = gY

                if (mag > 6f) {
                    sumGrad += mag
                    countGrad++
                }
            }
        }

        val avgGrad = if (countGrad > 0) (sumGrad / countGrad).toFloat() else 14f
        val edgeThreshold = max(10f, avgGrad * 0.90f)

        // Strategy 1: Ray-cast
        val rayQuad = detectQuadViaRayCast(gradMag, gradX, gradY, w, h, edgeThreshold)
        if (rayQuad != null) {
            val rayArea = calculateQuadArea(rayQuad[0], rayQuad[1], rayQuad[2], rayQuad[3])
            if (rayArea in 0.05f..0.95f && isValidConvex(rayQuad[0], rayQuad[1], rayQuad[2], rayQuad[3])) {
                return rayQuad
            }
        }

        // Strategy 2: Boundary peaks
        val peakQuad = detectQuadViaBoundaryPeaks(gradMag, w, h, edgeThreshold)
        if (peakQuad != null) {
            val peakArea = calculateQuadArea(peakQuad[0], peakQuad[1], peakQuad[2], peakQuad[3])
            if (peakArea in 0.05f..0.95f && isValidConvex(peakQuad[0], peakQuad[1], peakQuad[2], peakQuad[3])) {
                return peakQuad
            }
        }

        // Strategy 3: Robust extreme corners
        val marginX = (w * 0.03f).toInt().coerceAtLeast(3)
        val marginY = (h * 0.03f).toInt().coerceAtLeast(3)

        val edgePoints = ArrayList<Offset>(512)
        for (y in marginY until h - marginY) {
            val rowY = y * w
            for (x in marginX until w - marginX) {
                if (gradMag[rowY + x] >= edgeThreshold) {
                    edgePoints.add(Offset(x.toFloat() / w, y.toFloat() / h))
                }
            }
        }

        if (edgePoints.size >= 20) {
            val bestTl = robustExtremeCorner(edgePoints) { it.x * 0.7f + it.y * 1.1f }
            val bestBr = robustExtremeCorner(edgePoints, invert = true) { it.x * 0.7f + it.y * 1.1f }
            val bestTr = robustExtremeCorner(edgePoints, invert = true) { it.x * 1.1f - it.y * 0.7f }
            val bestBl = robustExtremeCorner(edgePoints) { it.x * 1.1f - it.y * 0.7f }

            val fallbackQuad = listOf(bestTl, bestTr, bestBr, bestBl)
            val fbArea = calculateQuadArea(fallbackQuad[0], fallbackQuad[1], fallbackQuad[2], fallbackQuad[3])

            if (fbArea in 0.05f..0.95f && isValidConvex(fallbackQuad[0], fallbackQuad[1], fallbackQuad[2], fallbackQuad[3])) {
                return fallbackQuad
            }
        }

        return defaultCorners()
    }

    private fun detectQuadViaRayCast(
        gradMag: FloatArray,
        gradX: FloatArray,
        gradY: FloatArray,
        w: Int,
        h: Int,
        edgeThresh: Float
    ): List<Offset>? {
        val centerX = w / 2
        val centerY = h / 2

        val topPoints = mutableListOf<Offset>()
        val bottomPoints = mutableListOf<Offset>()
        val leftPoints = mutableListOf<Offset>()
        val rightPoints = mutableListOf<Offset>()

        val colStep = max(3, w / 28)
        for (x in (w * 0.12f).toInt() until (w * 0.88f).toInt() step colStep) {
            var bestTopY = -1
            var maxTopGrad = edgeThresh
            for (y in centerY downTo (h * 0.03f).toInt()) {
                val mag = gradMag[y * w + x]
                if (mag > maxTopGrad && abs(gradY[y * w + x]) > abs(gradX[y * w + x]) * 0.4f) {
                    maxTopGrad = mag
                    bestTopY = y
                }
            }
            if (bestTopY > 0) topPoints.add(Offset(x.toFloat(), bestTopY.toFloat()))

            var bestBottomY = -1
            var maxBottomGrad = edgeThresh
            for (y in centerY until (h * 0.97f).toInt()) {
                val mag = gradMag[y * w + x]
                if (mag > maxBottomGrad && abs(gradY[y * w + x]) > abs(gradX[y * w + x]) * 0.4f) {
                    maxBottomGrad = mag
                    bestBottomY = y
                }
            }
            if (bestBottomY > 0) bottomPoints.add(Offset(x.toFloat(), bestBottomY.toFloat()))
        }

        val rowStep = max(3, h / 28)
        for (y in (h * 0.12f).toInt() until (h * 0.88f).toInt() step rowStep) {
            var bestLeftX = -1
            var maxLeftGrad = edgeThresh
            for (x in centerX downTo (w * 0.03f).toInt()) {
                val mag = gradMag[y * w + x]
                if (mag > maxLeftGrad && abs(gradX[y * w + x]) > abs(gradY[y * w + x]) * 0.4f) {
                    maxLeftGrad = mag
                    bestLeftX = x
                }
            }
            if (bestLeftX > 0) leftPoints.add(Offset(bestLeftX.toFloat(), y.toFloat()))

            var bestRightX = -1
            var maxRightGrad = edgeThresh
            for (x in centerX until (w * 0.97f).toInt()) {
                val mag = gradMag[y * w + x]
                if (mag > maxRightGrad && abs(gradX[y * w + x]) > abs(gradY[y * w + x]) * 0.4f) {
                    maxRightGrad = mag
                    bestRightX = x
                }
            }
            if (bestRightX > 0) rightPoints.add(Offset(bestRightX.toFloat(), y.toFloat()))
        }

        if (topPoints.size < 3 || bottomPoints.size < 3 || leftPoints.size < 3 || rightPoints.size < 3) {
            return null
        }

        val topLine = fitLineRansac(topPoints, isHorizontal = true) ?: return null
        val bottomLine = fitLineRansac(bottomPoints, isHorizontal = true) ?: return null
        val leftLine = fitLineRansac(leftPoints, isHorizontal = false) ?: return null
        val rightLine = fitLineRansac(rightPoints, isHorizontal = false) ?: return null

        val tl = intersectLines(topLine, leftLine) ?: return null
        val tr = intersectLines(topLine, rightLine) ?: return null
        val br = intersectLines(bottomLine, rightLine) ?: return null
        val bl = intersectLines(bottomLine, leftLine) ?: return null

        return listOf(
            Offset((tl.x / w).coerceIn(0.01f, 0.99f), (tl.y / h).coerceIn(0.01f, 0.99f)),
            Offset((tr.x / w).coerceIn(0.01f, 0.99f), (tr.y / h).coerceIn(0.01f, 0.99f)),
            Offset((br.x / w).coerceIn(0.01f, 0.99f), (br.y / h).coerceIn(0.01f, 0.99f)),
            Offset((bl.x / w).coerceIn(0.01f, 0.99f), (bl.y / h).coerceIn(0.01f, 0.99f))
        )
    }

    private fun detectQuadViaBoundaryPeaks(
        gradMag: FloatArray,
        w: Int,
        h: Int,
        edgeThresh: Float
    ): List<Offset>? {
        val marginX = (w * 0.03f).toInt()
        val marginY = (h * 0.03f).toInt()

        var minX = w
        var maxX = 0
        var minY = h
        var maxY = 0
        var foundCount = 0

        for (y in marginY until h - marginY step 2) {
            val row = y * w
            for (x in marginX until w - marginX step 2) {
                if (gradMag[row + x] >= edgeThresh) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                    foundCount++
                }
            }
        }

        if (foundCount < 30) return null

        return listOf(
            Offset((minX.toFloat() / w).coerceIn(0.01f, 0.99f), (minY.toFloat() / h).coerceIn(0.01f, 0.99f)),
            Offset((maxX.toFloat() / w).coerceIn(0.01f, 0.99f), (minY.toFloat() / h).coerceIn(0.01f, 0.99f)),
            Offset((maxX.toFloat() / w).coerceIn(0.01f, 0.99f), (maxY.toFloat() / h).coerceIn(0.01f, 0.99f)),
            Offset((minX.toFloat() / w).coerceIn(0.01f, 0.99f), (maxY.toFloat() / h).coerceIn(0.01f, 0.99f))
        )
    }

    private fun fitLineRansac(points: List<Offset>, isHorizontal: Boolean): LineCoeffs? {
        if (points.size < 3) return null
        var sumX = 0.0
        var sumY = 0.0
        for (p in points) {
            sumX += p.x
            sumY += p.y
        }
        val meanX = sumX / points.size
        val meanY = sumY / points.size

        var num = 0.0
        var den = 0.0
        if (isHorizontal) {
            for (p in points) {
                val dx = p.x - meanX
                val dy = p.y - meanY
                num += dx * dy
                den += dx * dx
            }
            val m = if (abs(den) > 1e-4) num / den else 0.0
            val c = meanY - m * meanX
            return LineCoeffs(a = m, b = -1.0, c = c)
        } else {
            for (p in points) {
                val dx = p.x - meanX
                val dy = p.y - meanY
                num += dy * dx
                den += dy * dy
            }
            val m = if (abs(den) > 1e-4) num / den else 0.0
            val c = meanX - m * meanY
            return LineCoeffs(a = -1.0, b = m, c = c)
        }
    }

    private data class LineCoeffs(val a: Double, val b: Double, val c: Double)

    private fun intersectLines(l1: LineCoeffs, l2: LineCoeffs): Offset? {
        val det = l1.a * l2.b - l2.a * l1.b
        if (abs(det) < 1e-5) return null
        val x = (l1.b * l2.c - l2.b * l1.c) / det
        val y = (l2.a * l1.c - l1.a * l2.c) / det
        return Offset(x.toFloat(), y.toFloat())
    }

    private fun robustExtremeCorner(points: List<Offset>, invert: Boolean = false, keyOf: (Offset) -> Float): Offset {
        val sorted = if (invert) {
            points.sortedByDescending { keyOf(it) }
        } else {
            points.sortedBy { keyOf(it) }
        }
        val clusterSize = max(3, (sorted.size * 0.04f).toInt()).coerceAtMost(sorted.size)
        var sumX = 0f
        var sumY = 0f
        for (i in 0 until clusterSize) {
            sumX += sorted[i].x
            sumY += sorted[i].y
        }
        return Offset(sumX / clusterSize, sumY / clusterSize)
    }

    private fun isRealDocumentDetected(corners: List<Offset>): Boolean {
        if (corners.size < 4) return false
        val def = defaultCorners()
        var diff = 0f
        for (i in 0 until 4) {
            diff += abs(corners[i].x - def[i].x) + abs(corners[i].y - def[i].y)
        }
        val area = calculateQuadArea(corners[0], corners[1], corners[2], corners[3])
        // Softened thresholds so valid detections are not rejected
        return diff > 0.015f && area in 0.04f..0.98f
    }

    fun defaultCorners(): List<Offset> {
        return listOf(
            Offset(0.08f, 0.08f),
            Offset(0.92f, 0.08f),
            Offset(0.92f, 0.92f),
            Offset(0.08f, 0.92f)
        )
    }

    fun calculateQuadArea(p1: Offset, p2: Offset, p3: Offset, p4: Offset): Float {
        return 0.5f * abs(
            (p1.x * p2.y - p2.x * p1.y) +
            (p2.x * p3.y - p3.x * p2.y) +
            (p3.x * p4.y - p4.x * p3.y) +
            (p4.x * p1.y - p1.x * p4.y)
        )
    }

    private fun isValidConvex(p1: Offset, p2: Offset, p3: Offset, p4: Offset): Boolean {
        fun crossProduct(a: Offset, b: Offset, c: Offset): Float {
            val abX = b.x - a.x
            val abY = b.y - a.y
            val bcX = c.x - b.x
            val bcY = c.y - b.y
            return abX * bcY - abY * bcX
        }

        val cp1 = crossProduct(p1, p2, p3)
        val cp2 = crossProduct(p2, p3, p4)
        val cp3 = crossProduct(p3, p4, p1)
        val cp4 = crossProduct(p4, p1, p2)

        return (cp1 > 0 && cp2 > 0 && cp3 > 0 && cp4 > 0) ||
               (cp1 < 0 && cp2 < 0 && cp3 < 0 && cp4 < 0)
    }
}

data class DetectionResult(
    val corners: List<Offset>,
    val isDocumentDetected: Boolean,
    val frameAspectRatio: Float = 3f / 4f,
    val confidence: Float = 0.5f
)

/**
 * Adapter that uses QuadTracker for temporal smoothing.
 */
class CornerSmoother(
    historySize: Int = 8,
    stabilityThreshold: Float = 0.022f,
    deadbandThreshold: Float = 0.010f,
    maxStepPerFrame: Float = 0.080f,
    minStableFramesCount: Int = 2,
    lostGraceFramesCount: Int = 8
) {
    private val tracker = QuadTracker(
        historySize = historySize,
        stabilityVarianceThreshold = stabilityThreshold,
        deadbandThreshold = deadbandThreshold,
        maxDisplacementPerFrame = maxStepPerFrame,
        minStableFrames = minStableFramesCount,
        lostGraceFrames = lostGraceFramesCount,
        lockThreshold = 0.38f,      // lowered
        keepThreshold = 0.25f       // lowered
    )

    fun processFrame(detected: DetectionResult): SmoothedFrameState {
        val tracked = tracker.processFrame(
            candidateCorners = detected.corners,
            isDocumentDetected = detected.isDocumentDetected,
            candidateConfidence = detected.confidence,
            frameAspectRatio = detected.frameAspectRatio
        )

        val detState = when (tracked.state) {
            TrackerState.SEARCHING -> DetectionState.SEARCHING_DOCUMENT
            TrackerState.DETECTED -> DetectionState.DOCUMENT_DETECTED
            TrackerState.STABLE -> DetectionState.DOCUMENT_STABLE
            TrackerState.LOST -> DetectionState.SEARCHING_DOCUMENT
        }

        return SmoothedFrameState(
            corners = tracked.corners,
            state = detState,
            isStable = tracked.isStable,
            stableFrames = tracked.stableFramesCount,
            frameAspectRatio = tracked.frameAspectRatio
        )
    }

    fun reset() {
        tracker.reset()
    }
}

data class SmoothedFrameState(
    val corners: List<Offset>,
    val state: DetectionState,
    val isStable: Boolean,
    val stableFrames: Int,
    val frameAspectRatio: Float = 3f / 4f
)