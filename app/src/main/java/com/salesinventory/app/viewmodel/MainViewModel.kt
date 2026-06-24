package com.salesinventory.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salesinventory.app.data.*
import com.salesinventory.app.util.PdfExporter
import com.salesinventory.app.util.ReceiptItem
import com.salesinventory.app.util.BluetoothPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val excelManager = ExcelManager(application)
    private val settingsManager = ReportSettingsManager(application)

    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventory: StateFlow<List<InventoryItem>> = _inventory.asStateFlow()

    private val _sales = MutableStateFlow<List<SaleRecord>>(emptyList())
    val sales: StateFlow<List<SaleRecord>> = _sales.asStateFlow()

    private val _discounts = MutableStateFlow<List<Discount>>(emptyList())
    val discounts: StateFlow<List<Discount>> = _discounts.asStateFlow()

    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    private val _suppliers = MutableStateFlow<List<Supplier>>(emptyList())
    val suppliers: StateFlow<List<Supplier>> = _suppliers.asStateFlow()

    private val _currentDiscount = MutableStateFlow<Discount?>(null)
    val currentDiscount: StateFlow<Discount?> = _currentDiscount.asStateFlow()

    private val _scanResult = MutableStateFlow<InventoryItem?>(null)
    val scanResult: StateFlow<InventoryItem?> = _scanResult.asStateFlow()

    private val _saleQuantity = MutableStateFlow(1)
    val saleQuantity: StateFlow<Int> = _saleQuantity.asStateFlow()

    private val _saleComplete = MutableStateFlow(false)
    val saleComplete: StateFlow<Boolean> = _saleComplete.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _todaySalesTotal = MutableStateFlow(0.0)
    val todaySalesTotal: StateFlow<Double> = _todaySalesTotal.asStateFlow()

    private val _todayProfit = MutableStateFlow(0.0)
    val todayProfit: StateFlow<Double> = _todayProfit.asStateFlow()

    private val _lowStockItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val lowStockItems: StateFlow<List<InventoryItem>> = _lowStockItems.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                excelManager.initializeFileIfNeeded()
                _inventory.value = excelManager.getAllInventory()
                _sales.value = excelManager.getAllSales()
                _discounts.value = excelManager.getDiscounts()
                _customers.value = excelManager.getAllCustomers()
                _suppliers.value = excelManager.getAllSuppliers()
                _currentDiscount.value = _discounts.value.find { it.isActive }
                updateStats()
                computeComparison()
            } catch (e: Exception) {
                _error.value = "Error loading data: ${e.message}"
            }
        }
    }

    private fun updateStats() {
        val todaySales = excelManager.getTodaySales()
        _todaySalesTotal.value = todaySales.sumOf { it.total }
        _todayProfit.value = todaySales.sumOf { it.profit }
        val threshold = settingsManager.load().minStockThreshold
        _lowStockItems.value = _inventory.value.filter { it.stock <= threshold }
    }

    fun setScanResult(item: InventoryItem?) {
        _scanResult.value = item
        _saleQuantity.value = 1
        _saleComplete.value = false
    }

    fun setSaleQuantity(qty: Int) {
        _saleQuantity.value = if (qty < 1) 1 else qty
    }

    fun setCurrentDiscount(discount: Discount?) {
        _currentDiscount.value = discount
    }

    fun processSale(barcode: String, quantity: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val item = excelManager.getItemByBarcode(barcode)
                if (item == null) {
                    _error.value = "Item not found: $barcode"
                    return@launch
                }
                if (item.stock < quantity) {
                    _error.value = "Insufficient stock! Available: ${item.stock}"
                    return@launch
                }

                val discount = _currentDiscount.value
                val unitPrice = item.price
                val subtotal = unitPrice * quantity
                val discountAmt = if (discount != null && discount.isActive) {
                    when (discount.type) {
                        DiscountType.PERCENTAGE -> subtotal * discount.value / 100.0
                        DiscountType.FIXED_AMOUNT -> discount.value * quantity
                    }
                } else 0.0
                val total = subtotal - discountAmt
                val discPct = if (discount != null && discount.isActive) discount.value else 0.0

                val sale = SaleRecord(
                    id = UUID.randomUUID().toString().take(8),
                    date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                    barcode = item.barcode,
                    productName = item.name,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    costPrice = item.costPrice,
                    discountPercent = discPct,
                    discountAmount = discountAmt,
                    subtotal = subtotal,
                    total = total
                )

                excelManager.recordSale(sale)
                excelManager.updateStock(barcode, item.stock - quantity)
                _sales.value = excelManager.getAllSales()
                _inventory.value = excelManager.getAllInventory()
                _saleComplete.value = true
                updateStats()
            } catch (e: Exception) {
                _error.value = "Error processing sale: ${e.message}"
            }
        }
    }

    fun addInventoryItem(item: InventoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                excelManager.addOrUpdateInventory(item)
                _inventory.value = excelManager.getAllInventory()
                updateStats()
            } catch (e: Exception) {
                _error.value = "Error adding item: ${e.message}"
            }
        }
    }

    fun importInventoryItems(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = excelManager.importInventoryFromUri(uri)
                _inventory.value = excelManager.getAllInventory()
                updateStats()
                if (count > 0) _error.value = "Imported $count items successfully"
                else _error.value = "No items found to import"
            } catch (e: Exception) {
                _error.value = "Error importing: ${e.message}"
            }
        }
    }

    fun removeInventoryItem(barcode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                excelManager.removeInventoryItem(barcode)
                _inventory.value = excelManager.getAllInventory()
                updateStats()
            } catch (e: Exception) {
                _error.value = "Error removing item: ${e.message}"
            }
        }
    }

    fun addDiscount(discount: Discount) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                excelManager.addOrUpdateDiscount(discount)
                _discounts.value = excelManager.getDiscounts()
                _currentDiscount.value = _discounts.value.find { it.isActive }
            } catch (e: Exception) {
                _error.value = "Error adding discount: ${e.message}"
            }
        }
    }

    fun removeDiscount(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                excelManager.removeDiscount(id)
                _discounts.value = excelManager.getDiscounts()
                _currentDiscount.value = _discounts.value.find { it.isActive }
            } catch (e: Exception) {
                _error.value = "Error removing discount: ${e.message}"
            }
        }
    }

    private val _comparisonData = MutableStateFlow<ComparisonData?>(null)
    val comparisonData: StateFlow<ComparisonData?> = _comparisonData.asStateFlow()

    private val _compareYear1 = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR) - 1)
    val compareYear1: StateFlow<Int> = _compareYear1.asStateFlow()

    private val _compareYear2 = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val compareYear2: StateFlow<Int> = _compareYear2.asStateFlow()

    private val _comparePeriod = MutableStateFlow(ComparePeriod.MONTH)
    val comparePeriod: StateFlow<ComparePeriod> = _comparePeriod.asStateFlow()

    fun setCompareYear1(year: Int) { _compareYear1.value = year; computeComparison() }
    fun setCompareYear2(year: Int) { _compareYear2.value = year; computeComparison() }
    fun setComparePeriod(period: ComparePeriod) { _comparePeriod.value = period; computeComparison() }

    fun computeComparison() {
        viewModelScope.launch(Dispatchers.Default) {
            val sales = _sales.value
            val year1 = _compareYear1.value
            val year2 = _compareYear2.value
            val period = _comparePeriod.value
            if (sales.isEmpty()) { _comparisonData.value = null; return@launch }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val cal = Calendar.getInstance()
            val months = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")

            val s1 = sales.filter { s ->
                try { dateFormat.parse(s.date)?.let { cal.time = it; cal.get(Calendar.YEAR) == year1 } ?: false }
                catch (e: Exception) { false }
            }
            val s2 = sales.filter { s ->
                try { dateFormat.parse(s.date)?.let { cal.time = it; cal.get(Calendar.YEAR) == year2 } ?: false }
                catch (e: Exception) { false }
            }

            fun periodKey(sale: SaleRecord): Int {
                val d = dateFormat.parse(sale.date) ?: return 0
                cal.time = d
                return when (period) {
                    ComparePeriod.DAY -> cal.get(Calendar.DAY_OF_YEAR)
                    ComparePeriod.WEEK -> cal.get(Calendar.WEEK_OF_YEAR)
                    ComparePeriod.MONTH -> cal.get(Calendar.MONTH)
                    ComparePeriod.YEAR -> 0
                }
            }

            fun periodLabel(sale: SaleRecord): String {
                val d = dateFormat.parse(sale.date) ?: return sale.date
                cal.time = d
                return when (period) {
                    ComparePeriod.DAY -> "${months[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.DAY_OF_MONTH)}"
                    ComparePeriod.WEEK -> "Week ${cal.get(Calendar.WEEK_OF_YEAR)}"
                    ComparePeriod.MONTH -> months[cal.get(Calendar.MONTH)]
                    ComparePeriod.YEAR -> sale.date.take(4)
                }
            }

            fun summaries(sales: List<SaleRecord>): Map<Int, PeriodSummary> {
                val groups = sales.groupBy { periodKey(it) }
                val result = mutableMapOf<Int, PeriodSummary>()
                for ((key, list) in groups) {
                    val lbl = periodLabel(list.first())
                    result[key] = PeriodSummary(lbl, list.sumOf { it.total }, list.size, list.sumOf { it.quantity })
                }
                return result
            }

            val sum1 = summaries(s1)
            val sum2 = summaries(s2)
            val allKeys = (sum1.keys + sum2.keys).sorted()

            val comparisons = allKeys.map { key ->
                val p1 = sum1[key] ?: PeriodSummary("", 0.0, 0, 0)
                val p2 = sum2[key] ?: PeriodSummary("", 0.0, 0, 0)
                val lbl = p1.label.ifBlank { p2.label }
                val p1f = p1.copy(label = lbl)
                val p2f = p2.copy(label = lbl)
                val growth = if (p1f.totalSales > 0) ((p2f.totalSales - p1f.totalSales) / p1f.totalSales) * 100 else 0.0
                SalesComparison(p1f, p2f, growth)
            }

            _comparisonData.value = ComparisonData(year1, year2, period, comparisons)
        }
    }

    fun processBulkSale(items: List<CartItem>, customerId: String = "", paymentType: PaymentType = PaymentType.CASH, isCredit: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val transactionId = UUID.randomUUID().toString().take(8)
                val discount = _currentDiscount.value
                for (cart in items) {
                    val item = excelManager.getItemByBarcode(cart.barcode)
                    if (item == null) { _error.value = "Item not found: ${cart.barcode}"; return@launch }
                    if (item.stock < cart.quantity) { _error.value = "Insufficient stock for ${item.name}"; return@launch }

                    val unitPrice = item.price
                    val subtotal = unitPrice * cart.quantity
                    val discountAmt = if (discount != null && discount.isActive) {
                        when (discount.type) {
                            DiscountType.PERCENTAGE -> subtotal * discount.value / 100.0
                            DiscountType.FIXED_AMOUNT -> discount.value * cart.quantity
                        }
                    } else 0.0
                    val discPct = if (discount != null && discount.isActive) discount.value else 0.0
                    val total = subtotal - discountAmt

                    val sale = SaleRecord(
                        id = UUID.randomUUID().toString().take(8),
                        date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                        barcode = item.barcode,
                        productName = item.name,
                        quantity = cart.quantity,
                        unitPrice = unitPrice,
                        costPrice = item.costPrice,
                        discountPercent = discPct,
                        discountAmount = discountAmt,
                        subtotal = subtotal,
                        total = total,
                        customerId = customerId,
                        paymentType = paymentType,
                        isCredit = isCredit,
                        transactionId = transactionId
                    )
                    excelManager.recordSale(sale)
                    excelManager.updateStock(cart.barcode, item.stock - cart.quantity)
                }
                _sales.value = excelManager.getAllSales()
                _inventory.value = excelManager.getAllInventory()
                updateStats()
                if (isCredit && customerId.isNotBlank()) {
                    val saleTotal = items.sumOf { cart ->
                        val i = excelManager.getItemByBarcode(cart.barcode)
                        if (i != null) {
                            val sub = i.price * cart.quantity
                            val dAmt = if (discount != null && discount.isActive) {
                                when (discount.type) {
                                    DiscountType.PERCENTAGE -> sub * discount.value / 100.0
                                    DiscountType.FIXED_AMOUNT -> discount.value * cart.quantity
                                }
                            } else 0.0
                            sub - dAmt
                        } else 0.0
                    }
                    val customer = excelManager.getCustomerById(customerId)
                    if (customer != null) {
                        excelManager.addOrUpdateCustomer(
                            customer.copy(
                                creditBalance = customer.creditBalance + saleTotal,
                                totalPurchases = customer.totalPurchases + saleTotal
                            )
                        )
                        _customers.value = excelManager.getAllCustomers()
                    }
                }
                _error.value = "Sale completed! Transaction: $transactionId"
            } catch (e: Exception) {
                _error.value = "Error processing sale: ${e.message}"
            }
        }
    }

    fun addOrUpdateCustomer(customer: Customer) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                excelManager.addOrUpdateCustomer(customer)
                _customers.value = excelManager.getAllCustomers()
            } catch (e: Exception) {
                _error.value = "Error saving customer: ${e.message}"
            }
        }
    }

    fun removeCustomer(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                excelManager.removeCustomer(id)
                _customers.value = excelManager.getAllCustomers()
            } catch (e: Exception) {
                _error.value = "Error removing customer: ${e.message}"
            }
        }
    }

    fun addOrUpdateSupplier(supplier: Supplier) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                excelManager.addOrUpdateSupplier(supplier)
                _suppliers.value = excelManager.getAllSuppliers()
            } catch (e: Exception) {
                _error.value = "Error saving supplier: ${e.message}"
            }
        }
    }

    fun removeSupplier(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                excelManager.removeSupplier(id)
                _suppliers.value = excelManager.getAllSuppliers()
            } catch (e: Exception) {
                _error.value = "Error removing supplier: ${e.message}"
            }
        }
    }

    fun recordCreditPayment(payment: CreditPayment) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                excelManager.recordCreditPayment(payment)
                _customers.value = excelManager.getAllCustomers()
            } catch (e: Exception) {
                _error.value = "Error recording payment: ${e.message}"
            }
        }
    }

    fun getCustomerPayments(customerId: String): List<CreditPayment> {
        return excelManager.getPaymentsByCustomer(customerId)
    }

    // ----- PDF EXPORT -----

    fun getPdfUri(context: android.content.Context): android.net.Uri? {
        return try {
            val salesData = _sales.value
            val settings = settingsManager.load()
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

            val headers = listOf("Date", "Product", "Qty", "Price", "Total")
            val rows = salesData.map { s ->
                listOf(
                    s.date.take(10),
                    s.productName.take(20),
                    "${s.quantity}",
                    "${"%.2f".format(s.unitPrice)}",
                    "${"%.2f".format(s.total)}"
                )
            }
            val summary = listOf(
                "Total Sales" to "${"%.2f".format(salesData.sumOf { it.total })}",
                "Total Profit" to "${"%.2f".format(salesData.sumOf { it.profit })}",
                "Transactions" to "${salesData.size}"
            )
            PdfExporter.generateSalesReport(
                context, settings.storeName,
                "Sales Report (${dateFormat.format(java.util.Date())})",
                headers, rows, summary
            )
        } catch (e: Exception) {
            _error.value = "PDF error: ${e.message}"
            null
        }
    }

    // ----- BLUETOOTH -----

    fun printReceipt(transactionId: String, address: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val connected = BluetoothPrinter.connect(address)
                if (!connected) { callback(false); return@launch }

                val sales = _sales.value.filter { it.transactionId == transactionId }
                val items = sales.map { ReceiptItem(it.productName, it.quantity, it.unitPrice, it.total) }
                val total = sales.sumOf { it.total }
                val paymentType = sales.firstOrNull()?.paymentType?.displayName ?: "Cash"
                val settings = settingsManager.load()

                val result = BluetoothPrinter.printReceipt(
                    settings.storeName, items, total, paymentType, transactionId
                )
                BluetoothPrinter.disconnect()
                callback(result)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    fun printBarcodeLabel(barcode: String, address: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val item = excelManager.getItemByBarcode(barcode)
                if (item == null) { callback(false); return@launch }
                val connected = BluetoothPrinter.connect(address)
                if (!connected) { callback(false); return@launch }
                val result = BluetoothPrinter.printBarcodeLabel(barcode, item.name, item.price)
                BluetoothPrinter.disconnect()
                callback(result)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    fun getBluetoothPrinters(): List<Pair<String, String>> {
        return com.salesinventory.app.util.BluetoothPrinter.getPairedPrinters()
    }

    // ----- BACKUP -----

    fun backupToCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                excelManager.syncToCloud()
                _error.value = "Backup completed successfully"
            } catch (e: Exception) {
                _error.value = "Backup failed: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun getExcelManager(): ExcelManager = excelManager

    fun setCloudStorageFolder(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                excelManager.setStorageFolder(uri)
                loadData()
                _error.value = "Linked to cloud storage successfully"
            } catch (e: Exception) {
                _error.value = "Error linking cloud storage: ${e.message}"
            }
        }
    }

    fun removeCloudStorage() {
        viewModelScope.launch(Dispatchers.IO) {
            excelManager.clearStorageFolder()
            loadData()
            _error.value = "Using local storage only"
        }
    }

    fun isUsingCloudStorage(): Boolean = excelManager.isUsingCloudStorage()
}
