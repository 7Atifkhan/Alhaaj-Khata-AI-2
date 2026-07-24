package com.example.services

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.data.local.CustomerEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReportSummaryData(
    val title: String,
    val dateRangeText: String,
    val businessName: String = "Alhaaj Khata AI",
    val ownerName: String = "Valued Business Owner",
    val totalCustomers: Int,
    val activeCustomers: Int,
    val inactiveCustomers: Int,
    val totalTransactions: Int,
    val moneyToReceive: Double,
    val moneyToPay: Double,
    val totalIncome: Double,
    val totalExpenses: Double,
    val netProfit: Double,
    val cashFlow: Double
)

object ReportExportService {

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val dateTimeFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    // 1. Generate Comprehensive Business Report PDF
    fun generateBusinessReportPdf(
        context: Context,
        summary: ReportSummaryData,
        transactions: List<TransactionEntity>,
        customers: List<CustomerEntity>
    ): File {
        val pdfDocument = PdfDocument()

        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points
        var pageNumber = 1

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint()
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerPaint = Paint().apply {
            color = Color.parseColor("#37474F")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#212121")
            textSize = 10f
        }
        val cardBgPaint = Paint().apply {
            color = Color.parseColor("#F5F5F5")
            style = Paint.Style.FILL
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
        }

        var y = 40f

        // Header Title
        canvas.drawText(summary.businessName, 30f, y, titlePaint)
        y += 18f
        canvas.drawText("Business Analytics & Financial Ledger Report", 30f, y, headerPaint)
        y += 15f
        canvas.drawText("Period: ${summary.dateRangeText} | Generated: ${dateTimeFormatter.format(Date())}", 30f, y, bodyPaint)
        canvas.drawText("Owner: ${summary.ownerName}", 350f, y, bodyPaint)
        y += 15f

        canvas.drawLine(30f, y, (pageWidth - 30).toFloat(), y, linePaint)
        y += 20f

        // Executive Summary Cards Grid
        canvas.drawRect(30f, y, (pageWidth - 30).toFloat(), y + 80f, cardBgPaint)

        canvas.drawText("TOTAL RECEIVABLES", 45f, y + 22f, headerPaint)
        paint.color = Color.parseColor("#00897B")
        paint.textSize = 14f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("PKR ${String.format("%.2f", summary.moneyToReceive)}", 45f, y + 42f, paint)

        canvas.drawText("TOTAL PAYABLES", 220f, y + 22f, headerPaint)
        paint.color = Color.parseColor("#E53935")
        canvas.drawText("PKR ${String.format("%.2f", summary.moneyToPay)}", 220f, y + 42f, paint)

        canvas.drawText("NET PROFIT / CASH FLOW", 395f, y + 22f, headerPaint)
        paint.color = if (summary.netProfit >= 0) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
        canvas.drawText("PKR ${String.format("%.2f", summary.netProfit)}", 395f, y + 42f, paint)

        canvas.drawText("Customers: ${summary.totalCustomers} (${summary.activeCustomers} Active) | Entries: ${summary.totalTransactions}", 45f, y + 68f, bodyPaint)
        y += 100f

        // Customer Summary Table
        canvas.drawText("CUSTOMER OVERVIEW SUMMARY", 30f, y, headerPaint)
        y += 15f

        // Table Headers
        canvas.drawRect(30f, y, (pageWidth - 30).toFloat(), y + 20f, cardBgPaint)
        canvas.drawText("Customer Name", 35f, y + 14f, headerPaint)
        canvas.drawText("Phone", 220f, y + 14f, headerPaint)
        canvas.drawText("Status", 340f, y + 14f, headerPaint)
        canvas.drawText("Current Balance", 450f, y + 14f, headerPaint)
        y += 24f

        val topCustomers = customers.take(8)
        for (cust in topCustomers) {
            canvas.drawText(cust.name, 35f, y, bodyPaint)
            canvas.drawText(cust.phone ?: "N/A", 220f, y, bodyPaint)
            canvas.drawText(if (cust.currentBalance != 0.0) "Active" else "Clear", 340f, y, bodyPaint)

            val balStr = "PKR ${String.format("%.2f", cust.currentBalance)}"
            canvas.drawText(balStr, 450f, y, bodyPaint)
            y += 16f
            canvas.drawLine(30f, y - 10f, (pageWidth - 30).toFloat(), y - 10f, linePaint)
        }

        y += 20f

        // Transaction History Table Header
        canvas.drawText("RECENT TRANSACTIONS LEDGER", 30f, y, headerPaint)
        y += 15f

        canvas.drawRect(30f, y, (pageWidth - 30).toFloat(), y + 20f, cardBgPaint)
        canvas.drawText("Date", 35f, y + 14f, headerPaint)
        canvas.drawText("Customer", 110f, y + 14f, headerPaint)
        canvas.drawText("Type", 250f, y + 14f, headerPaint)
        canvas.drawText("Category", 350f, y + 14f, headerPaint)
        canvas.drawText("Amount (PKR)", 450f, y + 14f, headerPaint)
        y += 24f

        for (tx in transactions) {
            if (y > pageHeight - 60) {
                // Footer for current page
                canvas.drawText("Page $pageNumber | Generated by Alhaaj Khata AI", 30f, pageHeight - 30f, bodyPaint)
                pdfDocument.finishPage(page)

                pageNumber++
                val newPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(newPageInfo)
                canvas = page.canvas
                y = 40f

                canvas.drawRect(30f, y, (pageWidth - 30).toFloat(), y + 20f, cardBgPaint)
                canvas.drawText("Date", 35f, y + 14f, headerPaint)
                canvas.drawText("Customer", 110f, y + 14f, headerPaint)
                canvas.drawText("Type", 250f, y + 14f, headerPaint)
                canvas.drawText("Category", 350f, y + 14f, headerPaint)
                canvas.drawText("Amount (PKR)", 450f, y + 14f, headerPaint)
                y += 24f
            }

            val dateStr = dateFormatter.format(Date(tx.date))
            canvas.drawText(dateStr, 35f, y, bodyPaint)
            canvas.drawText(tx.customerName.take(18), 110f, y, bodyPaint)
            canvas.drawText(tx.type.name, 250f, y, bodyPaint)
            canvas.drawText(tx.category.take(12), 350f, y, bodyPaint)

            val amtStr = String.format("%.2f", tx.amount)
            canvas.drawText(amtStr, 450f, y, bodyPaint)

            y += 18f
            canvas.drawLine(30f, y - 12f, (pageWidth - 30).toFloat(), y - 12f, linePaint)
        }

        // Footer
        canvas.drawText("Page $pageNumber | Generated by Alhaaj Khata AI", 30f, pageHeight - 30f, bodyPaint)
        pdfDocument.finishPage(page)

        val cacheDir = File(context.cacheDir, "reports")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val pdfFile = File(cacheDir, "Alhaaj_Khata_Report_${System.currentTimeMillis()}.pdf")

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    // 2. Generate Customer Statement PDF
    fun generateCustomerStatementPdf(
        context: Context,
        customer: CustomerEntity,
        transactions: List<TransactionEntity>
    ): File {
        val pdfDocument = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerPaint = Paint().apply {
            color = Color.parseColor("#37474F")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#212121")
            textSize = 10f
        }
        val cardBgPaint = Paint().apply {
            color = Color.parseColor("#F5F5F5")
            style = Paint.Style.FILL
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
        }

        var y = 40f

        // Header
        canvas.drawText("Alhaaj Khata AI - Customer Statement", 30f, y, titlePaint)
        y += 22f
        canvas.drawText("Statement Date: ${dateTimeFormatter.format(Date())}", 30f, y, bodyPaint)
        y += 18f
        canvas.drawLine(30f, y, (pageWidth - 30).toFloat(), y, linePaint)
        y += 20f

        // Customer Details Box
        canvas.drawRect(30f, y, (pageWidth - 30).toFloat(), y + 70f, cardBgPaint)
        canvas.drawText("CUSTOMER DETAILS", 45f, y + 20f, headerPaint)
        canvas.drawText("Name: ${customer.name}", 45f, y + 38f, bodyPaint)
        canvas.drawText("Phone: ${customer.phone ?: "N/A"}", 45f, y + 54f, bodyPaint)

        canvas.drawText("Opening Balance: PKR ${String.format("%.2f", customer.openingBalance)} (${customer.balanceType})", 300f, y + 38f, bodyPaint)
        val balColor = if (customer.currentBalance >= 0) "#00897B" else "#E53935"
        val balPaint = Paint().apply {
            color = Color.parseColor(balColor)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("Current Balance: PKR ${String.format("%.2f", customer.currentBalance)}", 300f, y + 56f, balPaint)
        y += 90f

        // Transaction Ledger Table
        canvas.drawText("TRANSACTION HISTORY", 30f, y, headerPaint)
        y += 15f

        canvas.drawRect(30f, y, (pageWidth - 30).toFloat(), y + 20f, cardBgPaint)
        canvas.drawText("Date", 35f, y + 14f, headerPaint)
        canvas.drawText("Type", 130f, y + 14f, headerPaint)
        canvas.drawText("Category", 230f, y + 14f, headerPaint)
        canvas.drawText("Particulars / Notes", 330f, y + 14f, headerPaint)
        canvas.drawText("Amount (PKR)", 460f, y + 14f, headerPaint)
        y += 24f

        for (tx in transactions) {
            val dateStr = dateFormatter.format(Date(tx.date))
            canvas.drawText(dateStr, 35f, y, bodyPaint)
            canvas.drawText(tx.type.name, 130f, y, bodyPaint)
            canvas.drawText(tx.category, 230f, y, bodyPaint)
            canvas.drawText((tx.notes ?: "N/A").take(18), 330f, y, bodyPaint)
            canvas.drawText(String.format("%.2f", tx.amount), 460f, y, bodyPaint)

            y += 18f
            canvas.drawLine(30f, y - 12f, (pageWidth - 30).toFloat(), y - 12f, linePaint)
        }

        // Footer
        canvas.drawText("Generated by Alhaaj Khata AI - Verified Digital Khata Ledger", 30f, pageHeight - 30f, bodyPaint)
        pdfDocument.finishPage(page)

        val cacheDir = File(context.cacheDir, "statements")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val pdfFile = File(cacheDir, "Statement_${customer.name.replace(" ", "_")}_${System.currentTimeMillis()}.pdf")

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    // 3. Generate Excel / Multi-Sheet CSV Data
    fun generateExcelWorksheetCsv(
        context: Context,
        summary: ReportSummaryData,
        customers: List<CustomerEntity>,
        transactions: List<TransactionEntity>
    ): File {
        val cacheDir = File(context.cacheDir, "reports")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val excelCsvFile = File(cacheDir, "Alhaaj_Khata_Report_${System.currentTimeMillis()}.xlsx.csv")

        excelCsvFile.printWriter().use { out ->
            out.println("=== SHEET 1: EXECUTIVE SUMMARY ===")
            out.println("Business Name,${summary.businessName}")
            out.println("Owner Name,${summary.ownerName}")
            out.println("Date Range,${summary.dateRangeText}")
            out.println("Generated Date,${dateTimeFormatter.format(Date())}")
            out.println("Total Customers,${summary.totalCustomers}")
            out.println("Active Customers,${summary.activeCustomers}")
            out.println("Inactive Customers,${summary.inactiveCustomers}")
            out.println("Total Transactions,${summary.totalTransactions}")
            out.println("Money to Receive (PKR),${summary.moneyToReceive}")
            out.println("Money to Pay (PKR),${summary.moneyToPay}")
            out.println("Total Income (PKR),${summary.totalIncome}")
            out.println("Total Expenses (PKR),${summary.totalExpenses}")
            out.println("Net Profit (PKR),${summary.netProfit}")
            out.println("Cash Flow (PKR),${summary.cashFlow}")
            out.println()

            out.println("=== SHEET 2: CUSTOMERS ===")
            out.println("Customer ID,Name,Phone,WhatsApp,Opening Balance,Balance Type,Current Balance,Last Transaction")
            for (c in customers) {
                out.println("\"${c.id}\",\"${c.name}\",\"${c.phone ?: ""}\",\"${c.whatsappNumber ?: ""}\",${c.openingBalance},${c.balanceType},${c.currentBalance},${dateFormatter.format(Date(c.lastTransactionDate))}")
            }
            out.println()

            out.println("=== SHEET 3: TRANSACTIONS ===")
            out.println("Transaction ID,Date,Customer ID,Customer Name,Type,Amount,Category,Payment Method,Notes,Running Balance")
            for (t in transactions) {
                val cleanNotes = (t.notes ?: "").replace("\"", "'")
                out.println("\"${t.id}\",\"${dateFormatter.format(Date(t.date))}\",\"${t.customerId}\",\"${t.customerName}\",\"${t.type.name}\",${t.amount},\"${t.category}\",\"${t.paymentMethod}\",\"$cleanNotes\",${t.runningBalance}")
            }
        }

        return excelCsvFile
    }

    // 4. Generate Standard CSV Report
    fun generateCsvReport(
        context: Context,
        transactions: List<TransactionEntity>
    ): File {
        val cacheDir = File(context.cacheDir, "reports")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val csvFile = File(cacheDir, "Transactions_History_${System.currentTimeMillis()}.csv")

        csvFile.printWriter().use { out ->
            out.println("Transaction ID,Date,Customer Name,Type,Amount (PKR),Category,Payment Method,Notes,Running Balance")
            for (t in transactions) {
                val cleanNotes = (t.notes ?: "").replace("\"", "'")
                out.println("\"${t.id}\",\"${dateTimeFormatter.format(Date(t.date))}\",\"${t.customerName}\",\"${t.type.name}\",${t.amount},\"${t.category}\",\"${t.paymentMethod}\",\"$cleanNotes\",${t.runningBalance}")
            }
        }

        return csvFile
    }

    // Share File via System Share Sheet / WhatsApp / Email
    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String = "Share Report") {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Alhaaj Khata AI Business Report")
            putExtra(Intent.EXTRA_TEXT, "Please find attached the financial ledger report generated by Alhaaj Khata AI.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, chooserTitle)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    // Share via WhatsApp
    fun shareToWhatsApp(context: Context, file: File, textMessage: String) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, textMessage)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general chooser if WhatsApp is not installed
            shareFile(context, file, "*/*", "Share via WhatsApp / Apps")
        }
    }

    // Print PDF Document
    fun printPdf(context: Context, pdfFile: File) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Alhaaj Khata Report Print"

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = android.print.PrintDocumentInfo.Builder("Alhaaj_Khata_Report.pdf")
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: android.os.ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    pdfFile.inputStream().use { input ->
                        FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.localizedMessage)
                }
            }
        }

        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }
}
