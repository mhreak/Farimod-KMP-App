package ir.farimod.app.screens.addnewcourse

import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import ir.farimod.app.data.model.UserData
import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiFilePart
import ir.farimod.app.network.ApiNetworkException
import ir.farimod.app.network.ApiResponse
import ir.farimod.app.network.FarimodHttpClient
import ir.farimod.app.network.bool
import ir.farimod.app.network.double
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.jsonOf
import ir.farimod.app.network.list
import ir.farimod.app.network.str
import ir.farimod.app.ui.ImageItem
import ir.farimod.app.ui.PickerOption
import ir.farimod.app.utils.PersianNumberUtils
import ir.farimod.app.utils.civilFromDays
import ir.farimod.app.utils.jsNow
import ir.farimod.app.utils.localDateTimeToEpoch
import ir.farimod.app.utils.toJalaali
import ir.farimod.app.utils.utcDateTimeToEpoch
import ir.farimod.app.utils.utcYmdOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal data class CourseWeekDay(val key: String, val label: String, val icon: String) {
    val capital: String = key.replaceFirstChar { it.uppercaseChar() }
}

internal val courseWeekDays = listOf(
    CourseWeekDay("saturday", "شنبه", "today"),
    CourseWeekDay("sunday", "یکشنبه", "today"),
    CourseWeekDay("monday", "دوشنبه", "today"),
    CourseWeekDay("tuesday", "سه‌شنبه", "today"),
    CourseWeekDay("wednesday", "چهارشنبه", "today"),
    CourseWeekDay("thursday", "پنج‌شنبه", "today"),
    CourseWeekDay("friday", "جمعه", "today")
)

internal data class CourseDaySession(
    val enabled: Boolean = false,
    val start: String = "",
    val finish: String = ""
)

internal fun defaultCourseSessions(): Map<String, CourseDaySession> =
    courseWeekDays.associate { it.key to CourseDaySession() }

internal data class CourseDraft(
    val courseName: String = "",
    val description: String? = null,
    val courseType: Int? = null,
    val cityId: Int? = null,
    val cityName: String = "",
    val provinceId: Int? = null,
    val provinceName: String = "",
    val courseAddress: String = "",
    val startDate: Long? = null,
    val finishDate: Long? = null,
    val registerStartDate: Long? = null,
    val registerFinishDate: Long? = null,
    val registerAmount: String? = null,
    val allowDiscountCode: Boolean? = null,
    val registerActive: Boolean? = null,
    val active: Boolean? = true,
    val sessions: Map<String, CourseDaySession> = defaultCourseSessions()
)

internal data class LoadedCourse(
    val draft: CourseDraft = CourseDraft(),
    val image: ImageItem? = null,
    val instructors: List<PickerOption> = emptyList(),
    val raw: JsonElement? = null,
    val error: String? = null
)

internal data class CourseSubmitOutcome(val ok: Boolean, val message: String)

private fun pad2(n: Int): String = n.toString().padStart(2, '0')
private fun pad3(n: Int): String = n.toString().padStart(3, '0')

/** `new Date().toISOString()` */
internal fun courseIsoNow(epoch: Long = jsNow()): String {
    val msPerDay = 86_400_000L
    val days = epoch.floorDiv(msPerDay)
    val msOfDay = epoch - days * msPerDay
    val (y, m, d) = civilFromDays(days)
    val h = (msOfDay / 3_600_000L).toInt()
    val mi = ((msOfDay % 3_600_000L) / 60_000L).toInt()
    val s = ((msOfDay % 60_000L) / 1000L).toInt()
    val ms = (msOfDay % 1000L).toInt()
    return "$y-${pad2(m)}-${pad2(d)}T${pad2(h)}:${pad2(mi)}:${pad2(s)}.${pad3(ms)}Z"
}

/**
 * `new Date(apiString)` for the values this screen stores.
 * Date-only `YYYY-MM-DD` is UTC midnight. A trailing `Z` or numeric offset is UTC.
 * A zone-less time is local, matching JavaScript.
 */
