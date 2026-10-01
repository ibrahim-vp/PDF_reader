package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdf.PdfEngine
import com.example.pdf.ReadingMode
import com.example.pdf.ToolMode
import com.example.ui.PdfViewModel
import com.example.ui.components.PageManagerSheet
import com.example.ui.dialogs.AddStampDialog
import com.example.ui.dialogs.DocumentInfoDialog
import com.example.ui.dialogs.JumpToPageDialog
import com.example.ui.dialogs.SplitPdfDialog
import com.example.ui.theme.PdfRedPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    viewModel: PdfViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val currentDoc by viewModel.currentDoc.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val pageBitmap by viewModel.currentPageBitmap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val readingMode by viewModel.readingMode.collectAsState()
    val toolMode by viewModel.toolMode.collectAsState()
    val selectedColor by viewModel.selectedColor.collectAsState()
    val strokeWidth by viewModel.strokeWidth.collectAsState()
    val activePages by viewModel.activePages.collectAsState()
    val pageAnnotations by viewModel.pageAnnotations.collectAsState()
    val isModified by viewModel.isModified.collectAsState()
    val exportStatus by viewModel.exportStatus.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val nonDeletedPages = activePages.filter { !it.isDeleted }
    val totalPages = nonDeletedPages.size
    val currentPageItem = nonDeletedPages.getOrNull(currentPageIndex)
    val origIndex = currentPageItem?.originalIndex ?: 0
    val pageState = pageAnnotations[origIndex]

    // Dialogs state
    var showJumpDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showPageManager by remember { mutableStateOf(false) }
    var showStampDialog by remember { mutableStateOf(false) }
    var showSplitDialog by remember { mutableStateOf(false) }
    var pendingTapOffset by remember { mutableStateOf(Offset.Zero) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Canvas zoom & pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Current in-progress drawing points
    val currentPoints = remember { mutableStateListOf<Offset>() }

    BackHandler {
        onBackClick()
    }

    if (showJumpDialog) {
        JumpToPageDialog(
            currentPage = currentPageIndex,
            totalPages = totalPages,
            onDismiss = { showJumpDialog = false },
            onJump = { page ->
                viewModel.goToPage(page)
                scale = 1f
                offsetX = 0f
                offsetY = 0f
            }
        )
    }

    if (showInfoDialog && currentDoc != null) {
        DocumentInfoDialog(
            docInfo = currentDoc!!,
            onDismiss = { showInfoDialog = false }
        )
    }

    if (showStampDialog) {
        AddStampDialog(
            onDismiss = { showStampDialog = false },
            onConfirm = { text, isStamp ->
                val xPercent = if (pendingTapOffset.x > 0) (pendingTapOffset.x / 1000f).coerceIn(0.1f, 0.8f) else 0.3f
                val yPercent = if (pendingTapOffset.y > 0) (pendingTapOffset.y / 1400f).coerceIn(0.1f, 0.8f) else 0.5f
                viewModel.addTextAnnotation(text, xPercent, yPercent, isStamp)
            }
        )
    }

    if (showPageManager) {
        PageManagerSheet(
            pages = activePages,
            currentPageIndex = currentPageIndex,
            onDismiss = { showPageManager = false },
            onSelectPage = { page ->
                viewModel.goToPage(page)
                scale = 1f
                offsetX = 0f
                offsetY = 0f
            },
            onRotatePage = { idx ->
                val p = nonDeletedPages.getOrNull(idx)
                if (p != null) {
                    viewModel.rotateCurrentPage()
                }
            },
            onDeletePage = { idx ->
                viewModel.deletePage(idx)
            },
            onMovePage = { from, to ->
                viewModel.movePage(from, to)
            }
        )
    }

    if (showSplitDialog && currentDoc != null) {
        SplitPdfDialog(
            docName = currentDoc!!.fileName,
            totalPageCount = totalPages,
            onDismiss = { showSplitDialog = false },
            onSplit = { selectedIndices, outputTitle ->
                viewModel.splitPdf(currentDoc!!.file, selectedIndices, outputTitle) {
                    showSplitDialog = false
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentDoc?.fileName ?: "مستند PDF",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "صفحة ${currentPageIndex + 1} من $totalPages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isModified) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• معدل",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PdfRedPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    // Tool mode toggle (View vs Annotate)
                    IconButton(
                        onClick = {
                            if (toolMode == ToolMode.VIEW) {
                                viewModel.setToolMode(ToolMode.PEN)
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                viewModel.setToolMode(ToolMode.VIEW)
                            }
                        },
                        modifier = Modifier.testTag("toggle_edit_mode")
                    ) {
                        Icon(
                            imageVector = if (toolMode == ToolMode.VIEW) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = if (toolMode == ToolMode.VIEW) "تعديل ورسم" else "وضع العرض",
                            tint = if (toolMode != ToolMode.VIEW) PdfRedPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Night / Dark Mode Toggle
                    IconButton(
                        onClick = {
                            viewModel.toggleDarkMode(currentSystemDark = false)
                            if (isDarkMode != true) {
                                viewModel.setReadingMode(ReadingMode.NIGHT)
                            } else {
                                viewModel.setReadingMode(ReadingMode.NORMAL)
                            }
                        },
                        modifier = Modifier.testTag("viewer_theme_toggle")
                    ) {
                        val isDark = isDarkMode == true
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDark) "الوضع النهاري" else "الوضع الليلي",
                            tint = if (isDark) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Share button
                    IconButton(
                        onClick = {
                            currentDoc?.let { doc ->
                                if (isModified) {
                                    viewModel.saveAndExport { savedFile ->
                                        PdfEngine.sharePdfFile(context, savedFile, "مشاركة PDF المعدل")
                                    }
                                } else {
                                    PdfEngine.sharePdfFile(context, doc.file, "مشاركة PDF")
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة")
                    }

                    // More Menu
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "المزيد")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("انتقال لصفحة...") },
                                onClick = {
                                    showMoreMenu = false
                                    showJumpDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Navigation, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("إدارة الصفحات وترتيبها") },
                                onClick = {
                                    showMoreMenu = false
                                    showPageManager = true
                                },
                                leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (readingMode) {
                                            ReadingMode.NORMAL -> "الوضع الليلي (داكن)"
                                            ReadingMode.NIGHT -> "وضع القراءة الدافئ (Sepia)"
                                            ReadingMode.SEPIA -> "الوضع العادي"
                                        }
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    val nextMode = when (readingMode) {
                                        ReadingMode.NORMAL -> ReadingMode.NIGHT
                                        ReadingMode.NIGHT -> ReadingMode.SEPIA
                                        ReadingMode.SEPIA -> ReadingMode.NORMAL
                                    }
                                    viewModel.setReadingMode(nextMode)
                                },
                                leadingIcon = {
                                    Icon(
                                        if (readingMode == ReadingMode.NIGHT) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("تقسيم واستخراج صفحات") },
                                onClick = {
                                    showMoreMenu = false
                                    showSplitDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("طباعة المستند") },
                                onClick = {
                                    showMoreMenu = false
                                    currentDoc?.let { PdfEngine.printPdf(context, it.file) }
                                },
                                leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("معلومات الملف") },
                                onClick = {
                                    showMoreMenu = false
                                    showInfoDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (toolMode != ToolMode.VIEW) {
                // Bottom Annotation Controls
                AnnotationControlsBar(
                    toolMode = toolMode,
                    selectedColor = selectedColor,
                    strokeWidth = strokeWidth,
                    onSelectTool = { viewModel.setToolMode(it) },
                    onSelectColor = { viewModel.setColor(it) },
                    onSelectStrokeWidth = { viewModel.setStrokeWidth(it) },
                    onUndo = { viewModel.undoLastStroke() },
                    onClear = { viewModel.clearCurrentPageAnnotations() },
                    onSave = {
                        viewModel.saveAndExport { savedFile ->
                            PdfEngine.sharePdfFile(context, savedFile, "مشاركة الملف المحفوظ")
                        }
                    }
                )
            } else {
                // Standard Reading Controls
                ReadingControlsBar(
                    currentPage = currentPageIndex,
                    totalPages = totalPages,
                    onPrevPage = {
                        viewModel.prevPage()
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onNextPage = {
                        viewModel.nextPage()
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onRotate = { viewModel.rotateCurrentPage() },
                    onOpenPageManager = { showPageManager = true },
                    onSeekPage = { page ->
                        viewModel.goToPage(page)
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    when (readingMode) {
                        ReadingMode.NORMAL -> Color(0xFFEBEBEB)
                        ReadingMode.NIGHT -> Color(0xFF121212)
                        ReadingMode.SEPIA -> Color(0xFFF7F1E3)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading && pageBitmap == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PdfRedPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(exportStatus ?: "جاري تحميل الصفحة...", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (pageBitmap != null) {
                // Main PDF Page Viewport
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val rotationDeg = ((currentPageItem?.rotation ?: 0) + (pageState?.rotation ?: 0)) % 360

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                                rotationZ = rotationDeg.toFloat()
                            }
                            .pointerInput(toolMode) {
                                if (toolMode == ToolMode.VIEW) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 5f)
                                        if (scale > 1f) {
                                            offsetX += pan.x
                                            offsetY += pan.y
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    }
                                } else if (toolMode == ToolMode.TEXT) {
                                    detectTapGestures { offset ->
                                        pendingTapOffset = offset
                                        showStampDialog = true
                                    }
                                } else {
                                    // Drawing gestures (PEN / HIGHLIGHTER / ERASER)
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentPoints.clear()
                                            currentPoints.add(offset)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            currentPoints.add(change.position)
                                        },
                                        onDragEnd = {
                                            if (currentPoints.size > 1) {
                                                if (toolMode == ToolMode.ERASER) {
                                                    // Remove nearby stroke
                                                    viewModel.undoLastStroke()
                                                } else {
                                                    viewModel.addStroke(
                                                        points = currentPoints.toList(),
                                                        isHighlighter = toolMode == ToolMode.HIGHLIGHTER
                                                    )
                                                }
                                            }
                                            currentPoints.clear()
                                        },
                                        onDragCancel = {
                                            currentPoints.clear()
                                        }
                                    )
                                }
                            }
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(6.dp))
                            .background(Color.White)
                    ) {
                        // Render PDF bitmap with Color Filter based on Reading Mode
                        val colorFilter = when (readingMode) {
                            ReadingMode.NORMAL -> null
                            ReadingMode.NIGHT -> {
                                val matrix = ColorMatrix(
                                    floatArrayOf(
                                        -1f, 0f, 0f, 0f, 255f,
                                        0f, -1f, 0f, 0f, 255f,
                                        0f, 0f, -1f, 0f, 255f,
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                                ColorFilter.colorMatrix(matrix)
                            }
                            ReadingMode.SEPIA -> {
                                val matrix = ColorMatrix(
                                    floatArrayOf(
                                        0.393f, 0.769f, 0.189f, 0f, 0f,
                                        0.349f, 0.686f, 0.168f, 0f, 0f,
                                        0.272f, 0.534f, 0.131f, 0f, 0f,
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                                ColorFilter.colorMatrix(matrix)
                            }
                        }

                        Image(
                            bitmap = pageBitmap!!.asImageBitmap(),
                            contentDescription = "صفحة ${currentPageIndex + 1}",
                            colorFilter = colorFilter,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Render Annotations Canvas Overlay
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw saved strokes
                            pageState?.paths?.forEach { stroke ->
                                if (stroke.points.size > 1) {
                                    val path = Path().apply {
                                        moveTo(stroke.points[0].x, stroke.points[0].y)
                                        for (i in 1 until stroke.points.size) {
                                            lineTo(stroke.points[i].x, stroke.points[i].y)
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color(stroke.color).copy(alpha = if (stroke.isHighlighter) 0.35f else 1f),
                                        style = Stroke(
                                            width = stroke.strokeWidth,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }

                            // Draw current in-progress stroke
                            if (currentPoints.size > 1) {
                                val currentPath = Path().apply {
                                    moveTo(currentPoints[0].x, currentPoints[0].y)
                                    for (i in 1 until currentPoints.size) {
                                        lineTo(currentPoints[i].x, currentPoints[i].y)
                                    }
                                }
                                drawPath(
                                    path = currentPath,
                                    color = selectedColor.copy(alpha = if (toolMode == ToolMode.HIGHLIGHTER) 0.35f else 1f),
                                    style = Stroke(
                                        width = strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }

                        // Render text notes / stamps overlay
                        pageState?.textNotes?.forEach { note ->
                            Box(
                                modifier = Modifier
                                    .padding(
                                        start = (note.xPercent * 300).dp,
                                        top = (note.yPercent * 400).dp
                                    )
                            ) {
                                if (note.isStamp) {
                                    Surface(
                                        color = Color(note.color).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(note.color))
                                    ) {
                                        Text(
                                            text = note.text,
                                            color = Color(note.color),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = note.text,
                                        color = Color(note.color),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Exporting progress banner
            if (exportStatus != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(exportStatus!!, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun ReadingControlsBar(
    currentPage: Int,
    totalPages: Int,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onRotate: () -> Unit,
    onOpenPageManager: () -> Unit,
    onSeekPage: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Slider to scrub pages
            if (totalPages > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${currentPage + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = currentPage.toFloat(),
                        onValueChange = { onSeekPage(it.toInt()) },
                        valueRange = 0f..(totalPages - 1).toFloat(),
                        steps = if (totalPages > 2) totalPages - 2 else 0,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = PdfRedPrimary,
                            activeTrackColor = PdfRedPrimary
                        )
                    )
                    Text(
                        "$totalPages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Navigation Prev / Next
                Row {
                    IconButton(
                        onClick = onPrevPage,
                        enabled = currentPage > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "الصفحة السابقة")
                    }
                    IconButton(
                        onClick = onNextPage,
                        enabled = currentPage < totalPages - 1
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "الصفحة التالية")
                    }
                }

                // Page Manager Button
                FilledTonalButton(
                    onClick = onOpenPageManager,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إدارة الصفحات (${currentPage + 1}/$totalPages)")
                }

                // Rotate Button
                IconButton(onClick = onRotate) {
                    Icon(Icons.Default.RotateRight, contentDescription = "تدوير 90°")
                }
            }
        }
    }
}

@Composable
fun AnnotationControlsBar(
    toolMode: ToolMode,
    selectedColor: Color,
    strokeWidth: Float,
    onSelectTool: (ToolMode) -> Unit,
    onSelectColor: (Color) -> Unit,
    onSelectStrokeWidth: (Float) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit
) {
    val colors = listOf(
        Color(0xFFD32F2F), // Red
        Color(0xFF1976D2), // Blue
        Color(0xFF388E3C), // Green
        Color(0xFFFBC02D), // Yellow highlighter
        Color(0xFF7B1FA2), // Purple
        Color(0xFF212121)  // Black
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Tools Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolIconButton(
                    icon = Icons.Default.Edit,
                    label = "قلم",
                    isSelected = toolMode == ToolMode.PEN,
                    onClick = { onSelectTool(ToolMode.PEN) }
                )
                ToolIconButton(
                    icon = Icons.Default.ColorLens,
                    label = "تظليل",
                    isSelected = toolMode == ToolMode.HIGHLIGHTER,
                    onClick = { onSelectTool(ToolMode.HIGHLIGHTER) }
                )
                ToolIconButton(
                    icon = Icons.Default.TextFields,
                    label = "ختم / نص",
                    isSelected = toolMode == ToolMode.TEXT,
                    onClick = { onSelectTool(ToolMode.TEXT) }
                )
                ToolIconButton(
                    icon = Icons.Default.Delete,
                    label = "ممحاة",
                    isSelected = toolMode == ToolMode.ERASER,
                    onClick = { onSelectTool(ToolMode.ERASER) }
                )
                IconButton(onClick = onUndo) {
                    Icon(Icons.Default.Undo, contentDescription = "تراجع")
                }
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "مسح الصفحة")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Color Chips and Save Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color palette
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { onSelectColor(color) }
                                .then(
                                    if (selectedColor == color) {
                                        Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == color) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (color == Color(0xFFFBC02D)) Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Save & Export Button
                FilledTonalButton(
                    onClick = onSave,
                    shape = RoundedCornerShape(10.dp),
                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                        containerColor = PdfRedPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("save_pdf_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ التعديلات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) PdfRedPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) PdfRedPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
