package com.salesinventory.app.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.OutputStream
import java.util.*

object BluetoothPrinter {

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    fun getPairedPrinters(): List<Pair<String, String>> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return adapter.bondedDevices
            .filter { it.type != BluetoothDevice.DEVICE_TYPE_LE }
            .map { it.address to it.name }
    }

    fun connect(address: String): Boolean {
        try {
            disconnect()
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
            val device = adapter.getRemoteDevice(address)
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            socket = device.createRfcommSocketToServiceRecord(uuid)
            adapter.cancelDiscovery()
            socket?.connect()
            outputStream = socket?.outputStream
            return outputStream != null
        } catch (e: IOException) {
            disconnect()
            return false
        }
    }

    fun printText(text: String): Boolean {
        return try {
            val os = outputStream ?: return false
            os.write(text.toByteArray())
            os.write("\n".toByteArray())
            os.flush()
            true
        } catch (e: IOException) {
            false
        }
    }

    fun printReceipt(
        storeName: String,
        items: List<ReceiptItem>,
        total: Double,
        paymentType: String,
        transactionId: String
    ): Boolean {
        val sb = StringBuilder()

        // Center align store name
        sb.append("\n")
        sb.append("          ${storeName.ifBlank { "Store" }}\n")
        sb.append("--------------------------------\n")

        for (item in items) {
            val line = String.format("%-16s %3d x %7.2f", item.name.take(16), item.qty, item.price)
            sb.append(line)
            sb.append("\n")
            val totalLine = String.format("%27.2f", item.total)
            sb.append("$totalLine\n")
        }

        sb.append("--------------------------------\n")
        sb.append(String.format("%-16s %11.2f\n", "TOTAL:", total))
        sb.append("Payment: $paymentType\n")
        sb.append("Txn: ${transactionId.take(8)}\n")
        sb.append("\n")
        sb.append("     Thank you! Please come again\n")
        sb.append("\n\n\n")

        return printText(sb.toString())
    }

    fun printBarcodeLabel(barcode: String, productName: String, price: Double): Boolean {
        val sb = StringBuilder()
        sb.append("\n")
        sb.append("$productName\n")
        sb.append("Price: PHP ${"%.2f".format(price)}\n")
        sb.append("Barcode: $barcode\n")
        sb.append("\n")
        return printText(sb.toString())
    }

    fun disconnect() {
        try { outputStream?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        outputStream = null
        socket = null
    }
}

data class ReceiptItem(
    val name: String,
    val qty: Int,
    val price: Double,
    val total: Double
)
