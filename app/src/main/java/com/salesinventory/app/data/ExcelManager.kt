package com.salesinventory.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ExcelManager(private val context: Context) {

    companion object {
        const val INVENTORY_FILE = "inventory.xlsx"
        const val SHEET_INVENTORY = "Inventory"
        const val SHEET_SALES = "Sales"
        const val SHEET_DISCOUNTS = "Discounts"
        const val SHEET_CUSTOMERS = "Customers"
        const val SHEET_SUPPLIERS = "Suppliers"
        const val SHEET_CREDIT_PAYMENTS = "CreditPayments"
        const val PREF_STORAGE_URI = "storage_folder_uri"
    }

    private val prefs = context.getSharedPreferences("excel_prefs", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun setStorageFolder(uri: Uri) {
        prefs.edit().putString(PREF_STORAGE_URI, uri.toString()).apply()
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {}
        migrateToStorage()
    }

    fun getStorageFolderUri(): Uri? {
        return prefs.getString(PREF_STORAGE_URI, null)?.let { Uri.parse(it) }
    }

    fun clearStorageFolder() {
        prefs.edit().remove(PREF_STORAGE_URI).apply()
    }

    private fun getDocumentFile(): DocumentFile? {
        val uri = getStorageFolderUri() ?: return null
        val tree = DocumentFile.fromTreeUri(context, uri) ?: return null
        var file = tree.findFile(INVENTORY_FILE)
        if (file == null) {
            file = tree.createFile("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", INVENTORY_FILE)
        }
        return file
    }

    private fun getFile(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        return File(dir, INVENTORY_FILE)
    }

    fun isUsingCloudStorage(): Boolean = getStorageFolderUri() != null

    fun initializeFileIfNeeded() {
        val docFile = getDocumentFile()
        if (docFile != null && docFile.exists()) return
        if (!getFile().exists()) {
            createNewWorkbook()
        }
    }

    private fun createNewWorkbook() {
        val wb = XSSFWorkbook()
        createInventorySheet(wb)
        createSalesSheet(wb)
        createDiscountsSheet(wb)
        createCustomersSheet(wb)
        createSuppliersSheet(wb)
        createCreditPaymentsSheet(wb)
        val docFile = getDocumentFile()
        if (docFile != null) {
            context.contentResolver.openOutputStream(docFile.uri)?.use { wb.write(it) }
        } else {
            wb.write(FileOutputStream(getFile()))
        }
        wb.close()
    }

    private fun migrateToStorage() {
        try {
            val localFile = getFile()
            if (!localFile.exists()) { createNewWorkbook(); return }
            val wb = XSSFWorkbook(FileInputStream(localFile))
            val docFile = getDocumentFile()
            if (docFile != null) {
                context.contentResolver.openOutputStream(docFile.uri)?.use { wb.write(it) }
            }
            wb.close()
        } catch (_: Exception) {}
    }

    private val INVENTORY_HEADERS = listOf("Barcode", "Product Name", "Category", "Price", "Cost Price", "Stock", "Unit", "Size", "Color", "Supplier ID", "Sub Label", "Min Stock", "Image URI")
    private val SALES_HEADERS = listOf("ID", "Date", "Barcode", "Product Name", "Quantity", "Unit Price", "Cost Price", "Disc%", "Disc Amt", "Subtotal", "Total", "Customer ID", "Payment Type", "Is Credit", "Transaction ID")
    private val DISCOUNT_HEADERS = listOf("ID", "Name", "Type (P=Percent/F=Fixed)", "Value", "Active (Y/N)")
    private val CUSTOMER_HEADERS = listOf("ID", "Name", "Phone", "Email", "Address", "Credit Balance", "Total Purchases", "Notes")
    private val SUPPLIER_HEADERS = listOf("ID", "Name", "Contact Person", "Phone", "Email", "Address", "Notes")
    private val CREDIT_PAYMENT_HEADERS = listOf("ID", "Customer ID", "Amount", "Date", "Notes")

    private fun createInventorySheet(wb: XSSFWorkbook) {
        if (wb.getSheet(SHEET_INVENTORY) != null) return
        val sheet = wb.createSheet(SHEET_INVENTORY)
        val header = sheet.createRow(0)
        INVENTORY_HEADERS.forEachIndexed { i, h -> header.createCell(i).setCellValue(h) }
        for (i in INVENTORY_HEADERS.indices) sheet.autoSizeColumn(i)
    }

    private fun createSalesSheet(wb: XSSFWorkbook) {
        if (wb.getSheet(SHEET_SALES) != null) return
        val sheet = wb.createSheet(SHEET_SALES)
        val header = sheet.createRow(0)
        SALES_HEADERS.forEachIndexed { i, h -> header.createCell(i).setCellValue(h) }
        for (i in SALES_HEADERS.indices) sheet.autoSizeColumn(i)
    }

    private fun createDiscountsSheet(wb: XSSFWorkbook) {
        if (wb.getSheet(SHEET_DISCOUNTS) != null) return
        val sheet = wb.createSheet(SHEET_DISCOUNTS)
        val header = sheet.createRow(0)
        DISCOUNT_HEADERS.forEachIndexed { i, h -> header.createCell(i).setCellValue(h) }
        for (i in DISCOUNT_HEADERS.indices) sheet.autoSizeColumn(i)
    }

    private fun createCustomersSheet(wb: XSSFWorkbook) {
        if (wb.getSheet(SHEET_CUSTOMERS) != null) return
        val sheet = wb.createSheet(SHEET_CUSTOMERS)
        val header = sheet.createRow(0)
        CUSTOMER_HEADERS.forEachIndexed { i, h -> header.createCell(i).setCellValue(h) }
        for (i in CUSTOMER_HEADERS.indices) sheet.autoSizeColumn(i)
    }

    private fun createSuppliersSheet(wb: XSSFWorkbook) {
        if (wb.getSheet(SHEET_SUPPLIERS) != null) return
        val sheet = wb.createSheet(SHEET_SUPPLIERS)
        val header = sheet.createRow(0)
        SUPPLIER_HEADERS.forEachIndexed { i, h -> header.createCell(i).setCellValue(h) }
        for (i in SUPPLIER_HEADERS.indices) sheet.autoSizeColumn(i)
    }

    private fun createCreditPaymentsSheet(wb: XSSFWorkbook) {
        if (wb.getSheet(SHEET_CREDIT_PAYMENTS) != null) return
        val sheet = wb.createSheet(SHEET_CREDIT_PAYMENTS)
        val header = sheet.createRow(0)
        CREDIT_PAYMENT_HEADERS.forEachIndexed { i, h -> header.createCell(i).setCellValue(h) }
        for (i in CREDIT_PAYMENT_HEADERS.indices) sheet.autoSizeColumn(i)
    }

    private fun createMissingSheets(wb: XSSFWorkbook) {
        createInventorySheet(wb)
        createSalesSheet(wb)
        createDiscountsSheet(wb)
        createCustomersSheet(wb)
        createSuppliersSheet(wb)
        createCreditPaymentsSheet(wb)
    }

    private fun createAllSheets(wb: XSSFWorkbook) {
        createInventorySheet(wb)
        createSalesSheet(wb)
        createDiscountsSheet(wb)
        createCustomersSheet(wb)
        createSuppliersSheet(wb)
        createCreditPaymentsSheet(wb)
    }

    fun syncToCloud() {
        val wb = getWorkbook()
        saveWorkbook(wb)
        wb.close()
    }

    private fun saveWorkbook(wb: XSSFWorkbook) {
        val docFile = getDocumentFile()
        if (docFile != null) {
            context.contentResolver.openOutputStream(docFile.uri)?.use { wb.write(it) }
        } else {
            wb.write(FileOutputStream(getFile()))
        }
    }

    fun getAllInventory(): List<InventoryItem> {
        val wb = getWorkbook()
        val sheet = wb.getSheet(SHEET_INVENTORY) ?: return emptyList()
        val headerMap = getHeaderMap(sheet)
        val items = mutableListOf<InventoryItem>()
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            items.add(rowToInventory(row, headerMap))
        }
        wb.close()
        return items
    }

    fun getItemByBarcode(barcode: String): InventoryItem? {
        val wb = getWorkbook()
        val sheet = wb.getSheet(SHEET_INVENTORY) ?: return null
        val headerMap = getHeaderMap(sheet)
        val bcIdx = headerMap["Barcode"] ?: return null
        var found: InventoryItem? = null
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val cell = row.getCell(bcIdx) ?: continue
            if (getCellString(cell) == barcode) {
                found = rowToInventory(row, headerMap)
                break
            }
        }
        wb.close()
        return found
    }

    fun addOrUpdateInventory(item: InventoryItem) {
        val wb = getWorkbook()
        val sheet = wb.getSheet(SHEET_INVENTORY) ?: return
        val headerMap = getHeaderMap(sheet)
        val bcIdx = headerMap["Barcode"] ?: return
        var found = false
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val cell = row.getCell(bcIdx) ?: continue
            if (getCellString(cell) == item.barcode) {
                updateRowFromInventory(row, item, headerMap)
                found = true
                break
            }
        }
        if (!found) {
            val newRow = sheet.createRow(sheet.lastRowNum + 1)
            updateRowFromInventory(newRow, item, headerMap)
        }
        saveWorkbook(wb)
        wb.close()
    }

    fun updateStock(barcode: String, newStock: Int) {
        val wb = getWorkbook()
        val sheet = wb.getSheet(SHEET_INVENTORY) ?: return
        val headerMap = getHeaderMap(sheet)
        val bcIdx = headerMap["Barcode"] ?: return
        val stockIdx = headerMap["Stock"] ?: return
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val cell = row.getCell(bcIdx) ?: continue
            if (getCellString(cell) == barcode) {
                val stockCell = row.getCell(stockIdx) ?: row.createCell(stockIdx)
                stockCell.setCellValue(newStock.toDouble())
                break
            }
        }
        saveWorkbook(wb)
        wb.close()
    }

    fun removeInventoryItem(barcode: String) {
        val wb = getWorkbook()
        val sheet = wb.getSheet(SHEET_INVENTORY) ?: return
        val headerMap = getHeaderMap(sheet)
        val bcIdx = headerMap["Barcode"] ?: return
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val cell = row.getCell(bcIdx) ?: continue
            if (getCellString(cell) == barcode) {
                sheet.removeRow(row)
                break
            }
        }
        saveWorkbook(wb)
        wb.close()
    }

    fun recordSale(sale: SaleRecord) {
        val wb = getWorkbook()
        val sheet = wb.getSheet(SHEET_SALES) ?: return
        val headerMap = getHeaderMap(sheet)
        val newRow = sheet.createRow(sheet.lastRowNum + 1)
        headerMap["ID"]?.let { newRow.createCell(it).setCellValue(sale.id) }
        headerMap["Date"]?.let { newRow.createCell(it).setCellValue(sale.date) }
        headerMap["Barcode"]?.let { newRow.createCell(it).setCellValue(sale.barcode) }
        headerMap["Product Name"]?.let { newRow.createCell(it).setCellValue(sale.productName) }
        headerMap["Quantity"]?.let { newRow.createCell(it).setCellValue(sale.quantity.toDouble()) }
        headerMap["Unit Price"]?.let { newRow.createCell(it).setCellValue(sale.unitPrice) }
        headerMap["Cost Price"]?.let { newRow.createCell(it).setCellValue(sale.costPrice) }
        headerMap["Disc%"]?.let { newRow.createCell(it).setCellValue(sale.discountPercent) }
        headerMap["Disc Amt"]?.let { newRow.createCell(it).setCellValue(sale.discountAmount) }
        headerMap["Subtotal"]?.let { newRow.createCell(it).setCellValue(sale.subtotal) }
        headerMap["Total"]?.let { newRow.createCell(it).setCellValue(sale.total) }
        headerMap["Customer ID"]?.let { newRow.createCell(it).setCellValue(sale.customerId) }
        headerMap["Payment Type"]?.let { newRow.createCell(it).setCellValue(sale.paymentType.name) }
        headerMap["Is Credit"]?.let { newRow.createCell(it).setCellValue(if (sale.isCredit) "Y" else "N") }
        headerMap["Transaction ID"]?.let { newRow.createCell(it).setCellValue(sale.transactionId) }
        saveWorkbook(wb)
        wb.close()
    }

    fun getAllSales(): List<SaleRecord> {
        val wb = getWorkbook()
        val sheet = wb.getSheet(SHEET_SALES) ?: return emptyList()
        val headerMap = getHeaderMap(sheet)
        val sales = mutableListOf<SaleRecord>()
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            sales.add(rowToSale(row, headerMap))
        }
        wb.close()
        return sales
    }

    fun getTodaySales(): List<SaleRecord> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return getAllSales().filter { it.date.startsWith(today) }
    }

    fun getDiscounts(): List<Discount> {
        val wb = getWorkbook()
        val sheet = wb.getSheet(SHEET_DISCOUNTS) ?: return emptyList()
        val headerMap = getHeaderMap(sheet)
        val discounts = mutableListOf<Discount>()
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            discounts.add(rowToDiscount(row, headerMap))
        }
        wb.close()
        return discounts
    }

    fun addOrUpdateDiscount(discount: Discount) {
        val wb = getWorkbook()
        val sheet = wb.getSheet(SHEET_DISCOUNTS) ?: return
        val headerMap = getHeaderMap(sheet)
        val idIdx = headerMap["ID"] ?: return
        var found = false
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val cell = row.getCell(idIdx) ?: continue
            if (getCellString(cell) == discount.id) {
                updateRowFromDiscount(row, discount, headerMap)
                found = true
                break
            }
        }
        if (!found) {
            val newRow = sheet.createRow(sheet.lastRowNum + 1)
            updateRowFromDiscount(newRow, discount, headerMap)
        }
        saveWorkbook(wb)
        wb.close()
    }

    fun removeDiscount(id: String) {
        val wb = getWorkbook()
        val sheet = wb.getSheet(SHEET_DISCOUNTS) ?: return
        val headerMap = getHeaderMap(sheet)
        val idIdx = headerMap["ID"] ?: return
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val cell = row.getCell(idIdx) ?: continue
            if (getCellString(cell) == id) {
                sheet.removeRow(row)
                break
            }
        }
        saveWorkbook(wb)
        wb.close()
    }

    // ----- CUSTOMERS -----

    fun getAllCustomers(): List<Customer> {
        val wb = getWorkbook()
        val sheet = wb.getSheet(SHEET_CUSTOMERS) ?: return emptyList()
        val headerMap = getHeaderMap(sheet)
        val customers = mutableListOf<Customer>()
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            customers.add(rowToCustomer(row, headerMap))
        }
        wb.close()
        return customers
    }

    fun getCustomerById(id: String): Customer? {
        return getAllCustomers().find { it.id == id }
    }

    fun addOrUpdateCustomer(customer: Customer) {
        val wb = getWorkbook()
        val sheet = wb.getSheet(SHEET_CUSTOMERS)
            ?: run { wb.close(); return }
        val headerMap = getHeaderMap(sheet)
        val idIdx = headerMap["ID"] ?: return
        var found = false
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val cell = row.getCell(idIdx) ?: continue
            if (getCellString(cell) == customer.id) {
                updateRowFromCustomer(row, customer, headerMap)
                found = true
                break
            }
        }
        if (!found) {
            val newRow = sheet.createRow(sheet.lastRowNum + 1)
            updateRowFromCustomer(newRow, customer, headerMap)
        }
        saveWorkbook(wb)
        wb.close()
    }

    fun removeCustomer(id: String) {
        val wb = getWorkbook()
        val sheet = wb.getSheet(SHEET_CUSTOMERS) ?: return
        val headerMap = getHeaderMap(sheet)
        val idIdx = headerMap["ID"] ?: return
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val cell = row.getCell(idIdx) ?: continue
            if (getCellString(cell) == id) {
                sheet.removeRow(row)
                break
            }
        }
        saveWorkbook(wb)
        wb.close()
    }

    // ----- SUPPLIERS -----

    fun getAllSuppliers(): List<Supplier> {
        val wb = getWorkbook()
        val sheet = wb.getSheet(SHEET_SUPPLIERS) ?: return emptyList()
        val headerMap = getHeaderMap(sheet)
        val suppliers = mutableListOf<Supplier>()
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            suppliers.add(rowToSupplier(row, headerMap))
        }
        wb.close()
        return suppliers
    }

    fun addOrUpdateSupplier(supplier: Supplier) {
        val wb = getWorkbook()
        val sheet = wb.getSheet(SHEET_SUPPLIERS)
            ?: run { wb.close(); return }
        val headerMap = getHeaderMap(sheet)
        val idIdx = headerMap["ID"] ?: return
        var found = false
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val cell = row.getCell(idIdx) ?: continue
            if (getCellString(cell) == supplier.id) {
                updateRowFromSupplier(row, supplier, headerMap)
                found = true
                break
            }
        }
        if (!found) {
            val newRow = sheet.createRow(sheet.lastRowNum + 1)
            updateRowFromSupplier(newRow, supplier, headerMap)
        }
        saveWorkbook(wb)
        wb.close()
    }

    fun removeSupplier(id: String) {
        val wb = getWorkbook()
        val sheet = wb.getSheet(SHEET_SUPPLIERS) ?: return
        val headerMap = getHeaderMap(sheet)
        val idIdx = headerMap["ID"] ?: return
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val cell = row.getCell(idIdx) ?: continue
            if (getCellString(cell) == id) {
                sheet.removeRow(row)
                break
            }
        }
        saveWorkbook(wb)
        wb.close()
    }

    // ----- CREDIT PAYMENTS -----

    fun recordCreditPayment(payment: CreditPayment) {
        val wb = getWorkbook()
        val sheet = wb.getSheet(SHEET_CREDIT_PAYMENTS)
            ?: run { wb.close(); return }
        val headerMap = getHeaderMap(sheet)
        val newRow = sheet.createRow(sheet.lastRowNum + 1)
        headerMap["ID"]?.let { newRow.createCell(it).setCellValue(payment.id) }
        headerMap["Customer ID"]?.let { newRow.createCell(it).setCellValue(payment.customerId) }
        headerMap["Amount"]?.let { newRow.createCell(it).setCellValue(payment.amount) }
        headerMap["Date"]?.let { newRow.createCell(it).setCellValue(payment.date) }
        headerMap["Notes"]?.let { newRow.createCell(it).setCellValue(payment.notes) }
        saveWorkbook(wb)
        wb.close()

        val customer = getCustomerById(payment.customerId)
        if (customer != null) {
            val updated = customer.copy(creditBalance = customer.creditBalance - payment.amount)
            addOrUpdateCustomer(updated)
        }
    }

    fun getPaymentsByCustomer(customerId: String): List<CreditPayment> {
        val wb = getWorkbook()
        val sheet = wb.getSheet(SHEET_CREDIT_PAYMENTS) ?: return emptyList()
        val headerMap = getHeaderMap(sheet)
        val payments = mutableListOf<CreditPayment>()
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val p = rowToCreditPayment(row, headerMap)
            if (p.customerId == customerId) payments.add(p)
        }
        wb.close()
        return payments
    }

    fun importFromUri(uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val wb = XSSFWorkbook(inputStream)
                val file = getFile()
                wb.write(FileOutputStream(file))
                wb.close()
                inputStream.close()
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    fun importInventoryFromUri(uri: Uri): Int {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return 0
            val importWb = XSSFWorkbook(inputStream)
            importWb.getSheet(SHEET_INVENTORY)?.let { importSheet ->
                val headerMap = getHeaderMap(importSheet)
                if (headerMap.isEmpty()) { importWb.close(); inputStream.close(); return 0 }
                val currentWb = getWorkbook()
                val currentSheet = currentWb.getSheet(SHEET_INVENTORY) ?: return 0
                val currentHeaders = getHeaderMap(currentSheet)
                var count = 0
                for (i in 1..importSheet.lastRowNum) {
                    val row = importSheet.getRow(i) ?: continue
                    val item = rowToInventory(row, headerMap)
                    if (item.barcode.isNotBlank() && item.name.isNotBlank()) {
                        var found = false
                        val bcIdx = currentHeaders["Barcode"] ?: return 0
                        for (j in 1..currentSheet.lastRowNum) {
                            val r = currentSheet.getRow(j) ?: continue
                            val c = r.getCell(bcIdx) ?: continue
                            if (getCellString(c) == item.barcode) {
                                updateRowFromInventory(r, item, currentHeaders)
                                found = true
                                count++
                                break
                            }
                        }
                        if (!found) {
                            val newRow = currentSheet.createRow(currentSheet.lastRowNum + 1)
                            updateRowFromInventory(newRow, item, currentHeaders)
                            count++
                        }
                    }
                }
                saveWorkbook(currentWb)
                currentWb.close()
                importWb.close()
                inputStream.close()
                count
            } ?: run { importWb.close(); inputStream.close(); 0 }
        } catch (e: Exception) {
            0
        }
    }

    fun getFileUri(): Uri {
        return Uri.fromFile(getFile())
    }

    fun exportFileUri(): Uri {
        val file = getFile()
        return androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
    }

    private fun rowToInventory(row: Row, h: Map<String, Int>): InventoryItem {
        return InventoryItem(
            barcode = getCellString(row.getCell(h["Barcode"] ?: -1)),
            name = getCellString(row.getCell(h["Product Name"] ?: -1)),
            category = getCellString(row.getCell(h["Category"] ?: -1)),
            price = getCellNumeric(row.getCell(h["Price"] ?: -1)),
            costPrice = getCellNumeric(row.getCell(h["Cost Price"] ?: -1)),
            stock = getCellNumeric(row.getCell(h["Stock"] ?: -1)).toInt(),
            unit = getCellString(row.getCell(h["Unit"] ?: -1)),
            size = getCellString(row.getCell(h["Size"] ?: -1)),
            color = getCellString(row.getCell(h["Color"] ?: -1)),
            supplierId = getCellString(row.getCell(h["Supplier ID"] ?: -1)),
            subLabel = getCellString(row.getCell(h["Sub Label"] ?: -1)),
            minStock = getCellNumeric(row.getCell(h["Min Stock"] ?: -1)).toInt(),
            imageUri = getCellString(row.getCell(h["Image URI"] ?: -1))
        )
    }

    private fun updateRowFromInventory(row: Row, item: InventoryItem, h: Map<String, Int>) {
        h["Barcode"]?.let { row.createCell(it).setCellValue(item.barcode) }
        h["Product Name"]?.let { row.createCell(it).setCellValue(item.name) }
        h["Category"]?.let { row.createCell(it).setCellValue(item.category) }
        h["Price"]?.let { row.createCell(it).setCellValue(item.price) }
        h["Cost Price"]?.let { row.createCell(it).setCellValue(item.costPrice) }
        h["Stock"]?.let { row.createCell(it).setCellValue(item.stock.toDouble()) }
        h["Unit"]?.let { row.createCell(it).setCellValue(item.unit) }
        h["Size"]?.let { row.createCell(it).setCellValue(item.size) }
        h["Color"]?.let { row.createCell(it).setCellValue(item.color) }
        h["Supplier ID"]?.let { row.createCell(it).setCellValue(item.supplierId) }
        h["Sub Label"]?.let { row.createCell(it).setCellValue(item.subLabel) }
        h["Min Stock"]?.let { row.createCell(it).setCellValue(item.minStock.toDouble()) }
        h["Image URI"]?.let { row.createCell(it).setCellValue(item.imageUri) }
    }

    private fun rowToSale(row: Row, h: Map<String, Int>): SaleRecord {
        return SaleRecord(
            id = getCellString(row.getCell(h["ID"] ?: -1)),
            date = getCellString(row.getCell(h["Date"] ?: -1)),
            barcode = getCellString(row.getCell(h["Barcode"] ?: -1)),
            productName = getCellString(row.getCell(h["Product Name"] ?: -1)),
            quantity = getCellNumeric(row.getCell(h["Quantity"] ?: -1)).toInt(),
            unitPrice = getCellNumeric(row.getCell(h["Unit Price"] ?: -1)),
            costPrice = getCellNumeric(row.getCell(h["Cost Price"] ?: -1)),
            discountPercent = getCellNumeric(row.getCell(h["Disc%"] ?: -1)),
            discountAmount = getCellNumeric(row.getCell(h["Disc Amt"] ?: -1)),
            subtotal = getCellNumeric(row.getCell(h["Subtotal"] ?: -1)),
            total = getCellNumeric(row.getCell(h["Total"] ?: -1)),
            customerId = getCellString(row.getCell(h["Customer ID"] ?: -1)),
            paymentType = try { PaymentType.valueOf(getCellString(row.getCell(h["Payment Type"] ?: -1))) } catch (e: Exception) { PaymentType.CASH },
            isCredit = getCellString(row.getCell(h["Is Credit"] ?: -1)).uppercase() == "Y",
            transactionId = getCellString(row.getCell(h["Transaction ID"] ?: -1))
        )
    }

    private fun rowToDiscount(row: Row, h: Map<String, Int>): Discount {
        return Discount(
            id = getCellString(row.getCell(h["ID"] ?: -1)),
            name = getCellString(row.getCell(h["Name"] ?: -1)),
            type = if (getCellString(row.getCell(h["Type (P=Percent/F=Fixed)"] ?: -1)).uppercase() == "F")
                DiscountType.FIXED_AMOUNT else DiscountType.PERCENTAGE,
            value = getCellNumeric(row.getCell(h["Value"] ?: -1)),
            isActive = getCellString(row.getCell(h["Active (Y/N)"] ?: -1)).uppercase() == "Y"
        )
    }

    private fun updateRowFromDiscount(row: Row, discount: Discount, h: Map<String, Int>) {
        h["ID"]?.let { row.createCell(it).setCellValue(discount.id) }
        h["Name"]?.let { row.createCell(it).setCellValue(discount.name) }
        h["Type (P=Percent/F=Fixed)"]?.let {
            row.createCell(it).setCellValue(if (discount.type == DiscountType.FIXED_AMOUNT) "F" else "P")
        }
        h["Value"]?.let { row.createCell(it).setCellValue(discount.value) }
        h["Active (Y/N)"]?.let { row.createCell(it).setCellValue(if (discount.isActive) "Y" else "N") }
    }

    private fun rowToCustomer(row: Row, h: Map<String, Int>): Customer {
        return Customer(
            id = getCellString(row.getCell(h["ID"] ?: -1)),
            name = getCellString(row.getCell(h["Name"] ?: -1)),
            phone = getCellString(row.getCell(h["Phone"] ?: -1)),
            email = getCellString(row.getCell(h["Email"] ?: -1)),
            address = getCellString(row.getCell(h["Address"] ?: -1)),
            creditBalance = getCellNumeric(row.getCell(h["Credit Balance"] ?: -1)),
            totalPurchases = getCellNumeric(row.getCell(h["Total Purchases"] ?: -1)),
            notes = getCellString(row.getCell(h["Notes"] ?: -1))
        )
    }

    private fun updateRowFromCustomer(row: Row, customer: Customer, h: Map<String, Int>) {
        h["ID"]?.let { row.createCell(it).setCellValue(customer.id) }
        h["Name"]?.let { row.createCell(it).setCellValue(customer.name) }
        h["Phone"]?.let { row.createCell(it).setCellValue(customer.phone) }
        h["Email"]?.let { row.createCell(it).setCellValue(customer.email) }
        h["Address"]?.let { row.createCell(it).setCellValue(customer.address) }
        h["Credit Balance"]?.let { row.createCell(it).setCellValue(customer.creditBalance) }
        h["Total Purchases"]?.let { row.createCell(it).setCellValue(customer.totalPurchases) }
        h["Notes"]?.let { row.createCell(it).setCellValue(customer.notes) }
    }

    private fun rowToSupplier(row: Row, h: Map<String, Int>): Supplier {
        return Supplier(
            id = getCellString(row.getCell(h["ID"] ?: -1)),
            name = getCellString(row.getCell(h["Name"] ?: -1)),
            contactPerson = getCellString(row.getCell(h["Contact Person"] ?: -1)),
            phone = getCellString(row.getCell(h["Phone"] ?: -1)),
            email = getCellString(row.getCell(h["Email"] ?: -1)),
            address = getCellString(row.getCell(h["Address"] ?: -1)),
            notes = getCellString(row.getCell(h["Notes"] ?: -1))
        )
    }

    private fun updateRowFromSupplier(row: Row, supplier: Supplier, h: Map<String, Int>) {
        h["ID"]?.let { row.createCell(it).setCellValue(supplier.id) }
        h["Name"]?.let { row.createCell(it).setCellValue(supplier.name) }
        h["Contact Person"]?.let { row.createCell(it).setCellValue(supplier.contactPerson) }
        h["Phone"]?.let { row.createCell(it).setCellValue(supplier.phone) }
        h["Email"]?.let { row.createCell(it).setCellValue(supplier.email) }
        h["Address"]?.let { row.createCell(it).setCellValue(supplier.address) }
        h["Notes"]?.let { row.createCell(it).setCellValue(supplier.notes) }
    }

    private fun rowToCreditPayment(row: Row, h: Map<String, Int>): CreditPayment {
        return CreditPayment(
            id = getCellString(row.getCell(h["ID"] ?: -1)),
            customerId = getCellString(row.getCell(h["Customer ID"] ?: -1)),
            amount = getCellNumeric(row.getCell(h["Amount"] ?: -1)),
            date = getCellString(row.getCell(h["Date"] ?: -1)),
            notes = getCellString(row.getCell(h["Notes"] ?: -1))
        )
    }

    private fun getWorkbook(): XSSFWorkbook {
        val file = getFile()
        return if (file.exists()) {
            try { XSSFWorkbook(FileInputStream(file)) }
            catch (e: Exception) { val wb = XSSFWorkbook(); createAllSheets(wb); wb }
        } else {
            val wb = XSSFWorkbook()
            createAllSheets(wb)
            wb.write(FileOutputStream(file))
            wb
        }
    }

    private fun getHeaderMap(sheet: Sheet): Map<String, Int> {
        val header = sheet.getRow(0) ?: return emptyMap()
        val map = mutableMapOf<String, Int>()
        for (i in 0..header.lastCellNum - 1) {
            val cell = header.getCell(i) ?: continue
            map[getCellString(cell)] = i
        }
        return map
    }

    private fun getCellString(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> {
                val v = cell.numericCellValue
                if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> try {
                cell.stringCellValue
            } catch (e: Exception) {
                try { cell.numericCellValue.toString() } catch (e2: Exception) { "" }
            }
            else -> ""
        }
    }

    private fun getCellNumeric(cell: Cell?): Double {
        if (cell == null) return 0.0
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> cell.stringCellValue.toDoubleOrNull() ?: 0.0
            CellType.FORMULA -> try { cell.numericCellValue } catch (e: Exception) { 0.0 }
            else -> 0.0
        }
    }
}
