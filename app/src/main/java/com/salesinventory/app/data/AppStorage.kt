package com.salesinventory.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppStorage(private val context: Context) {

    private val gson = Gson()
    private val mutex = Mutex()

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
        val tmp = File(file.absolutePath + ".tmp")
        tmp.writeText(gson.toJson(list))
        tmp.renameTo(file)
    }

    private suspend fun <T> synchronizedRead(file: File, type: java.lang.reflect.Type): List<T> =
        mutex.withLock { readList(file, type) }

    private suspend fun <T> synchronizedWrite(file: File, list: List<T>) =
        mutex.withLock { writeList(file, list) }

    suspend fun getAllInventory(): List<InventoryItem> {
        val type = object : TypeToken<List<InventoryItem>>() {}.type
        return synchronizedRead(inventoryFile, type)
    }

    suspend fun addOrUpdateInventory(item: InventoryItem) {
        val type = object : TypeToken<List<InventoryItem>>() {}.type
        val list = synchronizedRead<InventoryItem>(inventoryFile, type).toMutableList()
        val idx = list.indexOfFirst { it.barcode == item.barcode }
        if (idx >= 0) list[idx] = item else list.add(item)
        synchronizedWrite(inventoryFile, list)
    }

    suspend fun removeInventoryItem(barcode: String) {
        val type = object : TypeToken<List<InventoryItem>>() {}.type
        val list = synchronizedRead<InventoryItem>(inventoryFile, type).toMutableList()
        list.removeAll { it.barcode == barcode }
        synchronizedWrite(inventoryFile, list)
    }

    suspend fun getItemByBarcode(barcode: String): InventoryItem? {
        return getAllInventory().find { it.barcode == barcode }
    }

    suspend fun updateStock(barcode: String, newStock: Int) {
        val type = object : TypeToken<List<InventoryItem>>() {}.type
        val list = synchronizedRead<InventoryItem>(inventoryFile, type).toMutableList()
        val idx = list.indexOfFirst { it.barcode == barcode }
        if (idx >= 0) {
            list[idx] = list[idx].copy(stock = newStock)
            synchronizedWrite(inventoryFile, list)
        }
    }

    suspend fun getAllSales(): List<SaleRecord> {
        val type = object : TypeToken<List<SaleRecord>>() {}.type
        return synchronizedRead(salesFile, type)
    }

    suspend fun recordSale(sale: SaleRecord) {
        val type = object : TypeToken<List<SaleRecord>>() {}.type
        val list = synchronizedRead<SaleRecord>(salesFile, type).toMutableList()
        list.add(sale)
        synchronizedWrite(salesFile, list)
    }

    suspend fun getDiscounts(): List<Discount> {
        val type = object : TypeToken<List<Discount>>() {}.type
        return synchronizedRead(discountsFile, type)
    }

    suspend fun addOrUpdateDiscount(discount: Discount) {
        val type = object : TypeToken<List<Discount>>() {}.type
        val list = synchronizedRead<Discount>(discountsFile, type).toMutableList()
        val idx = list.indexOfFirst { it.id == discount.id }
        if (idx >= 0) list[idx] = discount else list.add(discount)
        synchronizedWrite(discountsFile, list)
    }

    suspend fun removeDiscount(id: String) {
        val type = object : TypeToken<List<Discount>>() {}.type
        val list = synchronizedRead<Discount>(discountsFile, type).toMutableList()
        list.removeAll { it.id == id }
        synchronizedWrite(discountsFile, list)
    }

    suspend fun getAllCustomers(): List<Customer> {
        val type = object : TypeToken<List<Customer>>() {}.type
        return synchronizedRead(customersFile, type)
    }

    suspend fun getCustomerById(id: String): Customer? {
        return getAllCustomers().find { it.id == id }
    }

    suspend fun addOrUpdateCustomer(customer: Customer) {
        val type = object : TypeToken<List<Customer>>() {}.type
        val list = synchronizedRead<Customer>(customersFile, type).toMutableList()
        val idx = list.indexOfFirst { it.id == customer.id }
        if (idx >= 0) list[idx] = customer else list.add(customer)
        synchronizedWrite(customersFile, list)
    }

    suspend fun removeCustomer(id: String) {
        val type = object : TypeToken<List<Customer>>() {}.type
        val list = synchronizedRead<Customer>(customersFile, type).toMutableList()
        list.removeAll { it.id == id }
        synchronizedWrite(customersFile, list)
    }

    suspend fun getAllSuppliers(): List<Supplier> {
        val type = object : TypeToken<List<Supplier>>() {}.type
        return synchronizedRead(suppliersFile, type)
    }

    suspend fun addOrUpdateSupplier(supplier: Supplier) {
        val type = object : TypeToken<List<Supplier>>() {}.type
        val list = synchronizedRead<Supplier>(suppliersFile, type).toMutableList()
        val idx = list.indexOfFirst { it.id == supplier.id }
        if (idx >= 0) list[idx] = supplier else list.add(supplier)
        synchronizedWrite(suppliersFile, list)
    }

    suspend fun removeSupplier(id: String) {
        val type = object : TypeToken<List<Supplier>>() {}.type
        val list = synchronizedRead<Supplier>(suppliersFile, type).toMutableList()
        list.removeAll { it.id == id }
        synchronizedWrite(suppliersFile, list)
    }

    suspend fun recordCreditPayment(payment: CreditPayment) {
        val type = object : TypeToken<List<CreditPayment>>() {}.type
        val list = synchronizedRead<CreditPayment>(creditPaymentsFile, type).toMutableList()
        list.add(payment)
        synchronizedWrite(creditPaymentsFile, list)
    }

    suspend fun getAllCreditPayments(): List<CreditPayment> {
        val type = object : TypeToken<List<CreditPayment>>() {}.type
        return synchronizedRead(creditPaymentsFile, type)
    }

    suspend fun getPaymentsByCustomer(customerId: String): List<CreditPayment> {
        return getAllCreditPayments().filter { it.customerId == customerId }
    }
}
