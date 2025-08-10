package biz.cunning.cunning_document_scanner.fallback.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import biz.cunning.cunning_document_scanner.fallback.models.Quad
import java.io.IOException
import kotlin.math.pow
import kotlin.math.sqrt


class ImageUtil {
    fun getImageFromFilePath(filePath: String): Bitmap? {
        val bitmap = BitmapFactory.decodeFile(filePath) ?: return null
        val rotatedBitmap: Bitmap
        try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1.0f, 1.0f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1.0f, -1.0f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.preScale(-1.0f, 1.0f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.preScale(-1.0f, 1.0f)
                }
                else -> return bitmap
            }
            rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width,
                bitmap.height, matrix, true
            )
            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return bitmap
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            return bitmap
        }
        return rotatedBitmap
    }


    fun crop(photoFilePath: String, corners: Quad): Bitmap? {
        val bitmap = getImageFromFilePath(photoFilePath) ?: return null

        // Convert Quad corners to a float array manually
        val src = floatArrayOf(
            corners.topLeftCorner.x, corners.topLeftCorner.y,
            corners.topRightCorner.x, corners.topRightCorner.y,
            corners.bottomRightCorner.x, corners.bottomRightCorner.y,
            corners.bottomLeftCorner.x, corners.bottomLeftCorner.y
        )

        val avgWidth = getAvgWidth(corners)
        val avgHeight = getAvgHeight(corners)

        // Maintain the aspect ratio based on the longer dimension
        val aspectRatio = avgWidth / avgHeight

        val dstWidth: Float
        val dstHeight: Float

        if (aspectRatio >= 1) { // Width is greater than height, landscape orientation
            dstWidth = avgWidth
            dstHeight = dstWidth / aspectRatio
        } else { // Height is greater than width, portrait orientation
            dstHeight = avgHeight
            dstWidth = dstHeight * aspectRatio
        }

        // Use dstWidth and dstHeight to define your dst points accordingly
        val dst = floatArrayOf(
            0f, 0f,                     // Top-left
            dstWidth, 0f,               // Top-right
            dstWidth, dstHeight,        // Bottom-right
            0f, dstHeight               // Bottom-left
        )

        val croppedBitmap = correctPerspective(bitmap, src, dst, dstWidth, dstHeight)
        if(bitmap != croppedBitmap) {
            bitmap.recycle()
        }
        return croppedBitmap
    }

    fun correctPerspective(b: Bitmap, srcPoints: FloatArray?, dstPoints: FloatArray?, w: Float, h: Float): Bitmap {
        val result = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val c = Canvas(result)
        val m = Matrix()
        m.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)
        c.drawBitmap(b, m, p)
        return result
    }

    private fun getAvgWidth(corners: Quad): Float {
        val widthTop = sqrt(
            (corners.topRightCorner.x - corners.topLeftCorner.x).toDouble().pow(2.0) + (corners.topRightCorner.y - corners.topLeftCorner.y).toDouble()
                .pow(2.0)
        ).toFloat()
        val widthBottom = sqrt(
            (corners.bottomLeftCorner.x - corners.bottomRightCorner.x).toDouble().pow(2.0) + (corners.bottomLeftCorner.y - corners.bottomRightCorner.y).toDouble()
                .pow(2.0)
        ).toFloat()
        return (widthTop + widthBottom) / 2
    }

    private fun getAvgHeight(corners: Quad): Float {
        val heightLeft = sqrt(
            (corners.bottomLeftCorner.x - corners.topLeftCorner.x).toDouble().pow(2.0) + (corners.bottomLeftCorner.y - corners.topLeftCorner.y).toDouble()
                .pow(2.0)
        ).toFloat()
        val heightRight = sqrt(
            (corners.topRightCorner.x - corners.bottomRightCorner.x).toDouble().pow(2.0) + (corners.topRightCorner.y - corners.bottomRightCorner.y).toDouble()
                .pow(2.0)
        ).toFloat()
        return (heightLeft + heightRight) / 2
    }
}