internal fun parseJsDateMillis(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null
    val s = raw.trim()
    if (s.contains('/') || s.length < 10) return null
    val y = s.substring(0, 4).toIntOrNull() ?: return null
    if (s[4] != '-') return null
    val mo = s.substring(5, 7).toIntOrNull() ?: return null
    if (s[7] != '-') return null
    val d = s.substring(8, 10).toIntOrNull() ?: return null
    if (s.length == 10) return utcDateTimeToEpoch(y, mo, d, 0, 0, 0, 0)
    val tIdx = when {
        s.length > 10 && (s[10] == 'T' || s[10] == ' ') -> 10
        else -> return utcDateTimeToEpoch(y, mo, d, 0, 0, 0, 0)
    }
    if (s.length < tIdx + 6) return utcDateTimeToEpoch(y, mo, d, 0, 0, 0, 0)
    val hh = s.substring(tIdx + 1, tIdx + 3).toIntOrNull() ?: 0
    val mm = s.substring(tIdx + 4, tIdx + 6).toIntOrNull() ?: 0
    var ss = 0
    var ms = 0
    var i = tIdx + 6
    if (i < s.length && s[i] == ':') {
        val end = (i + 3).coerceAtMost(s.length)
        ss = s.substring(i + 1, end).toIntOrNull() ?: 0
        i += 3
    }
    if (i < s.length && s[i] == '.') {
        val frac = s.substring(i + 1).takeWhile { it.isDigit() }
        ms = frac.padEnd(3, '0').take(3).toIntOrNull() ?: 0
        i += 1 + frac.length
    }
    if (i < s.length && (s[i] == 'Z' || s[i] == 'z')) {
        return utcDateTimeToEpoch(y, mo, d, hh, mm, ss, ms)
    }
    if (i < s.length && (s[i] == '+' || s[i] == '-')) {
        val sign = if (s[i] == '+') 1 else -1
        val rest = s.substring(i + 1)
        val oh = rest.take(2).toIntOrNull() ?: 0
        val om = when {
            rest.length >= 5 && rest[2] == ':' -> rest.substring(3, 5).toIntOrNull() ?: 0
            rest.length >= 4 -> rest.substring(2, 4).toIntOrNull() ?: 0
            else -> 0
        }
        val offsetMs = sign * (oh * 3_600_000L + om * 60_000L)
        return utcDateTimeToEpoch(y, mo, d, hh, mm, ss, ms) - offsetMs
    }
    return localDateTimeToEpoch(y, mo, d, hh, mm, ss, ms)
}

/** RN `formatPersianDate`: UTC calendar fields, then jalaali-js, `yyyy-MM-dd`. */
internal fun formatCourseSubmitDate(epoch: Long?): String {
    if (epoch == null) return ""
    val (y, m, d) = utcYmdOf(epoch)
    val j = toJalaali(y, m, d)
    return "${j.jy}-${pad2(j.jm)}-${pad2(j.jd)}"
}

internal fun courseAmountDisplay(raw: String?): String {
    if (raw.isNullOrEmpty()) return ""
    val digits = raw.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    return digits.reversed().chunked(3).joinToString(",").reversed()
}

/** RN `getMemberDisplayName`. Picker items only have `value`/`label`, so secondary instructors become "مربی". */
internal fun coursePersonName(
    memberName: String? = null,
    name: String? = null,
    fullName: String? = null,
    displayName: String? = null,
    firstName: String? = null,
    lastName: String? = null,
    userName: String? = null,
    memberId: Any? = null,
    id: Any? = null
): String {
    memberName?.takeIf { it.isNotEmpty() }?.let { return it }
    name?.takeIf { it.isNotEmpty() }?.let { return it }
    fullName?.takeIf { it.isNotEmpty() }?.let { return it }
    displayName?.takeIf { it.isNotEmpty() }?.let { return it }
    val combined = "${firstName.orEmpty()} ${lastName.orEmpty()}".trim()
    if (combined.isNotEmpty()) return combined
    userName?.takeIf { it.isNotEmpty() }?.let { return it }
    val tail = when {
        memberId != null && memberId.toString().isNotEmpty() -> memberId.toString()
        id != null && id.toString().isNotEmpty() -> id.toString()
        else -> ""
    }
    return "مربی $tail".trim()
}

