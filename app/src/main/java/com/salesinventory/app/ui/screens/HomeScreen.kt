package com.salesinventory.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salesinventory.app.R
import com.salesinventory.app.data.ComparePeriod
import com.salesinventory.app.data.ReportSettingsManager
import com.salesinventory.app.data.SalesComparison
import com.salesinventory.app.ui.theme.*
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
    val sales by viewModel.sales.collectAsState()
    val todaySalesTotal by viewModel.todaySalesTotal.collectAsState()
    val todayProfit by viewModel.todayProfit.collectAsState()
    val lowStockItems by viewModel.lowStockItems.collectAsState()
    val overallSalesTotal = remember(sales) { sales.sumOf { it.total } }
    val overallProfit = remember(sales) { sales.sumOf { it.profit } }
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
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Sales & Inventory", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Blue800,
                    titleContentColor = Color.White
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(listOf(Blue800, Blue900))
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Text(
                        "Dashboard",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Today's overview and quick actions",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Overall Sales",
                            value = "PHP ${"%.0f".format(overallSalesTotal)}",
                            icon = Icons.Filled.TrendingUp,
                            color = Color.White,
                            bgColor = Color.White.copy(alpha = 0.15f)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Overall Profit",
                            value = "PHP ${"%.0f".format(overallProfit)}",
                            icon = Icons.Filled.AccountBalance,
                            color = Color.White,
                            bgColor = Color.White.copy(alpha = 0.15f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Sales Today",
                            value = "PHP ${"%.0f".format(todaySalesTotal)}",
                            icon = Icons.Filled.TrendingUp,
                            color = Color.White,
                            bgColor = Color.White.copy(alpha = 0.15f)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Profit Today",
                            value = "PHP ${"%.0f".format(todayProfit)}",
                            icon = Icons.Filled.AccountBalance,
                            color = Color.White,
                            bgColor = Color.White.copy(alpha = 0.15f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Items",
                            value = "${inventory.size}",
                            icon = Icons.Filled.Inventory2,
                            color = Color.White,
                            bgColor = Color.White.copy(alpha = 0.15f)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Low Stock",
                            value = "${lowStockItems.size}",
                            icon = Icons.Filled.Warning,
                            color = Color.White,
                            bgColor = Color.White.copy(alpha = 0.15f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Quick Actions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Button(
                    onClick = onNavigateToScan,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue800)
                ) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("Scan Barcode to Sell", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionChip(
                        icon = Icons.Filled.List,
                        label = "Inventory",
                        onClick = onNavigateToInventory,
                        modifier = Modifier.weight(1f)
                    )
                    ActionChip(
                        icon = Icons.Filled.Assessment,
                        label = "Sales Report",
                        onClick = onNavigateToSalesReport,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionChip(
                        icon = Icons.Filled.LocalOffer,
                        label = "Discounts",
                        onClick = onNavigateToDiscount,
                        modifier = Modifier.weight(1f)
                    )
                    ActionChip(
                        icon = Icons.Filled.ShoppingCartCheckout,
                        label = "Bulk Checkout",
                        onClick = onNavigateToBulkCheckout,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionChip(
                        icon = Icons.Filled.People,
                        label = "Customers",
                        onClick = onNavigateToCustomers,
                        modifier = Modifier.weight(1f)
                    )
                    ActionChip(
                        icon = Icons.Filled.Business,
                        label = "Suppliers",
                        onClick = onNavigateToSuppliers,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (lowStockItems.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Amber50),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = Amber700, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Low Stock Alert (≤ $lowStockThreshold)",
                                    fontWeight = FontWeight.Bold,
                                    color = Amber700,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            lowStockItems.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.name, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    Text(
                                        "Stock: ${item.stock}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.stock == 0) Red700 else Amber700
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Comparative Analysis",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
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
                                    label = { Text("Year 1", fontSize = 12.sp) },
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
                                    label = { Text("Year 2", fontSize = 12.sp) },
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

                        Spacer(Modifier.height(8.dp))

                        ScrollableTabRow(
                            selectedTabIndex = comparePeriod.ordinal,
                            modifier = Modifier.fillMaxWidth(),
                            edgePadding = 0.dp
                        ) {
                            ComparePeriod.entries.forEachIndexed { index, period ->
                                Tab(
                                    selected = comparePeriod == period,
                                    onClick = { viewModel.setComparePeriod(period) },
                                    text = { Text(period.name, fontSize = 13.sp) }
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        if (comparisonData != null && comparisonData!!.comparisons.isNotEmpty()) {
                            val comps = comparisonData!!.comparisons
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Blue50),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                            Text("$compareYear1", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("PHP ${"%.0f".format(comps.sumOf { it.period1.totalSales })}", fontSize = 12.sp, color = Blue800)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                            Text("$compareYear2", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("PHP ${"%.0f".format(comps.sumOf { it.period2.totalSales })}", fontSize = 12.sp, color = Blue800)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                            val p1 = comps.sumOf { it.period1.totalSales }
                                            val p2 = comps.sumOf { it.period2.totalSales }
                                            val g = if (p1 > 0) ((p2 - p1) / p1 * 100) else 0.0
                                            Text(
                                                if (g >= 0) "+${"%.1f".format(g)}%" else "${"%.1f".format(g)}%",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (g >= 0) Green700 else Red700
                                            )
                                            Text("Change", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            comps.forEach { comp ->
                                ComparisonRow(comp = comp, periodName = comparePeriod.name.lowercase())
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = Grey600)
                                    Spacer(Modifier.width(8.dp))
                                    Text("No sales data for comparison", fontSize = 13.sp, color = Grey600)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ComparisonRow(comp: SalesComparison, periodName: String) {
    val g = comp.growthPercent
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(comp.period1.label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("PHP ${"%.0f".format(comp.period1.totalSales)}", fontSize = 12.sp, color = Blue800)
                Text("${comp.period1.transactionCount} tx", fontSize = 11.sp, color = Grey600)
            }
            Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = Grey600)
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(comp.period2.label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("PHP ${"%.0f".format(comp.period2.totalSales)}", fontSize = 12.sp, color = Blue800)
                Text("${comp.period2.transactionCount} tx", fontSize = 11.sp, color = Grey600)
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(60.dp)) {
                Text(
                    if (g >= 0) "+${"%.1f".format(g)}%" else "${"%.1f".format(g)}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (g >= 0) Green700 else Red700
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
    color: Color,
    bgColor: Color = Color.White.copy(alpha = 0.1f)
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            Text(title, fontSize = 11.sp, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun ActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue800)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp)
    }
}
