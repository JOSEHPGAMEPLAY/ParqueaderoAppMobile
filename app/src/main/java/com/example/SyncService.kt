package com.example

import android.os.Build
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// --- Authentication / Registration Models ---

@JsonClass(generateAdapter = true)
data class MobileRegistrationRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val masterKey: String, // Special admin key
    @Json(name = "deviceType") val deviceType: String = "android_mobile",
    @Json(name = "deviceBrand") val deviceBrand: String,
    @Json(name = "deviceModel") val deviceModel: String,
    @Json(name = "deviceOsVersion") val deviceOsVersion: String
)

@JsonClass(generateAdapter = true)
data class MobileRegistrationResponse(
    @Json(name = "token") val token: String,
    @Json(name = "username") val username: String,
    @Json(name = "role") val role: String,
    @Json(name = "isActive") val isActive: Boolean,
    @Json(name = "message") val message: String? = null
)

// --- Synced Transaction Data (Matching User's DB Schemas) ---

@JsonClass(generateAdapter = true)
data class NoteComment(
    @Json(name = "message") val message: String,
    @Json(name = "authorName") val authorName: String,
    @Json(name = "timestamp") val timestamp: Long
)

object NotesParser {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, NoteComment::class.java)
    private val adapter = moshi.adapter<List<NoteComment>>(listType)

    fun parse(notesStr: String?, defaultAuthor: String = "Operador", defaultTime: Long = System.currentTimeMillis()): List<NoteComment> {
        if (notesStr.isNullOrBlank()) return emptyList()
        val trimmed = notesStr.trim()
        if (trimmed.startsWith("[")) {
            try {
                return adapter.fromJson(trimmed) ?: emptyList()
            } catch (e: Exception) {
                // Return plain text parsing fallback below
            }
        }
        return listOf(NoteComment(message = trimmed, authorName = defaultAuthor, timestamp = defaultTime))
    }

    fun toJson(list: List<NoteComment>): String {
        return adapter.toJson(list)
    }
}

@JsonClass(generateAdapter = true)
data class DailyParkingRecordItem(
    @Json(name = "date") val date: Long, // Start of day timestamp
    @Json(name = "totalEarned") val totalEarned: Int
)

@JsonClass(generateAdapter = true)
data class ParkingRecordItem(
    @Json(name = "localId") val localId: String,
    @Json(name = "plateNumber") val plateNumber: String,
    @Json(name = "entryTime") val entryTime: Long,
    @Json(name = "exitTime") val exitTime: Long?,
    @Json(name = "price") val price: Double,
    @Json(name = "isFree") val isFree: Boolean,
    @Json(name = "date") val date: Long // Start of day timestamp for linking to DailyParkingRecord
)

@JsonClass(generateAdapter = true)
data class CommentParkingRecordItem(
    @Json(name = "localParkingRecordId") val localParkingRecordId: String, // Maps to localId of the ParkingRecord
    @Json(name = "message") val message: String,
    @Json(name = "timestamp") val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class WebSyncPayload(
    @Json(name = "deviceModel") val deviceModel: String,
    @Json(name = "dailyRecords") val dailyRecords: List<DailyParkingRecordItem>,
    @Json(name = "parkingRecords") val parkingRecords: List<ParkingRecordItem>,
    @Json(name = "comments") val comments: List<CommentParkingRecordItem>
)

@JsonClass(generateAdapter = true)
data class WebSyncResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String,
    @Json(name = "recordsProcessed") val recordsProcessed: Int,
    @Json(name = "serverTimestamp") val serverTimestamp: Long
)

// --- Retrofit Endpoints ---

interface SyncApi {
    @POST("api/auth/register-mobile")
    suspend fun registerMobile(
        @Body request: MobileRegistrationRequest
    ): MobileRegistrationResponse

    @POST("api/parking/sync")
    suspend fun syncData(
        @Header("Authorization") bearerToken: String,
        @Body payload: WebSyncPayload
    ): WebSyncResponse
}

// --- GitHub Release Models for Update Checker ---

@JsonClass(generateAdapter = true)
data class GitHubReleaseAsset(
    @Json(name = "name") val name: String,
    @Json(name = "browser_download_url") val browserDownloadUrl: String
)

@JsonClass(generateAdapter = true)
data class GitHubReleaseResponse(
    @Json(name = "tag_name") val tagName: String,
    @Json(name = "name") val name: String,
    @Json(name = "assets") val assets: List<GitHubReleaseAsset> = emptyList()
)

interface GithubApi {
    @retrofit2.http.GET
    suspend fun getLatestRelease(@retrofit2.http.Url url: String): GitHubReleaseResponse
}

// --- API Client and Facilitator ---

object SyncClient {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Dynamic Base URL that can be altered by user in settings.
    // Default is a standard node express development address.
    var baseApiUrl: String = "http://10.0.2.2:3000/" // 10.0.2.2 points to host machine from android emulator
        set(value) {
            val formatted = if (value.endsWith("/")) value else "$value/"
            field = formatted
            rebuildRetrofit(formatted)
        }

    private var retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseApiUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private var syncApi: SyncApi = retrofit.create(SyncApi::class.java)

    private var githubRetrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/") // fallback base url, actual is overwritten by dynamic @Url request
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private var githubApi: GithubApi = githubRetrofit.create(GithubApi::class.java)

    private fun rebuildRetrofit(newUrl: String) {
        try {
            retrofit = Retrofit.Builder()
                .baseUrl(newUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
            syncApi = retrofit.create(SyncApi::class.java)
        } catch (e: Exception) {
            android.util.Log.e("SyncClient", "Error rebuilding Retrofit: ${e.message}")
        }
    }

    /**
     * Attempts a real API mobile device registration.
     */
    suspend fun executeRealRegistration(username: String, masterKey: String): MobileRegistrationResponse {
        val request = MobileRegistrationRequest(
            username = username,
            masterKey = masterKey,
            deviceBrand = Build.MANUFACTURER,
            deviceModel = Build.MODEL,
            deviceOsVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        )
        return syncApi.registerMobile(request)
    }

    /**
     * Attempts a real API database Sync.
     */
    suspend fun executeRealSync(token: String, payload: WebSyncPayload): WebSyncResponse {
        val authHeader = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
        return syncApi.syncData(authHeader, payload)
    }

    /**
     * Pulls latest release info from GitHub API
     */
    suspend fun fetchLatestRelease(url: String): GitHubReleaseResponse {
        return githubApi.getLatestRelease(url)
    }
}
