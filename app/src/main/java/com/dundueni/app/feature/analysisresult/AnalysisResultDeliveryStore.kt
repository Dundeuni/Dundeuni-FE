package com.dundueni.app.feature.analysisresult

import com.dundueni.app.feature.analysis.AnalysisUiState
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.UUID
import java.util.concurrent.TimeUnit

internal data class AnalysisResultDelivery(
    val resultJson: String? = null,
    val screen: AnalysisUiState.ScreenSuccess? = null,
)

/** Result delivery survives recreation while the app's temporary cache entry is available. */
internal class AnalysisResultDeliveryStore(cacheDir: File) {
    private val directory = File(cacheDir, "analysis-result-delivery")
    private val gson = Gson()

    fun write(delivery: AnalysisResultDelivery, key: String = UUID.randomUUID().toString()): String {
        val target = file(key)
        if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
            throw IOException("Cannot prepare the result cache")
        }
        directory.listFiles()?.filter(::expired)?.forEach { it.delete() }
        val temporary = File.createTempFile("delivery-", ".tmp", directory)
        try {
            temporary.bufferedWriter().use { gson.toJson(delivery, it) }
            Files.move(temporary.toPath(), target.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } finally {
            temporary.delete()
        }
        return key
    }

    fun read(key: String): AnalysisResultDelivery? {
        val source = file(key)
        if (!source.isFile) return null
        if (expired(source)) {
            source.delete()
            return null
        }
        return source.bufferedReader().use { gson.fromJson(it, AnalysisResultDelivery::class.java) }
    }

    private fun file(key: String): File {
        require(UUID.fromString(key).toString() == key)
        return File(directory, "$key.json")
    }

    private fun expired(file: File): Boolean =
        System.currentTimeMillis() - file.lastModified() >= TimeUnit.HOURS.toMillis(24)
}
