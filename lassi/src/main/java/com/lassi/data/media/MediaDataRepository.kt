package com.lassi.data.media

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.database.getStringOrNull
import com.lassi.common.utils.KeyUtils
import com.lassi.common.utils.Logger
import com.lassi.data.mediadirectory.Folder
import com.lassi.data.mediadirectory.FolderDetail
import com.lassi.domain.media.LassiConfig
import com.lassi.domain.media.MediaRepository
import com.lassi.domain.media.MediaType
import io.reactivex.Single
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class MediaDataRepository(private val context: Context) : MediaRepository {

    private val minTimeInMillis = LassiConfig.getConfig().minTime * 1000L
    private val maxTimeInMillis = LassiConfig.getConfig().maxTime * 1000L
    private val fetchedFolders = arrayListOf<Folder>()
    private val folderMap = LinkedHashMap<String, Folder>()

    override fun fetchFolders(): Single<ArrayList<Folder>> {
        val projection = getProjections()
        val cursor = query(projection)
        cursor ?: return Single.error(Throwable())
        folderMap.clear()
        fetchedFolders.clear()
        try {
            if (cursor.moveToLast()) {
                do {
                    val id = cursor.getLong(cursor.getColumnIndex(projection[0]))
                    val name = cursor.getString(cursor.getColumnIndex(projection[1]))
                    val path = cursor.getString(cursor.getColumnIndex(projection[2]))
                    val bucket = cursor.getString(cursor.getColumnIndex(projection[3]))
                    val albumCoverPath =
                        if (LassiConfig.getConfig().mediaType == MediaType.AUDIO) {
                            val albumId = cursor.getString(cursor.getColumnIndex(projection[5]))
                            getAlbumArt(albumId)
                        } else {
                            ""
                        }
                    val duration =
                        if (LassiConfig.getConfig().mediaType == MediaType.VIDEO) {
                            cursor.getLong(cursor.getColumnIndex(projection[4]))
                        } else {
                            0
                        }

                    val file = makeSafeFile(path)
                    if (file != null && file.exists()) {
                        if (LassiConfig.getConfig().mediaType == MediaType.VIDEO ||
                            LassiConfig.getConfig().mediaType == MediaType.AUDIO
                        ) {
                            if (minTimeInMillis > KeyUtils.DEFAULT_VIDEO_DURATION &&
                                maxTimeInMillis > KeyUtils.DEFAULT_VIDEO_DURATION
                            ) {
                                if (duration in minTimeInMillis..maxTimeInMillis) {
                                    addFileToFolder(
                                        bucket,
                                        MiMedia(id, name, path, duration, albumCoverPath)
                                    )
                                }
                            } else if (minTimeInMillis == KeyUtils.DEFAULT_VIDEO_DURATION &&
                                maxTimeInMillis != KeyUtils.DEFAULT_VIDEO_DURATION
                            ) {
                                if (duration <= maxTimeInMillis) {
                                    addFileToFolder(
                                        bucket,
                                        MiMedia(id, name, path, duration, albumCoverPath)
                                    )
                                }
                            } else if (maxTimeInMillis == KeyUtils.DEFAULT_VIDEO_DURATION &&
                                minTimeInMillis != KeyUtils.DEFAULT_VIDEO_DURATION
                            ) {
                                if (minTimeInMillis <= duration) {
                                    addFileToFolder(
                                        bucket,
                                        MiMedia(id, name, path, duration, albumCoverPath)
                                    )
                                }
                            } else {
                                addFileToFolder(
                                    bucket,
                                    MiMedia(id, name, path, duration, albumCoverPath)
                                )
                            }
                        } else {
                            addFileToFolder(
                                bucket,
                                MiMedia(id, name, path, duration, albumCoverPath)
                            )
                        }
                    }
                } while (cursor.moveToPrevious())
            }
        } catch (e: Exception) {
            Logger.e("MediaDataRepository", "fetchFolders >> $e")
            return Single.error(e)
        } finally {
            cursor.close()
        }
        fetchedFolders.addAll(folderMap.values)
        return Single.just(fetchedFolders)
    }

    /**
     * Add file to folder
     */
    private fun addFileToFolder(
        bucket: String?,
        miMedia: MiMedia
    ) {
        val bucketName = bucket ?: "0"
        if (isFileTypeSupported(miMedia.path)) {
            var folder = folderMap[bucketName]
            if (folder == null) {
                folder = Folder(bucketName)
                folderMap[bucketName] = folder
            }
            folder.medias.add(miMedia)
        }
    }

    /**
     * Check if file type is supported
     */
    private fun isFileTypeSupported(path: String?): Boolean {
        if (path != null) {
            if (LassiConfig.getConfig().supportedFileType.isNotEmpty()) {
                for (fileType in LassiConfig.getConfig().supportedFileType) {
                    if (path.endsWith(fileType, true)) return true
                }
            } else {
                return true
            }
        }
        return false
    }

    /**
     * check if File path is not null
     */
    private fun makeSafeFile(path: String?): File? {
        if (path == null || path.isEmpty()) {
            return null
        }
        return try {
            File(path)
        } catch (ignored: Exception) {
            null
        }
    }

    /**
     * Get List of columns to fetch file info
     */
    private fun getProjections(): Array<String> {
        return when (LassiConfig.getConfig().mediaType) {
            MediaType.IMAGE -> arrayOf(
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.TITLE,
                MediaStore.Images.Media.DATA
            )
            MediaType.VIDEO -> arrayOf(
                MediaStore.Video.Media.BUCKET_ID,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.VideoColumns.DURATION
            )
            MediaType.AUDIO -> arrayOf(
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.AudioColumns.DURATION
            )
            else -> arrayOf(
                MediaStore.Files.FileColumns.BUCKET_ID,
                MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.TITLE,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.PARENT
            )
        }
    }

    private fun query(projection: Array<String>): Cursor? {
        val BUCKET_GROUP_BY = "1) GROUP BY 1,(2"
        val BUCKET_ORDER_BY = MediaStore.Images.Media.DATE_MODIFIED + " DESC"
        return when (LassiConfig.getConfig().mediaType) {
            MediaType.IMAGE -> context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                BUCKET_GROUP_BY,
                null,
                BUCKET_ORDER_BY
            )


            MediaType.VIDEO -> context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                BUCKET_GROUP_BY,
                null,
                BUCKET_ORDER_BY
            )

            MediaType.AUDIO -> context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                BUCKET_GROUP_BY,
                null,
                BUCKET_ORDER_BY
            )

            MediaType.DOC -> {
                val mimeTypes = mutableListOf<String>()
                LassiConfig.getConfig().supportedFileType.forEach { mimeType ->
                    MimeTypeMap
                        .getSingleton()
                        .getMimeTypeFromExtension(mimeType)?.let {
                            mimeTypes.add("'$it'")
                        }
                }
                val selectionMimeType =
                    MediaStore.Files.FileColumns.MIME_TYPE + " IN (${mimeTypes.joinToString()})"
                context.contentResolver.query(
                    MediaStore.Files.getContentUri("external"),
                    projection,
                    selectionMimeType,
                    null,
                    MediaStore.Video.Media.DATE_ADDED
                )
            }
        }
    }

    private fun fetchMediaQuery(projection: Array<String>, folderName: String): Cursor? {

        return when (LassiConfig.getConfig().mediaType) {
            MediaType.IMAGE -> context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Images.Media.DATA} like ? ",
                arrayOf("%$folderName%"),
                null
            )


            MediaType.VIDEO -> context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Video.Media.DATA} like ? ",
                arrayOf("%$folderName%"),
                null
            )

            MediaType.AUDIO -> context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.DATA} like ? ",
                arrayOf("%$folderName%"),
                null
            )


            else -> context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.DATA} like ? ",
                arrayOf("%$folderName%"),
                null
            )
        }
    }

    /**
     * fetch album art for audio files
     */
    private fun getAlbumArt(albumId: String): String {
        var albumCoverPath = ""
        val cursorAlbum = context.contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM_ART
            ),
            MediaStore.Audio.Albums._ID + "=" + albumId,
            null, null
        )

        if (cursorAlbum != null) {
            if (cursorAlbum.count > 0 && cursorAlbum.moveToFirst()) {
                albumCoverPath = cursorAlbum.getStringOrNull(
                    cursorAlbum.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)
                ) ?: ""
            }
        }
        cursorAlbum?.close()
        return albumCoverPath
    }

    override fun fetchDocs(): Single<ArrayList<MiMedia>> {
        val projection = getProjections()
        val cursor = query(projection)
        cursor ?: return Single.error(Throwable())
        val docs = ArrayList<MiMedia>()
        try {
            if (cursor.moveToLast()) {
                do {
                    val id = cursor.getLong(cursor.getColumnIndex(projection[0]))
                    val name = cursor.getString(cursor.getColumnIndex(projection[1]))
                    val path = cursor.getString(cursor.getColumnIndex(projection[2]))
                    docs.add(MiMedia(id, name, path, 0))
                } while (cursor.moveToPrevious())
            }
        } catch (e: Exception) {
            Logger.e("MediaDataRepository", "fetchFolders >> $e")
        } finally {
            cursor.close()
        }
        return Single.just(docs)
    }

    override fun fetchFolderList(): Single<ArrayList<FolderDetail>> {
        val folderDetailList = ArrayList<FolderDetail>()
        val projection = getProjections()

//        val imagecursor: Cursor? = context.contentResolver.query(images, projection, BUCKET_GROUP_BY, null, BUCKET_ORDER_BY)
//        val imagecursor: Cursor? = context.contentResolver.query(images, projection1,"_data IS NOT NULL", null, BUCKET_ORDER_BY);
//TODO MANAGE QUERY FOR ANDROID 10

        var cursor = query(projection)
        cursor?.let {
            while (it.moveToNext()) {
                val bucketColumnIndex = it.getColumnIndex(projection[1])
                val folderName = it.getString(bucketColumnIndex)
                val placeHolder = it.getColumnIndex(projection[4])
                folderDetailList.add(FolderDetail(folderName, it.getString(placeHolder)))
            }
            cursor.close()
        }
        return Single.just(folderDetailList)
    }


    override fun getAllMediaFromFolder(folderName: String): Single<ArrayList<MiMedia>> {
        var projection = getProjections()
        var mediaList: ArrayList<MiMedia> = ArrayList()
        var mediaCursor = fetchMediaQuery(projection, folderName)

        mediaCursor?.let {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndex(projection[2]))
                val name = it.getString(it.getColumnIndex(projection[3]))
                val path = it.getString(it.getColumnIndex(projection[4]))

                val albumCoverPath =
                    if (LassiConfig.getConfig().mediaType == MediaType.AUDIO) {
                        val albumId = it.getString(it.getColumnIndex(projection[0]))
                        getAlbumArt(albumId)
                    } else {
                        ""
                    }
                val duration =
                    if (LassiConfig.getConfig().mediaType == MediaType.VIDEO) {
                        it.getLong(it.getColumnIndex(projection[4]))
                    } else {
                        0L
                    }
                mediaList.add(MiMedia(id, name, path,duration,albumCoverPath))
            }
            it.close()
        }
        return Single.just(mediaList)
    }
}
