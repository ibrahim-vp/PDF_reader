package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PdfRecentItem
import com.example.ui.theme.PdfRedLight
import com.example.ui.theme.PdfRedPrimary

@Composable
fun MergePdfsDialog(
    initialItems: List<PdfRecentItem>,
    onDismiss: () -> Unit,
    onMerge: (itemsInOrder: List<PdfRecentItem>, mergedTitle: String) -> Unit
) {
    var title by remember { mutableStateOf("مستند_مدمج_${System.currentTimeMillis() % 10000}") }
    val items = remember { mutableStateListOf<PdfRecentItem>().apply { addAll(initialItems) } }
    var isMerging by remember { mutableStateOf(false) }

    val totalPages = items.sumOf { it.pageCount }

    AlertDialog(
        onDismissRequest = { if (!isMerging) onDismiss() },
        icon = {
            Icon(Icons.Default.Merge, contentDescription = null, tint = PdfRedPrimary, modifier = Modifier.size(28.dp))
        },
        title = {
            Text("دمج مستندات PDF", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم الملف المدمج") },
                    singleLine = true,
                    enabled = !isMerging,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ترتيب الملفات المحددة (${items.size}):",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "إجمالي الصفحات: $totalPages",
                        style = MaterialTheme.typography.bodySmall,
                        color = PdfRedPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(items) { index, item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Order badge
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(PdfRedLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${index + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PdfRedPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.fileName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.Medium,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "${item.pageCount} صفحة",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Reorder buttons
                                Row {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val removed = items.removeAt(index)
                                                items.add(index - 1, removed)
                                            }
                                        },
                                        enabled = index > 0 && !isMerging,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "أعلى", modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            if (index < items.size - 1) {
                                                val removed = items.removeAt(index)
                                                items.add(index + 1, removed)
                                            }
                                        },
                                        enabled = index < items.size - 1 && !isMerging,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "أسفل", modifier = Modifier.size(16.dp))
                                    }

                                    if (items.size > 2) {
                                        IconButton(
                                            onClick = { items.removeAt(index) },
                                            enabled = !isMerging,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "إزالة", modifier = Modifier.size(16.dp), tint = Color(0xFFD32F2F))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && items.size >= 2) {
                        isMerging = true
                        onMerge(items.toList(), title.trim())
                    }
                },
                enabled = !isMerging && title.isNotBlank() && items.size >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = PdfRedPrimary)
            ) {
                if (isMerging) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("جاري الدمج...")
                } else {
                    Text("دمج المستندات (${items.size})")
                }
            }
        },
        dismissButton = {
            if (!isMerging) {
                TextButton(onClick = onDismiss) {
                    Text("إلغاء")
                }
            }
        }
    )
}
