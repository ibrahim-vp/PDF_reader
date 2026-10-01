package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PdfFolder
import com.example.data.PdfRecentItem
import com.example.data.PdfRepository
import com.example.pdf.AnnotatedStroke
import com.example.pdf.PageAnnotationState
import com.example.pdf.PageItem
import com.example.pdf.PdfDocumentInfo
import com.example.pdf.PdfEngine
import com.example.pdf.ReadingMode
import com.example.pdf.TextAnnotation
import com.example.pdf.ToolMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PdfViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PdfRepository
    private val prefs = application.getSharedPreferences("pdf_app_prefs", Context.MODE_PRIVATE)

    // Dark Mode Theme State (persisted in SharedPreferences)
    private val _isDarkMode = MutableStateFlow<Boolean?>(
        if (prefs.contains("dark_mode_enabled")) prefs.getBoolean("dark_mode_enabled", false) else null
    )
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    // Search and Folder Filtering
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null) // null = All files
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PdfRepository(database.pdfDao())

        // Initialize default folders if needed
        viewModelScope.launch {
            repository.allFolders.collect { folders ->
                if (folders.isEmpty()) {
                    repository.insertFolder("العمل", "#1976D2")
                    repository.insertFolder("الفواتير", "#388E3C")
                    repository.insertFolder("شخصي", "#7B1FA2")
                    repository.insertFolder("دراسة", "#F57C00")
                }
            }
        }
    }

    val recentsList: StateFlow<List<PdfRecentItem>> = repository.allRecents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritesList: StateFlow<List<PdfRecentItem>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val foldersList: StateFlow<List<PdfFolder>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered lists reacting to Search Query and Selected Folder
    val filteredRecents: StateFlow<List<PdfRecentItem>> = combine(
        recentsList,
        _searchQuery,
        _selectedFolder
    ) { list, query, folder ->
        filterItems(list, query, folder)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredFavorites: StateFlow<List<PdfRecentItem>> = combine(
        favoritesList,
        _searchQuery,
        _selectedFolder
    ) { list, query, folder ->
        filterItems(list, query, folder)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun filterItems(list: List<PdfRecentItem>, query: String, folder: String?): List<PdfRecentItem> {
        var result = list
        if (!folder.isNullOrBlank()) {
            result = result.filter { it.folderName == folder }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.fileName.lowercase().contains(q) || it.folderName.lowercase().contains(q)
            }
        }
        return result
    }

    fun toggleDarkMode(currentSystemDark: Boolean) {
        val next = when (_isDarkMode.value) {
            null -> !currentSystemDark
            true -> false
            false -> true
        }
        _isDarkMode.value = next
        prefs.edit().putBoolean("dark_mode_enabled", next).apply()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFolder(folderName: String?) {
        _selectedFolder.value = folderName
    }

    fun createFolder(name: String, colorHex: String = "#D32F2F") {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.insertFolder(name.trim(), colorHex)
            }
        }
    }

    fun deleteFolder(folder: PdfFolder) {
        viewModelScope.launch {
            repository.deleteFolder(folder.id, folder.name)
            if (_selectedFolder.value == folder.name) {
                _selectedFolder.value = null
            }
        }
    }

    fun renameFile(id: Long, newName: String) {
        viewModelScope.launch {
            if (newName.isNotBlank()) {
                repository.renameFile(id, newName.trim())
            }
        }
    }

    fun moveFileToFolder(id: Long, folderName: String) {
        viewModelScope.launch {
            repository.moveFileToFolder(id, folderName)
        }
    }

    fun deleteFile(id: Long, uriString: String) {
        viewModelScope.launch {
            repository.deleteById(id)
            // Attempt to remove physical file if cached
            try {
                val uri = Uri.parse(uriString)
                if (uri.scheme == "file") {
                    File(uri.path ?: "").delete()
                }
            } catch (_: Exception) {}
        }
    }

    // Active Document State
    private val _currentDoc = MutableStateFlow<PdfDocumentInfo?>(null)
    val currentDoc: StateFlow<PdfDocumentInfo?> = _currentDoc.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    private val _currentPageBitmap = MutableStateFlow<Bitmap?>(null)
    val currentPageBitmap: StateFlow<Bitmap?> = _currentPageBitmap.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _readingMode = MutableStateFlow(ReadingMode.NORMAL)
    val readingMode: StateFlow<ReadingMode> = _readingMode.asStateFlow()

    // Editing State
    private val _toolMode = MutableStateFlow(ToolMode.VIEW)
    val toolMode: StateFlow<ToolMode> = _toolMode.asStateFlow()

    private val _selectedColor = MutableStateFlow(Color(0xFFD32F2F))
    val selectedColor: StateFlow<Color> = _selectedColor.asStateFlow()

    private val _strokeWidth = MutableStateFlow(5f)
    val strokeWidth: StateFlow<Float> = _strokeWidth.asStateFlow()

    private val _activePages = MutableStateFlow<List<PageItem>>(emptyList())
    val activePages: StateFlow<List<PageItem>> = _activePages.asStateFlow()

    private val _pageAnnotations = MutableStateFlow<Map<Int, PageAnnotationState>>(emptyMap())
    val pageAnnotations: StateFlow<Map<Int, PageAnnotationState>> = _pageAnnotations.asStateFlow()

    private val _isModified = MutableStateFlow(false)
    val isModified: StateFlow<Boolean> = _isModified.asStateFlow()

    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus: StateFlow<String?> = _exportStatus.asStateFlow()

    /**
     * Load a document from a Uri (e.g. from WhatsApp, Files, or download)
     */
    fun loadFromUri(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val context = getApplication<Application>()
                val cachedFile = PdfEngine.copyUriToCache(context, uri)
                val totalPages = PdfEngine.getPdfPageCount(cachedFile)
                val name = PdfEngine.getFileName(context, uri)

                val docInfo = PdfDocumentInfo(
                    file = cachedFile,
                    fileName = name,
                    fileSize = cachedFile.length(),
                    originalUriString = uri.toString(),
                    pageCount = totalPages,
                    isSample = false
                )

                setupNewDocument(docInfo)

                // Save to Room Recents
                val activeFolder = _selectedFolder.value ?: "الرئيسية"
                repository.insertOrUpdate(
                    PdfRecentItem(
                        uriString = uri.toString(),
                        fileName = name,
                        fileSize = cachedFile.length(),
                        pageCount = totalPages,
                        lastPageRead = 0,
                        lastOpenedTimestamp = System.currentTimeMillis(),
                        folderName = activeFolder
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = "تعذر فتح المستند: ${e.localizedMessage ?: "تنسيق غير مدعوم"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads the interactive sample PDF guide
     */
    fun loadSample() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val context = getApplication<Application>()
                val sampleFile = PdfEngine.createSamplePdf(context)
                val totalPages = PdfEngine.getPdfPageCount(sampleFile)

                val docInfo = PdfDocumentInfo(
                    file = sampleFile,
                    fileName = "دليل الاستخدام_Sample.pdf",
                    fileSize = sampleFile.length(),
                    originalUriString = "sample://guide",
                    pageCount = totalPages,
                    isSample = true
                )

                setupNewDocument(docInfo)

                repository.insertOrUpdate(
                    PdfRecentItem(
                        uriString = "sample://guide",
                        fileName = "دليل الاستخدام_Sample.pdf",
                        fileSize = sampleFile.length(),
                        pageCount = totalPages,
                        lastPageRead = 0,
                        lastOpenedTimestamp = System.currentTimeMillis(),
                        isSample = true,
                        folderName = "الرئيسية"
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = "تعذر تحميل المستند التجريبي: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Creates a new blank PDF
     */
    fun createBlank(pages: Int = 1, title: String = "مذكرة جديدة") {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val context = getApplication<Application>()
                val file = PdfEngine.createBlankPdf(context, pages, title)
                val totalPages = PdfEngine.getPdfPageCount(file)
                val activeFolder = _selectedFolder.value ?: "الرئيسية"
                val docInfo = PdfDocumentInfo(
                    file = file,
                    fileName = "$title.pdf",
                    fileSize = file.length(),
                    originalUriString = file.toURI().toString(),
                    pageCount = totalPages
                )
                setupNewDocument(docInfo)

                repository.insertOrUpdate(
                    PdfRecentItem(
                        uriString = file.toURI().toString(),
                        fileName = "$title.pdf",
                        fileSize = file.length(),
                        pageCount = totalPages,
                        lastPageRead = 0,
                        lastOpenedTimestamp = System.currentTimeMillis(),
                        folderName = activeFolder
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = "تعذر إنشاء المستند: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun setupNewDocument(docInfo: PdfDocumentInfo) {
        _currentDoc.value = docInfo
        _currentPageIndex.value = 0
        _pageAnnotations.value = emptyMap()
        _isModified.value = false
        _toolMode.value = ToolMode.VIEW

        val pages = (0 until docInfo.pageCount).map { i ->
            PageItem(pageNumber = i + 1, originalIndex = i)
        }
        _activePages.value = pages

        renderCurrentPage()
    }

    fun goToPage(displayIndex: Int) {
        val pages = _activePages.value.filter { !it.isDeleted }
        if (displayIndex in pages.indices) {
            _currentPageIndex.value = displayIndex
            renderCurrentPage()

            // Update reading progress in room
            _currentDoc.value?.let { doc ->
                viewModelScope.launch {
                    repository.updateProgress(doc.originalUriString, displayIndex)
                }
            }
        }
    }

    fun nextPage() {
        val pages = _activePages.value.filter { !it.isDeleted }
        if (_currentPageIndex.value < pages.size - 1) {
            goToPage(_currentPageIndex.value + 1)
        }
    }

    fun prevPage() {
        if (_currentPageIndex.value > 0) {
            goToPage(_currentPageIndex.value - 1)
        }
    }

    private fun renderCurrentPage() {
        val doc = _currentDoc.value ?: return
        val pages = _activePages.value.filter { !it.isDeleted }
        if (_currentPageIndex.value !in pages.indices) return

        val pageItem = pages[_currentPageIndex.value]
        viewModelScope.launch {
            _isLoading.value = true
            val bmp = PdfEngine.renderPageBitmap(doc.file, pageItem.originalIndex, targetWidth = 1200)
            _currentPageBitmap.value = bmp
            _isLoading.value = false
        }
    }

    fun setReadingMode(mode: ReadingMode) {
        _readingMode.value = mode
    }

    fun setToolMode(mode: ToolMode) {
        _toolMode.value = mode
    }

    fun setColor(color: Color) {
        _selectedColor.value = color
    }

    fun setStrokeWidth(width: Float) {
        _strokeWidth.value = width
    }

    /**
     * Add a drawn stroke to the active page
     */
    fun addStroke(points: List<androidx.compose.ui.geometry.Offset>, isHighlighter: Boolean) {
        val pages = _activePages.value.filter { !it.isDeleted }
        if (_currentPageIndex.value !in pages.indices) return
        val origIndex = pages[_currentPageIndex.value].originalIndex

        val stroke = AnnotatedStroke(
            points = points,
            color = _selectedColor.value.toArgb().toLong(),
            strokeWidth = _strokeWidth.value,
            isHighlighter = isHighlighter
        )

        val currentMap = _pageAnnotations.value.toMutableMap()
        val pageState = currentMap[origIndex] ?: PageAnnotationState(origIndex)
        pageState.paths.add(stroke)
        currentMap[origIndex] = pageState

        _pageAnnotations.value = currentMap
        _isModified.value = true
    }

    /**
     * Add a text note or stamp to the active page
     */
    fun addTextAnnotation(text: String, xPercent: Float, yPercent: Float, isStamp: Boolean) {
        val pages = _activePages.value.filter { !it.isDeleted }
        if (_currentPageIndex.value !in pages.indices) return
        val origIndex = pages[_currentPageIndex.value].originalIndex

        val annotation = TextAnnotation(
            text = text,
            xPercent = xPercent,
            yPercent = yPercent,
            color = _selectedColor.value.toArgb().toLong(),
            fontSizeSp = if (isStamp) 18f else 16f,
            isStamp = isStamp
        )

        val currentMap = _pageAnnotations.value.toMutableMap()
        val pageState = currentMap[origIndex] ?: PageAnnotationState(origIndex)
        pageState.textNotes.add(annotation)
        currentMap[origIndex] = pageState

        _pageAnnotations.value = currentMap
        _isModified.value = true
    }

    fun undoLastStroke() {
        val pages = _activePages.value.filter { !it.isDeleted }
        if (_currentPageIndex.value !in pages.indices) return
        val origIndex = pages[_currentPageIndex.value].originalIndex

        val currentMap = _pageAnnotations.value.toMutableMap()
        val pageState = currentMap[origIndex] ?: return
        if (pageState.paths.isNotEmpty()) {
            pageState.paths.removeAt(pageState.paths.size - 1)
            currentMap[origIndex] = pageState
            _pageAnnotations.value = currentMap
        }
    }

    fun clearCurrentPageAnnotations() {
        val pages = _activePages.value.filter { !it.isDeleted }
        if (_currentPageIndex.value !in pages.indices) return
        val origIndex = pages[_currentPageIndex.value].originalIndex

        val currentMap = _pageAnnotations.value.toMutableMap()
        currentMap.remove(origIndex)
        _pageAnnotations.value = currentMap
    }

    /**
     * Rotate current page by 90 degrees
     */
    fun rotateCurrentPage() {
        val pages = _activePages.value.filter { !it.isDeleted }
        if (_currentPageIndex.value !in pages.indices) return
        val pageItem = pages[_currentPageIndex.value]

        val allPages = _activePages.value.toMutableList()
        val indexInAll = allPages.indexOfFirst { it.originalIndex == pageItem.originalIndex }
        if (indexInAll != -1) {
            val updated = allPages[indexInAll].copy(rotation = (allPages[indexInAll].rotation + 90) % 360)
            allPages[indexInAll] = updated
            _activePages.value = allPages
            _isModified.value = true
        }
    }

    /**
     * Delete page at display position
     */
    fun deletePage(displayIndex: Int) {
        val pages = _activePages.value.filter { !it.isDeleted }
        if (pages.size <= 1) {
            _errorMessage.value = "لا يمكن حذف جميع صفحات المستند"
            return
        }
        if (displayIndex in pages.indices) {
            val itemToDelete = pages[displayIndex]
            val allPages = _activePages.value.toMutableList()
            val indexInAll = allPages.indexOfFirst { it.originalIndex == itemToDelete.originalIndex }
            if (indexInAll != -1) {
                allPages[indexInAll] = allPages[indexInAll].copy(isDeleted = true)
                _activePages.value = allPages
                _isModified.value = true

                // Adjust current page if needed
                val remaining = allPages.filter { !it.isDeleted }
                val newIndex = displayIndex.coerceAtMost(remaining.size - 1)
                _currentPageIndex.value = newIndex
                renderCurrentPage()
            }
        }
    }

    /**
     * Move page from one position to another
     */
    fun movePage(fromIndex: Int, toIndex: Int) {
        val allPages = _activePages.value.toMutableList()
        val nonDeleted = allPages.filter { !it.isDeleted }.toMutableList()
        if (fromIndex in nonDeleted.indices && toIndex in nonDeleted.indices) {
            val moved = nonDeleted.removeAt(fromIndex)
            nonDeleted.add(toIndex, moved)
            _activePages.value = nonDeleted
            _isModified.value = true
            _currentPageIndex.value = toIndex
            renderCurrentPage()
        }
    }

    /**
     * Saves and exports the modified PDF with all drawings and page manipulations.
     */
    fun saveAndExport(onSuccess: (File) -> Unit) {
        val doc = _currentDoc.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _exportStatus.value = "جاري حفظ وتصدير ملف الـ PDF..."
            try {
                val context = getApplication<Application>()
                val exportDir = File(context.cacheDir, "exported_pdfs").apply { mkdirs() }
                val baseName = doc.fileName.substringBeforeLast('.')
                val outFile = File(exportDir, "${baseName}_edited_${System.currentTimeMillis()}.pdf")

                val success = PdfEngine.exportModifiedPdf(
                    context = context,
                    sourceFile = doc.file,
                    pages = _activePages.value,
                    annotations = _pageAnnotations.value,
                    outputFile = outFile
                )

                if (success) {
                    _isModified.value = false
                    _exportStatus.value = "تم الحفظ بنجاح!"

                    // Add the newly saved document to Recents list
                    val pageCount = PdfEngine.getPdfPageCount(outFile)
                    val activeFolder = _selectedFolder.value ?: "الرئيسية"
                    repository.insertOrUpdate(
                        PdfRecentItem(
                            uriString = outFile.toURI().toString(),
                            fileName = outFile.name,
                            fileSize = outFile.length(),
                            pageCount = pageCount,
                            lastPageRead = 0,
                            lastOpenedTimestamp = System.currentTimeMillis(),
                            folderName = activeFolder
                        )
                    )

                    onSuccess(outFile)
                } else {
                    _errorMessage.value = "حدث خطأ أثناء حفظ الملف المعدل"
                }
            } catch (e: Exception) {
                _errorMessage.value = "فشل التصدير: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
                _exportStatus.value = null
            }
        }
    }

    /**
     * Merges multiple selected PDF items into a single document.
     */
    fun mergePdfs(
        items: List<PdfRecentItem>,
        mergedTitle: String,
        onSuccess: (File) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _exportStatus.value = "جاري دمج ملفات الـ PDF..."
            try {
                val context = getApplication<Application>()
                val sourceFiles = mutableListOf<File>()

                for (item in items) {
                    val uri = Uri.parse(item.uriString)
                    val file = if (uri.scheme == "file") {
                        File(uri.path ?: "")
                    } else {
                        PdfEngine.copyUriToCache(context, uri)
                    }
                    if (file.exists() && file.length() > 0) {
                        sourceFiles.add(file)
                    }
                }

                if (sourceFiles.size < 2) {
                    _errorMessage.value = "يرجى اختيار ملفين صالحين على الأقل للدمج"
                    return@launch
                }

                val folder = File(context.cacheDir, "merged_pdfs").apply { mkdirs() }
                val cleanTitle = if (mergedTitle.endsWith(".pdf", ignoreCase = true)) mergedTitle else "$mergedTitle.pdf"
                val outFile = File(folder, "${System.currentTimeMillis()}_$cleanTitle")

                val success = PdfEngine.mergePdfFiles(context, sourceFiles, outFile)
                if (success) {
                    val pageCount = PdfEngine.getPdfPageCount(outFile)
                    val activeFolder = _selectedFolder.value ?: "الرئيسية"
                    repository.insertOrUpdate(
                        PdfRecentItem(
                            uriString = outFile.toURI().toString(),
                            fileName = cleanTitle,
                            fileSize = outFile.length(),
                            pageCount = pageCount,
                            lastPageRead = 0,
                            lastOpenedTimestamp = System.currentTimeMillis(),
                            folderName = activeFolder
                        )
                    )

                    // Immediately load the merged document
                    loadFromUri(Uri.fromFile(outFile))
                    onSuccess(outFile)
                } else {
                    _errorMessage.value = "حدث خطأ أثناء دمج الملفات"
                }
            } catch (e: Exception) {
                _errorMessage.value = "فشل الدمج: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
                _exportStatus.value = null
            }
        }
    }

    /**
     * Merges multiple Uris directly from system file picker
     */
    fun mergeFromUris(
        uris: List<Uri>,
        mergedTitle: String,
        onSuccess: (File) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _exportStatus.value = "جاري دمج ملفات الـ PDF..."
            try {
                val context = getApplication<Application>()
                val sourceFiles = mutableListOf<File>()

                for (uri in uris) {
                    val file = PdfEngine.copyUriToCache(context, uri)
                    if (file.exists() && file.length() > 0) {
                        sourceFiles.add(file)
                    }
                }

                if (sourceFiles.size < 2) {
                    _errorMessage.value = "يرجى اختيار ملفين صالحين على الأقل للدمج"
                    return@launch
                }

                val folder = File(context.cacheDir, "merged_pdfs").apply { mkdirs() }
                val cleanTitle = if (mergedTitle.endsWith(".pdf", ignoreCase = true)) mergedTitle else "$mergedTitle.pdf"
                val outFile = File(folder, "${System.currentTimeMillis()}_$cleanTitle")

                val success = PdfEngine.mergePdfFiles(context, sourceFiles, outFile)
                if (success) {
                    val pageCount = PdfEngine.getPdfPageCount(outFile)
                    val activeFolder = _selectedFolder.value ?: "الرئيسية"
                    repository.insertOrUpdate(
                        PdfRecentItem(
                            uriString = outFile.toURI().toString(),
                            fileName = cleanTitle,
                            fileSize = outFile.length(),
                            pageCount = pageCount,
                            lastPageRead = 0,
                            lastOpenedTimestamp = System.currentTimeMillis(),
                            folderName = activeFolder
                        )
                    )

                    loadFromUri(Uri.fromFile(outFile))
                    onSuccess(outFile)
                } else {
                    _errorMessage.value = "حدث خطأ أثناء دمج الملفات"
                }
            } catch (e: Exception) {
                _errorMessage.value = "فشل الدمج: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
                _exportStatus.value = null
            }
        }
    }

    /**
     * Splits a PDF file by extracting selected page indices into a new document.
     */
    fun splitPdf(
        sourceFile: File,
        selectedPageIndices: List<Int>,
        outputTitle: String,
        onSuccess: (File) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _exportStatus.value = "جاري استخراج الصفحات المحددة..."
            try {
                val context = getApplication<Application>()
                val folder = File(context.cacheDir, "split_pdfs").apply { mkdirs() }
                val cleanTitle = if (outputTitle.endsWith(".pdf", ignoreCase = true)) outputTitle else "$outputTitle.pdf"
                val outFile = File(folder, "${System.currentTimeMillis()}_$cleanTitle")

                val success = PdfEngine.extractPagesToPdf(context, sourceFile, selectedPageIndices, outFile)
                if (success) {
                    val pageCount = PdfEngine.getPdfPageCount(outFile)
                    val activeFolder = _selectedFolder.value ?: "الرئيسية"
                    repository.insertOrUpdate(
                        PdfRecentItem(
                            uriString = outFile.toURI().toString(),
                            fileName = cleanTitle,
                            fileSize = outFile.length(),
                            pageCount = pageCount,
                            lastPageRead = 0,
                            lastOpenedTimestamp = System.currentTimeMillis(),
                            folderName = activeFolder
                        )
                    )

                    loadFromUri(Uri.fromFile(outFile))
                    onSuccess(outFile)
                } else {
                    _errorMessage.value = "فشل استخراج الصفحات المحددة"
                }
            } catch (e: Exception) {
                _errorMessage.value = "خطأ أثناء تقسيم الملف: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
                _exportStatus.value = null
            }
        }
    }

    /**
     * Converts selected photos/images into a paginated PDF document.
     */
    fun convertPhotosToPdf(
        imageUris: List<Uri>,
        title: String,
        onSuccess: (File) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _exportStatus.value = "جاري تحويل الصور إلى PDF..."
            try {
                val context = getApplication<Application>()
                val folder = File(context.cacheDir, "created_pdfs").apply { mkdirs() }
                val cleanTitle = if (title.endsWith(".pdf", ignoreCase = true)) title else "$title.pdf"
                val outFile = File(folder, "${System.currentTimeMillis()}_$cleanTitle")

                val success = PdfEngine.convertImagesToPdf(context, imageUris, outFile)
                if (success) {
                    val pageCount = PdfEngine.getPdfPageCount(outFile)
                    val activeFolder = _selectedFolder.value ?: "الرئيسية"
                    repository.insertOrUpdate(
                        PdfRecentItem(
                            uriString = outFile.toURI().toString(),
                            fileName = cleanTitle,
                            fileSize = outFile.length(),
                            pageCount = pageCount,
                            lastPageRead = 0,
                            lastOpenedTimestamp = System.currentTimeMillis(),
                            folderName = activeFolder
                        )
                    )

                    loadFromUri(Uri.fromFile(outFile))
                    onSuccess(outFile)
                } else {
                    _errorMessage.value = "فشل تحويل الصور إلى PDF"
                }
            } catch (e: Exception) {
                _errorMessage.value = "خطأ في التحويل: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
                _exportStatus.value = null
            }
        }
    }

    fun prepareFileForSplit(uri: Uri, onReady: (PdfRecentItem) -> Unit) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val cached = PdfEngine.copyUriToCache(context, uri)
                val count = PdfEngine.getPdfPageCount(cached)
                val name = PdfEngine.getFileName(context, uri)
                onReady(
                    PdfRecentItem(
                        uriString = cached.toURI().toString(),
                        fileName = name,
                        fileSize = cached.length(),
                        pageCount = count
                    )
                )
            } catch (e: Exception) {
                loadFromUri(uri)
            }
        }
    }

    fun closeDocument() {
        _currentDoc.value = null
        _currentPageBitmap.value = null
        _pageAnnotations.value = emptyMap()
        _activePages.value = emptyList()
        _isModified.value = false
        _toolMode.value = ToolMode.VIEW
    }

    fun toggleFavorite(id: Long, current: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(id, current)
        }
    }

    fun deleteRecent(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllRecents() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
