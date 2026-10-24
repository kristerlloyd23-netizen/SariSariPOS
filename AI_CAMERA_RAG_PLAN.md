# Product Photo, Inventory Barcode Scan & AI Assistant (RAG) — Implementation Plan

**Project:** Sari-Sari POS (`com.sarisari.pos`)  
**Current baseline:** Kotlin + Compose, Room, CameraX, ML Kit barcode scanning, offline-first local database.  
**Goal:** Add a product-photo workflow, scan/create inventory items from the camera, and a trustworthy assistant grounded in store data and reference documents.  
**AI direction selected:** Hybrid — retain store data and retrieval locally; cloud answer generation is optional and receives only minimal retrieved context after user opt-in.

**Implementation status (2026-10-23):** Product photo capture/gallery import, inventory barcode scanning, local image persistence/backup, Open Food Facts barcode lookup and import, one bundled Piattos photo with attribution, Room v1→v2→v3 migrations, FTS knowledge retrieval, cited assistant sources, live read-only store queries, and optional generated answers are implemented in the current source/APK. The assistant persona is **Ate Sari**. Local retrieval always runs on-device. Gemini `gemini-3.8-flash` is available through the Developer API after the owner provides a key and explicitly enables cloud answers; the key is encrypted at rest, not embedded in the APK. The free API tier may use prompts/replies to improve Google products, so the UI warns owners not to send personal, sensitive or confidential data. A private HTTPS backend remains available. No key or backend credentials were supplied, so remote generation is not active by default. This preserves minSdk 24. The on-device ML Kit Prompt API requires API 26+, so it was not added.

---

## Recommendation in brief

Build the features in three independently useful steps:

1. **Inventory camera flow:** take or choose a product photo; scan a barcode into the product form; store a resized photo in app-private files and its path in Room.
2. **Useful offline assistant foundation:** add a small local knowledge base and Room FTS search. Keep changing inventory, stock and sales answers as read-only repository/DAO queries—not vectorized copies of mutable database rows.
3. **Optional language-model layer:** use a cloud model with App Check, or use an on-device model only when the device supports it. Add semantic/vector retrieval later if real user questions show lexical FTS search is insufficient.

This keeps the POS usable when offline, avoids uploading customer credit data by default, and avoids adding a vector database before it is needed.

### Current implementation additions

- Inventory's barcode form now offers an explicit Open Food Facts lookup. The owner reviews the returned product/brand before choosing the image; imported photos are cached locally and keep a product-page attribution link. Camera/gallery images remain the fallback. A matching Piattos package photo is bundled for the seeded product.
- The owner can enter their own Gemini Developer API key in Settings; AES-GCM/Android Keystore encrypts it at rest. Cloud use stays opt-in and only the question plus selected retrieved facts/notes are sent. This is a single-owner convenience, not a production-safe API-key architecture.
- Google documents that the free Gemini API tier may use submitted prompts/responses to improve products. The app warns against sending personal, sensitive, or confidential information. A secured private backend is the recommended route for sensitive or multi-user deployments.
- **Ate Sari** is the assistant persona, with concise, locally grounded answers, source references, abstention on missing evidence, and no transaction authority.

## 1. Product photo + barcode capture

### User flow

In **Inventory → Add/Edit product**:

- Show an image preview and **Take photo** / **Choose photo** actions.
- **Scan barcode** opens the existing CameraX + ML Kit scanning flow, but returns the decoded value to the inventory form rather than the POS cart.
- If the scanned barcode matches a local product, offer **Open existing item**. If it is unknown, fill the barcode field and let the shopkeeper enter the name, category, cost, SRP and starting stock.
- Keep manual barcode entry and photo selection available when camera hardware or permission is unavailable.
- Never infer or auto-save product name, cost, selling price or stock from a photo/barcode without showing the proposed values for review.

### Implementation

