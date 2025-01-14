package io.zimran.translator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatGPTRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Float? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("top_p") val topP: Float? = null,
    @SerialName("frequency_penalty") val frequencyPenalty: Float? = null,
    @SerialName("presence_penalty") val presencePenalty: Float? = null
)

@Serializable
data class Message(
    val role: String, // Values: "system", "user", "assistant"
    val content: String
)