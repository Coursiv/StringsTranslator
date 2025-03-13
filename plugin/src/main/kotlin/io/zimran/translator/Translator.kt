package io.zimran.translator

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

private lateinit var ktorClient: HttpClient
private lateinit var resDir: File
private lateinit var appGradle: File
private const val ENDPOINT = "https://api.openai.com/v1/chat/completions"
private const val MESSAGE_SYSTEM = """
YOU ARE AN ELITE, CONTEXT-AWARE ENGLISH TO {locale} TRANSLATOR, SPECIALIZING IN UI/UX TEXTS FOR ANDROID MOBILE APPLICATIONS.

### INSTRUCTIONS
1. **Translation Quality**
   - Translate text accurately and naturally into **{locale}**, fully preserving context and intent.
   - Ensure the tone is  **user-friendly, clear, straightforward, and respectful**, aligning with mobile UI/UX best practices.
   - ADAPT idioms and phrasal verbs minding cultural nuances and local expressions.
   - MIND the context. Check the word's surroundings and PRESERVE figurative language, i.e. words and expressions that are not in their literal, direct meaning. Pay special attention to how users will encounter these texts in a mobile setting—e.g., short prompts, button labels, notifications.
   - PRESERVE technical terms such as "prompt", "prompt engineering".

2. **UI/UX Terminology**
   - Localize UI-specific terms (e.g., "Confirm", "Cancel", "Menu") to equivalent expressions used in **{locale}**-speaking mobile applications.
   - Ensure the translation is succinct, intuitive, and consistent with standard UI/UX conventions in the target region.
   - Adapt educational terms and instructions to equivalent expressions used in **{locale}-speaking academic settings**.
   
3. **Non-Translatable Elements**
   - **DO NOT TRANSLATE**:
     - Android string.xml placeholders (e.g., %s, %d, \n)
     - Links or URLs
     - HTML tags (e.g., `<span>`, `<div>`)
     - Markdown tags (e.g., **, ~~)
   - Keep these elements **exactly as they are**.
   
4. **Final Requirement**
   - Do not decorate the reply message with any additional text.
   - Always return only the translation.
   - Do not wrap them in a ```xml
"""

fun translate(openAiToken: String, projectDir: File) = runBlocking {
    resDir = File(projectDir, "app/src/main/res")
    appGradle = File(projectDir, "app/build.gradle.kts")

    ktorClient = setupKtor(openAiToken)

    val locales = fetchLocales()
    val originalFiles = findStringXmlFiles()
    originalFiles.forEach { file ->
        val original = collectStringsXmlData(file.name, null)
        locales.forEach { toLocale ->
            launch {
                val toLocaleData = collectStringsXmlData(file.name, toLocale)
                val translationCandidates =
                    createListOfTranslationCandidates(original, toLocaleData)
                if (translationCandidates.isEmpty()) {
                    return@launch
                }
                val originalStringsData = translationCandidates.joinToString(separator = "\n")
                val translatedStringsData =
                    makeTranslateRequest(originalStringsData, toLocale = toLocale)
                appendTranslatedStrings(
                    fileName = file.name,
                    translatedStringsData = translatedStringsData,
                    locale = toLocale
                )
            }
        }
    }
}

private fun fetchLocales(): List<String> {
    val regex = Regex("resourceConfigurations\\.addAll\\(listOf\\((.*?)\\)\\)")
    val gradleText = appGradle.readText()
    val matchResult = regex.find(gradleText)
    return if (matchResult != null) {
        val locales = matchResult.groupValues[1]
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it != "en" }// Remove quotes and trim

        return locales
    } else {
        emptyList()
    }
}

fun findStringXmlFiles(): List<File> {
    // Find the "values" folder inside the directory
    val valuesFolder = File(resDir, "values")
    if (!valuesFolder.exists() || !valuesFolder.isDirectory) {
        throw IllegalArgumentException("No 'values' folder found in the directory: ${resDir.absolutePath}")
    }

    // Filter XML files containing "strings" in their name
    return valuesFolder.listFiles { file ->
        file.isFile && file.name.contains(
            "strings",
            ignoreCase = true
        ) && file.name.endsWith(".xml", ignoreCase = true)
    }?.toList() ?: emptyList()
}

