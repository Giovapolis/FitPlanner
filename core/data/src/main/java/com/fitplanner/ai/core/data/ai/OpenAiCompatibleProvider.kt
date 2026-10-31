package com.fitplanner.ai.core.data.ai

import com.fitplanner.ai.core.data.AppMoshi
import com.fitplanner.ai.core.data.readJson
import com.fitplanner.ai.core.domain.AiSuggestionProvider
import com.fitplanner.ai.core.model.AiSettings
import com.fitplanner.ai.core.model.AiSuggestionRequest
import com.fitplanner.ai.core.model.AiSuggestionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAiCompatibleProvider @Inject constructor() : AiSuggestionProvider {
    override suspend fun suggest(
        request: AiSuggestionRequest,
        settings: AiSettings,
        apiKey: String
    ): Result<AiSuggestionResponse> = withContext(Dispatchers.IO) {
        runCatching {
            require(settings.baseUrl.isNotBlank()) { "Configura la URL base del proveedor." }
            val lower = settings.baseUrl.lowercase()
            val isLocal = lower.startsWith("http://localhost") ||
                lower.startsWith("http://127.0.0.1") ||
                lower.startsWith("http://10.") ||
                lower.startsWith("http://192.168.") ||
                lower.startsWith("http://172.16.") ||
                lower.startsWith("http://172.17.") ||
                lower.startsWith("http://172.18.") ||
                lower.startsWith("http://172.19.") ||
                lower.startsWith("http://172.2") ||
                lower.startsWith("http://172.30.") ||
                lower.startsWith("http://172.31.")
            require(lower.startsWith("https://") || isLocal) {
                "Usa una URL HTTPS. Solo permitimos HTTP para servidores locales."
            }
            require(settings.model.isNotBlank()) { "Configura un modelo." }
            require(apiKey.isNotBlank()) { "Falta la API key." }
            val url = endpoint(settings.baseUrl)
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Connection", "close")
            }
            val body = JSONObject()
                .put("model", settings.model)
                .put("temperature", 0.2)
                .put("messages", buildMessages(request))
                .toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val responseText = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()
            if (code !in 200..299) {
                throw IOException("El proveedor de IA respondió con código $code.")
            }
            val root = JSONObject(responseText)
            val content = root
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val parsed = AppMoshi.readJson<AiSuggestionResponse>(content)
                ?: throw IllegalStateException("La IA devolvió un formato que no podemos leer.")
            parsed.copy(
                notes = parsed.notes.take(4),
                exerciseSlugs = parsed.exerciseSlugs.distinct().take(3)
            )
        }
    }

    private fun endpoint(baseUrl: String): String {
        val base = baseUrl.trim().trimEnd('/')
        return if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
    }

    private fun buildMessages(request: AiSuggestionRequest): JSONArray {
        val system = """
            Eres un asistente de programación de entrenamiento prudente.
            Devuelve SOLO un objeto JSON válido con esta forma:
            {"notes":["nota breve"],"exerciseSlugs":["slug"]}
            Usa únicamente slugs del catálogo que recibes. No inventes ejercicios.
            Propón como máximo 3 alternativas para el ejercicio actual.
            No des diagnósticos ni consejo médico. No prometas que un ejercicio es seguro para una lesión.
        """.trimIndent()
        val catalog = request.catalog.joinToString("\n") { item ->
            "- ${item.id} | ${item.name} | ${item.muscle} | ${item.equipment.joinToString("/")}"
        }
        val user = """
            Objetivo: ${request.goal.label}
            Nivel: ${request.level.label}
            Días por semana: ${request.daysPerWeek}
            Minutos por sesión: ${request.minutesPerSession}
            División/sesión: ${request.split}
            Equipamiento disponible: ${request.equipment.joinToString(", ")}
            Restricciones declaradas: ${request.restrictionTags.ifEmpty { listOf("ninguna") }.joinToString(", ")}
            Ejercicio actual: ${request.currentExerciseId}
            Ejercicios de la sesión: ${request.exerciseSlugs.joinToString(", ")}
            Catálogo disponible:
            $catalog
        """.trimIndent()
        return JSONArray()
            .put(JSONObject().put("role", "system").put("content", system))
            .put(JSONObject().put("role", "user").put("content", user))
    }
}
