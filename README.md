# LocalShop Business Android

Native Android receiver and operations app for the existing LocalShop / Alpha Mart engine.

## Roles
- **Owner/Admin**: server-scoped to one Shop ID only. Orders, products, stock, cash sales, shop settings.
- **Super Founder**: sees all Shop IDs, can switch shops, and has global reporting/control tools.

There is no separate per-shop founder role.

## Order source of truth
Customer storefront -> backend saves order and decrements stock -> backend sends high-priority FCM -> Android notification system / app receiver -> vibration + spoken order alert -> tapping notification opens the correct order.

Push, WhatsApp, sound, and voice are notification layers only. They must never create, delete, or roll back an order.

## Background voice strategy
For reliable background/locked-screen behavior, the app no longer depends only on live TextToSpeech. It bundles a spoken notification sound (`order_voice_alert.ogg`) saying `New order received. Start packing.` and assigns it to the high-importance Android order channel `new_orders_voice_v3`. This lets Android's notification system play the spoken alert even when the app UI is not open. Dynamic order number TTS remains best-effort when the process is running.

## Order screen
Each order must show daily order number, COD/Online payment mode, itemized products/quantity/order-time price snapshot, merchandise subtotal, delivery fee separately, final total, customer name/phone, Maps location, notes, Payment Verified and Pending -> Preparing -> Delivered.

## Security
Owner APIs must derive `shop_id` from the authenticated owner session. A client-supplied Shop ID must never grant cross-shop access. Super Founder is the only role allowed to switch Shop IDs.

## Build
GitHub Actions builds the Android APK in the cloud. Firebase Android package is `com.localshop.business`.

Never commit backend database credentials, Firebase service-account private keys, or other server secrets to this repository.