private suspend fun makeTranslateRequest(originalStringsData: String, toLocale: String): String {
    println(MESSAGE_SYSTEM)
    val chatGPTRequest = ChatGPTRequest(
        model = "gpt-4o-mini",
        messages = listOf(
            Message(role = "system", content = MESSAGE_SYSTEM.replace("{locale}", toLocale)),
            Message(
                role = "user",
                content = "Provide translation for the following strings.xml to locale of \"$toLocale\".\n" +
                        originalStringsData
            )
        ),
        temperature = 1f,
    )
    val chatGPTResponse = ktorClient.post(ENDPOINT) {
        setBody(chatGPTRequest)
    }.body<ChatGPTResponse>()
    return chatGPTResponse.choices[0].message.content
}

fun collectStringsXmlData(fileName: String, locale: String? = null): Map<String, String> {
    val localeSuffix = if (locale != null) "-$locale" else ""
    val localeValuesFolder = File(resDir, "values$localeSuffix")
    if (!localeValuesFolder.exists()) {
        localeValuesFolder.mkdir()
    }
    val stringsFile = File(localeValuesFolder, fileName)
    if (!stringsFile.exists()) {
        stringsFile.createNewFile()
        stringsFile.writeText("<resources>\n</resources>")
    }
    val data = stringsFile.readText()
    return getKeyAndStringsMap(data)
}

fun getKeyAndStringsMap(data: String): Map<String, String> {
    val stringResRegex = Regex("<string name=(\".+\")>(.+)</string>")
    val arrayResRegex =
        Regex("<string-array\\s+name=\"([^\"]+)\">\\s*(<item>(.*?)</item>\\s*)+</string-array>")
    val pluralResRegex =
        Regex("<plurals\\s+name=\"([^\"]+)\">\\s*(<item\\s+quantity=\"([^\"]+)\">(.*?)</item>\\s*)+</plurals>")

    val result = HashMap<String, String>()

    stringResRegex.findAll(data).forEach {
        val key = it.groups[1]?.value ?: return@forEach
        val value = it.groupValues[0]
        if (key.contains("translatable=\"false\"")) {
            return@forEach
        }
        result[key] = value
    }
    arrayResRegex.findAll(data).forEach {
        val key = it.groups[1]?.value ?: return@forEach
        val value = it.groupValues[0]
        if (key.contains("translatable=\"false\"")) {
            return@forEach
        }

        result[key] = value
    }
    pluralResRegex.findAll(data).forEach {
        val key = it.groups[1]?.value ?: return@forEach
        val value = it.groupValues[0]
        if (key.contains("translatable=\"false\"")) {
            return@forEach
        }
        result[key] = value
    }
    return result
}

private fun createListOfTranslationCandidates(
    original: Map<String, String>,
    toLocale: Map<String, String>,
): List<String> {
    val result = mutableListOf<String>()
    original.forEach { (key, value) ->
        if (toLocale[key].isNullOrBlank()) {
            result.add(value)
        }
    }
    return result
}

private fun appendTranslatedStrings(
    fileName: String,
    translatedStringsData: String,
    locale: String? = null,
) {
    val localeSuffix = if (locale != null) "-$locale" else ""
    val localeValuesFolder = File(resDir, "values$localeSuffix")
    if (!localeValuesFolder.exists()) {
        localeValuesFolder.mkdir()
    }
    val stringsFile = File(localeValuesFolder, fileName)
    var data = stringsFile.readText()
    val tabbedTranslations = translatedStringsData
        .replace(Regex("^<"), "\t<")
        .replace(Regex("\n<"), "\n\t<")
        .replace("<item", "\t<item")

    data = data
        .replace("</resources>", "$tabbedTranslations\n</resources>")
        .replace("'", "’")
    stringsFile.writeText(data)
}