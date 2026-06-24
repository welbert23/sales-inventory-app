# Sales & Inventory App

An Android point-of-sale and inventory management app built with Jetpack Compose. Data is stored in Excel (.xlsx) format via Apache POI.

## Features

- **Barcode Scanning** — Scan product barcodes using CameraX + ML Kit for quick checkout
- **Inventory Management** — Add, edit, delete, and search products with image support
- **Sales Reporting** — Daily and monthly sales reports with sub-label grouping (MS/CS/LS)
- **Bulk Checkout** — Multi-item cart with customer assignment and credit tracking
- **Customer Management** — Track credit balances and payment history
- **Supplier Management** — Store supplier contact information
- **Profit Tracking** — Cost price tracking and profit calculation per sale
- **Discounts** — Percentage or fixed-amount discount support
- **PDF Export** — Export sales reports as PDF
- **Bluetooth Printing** — Print receipts and barcode labels via Bluetooth thermal printer
- **Comparative Analysis** — Compare sales across different periods
- **Cloud Backup** — Backup Excel data to Google Drive or external folder via Storage Access Framework

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Camera:** CameraX
- **Barcode:** ML Kit Barcode Scanning
- **Storage:** Apache POI (XSSFWorkbook), Excel .xlsx
- **Image Loading:** Coil
- **Barcode Generation:** ZXing

## Requirements

- Android 8.0 (API 26) or higher
- Camera (for barcode scanning)
- Bluetooth (optional, for printing)

## Installation

Download the latest APK from the [Releases](https://github.com/bogsby/sales-inventory-app/releases) page and install it on your device.

## Usage

1. Launch the app and start adding products to your inventory
2. Use the **Scan** screen to quickly sell items by barcode
3. Use **Bulk Checkout** for multi-item purchases
4. View sales reports and profit on the **Reports** screen
5. Manage customers, suppliers, and discounts from the **Home** screen

## Data Storage

All data is stored locally in an `inventory.xlsx` file on your device. You can back up this file to Google Drive or any cloud storage app using the **Backup to Cloud** feature.

## License

MIT License — see [LICENSE](LICENSE) for details.
