package ir.farimod.app.billing.myket

import android.app.Activity
import android.content.Context
import android.content.Intent

/**
 * Kotlin drop-in replacement for the Java [IabHelper] class described in the Myket documentation:
 * https://myket.ir/kb/pages/java/
 *
 * Provides the exact same methods and callback signatures adapted to modern Kotlin.
 */
class MyketBillingHelper(
    context: Context,
    base64PublicKey: String = MyketConfig.PUBLIC_KEY
) {
    private val client = MyketBillingClient(context, base64PublicKey)

    fun interface OnIabSetupFinishedListener {
        fun onIabSetupFinished(result: MyketResult)
    }

    fun interface QueryInventoryFinishedListener {
        fun onQueryInventoryFinished(result: MyketResult, inventory: MyketInventory?)
    }

    fun interface OnIabPurchaseFinishedListener {
        fun onIabPurchaseFinished(result: MyketResult, purchase: MyketPurchase?)
    }

    fun interface OnConsumeFinishedListener {
        fun onConsumeFinished(purchase: MyketPurchase?, result: MyketResult)
    }

    fun enableDebugLogging(enable: Boolean) {
        client.enableDebugLogging(enable)
    }

    fun startSetup(listener: OnIabSetupFinishedListener) {
        client.startSetup { result ->
            listener.onIabSetupFinished(result)
        }
    }

    fun queryInventoryAsync(
        querySkuDetails: Boolean,
        moreItemSkus: List<String>?,
        listener: QueryInventoryFinishedListener
    ) {
        client.queryInventoryAsync(querySkuDetails, moreItemSkus ?: emptyList()) { result, inventory ->
            listener.onQueryInventoryFinished(result, inventory)
        }
    }

    fun launchPurchaseFlow(
        activity: Activity,
        sku: String,
        listener: OnIabPurchaseFinishedListener,
        developerPayload: String = "",
        requestCode: Int = MyketBillingClient.DEFAULT_REQUEST_CODE
    ) {
        client.launchPurchaseFlow(activity, sku, developerPayload, requestCode) { result, purchase ->
            listener.onIabPurchaseFinished(result, purchase)
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return client.handleActivityResult(requestCode, resultCode, data)
    }

    fun consumeAsync(
        purchase: MyketPurchase,
        listener: OnConsumeFinishedListener
    ) {
        client.consumeAsync(purchase) { consumed, result ->
            listener.onConsumeFinished(consumed, result)
        }
    }

    fun dispose() {
        client.dispose()
    }

    fun disposeWhenFinished() {
        client.dispose()
    }
}
