package com.example.data

import kotlinx.coroutines.flow.Flow

class PdfRepository(private val pdfDao: PdfDao) {
    val allRecents: Flow<List<PdfRecentItem>> = pdfDao.getAllRecents()
    val favorites: Flow<List<PdfRecentItem>> = pdfDao.getFavorites()
    val allFolders: Flow<List<PdfFolder>> = pdfDao.getAllFolders()

    suspend fun getByUri(uriString: String): PdfRecentItem? {
        return pdfDao.getByUri(uriString)
    }

    suspend fun insertOrUpdate(item: PdfRecentItem): Long {
        return pdfDao.insertOrUpdate(item)
    }

    suspend fun renameFile(id: Long, newName: String) {
        val sanitized = if (newName.endsWith(".pdf", ignoreCase = true)) newName else "$newName.pdf"
        pdfDao.updateFileName(id, sanitized)
    }

    suspend fun moveFileToFolder(id: Long, folderName: String) {
        pdfDao.updateFileFolder(id, folderName)
    }

    suspend fun updateProgress(uriString: String, page: Int) {
        pdfDao.updateProgress(uriString, page)
    }

    suspend fun toggleFavorite(id: Long, currentStatus: Boolean) {
        pdfDao.updateFavorite(id, !currentStatus)
    }

    suspend fun deleteById(id: Long) {
        pdfDao.deleteById(id)
    }

    suspend fun clearAll() {
        pdfDao.clearAll()
    }

    suspend fun insertFolder(name: String, colorHex: String = "#D32F2F") {
        pdfDao.insertFolder(PdfFolder(name = name.trim(), colorHex = colorHex))
    }

    suspend fun deleteFolder(id: Long, folderName: String) {
        // Move files inside this folder back to default folder "الرئيسية"
        pdfDao.updateFolderForFiles(folderName, "الرئيسية")
        pdfDao.deleteFolder(id)
    }
}
