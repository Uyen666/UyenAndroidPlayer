package com.uyen.launcher.core.scanner

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import com.uyen.launcher.data.model.GameCategory
import com.uyen.launcher.data.model.GameItem
import java.util.Locale

/**
 * Galgame 引擎資訊模型
 */
data class GalgameEngineInfo(
    val engineName: String,
    val mimeType: String,
    val defaultLaunchPrefix: String,
    val tags: List<String>
)

/**
 * 本機遊戲與 ROM 智慧掃描引擎 (LocalRomScanner)
 * 
 * 商業級設計亮點：
 * 1. 智慧子資料夾識別：以子資料夾為遊戲實體單位，根除同一款 Galgame 被拆散成數十個 .xp3 碎片的痛點。
 * 2. 智慧引擎特徵比對：自動辨識 Kirikiri 2/Z、Ren'Py、Tyrano、Wolf RPG / JoiPlay 等主流 AVG 格式。
 * 3. 高畫質海報優先提取：自動探測子資料夾內之 cover.jpg/png、poster、folder 圖片，無縫繫結為海報。
 * 4. 向上相容單檔 ROM：根目錄或單檔夾之 .nes, .gba, .sfc 等復古單檔與獨立 .xp3 依然完美收錄。
 * 5. 安全防禦：嚴格遵守 Storage Access Framework (SAF) 權限邊界，具備遞迴深度保護與游標例外安全。
 */
class LocalRomScanner(private val contentResolver: ContentResolver) {

