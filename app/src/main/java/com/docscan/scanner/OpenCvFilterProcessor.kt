package com.docscan.scanner

import android.graphics.Bitmap
import android.util.Log
import com.docscan.data.model.FilterType
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * High-performance OpenCV Image Filter Engine for Document Scanning.
 *
 * Implements professional document processing algorithms:
 * 1. [MAGIC_COLOR]: Signature CamScanner multi-scale illumination division, CLAHE,
 *    unsharp text masking, and chrominance saturation boost for vivid stamps and signatures.
 * 2. [BW]: Adaptive Gaussian binarization with bilateral smoothing and speckle noise rejection.
 * 3. [GRAYSCALE]: 8-bit dynamic luminance balancing with CLAHE and subtle unsharp edge sharpening.
 * 4. [DOCUMENT]: Ultra high-contrast crisp text extraction on clean white paper.
 * 5. [CLEAR]: Whitening of yellow/gray casts while preserving authentic color reproduction.
 * 6. [LIGHTEN]: Illumination shadow compensation and background lift.
 * 7. [AUTO]: Intelligent white-balance, automatic levels, and detail enhancement.
 */
object OpenCvFilterProcessor {

    private const val TAG = "OpenCvFilterProcessor"

    /**
     * Applies the requested [FilterType] to the given [source] Bitmap using native OpenCV pipelines.
     * Guaranteed to return a valid Bitmap (or null on unexpected OpenCV failure to allow fallback).
     */
    fun applyFilter(
        source: Bitmap,
        filterType: FilterType,
        brightness: Float = 0f, // -1f to 1f
        contrast: Float = 1f   // 0.5f to 2f
    ): Bitmap? {
        if (source.width <= 0 || source.height <= 0) return null

        val srcMat = Mat()
        var outMat: Mat? = null
        return try {
            Utils.bitmapToMat(source, srcMat)

            outMat = when (filterType) {
                FilterType.ORIGINAL -> applyOriginal(srcMat, brightness, contrast)
                FilterType.MAGIC_COLOR -> applyMagicColor(srcMat, brightness, contrast)
                FilterType.BW -> applyBw(srcMat, brightness, contrast)
                FilterType.GRAYSCALE -> applyGrayscale(srcMat, brightness, contrast)
                FilterType.DOCUMENT -> applyDocument(srcMat, brightness, contrast)
                FilterType.CLEAR -> applyClear(srcMat, brightness, contrast)
                FilterType.LIGHTEN -> applyLighten(srcMat, brightness, contrast)
                FilterType.AUTO -> applyAuto(srcMat, brightness, contrast)
            }

            val resultBitmap = Bitmap.createBitmap(outMat.cols(), outMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(outMat, resultBitmap)
            resultBitmap
        } catch (e: Throwable) {
            Log.e(TAG, "OpenCV filter execution failed for type $filterType", e)
            null
        } finally {
            srcMat.release()
            outMat?.release()
        }
    }

    /**
     * Magic Color Filter (CamScanner Signature):
     * 1. Converts to YCrCb color space.
     * 2. Estimates background illumination using large morphological dilation & Gaussian blur.
     * 3. Divides luminance channel by background estimation to whiten paper and erase dark shadows.
     * 4. Applies CLAHE for local contrast and unsharp masking for razor-sharp text strokes.
     * 5. Amplifies chromatic saturation (Cr & Cb) so stamps, signatures, and colored inks stand out vibrantly.
     */
    fun applyMagicColor(srcMat: Mat, brightness: Float = 0f, contrast: Float = 1f): Mat {
        val rgbMat = Mat()
        val ycrcbMat = Mat()
        Imgproc.cvtColor(srcMat, rgbMat, Imgproc.COLOR_RGBA2RGB)
        Imgproc.cvtColor(rgbMat, ycrcbMat, Imgproc.COLOR_RGB2YCrCb)

        val channels = ArrayList<Mat>(3)
        Core.split(ycrcbMat, channels)
        val y = channels[0]   // Luminance
        val cr = channels[1]  // Chrominance Red
        val cb = channels[2]  // Chrominance Blue

        // 1. Paper Whitening: Background illumination estimation
        val background = Mat()
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(25.0, 25.0))
        Imgproc.morphologyEx(y, background, Imgproc.MORPH_DILATE, kernel)
        Imgproc.GaussianBlur(background, background, Size(31.0, 31.0), 0.0)

        // Normalize: Y_norm = (Y / Background) * 255.0
        val yFloat = Mat()
        val bgFloat = Mat()
        val yNorm = Mat()
        y.convertTo(yFloat, CvType.CV_32F)
        background.convertTo(bgFloat, CvType.CV_32F)
        Core.add(bgFloat, Scalar(1.0), bgFloat) // Prevent division by zero
        Core.divide(yFloat, bgFloat, yNorm, 255.0)

        val yNorm8u = Mat()
        yNorm.convertTo(yNorm8u, CvType.CV_8U)

        // 2. Contrast-Limited Adaptive Histogram Equalization (CLAHE)
        val clahe = Imgproc.createCLAHE(2.2, Size(8.0, 8.0))
        val yClahe = Mat()
        clahe.apply(yNorm8u, yClahe)

        // 3. Unsharp Masking for crisp text edges
        val yBlur = Mat()
        Imgproc.GaussianBlur(yClahe, yBlur, Size(0.0, 0.0), 1.6)
        val ySharp = Mat()
        Core.addWeighted(yClahe, 1.45, yBlur, -0.45, 0.0, ySharp)

        // 4. Boost Chrominance (Stamps, signatures, colored ink)
        // Transform around 128 neutral center: val = 1.35 * (cr - 128) + 128
        val crEnhanced = Mat()
        val cbEnhanced = Mat()
        cr.convertTo(crEnhanced, -1, 1.35, -44.8)
        cb.convertTo(cbEnhanced, -1, 1.35, -44.8)

        // 5. Merge & convert back
        val mergedYCrCb = Mat()
        Core.merge(listOf(ySharp, crEnhanced, cbEnhanced), mergedYCrCb)
        val outRgb = Mat()
        Imgproc.cvtColor(mergedYCrCb, outRgb, Imgproc.COLOR_YCrCb2RGB)

        // 6. User brightness/contrast adjustments
        val finalRgb = Mat()
        val alpha = (contrast * 1.15f).toDouble()
        val beta = (brightness * 60.0 + 10.0)
        outRgb.convertTo(finalRgb, -1, alpha, beta)

        val outRgba = Mat()
        Imgproc.cvtColor(finalRgb, outRgba, Imgproc.COLOR_RGB2RGBA)

        // Cleanup native Mats
        rgbMat.release(); ycrcbMat.release(); channels.forEach { it.release() }
        background.release(); kernel.release(); yFloat.release(); bgFloat.release()
        yNorm.release(); yNorm8u.release(); yClahe.release(); yBlur.release(); ySharp.release()
        crEnhanced.release(); cbEnhanced.release(); mergedYCrCb.release(); outRgb.release(); finalRgb.release()
        clahe.collectGarbage()

        return outRgba
    }