- Reuse CameraX lifecycle handling; add an `ImageCapture` use case to the inventory camera screen. CameraX separates `Preview`, `ImageAnalysis` and `ImageCapture` use cases, and the still image can be saved asynchronously to a file. See [CameraX architecture](https://developer.android.com/media/camera/camerax/architecture) and [capture a photo](https://developer.android.com/media/camera/camerax/take-photo).
- Continue using on-device ML Kit Barcode Scanning on the camera analysis frames. The current project already has ML Kit and CameraX. Google's [ML Kit Analyzer guide](https://developer.android.com/media/camera/camerax/mlkitanalyzer) describes a CameraX integration that handles analyzer output and coordinate mapping.
- For a gallery image, use Android's system Photo Picker (`PickVisualMedia`) instead of broad media/storage permissions. The picker has a fallback for older devices: [Photo Picker documentation](https://developer.android.com/training/data-storage/shared/photo-picker).
- Compress/resize photos (for example, longest edge around 1,024 px, JPEG quality around 80) before storing; display thumbnails in POS/inventory and load the full photo only when needed.
- Store image bytes in `filesDir/products/<stable-id>.jpg`; store only a relative path in Room. Delete orphaned files when a product is deleted or its photo is replaced. Do not put image bytes in SQLite or JSON.

### Database & backup change

- Add nullable `imagePath: String? = null` to `ProductEntity`.
- Increment Room database version and add a non-destructive `Migration(1, 2)` that adds the nullable column. Do not use destructive migration because this app holds sales and customer balances.
- Update the backup export to include image filenames and hashes, and optionally package image files as separate attachments. A JSON-only backup must not silently omit the new product photos.
- Add tests for migration from the v1 schema and orphan-file cleanup.

## 2. Assistant scope and grounding

### Start with practical shop questions

Examples:

- “Aling items ang paubos na?”
- “Magkano ang benta at estimated tubo kahapon?”
- “Ilan pang sachet ang mabebenta kung bubuksan ang natitirang kahon?”
- “Ano ang store policy sa utang?”
- “Paano mag-record ng partial payment?”

### Separate live facts from reference knowledge

**Live / changing data:** query the app's repositories and DAOs at the time of the question. Implement a small allowlisted tool interface such as:

- `getLowStockProducts()`
- `findProductByNameOrBarcode(query)`
- `getBreakBulkAvailability(productId)`
- `getSalesSummary(start, end)`
- `getReceivablesSummary()` (aggregate only by default)

**Reference documents:** retrieve from a curated knowledge base, such as store rules, supplier notes, a user-edited FAQ, and short product-handling notes. Do not index customer names, phone numbers, individual Utang history, or unrestricted transaction records for semantic retrieval.

For every factual answer, show the data period or “as of” timestamp and where the answer came from (for example, “Inventory · updated now” or a document title). If retrieval finds no useful support, the assistant should say it does not know and point the user to the relevant screen.

### Local-first RAG MVP

- Add `AssistantDocumentEntity` for source metadata and original text.
- Add a Room FTS-backed chunk table (`@Fts4` or supported FTS variant) with document id, heading and chunk text. Room documents FTS entities and query-backed DAO access; see [Room FTS documentation](https://developer.android.com/training/data-storage/room/defining-data).
- Chunk user-provided documents into small heading-aware passages; retrieve a handful of matches using FTS, category/source filters and a relevance threshold.
- Compose the response context from those passages plus explicitly queried live facts. Keep source identifiers alongside each passage so the UI can show citations.
- Start with deterministic templates/FAQ responses while validating retrieval. This provides a real offline search feature before selecting or provisioning an LLM.

**Important:** This is lexical retrieval rather than vector-semantic RAG. For a small shop FAQ it may be enough. If real questions need semantic matching, add embeddings and a vector store as a separate, measured upgrade.

## 3. Model options

### Option A — Fully offline (best privacy, hardware-dependent model)

Use local FTS retrieval and optionally generate a natural-language answer with Android's ML Kit GenAI Prompt API / Gemini Nano on compatible devices. Android describes Gemini Nano as on-device and useful for offline/privacy-sensitive cases, but model availability and hardware support are device-dependent: [Gemini Nano / ML Kit GenAI](https://developer.android.com/ai/gemini-nano). The app's current min SDK is 24; Firebase's Android hybrid on-device setup documents a minimum API 26 for its on-device artifact. Keep a no-model/FAQ fallback for unsupported devices: [Firebase AI Logic hybrid setup](https://firebase.google.com/docs/ai-logic/hybrid/android/get-started).

### Option B — Local retrieval + cloud generation (simpler hybrid MVP)

Keep product/customer data and FTS search on-device. After the user opts in, send only the question plus minimum relevant, non-sensitive retrieved context to a cloud LLM. Firebase AI Logic provides Android Kotlin/Java client SDKs and supports model chat, but its current capability list says embeddings generation and semantic retrieval are not supported by Firebase AI Logic itself. Thus, it can generate an answer from context retrieved locally, but it is not by itself a vector-RAG system: [Firebase AI Logic capabilities/models](https://firebase.google.com/docs/ai-logic/models).

If using Firebase AI Logic, enforce App Check, configure restrictions/quotas, and do not put a Gemini API key in the app. The current production guidance calls for App Check; Firebase documents that enforcement becomes required on **November 2, 2026**: [Firebase AI Logic production checklist](https://firebase.google.com/docs/ai-logic/production-checklist).

### Option C — Cloud semantic RAG (only if the store needs it)

Add an authenticated backend (for example, Cloud Run) that performs retrieval, model calls, tenant/store authorization and rate limiting. Store only deliberately selected documents or approved aggregates in the cloud. Google's Vertex AI RAG Engine provides a managed context retrieval framework and configurable top-k/hybrid retrieval: [RAG Engine overview](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/rag-engine/rag-overview). This option adds hosting, costs, network dependence, data governance, and a sync/tenant-isolation design; it should not be the first change to a single-device POS.

**Do not** embed a secret service-account key or unrestricted model API key in the APK. For a backend design, keep credentials server-side and scope access to the authenticated store.

## 4. Safety boundaries for the assistant

- First release is **read-only**. The assistant may explain and recommend; it must not silently change stock, create a sale, increase credit, record a repayment, or edit a price.
- If action buttons are added later, show a review/confirmation screen and run the same repository validations/Room transaction as manual UI actions.
- Use typed, allowlisted tool calls. Never let generated text become arbitrary SQL or unrestricted repository access.
- Treat retrieved document text as untrusted input; do not follow instructions found inside retrieved content.
- Ask before sending store data to a cloud model. Exclude PII by default; provide a clear “clear AI history” action and avoid logging raw prompts containing personal data.
- Display source citations and clearly distinguish reported figures from estimates/advice.

## 5. Proposed code organization

```text
com.sarisari.pos/
├── data/local/entity/ProductEntity.kt             # nullable imagePath
├── data/local/entity/AssistantDocumentEntity.kt
├── data/local/entity/AssistantChunkEntity.kt      # FTS-backed
├── data/local/dao/AssistantDao.kt
├── data/repository/ProductImageRepository.kt      # capture import/compress/delete
├── data/repository/AssistantRepository.kt         # local retrieve + context assembly
├── domain/assistant/AssistantTool.kt              # allowlisted read-only tools
├── ui/inventory/ProductFormDialog.kt              # photo + barcode callbacks
├── ui/components/InventoryCameraDialog.kt          # CameraX preview/capture/scan
└── ui/assistant/AssistantScreen.kt                 # chat, citations, offline state
```

For an MVP, the assistant may be a floating action button or a separate bottom-navigation destination. Do not put chat over the checkout button or make a model call a prerequisite for selling.

## 6. Delivery phases and acceptance checks

### Phase 1 — Camera + inventory
- Capture image, preview, replace/remove, choose from system photo picker.
- Scan code into inventory add/edit; unknown code fills the form; known code opens matching product.
- Image is resized and persisted privately; existing database data survives the migration.
- Test on a low-memory/API 24–26 device/emulator and a newer physical Android phone. Check orientation, camera permission denial, rotation, image size, and failed scan/manual fallback.

### Phase 2 — Offline knowledge and assistant UI
- Import/edit a few store FAQ/policy documents.
- FTS retrieval returns relevant passages with source ids; unsupported questions abstain.
- Read-only tools return exact current stock, break-bulk availability and sales aggregates from Room.
- Test in airplane mode; no sale/POS workflow is blocked by AI availability.

### Phase 3 — Optional model
- Select offline Nano or cloud generation after deciding supported devices, consent, latency and operating cost.
- Make retrieved sources visible, add bilingual/Taglish sample questions, and test response grounding against known inventory/report answers.
- For cloud: App Check/auth, request limits, privacy review, error handling and model/provider configuration before release.

### Phase 4 — Quality & operations
- Build a test set of at least 30 realistic Filipino/English/Taglish questions and expected source facts.
- Measure retrieval precision, unsupported-claim rate, latency, offline behavior and data leakage.
- Test Room migration, backup/restore including images, product deletion, and assistant history deletion.

## Research links

- [CameraX architecture and ImageCapture](https://developer.android.com/media/camera/camerax/architecture)
- [CameraX take-photo guide](https://developer.android.com/media/camera/camerax/take-photo)
- [ML Kit Analyzer with CameraX](https://developer.android.com/media/camera/camerax/mlkitanalyzer)
- [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker)
- [Room FTS entities](https://developer.android.com/training/data-storage/room/defining-data)
- [Gemini Nano / ML Kit GenAI](https://developer.android.com/ai/gemini-nano)
- [Firebase AI Logic supported models/capabilities](https://firebase.google.com/docs/ai-logic/models)
- [Firebase AI Logic hybrid Android](https://firebase.google.com/docs/ai-logic/hybrid/android/get-started)
- [Firebase AI Logic production checklist](https://firebase.google.com/docs/ai-logic/production-checklist)
- [Vertex AI RAG Engine overview](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/rag-engine/rag-overview)
