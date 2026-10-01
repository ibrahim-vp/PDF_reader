package com.example

import com.example.data.PdfFolder
import com.example.data.PdfRecentItem
import com.example.pdf.AnnotatedStroke
import com.example.pdf.PageAnnotationState
import com.example.pdf.PageItem
import com.example.pdf.ReadingMode
import com.example.pdf.ToolMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfUnitTest {

    @Test
    fun testPdfRecentItemCreation() {
        val item = PdfRecentItem(
            uriString = "content://com.whatsapp.provider.media/item/123",
            fileName = "invoice_2026.pdf",
            fileSize = 102400L,
            pageCount = 5,
            lastPageRead = 2,
            isFavorite = true,
            folderName = "الفواتير"
        )

        assertEquals("invoice_2026.pdf", item.fileName)
        assertEquals(5, item.pageCount)
        assertEquals(2, item.lastPageRead)
        assertTrue(item.isFavorite)
        assertEquals("الفواتير", item.folderName)
    }

    @Test
    fun testPdfFolderCreation() {
        val folder = PdfFolder(name = "العمل", colorHex = "#1976D2")
        assertEquals("العمل", folder.name)
        assertEquals("#1976D2", folder.colorHex)
    }

    @Test
    fun testMergeCalculations() {
        val item1 = PdfRecentItem(uriString = "file:///doc1.pdf", fileName = "doc1.pdf", pageCount = 3)
        val item2 = PdfRecentItem(uriString = "file:///doc2.pdf", fileName = "doc2.pdf", pageCount = 4)
        val list = listOf(item1, item2)

        val totalPages = list.sumOf { it.pageCount }
        assertEquals(7, totalPages)
        assertEquals(2, list.size)
    }

    @Test
    fun testSplitPageIndices() {
        val totalPages = 10
        val selectedIndices = listOf(0, 2, 4, 6) // Pages 1, 3, 5, 7
        assertEquals(4, selectedIndices.size)
        assertTrue(selectedIndices.all { it in 0 until totalPages })
    }

    @Test
    fun testPageAnnotationState() {
        val state = PageAnnotationState(pageIndex = 0)
        assertEquals(0, state.paths.size)
        assertEquals(0, state.rotation)

        state.rotation = 90
        assertEquals(90, state.rotation)
    }

    @Test
    fun testPageItemRotation() {
        val page = PageItem(pageNumber = 1, originalIndex = 0, rotation = 0)
        val rotated = page.copy(rotation = (page.rotation + 90) % 360)
        assertEquals(90, rotated.rotation)
        assertFalse(rotated.isDeleted)
    }

    @Test
    fun testToolModes() {
        assertNotNull(ToolMode.VIEW)
        assertNotNull(ToolMode.PEN)
        assertNotNull(ToolMode.HIGHLIGHTER)
        assertNotNull(ToolMode.TEXT)
        assertNotNull(ToolMode.ERASER)
        assertNotNull(ReadingMode.NIGHT)
        assertNotNull(ReadingMode.SEPIA)
    }
}
