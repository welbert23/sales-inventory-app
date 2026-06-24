package com.salesinventory.app.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "Dashboard")
    data object Scan : Screen("scan", "Scan Barcode")
    data object Inventory : Screen("inventory", "Inventory")
    data object SalesReport : Screen("sales_report", "Sales Report")
    data object Discount : Screen("discount", "Discounts")
    data object AddItem : Screen("add_item", "Add Item")
    data object EditItem : Screen("edit_item/{barcode}", "Edit Item") {
        fun createRoute(barcode: String) = "edit_item/$barcode"
    }
    data object Settings : Screen("settings", "Report Settings")
    data object BulkCheckout : Screen("bulk_checkout", "Bulk Checkout")
    data object Customers : Screen("customers", "Customers")
    data object Suppliers : Screen("suppliers", "Suppliers")
}
