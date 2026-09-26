package com.antonbasson.productcatalog.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.antonbasson.productcatalog.data.ProductEntity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {
    fun exportCatalogue(
        context: Context,
        categoryName: String,
        products: List<ProductEntity>
    ): Uri? {
        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        val canvas = page.canvas
        var y = pageHeight - 60f

        val titlePaint = Paint().apply {
            textSize = 24f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            textSize = 12f
        }
        val smallPaint = Paint().apply {
            textSize = 10f
        }

        canvas.drawText("$categoryName Product Catalogue", 40f, y, titlePaint)
        y -= 34f
        canvas.drawText("Generated: ${SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date())}", 40f, y, smallPaint)
        y -= 36f

        for (product in products) {
            if (y < 170f) {
                pdf.finishPage(page)
                pageNumber += 1
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas.setBitmap(page.canvas.nativeCanvas) // no-op placeholder
                y = pageHeight - 60f
            }

            val bitmap = product.imagePath?.let { decodeBitmap(it) }
            if (bitmap != null) {
                val scaled = Bitmap.createScaledBitmap(bitmap, 120, 120, false)
                canvas.drawBitmap(scaled, 40f, y - 120f, null)
            }

            canvas.drawText(product.description, 180f, y - 6f, bodyPaint)
            canvas.drawText("Code: ${product.code}", 180f, y - 26f, bodyPaint)
            canvas.drawText("Barcode: ${product.barcode}", 180f, y - 46f, bodyPaint)
            canvas.drawText("Selling: ${PriceUtils.formatCurrency(product.sellingPrice)}", 180f, y - 66f, bodyPaint)
            canvas.drawText("Cost: ${PriceUtils.formatCurrency(product.costPrice)}", 180f, y - 86f, bodyPaint)
            y -= 140f

            if (y < 100f) {
                pdf.finishPage(page)
                pageNumber += 1
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                y = pageHeight - 60f
            }
        }

        pdf.finishPage(page)

        val directory = File(context.getExternalFilesDir(null), "catalogues")
        if (!directory.exists()) directory.mkdirs()

        val file = File(directory, "${categoryName.replace("/", "_").replace(" ", "_").lowercase()}_catalogue.pdf")
        return try {
            FileOutputStream(file).use { outputStream ->
                pdf.writeTo(outputStream)
            }
            pdf.close()
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: IOException) {
            pdf.close()
            null
        }
    }

    private fun decodeBitmap(path: String): Bitmap? {
        val file = File(path)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }
}
