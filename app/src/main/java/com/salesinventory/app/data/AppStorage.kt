package com.salesinventory.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class AppStorage(private val context: Context) {

    private val gson = Gson()

    private val inventoryFile get() = File(context.filesDir, "inventory.json")
    private val salesFile get() = File(context.filesDir, "sales.json")
    private val discountsFile get() = File(context.filesDir, "discounts.json")
    private val customersFile get() = File(context.filesDir, "customers.json")
    private val suppliersFile get() = File(context.filesDir, "suppliers.json")
    private val creditPaymentsFile get() = File(context.filesDir, "credit_payments.json")

    private fun <T> readList(file: File, type: java.lang.reflect.Type): List<T> {
        if (!file.exists()) return emptyList()
        return try {
            val json = file.readText()
            if (json.isBlank()) emptyList() else gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    private fun <T> writeList(file: File, list: List<T>) {
        file.writeText(gson.toJson(list))
    }

    fun getAllInventory(): List<InventoryItem> {
        val type = object : TypeToken<List<InventoryItem>>() {}.type
        return readList(inventoryFile, type)
    }

    fun addOrUpdateInventory(item: InventoryItem) {
        val type = object : TypeToken<List<InventoryItem>>() {}.type
        val list = readList<InventoryItem>(inventoryFile, type).toMutableList()
        val idx = list.indexOfFirst { it.barcode == item.barcode }
        if (idx >= 0) list[idx] = item else list.add(item)
        writeList(inventoryFile, list)
    }

    fun removeInventoryItem(barcode: String) {
        val type = object : TypeToken<List<InventoryItem>>() {}.type
        val list = readList<InventoryItem>(inventoryFile, type).toMutableList()
        list.removeAll { it.barcode == barcode }
        writeList(inventoryFile, list)
    }

    fun getItemByBarcode(barcode: String): InventoryItem? {
        return getAllInventory().find { it.barcode == barcode }
    }

    fun updateStock(barcode: String, newStock: Int) {
        val type = object : TypeToken<List<InventoryItem>>() {}.type
        val list = readList<InventoryItem>(inventoryFile, type).toMutableList()
        val idx = list.indexOfFirst { it.barcode == barcode }
        if (idx >= 0) {
            list[idx] = list[idx].copy(stock = newStock)
            writeList(inventoryFile, list)
        }
    }

    fun getAllSales(): List<SaleRecord> {
        val type = object : TypeToken<List<SaleRecord>>() {}.type
        return readList(salesFile, type)
    }

    fun recordSale(sale: SaleRecord) {
        val type = object : TypeToken<List<SaleRecord>>() {}.type
        val list = readList<SaleRecord>(salesFile, type).toMutableList()
        list.add(sale)
        writeList(salesFile, list)
    }

    fun getDiscounts(): List<Discount> {
        val type = object : TypeToken<List<Discount>>() {}.type
        return readList(discountsFile, type)
    }

    fun addOrUpdateDiscount(discount: Discount) {
        val type = object : TypeToken<List<Discount>>() {}.type
        val list = readList<Discount>(discountsFile, type).toMutableList()
        val idx = list.indexOfFirst { it.id == discount.id }
        if (idx >= 0) list[idx] = discount else list.add(discount)
        writeList(discountsFile, list)
    }

    fun removeDiscount(id: String) {
        val type = object : TypeToken<List<Discount>>() {}.type
        val list = readList<Discount>(discountsFile, type).toMutableList()
        list.removeAll { it.id == id }
        writeList(discountsFile, list)
    }

    fun getAllCustomers(): List<Customer> {
        val type = object : TypeToken<List<Customer>>() {}.type
        return readList(customersFile, type)
    }

    fun getCustomerById(id: String): Customer? {
        return getAllCustomers().find { it.id == id }
    }

    fun addOrUpdateCustomer(customer: Customer) {
        val type = object : TypeToken<List<Customer>>() {}.type
        val list = readList<Customer>(customersFile, type).toMutableList()
        val idx = list.indexOfFirst { it.id == customer.id }
        if (idx >= 0) list[idx] = customer else list.add(customer)
        writeList(customersFile, list)
    }

    fun removeCustomer(id: String) {
        val type = object : TypeToken<List<Customer>>() {}.type
        val list = readList<Customer>(customersFile, type).toMutableList()
        list.removeAll { it.id == id }
        writeList(customersFile, list)
    }

    fun getAllSuppliers(): List<Supplier> {
        val type = object : TypeToken<List<Supplier>>() {}.type
        return readList(suppliersFile, type)
    }

    fun addOrUpdateSupplier(supplier: Supplier) {
        val type = object : TypeToken<List<Supplier>>() {}.type
        val list = readList<Supplier>(suppliersFile, type).toMutableList()
        val idx = list.indexOfFirst { it.id == supplier.id }
        if (idx >= 0) list[idx] = supplier else list.add(supplier)
        writeList(suppliersFile, list)
    }

    fun removeSupplier(id: String) {
        val type = object : TypeToken<List<Supplier>>() {}.type
        val list = readList<Supplier>(suppliersFile, type).toMutableList()
        list.removeAll { it.id == id }
        writeList(suppliersFile, list)
    }

    fun recordCreditPayment(payment: CreditPayment) {
        val type = object : TypeToken<List<CreditPayment>>() {}.type
        val list = readList<CreditPayment>(creditPaymentsFile, type).toMutableList()
        list.add(payment)
        writeList(creditPaymentsFile, list)
    }

    fun getAllCreditPayments(): List<CreditPayment> {
        val type = object : TypeToken<List<CreditPayment>>() {}.type
        return readList(creditPaymentsFile, type)
    }

    fun getPaymentsByCustomer(customerId: String): List<CreditPayment> {
        return getAllCreditPayments().filter { it.customerId == customerId }
    }

    fun clearAll() {
        inventoryFile.delete()
        salesFile.delete()
        discountsFile.delete()
        customersFile.delete()
        suppliersFile.delete()
        creditPaymentsFile.delete()
    }
}
