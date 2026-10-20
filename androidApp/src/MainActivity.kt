package ir.farimod.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ir.farimod.app.platform.AndroidAppContext
import ir.farimod.app.platform.PlatformBilling

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Content draws edge-to-edge; each screen adds statusBarHeight() once.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        AndroidAppContext.appContext = applicationContext
        AndroidAppContext.currentActivity = this

        // Initialize Myket Billing service at app launch as recommended in Myket docs
        PlatformBilling.initialize { success, _ ->
            if (success) {
                // Update purchases at app startup
                PlatformBilling.queryPurchases { }
            }
        }

        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        AndroidAppContext.currentActivity = this
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (PlatformBilling.client.handleActivityResult(requestCode, resultCode, data)) {
            return
        }
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        if (AndroidAppContext.currentActivity === this) {
            AndroidAppContext.currentActivity = null
        }
        PlatformBilling.client.dispose()
        super.onDestroy()
    }
}

