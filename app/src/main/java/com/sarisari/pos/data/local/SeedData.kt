package com.sarisari.pos.data.local

import androidx.room.withTransaction
import com.sarisari.pos.data.local.entity.CustomerEntity
import com.sarisari.pos.data.local.entity.ProductEntity
import com.sarisari.pos.domain.Categories
import com.sarisari.pos.data.repository.ProductImageRepository

/**
 * Demo data with popular Philippine sari-sari items.
 * Prices are typical 2025–2026 neighbourhood-store prices and are meant to be edited.
 * Barcodes are demo values – re-scan your actual stock to replace them.
 */
object SeedData {

    suspend fun seed(db: AppDatabase) {
        val productDao = db.productDao()
        val customerDao = db.customerDao()
        if (productDao.count() > 0) return

        db.withTransaction {
            // ---------- Regular items ----------
            productDao.insertAll(
                listOf(
                    // Snacks
                    p("Piattos Cheese 40g", Categories.SNACKS, "4800016644504", 17.0, 20.0, 24),
                    p("Nova Country Cheddar 40g", Categories.SNACKS, "4800016644702", 17.0, 20.0, 18),
                    p("Chippy BBQ 27g", Categories.SNACKS, "4800092110450", 10.0, 12.0, 30),
                    p("SkyFlakes Crackers (single)", Categories.SNACKS, "4800092550041", 7.0, 9.0, 40),
                    p("Rebisco Crackers (single)", Categories.SNACKS, "4800092330018", 7.0, 9.0, 4, low = 10),

                    // Drinks
                    p("Coke Mismo 290ml", Categories.DRINKS, "4801981116072", 17.0, 20.0, 24),
                    p("Royal Tru-Orange Mismo 290ml", Categories.DRINKS, "4801981116089", 17.0, 20.0, 12),
                    p("Sprite Mismo 290ml", Categories.DRINKS, "4801981116096", 17.0, 20.0, 3),
                    p("C2 Green Tea Apple 230ml", Categories.DRINKS, "4800016022098", 16.0, 20.0, 15),
                    p("Absolute Distilled Water 500ml", Categories.DRINKS, "4801668600104", 12.0, 15.0, 20),
                    p("Kopiko Brown Coffee Twin Pack", Categories.DRINKS, "8996001600146", 7.0, 9.0, 30),

                    // Canned
                    p("Century Tuna Flakes in Oil 155g", Categories.CANNED, "748485100012", 36.0, 42.0, 12),
                    p("Argentina Corned Beef 150g", Categories.CANNED, "4800110100016", 38.0, 45.0, 10),
                    p("Ligo Sardines Red 155g", Categories.CANNED, "4800024560109", 22.0, 26.0, 20),
                    p("Mega Sardines Green 155g", Categories.CANNED, "4806501900012", 21.0, 25.0, 2),
                    p("555 Carne Norte 150g", Categories.CANNED, "4800194100018", 30.0, 36.0, 8),

                    // Toiletries
                    p("Safeguard Pure White 60g", Categories.TOILETRIES, "4800888120012", 22.0, 27.0, 12),
                    p("Colgate Toothpaste 22ml", Categories.TOILETRIES, "8850006320012", 18.0, 22.0, 10),
                    p("Joy Dishwashing Sachet 20ml", Categories.TOILETRIES, "4800888130011", 6.0, 7.0, 30),
                    p("Tide Powder Sachet 66g", Categories.TOILETRIES, "4800888140010", 10.0, 12.0, 25),

                    // Others (noodles, condiments, staples)
                    p("Lucky Me! Pancit Canton Original", Categories.OTHERS, "4807770270017", 14.0, 17.0, 36),
                    p("Lucky Me! Beef Mami", Categories.OTHERS, "4807770120015", 10.0, 13.0, 30),
                    p("Lucky Me! Supreme La Paz Batchoy", Categories.OTHERS, "4807770190018", 18.0, 22.0, 12),
                    p("Silver Swan Soy Sauce 200ml", Categories.OTHERS, "4800047810012", 15.0, 18.0, 15),
                    p("Datu Puti Vinegar 200ml", Categories.OTHERS, "4800047820011", 14.0, 17.0, 15),
                    p("Itlog (Egg, per piece)", Categories.OTHERS, null, 7.0, 9.0, 30, low = 12),
                    p("Bigas (Rice, per kilo)", Categories.OTHERS, null, 45.0, 52.0, 25)
                )
            )

            // ---------- Break-bulk pairs (bulk parent + patingi-tingi child) ----------
            val marlboroPack = productDao.insert(
                p("Marlboro Red (Pack of 20)", Categories.CIGARETTES, "4800100100204", 150.0, 175.0, 5, low = 2)
            )
            productDao.insert(
                p("Marlboro Red (Per Stick)", Categories.CIGARETTES, null, 7.5, 10.0, 12, low = 5)
                    .copy(isBreakBulk = true, parentProductId = marlboroPack, conversionRate = 20)
            )

            val fortunePack = productDao.insert(
                p("Fortune Red (Pack of 20)", Categories.CIGARETTES, "4800100200201", 110.0, 130.0, 4, low = 2)
            )
            productDao.insert(
                p("Fortune Red (Per Stick)", Categories.CIGARETTES, null, 5.5, 7.0, 8, low = 5)
                    .copy(isBreakBulk = true, parentProductId = fortunePack, conversionRate = 20)
            )

            val nescafePack = productDao.insert(
                p("Nescafé Original 3in1 (10 Twin Packs)", Categories.DRINKS, "4800361380012", 75.0, 90.0, 6, low = 2)
            )
            productDao.insert(
                p("Nescafé Original 3in1 Sachet", Categories.DRINKS, "4800361380029", 7.5, 9.0, 6, low = 5)
                    .copy(isBreakBulk = true, parentProductId = nescafePack, conversionRate = 10)
            )

            val shampooStrip = productDao.insert(
                p("Palmolive Shampoo (Strip of 12)", Categories.TOILETRIES, "8850006930012", 72.0, 84.0, 4, low = 1)
            )
            productDao.insert(
                p("Palmolive Shampoo Sachet", Categories.TOILETRIES, null, 6.0, 7.0, 5, low = 6)
                    .copy(isBreakBulk = true, parentProductId = shampooStrip, conversionRate = 12)
            )

            // ---------- Suki ----------
            customerDao.insertAll(
                listOf(
                    CustomerEntity(name = "Aling Nena", phoneNumber = "09171234567", creditLimit = 1500.0, currentBalance = 230.0),
                    CustomerEntity(name = "Mang Tonyo", phoneNumber = "09281234567", creditLimit = 1000.0, currentBalance = 0.0),
                    CustomerEntity(name = "Ate Joy", phoneNumber = "09391234567", creditLimit = 500.0, currentBalance = 460.0),
                    CustomerEntity(name = "Kuya Ben", phoneNumber = null, creditLimit = 800.0, currentBalance = 85.0)
                )
            )
        }
    }

    /** Attach one bundled, barcode-matched catalog image without ever replacing an owner photo. */
    suspend fun addBundledProductImageIfMissing(db: AppDatabase, images: ProductImageRepository) {
        val product = db.productDao().getByBarcode("4800016644504") ?: return
        if (product.imagePath != null) return
        val path = images.importAsset("openfoodfacts/piattos_cheese_40g.jpg")
        db.productDao().update(
            product.copy(
                imagePath = path,
                imageSource = "https://world.openfoodfacts.org/product/4800016644504"
            )
        )
    }

    private fun p(
        name: String,
        category: String,
        barcode: String?,
        cost: Double,
        srp: Double,
        stock: Int,
        low: Int = 5
    ) = ProductEntity(
        name = name,
        category = category,
        barcode = barcode,
        costPrice = cost,
        sellingPrice = srp,
        stockQuantity = stock,
        lowStockThreshold = low
    )
}
