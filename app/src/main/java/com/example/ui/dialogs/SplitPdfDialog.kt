package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PdfRedPrimary

@Composable
fun SplitPdfDialog(
    docName: String,
    totalPageCount: Int,
    onDismiss: () -> Unit,
    onSplit: (selectedPageIndices: List<Int>, outputTitle: String) -> Unit
) {
    val cleanBase = docName.removeSuffix(".pdf")
    var title by remember { mutableStateOf("${cleanBase}_مستخرج") }
    val selectedPages = remember { mutableStateListOf<Int>() }
    var isSplitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSplitting) onDismiss() },
        icon = {
            Icon(Icons.Default.ContentCut, contentDescription = null, tint = PdfRedPrimary, modifier = Modifier.size(28.dp))
        },
        title = {
            Text("تقسيم واستخراج صفحات", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم الملف المستخرج") },
                    singleLine = true,
                    enabled = !isSplitting,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "حدد الصفحات المراد استخراجها:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "تم تحديد ${selectedPages.size} من $totalPageCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = PdfRedPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick selection shortcuts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = {
                            selectedPages.clear()
                            selectedPages.addAll(0 until totalPageCount)
                        },
                        label = { Text("الكل", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = {
                            selectedPages.clear()
                            for (i in 0 until totalPageCount step 2) {
                                selectedPages.add(i)
                            }
                        },
                        label = { Text("فردي", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = {
                            selectedPages.clear()
                            for (i in 1 until totalPageCount step 2) {
                                selectedPages.add(i)
                            }
                        },
                        label = { Text("زوجي", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { selectedPages.clear() },
                        label = { Text("مسح", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Grid of Pages to select
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 44.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    items(totalPageCount) { pageIndex ->
                        val isSelected = selectedPages.contains(pageIndex)
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) PdfRedPrimary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) PdfRedPrimary else Color(0xFFB0BEC5),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (isSelected) selectedPages.remove(pageIndex)
                                    else selectedPages.add(pageIndex)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${pageIndex + 1}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedPages.isNotEmpty()) {
                        isSplitting = true
                        onSplit(selectedPages.sorted(), title.trim())
                    }
                },
                enabled = !isSplitting && title.isNotBlank() && selectedPages.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = PdfRedPrimary)
            ) {
                if (isSplitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("جاري الاستخراج...")
                } else {
                    Text("استخراج (${selectedPages.size}) صفحات")
                }
            }
        },
        dismissButton = {
            if (!isSplitting) {
                TextButton(onClick = onDismiss) {
                    Text("إلغاء")
                }
            }
        }
    )
}
