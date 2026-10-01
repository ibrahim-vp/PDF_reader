package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM pdf_recents ORDER BY lastOpenedTimestamp DESC")
    fun getAllRecents(): Flow<List<PdfRecentItem>>

    @Query("SELECT * FROM pdf_recents WHERE isFavorite = 1 ORDER BY lastOpenedTimestamp DESC")
    fun getFavorites(): Flow<List<PdfRecentItem>>

    @Query("SELECT * FROM pdf_recents WHERE folderName = :folder ORDER BY lastOpenedTimestamp DESC")
    fun getByFolder(folder: String): Flow<List<PdfRecentItem>>

    @Query("SELECT * FROM pdf_recents WHERE fileName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%' ORDER BY lastOpenedTimestamp DESC")
    fun searchFiles(query: String): Flow<List<PdfRecentItem>>

    @Query("SELECT * FROM pdf_recents WHERE uriString = :uriString LIMIT 1")
    suspend fun getByUri(uriString: String): PdfRecentItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: PdfRecentItem): Long

    @Update
    suspend fun update(item: PdfRecentItem)

    @Query("UPDATE pdf_recents SET fileName = :newName WHERE id = :id")
    suspend fun updateFileName(id: Long, newName: String)

    @Query("UPDATE pdf_recents SET folderName = :folderName WHERE id = :id")
    suspend fun updateFileFolder(id: Long, folderName: String)

    @Query("UPDATE pdf_recents SET lastPageRead = :page, lastOpenedTimestamp = :timestamp WHERE uriString = :uriString")
    suspend fun updateProgress(uriString: String, page: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE pdf_recents SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM pdf_recents WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pdf_recents")
    suspend fun clearAll()

    // Folder Operations
    @Query("SELECT * FROM pdf_folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<PdfFolder>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolder(folder: PdfFolder): Long

    @Query("DELETE FROM pdf_folders WHERE id = :id")
    suspend fun deleteFolder(id: Long)

    @Query("UPDATE pdf_recents SET folderName = :newFolder WHERE folderName = :oldFolder")
    suspend fun updateFolderForFiles(oldFolder: String, newFolder: String)
}
