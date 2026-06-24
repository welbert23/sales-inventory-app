package com.salesinventory.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.oned.Code128Writer

object BarcodeGenerator {

    fun generateBitmap(barcode: String, width: Int = 400, height: Int = 150): Bitmap {
        return try {
            val writer = Code128Writer()
            val bitMatrix: BitMatrix = writer.encode(barcode, BarcodeFormat.CODE_128, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(barcode, width / 2f, height / 2f, paint)
            bitmap
        }
    }
}