private fun JsonElement?.optionalBool(key: String): Boolean? {
    val v = this[key] ?: return null
    return v.bool
}

private fun idOrNull(el: JsonElement?): Int? = el.int?.takeIf { it != 0 }

private fun amountOrNull(el: JsonElement?): String? {
    val n = el.double ?: return null
    if (n == 0.0) return null
    val asLong = n.toLong()
    return if (n == asLong.toDouble()) asLong.toString() else n.toString()
}

internal fun courseDraftFrom(data: JsonElement): CourseDraft {
    val sessions = courseWeekDays.associate { day ->
        day.key to CourseDaySession(
            enabled = data["Has${day.capital}Session"].bool,
            start = data.str("${day.capital}_StartTime").orEmpty(),
            finish = data.str("${day.capital}_FinishTime").orEmpty()
        )
    }
    return CourseDraft(
        courseName = data.str("CourseName").orEmpty(),
        description = data.str("Description")?.takeIf { it.isNotEmpty() },
        courseType = idOrNull(data["CourseType"]),
        cityId = idOrNull(data["CityId"]),
        cityName = data.str("CityName").orEmpty(),
        provinceId = idOrNull(data["ProvinceId"]),
        provinceName = data.str("ProvinceName").orEmpty(),
        courseAddress = data.str("CourseAddress").orEmpty(),
        startDate = parseJsDateMillis(data.str("StartDate")),
        finishDate = parseJsDateMillis(data.str("FinishDate")),
        registerStartDate = parseJsDateMillis(data.str("RegisterStartDate")),
        registerFinishDate = parseJsDateMillis(data.str("RegisterFinishDate")),
        registerAmount = amountOrNull(data["RegisterAmount"]),
        allowDiscountCode = data.optionalBool("AllowDiscountCode"),
        registerActive = data.optionalBool("RegisterActive"),
        active = data.optionalBool("Active") ?: true,
        sessions = sessions
    )
}

internal fun courseInstructorsFrom(data: JsonElement, userId: Int?): List<PickerOption> {
    val primary = data["Course_Member_List"]
    val fallback = data["Course_Member_ViewModel_List"]
    val list = when {
        primary is JsonArray -> primary
        fallback is JsonArray -> fallback
        else -> return emptyList()
    }
    return list.mapNotNull { member ->
        val id = member["MemberId"].int ?: return@mapNotNull null
        if (id == 0 || (userId != null && id == userId)) return@mapNotNull null
        val label = member.str("MemberName")
            ?: member.str("Name")
            ?: member.str("FullName")
            ?: "مربی $id"
        PickerOption(label = label, value = id)
    }
}

internal fun courseImageFrom(data: JsonElement, courseId: Int): ImageItem? {
    val url = data.str("FeaturedImageURL")
    if (url.isNullOrEmpty()) return null
    val name = data.str("FeaturedImageFileName")?.takeIf { it.isNotEmpty() } ?: "course_image.jpg"
    val type = data.str("FeaturedImageType")?.takeIf { it.isNotEmpty() } ?: "image/jpeg"
    return ImageItem(id = "course-image-$courseId", uri = url, name = name, type = type)
}

