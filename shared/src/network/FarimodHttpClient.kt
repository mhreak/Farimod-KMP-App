package ir.farimod.app.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object FarimodHttpClient {
    val jsonInstance = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    val client: HttpClient = HttpClient {
        followRedirects = true

        install(HttpRedirect) {
            checkHttpMethod = false
            allowHttpsDowngrade = false
        }

        install(ContentNegotiation) {
            json(jsonInstance)
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("[FarimodHTTP] $message")
                }
            }
            level = LogLevel.ALL
        }

        install(HttpTimeout) {
            requestTimeoutMillis = ApiConfig.TIMEOUT_MILLIS
            connectTimeoutMillis = ApiConfig.TIMEOUT_MILLIS
            socketTimeoutMillis = ApiConfig.TIMEOUT_MILLIS
        }

        defaultRequest {
            url(ApiConfig.BASE_URL)
            header(HttpHeaders.Accept, "*/*")
        }
    }
}
