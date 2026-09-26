# Product Catalog Android App

This project is a starter Android app for managing products by category and exporting catalogue PDFs.

Features
- Add and edit products by category
- Store code, description, barcode, selling price, cost price, and image
- Display South African Rand pricing using the `R` currency format
- Export a PDF catalogue for the selected category
- Bulk upload products with CSV files

How to run
1. Open the project in Android Studio.
2. Let Gradle sync.
3. Run the app on an emulator or Android device.

Notes
- The app uses Room for local storage.
- The export feature creates a PDF package with product images and basic product details.
- CSV bulk import expects headers like: `code,description,barcode,sellingprice,costprice`
