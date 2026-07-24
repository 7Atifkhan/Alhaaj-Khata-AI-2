package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.CustomerEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfStatementGenerator {

    fun generateAndOpenStatement(
        context: Context,
        customer: CustomerEntity,
        businessName: String = "Alhaaj Khata Store"
    ) {
        val file = generatePdfFile(context, customer, businessName)
        if (file != null && file.exists()) {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Open Statement PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    fun generateAndShareStatement(
        context: Context,
        customer: CustomerEntity,
        businessName: String = "Alhaaj Khata Store"
    ) {
        val file = generatePdfFile(context, customer, businessName)
        if (file != null && file.exists()) {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Account Statement - ${customer.name}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Dear ${customer.name},\nAttached is your current account statement from $businessName."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Statement PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    private fun generatePdfFile(
        context: Context,
        customer: CustomerEntity,
        businessName: String
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points (595x842)
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(24, 90, 219)
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val subTitlePaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val bodyPaint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
            }

            val headerPaint = Paint().apply {
                color = Color.rgb(240, 243, 248)
            }

            val borderPaint = Paint().apply {
                color = Color.LTGRAY
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            var y = 50f

            // Top Header Box
            canvas.drawRect(30f, 30f, 565f, 100f, headerPaint)
            canvas.drawRect(30f, 30f, 565f, 100f, borderPaint)

            canvas.drawText(businessName, 45f, 60f, titlePaint)
            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("Digital Ledger Statement | Generated $dateStr", 45f, 85f, bodyPaint)

            y = 130f

            // Customer Info Box
            canvas.drawText("CUSTOMER DETAILS", 30f, y, subTitlePaint)
            y += 20f
            canvas.drawLine(30f, y, 565f, y, borderPaint)
            y += 20f

            canvas.drawText("Name: ${customer.name}", 40f, y, bodyPaint)
            y += 20f
            if (!customer.phone.isNull_or_blank()) {
                canvas.drawText("Phone: ${customer.phone}", 40f, y, bodyPaint)
                y += 20f
            }
            if (!customer.whatsappNumber.isNull_or_blank()) {
                canvas.drawText("WhatsApp: ${customer.whatsappNumber}", 40f, y, bodyPaint)
                y += 20f
            }
            if (!customer.address.isNull_or_blank()) {
                canvas.drawText("Address: ${customer.address}", 40f, y, bodyPaint)
                y += 20f
            }

            y += 15f
            // Account Summary Box
            canvas.drawText("ACCOUNT SUMMARY", 30f, y, subTitlePaint)
            y += 20f
            canvas.drawLine(30f, y, 565f, y, borderPaint)
            y += 25f

            val balanceColor = if (customer.currentBalance > 0) Color.rgb(0, 150, 136) else if (customer.currentBalance < 0) Color.rgb(229, 57, 53) else Color.BLACK
            val balanceText = if (customer.currentBalance > 0) {
                "You Will Get: PKR ${String.format("%.2f", customer.currentBalance)}"
            } else if (customer.currentBalance < 0) {
                "You Will Give: PKR ${String.format("%.2f", Math.abs(customer.currentBalance))}"
            } else {
                "Settled / Zero Balance (0.00)"
            }

            val balancePaint = Paint().apply {
                color = balanceColor
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            canvas.drawText("Opening Balance: PKR ${String.format("%.2f", customer.openingBalance)} (${if (customer.balanceType == "YOU_WILL_GET") "You Will Get" else "You Will Give"})", 40f, y, bodyPaint)
            y += 25f
            canvas.drawText("Net Current Balance: $balanceText", 40f, y, balancePaint)

            y += 30f
            if (!customer.notes.isNull_or_blank()) {
                canvas.drawText("Notes / Terms:", 30f, y, subTitlePaint)
                y += 20f
                canvas.drawText(customer.notes ?: "", 40f, y, bodyPaint)
                y += 30f
            }

            // Footer
            y = 780f
            canvas.drawLine(30f, y, 565f, y, borderPaint)
            y += 20f
            val footerPaint = Paint().apply {
                color = Color.GRAY
                textSize = 10f
            }
            canvas.drawText("Thank you for your business! Powered by Alhaaj Khata AI.", 30f, y, footerPaint)

            pdfDocument.finishPage(page)

            val dir = File(context.cacheDir, "statements").apply { mkdirs() }
            val pdfFile = File(dir, "Statement_${customer.name.replace(" ", "_")}_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
}
