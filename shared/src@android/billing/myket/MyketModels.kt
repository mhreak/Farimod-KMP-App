package ir.farimod.app.billing.myket

import org.json.JSONObject

/**
 * Represents an In-App purchase received from Myket.
 */
data class MyketPurchase(
    val itemType: String,
    val orderId: String,
    val packageName: String,
    val sku: String,
    val purchaseTime: Long,
    val purchaseState: Int,
    val developerPayload: String,
    val token: String,
    val originalJson: String,
    val signature: String,
    val isAutoRenewing: Boolean = false
) {
    companion object {
        fun fromJson(itemType: String, originalJson: String, signature: String): MyketPurchase {
            val json = JSONObject(originalJson)
            return MyketPurchase(
                itemType = itemType,
                orderId = json.optString("orderId"),
                packageName = json.optString("packageName"),
                sku = json.optString("productId"),
                purchaseTime = json.optLong("purchaseTime"),
                purchaseState = json.optInt("purchaseState"),
                developerPayload = json.optString("developerPayload"),
                token = json.optString("token").ifEmpty { json.optString("purchaseToken") },
                originalJson = originalJson,
                signature = signature,
                isAutoRenewing = json.optBoolean("autoRenewing", false)
            )
        }
    }
}

/**
 * Represents SKU details (product information) returned from Myket.
 */
data class MyketSkuDetails(
    val itemType: String,
    val sku: String,
    val type: String,
    val price: String,
    val title: String,
    val description: String,
    val originalJson: String
) {
    companion object {
        fun fromJson(itemType: String, originalJson: String): MyketSkuDetails {
            val json = JSONObject(originalJson)
            return MyketSkuDetails(
                itemType = itemType,
                sku = json.optString("productId"),
                type = json.optString("type"),
                price = json.optString("price"),
                title = json.optString("title"),
                description = json.optString("description"),
                originalJson = originalJson
            )
        }
    }
}

/**
 * Holds queried inventory: owned purchases and SKU details.
 */
class MyketInventory {
    private val skuMap = mutableMapOf<String, MyketSkuDetails>()
    private val purchaseMap = mutableMapOf<String, MyketPurchase>()

    fun getSkuDetails(sku: String): MyketSkuDetails? = skuMap[sku]
    fun getPurchase(sku: String): MyketPurchase? = purchaseMap[sku]
    fun hasPurchase(sku: String): Boolean = purchaseMap.containsKey(sku)
    fun getAllPurchases(): List<MyketPurchase> = purchaseMap.values.toList()
    fun getAllSkuDetails(): List<MyketSkuDetails> = skuMap.values.toList()

    internal fun addSkuDetails(skuDetails: MyketSkuDetails) {
        skuMap[skuDetails.sku] = skuDetails
    }

    internal fun addPurchase(purchase: MyketPurchase) {
        purchaseMap[purchase.sku] = purchase
    }

    internal fun removePurchase(sku: String) {
        purchaseMap.remove(sku)
    }
}

/**
 * Result wrapper for billing operations.
 */
data class MyketResult(
    val responseCode: Int,
    val message: String
) {
    val isSuccess: Boolean get() = responseCode == MyketConfig.BILLING_RESPONSE_RESULT_OK
    val isFailure: Boolean get() = !isSuccess

    companion object {
        fun ok(message: String = "موفق"): MyketResult =
            MyketResult(MyketConfig.BILLING_RESPONSE_RESULT_OK, message)

        fun error(responseCode: Int, message: String? = null): MyketResult =
            MyketResult(responseCode, message ?: MyketConfig.getResponseDescription(responseCode))
    }
}

class MyketException(
    val result: MyketResult,
    cause: Throwable? = null
) : Exception(result.message, cause)
