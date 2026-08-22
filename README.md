# LocalShop Business Android

Native Android receiver and operations app for the existing LocalShop / Alpha Mart engine.

## Roles
- **Owner/Admin**: server-scoped to one Shop ID only. Orders, products, stock, cash sales, shop settings.
- **Super Founder**: sees all Shop IDs, can switch shops, and has global reporting/control tools.

There is no separate per-shop founder role.

## Order source of truth
Customer storefront -> backend saves order and decrements stock -> backend sends high-priority FCM -> Android app receives native notification -> vibration + order sound + native TextToSpeech -> tapping notification opens the correct order.

Push, WhatsApp, sound, and TTS are notification layers only. They must never create, delete, or roll back an order.

## Order screen
Each order must show:
- daily order number
- payment mode (COD / Online)
- itemized products, quantity, item price snapshot
- merchandise subtotal
- delivery fee separately
- final total
- customer name and tap-to-call phone
- clickable shared location / Maps
- customer note
- Payment Verified manual control (must not change sales accounting)
- Pending -> Preparing -> Delivered

## Notifications
Native Android implementation uses Firebase Cloud Messaging and a high-importance notification channel. The app includes vibration, a custom order sound, lock-screen notification, and Android TextToSpeech announcing: `New order received. Start packing. Order number N.`

The setup screen guides the user through notification permission, battery-optimization exemption, channel sound/vibration, lock-screen visibility, DND/channel access where the device exposes it, and TTS availability.

## Security
Owner APIs must derive `shop_id` from the authenticated owner session. A client-supplied Shop ID must never grant cross-shop access. Super Founder is the only role allowed to switch Shop IDs.

## Build
GitHub Actions will build the Android APK in the cloud. FCM requires a Firebase Android app configuration (`app/google-services.json`) or an equivalent CI secret-based injection step before an FCM-enabled APK can be produced.

Never commit backend database credentials, Firebase service-account private keys, or other server secrets to this repository.
