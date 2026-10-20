package ir.farimod.app.platform

actual object PlatformBilling {
    actual val isSupported: Boolean = false
    actual val isMarketInstalled: Boolean = false

    actual fun initialize(onFinished: (Boolean, String) -> Unit) {
        onFinished(false, "Myket billing is only available on Android")
    }

    actual fun queryPurchases(onResult: (List<InAppPurchaseData>) -> Unit) {
        onResult(emptyList())
    }

    actual fun launchPurchase(
        sku: String,
        developerPayload: String,
        onResult: (InAppPurchaseResult) -> Unit
    ) {
        onResult(InAppPurchaseResult.Error(-1, "پرداخت مایکت تنها بر روی دستگاه‌های اندرویدی پشتیبانی می‌شود"))
    }

    actual fun consumePurchase(
        token: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        onResult(false, "Myket billing is only available on Android")
    }
}
