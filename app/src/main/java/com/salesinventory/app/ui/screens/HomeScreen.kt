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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salesinventory.app.data.ComparePeriod
import com.salesinventory.app.data.ReportSettingsManager
import com.salesinventory.app.data.SalesComparison
import com.salesinventory.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToSalesReport: () -> Unit,
    onNavigateToDiscount: () -> Unit,
    onNavigateToBulkCheckout: () -> Unit = {},
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToSuppliers: () -> Unit = {},
) {
    val inventory by viewModel.inventory.collectAsState()
    val todaySalesTotal by viewModel.todaySalesTotal.collectAsState()
    val todayProfit by viewModel.todayProfit.collectAsState()
    val lowStockItems by viewModel.lowStockItems.collectAsState()
    val comparisonData by viewModel.comparisonData.collectAsState()
    val compareYear1 by viewModel.compareYear1.collectAsState()
    val compareYear2 by viewModel.compareYear2.collectAsState()
    val comparePeriod by viewModel.comparePeriod.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsManager = remember { ReportSettingsManager(context) }
    val lowStockThreshold = remember { settingsManager.load().minStockThreshold }

    var showYearPicker1 by remember { mutableStateOf(false) }
    var showYearPicker2 by remember { mutableStateOf(false) }
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val years = (currentYear - 5..currentYear).toList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales & Inventory") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
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
            Text("Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Items",
                    value = "${inventory.size}",
                    icon = Icons.Filled.List,
                    color = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Today Sales",
                    value = "PHP ${"%.2f".format(todaySalesTotal)}",
                    icon = Icons.Filled.ShoppingCart,
                    color = Color(0xFF2E7D32)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Today Profit",
                    value = "PHP ${"%.2f".format(todayProfit)}",
                    icon = Icons.Filled.TrendingUp,
                    color = Color(0xFF1565C0)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Low Stock",
                    value = "${lowStockItems.size}",
                    icon = Icons.Filled.Warning,
                    color = Color(0xFFF57F17)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Button(
                onClick = onNavigateToScan,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Scan Barcode to Sell", fontSize = 16.sp)
            }

            OutlinedButton(
                onClick = onNavigateToInventory,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Filled.List, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Manage Inventory")
            }

            OutlinedButton(
                onClick = onNavigateToSalesReport,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Filled.List, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("View Sales History")
            }

            OutlinedButton(
                onClick = onNavigateToDiscount,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Filled.Star, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Manage Discounts")
            }

            OutlinedButton(
                onClick = onNavigateToBulkCheckout,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Filled.ShoppingCartCheckout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Bulk Checkout")
            }

            OutlinedButton(
                onClick = onNavigateToCustomers,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Filled.People, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Manage Customers")
            }

            OutlinedButton(
                onClick = onNavigateToSuppliers,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Filled.Business, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Manage Suppliers")
            }

            if (lowStockItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Low Stock Alert (≤ $lowStockThreshold)",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF57F17)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        lowStockItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.name, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text(
                                    "Stock: ${item.stock}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.stock == 0) Color.Red else Color(0xFFF57F17)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            Text("Comparative Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = showYearPicker1,
                    onExpandedChange = { showYearPicker1 = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "$compareYear1",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Year 1") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showYearPicker1) },
                        modifier = Modifier.menuAnchor(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(expanded = showYearPicker1, onDismissRequest = { showYearPicker1 = false }) {
                        years.forEach { year ->
                            DropdownMenuItem(
                                text = { Text("$year") },
                                onClick = { viewModel.setCompareYear1(year); showYearPicker1 = false }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = showYearPicker2,
                    onExpandedChange = { showYearPicker2 = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "$compareYear2",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Year 2") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showYearPicker2) },
                        modifier = Modifier.menuAnchor(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(expanded = showYearPicker2, onDismissRequest = { showYearPicker2 = false }) {
                        years.forEach { year ->
                            DropdownMenuItem(
                                text = { Text("$year") },
                                onClick = { viewModel.setCompareYear2(year); showYearPicker2 = false }
                            )
                        }
                    }
                }
            }

            ScrollableTabRow(
                selectedTabIndex = comparePeriod.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                ComparePeriod.entries.forEachIndexed { index, period ->
                    Tab(
                        selected = comparePeriod == period,
                        onClick = { viewModel.setComparePeriod(period) },
                        text = { Text(period.name) }
                    )
                }
            }

            if (comparisonData != null && comparisonData!!.comparisons.isNotEmpty()) {
                val comps = comparisonData!!.comparisons

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("$compareYear1", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Total: PHP ${"%.0f".format(comps.sumOf { it.period1.totalSales })}", fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("$compareYear2", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Total: PHP ${"%.0f".format(comps.sumOf { it.period2.totalSales })}", fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                val p1 = comps.sumOf { it.period1.totalSales }
                                val p2 = comps.sumOf { it.period2.totalSales }
                                val g = if (p1 > 0) ((p2 - p1) / p1 * 100) else 0.0
                                Text(
                                    if (g >= 0) "+${"%.1f".format(g)}%" else "${"%.1f".format(g)}%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (g >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                )
                                Text("Change", fontSize = 12.sp)
                            }
                        }
                    }
                }

                comps.forEach { comp ->
                    ComparisonRow(comp = comp, periodName = comparePeriod.name.lowercase())
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("No sales data available for comparison", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ComparisonRow(comp: SalesComparison, periodName: String) {
    val g = comp.growthPercent
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(comp.period1.label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PHP ${"%.0f".format(comp.period1.totalSales)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Text("(${comp.period1.transactionCount} tx)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(comp.period2.label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PHP ${"%.0f".format(comp.period2.totalSales)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Text("(${comp.period2.transactionCount} tx)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(60.dp)) {
                Text(
                    if (g >= 0) "+${"%.1f".format(g)}%" else "${"%.1f".format(g)}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (g >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