internal fun validateCourseDraft(d: CourseDraft): String? {
    val errors = LinkedHashMap<String, String>()
    if (d.courseName.trim().isEmpty()) errors["courseName"] = "نام دوره الزامی است"
    if (d.courseType == null) errors["courseType"] = "نوع دوره الزامی است"
    if (d.courseType != 2) {
        if (d.cityId == null) errors["cityId"] = "شهر الزامی است"
        if (d.provinceId == null) errors["provinceId"] = "استان الزامی است"
    }
    if (d.courseAddress.trim().isEmpty()) errors["courseAddress"] = "آدرس دوره الزامی است"
    if (d.startDate == null) errors["startDate"] = "تاریخ شروع الزامی است"
    if (d.finishDate == null) errors["finishDate"] = "تاریخ پایان الزامی است"
    if (d.registerStartDate == null) errors["registerStartDate"] = "تاریخ شروع ثبت نام الزامی است"
    if (d.registerFinishDate == null) errors["registerFinishDate"] = "تاریخ پایان ثبت نام الزامی است"
    if (d.registerAmount == null) {
        errors["registerAmount"] = "مبلغ ثبت نام الزامی است"
    } else {
        val n = d.registerAmount.toDoubleOrNull()
        if (n != null && n < 0.0) errors["registerAmount"] = "مبلغ نمی تواند منفی باشد"
    }
    if (d.active == null) errors["active"] = "وضعیت دوره الزامی است"
    courseWeekDays.forEach { day ->
        val session = d.sessions[day.key] ?: return@forEach
        if (session.enabled) {
            if (session.start.isEmpty()) errors["${day.key}StartTime"] = "ساعت شروع ${day.label} الزامی است"
            if (session.finish.isEmpty()) errors["${day.key}FinishTime"] = "ساعت پایان ${day.label} الزامی است"
        }
    }
    return errors.values.firstOrNull()
}

internal suspend fun fetchCourseProvinces(): Pair<List<PickerOption>, String?> {
    return try {
        var currentPage = 1
        val pageSize = 20
        val allProvinces = mutableListOf<JsonElement>()
        while (true) {
            val response = Api.get(
                "Province/GetAllActive",
                query = mapOf(
                    "currentPage" to currentPage,
                    "page" to currentPage,
                    "pageSize" to pageSize
                )
            )
            if (!response.ok) {
                if (allProvinces.isEmpty()) return emptyList<PickerOption>() to "خطا در دریافت لیست استان‌ها"
                break
            }
            val result = response.json ?: break
            val items = when {
                result is JsonArray -> result
                result["Items"] is JsonArray -> result["Items"]
                result["Data"] is JsonArray -> result["Data"]
                else -> null
            } as? JsonArray ?: break

            val list = items.list
            allProvinces.addAll(list)
            val total = result["Total"].int ?: result["TotalCount"].int ?: result["TotalItems"].int ?: allProvinces.size
            if (list.size < pageSize || allProvinces.size >= total) {
                break
            }
            currentPage++
        }
        val items = allProvinces.map { province ->
            PickerOption(
                label = province.str("ProvinceName").orEmpty(),
                value = province["ProvinceId"].int ?: 0
            )
        }
        items to null
    } catch (e: CancellationException) {
        throw e
    } catch (_: ApiNetworkException) {
        emptyList<PickerOption>() to "خطا در دریافت لیست استان‌ها"
    }
}

internal suspend fun fetchCourseCities(provinceId: Int): Pair<List<PickerOption>, String?> {
    return try {
        var currentPage = 1
        val pageSize = 20
        val allCities = mutableListOf<JsonElement>()
        while (true) {
            val response = Api.get(
                "City/GetAllActiveByProvinceId",
                query = mapOf(
                    "provinceId" to provinceId,
                    "currentPage" to currentPage,
                    "page" to currentPage,
                    "pageSize" to pageSize
                )
            )
            if (!response.ok) {
                if (allCities.isEmpty()) return emptyList<PickerOption>() to "خطا در دریافت لیست شهرها"
                break
            }
            val result = response.json ?: break
            val items = when {
                result is JsonArray -> result
                result["Items"] is JsonArray -> result["Items"]
                result["Data"] is JsonArray -> result["Data"]
                else -> null
            } as? JsonArray ?: break

            val list = items.list
            allCities.addAll(list)
            val total = result["Total"].int ?: result["TotalCount"].int ?: result["TotalItems"].int ?: allCities.size
            if (list.size < pageSize || allCities.size >= total) {
                break
            }
            currentPage++
        }
        val items = allCities.map { city ->
            PickerOption(label = city.str("CityName").orEmpty(), value = city["CityId"].int ?: 0)
        }
        items to null
    } catch (e: CancellationException) {
        throw e
    } catch (_: ApiNetworkException) {
        emptyList<PickerOption>() to "خطا در دریافت لیست شهرها"
    }
}

