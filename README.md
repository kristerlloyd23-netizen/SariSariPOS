# Sari-Sari POS & Inventory (Android)

An offline-first, Kotlin/Jetpack Compose point-of-sale and inventory app for a neighbourhood sari-sari store. Local data is stored in Room/SQLite. Background work creates local ZIP sync/backup exports; no cloud account or internet connection is required for sales.

## Build

Requirements: JDK 17, Android SDK Platform 34, Build Tools 34.0.0, and Android Gradle Plugin-compatible Gradle 8.5.

```bash
./gradlew assembleDebug
```

Installable debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

A pre-built APK is available to download:
- **GitHub Release:** [Download SariSariPOS-debug.apk (v1.0.0)](https://github.com/kristerlloyd23-netizen/SariSariPOS/releases/download/v1.0.0/SariSariPOS-debug.apk)
- **Repository path:** [`apk/SariSariPOS-debug.apk`](apk/SariSariPOS-debug.apk)

Run the unit tests:

```bash
./gradlew testDebugUnitTest
```

Release APKs are currently signed with the debug key to permit local testing. Configure your own signing key before Play Store distribution.

## Features

- **POS:** searchable/filterable product grid, active cart, barcode scanner (CameraX + on-device ML Kit), Cash / Utang / GCash-Maya checkout, cash received and change calculation.
- **Utang:** suki records, credit limits checked during checkout, partial repayments, repayment and purchase history.
- **Inventory:** add/edit/delete, capital and SRP, low-stock thresholds, barcode scan directly into the product form, camera capture or Photo Picker images, restock, and pack/box-to-piece break-bulk conversion. A real Piattos package photo is bundled for the matching demo product, and **Find product photo by barcode** looks up Open Food Facts so you can review a catalog match and apply its package image. Images are cached in private app storage with the attribution URL; owner photos remain supported as fallback.
- **Ate Sari assistant:** a friendly, practical store-helper persona; local Room FTS retrieval over seeded and user-added notes; allowlisted read-only inventory, sales and Utang aggregate queries; and cited source cards. Offline answers work without a model. Optionally configure your own Gemini API key for `gemini-3.8-flash` free-tier replies or a private HTTPS `/ask` endpoint. Cloud generation is off until enabled; only the question and relevant retrieved facts/notes are sent. No API key is embedded in the APK.
- **Reports:** today, rolling 7-day and current-month revenue, cost of goods, estimated gross profit, payment breakdown, repayments collected, sales trend and top products.
- **Settings:** store and wallet details, private local static QR images, background and manual ZIP backup export/share (manifest plus saved product photos).
- **Demo data:** popular products and example customers are inserted on first launch. Demo barcodes and prices are illustrative; verify/edit them against your actual stock and local prices.

## First use

1. Open **Inventory** and edit the example stock/prices or add your products.
2. Optionally use **Settings → GCash/Maya receive QR** to save your wallet QR image.
3. Add your suki in **Utang** and set credit limits.
4. Tap items in **POS**, adjust cart quantities, and check out.
5. Create and share backup files regularly from **Settings**. The scheduled worker writes a backup about every six hours when Android permits it.

## Project layout

- `data/local`: Room entities, DAOs, database, first-run seed data.
- `data/repository`: transactional checkout, customer ledger, stock operations, reporting and backup.
- `data/worker`: WorkManager local backup worker.
- `domain`: categories, payment types and break-bulk rules.
- `ui`: Compose POS, inventory, utang, reports, settings, navigation and theme.

## Sync/backup note

This project intentionally implements an offline local export, not a server/cloud sync service: the generated JSON includes pending sales, repayments and a current product/customer snapshot. A real cloud service needs a server endpoint, authentication, conflict handling and a security review; `BackupRepository.pushBatch()` is the seam intended for that integration. Backups can contain customer names, phone numbers and store activity, so keep shared copies private.


## Ate Sari AI and privacy

- **Gemini option:** Create a Gemini Developer API key through Google AI Studio, save it in **Settings → Ate Sari assistant**, then enable cloud answer generation. The app uses `gemini-3.8-flash`; availability, quotas and free-tier terms can change. No shared key is included in this repository or APK.
- The owner's key is encrypted at rest with Android Keystore. It is still a client-side key and could be exposed on a compromised/running phone; for a public or multi-user release, use a protected backend instead.
- The free API tier may use prompts and replies to improve Google products. Avoid personal, sensitive, or confidential store details when using that tier. If this is unacceptable, leave cloud answers off (local RAG still works), or use a properly secured private backend and review its provider/data terms.
- Cloud generation is explicitly opt-in. The app sends the current question and only relevant retrieved product/store facts and note excerpts—not the whole Room database. The model cannot execute checkout, edit inventory, or run arbitrary SQL. If remote generation fails, it falls back to local grounded answers.
- **Private backend:** Configure a trusted HTTPS `/ask` URL in Settings. The request includes `assistantPersona: "Ate Sari"`, a grounding system instruction, the question, relevant facts and retrieved sources. Return JSON such as `{ "answer": "..." }`. Protect the endpoint with authentication, rate limits and abuse controls; do not use a public unauthenticated model endpoint.

## Product-photo attribution

The bundled Piattos cheese package image and any imported Open Food Facts photos are attributed to **Open Food Facts** with a link to the product page and the **CC BY-SA 3.0** image license. Catalog entries are crowdsourced and may be incomplete or not match the exact local package, so review the name/photo before applying it. Open Food Facts notes that packaging artwork can have separate third-party rights; keep catalog-photo use appropriately scoped. See [Open Food Facts re-use terms](https://world.openfoodfacts.org/terms-of-use).
