package ir.farimod.app.network

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

suspend inline fun <reified T> safeApiCall(
    crossinline call: suspend () -> HttpResponse
): ApiResult<T> {
    return try {
        val response = call()
        val status = response.status
        println("[FarimodAPI] ⬅️ Response: ${status.value} ${status.description} | URL: ${response.request.url}")

        if (status.isSuccess()) {
            val body = response.body<T>()
            println("[FarimodAPI] ✅ Success body parsed for ${response.request.url}")
            ApiResult.Success(body)
        } else {
            val err = handleErrorResponse(response)
            println("[FarimodAPI] ⚠️ HttpError: status=${err.code}, message=${err.message}")
            err
        }
    } catch (e: ClientRequestException) {
        println("[FarimodAPI] ⚠️ ClientRequestException: status=${e.response.status.value}, message=${e.message}")
        handleErrorResponse(e.response)
    } catch (e: ServerResponseException) {
        println("[FarimodAPI] ⚠️ ServerResponseException: status=${e.response.status.value}, message=${e.message}")
        handleErrorResponse(e.response)
    } catch (e: HttpRequestTimeoutException) {
        println("[FarimodAPI] ⏱️ TimeoutException: ${e.message}")
        ApiResult.Error.TimeoutError
    } catch (e: SerializationException) {
        println("[FarimodAPI] 📦 SerializationException: ${e.message}")
        ApiResult.Error.SerializationError(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        println("[FarimodAPI] 💥 Exception: ${e::class.simpleName} - ${e.message}")
        val message = (e.message ?: e.cause?.message ?: "").lowercase()
        if (message.contains("timeout")) {
            ApiResult.Error.TimeoutError
        } else if (message.contains("unknownhost") || message.contains("connect") || message.contains("network") || message.contains("unresolved")) {
            ApiResult.Error.NetworkError(e)
        } else {
            ApiResult.Error.UnknownError(e)
        }
    }
}

suspend fun handleErrorResponse(response: HttpResponse): ApiResult.Error.HttpError {
    val code = response.status.value
    val bodyText = try {
        response.bodyAsText()
    } catch (_: Exception) {
        ""
    }

    println("[FarimodAPI] 📥 Raw Response Body (Code $code): $bodyText")

    var message: String? = null
    var validationErrors: Map<String, List<String>>? = null

    if (bodyText.isNotBlank()) {
        try {
            val json = Json { ignoreUnknownKeys = true; isLenient = true }
            val element = json.parseToJsonElement(bodyText)
            if (element is JsonObject) {
                // Check if ASP.NET validation error dictionary exists: "errors": { "Field": ["Error message"] }
                val errorsObj = element["errors"]
                if (errorsObj is JsonObject) {
                    val parsedErrors = mutableMapOf<String, List<String>>()
                    for ((field, errElement) in errorsObj) {
                        if (errElement is JsonArray) {
                            parsedErrors[field] = errElement.mapNotNull { it.jsonPrimitive.contentOrNull }
                        }
                    }
                    validationErrors = parsedErrors
                    message = parsedErrors.values.flatten().firstOrNull()
                }

                // Check standard message keys if not already extracted
                if (message == null) {
                    val msgElement = element["Message"] ?: element["message"] ?: element["error"] ?: element["title"]
                    if (msgElement is JsonPrimitive) {
                        message = msgElement.contentOrNull
                    }
                }
            }
        } catch (_: Exception) {
            // Ignored
        }
    }

    return ApiResult.Error.HttpError(
        code = code,
        message = message,
        validationErrors = validationErrors
    )
}
