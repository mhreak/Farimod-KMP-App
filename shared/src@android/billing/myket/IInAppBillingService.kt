package ir.farimod.app.billing.myket

import android.os.Bundle
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel

/**
 * Kotlin IPC client interface for Android In-App Billing (AIDL: com.android.vending.billing.IInAppBillingService).
 * Implemented directly using [android.os.IBinder] transactions so it requires no external AIDL compilers.
 */
interface IInAppBillingService : IInterface {

    fun isBillingSupported(apiVersion: Int, packageName: String, type: String): Int

    fun getSkuDetails(apiVersion: Int, packageName: String, type: String, skusBundle: Bundle): Bundle?

    fun getBuyIntent(
        apiVersion: Int,
        packageName: String,
        sku: String,
        type: String,
        developerPayload: String
    ): Bundle?

    fun getPurchases(
        apiVersion: Int,
        packageName: String,
        type: String,
        continuationToken: String?
    ): Bundle?

    fun consumePurchase(apiVersion: Int, packageName: String, purchaseToken: String): Int

    fun getBuyIntentV2(
        apiVersion: Int,
        packageName: String,
        sku: String,
        type: String,
        developerPayload: String
    ): Bundle?

    fun getPurchaseConfig(apiVersion: Int): Bundle?

    companion object {
        private const val DESCRIPTOR = "com.android.vending.billing.IInAppBillingService"

        private const val TRANSACTION_isBillingSupported = IBinder.FIRST_CALL_TRANSACTION + 0
        private const val TRANSACTION_getSkuDetails = IBinder.FIRST_CALL_TRANSACTION + 1
        private const val TRANSACTION_getBuyIntent = IBinder.FIRST_CALL_TRANSACTION + 2
        private const val TRANSACTION_getPurchases = IBinder.FIRST_CALL_TRANSACTION + 3
        private const val TRANSACTION_consumePurchase = IBinder.FIRST_CALL_TRANSACTION + 4
        private const val TRANSACTION_getBuyIntentV2 = IBinder.FIRST_CALL_TRANSACTION + 5
        private const val TRANSACTION_getPurchaseConfig = IBinder.FIRST_CALL_TRANSACTION + 6

        fun asInterface(binder: IBinder?): IInAppBillingService? {
            if (binder == null) return null
            val iin = binder.queryLocalInterface(DESCRIPTOR)
            if (iin is IInAppBillingService) return iin
            return Proxy(binder)
        }
    }

    private class Proxy(private val remote: IBinder) : IInAppBillingService {
        override fun asBinder(): IBinder = remote

        override fun isBillingSupported(apiVersion: Int, packageName: String, type: String): Int {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            return try {
                data.writeInterfaceToken(DESCRIPTOR)
                data.writeInt(apiVersion)
                data.writeString(packageName)
                data.writeString(type)
                remote.transact(TRANSACTION_isBillingSupported, data, reply, 0)
                reply.readException()
                reply.readInt()
            } finally {
                reply.recycle()
                data.recycle()
            }
        }

        override fun getSkuDetails(
            apiVersion: Int,
            packageName: String,
            type: String,
            skusBundle: Bundle
        ): Bundle? {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            return try {
                data.writeInterfaceToken(DESCRIPTOR)
                data.writeInt(apiVersion)
                data.writeString(packageName)
                data.writeString(type)
                data.writeInt(1)
                skusBundle.writeToParcel(data, 0)
                remote.transact(TRANSACTION_getSkuDetails, data, reply, 0)
                reply.readException()
                if (reply.readInt() != 0) Bundle.CREATOR.createFromParcel(reply) else null
            } finally {
                reply.recycle()
                data.recycle()
            }
        }

        override fun getBuyIntent(
            apiVersion: Int,
            packageName: String,
            sku: String,
            type: String,
            developerPayload: String
        ): Bundle? {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            return try {
                data.writeInterfaceToken(DESCRIPTOR)
                data.writeInt(apiVersion)
                data.writeString(packageName)
                data.writeString(sku)
                data.writeString(type)
                data.writeString(developerPayload)
                remote.transact(TRANSACTION_getBuyIntent, data, reply, 0)
                reply.readException()
                if (reply.readInt() != 0) Bundle.CREATOR.createFromParcel(reply) else null
            } finally {
                reply.recycle()
                data.recycle()
            }
        }

        override fun getPurchases(
            apiVersion: Int,
            packageName: String,
            type: String,
            continuationToken: String?
        ): Bundle? {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            return try {
                data.writeInterfaceToken(DESCRIPTOR)
                data.writeInt(apiVersion)
                data.writeString(packageName)
                data.writeString(type)
                data.writeString(continuationToken)
                remote.transact(TRANSACTION_getPurchases, data, reply, 0)
                reply.readException()
                if (reply.readInt() != 0) Bundle.CREATOR.createFromParcel(reply) else null
            } finally {
                reply.recycle()
                data.recycle()
            }
        }

        override fun consumePurchase(
            apiVersion: Int,
            packageName: String,
            purchaseToken: String
        ): Int {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            return try {
                data.writeInterfaceToken(DESCRIPTOR)
                data.writeInt(apiVersion)
                data.writeString(packageName)
                data.writeString(purchaseToken)
                remote.transact(TRANSACTION_consumePurchase, data, reply, 0)
                reply.readException()
                reply.readInt()
            } finally {
                reply.recycle()
                data.recycle()
            }
        }

        override fun getBuyIntentV2(
            apiVersion: Int,
            packageName: String,
            sku: String,
            type: String,
            developerPayload: String
        ): Bundle? {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            return try {
                data.writeInterfaceToken(DESCRIPTOR)
                data.writeInt(apiVersion)
                data.writeString(packageName)
                data.writeString(sku)
                data.writeString(type)
                data.writeString(developerPayload)
                remote.transact(TRANSACTION_getBuyIntentV2, data, reply, 0)
                reply.readException()
                if (reply.readInt() != 0) Bundle.CREATOR.createFromParcel(reply) else null
            } finally {
                reply.recycle()
                data.recycle()
            }
        }

        override fun getPurchaseConfig(apiVersion: Int): Bundle? {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            return try {
                data.writeInterfaceToken(DESCRIPTOR)
                data.writeInt(apiVersion)
                remote.transact(TRANSACTION_getPurchaseConfig, data, reply, 0)
                reply.readException()
                if (reply.readInt() != 0) Bundle.CREATOR.createFromParcel(reply) else null
            } finally {
                reply.recycle()
                data.recycle()
            }
        }
    }
}
