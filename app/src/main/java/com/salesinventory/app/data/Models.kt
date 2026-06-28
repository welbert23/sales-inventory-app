package com.salesinventory.app.data

data class InventoryItem(
    val barcode: String = "",
    val name: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val costPrice: Double = 0.0,
    val stock: Int = 0,
    val unit: String = "pcs",
    val size: String = "",
    val color: String = "",
    val supplierId: String = "",
    val subLabel: String = "",
    val minStock: Int = 0,
    val imageUri: String = ""
)

data class SaleRecord(
    val id: String = "",
    val date: String = "",
    val barcode: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val costPrice: Double = 0.0,
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val subtotal: Double = 0.0,
    val total: Double = 0.0,
    val customerId: String = "",
    val customerType: String = "",
    val paymentType: PaymentType = PaymentType.CASH,
    val isCredit: Boolean = false,
    val transactionId: String = ""
) {
    val profit: Double get() = (unitPrice - costPrice) * quantity - discountAmount
}

data class Discount(
    val id: String = "",
    val name: String = "",
    val type: DiscountType = DiscountType.PERCENTAGE,
    val value: Double = 0.0,
    val isActive: Boolean = true
)

enum class DiscountType { PERCENTAGE, FIXED_AMOUNT }

enum class PaymentType(val displayName: String) {
    CASH("Cash"), GCASH("GCash"), BANK_TRANSFER("Bank Transfer"), CREDIT("Credit")
}

data class Supplier(
    val id: String = "",
    val name: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = ""
)

data class Customer(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val customerType: String = "Walk-in",
    val creditBalance: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val notes: String = ""
)

data class CartItem(
    val barcode: String = "",
    val productName: String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val costPrice: Double = 0.0,
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val subtotal: Double = 0.0,
    val total: Double = 0.0
)

data class CreditPayment(
    val id: String = "",
    val customerId: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val notes: String = ""
)
enum class ComparePeriod { DAY, WEEK, MONTH, YEAR }

data class PeriodSummary(
    val label: String,
    val totalSales: Double,
    val transactionCount: Int,
    val itemCount: Int
)

data class SalesComparison(
    val period1: PeriodSummary,
    val period2: PeriodSummary,
    val growthPercent: Double
)

data class ComparisonData(
    val year1: Int,
    val year2: Int,
    val period: ComparePeriod,
    val comparisons: List<SalesComparison>
)

data class ReportCustomSection(
    val id: String = "",
    val title: String = "",
    val content: String = ""
)

data class ReportSettings(
    val storeName: String = "",
    val monthlyTarget: Double = 0.0,
    val minStockThreshold: Int = 5,
    val showDailySales: Boolean = true,
    val showMTD: Boolean = true,
    val showTarget: Boolean = true,
    val showStockLevel: Boolean = true,
    val showGeneratedTime: Boolean = true,
    val enableSubGrouping: Boolean = true,
    val subGroupSuffixes: String = "MS,CS,LS",
    val customSections: List<ReportCustomSection> = emptyList()
)
