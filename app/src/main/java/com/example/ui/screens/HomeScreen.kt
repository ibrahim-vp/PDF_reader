package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PdfFolder
import com.example.data.PdfRecentItem
import com.example.pdf.PdfEngine
import com.example.ui.PdfViewModel
import com.example.ui.dialogs.ConvertPhotosDialog
import com.example.ui.dialogs.CreateFolderDialog
import com.example.ui.dialogs.DeleteFileDialog
import com.example.ui.dialogs.MergePdfsDialog
import com.example.ui.dialogs.MoveToFolderDialog
import com.example.ui.dialogs.RenameFileDialog
import com.example.ui.dialogs.SplitPdfDialog
import com.example.ui.theme.PdfRedLight
import com.example.ui.theme.PdfRedPrimary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PdfViewModel,
    onOpenPdf: () -> Unit
) {
    val context = LocalContext.current
    val filteredRecents by viewModel.filteredRecents.collectAsState()
    val filteredFavorites by viewModel.filteredFavorites.collectAsState()
    val foldersList by viewModel.foldersList.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Recents, 1: Favorites
    var showCreateBlankDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    // Multi-Selection State for Merging
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedItemsForMerge = remember { mutableStateListOf<PdfRecentItem>() }
    var showMergeDialog by remember { mutableStateOf(false) }
    var itemsForMergeDialog by remember { mutableStateOf<List<PdfRecentItem>>(emptyList()) }

    // Photos to PDF State
    var selectedPhotosToConvert by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showConvertPhotosDialog by remember { mutableStateOf(false) }

    // Split PDF State
    var itemToSplit by remember { mutableStateOf<PdfRecentItem?>(null) }

    // Dialogs for item actions
    var itemToRename by remember { mutableStateOf<PdfRecentItem?>(null) }
    var itemToMove by remember { mutableStateOf<PdfRecentItem?>(null) }
    var itemToDelete by remember { mutableStateOf<PdfRecentItem?>(null) }

    // System Document Picker for single PDF
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadFromUri(uri)
            onOpenPdf()
        }
    }

    // System Document Picker for multiple PDFs to merge directly
    val multiplePdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.size >= 2) {
            viewModel.mergeFromUris(
                uris = uris,
                mergedTitle = "مستند_مدمج_${System.currentTimeMillis() % 10000}"
            ) {
                onOpenPdf()
            }
        } else if (uris.size == 1) {
            viewModel.loadFromUri(uris[0])
            onOpenPdf()
        }
    }

    // System Photo Picker (Zero permissions required)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedPhotosToConvert = uris
            showConvertPhotosDialog = true
        }
    }

    // System Document Picker for selecting a file to split
    val splitPdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.prepareFileForSplit(uri) { item ->
                itemToSplit = item
            }
        }
    }

    if (showCreateBlankDialog) {
        CreateBlankPdfDialog(
            onDismiss = { showCreateBlankDialog = false },
            onCreate = { title, pages ->
                viewModel.createBlank(pages, title)
                onOpenPdf()
            }
        )
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name, colorHex ->
                viewModel.createFolder(name, colorHex)
            }
        )
    }

    if (showConvertPhotosDialog && selectedPhotosToConvert.isNotEmpty()) {
        ConvertPhotosDialog(
            selectedImageUris = selectedPhotosToConvert,
            onDismiss = {
                showConvertPhotosDialog = false
                selectedPhotosToConvert = emptyList()
            },
            onConvert = { title ->
                viewModel.convertPhotosToPdf(selectedPhotosToConvert, title) {
                    showConvertPhotosDialog = false
                    selectedPhotosToConvert = emptyList()
                    onOpenPdf()
                }
            }
        )
    }

    if (itemToSplit != null) {
        SplitPdfDialog(
            docName = itemToSplit!!.fileName,
            totalPageCount = itemToSplit!!.pageCount,
            onDismiss = { itemToSplit = null },
            onSplit = { selectedPages, outputTitle ->
                try {
                    val uri = Uri.parse(itemToSplit!!.uriString)
                    val sourceFile = if (uri.scheme == "file") {
                        File(uri.path ?: "")
                    } else {
                        File(context.cacheDir, "split_source.pdf").also { temp ->
                            context.contentResolver.openInputStream(uri)?.use { inStream ->
                                temp.outputStream().use { outStream -> inStream.copyTo(outStream) }
                            }
                        }
                    }

                    viewModel.splitPdf(sourceFile, selectedPages, outputTitle) {
                        itemToSplit = null
                        onOpenPdf()
                    }
                } catch (e: Exception) {
                    itemToSplit = null
                }
            }
        )
    }

    if (showMergeDialog && itemsForMergeDialog.isNotEmpty()) {
        MergePdfsDialog(
            initialItems = itemsForMergeDialog,
            onDismiss = {
                showMergeDialog = false
                itemsForMergeDialog = emptyList()
            },
            onMerge = { orderedItems, mergedTitle ->
                viewModel.mergePdfs(orderedItems, mergedTitle) {
                    showMergeDialog = false
                    itemsForMergeDialog = emptyList()
                    isSelectionMode = false
                    selectedItemsForMerge.clear()
                    onOpenPdf()
                }
            }
        )
    }

    if (itemToRename != null) {
        RenameFileDialog(
            currentName = itemToRename!!.fileName,
            onDismiss = { itemToRename = null },
            onConfirm = { newName ->
                viewModel.renameFile(itemToRename!!.id, newName)
                itemToRename = null
            }
        )
    }

    if (itemToMove != null) {
        MoveToFolderDialog(
            currentFolder = itemToMove!!.folderName,
            folders = foldersList,
            onDismiss = { itemToMove = null },
            onConfirm = { targetFolder ->
                viewModel.moveFileToFolder(itemToMove!!.id, targetFolder)
                itemToMove = null
            }
        )
    }

    if (itemToDelete != null) {
        DeleteFileDialog(
            fileName = itemToDelete!!.fileName,
            onDismiss = { itemToDelete = null },
            onConfirm = {
                viewModel.deleteFile(itemToDelete!!.id, itemToDelete!!.uriString)
                itemToDelete = null
            }
        )
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // Multi-Selection TopAppBar
                TopAppBar(
                    title = {
                        Text(
                            "تم تحديد ${selectedItemsForMerge.size} مستندات",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedItemsForMerge.clear()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "إلغاء التحديد")
                        }
                    },
                    actions = {
                        if (selectedItemsForMerge.size >= 2) {
                            FilledTonalButton(
                                onClick = {
                                    itemsForMergeDialog = selectedItemsForMerge.toList()
                                    showMergeDialog = true
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = PdfRedPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Merge, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("دمج الآن")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Red PDF Logo Badge
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PdfRedPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "PDF",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "قارئ ومحرر PDF",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "100% Ad-Free • بدون إعلانات",
                                        fontSize = 10.sp,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Multi-selection button
                        IconButton(
                            onClick = { isSelectionMode = true },
                            modifier = Modifier.testTag("selection_mode_button")
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = "تحديد متعدد للدمج")
                        }

                        // Dark / Light Mode Toggle
                        IconButton(
                            onClick = { viewModel.toggleDarkMode(currentSystemDark = false) },
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            val isDark = isDarkMode == true
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDark) "تفعيل الوضع النهاري" else "تفعيل الوضع الليلي",
                                tint = if (isDark) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        floatingActionButton = {
            if (isSelectionMode && selectedItemsForMerge.size >= 2) {
                ExtendedFloatingActionButton(
                    onClick = {
                        itemsForMergeDialog = selectedItemsForMerge.toList()
                        showMergeDialog = true
                    },
                    containerColor = PdfRedPrimary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Merge, contentDescription = null) },
                    text = { Text("دمج ${selectedItemsForMerge.size} مستندات", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("fab_merge_selected")
                )
            } else {
                FloatingActionButton(
                    onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                    containerColor = PdfRedPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_open_pdf")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "فتح PDF")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("فتح PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("بحث عن مستند أو مجلد...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "بحث", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح البحث")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = PdfRedPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_text_field")
                )
            }

            // Folders Horizontal Category Bar
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "المجلدات والتصنيفات",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { showCreateFolderDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مجلد جديد", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "All" Chip
                        FilterChip(
                            selected = selectedFolder == null,
                            onClick = { viewModel.selectFolder(null) },
                            label = { Text("الكل") },
                            leadingIcon = {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = PdfRedPrimary
                            )
                        )

                        // Dynamic Folder Chips
                        foldersList.forEach { folder ->
                            val isSelected = selectedFolder == folder.name
                            val folderColor = try {
                                Color(android.graphics.Color.parseColor(folder.colorHex))
                            } catch (_: Exception) {
                                PdfRedPrimary
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) viewModel.selectFolder(null) else viewModel.selectFolder(folder.name)
                                },
                                label = { Text(folder.name) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(folderColor)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = folderColor.copy(alpha = 0.2f),
                                    selectedLabelColor = folderColor
                                )
                            )
                        }
                    }
                }
            }

            // Quick Open & Tools Banner (When not searching)
            if (searchQuery.isBlank() && selectedFolder == null) {
                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
                            .testTag("open_pdf_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = PdfRedPrimary
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "فتح ملف PDF من جهازك",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "تصفح مستنداتك وتعديلها وحفظها بدون أي إعلانات",
                                    color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // 4 Quick Feature Cards: Photos to PDF, Merge, Split, Sample
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Photos to PDF
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("photos_to_pdf_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE8F5E9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("صور إلى PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("تحويل الصور لمستند", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }

                        // 2. Merge PDFs
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    multiplePdfPickerLauncher.launch(arrayOf("application/pdf"))
                                }
                                .testTag("merge_pdfs_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE3F2FD)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Merge,
                                        contentDescription = null,
                                        tint = Color(0xFF1976D2),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("دمج ملفات", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("دمج عدة ملفات", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }

                        // 3. Split PDF
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    splitPdfPickerLauncher.launch(arrayOf("application/pdf"))
                                }
                                .testTag("split_pdf_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFF3E0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ContentCut,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("تقسيم PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("استخراج صفحات", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }

                        // 4. Sample Document
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.loadSample()
                                    onOpenPdf()
                                }
                                .testTag("sample_doc_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PdfRedLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = PdfRedPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("مستند تجريبي", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("دليل لاختبار الأدوات", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Tabs: Recents vs Favorites
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = PdfRedPrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("الملفات (${filteredRecents.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("المفضلة (${filteredFavorites.size})") }
                    )
                }
            }

            val displayedItems = if (selectedTab == 0) filteredRecents else filteredFavorites

            if (displayedItems.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (searchQuery.isNotEmpty()) Icons.Default.Search
                                else if (selectedTab == 0) Icons.Default.Description
                                else Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (searchQuery.isNotEmpty()) "لا توجد نتائج تطابق '$searchQuery'"
                            else if (selectedTab == 0) "لا توجد مستندات في هذا المجلد"
                            else "لا توجد ملفات في المفضلة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (searchQuery.isNotEmpty()) "جرّب البحث باسم آخر أو إزالة التصفية"
                            else "افتح ملفاً جديداً أو حوّل صوراً إلى PDF",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(displayedItems, key = { it.id }) { item ->
                    val isChecked = selectedItemsForMerge.any { it.id == item.id }

                    RecentPdfCard(
                        item = item,
                        isSelectionMode = isSelectionMode,
                        isChecked = isChecked,
                        onCheckedChange = { checked ->
                            if (checked) {
                                selectedItemsForMerge.add(item)
                            } else {
                                selectedItemsForMerge.removeAll { it.id == item.id }
                            }
                        },
                        onClick = {
                            if (isSelectionMode) {
                                if (isChecked) {
                                    selectedItemsForMerge.removeAll { it.id == item.id }
                                } else {
                                    selectedItemsForMerge.add(item)
                                }
                            } else {
                                if (item.isSample) {
                                    viewModel.loadSample()
                                } else {
                                    viewModel.loadFromUri(Uri.parse(item.uriString))
                                }
                                onOpenPdf()
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedItemsForMerge.add(item)
                            }
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(item.id, item.isFavorite) },
                        onRename = { itemToRename = item },
                        onMove = { itemToMove = item },
                        onSplit = { itemToSplit = item },
                        onDelete = { itemToDelete = item }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Clearance for FAB
            }
        }
    }
}

@Composable
fun RecentPdfCard(
    item: PdfRecentItem,
    isSelectionMode: Boolean = false,
    isChecked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onSplit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val dateStr = remember(item.lastOpenedTimestamp) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(item.lastOpenedTimestamp))
    }

    val sizeKb = item.fileSize / 1024
    val sizeText = if (sizeKb > 1024) String.format("%.1f MB", sizeKb / 1024f) else "$sizeKb KB"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(
                if (isChecked) Modifier.border(2.dp, PdfRedPrimary, RoundedCornerShape(12.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox in selection mode
            if (isSelectionMode) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(checkedColor = PdfRedPrimary)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            // PDF Emblem Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PdfRedLight),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = PdfRedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "PDF",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = PdfRedPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.fileName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Folder badge
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(item.folderName, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${item.pageCount} ص • $sizeText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            if (!isSelectionMode) {
                // Favorite Button
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "المفضلة",
                        tint = if (item.isFavorite) PdfRedPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // More Options Menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "خيارات الملف")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("تقسيم واستخراج صفحات") },
                            onClick = {
                                showMenu = false
                                onSplit()
                            },
                            leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("إعادة تسمية") },
                            onClick = {
                                showMenu = false
                                onRename()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("نقل إلى مجلد...") },
                            onClick = {
                                showMenu = false
                                onMove()
                            },
                            leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("مشاركة") },
                            onClick = {
                                showMenu = false
                                try {
                                    val uri = Uri.parse(item.uriString)
                                    val file = if (uri.scheme == "file") File(uri.path ?: "") else null
                                    if (file != null && file.exists()) {
                                        PdfEngine.sharePdfFile(context, file, item.fileName)
                                    }
                                } catch (_: Exception) {}
                            },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف الملف", color = Color(0xFFD32F2F)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD32F2F)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateBlankPdfDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, pages: Int) -> Unit
) {
    var title by remember { mutableStateOf("مذكرة جديدة") }
    var pageCount by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.NoteAdd, contentDescription = null, tint = PdfRedPrimary)
        },
        title = {
            Text("إنشاء ملف PDF جديد", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان المستند") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("عدد الصفحات: $pageCount", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 3, 5).forEach { count ->
                        FilledTonalButton(
                            onClick = { pageCount = count },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("$count")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(title.trim(), pageCount)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PdfRedPrimary)
            ) {
                Text("إنشاء وفتح")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