internal suspend fun fetchCourseMembers(query: String, userId: Int?): Pair<List<PickerOption>, String?> {
    return try {
        val response = Api.get(
            "Member/GetAll",
            query = mapOf("currentPage" to 1, "pageSize" to 200, "filterName" to query)
        )
        if (!response.ok) return emptyList<PickerOption>() to "خطا در دریافت لیست مربیان"
        val data = response.json
        val arr: List<JsonElement>? = when {
            data["Data"] is JsonArray -> data["Data"].list
            data["Items"] is JsonArray -> data["Items"].list
            data is JsonArray -> data.list
            else -> null
        }
        if (arr == null) return emptyList<PickerOption>() to "خطا در دریافت لیست مربیان"
        val items = arr.mapNotNull { member ->
            val id = member["MemberId"].int ?: return@mapNotNull null
            if (id == 0 || (userId != null && id == userId)) return@mapNotNull null
            val combined = "${member.str("FirstName").orEmpty()} ${member.str("LastName").orEmpty()}".trim()
            val label = member.str("MemberName")
                ?: member.str("Name")
                ?: combined.takeIf { it.isNotEmpty() }
                ?: "مربی $id"
            PickerOption(label = label, value = id)
        }
        items to null
    } catch (e: CancellationException) {
        throw e
    } catch (_: ApiNetworkException) {
        emptyList<PickerOption>() to "خطا در دریافت لیست مربیان"
    }
}

internal suspend fun fetchCourseForEdit(courseId: Int, userId: Int?): LoadedCourse {
    return try {
        val response = Api.get("Course/Get", query = mapOf("courseId" to courseId))
        if (!response.ok) return LoadedCourse(error = "خطا در بارگذاری اطلاعات دوره")
        val course = response.json["Course"] ?: return LoadedCourse(error = "خطا در بارگذاری اطلاعات دوره")
        LoadedCourse(
            draft = courseDraftFrom(course),
            image = courseImageFrom(course, courseId),
            instructors = courseInstructorsFrom(course, userId),
            raw = course
        )
    } catch (e: CancellationException) {
        throw e
    } catch (_: ApiNetworkException) {
        LoadedCourse(error = "خطا در بارگذاری اطلاعات دوره")
    }
}

private fun optionId(value: Any?): Int? = (value as? Number)?.toInt()?.takeIf { it != 0 }

private fun appendField(fields: MutableList<Pair<String, String>>, key: String, value: String) {
    fields += key to value
}

private suspend fun courseUploadOnce(
    path: String,
    method: String,
    fields: List<Pair<String, String>>,
    file: ApiFilePart?
): ApiResponse {
    try {
        val content = MultiPartFormDataContent(
            formData {
                for ((k, v) in fields) append(k, v)
                if (file != null) {
                    append(
                        file.fieldName,
                        file.bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, file.mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=\"${file.fileName}\"")
                        }
                    )
                }
            }
        )
        val response = if (method.equals("PUT", true)) {
            FarimodHttpClient.client.put {
                url(path)
                header(HttpHeaders.Accept, "application/json")
                timeout {
                    requestTimeoutMillis = 120_000
                    socketTimeoutMillis = 120_000
                }
                setBody(content)
            }
        } else {
            FarimodHttpClient.client.post {
                url(path)
                header(HttpHeaders.Accept, "application/json")
                timeout {
                    requestTimeoutMillis = 120_000
                    socketTimeoutMillis = 120_000
                }
                setBody(content)
            }
        }
        val text = try {
            response.bodyAsText()
        } catch (_: Exception) {
            ""
        }
        return ApiResponse(response.status.value, text)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val m = (e.message ?: "").lowercase()
        if (m.contains("timeout") || m.contains("timed out")) throw ApiNetworkException("Request timeout", e)
        throw ApiNetworkException("Network request failed", e)
    }
}

