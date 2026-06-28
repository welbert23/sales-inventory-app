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

    // ESC/POS commands
    private const val ESC = 0x1B
    private const val GS = 0x1D
    private const val LF = 0x0A

    private fun escp(vararg bytes: Int) = bytes.map { it.toByte() }.toByteArray()

    private fun cmdInit() = escp(ESC, 0x40)
    private fun cmdBold(on: Boolean) = escp(ESC, 0x45, if (on) 1 else 0)
    private fun cmdAlign(align: Int) = escp(ESC, 0x61, align)
    private fun cmdLineFeed(n: Int) = ByteArray(n) { LF.toByte() }
    private fun cmdCut() = escp(GS, 0x56, 0x00)
    private fun cmdSetCharSize(w: Int, h: Int) = escp(GS, 0x21, ((w and 0x0F) or ((h and 0x0F) shl 4)))

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

    private fun write(vararg data: ByteArray): Boolean {
        return try {
            val os = outputStream ?: return false
            for (d in data) os.write(d)
            os.flush()
            true
        } catch (e: IOException) {
            false
        }
    }

    private fun writeLine(line: String): ByteArray {
        return (line + "\n").toByteArray(Charsets.UTF_8)
    }

    fun printReceipt(
        storeName: String,
        items: List<ReceiptItem>,
        total: Double,
        paymentType: String,
        transactionId: String
    ): Boolean {
        val lines = mutableListOf<ByteArray>()

        // Initialize printer
        lines.add(cmdInit())
        lines.add(cmdAlign(1)) // center

        // Store name (double height + double width)
        lines.add(cmdSetCharSize(2, 2))
        lines.add(cmdBold(true))
        lines.add(writeLine(storeName.ifBlank { "STORE" }))
        lines.add(cmdSetCharSize(1, 1))
        lines.add(cmdBold(false))

        // Divider
        lines.add(writeLine("================================"))
        lines.add(cmdLineFeed(1))

        // Items
        lines.add(cmdAlign(0)) // left
        for (item in items) {
            lines.add(cmdBold(true))
            lines.add(writeLine(item.name.take(20)))
            lines.add(cmdBold(false))
            val line = String.format("  %3d x PHP %7.2f    PHP %8.2f", item.qty, item.price, item.total)
            lines.add(writeLine(line))
        }

        // Divider
        lines.add(cmdLineFeed(1))
        lines.add(cmdAlign(0))
        lines.add(writeLine("--------------------------------"))

        // Total
        lines.add(cmdBold(true))
        lines.add(cmdSetCharSize(1, 2))
        lines.add(writeLine(String.format("TOTAL:                 PHP %8.2f", total)))
        lines.add(cmdSetCharSize(1, 1))
        lines.add(cmdBold(false))

        // Payment info
        lines.add(cmdAlign(0))
        lines.add(writeLine("Payment: $paymentType"))
        lines.add(writeLine("Txn: ${transactionId.take(8)}"))

        // Footer
        lines.add(cmdLineFeed(1))
        lines.add(cmdAlign(1))
        lines.add(cmdBold(true))
        lines.add(writeLine("Thank you! Please come again"))
        lines.add(cmdBold(false))

        // Cut
        lines.add(cmdLineFeed(4))
        lines.add(cmdCut())

        return write(*lines.toTypedArray())
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
