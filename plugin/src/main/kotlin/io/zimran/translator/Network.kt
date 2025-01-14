package io.zimran.translator
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun setupKtor(openAiToken: String): HttpClient {
    return HttpClient(CIO) {
        install(ContentNegotiation) {

            json(Json { ignoreUnknownKeys = true })
        }

        install(DefaultRequest) {
            header("Authorization", "Bearer $openAiToken")
            header("Content-Type", "application/json")
        }

        install(Logging) {
            level = LogLevel.ALL // Log everything (headers, body, etc.)
            logger = Logger.DEFAULT // Default logger
        }

        install(HttpTimeout) {
            socketTimeoutMillis = 1000 * 60 * 10
            connectTimeoutMillis = 1000 * 60 * 10
            requestTimeoutMillis = 1000 * 60 * 10
        }
    }
}