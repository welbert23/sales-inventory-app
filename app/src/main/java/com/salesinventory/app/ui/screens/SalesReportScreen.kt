package com.salesinventory.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.salesinventory.app.data.InventoryItem
import com.salesinventory.app.data.ReportSettings
import com.salesinventory.app.data.ReportSettingsManager
import com.salesinventory.app.data.SaleRecord
import com.salesinventory.app.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesReportScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val sales by viewModel.sales.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    val context = LocalContext.current
    val settingsManager = remember { ReportSettingsManager(context) }
    val settings by remember { mutableStateOf(settingsManager.load()) }

    var filterOption by remember { mutableStateOf(0) }
    val filterOptions = listOf("All", "Today", "This Week", "This Month")

    val filteredSales = when (filterOption) {
        1 -> {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            sales.filter { it.date.startsWith(today) }
        }
        2 -> {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            val weekStart = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            sales.filter { it.date >= weekStart }
        }
        3 -> {
            val month = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            sales.filter { it.date.startsWith(month) }
        }
        else -> sales
    }

    val totalSales = filteredSales.sumOf { it.total }
    val totalItems = filteredSales.sumOf { it.quantity }

    var showShareDialog by remember { mutableStateOf(false) }
    var shareText by remember { mutableStateOf("") }

    val reportText = remember(filteredSales, totalSales, totalItems, inventory, settings) {
        buildReportText(sales, filteredSales, totalSales, totalItems, inventory, settings)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Report Settings")
                    }
                    IconButton(onClick = {
                        val uri = viewModel.getPdfUri(context)
                        if (uri != null) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share PDF Report"))
                        }
                    }) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export PDF")
                    }
                    IconButton(onClick = {
                        shareText = reportText
                        showShareDialog = true
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share Report")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sales Summary", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PHP ${"%.2f".format(totalSales)}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Total Sales", fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$totalItems", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Items Sold", fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${filteredSales.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Transactions", fontSize = 12.sp)
                        }
                    }
                }
            }

            ScrollableTabRow(
                selectedTabIndex = filterOption,
                modifier = Modifier.fillMaxWidth()
            ) {
                filterOptions.forEachIndexed { index, label ->
                    Tab(
                        selected = filterOption == index,
                        onClick = { filterOption = index },
                        text = { Text(label) }
                    )
                }
            }

            if (filteredSales.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.List, contentDescription = "Reports", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text("No sales records", style = MaterialTheme.typography.titleMedium)
                        Text("Start scanning items to record sales", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredSales.sortedByDescending { it.date }, key = { it.id }) { sale ->
                        SaleCard(sale = sale)
                    }
                }
            }
        }
    }

    if (showShareDialog) {
        Dialog(
            onDismissRequest = { showShareDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = { Text("Edit Report Text") },
                        navigationIcon = {
                            IconButton(onClick = { showShareDialog = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            Button(onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    putExtra(Intent.EXTRA_SUBJECT, "Sales Report")
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Sales Report"))
                                showShareDialog = false
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    OutlinedTextField(
                        value = shareText,
                        onValueChange = { shareText = it },
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SaleCard(sale: SaleRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sale.productName, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text("PHP ${"%.2f".format(sale.total)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(4.dp))
                Row {
                    Text("Qty: ${sale.quantity} ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("@ PHP ${"%.2f".format(sale.unitPrice)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(sale.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (sale.discountAmount > 0) {
                        Text("Disc: -PHP ${"%.2f".format(sale.discountAmount)}", fontSize = 11.sp, color = androidx.compose.ui.graphics.Color(0xFFF57F17))
                    }
                }
            }
        }
    }
}

private fun splitSubLabel(name: String, suffixes: Set<String>): Pair<String, String> {
    val words = name.trim().split(" ")
    if (words.size >= 2) {
        val last = words.last()
        if (last.uppercase() in suffixes) {
            return words.dropLast(1).joinToString(" ") to last.uppercase()
        }
    }
    return name to ""
}

private fun groupSalesByBase(
    sales: List<SaleRecord>,
    suffixes: Set<String>,
    invSubLabels: Map<String, String> = emptyMap()
): List<Pair<String, List<Pair<String, List<SaleRecord>>>>> {
    val withBase = sales.map { s ->
        val subFromInv = invSubLabels[s.productName]?.takeIf { it.isNotBlank() }
        val sub = subFromInv ?: splitSubLabel(s.productName, suffixes).second
        val base = if (sub.isNotBlank()) {
            s.productName.removeSuffix(" $sub").removeSuffix(" ${sub.lowercase()}").trim()
        } else {
            s.productName
        }
        Triple(if (base.isBlank()) s.productName else base, sub, s)
    }
    val grouped = withBase.groupBy { it.first }
    return grouped.map { (base, triples) ->
        val subGroups = triples.groupBy { it.second }
        val subs = subGroups.map { (sub, items) -> sub to items.map { it.third } }
        base to subs
    }
}

private fun buildReportText(
    allSales: List<SaleRecord>,
    filteredSales: List<SaleRecord>,
    totalSales: Double,
    totalItems: Int,
    inventory: List<InventoryItem>,
    settings: ReportSettings
): String {
    val sb = StringBuilder()
    val dateNow = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    val dateLabel = SimpleDateFormat("MMMM dd yyyy", Locale.getDefault()).format(Date())

    val store = settings.storeName.ifBlank { "__________________" }
    sb.appendLine("Store: $store")
    sb.appendLine("Date: $dateLabel")
    sb.appendLine()

    val suffixes = if (settings.enableSubGrouping)
        settings.subGroupSuffixes.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }.toSet()
    else emptySet()

    val invSubLabels = inventory.filter { it.subLabel.isNotBlank() }.associate { it.name to it.subLabel }

    if (settings.showDailySales && filteredSales.isNotEmpty()) {
        val grouped = groupSalesByBase(filteredSales, suffixes, invSubLabels)
        grouped.forEach { (base, subGroups) ->
            sb.appendLine(base)
            var baseTotal = 0.0
            var baseQty = 0
            subGroups.forEach { (sub, items) ->
                val subTotal = items.sumOf { it.total }
                val subQty = items.sumOf { it.quantity }
                if (sub.isNotBlank()) {
                    sb.appendLine("$sub= ${"%,.0f".format(subTotal)}/$subQty")
                }
                baseTotal += subTotal
                baseQty += subQty
            }
            sb.appendLine("Total= ${"%,.0f".format(baseTotal)}/$baseQty")
            sb.appendLine()
        }

        sb.appendLine("OVER ALL SALES= ${"%,.0f".format(totalSales)}/${filteredSales.size}")
        sb.appendLine()
    }

    if (settings.showTarget && settings.monthlyTarget > 0) {
        val month = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val mtdSalesTotal = allSales.filter { it.date.startsWith(month) }.sumOf { it.total }
        val mtdSalesCount = allSales.filter { it.date.startsWith(month) }.size
        val target = settings.monthlyTarget
        val pct = if (target > 0) (mtdSalesTotal / target) * 100 else 0.0

        sb.appendLine(" MTD= ${"%,.0f".format(mtdSalesTotal)}/$mtdSalesCount")
        sb.appendLine("TARGET = ${"%,.3f".format(target / 1000000)}M")
        sb.appendLine("% TO PLAN =${"%.0f".format(pct)}%")
        sb.appendLine()
    }

    if (settings.showMTD && allSales.isNotEmpty()) {
        val month = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val mtdSales = allSales.filter { it.date.startsWith(month) }
        val mtdTotal = mtdSales.sumOf { it.total }
        val mtdCount = mtdSales.size

        sb.appendLine("MONTH TO DATE")
        sb.appendLine()
        val grouped = groupSalesByBase(mtdSales, suffixes, invSubLabels)
        grouped.forEach { (base, subGroups) ->
            sb.appendLine(base)
            var baseTotal = 0.0
            var baseQty = 0
            subGroups.forEach { (sub, items) ->
                val subTotal = items.sumOf { it.total }
                val subQty = items.sumOf { it.quantity }
                if (sub.isNotBlank()) {
                    sb.appendLine("$sub= ${"%,.0f".format(subTotal)}/$subQty")
                }
                baseTotal += subTotal
                baseQty += subQty
            }
            sb.appendLine("Total= ${"%,.0f".format(baseTotal)}/$baseQty")
            sb.appendLine()
        }
        sb.appendLine("GT= ${"%,.0f".format(mtdTotal)}/$mtdCount")
        sb.appendLine()
    }

    if (settings.showStockLevel && inventory.isNotEmpty()) {
        sb.appendLine("STOCK LEVEL")
        sb.appendLine()
        val byCategory = inventory.groupBy { inv -> inv.category.ifBlank { "GENERAL" } }
        byCategory.forEach { (category, items) ->
            sb.appendLine("${category.uppercase()}")
            items.forEach { item ->
                sb.appendLine(item.name)
                sb.appendLine("REG= ${"%,.0f".format(item.price * item.stock)}")
                sb.appendLine()
            }
            val catTotal = items.sumOf { it.price * it.stock }
            sb.appendLine("TOTAL = ${"%,.0f".format(catTotal)}")
            sb.appendLine()
        }

        val grandTotal = inventory.sumOf { it.price * it.stock }
        sb.appendLine("GRAND TOTAL= ${"%,.0f".format(grandTotal)}")
        sb.appendLine()
    }

    settings.customSections.filter { it.title.isNotBlank() }.forEach { section ->
        sb.appendLine(section.title.uppercase())
        sb.appendLine()
        sb.appendLine(section.content)
        sb.appendLine()
    }

    if (settings.showGeneratedTime) {
        sb.appendLine("Generated: $dateNow")
    }

    sb.appendLine()
    return sb.toString()
}
