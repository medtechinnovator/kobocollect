package org.odk.collect.android.mainmenu

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.Executors

/**
 * Judge from GET /events/judges.
 * We use cleanName for the username input; display firstName + lastName with cleanName as secondary.
 */
data class Judge(
    val id: Int,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    val email: String? = null,
    @SerializedName("cleanName") val cleanName: String? = null,
    val role: String? = null,
    val phoneNumber: String? = null,
    val company: JudgeCompany? = null,
    @SerializedName("koboJudgeId") val koboJudgeId: String? = null,
    @SerializedName("userLists") val userLists: List<UserListEntry>? = null,
    val notes: List<Any>? = null
) {
    val displayName: String get() = "${firstName.orEmpty()} ${lastName.orEmpty()}".trim()
    val safeCleanName: String get() = cleanName.orEmpty()
    val safeEmail: String get() = email.orEmpty()
}

data class JudgeCompany(
    val id: Int,
    val name: String,
    @SerializedName("profileImageUrl") val profileImageUrl: String? = null
)

data class UserListEntry(
    @SerializedName("pitchEventId") val pitchEventId: Int,
    @SerializedName("eventAttendanceStatus") val eventAttendanceStatus: String,
    @SerializedName("koboJudgeId") val koboJudgeId: String? = null
)

/**
 * Fetches judges from MTI API. Call from background; callbacks are invoked on the main thread.
 */
class MtiJudgesApi(
    private val baseUrl: String,
    private val apiKey: String
) {
    private val client = OkHttpClient.Builder().build()
    private val gson = Gson()
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun fetchJudges(
        onSuccess: (List<Judge>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/events/judges"
        Timber.tag("MtiJudgesApi").d("fetchJudges triggered: GET %s (baseUrl=%s, apiKey present=%s)", url, baseUrl, apiKey.isNotBlank())
        executor.execute {
            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("X-API-Key", apiKey)
                    .addHeader("Accept", "application/json")
                    .get()
                    .build()
                Timber.tag("MtiJudgesApi").d("Request: %s %s", request.method, request.url)
                val response = client.newCall(request).execute()
                val code = response.code
                val body = response.body?.string() ?: ""
                val bodyPreview = if (body.length > 500) body.take(500) + "..." else body
                Timber.tag("MtiJudgesApi").d("Response: %d %s", code, response.message)
                Timber.tag("MtiJudgesApi").d("Response body: %s", bodyPreview)
                if (!response.isSuccessful) {
                    Timber.tag("MtiJudgesApi").e("HTTP error %d: %s", code, bodyPreview)
                    mainHandler.post { onError(Exception("HTTP $code: $bodyPreview")) }
                    return@execute
                }
                val judges = gson.fromJson(body, Array<Judge>::class.java).toList()
                Timber.tag("MtiJudgesApi").d("Parsed %d judges", judges.size)
                mainHandler.post { onSuccess(judges) }
            } catch (e: Exception) {
                Timber.tag("MtiJudgesApi").e(e, "fetchJudges failed")
                mainHandler.post { onError(e) }
            }
        }
    }
}
