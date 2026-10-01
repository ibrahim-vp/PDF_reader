package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

object PdfEngine {

    /**
     * Resolves display name of a Uri
     */
    fun getFileName(context: Context, uri: Uri): String {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (_: Exception) {}
        }
        if (name.isNullOrBlank()) {
            name = uri.lastPathSegment?.substringAfterLast('/')
        }
        if (name.isNullOrBlank() || !name!!.endsWith(".pdf", ignoreCase = true)) {
            name = if (name.isNullOrBlank()) "document_${System.currentTimeMillis()}.pdf" else "$name.pdf"
        }
        return name!!
    }

    /**
     * Copies a Uri stream into a local cache file for seekable PDF access.
     */
    suspend fun copyUriToCache(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        val folder = File(context.cacheDir, "opened_pdfs").apply { mkdirs() }
        val fileName = getFileName(context, uri)
        val sanitizedName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val destFile = File(folder, "${System.currentTimeMillis()}_$sanitizedName")

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Could not open input stream for Uri: $uri")

        destFile
    }

    /**
     * Returns total page count of a PDF file safely.
     */
    suspend fun getPdfPageCount(file: File): Int = withContext(Dispatchers.IO) {
        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        } catch (e: Exception) {
            1
        }
    }

    /**
     * Renders a specific page into a Bitmap.
     */
    suspend fun renderPageBitmap(
        file: File,
        pageIndex: Int,
        targetWidth: Int = 1200
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
                    renderer.openPage(pageIndex).use { page ->
                        val ratio = page.height.toFloat() / page.width.toFloat()
                        val width = targetWidth
                        val height = (targetWidth * ratio).toInt().coerceAtLeast(100)

                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(android.graphics.Color.WHITE)

                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generates a rich, interactive sample PDF document to try all viewer and editing features.
     */
    suspend fun createSamplePdf(context: Context): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "sample_guide.pdf")
        if (file.exists() && file.length() > 1000) {
            return@withContext file
        }

        val pdfDoc = PdfDocument()

        // Page 1: Cover & Feature Overview
        val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4
        val page1 = pdfDoc.startPage(pageInfo1)
        val canvas1 = page1.canvas

        // Header Background Banner (Red)
        val paint = Paint().apply { isAntiAlias = true }
        paint.color = android.graphics.Color.parseColor("#D32F2F")
        canvas1.drawRect(0f, 0f, 595f, 150f, paint)

        // Banner Title
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas1.drawText("PDF Reader & Editor", 40f, 65f, paint)

        paint.textSize = 15f
        paint.isFakeBoldText = false
        canvas1.drawText("تطبيق فتح وتعديل مستندات PDF بدون إعلانات", 40f, 95f, paint)
        canvas1.drawText("Ad-Free • Fast • Annotations • WhatsApp Support", 40f, 120f, paint)

        // Content
        paint.color = android.graphics.Color.parseColor("#263238")
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas1.drawText("مرحباً بك في تطبيقك المتكامل لملفات PDF!", 40f, 195f, paint)

        paint.color = android.graphics.Color.parseColor("#455A64")
        paint.textSize = 13f
        paint.isFakeBoldText = false

        val lines = listOf(
            "• الفتح المباشر من واتساب: عند استلام أي ملف PDF في واتساب أو أي تطبيق آخر،",
            "  يمكنك اختياره لفتحه مباشرة وبسهولة في هذا التطبيق.",
            "• أدوات التعديل والرسم: يمكنك التظليل بالقلم المضيء أو الرسم وكتابة الملاحظات.",
            "• إدارة الصفحات: تدوير الصفحات (90 درجة)، إعادة ترتيبها، أو حذف أي صفحة.",
            "• تصدير وحفظ: حفظ النسخة المعدلة كملف PDF جديد ومشاركته عبر واتساب بنقرة واحدة.",
            "• أوضاع قراءة مريحة: وضع ليلي مريح للعين أثناء القراءة في الظلام ووضع دافئ.",
            "• تجربة نظيفة 100%: بدون أي إعلانات مزعجة وبدون أي اشتراكات."
        )

        var y = 230f
        for (line in lines) {
            canvas1.drawText(line, 40f, y, paint)
            y += 24f
        }

        // Feature Highlight Cards
        val cardPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#FFEBEE")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas1.drawRoundRect(RectF(40f, 430f, 555f, 530f), 12f, 12f, cardPaint)

        paint.color = android.graphics.Color.parseColor("#B71C1C")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas1.drawText("جرب الآن أدوات التحرير أعلى الشاشة:", 60f, 465f, paint)
        paint.color = android.graphics.Color.parseColor("#37474F")
        paint.isFakeBoldText = false
        paint.textSize = 12f
        canvas1.drawText("1. انقر على أيقونة القلم (تحرير ورسم) للتظليل أو التوقيع على هذا المستند.", 60f, 490f, paint)
        canvas1.drawText("2. انقر على أيقونة التدوير أو إدارة الصفحات لتعديل الترتيب.", 60f, 510f, paint)

        // Footer
        paint.color = android.graphics.Color.parseColor("#9E9E9E")
        paint.textSize = 11f
        canvas1.drawText("الصفحة 1 من 3 • مستند تجريبي", 40f, 800f, paint)

        pdfDoc.finishPage(page1)

        // Page 2: Sample Report & Table
        val pageInfo2 = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        val page2 = pdfDoc.startPage(pageInfo2)
        val canvas2 = page2.canvas

        paint.color = android.graphics.Color.parseColor("#37474F")
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas2.drawText("تقرير إحصائي وعينات جدولية", 40f, 60f, paint)

        paint.textSize = 13f
        paint.isFakeBoldText = false
        paint.color = android.graphics.Color.parseColor("#546E7A")
        canvas2.drawText("نموذج لبيانات مستند تجاري لاختبار وضوح الخطوط وجودة العرض والتكبير", 40f, 85f, paint)

        // Table Header
        val headerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E0E0E0")
            style = Paint.Style.FILL
        }
        canvas2.drawRect(40f, 120f, 555f, 155f, headerPaint)

        paint.color = android.graphics.Color.BLACK
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas2.drawText("البيان", 60f, 142f, paint)
        canvas2.drawText("الكمية", 220f, 142f, paint)
        canvas2.drawText("السعر", 340f, 142f, paint)
        canvas2.drawText("الحالة", 450f, 142f, paint)

        // Table Rows
        val rowData = listOf(
            Triple("تراخيص المستندات", "25", "معتمد"),
            Triple("تقارير المراجعة الشهرية", "12", "مكتمل"),
            Triple("نماذج العقود والمذكرات", "40", "قيد التدقيق"),
            Triple("ملفات التدريب التقني", "8", "جاهز")
        )

        var rowY = 185f
        paint.isFakeBoldText = false
        val linePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#EEEEEE")
            strokeWidth = 1f
        }

        for ((item, qty, status) in rowData) {
            canvas2.drawText(item, 60f, rowY, paint)
            canvas2.drawText(qty, 220f, rowY, paint)
            canvas2.drawText("$1,200", 340f, rowY, paint)
            canvas2.drawText(status, 450f, rowY, paint)
            canvas2.drawLine(40f, rowY + 12f, 555f, rowY + 12f, linePaint)
            rowY += 38f
        }

        // Note Box
        cardPaint.color = android.graphics.Color.parseColor("#E8F5E9")
        canvas2.drawRoundRect(RectF(40f, 400f, 555f, 480f), 8f, 8f, cardPaint)
        paint.color = android.graphics.Color.parseColor("#2E7D32")
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas2.drawText("ملاحظة هامة:", 60f, 430f, paint)
        paint.isFakeBoldText = false
        canvas2.drawText("يمكنك تكبير الصفحة باستخدام أصابعك (Pinch to Zoom) لقراءة أصغر التفاصيل بوضوح تام.", 60f, 455f, paint)

        paint.color = android.graphics.Color.parseColor("#9E9E9E")
        paint.textSize = 11f
        canvas2.drawText("الصفحة 2 من 3 • مستند تجريبي", 40f, 800f, paint)

        pdfDoc.finishPage(page2)

        // Page 3: Signature & Contract Form
        val pageInfo3 = PdfDocument.PageInfo.Builder(595, 842, 3).create()
        val page3 = pdfDoc.startPage(pageInfo3)
        val canvas3 = page3.canvas

        paint.color = android.graphics.Color.parseColor("#D32F2F")
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas3.drawText("نموذج اعتماد وتوقيع", 40f, 60f, paint)

        paint.color = android.graphics.Color.parseColor("#455A64")
        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas3.drawText("يمكنك استخدام قلم الرسم أدناه للتوقيع أو كتابة أي ملاحظة وتصدير الملف المعدل.", 40f, 85f, paint)

        // Signature Box
        val boxBorder = Paint().apply {
            color = android.graphics.Color.parseColor("#B0BEC5")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas3.drawRoundRect(RectF(40f, 150f, 555f, 320f), 12f, 12f, boxBorder)

        paint.color = android.graphics.Color.parseColor("#78909C")
        paint.textSize = 14f
        canvas3.drawText("مساحة التوقيع والملاحظات (Sign Here)", 180f, 240f, paint)

        // Stamp Box
        canvas3.drawRoundRect(RectF(40f, 380f, 260f, 480f), 8f, 8f, boxBorder)
        paint.color = android.graphics.Color.parseColor("#90A4AE")
        paint.textSize = 12f
        canvas3.drawText("مكان الختم / الاعتماد", 90f, 435f, paint)

        paint.color = android.graphics.Color.parseColor("#9E9E9E")
        paint.textSize = 11f
        canvas3.drawText("الصفحة 3 من 3 • مستند تجريبي", 40f, 800f, paint)

        pdfDoc.finishPage(page3)

        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()

        file
    }

    /**
     * Creates a new blank PDF document for notes or sketching.
     */
    suspend fun createBlankPdf(context: Context, pageCount: Int = 1, title: String = "New Note"): File = withContext(Dispatchers.IO) {
        val folder = File(context.cacheDir, "created_pdfs").apply { mkdirs() }
        val file = File(folder, "${System.currentTimeMillis()}_${title.replace(" ", "_")}.pdf")
        val pdfDoc = PdfDocument()

        for (i in 1..pageCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, i).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(android.graphics.Color.WHITE)

            // Optional subtle header
            val paint = Paint().apply {
                color = android.graphics.Color.parseColor("#ECEFF1")
                isAntiAlias = true
            }
            canvas.drawRect(0f, 0f, 595f, 60f, paint)

            paint.color = android.graphics.Color.parseColor("#78909C")
            paint.textSize = 14f
            canvas.drawText("$title - Page $i", 30f, 38f, paint)

            pdfDoc.finishPage(page)
        }

        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        file
    }

    /**
     * Exports a modified PDF applying all annotations, rotations, reordering, and deleted pages!
     */
    suspend fun exportModifiedPdf(
        context: Context,
        sourceFile: File,
        pages: List<PageItem>,
        annotations: Map<Int, PageAnnotationState>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val activePages = pages.filter { !it.isDeleted }
            if (activePages.isEmpty()) return@withContext false

            val pdfDoc = PdfDocument()

            ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    for ((newIndex, pageItem) in activePages.withIndex()) {
                        val origIndex = pageItem.originalIndex
                        if (origIndex < 0 || origIndex >= renderer.pageCount) continue

                        renderer.openPage(origIndex).use { rawPage ->
                            val origW = rawPage.width
                            val origH = rawPage.height

                            val totalRotation = (pageItem.rotation + (annotations[origIndex]?.rotation ?: 0)) % 360
                            val isFlipped = totalRotation == 90 || totalRotation == 270
                            val targetW = if (isFlipped) origH else origW
                            val targetH = if (isFlipped) origW else origH

                            val pageInfo = PdfDocument.PageInfo.Builder(targetW, targetH, newIndex + 1).create()
                            val newPage = pdfDoc.startPage(pageInfo)
                            val canvas = newPage.canvas

                            // Render base original page into a bitmap
                            val baseBmp = Bitmap.createBitmap(origW, origH, Bitmap.Config.ARGB_8888)
                            val baseCanvas = Canvas(baseBmp)
                            baseCanvas.drawColor(android.graphics.Color.WHITE)
                            rawPage.render(baseBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            // Apply rotation transform
                            canvas.save()
                            when (totalRotation) {
                                90 -> {
                                    canvas.translate(targetW.toFloat(), 0f)
                                    canvas.rotate(90f)
                                }
                                180 -> {
                                    canvas.translate(targetW.toFloat(), targetH.toFloat())
                                    canvas.rotate(180f)
                                }
                                270 -> {
                                    canvas.translate(0f, targetH.toFloat())
                                    canvas.rotate(270f)
                                }
                            }
                            canvas.drawBitmap(baseBmp, 0f, 0f, null)

                            // Render annotations (strokes, highlighter, text)
                            val annoState = annotations[origIndex]
                            if (annoState != null) {
                                val strokePaint = Paint().apply {
                                    isAntiAlias = true
                                    style = Paint.Style.STROKE
                                    strokeCap = Paint.Cap.ROUND
                                    strokeJoin = Paint.Join.ROUND
                                }

                                for (stroke in annoState.paths) {
                                    if (stroke.points.size < 2) continue
                                    strokePaint.color = stroke.color.toInt()
                                    strokePaint.strokeWidth = stroke.strokeWidth
                                    if (stroke.isHighlighter) {
                                        strokePaint.alpha = 90
                                    } else {
                                        strokePaint.alpha = 255
                                    }

                                    val androidPath = android.graphics.Path()
                                    val first = stroke.points[0]
                                    androidPath.moveTo(first.x, first.y)
                                    for (k in 1 until stroke.points.size) {
                                        val pt = stroke.points[k]
                                        androidPath.lineTo(pt.x, pt.y)
                                    }
                                    canvas.drawPath(androidPath, strokePaint)
                                }

                                // Render Text Notes & Stamps
                                val textPaint = Paint().apply {
                                    isAntiAlias = true
                                }
                                for (note in annoState.textNotes) {
                                    val nx = note.xPercent * origW
                                    val ny = note.yPercent * origH
                                    if (note.isStamp) {
                                        // Stamp style: filled rounded box
                                        val stampBg = Paint().apply {
                                            color = note.color.toInt()
                                            alpha = 40
                                            style = Paint.Style.FILL
                                        }
                                        val stampBorder = Paint().apply {
                                            color = note.color.toInt()
                                            style = Paint.Style.STROKE
                                            strokeWidth = 2f
                                        }
                                        textPaint.color = note.color.toInt()
                                        textPaint.textSize = note.fontSizeSp * 1.5f
                                        textPaint.isFakeBoldText = true

                                        val textW = textPaint.measureText(note.text)
                                        val textH = note.fontSizeSp * 1.8f
                                        val rect = RectF(nx - 12f, ny - textH, nx + textW + 12f, ny + 10f)
                                        canvas.drawRoundRect(rect, 6f, 6f, stampBg)
                                        canvas.drawRoundRect(rect, 6f, 6f, stampBorder)
                                        canvas.drawText(note.text, nx, ny, textPaint)
                                    } else {
                                        textPaint.color = note.color.toInt()
                                        textPaint.textSize = note.fontSizeSp * 1.3f
                                        textPaint.isFakeBoldText = false
                                        canvas.drawText(note.text, nx, ny, textPaint)
                                    }
                                }
                            }

                            canvas.restore()
                            baseBmp.recycle()
                            pdfDoc.finishPage(newPage)
                        }
                    }
                }
            }

            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Shares a PDF file using Android Intent and FileProvider.
     */
    fun sharePdfFile(context: Context, file: File, title: String = "Share PDF") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Merges multiple PDF files into a single destination PDF file.
     */
    suspend fun mergePdfFiles(
        context: Context,
        sourceFiles: List<File>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        if (sourceFiles.isEmpty()) return@withContext false
        try {
            val pdfDoc = PdfDocument()
            var globalPageNumber = 1

            for (file in sourceFiles) {
                if (!file.exists() || file.length() == 0L) continue
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        for (pageIndex in 0 until renderer.pageCount) {
                            renderer.openPage(pageIndex).use { page ->
                                val origW = page.width
                                val origH = page.height

                                val pageInfo = PdfDocument.PageInfo.Builder(origW, origH, globalPageNumber).create()
                                val newPage = pdfDoc.startPage(pageInfo)
                                val canvas = newPage.canvas

                                val bmp = Bitmap.createBitmap(origW, origH, Bitmap.Config.ARGB_8888)
                                val baseCanvas = Canvas(bmp)
                                baseCanvas.drawColor(android.graphics.Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                canvas.drawBitmap(bmp, 0f, 0f, null)
                                bmp.recycle()
                                pdfDoc.finishPage(newPage)
                                globalPageNumber++
                            }
                        }
                    }
                }
            }

            if (globalPageNumber == 1) {
                pdfDoc.close()
                return@withContext false
            }

            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Converts multiple image Uris into a clean, paginated PDF document.
     */
    suspend fun convertImagesToPdf(
        context: Context,
        imageUris: List<Uri>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        if (imageUris.isEmpty()) return@withContext false
        try {
            val pdfDoc = PdfDocument()
            var pageNum = 1

            for (uri in imageUris) {
                val bitmap: Bitmap? = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
                if (bitmap == null) continue

                // Standard A4 dimensions (595 x 842 pt)
                val a4Width = 595
                val a4Height = 842

                val pageInfo = PdfDocument.PageInfo.Builder(a4Width, a4Height, pageNum).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawColor(android.graphics.Color.WHITE)

                val maxW = a4Width - 40f
                val maxH = a4Height - 40f
                val scale = minOf(maxW / bitmap.width.toFloat(), maxH / bitmap.height.toFloat())

                val drawW = bitmap.width * scale
                val drawH = bitmap.height * scale
                val left = (a4Width - drawW) / 2f
                val top = (a4Height - drawH) / 2f

                val destRect = RectF(left, top, left + drawW, top + drawH)
                canvas.drawBitmap(bitmap, null, destRect, null)
                bitmap.recycle()

                pdfDoc.finishPage(page)
                pageNum++
            }

            if (pageNum == 1) {
                pdfDoc.close()
                return@withContext false
            }

            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Extracts specific pages from a PDF file into a new PDF document.
     */
    suspend fun extractPagesToPdf(
        context: Context,
        sourceFile: File,
        pageIndices: List<Int>, // 0-based
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        if (pageIndices.isEmpty() || !sourceFile.exists()) return@withContext false
        try {
            val pdfDoc = PdfDocument()
            var newPageNum = 1

            ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    for (index in pageIndices) {
                        if (index < 0 || index >= renderer.pageCount) continue
                        renderer.openPage(index).use { page ->
                            val origW = page.width
                            val origH = page.height

                            val pageInfo = PdfDocument.PageInfo.Builder(origW, origH, newPageNum).create()
                            val newPage = pdfDoc.startPage(pageInfo)
                            val canvas = newPage.canvas

                            val bmp = Bitmap.createBitmap(origW, origH, Bitmap.Config.ARGB_8888)
                            val baseCanvas = Canvas(bmp)
                            baseCanvas.drawColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            canvas.drawBitmap(bmp, 0f, 0f, null)
                            bmp.recycle()
                            pdfDoc.finishPage(newPage)
                            newPageNum++
                        }
                    }
                }
            }

            if (newPageNum == 1) {
                pdfDoc.close()
                return@withContext false
            }

            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Prints a PDF file using the Android PrintManager framework.
     */
    fun printPdf(context: Context, file: File) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(file.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    FileInputStream(file).use { input ->
                        FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(file.name, printAdapter, PrintAttributes.Builder().build())
    }
}
