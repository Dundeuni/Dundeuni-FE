package com.dundueni.app.feature.analysisresult

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore.Images.Media
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35])
class ResultImageStorageTest {
    @Test fun savesPngToTheDundueniAlbumAndPublishesItOnScopedStorage() {
        val provider = saveWithProvider()
        assertEquals("image/png", provider.inserted.getAsString(Media.MIME_TYPE))
        assertTrue(provider.inserted.getAsString(Media.DISPLAY_NAME).endsWith(".png"))
        if (Build.VERSION.SDK_INT >= 29) {
            assertEquals("Pictures/Dundueni/", provider.inserted.getAsString(Media.RELATIVE_PATH))
            assertEquals(1, provider.inserted.getAsInteger(Media.IS_PENDING).toInt())
            assertEquals(0, provider.updated?.getAsInteger(Media.IS_PENDING)?.toInt())
        } else {
            assertTrue(provider.file.parentFile?.path.orEmpty().replace(File.separatorChar, '/').endsWith("/Pictures/Dundueni"))
        }
        assertTrue(provider.file.length() > 0)
        assertFalse(provider.deleted)
    }

    @Test fun clearsIncompleteMediaStoreEntryAndFileWhenWritingFails() {
        val provider = saveWithProvider(failWrite = true)
        assertTrue(provider.deleted)
        assertFalse(provider.file.exists())
    }

    private fun saveWithProvider(failWrite: Boolean = false): StorageProvider {
        val context = RuntimeEnvironment.getApplication()
        val provider = StorageProvider(context.cacheDir, context.contentResolver, failWrite)
        provider.attachInfo(context, ProviderInfo().apply { authority = "media" })
        ShadowContentResolver.registerProviderInternal("media", provider)
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        try {
            if (failWrite) {
                assertThrows(IOException::class.java) { saveResultPng(context, bitmap) }
            } else {
                saveResultPng(context, bitmap)
            }
        } finally {
            bitmap.recycle()
        }
        return provider
    }

    @Suppress("DEPRECATION")
    private class StorageProvider(
        private val cacheDir: File,
        private val resolver: ContentResolver,
        private val failWrite: Boolean,
    ) : ContentProvider() {
        lateinit var inserted: ContentValues
        lateinit var file: File
        var updated: ContentValues? = null
        var deleted = false

        override fun insert(uri: Uri, values: ContentValues?): Uri {
            inserted = ContentValues(requireNotNull(values))
            file = inserted.getAsString(Media.DATA)?.let(::File) ?: File.createTempFile("result", ".png", cacheDir)
            val row = uri.buildUpon().appendPath("1").build()
            shadowOf(resolver).registerOutputStreamSupplier(row) {
                if (failWrite) {
                    object : FileOutputStream(file) {
                        override fun close() {
                            super.close()
                            throw IOException("Simulated write error")
                        }
                    }
                } else {
                    FileOutputStream(file)
                }
            }
            return row
        }

        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
            updated = ContentValues(requireNotNull(values))
            return 1
        }

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
            deleted = true
            if (Build.VERSION.SDK_INT >= 29) file.delete()
            return 1
        }

        override fun onCreate() = true
        override fun getType(uri: Uri): String? = null
        override fun query(
            uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?,
        ): Cursor? = null
    }
}
