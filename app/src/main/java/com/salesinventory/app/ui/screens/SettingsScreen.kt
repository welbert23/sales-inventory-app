package com.salesinventory.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salesinventory.app.data.ReportCustomSection
import com.salesinventory.app.data.ReportSettings
import com.salesinventory.app.data.ReportSettingsManager
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { ReportSettingsManager(context) }
    var settings by remember { mutableStateOf(manager.load()) }
    var showAddSection by remember { mutableStateOf(false) }
    var editSectionIndex by remember { mutableStateOf(-1) }

    fun save(s: ReportSettings) {
        settings = s
        manager.save(s)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Header", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = settings.storeName,
                onValueChange = { save(settings.copy(storeName = it)) },
                label = { Text("Store Name") },
                leadingIcon = { Icon(Icons.Filled.Store, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Divider()
            Text("Sections to Include", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            SettingsToggle(
                checked = settings.showDailySales,
                onCheckedChange = { save(settings.copy(showDailySales = it)) },
                label = "Daily Sales",
                description = "Product-wise sales breakdown for selected date",
                icon = Icons.Filled.ShoppingCart
            )

            SettingsToggle(
                checked = settings.showMTD,
                onCheckedChange = { save(settings.copy(showMTD = it)) },
                label = "Month-to-Date (MTD)",
                description = "Total sales from start of month to today",
                icon = Icons.Filled.DateRange
            )

            SettingsToggle(
                checked = settings.showTarget,
                onCheckedChange = { save(settings.copy(showTarget = it)) },
                label = "Target & % to Plan",
                description = "Monthly target and percentage achieved",
                icon = Icons.Filled.TrackChanges
            )

            SettingsToggle(
                checked = settings.showStockLevel,
                onCheckedChange = { save(settings.copy(showStockLevel = it)) },
                label = "Stock Levels",
                description = "Current inventory stock summary",
                icon = Icons.Filled.Inventory2
            )

            OutlinedTextField(
                value = settings.minStockThreshold.toString(),
                onValueChange = { v ->
                    val num = v.filter { it.isDigit() }.toIntOrNull() ?: 1
                    save(settings.copy(minStockThreshold = num.coerceAtLeast(1)))
                },
                label = { Text("Low Stock Threshold") },
                leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null) },
                supportingText = { Text("Items with stock ≤ this value show as low stock") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            SettingsToggle(
                checked = settings.showGeneratedTime,
                onCheckedChange = { save(settings.copy(showGeneratedTime = it)) },
                label = "Generated Timestamp",
                description = "Show when the report was generated",
                icon = Icons.Filled.AccessTime
            )

            Divider()
            Text("Sub-Label Grouping", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            SettingsToggle(
                checked = settings.enableSubGrouping,
                onCheckedChange = { save(settings.copy(enableSubGrouping = it)) },
                label = "Auto-group sub-labels",
                description = "Group 'Product MS' as 'Product' with sub-label 'MS'",
                icon = Icons.Filled.AccountTree
            )

            if (settings.enableSubGrouping) {
                OutlinedTextField(
                    value = settings.subGroupSuffixes,
                    onValueChange = { save(settings.copy(subGroupSuffixes = it)) },
                    label = { Text("Sub-label Suffixes") },
                    leadingIcon = { Icon(Icons.Filled.Label, contentDescription = null) },
                    supportingText = { Text("Comma-separated (e.g. MS,CS,LS,S,M,L,XL)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            if (settings.showTarget) {
                OutlinedTextField(
                    value = if (settings.monthlyTarget > 0) settings.monthlyTarget.toLong().toString() else "",
                    onValueChange = {
                        val v = it.filter { c -> c.isDigit() || c == '.' }
                        save(settings.copy(monthlyTarget = v.toDoubleOrNull() ?: 0.0))
                    },
                    label = { Text("Monthly Target Amount") },
                    leadingIcon = { Text("PHP", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("PHP ") }
                )
            }

            Divider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Custom Sections", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showAddSection = true }) {
                    Icon(Icons.Filled.AddCircle, contentDescription = "Add Section", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (settings.customSections.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.TextSnippet, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("No custom sections yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Add notes like MTD breakdown, stock details, etc.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            settings.customSections.forEachIndexed { index, section ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(section.title.ifBlank { "Untitled" }, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                editSectionIndex = index
                                showAddSection = true
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                val updated = settings.customSections.toMutableList()
                                updated.removeAt(index)
                                save(settings.copy(customSections = updated))
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (section.content.isNotBlank()) {
                            Text(section.content.take(100), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (section.content.length > 100) Text("...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAddSection) {
        AddEditSectionDialog(
            initial = if (editSectionIndex >= 0) settings.customSections[editSectionIndex] else null,
            onDismiss = {
                showAddSection = false
                editSectionIndex = -1
            },
            onSave = { section ->
                val updated = settings.customSections.toMutableList()
                if (editSectionIndex >= 0) {
                    updated[editSectionIndex] = section
                } else {
                    updated.add(section)
                }
                save(settings.copy(customSections = updated))
                showAddSection = false
                editSectionIndex = -1
            }
        )
    }
}

@Composable
private fun SettingsToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Medium)
                Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun AddEditSectionDialog(
    initial: ReportCustomSection?,
    onDismiss: () -> Unit,
    onSave: (ReportCustomSection) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial != null) "Edit Section" else "Add Custom Section") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Section Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(ReportCustomSection(
                    id = initial?.id ?: UUID.randomUUID().toString().take(8),
                    title = title,
                    content = content
                ))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
