package ir.farimod.app.billing.myket

import android.app.Activity
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.util.ArrayList

/**
 * Idiomatic, Coroutine-friendly Kotlin Client for Myket In-App Billing.
 * Supports both suspend functions and standard callback listeners.
 */
class MyketBillingClient(
    private val context: Context,
    private val publicKey: String = MyketConfig.PUBLIC_KEY
) {
    companion object {
        private const val TAG = "MyketBillingClient"
        const val DEFAULT_REQUEST_CODE: Int = 10001
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val appContext: Context = context.applicationContext

    private var billingService: IInAppBillingService? = null
    private var serviceConnection: ServiceConnection? = null

    private var isDisposed: Boolean = false
    private var isSetupDone: Boolean = false
    private var isDebugLogging: Boolean = true

    // Active purchase callback
    private var activePurchaseRequestCode: Int = DEFAULT_REQUEST_CODE
    private var activePurchaseSku: String? = null
    private var activePurchaseCallback: ((MyketResult, MyketPurchase?) -> Unit)? = null
    private var activePurchaseDeferred: CompletableDeferred<MyketPurchase>? = null

    fun enableDebugLogging(enable: Boolean) {
        isDebugLogging = enable
    }

    private fun logDebug(message: String) {
        if (isDebugLogging) {
            Log.i("FarimodBilling", message)
            println("[FarimodBilling] $message")
        }
    }

    private fun logError(message: String, throwable: Throwable? = null) {
        Log.e("FarimodBilling", message, throwable)
        println("[FarimodBilling] ❌ ERROR: $message ${throwable?.message ?: ""}")
    }

    /**
     * Checks if Myket app is installed on the user's device.
     */
    fun isMyketInstalled(): Boolean {
        val pm = appContext.packageManager
        val hasPackage = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(MyketConfig.MARKET_PACKAGE, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(MyketConfig.MARKET_PACKAGE, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
        if (hasPackage) return true

        return try {
            val serviceIntent = Intent(MyketConfig.BIND_ACTION).apply {
                setPackage(MyketConfig.MARKET_PACKAGE)
            }
            val resolveList = pm.queryIntentServices(serviceIntent, 0)
            resolveList.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Connects and initializes the Myket billing service.
     */
    fun startSetup(onFinished: (MyketResult) -> Unit) {
        if (isDisposed) {
            onFinished(MyketResult.error(MyketConfig.IABHELPER_UNKNOWN_ERROR, "Billing client has been disposed."))
            return
        }
        if (isSetupDone && billingService != null) {
            logDebug("Billing service already connected and setup.")
            onFinished(MyketResult.ok("اتصال از قبل برقرار است"))
            return
        }

        if (!isMyketInstalled()) {
            logError("Myket app is not installed.")
            onFinished(MyketResult.error(MyketConfig.IABHELPER_MYKET_NOT_INSTALLED))
            return
        }

        val serviceIntent = Intent(MyketConfig.BIND_ACTION).apply {
            setPackage(MyketConfig.MARKET_PACKAGE)
        }

        val resolveList = appContext.packageManager.queryIntentServices(serviceIntent, 0)
        if (resolveList.isEmpty()) {
            logError("Myket billing service intent cannot be resolved.")
            onFinished(MyketResult.error(MyketConfig.BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE))
            return
        }

        logDebug("Binding to Myket InAppBillingService...")
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                if (isDisposed) return
                logDebug("Myket billing service connected.")
                billingService = IInAppBillingService.asInterface(service)

                scope.launch(Dispatchers.IO) {
                    try {
                        val packageName = appContext.packageName
                        val response = billingService?.isBillingSupported(
                            MyketConfig.API_VERSION,
                            packageName,
                            MyketConfig.ITEM_TYPE_INAPP
                        ) ?: MyketConfig.BILLING_RESPONSE_RESULT_ERROR

                        withContext(Dispatchers.Main) {
                            if (response == MyketConfig.BILLING_RESPONSE_RESULT_OK) {
                                isSetupDone = true
                                logDebug("Setup completed successfully. Billing is supported.")
                                onFinished(MyketResult.ok("اتصال به سرویس مایکت با موفقیت انجام شد"))
                            } else {
                                isSetupDone = false
                                logError("Billing not supported for package $packageName, response: $response")
                                onFinished(MyketResult.error(response))
                            }
                        }
                    } catch (e: Exception) {
                        logError("Remote exception during isBillingSupported check", e)
                        withContext(Dispatchers.Main) {
                            onFinished(MyketResult.error(MyketConfig.IABHELPER_REMOTE_EXCEPTION, e.message))
                        }
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                logDebug("Myket billing service disconnected.")
                billingService = null
                isSetupDone = false
            }
        }

        val bound = appContext.bindService(serviceIntent, serviceConnection!!, Context.BIND_AUTO_CREATE)
        if (!bound) {
            logError("Failed to bind to Myket InAppBillingService.")
            onFinished(MyketResult.error(MyketConfig.BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE))
        }
    }

    /**
     * Coroutine-based setup.
     */
    suspend fun startSetup(): Result<Unit> = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<Result<Unit>>()
        startSetup { result ->
            if (result.isSuccess) deferred.complete(Result.success(Unit))
            else deferred.complete(Result.failure(MyketException(result)))
        }
        deferred.await()
    }

    /**
     * Queries owned purchases and optionally product details (price, title, description).
     *
     * @param querySkuDetails If true, queries SKU details for items defined in [itemSkus].
     * @param itemSkus List of product SKUs to query details for.
     */
    fun queryInventoryAsync(
        querySkuDetails: Boolean,
        itemSkus: List<String> = emptyList(),
        onFinished: (MyketResult, MyketInventory?) -> Unit
    ) {
        if (!isSetupDone || billingService == null) {
            onFinished(MyketResult.error(MyketConfig.IABHELPER_UNKNOWN_ERROR, "سرویس مایکت هنوز متصل نشده است. ابتدا startSetup را فراخوانی کنید"), null)
            return
        }

        scope.launch(Dispatchers.IO) {
            val inventory = MyketInventory()
            val packageName = appContext.packageName

            try {
                // 1. Query owned purchases
                var continuationToken: String? = null
                do {
                    val purchaseBundle = billingService?.getPurchases(
                        MyketConfig.API_VERSION,
                        packageName,
                        MyketConfig.ITEM_TYPE_INAPP,
                        continuationToken
                    )

                    val response = purchaseBundle?.getInt(MyketConfig.RESPONSE_CODE, -1) ?: -1
                    if (response != MyketConfig.BILLING_RESPONSE_RESULT_OK) {
                        withContext(Dispatchers.Main) {
                            onFinished(MyketResult.error(response, "خطا در دریافت خریدهای کاربر"), null)
                        }
                        return@launch
                    }

                    val purchaseDataList = purchaseBundle?.getStringArrayList(MyketConfig.INAPP_PURCHASE_DATA_LIST)
                    val signatureList = purchaseBundle?.getStringArrayList(MyketConfig.INAPP_DATA_SIGNATURE_LIST)

                    if (purchaseDataList != null && signatureList != null) {
                        for (i in purchaseDataList.indices) {
                            val purchaseJson = purchaseDataList[i]
                            val signature = signatureList[i]
                            if (MyketSecurity.verifyPurchase(publicKey, purchaseJson, signature)) {
                                val purchase = MyketPurchase.fromJson(MyketConfig.ITEM_TYPE_INAPP, purchaseJson, signature)
                                inventory.addPurchase(purchase)
                            } else {
                                logError("Purchase signature verification failed for: $purchaseJson")
                            }
                        }
                    }

                    continuationToken = purchaseBundle?.getString(MyketConfig.INAPP_CONTINUATION_TOKEN)
                } while (!continuationToken.isNullOrEmpty())

                // 2. Query SKU details if requested
                if (querySkuDetails && itemSkus.isNotEmpty()) {
                    val batches = itemSkus.chunked(20) // API limit is 20 items per request
                    for (batch in batches) {
                        val queryBundle = Bundle().apply {
                            putStringArrayList(MyketConfig.ITEM_ID_LIST, ArrayList(batch))
                        }
                        val skuBundle = billingService?.getSkuDetails(
                            MyketConfig.API_VERSION,
                            packageName,
                            MyketConfig.ITEM_TYPE_INAPP,
                            queryBundle
                        )
                        val skuResponse = skuBundle?.getInt(MyketConfig.RESPONSE_CODE, -1) ?: -1
                        if (skuResponse == MyketConfig.BILLING_RESPONSE_RESULT_OK) {
                            val detailsList = skuBundle?.getStringArrayList(MyketConfig.DETAILS_LIST)
                            detailsList?.forEach { jsonStr ->
                                try {
                                    val skuDetails = MyketSkuDetails.fromJson(MyketConfig.ITEM_TYPE_INAPP, jsonStr)
                                    inventory.addSkuDetails(skuDetails)
                                } catch (e: JSONException) {
                                    logError("Failed to parse SKU details JSON: $jsonStr", e)
                                }
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    onFinished(MyketResult.ok("اطلاعات خرید با موفقیت به‌روزرسانی شد"), inventory)
                }
            } catch (e: Exception) {
                logError("Exception in queryInventoryAsync", e)
                withContext(Dispatchers.Main) {
                    onFinished(MyketResult.error(MyketConfig.IABHELPER_REMOTE_EXCEPTION, e.message), null)
                }
            }
        }
    }

    /**
     * Coroutine-based inventory query.
     */
    suspend fun queryInventory(
        querySkuDetails: Boolean = true,
        itemSkus: List<String> = emptyList()
    ): Result<MyketInventory> = withContext(Dispatchers.IO) {
        val deferred = CompletableDeferred<Result<MyketInventory>>()
        queryInventoryAsync(querySkuDetails, itemSkus) { result, inventory ->
            if (result.isSuccess && inventory != null) {
                deferred.complete(Result.success(inventory))
            } else {
                deferred.complete(Result.failure(MyketException(result)))
            }
        }
        deferred.await()
    }

    /**
     * Launches the Myket purchase flow dialog on top of the given [activity].
     *
     * @param activity The calling activity to launch IntentSender.
     * @param sku The product ID to buy.
     * @param developerPayload Optional unique token for purchase validation.
     * @param requestCode Request code for onActivityResult (default: [DEFAULT_REQUEST_CODE]).
     */
    fun launchPurchaseFlow(
        activity: Activity,
        sku: String,
        developerPayload: String = "",
        requestCode: Int = DEFAULT_REQUEST_CODE,
        onFinished: (MyketResult, MyketPurchase?) -> Unit
    ) {
        if (!isSetupDone || billingService == null) {
            logDebug("Billing service not connected yet. Connecting now...")
            startSetup { setupResult ->
                if (setupResult.isSuccess && billingService != null) {
                    launchPurchaseFlow(activity, sku, developerPayload, requestCode, onFinished)
                } else {
                    onFinished(setupResult, null)
                }
            }
            return
        }

        activePurchaseRequestCode = requestCode
        activePurchaseSku = sku
        activePurchaseCallback = onFinished

        scope.launch(Dispatchers.IO) {
            try {
                val buyBundle = billingService?.getBuyIntent(
                    MyketConfig.API_VERSION,
                    appContext.packageName,
                    sku,
                    MyketConfig.ITEM_TYPE_INAPP,
                    developerPayload
                )

                val responseCode = buyBundle?.getInt(MyketConfig.RESPONSE_CODE, -1) ?: -1
                if (responseCode != MyketConfig.BILLING_RESPONSE_RESULT_OK) {
                    withContext(Dispatchers.Main) {
                        activePurchaseCallback?.invoke(MyketResult.error(responseCode), null)
                        clearActivePurchase()
                    }
                    return@launch
                }

                @Suppress("DEPRECATION")
                val pendingIntent = buyBundle?.getParcelable<PendingIntent>(MyketConfig.BUY_INTENT)
                if (pendingIntent == null) {
                    withContext(Dispatchers.Main) {
                        activePurchaseCallback?.invoke(
                            MyketResult.error(MyketConfig.IABHELPER_BAD_RESPONSE, "BuyIntent pending intent is null"),
                            null
                        )
                        clearActivePurchase()
                    }
                    return@launch
                }

                logDebug("Launching IntentSender for purchase of SKU: $sku")
                withContext(Dispatchers.Main) {
                    try {
                        val optionsBundle = createBackgroundActivityStartOptions()
                        activity.startIntentSenderForResult(
                            pendingIntent.intentSender,
                            requestCode,
                            Intent(),
                            0,
                            0,
                            0,
                            optionsBundle
                        )
                    } catch (e: Exception) {
                        logError("Failed to start IntentSender for purchase", e)
                        val cb = activePurchaseCallback
                        val def = activePurchaseDeferred
                        clearActivePurchase()
                        cb?.invoke(
                            MyketResult.error(MyketConfig.IABHELPER_SEND_INTENT_FAILED, e.message),
                            null
                        )
                        def?.completeExceptionally(e)
                    }
                }
            } catch (e: Exception) {
                logError("RemoteException in getBuyIntent", e)
                withContext(Dispatchers.Main) {
                    val cb = activePurchaseCallback
                    val def = activePurchaseDeferred
                    clearActivePurchase()
                    cb?.invoke(
                        MyketResult.error(MyketConfig.IABHELPER_REMOTE_EXCEPTION, e.message),
                        null
                    )
                    def?.completeExceptionally(e)
                }
            }
        }
    }

    /**
     * Coroutine-based purchase launcher.
     */
    suspend fun launchPurchaseFlow(
        activity: Activity,
        sku: String,
        developerPayload: String = "",
        requestCode: Int = DEFAULT_REQUEST_CODE
    ): Result<MyketPurchase> = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<MyketPurchase>()
        activePurchaseDeferred = deferred

        launchPurchaseFlow(activity, sku, developerPayload, requestCode) { result, purchase ->
            if (result.isSuccess && purchase != null) {
                deferred.complete(purchase)
            } else {
                deferred.completeExceptionally(MyketException(result))
            }
        }
        try {
            Result.success(deferred.await())
        } catch (e: Throwable) {
            Result.failure(e)
        } finally {
            activePurchaseDeferred = null
        }
    }

    /**
     * Must be called from Activity's [Activity.onActivityResult] to process the purchase result from Myket.
     * Returns true if the result was handled by this billing client.
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (activePurchaseRequestCode == -1 || requestCode != activePurchaseRequestCode) return false

        logDebug("handleActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        val callback = activePurchaseCallback
        val deferred = activePurchaseDeferred
        val sku = activePurchaseSku

        // Clear active purchase state immediately to prevent duplicate invocations
        clearActivePurchase()

        if (resultCode == Activity.RESULT_CANCELED) {
            val responseCode = data?.getIntExtra(MyketConfig.RESPONSE_CODE, MyketConfig.BILLING_RESPONSE_RESULT_USER_CANCELED)
                ?: MyketConfig.BILLING_RESPONSE_RESULT_USER_CANCELED
            val message = if (responseCode == MyketConfig.BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE) {
                "کالای مورد نظر در پنل مایکت یافت نشد. لطفاً محصول ($sku) را در پنل مایکت تعریف کنید."
            } else {
                "پرداخت لغو شد یا محصول ($sku) در پنل مایکت تعریف نشده است."
            }
            logDebug("Purchase canceled: $message (responseCode=$responseCode)")
            val result = MyketResult.error(responseCode, message)
            callback?.invoke(result, null)
            deferred?.completeExceptionally(MyketException(result))
            return true
        }

        if (data == null) {
            val result = MyketResult.error(MyketConfig.IABHELPER_BAD_RESPONSE, "داده‌های خرید از مایکت دریافت نشد")
            callback?.invoke(result, null)
            deferred?.completeExceptionally(MyketException(result))
            return true
        }

        val responseCode = data.getIntExtra(MyketConfig.RESPONSE_CODE, -1)
        val purchaseData = data.getStringExtra(MyketConfig.INAPP_PURCHASE_DATA)
        val dataSignature = data.getStringExtra(MyketConfig.INAPP_DATA_SIGNATURE)

        if (resultCode == Activity.RESULT_OK && responseCode == MyketConfig.BILLING_RESPONSE_RESULT_OK) {
            if (purchaseData.isNullOrBlank() || dataSignature.isNullOrBlank()) {
                val result = MyketResult.error(MyketConfig.IABHELPER_BAD_RESPONSE, "اطلاعات خرید یا امضای دیجیتالی خالی است")
                callback?.invoke(result, null)
                deferred?.completeExceptionally(MyketException(result))
                return true
            }

            // Verify RSA signature
            val isSignatureValid = MyketSecurity.verifyPurchase(publicKey, purchaseData, dataSignature)
            if (!isSignatureValid) {
                logError("Purchase verification failed! Signature does not match public key.")
                val result = MyketResult.error(MyketConfig.IABHELPER_VERIFICATION_FAILED)
                callback?.invoke(result, null)
                deferred?.completeExceptionally(MyketException(result))
                return true
            }

            try {
                val purchase = MyketPurchase.fromJson(MyketConfig.ITEM_TYPE_INAPP, purchaseData, dataSignature)
                logDebug("Purchase successful: $purchase")
                callback?.invoke(MyketResult.ok("خرید با موفقیت انجام شد"), purchase)
                deferred?.complete(purchase)
            } catch (e: Exception) {
                logError("Error parsing purchase JSON: $purchaseData", e)
                val result = MyketResult.error(MyketConfig.IABHELPER_BAD_RESPONSE, e.message)
                callback?.invoke(result, null)
                deferred?.completeExceptionally(MyketException(result))
            }
        } else if (responseCode == MyketConfig.BILLING_RESPONSE_RESULT_USER_CANCELED) {
            logDebug("Purchase cancelled by user.")
            val result = MyketResult.error(MyketConfig.BILLING_RESPONSE_RESULT_USER_CANCELED)
            callback?.invoke(result, null)
            deferred?.completeExceptionally(MyketException(result))
        } else {
            logError("Purchase failed with response code: $responseCode")
            val result = MyketResult.error(responseCode)
            callback?.invoke(result, null)
            deferred?.completeExceptionally(MyketException(result))
        }

        return true
    }

    private fun clearActivePurchase() {
        activePurchaseCallback = null
        activePurchaseSku = null
        activePurchaseRequestCode = -1
        activePurchaseDeferred = null
    }

    /**
     * Builds [ActivityOptions] bundle to grant Background Activity Launch (BAL) privileges on Android 14+ (API 34+).
     * Without this, Android blocks launching Myket's [InAppPurchaseActivity] IntentSender from background service.
     */
    private fun createBackgroundActivityStartOptions(): Bundle? {
        if (Build.VERSION.SDK_INT < 34) return null
        return try {
            val options = ActivityOptions.makeBasic()
            // Android 14 (API 34) Background Activity Launch mode: MODE_BACKGROUND_ACTIVITY_START_ALLOWED = 1
            try {
                val method = ActivityOptions::class.java.getMethod(
                    "setPendingIntentBackgroundActivityStartMode",
                    Int::class.javaPrimitiveType
                )
                method.invoke(options, 1)
            } catch (_: Throwable) {}

            // Android 15 (API 35) PendingIntent creator background activity start mode
            try {
                val method = ActivityOptions::class.java.getMethod(
                    "setPendingIntentCreatorBackgroundActivityStartMode",
                    Int::class.javaPrimitiveType
                )
                method.invoke(options, 1)
            } catch (_: Throwable) {}

            options.toBundle()
        } catch (e: Throwable) {
            logError("Failed to create ActivityOptions with BAL permissions", e)
            null
        }
    }

    /**
     * Consumes a purchased item to make it available for purchase again.
     * Required for consumable products before delivering benefits to user.
     */
    fun consumeAsync(
        purchase: MyketPurchase,
        onFinished: (MyketPurchase?, MyketResult) -> Unit
    ) {
        if (!isSetupDone || billingService == null) {
            onFinished(purchase, MyketResult.error(MyketConfig.IABHELPER_UNKNOWN_ERROR, "سرویس مایکت متصل نیست"))
            return
        }

        val token = purchase.token
        if (token.isBlank()) {
            onFinished(purchase, MyketResult.error(MyketConfig.IABHELPER_MISSING_TOKEN, "توکن خرید برای مصرف خالی است"))
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val response = billingService?.consumePurchase(
                    MyketConfig.API_VERSION,
                    appContext.packageName,
                    token
                ) ?: MyketConfig.BILLING_RESPONSE_RESULT_ERROR

                withContext(Dispatchers.Main) {
                    if (response == MyketConfig.BILLING_RESPONSE_RESULT_OK) {
                        logDebug("Successfully consumed purchase token: $token")
                        onFinished(purchase, MyketResult.ok("محصول با موفقیت مصرف شد"))
                    } else {
                        logError("Failed to consume purchase. Code: $response")
                        onFinished(purchase, MyketResult.error(response))
                    }
                }
            } catch (e: Exception) {
                logError("RemoteException in consumePurchase", e)
                withContext(Dispatchers.Main) {
                    onFinished(purchase, MyketResult.error(MyketConfig.IABHELPER_REMOTE_EXCEPTION, e.message))
                }
            }
        }
    }

    /**
     * Coroutine-based purchase consumption.
     */
    suspend fun consume(purchase: MyketPurchase): Result<MyketPurchase> = withContext(Dispatchers.IO) {
        val deferred = CompletableDeferred<Result<MyketPurchase>>()
        consumeAsync(purchase) { consumed, result ->
            if (result.isSuccess && consumed != null) {
                deferred.complete(Result.success(consumed))
            } else {
                deferred.complete(Result.failure(MyketException(result)))
            }
        }
        deferred.await()
    }

    /**
     * Closes the connection to Myket InAppBillingService and prevents memory leaks.
     * Call this in Activity's onDestroy.
     */
    fun dispose() {
        logDebug("Disposing MyketBillingClient...")
        isDisposed = true
        isSetupDone = false
        if (serviceConnection != null) {
            try {
                appContext.unbindService(serviceConnection!!)
            } catch (_: Exception) {}
            serviceConnection = null
        }
        billingService = null
        clearActivePurchase()
    }
}
