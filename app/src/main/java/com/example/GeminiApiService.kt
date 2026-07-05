package com.example

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// --- Gemini Request Data Classes (Moshi compatible) ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Float? = null
)

// --- Gemini Response Data Classes (Moshi compatible) ---

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

// --- Parsed Scan Result structure ---

@JsonClass(generateAdapter = true)
data class ParsedPlateInfo(
    @Json(name = "plateText") val plateText: String?,
    @Json(name = "country") val country: String?,
    @Json(name = "state") val state: String?,
    @Json(name = "vehicleBrand") val vehicleBrand: String?,
    @Json(name = "vehicleColor") val vehicleColor: String?,
    @Json(name = "confidence") val confidence: Int?,
    @Json(name = "notes") val notes: String?
)

// --- Retrofit Service Definition ---

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun analyzeImage(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: GeminiApi = retrofit.create(GeminiApi::class.java)

    // Helper to convert Bitmap to JPEG Base64
    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        // Compress bitmap slightly to save bandwidth and fit well within prompt tokens
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun scanLicensePlate(bitmap: Bitmap, customPrompt: String? = null): ParsedPlateInfo? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("GeminiClient", "API Key is missing or placeholder!")
            return null
        }

        val base64Image = bitmap.toBase64()

        val prompt = customPrompt ?: """
            Analiza esta imagen para encontrar una placa/patente/matrícula de vehículo (auto, camión, moto, etc.).
            Es MUY importante identificar de forma exacta el texto/letras/números visibles en la placa de manera prioritaria.
            Devuelve un objeto JSON con los siguientes campos estrictamente (utiliza texto en español):
            {
               "plateText": "las letras y números de la placa sin espacios ni guiones si es posible, o en su defecto de forma legible",
               "country": "país del vehículo o placa si se identifica, ej. México, España, Chile, etc.",
               "state": "estado o provincia si se puede leer o identificar, ej. Nuevo León, Madrid, Santiago, etc.",
               "vehicleBrand": "marca del auto, ej. Toyota, Chevrolet, Audi (si se ve en la foto)",
               "vehicleColor": "color aproximado del auto si es visible",
               "confidence": un número entero del 0 al 100 estimando tu confianza de lectura de la matrícula,
               "notes": "una descripción muy breve de 1 o 2 líneas sobre detalles curiosos detectados, ej. Placa con desgaste, auto estacionado, etc."
            }
            Si no hay ninguna placa visible en la imagen, genera el JSON asignando "plateText": null y describe en el campo "notes" que no se detectó una matrícula legible.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                    )
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f // low temperature for precise factual extraction
            ),
            systemInstruction = Content(
                parts = listOf(
                    Part(text = "Eres un asistente experto en reconocimiento de patentes/matrículas vehiculares y análisis de automóviles. Tu único objetivo es extraer datos automotores verídicos y precisos en formato JSON estructurado.")
                )
            )
        )

        return try {
            val response = apiService.analyzeImage(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                Log.d("GeminiClient", "Received JSON: $jsonText")
                val adapter = moshi.adapter(ParsedPlateInfo::class.java)
                adapter.fromJson(jsonText)
            } else {
                Log.e("GeminiClient", "No content text in candidates response.")
                null
            }
        } catch (e: Exception) {
            Log.e("GeminiClient", "Error scanning license plate", e)
            null
        }
    }
}
