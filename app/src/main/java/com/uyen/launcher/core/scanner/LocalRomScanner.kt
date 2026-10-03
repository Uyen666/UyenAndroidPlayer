package com.uyen.launcher.core.scanner

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import com.uyen.launcher.data.model.GameCategory
import com.uyen.launcher.data.model.GameItem

/** Scans only a folder explicitly granted by the user through Android's document picker. */
class LocalRomScanner(private val contentResolver: ContentResolver) {

    fun scan(treeUri: Uri): List<GameItem> {
        val rootId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return emptyList()
        val pending = ArrayDeque<String>().apply { add(rootId) }
        val visited = mutableSetOf<String>()
        val found = mutableListOf<GameItem>()
        var inspectedDocuments = 0

        while (pending.isNotEmpty() && inspectedDocuments < MAX_DOCUMENTS) {
            val parentId = pending.removeFirst()
            if (!visited.add(parentId)) continue
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            runCatching {
                contentResolver.query(childrenUri, PROJECTION, null, null, null)?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    while (cursor.moveToNext()) {
                        if (inspectedDocuments >= MAX_DOCUMENTS) break
                        inspectedDocuments++
                        val documentId = cursor.getString(idIndex) ?: continue
                        val name = cursor.getString(nameIndex) ?: continue
                        val mimeType = cursor.getString(mimeIndex)
                        if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                            pending.add(documentId)
                        } else {
                            toGameItem(treeUri, documentId, name, sizeIndex.takeIf { it >= 0 }?.let(cursor::getLong))
                                ?.let(found::add)
                        }
                    }
                }
            }
        }
        return found.sortedBy(GameItem::title)
    }

    private fun toGameItem(treeUri: Uri, documentId: String, fileName: String, size: Long?): GameItem? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val category = categoryForExtension(extension) ?: return null
        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        val sizeLabel = size?.takeIf { it >= 0 }?.let { " • ${it / (1024 * 1024)} MB" }.orEmpty()
        val mimeType = when (extension) {
            "nes", "fc" -> "application/x-nes-rom"
            "gba" -> "application/x-gba-rom"
            "sfc", "smc" -> "application/x-snes-rom"
            "xp3" -> "application/x-xp3"
            else -> "application/octet-stream"
        }
        return GameItem(
            id = "local:${uri}",
            title = fileName.substringBeforeLast('.', fileName),
            subtitle = "本機遊戲檔$sizeLabel",
            category = category,
            launchIntentUri = uri.toString(),
            mimeType = mimeType,
            tags = listOf("本機遊戲", extension.uppercase())
        )
    }

    companion object {
        fun categoryForExtension(extension: String): GameCategory? = when (extension.lowercase()) {
            "xp3", "rpa", "ons" -> GameCategory.GALGAME
            "nes", "fc", "gba", "sfc", "smc" -> GameCategory.RETRO
            else -> null
        }

        const val MAX_DOCUMENTS = 20_000
        val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )
    }
}
