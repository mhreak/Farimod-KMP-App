package ir.farimod.app.platform

import ir.farimod.app.billing.myket.MyketBillingClient
import ir.farimod.app.billing.myket.MyketConfig
import ir.farimod.app.billing.myket.MyketPurchase

actual object PlatformBilling {
    actual val isSupported: Boolean
        get() = when (AppBuildConfig.activeDistribution) {
            AppDistribution.MYKET -> true
            AppDistribution.CAFE_BAZAAR -> false // Placeholder until Cafe Bazaar IAP is implemented
            AppDistribution.WEBSITE -> false // Direct web gateway only
        }

    val client: MyketBillingClient by lazy {
        MyketBillingClient(AndroidAppContext.appContext, MyketConfig.PUBLIC_KEY)
    }

    actual val isMarketInstalled: Boolean
        get() = when (AppBuildConfig.activeDistribution) {
            AppDistribution.MYKET -> client.isMyketInstalled()
            AppDistribution.CAFE_BAZAAR -> false
            AppDistribution.WEBSITE -> false
        }

    actual fun initialize(onFinished: (Boolean, String) -> Unit) {
        if (!isSupported) {
            onFinished(false, "In-app store billing disabled for ${AppBuildConfig.activeDistribution.titleFa}")
            return
        }
        client.startSetup { result ->
            onFinished(result.isSuccess, result.message)
        }
    }

    actual fun queryPurchases(onResult: (List<InAppPurchaseData>) -> Unit) {
        client.queryInventoryAsync(querySkuDetails = false) { result, inventory ->
            if (result.isSuccess && inventory != null) {
                val list = inventory.getAllPurchases().map { it.toCommon() }
                onResult(list)
            } else {
                onResult(emptyList())
            }
        }
    }

    actual fun launchPurchase(
        sku: String,
        developerPayload: String,
        onResult: (InAppPurchaseResult) -> Unit
    ) {
        val activity = AndroidAppContext.currentActivity
        if (activity == null) {
            println("[FarimodBilling] ❌ Current Activity is null! Cannot launch purchase.")
            onResult(InAppPurchaseResult.Error(-1, "اکتیویتی برنامه در دسترس نیست"))
            return
        }

        println("[FarimodBilling] 📲 PlatformBilling.launchPurchase -> client.launchPurchaseFlow(sku=$sku, payload=$developerPayload)")
        client.launchPurchaseFlow(activity, sku, developerPayload) { result, purchase ->
            println("[FarimodBilling] 📩 client.launchPurchaseFlow callback: responseCode=${result.responseCode}, isSuccess=${result.isSuccess}")
            if (result.isSuccess && purchase != null) {
                onResult(InAppPurchaseResult.Success(purchase.toCommon()))
            } else if (result.responseCode == MyketConfig.BILLING_RESPONSE_RESULT_USER_CANCELED) {
                onResult(InAppPurchaseResult.UserCancelled)
            } else {
                onResult(InAppPurchaseResult.Error(result.responseCode, result.message))
            }
        }
    }

    actual fun consumePurchase(
        token: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        println("[FarimodBilling] 📲 PlatformBilling.consumePurchase -> token=$token")
        val dummyPurchase = MyketPurchase(
            itemType = MyketConfig.ITEM_TYPE_INAPP,
            orderId = "",
            packageName = AndroidAppContext.appContext.packageName,
            sku = "",
            purchaseTime = 0,
            purchaseState = 0,
            developerPayload = "",
            token = token,
            originalJson = "",
            signature = ""
        )
        client.consumeAsync(dummyPurchase) { _, result ->
            println("[FarimodBilling] 📩 client.consumeAsync callback: isSuccess=${result.isSuccess}, message=${result.message}")
            onResult(result.isSuccess, result.message)
        }
    }

    private fun MyketPurchase.toCommon(): InAppPurchaseData = InAppPurchaseData(
        sku = sku,
        orderId = orderId,
        token = token,
        originalJson = originalJson,
        signature = signature,
        developerPayload = developerPayload
    )
}
