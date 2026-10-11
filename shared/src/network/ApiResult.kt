package ir.farimod.app.network

/**
 * A sealed interface representing the result of an API call.
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>

    sealed interface Error : ApiResult<Nothing> {
        data class HttpError(
            val code: Int,
            val message: String? = null,
            val validationErrors: Map<String, List<String>>? = null
        ) : Error

        data class NetworkError(val cause: Throwable) : Error
        data object TimeoutError : Error
        data class SerializationError(val cause: Throwable) : Error
        data class UnknownError(val cause: Throwable) : Error
    }
}

/**
 * Resolves an [ApiResult.Error] to a localized user-friendly Persian string.
 */
fun ApiResult.Error.toErrorMessage(): String {
    return when (this) {
        is ApiResult.Error.HttpError -> {
            val validationMsg = validationErrors?.values?.flatten()?.firstOrNull()
            when {
                !validationMsg.isNullOrBlank() -> validationMsg
                !message.isNullOrBlank() -> message
                code in 500..599 -> "خطای سمت سرور رخ داده است. لطفاً مجدداً تلاش کنید."
                code == 401 -> "نشست کاربری شما به پایان رسیده است. لطفاً مجدداً وارد شوید."
                code == 403 -> "شما دسترسی لازم برای این عملیات را ندارید."
                code == 404 -> "اطلاعات درخواستی یافت نشد."
                else -> "خطایی با کد $code در ارتباط با سرور رخ داد."
            }
        }
        is ApiResult.Error.NetworkError -> "عدم دسترسی به اینترنت. لطفاً اتصال شبکه خود را بررسی نمایید."
        is ApiResult.Error.TimeoutError -> "مهلت اتصال به سرور به پایان رسید. لطفاً مجدداً تلاش کنید."
        is ApiResult.Error.SerializationError -> "خطا در پردازش اطلاعات دریافتی از سرور."
        is ApiResult.Error.UnknownError -> "خطای ناشناخته‌ای رخ داده است."
    }
}