    fun scan(treeUri: Uri): List<GameItem> {
        val rootId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return emptyList()
        val found = mutableListOf<GameItem>()
        var inspectedDocuments = 0

        // 1. 查詢根目錄直屬子項目
        val rootChildren = queryDirectoryChildren(treeUri, rootId)
        inspectedDocuments += rootChildren.size

        for (child in rootChildren) {
            if (inspectedDocuments >= MAX_DOCUMENTS) break

            if (child.isDirectory) {
                // 檢查此子目錄是否為單一 Galgame 遊戲包
                val subFiles = queryDirectoryChildren(treeUri, child.documentId)
                inspectedDocuments += subFiles.size

                val fileNames = subFiles.map { it.name }
                val detectedEngine = detectGalgameEngine(fileNames)

                if (detectedEngine != null) {
                    // 識別為完整 Galgame 遊戲目錄！
                    val coverFile = subFiles.firstOrNull { isCoverFileName(it.name) }
                    val coverDocUri = coverFile?.let {
                        DocumentsContract.buildDocumentUriUsingTree(treeUri, it.documentId).toString()
                    }

                    // 挑選最佳啟動檔案 (如 data.xp3、game.rpa 或 index.html)
                    val launchTarget = selectLaunchTarget(subFiles, detectedEngine)
                    val launchUri = launchTarget?.let {
                        DocumentsContract.buildDocumentUriUsingTree(treeUri, it.documentId).toString()
                    } ?: DocumentsContract.buildDocumentUriUsingTree(treeUri, child.documentId).toString()

                    val totalBytes = subFiles.sumOf { it.size }
                    val sizeLabel = formatGameSize(totalBytes).takeIf { it.isNotEmpty() }?.let { " • $it" }.orEmpty()

                    found.add(
                        GameItem(
                            id = "local_dir:${child.documentId}",
                            title = child.name,
                            subtitle = "${detectedEngine.engineName}$sizeLabel",
                            category = GameCategory.GALGAME,
                            launchIntentUri = launchUri,
                            mimeType = detectedEngine.mimeType,
                            coverUrl = coverDocUri,
                            bannerUrl = coverDocUri,
                            tags = detectedEngine.tags
                        )
                    )
                } else {
                    // 若非 Galgame 包，檢查是否為存放 Retro ROM 的資料夾 (如 NES / GBA / SFC)
                    var hasRetroRoms = false
                    for (file in subFiles) {
                        if (!file.isDirectory) {
                            val ext = file.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                            if (categoryForExtension(ext) == GameCategory.RETRO) {
                                toSingleFileGameItem(treeUri, file.documentId, file.name, file.size)?.let {
                                    found.add(it)
                                    hasRetroRoms = true
                                }
                            }
                        }
                    }

                    // 若既不是 Galgame 也無直接 ROM，但有次級資料夾 (例如 Galgames/Yuzusoft/SenrenBanka/)，再探測一層
                    if (!hasRetroRoms) {
                        for (grandChild in subFiles.filter { it.isDirectory }) {
                            if (inspectedDocuments >= MAX_DOCUMENTS) break
                            val grandSubFiles = queryDirectoryChildren(treeUri, grandChild.documentId)
                            inspectedDocuments += grandSubFiles.size

                            val grandNames = grandSubFiles.map { it.name }
                            val grandEngine = detectGalgameEngine(grandNames)
                            if (grandEngine != null) {
                                val grandCover = grandSubFiles.firstOrNull { isCoverFileName(it.name) }
                                val grandCoverUri = grandCover?.let {
                                    DocumentsContract.buildDocumentUriUsingTree(treeUri, it.documentId).toString()
                                }
                                val grandLaunch = selectLaunchTarget(grandSubFiles, grandEngine)
                                val grandLaunchUri = grandLaunch?.let {
                                    DocumentsContract.buildDocumentUriUsingTree(treeUri, it.documentId).toString()
                                } ?: DocumentsContract.buildDocumentUriUsingTree(treeUri, grandChild.documentId).toString()

                                val grandBytes = grandSubFiles.sumOf { it.size }
                                val grandSizeLabel = formatGameSize(grandBytes).takeIf { it.isNotEmpty() }?.let { " • $it" }.orEmpty()

                                found.add(
                                    GameItem(
                                        id = "local_dir:${grandChild.documentId}",
                                        title = grandChild.name,
                                        subtitle = "${grandEngine.engineName}$grandSizeLabel",
                                        category = GameCategory.GALGAME,
                                        launchIntentUri = grandLaunchUri,
                                        mimeType = grandEngine.mimeType,
                                        coverUrl = grandCoverUri,
                                        bannerUrl = grandCoverUri,
                                        tags = grandEngine.tags
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                // 根目錄之獨立單檔 (單檔 ROM 或獨立 .xp3/.rpa 檔)
                toSingleFileGameItem(treeUri, child.documentId, child.name, child.size)?.let {
                    found.add(it)
                }
            }
        }

        return found.distinctBy { it.id }.sortedBy { it.title }
    }

    private fun queryDirectoryChildren(treeUri: Uri, parentDocId: String): List<SafDocumentInfo> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val result = mutableListOf<SafDocumentInfo>()
        runCatching {
            contentResolver.query(childrenUri, PROJECTION, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idIdx) ?: continue
                    val name = cursor.getString(nameIdx) ?: continue
                    val mime = cursor.getString(mimeIdx) ?: ""
                    val size = if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L
                    val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                    result.add(SafDocumentInfo(docId, name, mime, size, isDir))
                }
            }
        }
        return result
    }

    private fun toSingleFileGameItem(treeUri: Uri, documentId: String, fileName: String, size: Long): GameItem? {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val category = categoryForExtension(extension) ?: return null
        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        val sizeLabel = formatGameSize(size).takeIf { it.isNotEmpty() }?.let { " • $it" }.orEmpty()
        val mimeType = when (extension) {
            "nes", "fc" -> "application/x-nes-rom"
            "gba" -> "application/x-gba-rom"
            "sfc", "smc" -> "application/x-snes-rom"
            "xp3" -> "application/x-xp3"
            "rpa" -> "application/x-rpa"
            else -> "application/octet-stream"
        }
        val tagLabel = when (category) {
            GameCategory.GALGAME -> listOf("Galgame", extension.uppercase(Locale.ROOT))
            GameCategory.RETRO -> listOf("復古遊戲", extension.uppercase(Locale.ROOT))
            else -> listOf("本機遊戲", extension.uppercase(Locale.ROOT))
        }
        return GameItem(
            id = "local_file:${documentId}",
            title = fileName.substringBeforeLast('.', fileName),
            subtitle = "本機單檔$sizeLabel",
            category = category,
            launchIntentUri = uri.toString(),
            mimeType = mimeType,
            tags = tagLabel
        )
    }

    private data class SafDocumentInfo(
        val documentId: String,
        val name: String,
        val mimeType: String,
        val size: Long,
        val isDirectory: Boolean
    )

    companion object {
        const val MAX_DOCUMENTS = 20_000

        val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )

        /**
         * 智慧特徵識別 Galgame 引擎種類
         */
        fun detectGalgameEngine(fileNames: List<String>): GalgameEngineInfo? {
            val lowerNames = fileNames.map { it.lowercase(Locale.ROOT) }

            // 1. 吉里吉里 2 / 吉里吉里 Z (Kirikiri)
            if (lowerNames.any { it.endsWith(".xp3") || it == "startup.tjs" || it.endsWith(".krz") }) {
                return GalgameEngineInfo(
                    engineName = "吉里吉里 2/Z (Kirikiri)",
                    mimeType = "application/x-xp3",
                    defaultLaunchPrefix = "data.xp3",
                    tags = listOf("Galgame", "吉里吉里", "Kirikiri")
                )
            }

            // 2. Ren'Py AVG 視覺小說
            if (lowerNames.any { it.endsWith(".rpa") || it == "options.rpy" || it == "game" }) {
                return GalgameEngineInfo(
                    engineName = "Ren'Py 視覺小說",
                    mimeType = "application/x-rpa",
                    defaultLaunchPrefix = "game.rpa",
                    tags = listOf("Galgame", "Ren'Py", "AVG")
                )
            }

            // 3. TyranoBuilder / TyranoScript
            if (lowerNames.contains("index.html") && lowerNames.any { it.contains("tyrano") || it.contains("data") }) {
                return GalgameEngineInfo(
                    engineName = "TyranoBuilder 引擎",
                    mimeType = "text/html",
                    defaultLaunchPrefix = "index.html",
                    tags = listOf("Galgame", "Tyrano", "AVG")
                )
            }

            // 4. RPG Maker / Wolf RPG / JoiPlay 相容視覺系
            if (lowerNames.any { it == "data.wolf" || it == "game.exe" || it == "rpg_rt.ldb" || it.endsWith(".rgss3a") }) {
                return GalgameEngineInfo(
                    engineName = "RPG Maker / Wolf RPG",
                    mimeType = "application/x-msdos-program",
                    defaultLaunchPrefix = "game.exe",
                    tags = listOf("Galgame", "RPG", "JoiPlay")
                )
            }

            // 5. ONScripter 視覺小說
            if (lowerNames.any { it.endsWith(".nsa") || it.endsWith(".ons") || it == "0.txt" || it == "default.txt" }) {
                return GalgameEngineInfo(
                    engineName = "ONScripter 視覺小說",
                    mimeType = "application/octet-stream",
                    defaultLaunchPrefix = "0.txt",
                    tags = listOf("Galgame", "ONScripter")
                )
            }

            return null
        }

        /**
         * 挑選最適合喚起的啟動檔案
         */
        private fun selectLaunchTarget(files: List<SafDocumentInfo>, engine: GalgameEngineInfo): SafDocumentInfo? {
            // 優先尋找指定標準啟動檔 (如 data.xp3 或 game.rpa)
            val exactMatch = files.firstOrNull { it.name.equals(engine.defaultLaunchPrefix, ignoreCase = true) }
            if (exactMatch != null) return exactMatch

            // 次選：任何符合引擎副檔名的第一個主要檔案
            return when (engine.mimeType) {
                "application/x-xp3" -> files.firstOrNull { it.name.endsWith(".xp3", ignoreCase = true) }
                "application/x-rpa" -> files.firstOrNull { it.name.endsWith(".rpa", ignoreCase = true) }
                "text/html" -> files.firstOrNull { it.name.equals("index.html", ignoreCase = true) }
                else -> files.firstOrNull { !it.isDirectory }
            }
        }

        /**
         * 探測是否為常見海報/封面圖片命名 (不區分大小寫)
         */
        fun isCoverFileName(name: String): Boolean {
            val lower = name.lowercase(Locale.ROOT).trim()
            val ext = lower.substringAfterLast('.', "")
            if (ext !in listOf("jpg", "jpeg", "png", "webp", "bmp")) return false

            val base = lower.substringBeforeLast('.')
            return base in listOf("cover", "folder", "poster", "banner", "thumb", "thumbnail", "icon", "front") ||
                    base.contains("cover") || base.contains("poster") || base.contains("thumb")
        }

        /**
         * 副檔名分類比對
         */
        fun categoryForExtension(extension: String): GameCategory? = when (extension.lowercase(Locale.ROOT)) {
            "xp3", "rpa", "ons", "nsa" -> GameCategory.GALGAME
            "nes", "fc", "gba", "sfc", "smc" -> GameCategory.RETRO
            else -> null
        }

        /**
         * 格式化遊戲容量大小顯示 (如 3.2 GB, 450 MB)
         */
        fun formatGameSize(bytes: Long): String {
            if (bytes <= 0) return ""
            val gb = bytes / (1024.0 * 1024.0 * 1024.0)
            return if (gb >= 1.0) {
                String.format(Locale.US, "%.1f GB", gb)
            } else {
                val mb = bytes / (1024.0 * 1024.0)
                if (mb >= 1.0) {
                    String.format(Locale.US, "%.0f MB", mb)
                } else {
                    val kb = bytes / 1024.0
                    String.format(Locale.US, "%.0f KB", kb)
                }
            }
        }
    }
}