/** Same retry rule as this screen's `axios-retry` (2 retries, delay = attempt * 2000, network or HTTP >= 500). */
private suspend fun courseUpload(
    path: String,
    method: String,
    fields: List<Pair<String, String>>,
    file: ApiFilePart?
): ApiResponse {
    var attempt = 0
    while (true) {
        try {
            val response = courseUploadOnce(path, method, fields, file)
            if (response.status >= 500 && attempt < 2) {
                attempt += 1
                delay(attempt * 2000L)
                continue
            }
            return response
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiNetworkException) {
            if (attempt < 2) {
                attempt += 1
                delay(attempt * 2000L)
                continue
            }
            throw e
        }
    }
}

private fun courseHttpError(response: ApiResponse): String {
    val el = response.json
    if (el is JsonObject) {
        el.str("message")?.let { return it }
        el.str("Message")?.let { return it }
        el.str("error")?.let { return it }
        el.str("Error")?.let { return it }
    }
    if (el is JsonPrimitive && el.isString && el.content.isNotEmpty()) return el.content
    if (el == null && response.text.isNotBlank()) return response.text
    return "خطا ${response.status}"
}

internal suspend fun submitCourseForm(
    draft: CourseDraft,
    courseId: Int?,
    courseJson: JsonElement?,
    user: UserData?,
    image: ImageItem?,
    instructors: List<PickerOption>
): CourseSubmitOutcome {
    val editing = courseId != null
    val fail = if (editing) "خطا در ویرایش دوره" else "خطا در ثبت دوره"
    val okMessage = "دوره با موفقیت ${if (editing) "ویرایش" else "ثبت"} شد"
    return try {
        val fields = mutableListOf<Pair<String, String>>()
        val virtual = draft.courseType == 2
        appendField(fields, "CourseId", courseId?.toString() ?: "0")
        appendField(fields, "CourseName", draft.courseName)
        appendField(fields, "MemberId", user?.MemberId?.toString() ?: "0")
        // RN reads user.FullName || user.Name. UserData stores neither, so this is empty.
        appendField(fields, "MemberName", "")
        appendField(fields, "CourseType", draft.courseType?.toString() ?: "1")
        val typeStr = when (draft.courseType) {
            1 -> "حضوری"
            2 -> "مجازی"
            else -> "حضوری و مجازی"
        }
        appendField(fields, "CourseTypeStr", typeStr)
        if (virtual) {
            appendField(fields, "CityId", "")
            appendField(fields, "CityName", "")
            appendField(fields, "ProvinceId", "")
            appendField(fields, "ProvinceName", "")
        } else {
            appendField(fields, "CityId", draft.cityId?.toString() ?: "0")
            appendField(fields, "CityName", draft.cityName)
            appendField(fields, "ProvinceId", draft.provinceId?.toString() ?: "0")
            appendField(fields, "ProvinceName", draft.provinceName)
        }
        appendField(fields, "CourseAddress", draft.courseAddress)
        // RN FormData stringifies a null Description as "null".
        appendField(fields, "Description", draft.description ?: "null")
        appendField(fields, "Rating", "0")
        appendField(fields, "Active", if (draft.active == true) "true" else "false")
        courseWeekDays.forEach { day ->
            val session = draft.sessions[day.key] ?: CourseDaySession()
            appendField(fields, "Has${day.capital}Session", if (session.enabled) "true" else "false")
            appendField(fields, "${day.capital}_StartTime", PersianNumberUtils.toEnglishDigits(session.start))
            appendField(fields, "${day.capital}_FinishTime", PersianNumberUtils.toEnglishDigits(session.finish))
        }
        val start = formatCourseSubmitDate(draft.startDate)
        val finish = formatCourseSubmitDate(draft.finishDate)
        val regStart = formatCourseSubmitDate(draft.registerStartDate)
        val regFinish = formatCourseSubmitDate(draft.registerFinishDate)
        appendField(fields, "StartDate", start)
        appendField(fields, "ShamsiStartDate", start)
        appendField(fields, "FinishDate", finish)
        appendField(fields, "ShamsiFinishDate", finish)
        appendField(fields, "RegisterStartDate", regStart)
        appendField(fields, "ShamsiRegisterStartDate", regStart)
        appendField(fields, "RegisterFinishDate", regFinish)
        appendField(fields, "ShamsiRegisterFinishDate", regFinish)
        val amountEnglish = PersianNumberUtils.toEnglishDigits(draft.registerAmount)
        appendField(fields, "RegisterAmount", amountEnglish.ifEmpty { "0" })
        appendField(fields, "AllowDiscountCode", if (draft.allowDiscountCode == true) "true" else "false")
        val registerOn = draft.registerActive != false
        appendField(fields, "RegisterActive", if (registerOn) "true" else "false")
        appendField(fields, "RegisterActiveStr", if (registerOn) "فعال" else "غیرفعال")
        appendField(fields, "InsertDate", courseIsoNow())
        appendField(fields, "LikeCount", "0")

        val localImage = image?.takeIf { it.uri.isNotEmpty() && !it.uri.startsWith("http") }
        if (localImage != null) {
            val imageName = localImage.name?.takeIf { it.isNotEmpty() } ?: "course_poster_${jsNow()}.jpg"
            appendField(fields, "FeaturedImageFileName", imageName)
            appendField(fields, "FeaturedImageURL", "")
        } else if (editing && courseJson != null) {
            appendField(fields, "FeaturedImageURL", courseJson.str("FeaturedImageURL").orEmpty())
            appendField(fields, "FeaturedImageFileName", courseJson.str("FeaturedImageFileName").orEmpty())
        } else {
            appendField(fields, "FeaturedImageURL", "")
            appendField(fields, "FeaturedImageFileName", "")
        }

        val memberIds = buildList {
            val self = user?.MemberId
            if (self != null && self != 0) add(self)
            instructors.forEach { option ->
                val id = optionId(option.value) ?: return@forEach
                add(id)
            }
        }
        if (memberIds.isEmpty()) {
            appendField(fields, "Course_MemberId_List", "0")
        } else {
            memberIds.forEach { appendField(fields, "Course_MemberId_List", it.toString()) }
        }

        val payloads = buildList {
            add(
                jsonOf(
                    "CourseId" to (courseId ?: 0),
                    "MemberId" to (user?.MemberId ?: 0),
                    "CourseName" to draft.courseName,
                    "MemberName" to coursePersonName(
                        firstName = user?.FirstName,
                        lastName = user?.LastName,
                        memberId = user?.MemberId
                    ),
                    "Title" to "مربی اصلی",
                    "InsertDate" to courseIsoNow()
                ).toString()
            )
            instructors.forEach { option ->
                val id = optionId(option.value) ?: return@forEach
                add(
                    jsonOf(
                        "CourseId" to (courseId ?: 0),
                        "MemberId" to id,
                        "CourseName" to draft.courseName,
                        "MemberName" to coursePersonName(),
                        "Title" to "مربی فرعی",
                        "InsertDate" to courseIsoNow()
                    ).toString()
                )
            }
        }
        if (payloads.isEmpty()) {
            appendField(fields, "Course_Member_List", "[]")
        } else {
            payloads.forEach { appendField(fields, "Course_Member_List", it) }
        }

        val file = if (localImage != null) {
            val bytes = localImage.bytesOrNull() ?: return CourseSubmitOutcome(false, fail)
            val imageName = localImage.name?.takeIf { it.isNotEmpty() } ?: "course_poster_${jsNow()}.jpg"
            var mime = localImage.type ?: "image/jpeg"
            if (mime == "image") mime = "image/jpeg"
            ApiFilePart("featuredImageFile", imageName, mime, bytes)
        } else {
            appendField(fields, "DeleteFeaturedImage", if (image != null) "false" else "true")
            null
        }

        val path = if (editing) "Course/Edit" else "Course/Add"
        val method = if (editing) "PUT" else "POST"
        val response = courseUpload(path, method, fields, file)
        if (response.ok) CourseSubmitOutcome(true, okMessage)
        else CourseSubmitOutcome(false, courseHttpError(response))
    } catch (e: CancellationException) {
        throw e
    } catch (_: ApiNetworkException) {
        CourseSubmitOutcome(false, "خطا در اتصال به سرور,اتصال خود را به اینترنت بررسی کنید")
    } catch (e: Exception) {
        CourseSubmitOutcome(false, e.message?.takeIf { it.isNotEmpty() } ?: fail)
    }
}