    /**
     * Black & White (B&W) Adaptive Document Binarization:
     * 1. Converts to Grayscale.
     * 2. Applies Bilateral Filtering to remove paper grain texture while preserving character boundaries.
     * 3. Executes Adaptive Gaussian Thresholding for pure crisp black text on pure white paper.
     * 4. Morphological closure to eliminate isolated noise speckles.
     */
    fun applyBw(srcMat: Mat, brightness: Float = 0f, contrast: Float = 1f): Mat {
        val grayMat = Mat()
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY)

        // 1. Bilateral Smoothing
        val smoothed = Mat()
        Imgproc.bilateralFilter(grayMat, smoothed, 5, 50.0, 50.0)

        // 2. Adaptive Gaussian Thresholding
        val cOffset = (11.0 - (brightness * 16.0)).coerceIn(3.0, 24.0)
        val binary = Mat()
        Imgproc.adaptiveThreshold(
            smoothed,
            binary,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            25,
            cOffset
        )

        // 3. Speckle Noise Reduction (Morphological Close)
        val clean = Mat()
        val k = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(2.0, 2.0))
        Imgproc.morphologyEx(binary, clean, Imgproc.MORPH_CLOSE, k)

        // 4. Convert to 4-channel RGBA
        val outRgba = Mat()
        Imgproc.cvtColor(clean, outRgba, Imgproc.COLOR_GRAY2RGBA)

        grayMat.release(); smoothed.release(); binary.release(); clean.release(); k.release()
        return outRgba
    }

    /**
     * Grayscale Filter:
     * 1. Converts to 8-bit single channel Luminance.
     * 2. Applies CLAHE for local contrast normalization across unevenly lit pages.
     * 3. Applies Unsharp Mask for character sharpness and crisp line rendering.
     * 4. Maps back to RGBA with user brightness & contrast.
     */
    fun applyGrayscale(srcMat: Mat, brightness: Float = 0f, contrast: Float = 1f): Mat {
        val grayMat = Mat()
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY)

        // 1. CLAHE
        val clahe = Imgproc.createCLAHE(1.8, Size(8.0, 8.0))
        val claheMat = Mat()
        clahe.apply(grayMat, claheMat)

        // 2. Subtle Unsharp Masking
        val blur = Mat()
        Imgproc.GaussianBlur(claheMat, blur, Size(0.0, 0.0), 1.2)
        val sharp = Mat()
        Core.addWeighted(claheMat, 1.25, blur, -0.25, 0.0, sharp)

        // 3. User Adjustments
        val adjusted = Mat()
        val alpha = (contrast * 1.1f).toDouble()
        val beta = (brightness * 50.0 + 8.0)
        sharp.convertTo(adjusted, -1, alpha, beta)

        val outRgba = Mat()
        Imgproc.cvtColor(adjusted, outRgba, Imgproc.COLOR_GRAY2RGBA)

        grayMat.release(); claheMat.release(); blur.release(); sharp.release(); adjusted.release()
        clahe.collectGarbage()
        return outRgba
    }

    /**
     * Document Text Filter:
     * High text contrast booster for maximum legibility of fine print.
     */
    fun applyDocument(srcMat: Mat, brightness: Float = 0f, contrast: Float = 1f): Mat {
        val rgbMat = Mat()
        val ycrcbMat = Mat()
        Imgproc.cvtColor(srcMat, rgbMat, Imgproc.COLOR_RGBA2RGB)
        Imgproc.cvtColor(rgbMat, ycrcbMat, Imgproc.COLOR_RGB2YCrCb)

        val channels = ArrayList<Mat>(3)
        Core.split(ycrcbMat, channels)
        val y = channels[0]

        // Strong CLAHE on Luminance
        val clahe = Imgproc.createCLAHE(3.0, Size(8.0, 8.0))
        val yClahe = Mat()
        clahe.apply(y, yClahe)

        // Unsharp Mask
        val yBlur = Mat()
        Imgproc.GaussianBlur(yClahe, yBlur, Size(0.0, 0.0), 1.8)
        val ySharp = Mat()
        Core.addWeighted(yClahe, 1.6, yBlur, -0.6, 0.0, ySharp)

        // Low saturation for text clarity
        val cr = channels[1]
        val cb = channels[2]
        val crMuted = Mat()
        val cbMuted = Mat()
        cr.convertTo(crMuted, -1, 0.85, 19.2) // Dampen color noise
        cb.convertTo(cbMuted, -1, 0.85, 19.2)

        val merged = Mat()
        Core.merge(listOf(ySharp, crMuted, cbMuted), merged)
        val outRgb = Mat()
        Imgproc.cvtColor(merged, outRgb, Imgproc.COLOR_YCrCb2RGB)

        val finalRgb = Mat()
        val alpha = (contrast * 1.3f).toDouble()
        val beta = (brightness * 50.0 + 15.0)
        outRgb.convertTo(finalRgb, -1, alpha, beta)

        val outRgba = Mat()
        Imgproc.cvtColor(finalRgb, outRgba, Imgproc.COLOR_RGB2RGBA)

        rgbMat.release(); ycrcbMat.release(); channels.forEach { it.release() }
        yClahe.release(); yBlur.release(); ySharp.release(); crMuted.release(); cbMuted.release()
        merged.release(); outRgb.release(); finalRgb.release()
        clahe.collectGarbage()
        return outRgba
    }

    /**
     * Clear Filter:
     * Whitens yellowed paper background while maintaining authentic natural ink colors.
     */
    fun applyClear(srcMat: Mat, brightness: Float = 0f, contrast: Float = 1f): Mat {
        val rgbMat = Mat()
        val ycrcbMat = Mat()
        Imgproc.cvtColor(srcMat, rgbMat, Imgproc.COLOR_RGBA2RGB)
        Imgproc.cvtColor(rgbMat, ycrcbMat, Imgproc.COLOR_RGB2YCrCb)

        val channels = ArrayList<Mat>(3)
        Core.split(ycrcbMat, channels)
        val y = channels[0]

        // Light background lift
        val yLift = Mat()
        val alphaY = 1.15
        val betaY = 20.0
        y.convertTo(yLift, -1, alphaY, betaY)

        val clahe = Imgproc.createCLAHE(1.5, Size(8.0, 8.0))
        val yClahe = Mat()
        clahe.apply(yLift, yClahe)

        val merged = Mat()
        Core.merge(listOf(yClahe, channels[1], channels[2]), merged)
        val outRgb = Mat()
        Imgproc.cvtColor(merged, outRgb, Imgproc.COLOR_YCrCb2RGB)

        val finalRgb = Mat()
        val alpha = (contrast * 1.2f).toDouble()
        val beta = (brightness * 45.0 + 12.0)
        outRgb.convertTo(finalRgb, -1, alpha, beta)

        val outRgba = Mat()
        Imgproc.cvtColor(finalRgb, outRgba, Imgproc.COLOR_RGB2RGBA)

        rgbMat.release(); ycrcbMat.release(); channels.forEach { it.release() }
        yLift.release(); yClahe.release(); merged.release(); outRgb.release(); finalRgb.release()
        clahe.collectGarbage()
        return outRgba
    }

    /**
     * Lighten Filter:
     * Removes dark corner shadows and lifts underexposed document scans.
     */
    fun applyLighten(srcMat: Mat, brightness: Float = 0f, contrast: Float = 1f): Mat {
        val rgbMat = Mat()
        Imgproc.cvtColor(srcMat, rgbMat, Imgproc.COLOR_RGBA2RGB)

        // Gamma correction / brightness lift
        val brightRgb = Mat()
        val alpha = (contrast * 1.1f).toDouble()
        val beta = (brightness * 50.0 + 35.0)
        rgbMat.convertTo(brightRgb, -1, alpha, beta)

        val outRgba = Mat()
        Imgproc.cvtColor(brightRgb, outRgba, Imgproc.COLOR_RGB2RGBA)

        rgbMat.release(); brightRgb.release()
        return outRgba
    }

    /**
     * Auto Filter:
     * Smart contrast & balanced natural color enhancement.
     */
    fun applyAuto(srcMat: Mat, brightness: Float = 0f, contrast: Float = 1f): Mat {
        val rgbMat = Mat()
        val ycrcbMat = Mat()
        Imgproc.cvtColor(srcMat, rgbMat, Imgproc.COLOR_RGBA2RGB)
        Imgproc.cvtColor(rgbMat, ycrcbMat, Imgproc.COLOR_RGB2YCrCb)

        val channels = ArrayList<Mat>(3)
        Core.split(ycrcbMat, channels)
        val y = channels[0]

        // Subtle adaptive equalization
        val clahe = Imgproc.createCLAHE(1.6, Size(8.0, 8.0))
        val yClahe = Mat()
        clahe.apply(y, yClahe)

        // Unsharp mask
        val yBlur = Mat()
        Imgproc.GaussianBlur(yClahe, yBlur, Size(0.0, 0.0), 1.2)
        val ySharp = Mat()
        Core.addWeighted(yClahe, 1.3, yBlur, -0.3, 0.0, ySharp)

        val merged = Mat()
        Core.merge(listOf(ySharp, channels[1], channels[2]), merged)
        val outRgb = Mat()
        Imgproc.cvtColor(merged, outRgb, Imgproc.COLOR_YCrCb2RGB)

        val finalRgb = Mat()
        val alpha = (contrast * 1.2f).toDouble()
        val beta = (brightness * 50.0 + 10.0)
        outRgb.convertTo(finalRgb, -1, alpha, beta)

        val outRgba = Mat()
        Imgproc.cvtColor(finalRgb, outRgba, Imgproc.COLOR_RGB2RGBA)

        rgbMat.release(); ycrcbMat.release(); channels.forEach { it.release() }
        yClahe.release(); yBlur.release(); ySharp.release(); merged.release(); outRgb.release(); finalRgb.release()
        clahe.collectGarbage()
        return outRgba
    }

    /**
     * Original identity pass-through with user brightness & contrast.
     */
    private fun applyOriginal(srcMat: Mat, brightness: Float = 0f, contrast: Float = 1f): Mat {
        if (brightness == 0f && contrast == 1f) {
            return srcMat.clone()
        }
        val adjusted = Mat()
        val alpha = contrast.toDouble()
        val beta = (brightness * 100.0)
        srcMat.convertTo(adjusted, -1, alpha, beta)
        return adjusted
    }
}
