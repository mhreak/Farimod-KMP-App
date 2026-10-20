package ir.farimod.app.billing.myket

import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Security and digital signature verification for Myket In-App purchases.
 * Corresponds to Security.java from Google IAB and Myket documentation.
 */
object MyketSecurity {
    private const val TAG = "MyketSecurity"
    private const val KEY_FACTORY_ALGORITHM = "RSA"
    private const val SIGNATURE_ALGORITHM = "SHA1withRSA"

    /**
     * Verifies that the data was signed with the private key corresponding to the given base64 public key.
     *
     * @param base64PublicKey The developer's RSA public key from Myket panel.
     * @param signedData The original JSON string containing purchase data.
     * @param signature The base64-encoded signature received from Myket.
     * @return true if the signature is valid, false otherwise.
     */
    fun verifyPurchase(base64PublicKey: String, signedData: String, signature: String): Boolean {
        if (signedData.isBlank() || base64PublicKey.isBlank() || signature.isBlank()) {
            Log.e(TAG, "Purchase verification failed: missing signedData, publicKey or signature.")
            return false
        }
        val key = generatePublicKey(base64PublicKey) ?: run {
            Log.e(TAG, "Cannot verify purchase: invalid public key.")
            return false
        }
        return verify(key, signedData, signature)
    }

    /**
     * Generates a [PublicKey] object from its base64-encoded string.
     */
    fun generatePublicKey(encodedPublicKey: String): PublicKey? {
        return try {
            val decodedKey = Base64.decode(encodedPublicKey.trim(), Base64.DEFAULT)
            val keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
            keyFactory.generatePublic(X509EncodedKeySpec(decodedKey))
        } catch (e: Exception) {
            Log.e(TAG, "Could not decode or generate public key", e)
            null
        }
    }

    /**
     * Verifies a signature against the given [PublicKey] using SHA1withRSA.
     */
    fun verify(publicKey: PublicKey, signedData: String, signature: String): Boolean {
        return try {
            val sig = Signature.getInstance(SIGNATURE_ALGORITHM)
            sig.initVerify(publicKey)
            sig.update(signedData.toByteArray(Charsets.UTF_8))
            val signatureBytes = Base64.decode(signature.trim(), Base64.DEFAULT)
            if (!sig.verify(signatureBytes)) {
                Log.e(TAG, "Signature verification failed: signature does not match.")
                false
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Signature verification exception", e)
            false
        }
    }
}
