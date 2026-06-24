package com.salesinventory.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    fun generateSalesReport(
        context: Context,
        storeName: String,
        title: String,
        headers: List<String>,
        rows: List<List<String>>,
        summary: List<Pair<String, String>>
    ): Uri {
        val document = PdfDocument()
        val paint = Paint()
        val paintBold = Paint().apply {
            typeface = Typeface.DEFAULT_BOLD
        }

        val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        var y = 40f
        val leftMargin = 40f
        val columnWidth = 80f

        paintBold.textSize = 18f
        canvas.drawText(storeName.ifBlank { "Sales Report" }, leftMargin, y, paintBold)

        y += 24f
        paint.textSize = 12f
        paint.color = Color.DKGRAY
        canvas.drawText(title, leftMargin, y, paint)
        paint.color = Color.BLACK

        y += 20f
        paint.color = Color.LTGRAY
        canvas.drawRect(leftMargin - 5, y - 14f, 572f, y + 4f, paint)
        paint.color = Color.BLACK

        paintBold.textSize = 10f
        paint.textSize = 10f

        var x = leftMargin
        for (h in headers) {
            canvas.drawText(h, x, y, paintBold)
            x += columnWidth
        }

        y += 16f
        for (row in rows) {
            x = leftMargin
            for (cell in row) {
                canvas.drawText(cell, x, y, paint)
                x += columnWidth
            }
            y += 14f
            if (y > 750f) break
        }

        y += 20f
        paintBold.textSize = 11f
        for ((label, value) in summary) {
            canvas.drawText("$label: $value", leftMargin, y, paintBold)
            y += 16f
        }

        document.finishPage(page)

        val file = File(context.cacheDir, "sales_report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
