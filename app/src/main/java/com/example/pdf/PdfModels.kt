package com.example.pdf

import android.graphics.PointF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.File

/**
 * Data structures for PDF viewing, editing, and annotations.
 */

data class PageAnnotationState(
    val pageIndex: Int,
    val paths: MutableList<AnnotatedStroke> = mutableListOf(),
    val textNotes: MutableList<TextAnnotation> = mutableListOf(),
    var rotation: Int = 0 // 0, 90, 180, 270
)

data class AnnotatedStroke(
    val points: List<Offset>,
    val color: Long, // Color.value
    val strokeWidth: Float,
    val isHighlighter: Boolean = false
)

data class TextAnnotation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val xPercent: Float, // Normalized 0..1 for resolution independence
    val yPercent: Float,
    val color: Long,
    val fontSizeSp: Float = 16f,
    val isStamp: Boolean = false
)

data class PageItem(
    val pageNumber: Int, // 1-based display number
    val originalIndex: Int, // index in the source PDF
    val rotation: Int = 0,
    val isDeleted: Boolean = false
)

enum class ReadingMode {
    NORMAL, // Standard crisp white
    NIGHT,  // Inverted dark mode
    SEPIA   // Warm eye-comfort paper
}

enum class ToolMode {
    VIEW,         // Pan & Zoom
    PEN,          // Draw freehand
    HIGHLIGHTER,  // Transparent marker
    TEXT,         // Add text note / stamp
    ERASER        // Erase strokes
}

data class PdfDocumentInfo(
    val file: File,
    val fileName: String,
    val fileSize: Long,
    val originalUriString: String,
    val pageCount: Int,
    val isSample: Boolean = false
)
