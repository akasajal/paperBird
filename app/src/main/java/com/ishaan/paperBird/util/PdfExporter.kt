package com.ishaan.paperBird.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.ishaan.paperBird.domain.model.Letter
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private const val PAGE_WIDTH = 595 // A4 width in points
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 50f

    fun exportLettersToPdf(letters: List<Letter>, categoryColors: Map<String, Long>): ByteArray {
        val document = PdfDocument()
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())

        // Paints
        val titlePaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val metaPaint = TextPaint().apply {
            color = Color.GRAY
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val bodyPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val badgePaint = Paint().apply {
            style = Paint.Style.FILL
        }

        letters.forEach { letter ->
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            var currentY = MARGIN

            // Draw Title
            val titleText = letter.title.ifBlank { "(Untitled)" }
            val titleLayout = StaticLayout.Builder.obtain(titleText, 0, titleText.length, titlePaint, PAGE_WIDTH - (MARGIN * 2).toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            
            canvas.save()
            canvas.translate(MARGIN, currentY)
            titleLayout.draw(canvas)
            canvas.restore()
            currentY += titleLayout.height + 10f

            // Draw Category Badge & Date
            val categoryColor = categoryColors[letter.category] ?: 0xFF9A9A9A
            badgePaint.color = categoryColor.toInt()
            
            canvas.drawCircle(MARGIN + 6f, currentY + 8f, 6f, badgePaint)
            
            val metaText = "${letter.category}  •  ${dateFormat.format(Date(letter.updatedAt))}"
            canvas.drawText(metaText, MARGIN + 20f, currentY + 12f, metaPaint)
            currentY += 40f

            // Draw Body with pagination
            val bodyLayout = StaticLayout.Builder.obtain(letter.body, 0, letter.body.length, bodyPaint, PAGE_WIDTH - (MARGIN * 2).toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()

            var line = 0
            while (line < bodyLayout.lineCount) {
                val lineBottom = bodyLayout.getLineBottom(line)
                val lineTop = bodyLayout.getLineTop(line)
                val lineHeight = lineBottom - lineTop
                
                if (currentY + lineHeight > PAGE_HEIGHT - MARGIN) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = MARGIN
                }
                
                canvas.save()
                // Translate so the current line is drawn at currentY
                canvas.translate(MARGIN, currentY - lineTop)
                // Clip to only draw this line
                canvas.clipRect(0f, lineTop.toFloat(), PAGE_WIDTH.toFloat(), lineBottom.toFloat())
                bodyLayout.draw(canvas)
                canvas.restore()
                
                currentY += lineHeight
                line++
            }
            document.finishPage(page)
        }

        val outputStream = ByteArrayOutputStream()
        document.writeTo(outputStream)
        document.close()
        return outputStream.toByteArray()
    }
}
