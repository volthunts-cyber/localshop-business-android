# Alpha Mart Business Android

Native Android receiver for the LocalShop engine.

## Architecture

Customer orders remain owned by the existing LocalShop backend/database. This Android app is the business-side receiver for Owner/Admin and Super Founder workflows.

Critical notification chain:

`customer checkout -> order saved -> trusted backend sends FCM data message -> FirebaseMessagingService -> Android notification/vibration -> tap opens correct order`

The backend must save the order **before** sending FCM. FCM failure must never roll back or block an order.

## FCM data payload contract

Send high-priority FCM **data** messages containing:

- `order_id`
- `order_number`
- `shop_id`
- `shop_name`
- `total`
- `role`: `owner` or `super_founder`

Owner device tokens must be scoped server-side to their Shop ID. Super Founder tokens may receive orders across shops. Never trust a client-provided Shop ID for authorization.

## Important Android limitation

The app creates a high-importance order notification channel and requests vibration. `setBypassDnd(true)` only takes effect where Android/OEM policy allows the app/channel to bypass DND. No application can guarantee bypass on every device without the OS granting notification-policy access. TTS is attempted from the FCM service, but Android/OEM background execution may prevent speech when the app process is cold; the notification itself is the reliable background mechanism.

## Still required on the LocalShop backend

1. Authenticated endpoint to register/refresh FCM tokens with `user_id`, role and authorized `shop_id`.
2. On successful order INSERT/transaction, server-side FCM multicast to the shop's registered owner/admin devices plus Super Founder devices.
3. Remove invalid/expired FCM tokens returned by FCM.
4. Send `android.priority=high` and data payload above.
5. Keep WhatsApp as a separate best-effort notification; never use it as the order database.

## Build

Open in Android Studio with JDK 17, sync Gradle and build the debug APK. Firebase Android package is `com.localshop.business`.
