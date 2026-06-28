# Sales & Inventory App

An Android point-of-sale and inventory management app built with Jetpack Compose and Material 3. Data is stored locally in JSON format.

## Features

- **Barcode Scanning** — Scan product barcodes using CameraX + ML Kit for quick checkout
- **Inventory Management** — Add, edit, delete, and search products with image support
- **Sales History** — View grouped sales records with profit tracking
- **Bulk Checkout** — Multi-item cart with customer assignment, credit tracking, and discount support
- **Customer Management** — Track credit balances and payment history
- **Supplier Management** — Store supplier contact information
- **Profit Tracking** — Cost price tracking and profit calculation per sale
- **Discounts** — Percentage or fixed-amount discount support
- **Dashboard** — Overall and daily sales/profit overview with comparative analysis
- **Bluetooth Printing** — Print receipts via Bluetooth thermal printer

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Camera:** CameraX
- **Barcode:** ML Kit Barcode Scanning
- **Storage:** JSON files with thread-safe Mutex synchronization
- **Image Loading:** Coil
- **Architecture:** MVVM with StateFlow

## Requirements

- Android 8.0 (API 26) or higher
- Camera (for barcode scanning)
- Bluetooth (optional, for printing)

## Installation

Download the latest APK from the [Releases](https://github.com/welbert23/sales-inventory-app/releases) page and install it on your device.

## Usage

1. Launch the app and start adding products to your inventory
2. Use the **Scan** screen to quickly sell items by barcode
3. Use **Bulk Checkout** for multi-item purchases with discounts
4. View sales and profit on the **Dashboard**
5. Manage customers, suppliers, and discounts from the navigation tabs

## Data Storage

All data is stored locally in JSON files on your device. The app uses thread-safe atomic writes with Mutex synchronization to prevent data corruption.

## License

MIT License — see [LICENSE](LICENSE) for details.
