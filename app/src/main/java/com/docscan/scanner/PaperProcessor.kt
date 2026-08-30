package com.docscan.scanner

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max

/**
 * Image Preprocessing & Illumination Normalization Engine for Document Scanning.
 *
 * Handles:
 * 1. Resolution scaling (live preview vs high-res)
 * 2. Grayscale conversion
 * 3. Contrast Equalization via CLAHE (Contrast-Limited Adaptive Histogram Equalization)
 * 4. Illumination / Shadow compensation
 * 5. Gaussian & Bilateral filtering to smooth texture noise while retaining crisp edge gradients
 */
object PaperProcessor {

    /**
     * Preprocesses a scaled color Mat into an enhanced grayscale Mat for multi-channel edge detection.
     */
    fun processForEdgeDetection(
        rgbaMat: Mat,
        config: EdgeDetectionConfig = EdgeDetectionConfig.Default
    ): ProcessedFrame {
        val gray = Mat()
        val claheGray = Mat()
        val blurred = Mat()

        // 1. Grayscale
        Imgproc.cvtColor(rgbaMat, gray, Imgproc.COLOR_RGBA2GRAY)

        // 2. Contrast Enhancement (CLAHE) for low light / shadows / uneven illumination
        val clahe = Imgproc.createCLAHE(
            config.claheClipLimit,
            Size(config.claheTileSize.toDouble(), config.claheTileSize.toDouble())
        )
        clahe.apply(gray, claheGray)
        clahe.collectGarbage()

        // 3. Gaussian Filter: Smooth out paper grain, bedsheet fabric texture, and noise
        Imgproc.GaussianBlur(claheGray, blurred, Size(5.0, 5.0), 1.2)

        return ProcessedFrame(
            rawGray = gray,
            claheGray = claheGray,
            blurred = blurred
        )
    }
}

/**
 * Container holding intermediate Mats. Call [release] in a finally block to free native memory.
 */
data class ProcessedFrame(
    val rawGray: Mat,
    val claheGray: Mat,
    val blurred: Mat
) {
    fun release() {
        rawGray.release()
        claheGray.release()
        blurred.release()
    }
}
