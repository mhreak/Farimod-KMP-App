package ir.farimod.app.platform

/**
 * Common data model representing an in-app purchase across platforms.
 */
data class InAppPurchaseData(
    val sku: String,
    val orderId: String,
    val token: String,
    val originalJson: String,
    val signature: String,
    val developerPayload: String = ""
)

/**
 * Result of launching an in-app purchase flow.
 */
sealed class InAppPurchaseResult {
    data class Success(val purchase: InAppPurchaseData) : InAppPurchaseResult()
    data class Error(val code: Int, val message: String) : InAppPurchaseResult()
    data object UserCancelled : InAppPurchaseResult()
}

/**
 * Common abstraction for store in-app billing.
 * Implemented via Myket on Android, stubbed on iOS.
 */
expect object PlatformBilling {
    val isSupported: Boolean
    val isMarketInstalled: Boolean

    fun initialize(onFinished: (Boolean, String) -> Unit = { _, _ -> })

    fun queryPurchases(onResult: (List<InAppPurchaseData>) -> Unit)

    fun launchPurchase(
        sku: String,
        developerPayload: String = "",
        onResult: (InAppPurchaseResult) -> Unit
    )

    fun consumePurchase(
        token: String,
        onResult: (success: Boolean, message: String) -> Unit
    )
}
