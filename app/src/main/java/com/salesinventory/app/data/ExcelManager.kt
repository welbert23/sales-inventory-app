package com.salesinventory.app.data

import android.content.Context
import android.net.Uri
import android.os.Environment
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
        const val PREF_MIGRATED = "data_migrated_to_json"
    }

    private val prefs = context.getSharedPreferences("excel_prefs", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun hasMigrated(): Boolean = prefs.getBoolean(PREF_MIGRATED, false)

    fun markMigrated() = prefs.edit().putBoolean(PREF_MIGRATED, true).apply()

    // ----- EXCEL DATA IMPORT (for migration) -----

    fun importExcelToJson(appStorage: AppStorage) {
        try {
            val file = getFile()
            if (!file.exists()) return
            val wb = XSSFWorkbook(FileInputStream(file))
            importInventorySheet(wb, appStorage)
            importSalesSheet(wb, appStorage)
            importDiscountsSheet(wb, appStorage)
            importCustomersSheet(wb, appStorage)
            importSuppliersSheet(wb, appStorage)
            importCreditPaymentsSheet(wb, appStorage)
            wb.close()
            markMigrated()
        } catch (_: Exception) {}
    }

    private fun importInventorySheet(wb: XSSFWorkbook, storage: AppStorage) {
        val sheet = wb.getSheet(SHEET_INVENTORY) ?: return
        val h = getHeaderMap(sheet)
        if (h.isEmpty()) return
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            storage.addOrUpdateInventory(rowToInventory(row, h))
        }
    }

    private fun importSalesSheet(wb: XSSFWorkbook, storage: AppStorage) {
        val sheet = wb.getSheet(SHEET_SALES) ?: return
        val h = getHeaderMap(sheet)
        if (h.isEmpty()) return
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            storage.recordSale(rowToSale(row, h))
        }
    }

    private fun importDiscountsSheet(wb: XSSFWorkbook, storage: AppStorage) {
        val sheet = wb.getSheet(SHEET_DISCOUNTS) ?: return
        val h = getHeaderMap(sheet)
        if (h.isEmpty()) return
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            storage.addOrUpdateDiscount(rowToDiscount(row, h))
        }
    }

    private fun importCustomersSheet(wb: XSSFWorkbook, storage: AppStorage) {
        val sheet = wb.getSheet(SHEET_CUSTOMERS) ?: return
        val h = getHeaderMap(sheet)
        if (h.isEmpty()) return
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            storage.addOrUpdateCustomer(rowToCustomer(row, h))
        }
    }

    private fun importSuppliersSheet(wb: XSSFWorkbook, storage: AppStorage) {
        val sheet = wb.getSheet(SHEET_SUPPLIERS) ?: return
        val h = getHeaderMap(sheet)
        if (h.isEmpty()) return
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            storage.addOrUpdateSupplier(rowToSupplier(row, h))
        }
    }

    private fun importCreditPaymentsSheet(wb: XSSFWorkbook, storage: AppStorage) {
        val sheet = wb.getSheet(SHEET_CREDIT_PAYMENTS) ?: return
        val h = getHeaderMap(sheet)
        if (h.isEmpty()) return
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            storage.recordCreditPayment(rowToCreditPayment(row, h))
        }
    }

    // ----- EXCEL EXPORT (from JSON to Excel for backup) -----

    fun exportToExcel(appStorage: AppStorage): Uri? {
        val wb = XSSFWorkbook()
        exportInventorySheet(wb, appStorage)
        exportSalesSheet(wb, appStorage)
        exportDiscountsSheet(wb, appStorage)
        exportCustomersSheet(wb, appStorage)
        exportSuppliersSheet(wb, appStorage)
        exportCreditPaymentsSheet(wb, appStorage)
        return try {
            val file = getFile()
            wb.write(FileOutputStream(file))
            Uri.fromFile(file)
        } catch (_: Exception) { null }
        finally { wb.close() }
    }

    private fun exportInventorySheet(wb: XSSFWorkbook, storage: AppStorage) {
        val sheet = wb.createSheet(SHEET_INVENTORY)
        val h = listOf("Barcode", "Product Name", "Category", "Price", "Cost Price", "Stock", "Unit", "Size", "Color", "Supplier ID", "Sub Label", "Min Stock", "Image URI")
        h.forEachIndexed { i, it -> sheet.createRow(0).createCell(i).setCellValue(it) }
        storage.getAllInventory().forEachIndexed { idx, item ->
            val r = sheet.createRow(idx + 1)
            r.createCell(0).setCellValue(item.barcode)
            r.createCell(1).setCellValue(item.name)
            r.createCell(2).setCellValue(item.category)
            r.createCell(3).setCellValue(item.price)
            r.createCell(4).setCellValue(item.costPrice)
            r.createCell(5).setCellValue(item.stock.toDouble())
            r.createCell(6).setCellValue(item.unit)
            r.createCell(7).setCellValue(item.size)
            r.createCell(8).setCellValue(item.color)
            r.createCell(9).setCellValue(item.supplierId)
            r.createCell(10).setCellValue(item.subLabel)
            r.createCell(11).setCellValue(item.minStock.toDouble())
            r.createCell(12).setCellValue(item.imageUri)
        }
    }

    private fun exportSalesSheet(wb: XSSFWorkbook, storage: AppStorage) {
        val sheet = wb.createSheet(SHEET_SALES)
        val h = listOf("ID", "Date", "Barcode", "Product Name", "Quantity", "Unit Price", "Cost Price", "Disc%", "Disc Amt", "Subtotal", "Total", "Customer ID", "Payment Type", "Is Credit", "Transaction ID")
        h.forEachIndexed { i, it -> sheet.createRow(0).createCell(i).setCellValue(it) }
        storage.getAllSales().forEachIndexed { idx, s ->
            val r = sheet.createRow(idx + 1)
            r.createCell(0).setCellValue(s.id)
            r.createCell(1).setCellValue(s.date)
            r.createCell(2).setCellValue(s.barcode)
            r.createCell(3).setCellValue(s.productName)
            r.createCell(4).setCellValue(s.quantity.toDouble())
            r.createCell(5).setCellValue(s.unitPrice)
            r.createCell(6).setCellValue(s.costPrice)
            r.createCell(7).setCellValue(s.discountPercent)
            r.createCell(8).setCellValue(s.discountAmount)
            r.createCell(9).setCellValue(s.subtotal)
            r.createCell(10).setCellValue(s.total)
            r.createCell(11).setCellValue(s.customerId)
            r.createCell(12).setCellValue(s.paymentType.name)
            r.createCell(13).setCellValue(if (s.isCredit) "Y" else "N")
            r.createCell(14).setCellValue(s.transactionId)
        }
    }

    private fun exportDiscountsSheet(wb: XSSFWorkbook, storage: AppStorage) {
        val sheet = wb.createSheet(SHEET_DISCOUNTS)
        val h = listOf("ID", "Name", "Type (P=Percent/F=Fixed)", "Value", "Active (Y/N)")
        h.forEachIndexed { i, it -> sheet.createRow(0).createCell(i).setCellValue(it) }
        storage.getDiscounts().forEachIndexed { idx, d ->
            val r = sheet.createRow(idx + 1)
            r.createCell(0).setCellValue(d.id)
            r.createCell(1).setCellValue(d.name)
            r.createCell(2).setCellValue(if (d.type == DiscountType.FIXED_AMOUNT) "F" else "P")
            r.createCell(3).setCellValue(d.value)
            r.createCell(4).setCellValue(if (d.isActive) "Y" else "N")
        }
    }

    private fun exportCustomersSheet(wb: XSSFWorkbook, storage: AppStorage) {
        val sheet = wb.createSheet(SHEET_CUSTOMERS)
        val h = listOf("ID", "Name", "Phone", "Email", "Address", "Credit Balance", "Total Purchases", "Notes")
        h.forEachIndexed { i, it -> sheet.createRow(0).createCell(i).setCellValue(it) }
        storage.getAllCustomers().forEachIndexed { idx, c ->
            val r = sheet.createRow(idx + 1)
            r.createCell(0).setCellValue(c.id)
            r.createCell(1).setCellValue(c.name)
            r.createCell(2).setCellValue(c.phone)
            r.createCell(3).setCellValue(c.email)
            r.createCell(4).setCellValue(c.address)
            r.createCell(5).setCellValue(c.creditBalance)
            r.createCell(6).setCellValue(c.totalPurchases)
            r.createCell(7).setCellValue(c.notes)
        }
    }

    private fun exportSuppliersSheet(wb: XSSFWorkbook, storage: AppStorage) {
        val sheet = wb.createSheet(SHEET_SUPPLIERS)
        val h = listOf("ID", "Name", "Contact Person", "Phone", "Email", "Address", "Notes")
        h.forEachIndexed { i, it -> sheet.createRow(0).createCell(i).setCellValue(it) }
        storage.getAllSuppliers().forEachIndexed { idx, s ->
            val r = sheet.createRow(idx + 1)
            r.createCell(0).setCellValue(s.id)
            r.createCell(1).setCellValue(s.name)
            r.createCell(2).setCellValue(s.contactPerson)
            r.createCell(3).setCellValue(s.phone)
            r.createCell(4).setCellValue(s.email)
            r.createCell(5).setCellValue(s.address)
            r.createCell(6).setCellValue(s.notes)
        }
    }

    private fun exportCreditPaymentsSheet(wb: XSSFWorkbook, storage: AppStorage) {
        val sheet = wb.createSheet(SHEET_CREDIT_PAYMENTS)
        val h = listOf("ID", "Customer ID", "Amount", "Date", "Notes")
        h.forEachIndexed { i, it -> sheet.createRow(0).createCell(i).setCellValue(it) }
        storage.getAllCreditPayments().forEachIndexed { idx, p ->
            val r = sheet.createRow(idx + 1)
            r.createCell(0).setCellValue(p.id)
            r.createCell(1).setCellValue(p.customerId)
            r.createCell(2).setCellValue(p.amount)
            r.createCell(3).setCellValue(p.date)
            r.createCell(4).setCellValue(p.notes)
        }
    }

    // ----- IMPORT EXCEL FILE (user-initiated) -----

    fun importInventoryFromUri(uri: Uri): Int {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return 0
            val importWb = XSSFWorkbook(inputStream)
            val sheet = importWb.getSheet(SHEET_INVENTORY) ?: return 0
            val h = getHeaderMap(sheet)
            if (h.isEmpty()) { importWb.close(); inputStream.close(); return 0 }
            val items = mutableListOf<InventoryItem>()
            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                val item = rowToInventory(row, h)
                if (item.barcode.isNotBlank() && item.name.isNotBlank()) items.add(item)
            }
            importWb.close()
            inputStream.close()
            items.size
        } catch (e: Exception) { 0 }
    }

    // ----- FILE HELPERS -----

    private fun getFile(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        return File(dir, INVENTORY_FILE)
    }

    // ----- ROW MAPPERS (for import/export) -----

    private val INVENTORY_HEADERS = listOf("Barcode", "Product Name", "Category", "Price", "Cost Price", "Stock", "Unit", "Size", "Color", "Supplier ID", "Sub Label", "Min Stock", "Image URI")

    private fun getHeaderMap(sheet: Sheet): Map<String, Int> {
        val header = sheet.getRow(0) ?: return emptyMap()
        val map = mutableMapOf<String, Int>()
        for (i in 0..header.lastCellNum - 1) {
            val cell = header.getCell(i) ?: continue
            map[getCellString(cell)] = i
        }
        return map
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

    private fun rowToCreditPayment(row: Row, h: Map<String, Int>): CreditPayment {
        return CreditPayment(
            id = getCellString(row.getCell(h["ID"] ?: -1)),
            customerId = getCellString(row.getCell(h["Customer ID"] ?: -1)),
            amount = getCellNumeric(row.getCell(h["Amount"] ?: -1)),
            date = getCellString(row.getCell(h["Date"] ?: -1)),
            notes = getCellString(row.getCell(h["Notes"] ?: -1))
        )
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
            CellType.FORMULA -> try { cell.stringCellValue } catch (_: Exception) { try { cell.numericCellValue.toString() } catch (_: Exception) { "" } }
            else -> ""
        }
    }

    private fun getCellNumeric(cell: Cell?): Double {
        if (cell == null) return 0.0
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> cell.stringCellValue.toDoubleOrNull() ?: 0.0
            CellType.FORMULA -> try { cell.numericCellValue } catch (_: Exception) { 0.0 }
            else -> 0.0
        }
    }
}
