package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_recents")
data class PdfRecentItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uriString: String,
    val fileName: String,
    val fileSize: Long = 0L,
    val pageCount: Int = 1,
    val lastPageRead: Int = 0,
    val lastOpenedTimestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isSample: Boolean = false,
    val folderName: String = "الرئيسية"
)
