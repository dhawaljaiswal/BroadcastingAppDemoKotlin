package com.ratufa.livebroadcastingdemo.utils

import android.content.ContentResolver
import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.bambuser.broadcaster.BackendApi
import okhttp3.*
import okio.BufferedSink
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by Nand Kishor Patidar on 21,August,2018
 * Email nandkishor.patidar@ratufa.com.
 *
 */
internal object UploadHelper {
    private val LOGTAG = "UploadHelper"

    private val HTTP_CLIENT = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

    /** Implement the ProgressCallback interface to know when an upload is done
     * or failed, and to get regular callbacks about progress while uploading.
     * These methods may be invoked on a worker thread.  */
    internal interface ProgressCallback {
        fun onSuccess(fileName: String?)
        fun onError(error: String)
        /** Called regularly while uploading, to inform the observer about progress,
         * and to check whether uploading is still desired or should be aborted.
         * @param currentBytes The number of bytes sent so far
         * @param totalBytes The number of bytes which should be sent
         * @return Return true if the upload should continue, false to abort.
         */
        fun onProgress(currentBytes: Long, totalBytes: Long): Boolean
    }

    /** Helper method for uploading data on a worker thread.
     * Uploading files is only possible using an applicationId with special rights.
     * @param context [Context] used for resolving the Uri and reading metadata.
     * @param uri [Uri] to the media data (file) that should be uploaded.
     * @param applicationId Secret Bambuser-provided application specific ID.
     * @param author Author for this particular file, optional.
     * @param title Title/description for this file, optional.
     * @param location Location for this file, optional.
     * @param cb An object implementing the [ProgressCallback] interface.
     */
    fun upload(context: Context, uri: Uri, applicationId: String, author: String?, title: String?, location: Location?, cb: ProgressCallback) {
        object : Thread("UploadHelperThread") {
            override fun run() {
                val cr = context.contentResolver
                val mimeType = cr.getType(uri)
                if (mimeType == null || mimeType.indexOf("/") < 0) {
                    cb.onError("unsupported media type: " + mimeType!!)
                    return
                }
                val type: String
                if (mimeType.startsWith("video"))
                    type = "video"
                else if (mimeType.startsWith("image"))
                    type = "image"
                else {
                    cb.onError("unsupported media type: $mimeType")
                    return
                }
                val inputStream: InputStream?
                try {
                    inputStream = cr.openInputStream(uri)
                } catch (e: Exception) {
                    cb.onError("could not open $uri")
                    return
                }

                if (inputStream == null) {
                    cb.onError("could not open $uri")
                    return
                }

                val dateTaken = getDateTaken(cr, uri)
                val proj = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
                val cursor = cr.query(uri, proj, null, null, null)
                cursor!!.moveToFirst()
                val displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                // openableSize is sometimes off by a few bytes at least on kitkat. not reliable
                // final long openableSize = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
                val fileName: String?
                if (displayName != null && displayName.length > 1) {
                    fileName = displayName
                } else {
                    val suffix = mimeType.substring(mimeType.indexOf("/") + 1)
                    val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
                    fileName = "upload_" + sdf.format(Date()) + "." + suffix
                }
                cursor.close()

                val fileSize: Long
                try {
                    val pfd = cr.openFileDescriptor(uri, "r")
                    fileSize = pfd!!.statSize
                    pfd.close()
                } catch (e: Exception) {
                    cb.onError("Unable to determine data size")
                    return
                }

                if (fileSize < 1024) {
                    cb.onError("File has no content")
                    return
                }

                val params = HashMap<String, String>()
                params[BackendApi.TICKET_FILE_NAME] = fileName
                params[BackendApi.TICKET_FILE_TYPE] = type
                if (dateTaken > 0)
                    params[BackendApi.TICKET_FILE_CREATED] = (dateTaken / 1000).toString()
                if (author != null && author.length > 0)
                    params[BackendApi.TICKET_FILE_AUTHOR] = author
                if (title != null && title.length > 0)
                    params[BackendApi.TICKET_FILE_TITLE] = title
                if (location != null) {
                    params[BackendApi.TICKET_FILE_POS_LAT] = location.latitude.toString()
                    params[BackendApi.TICKET_FILE_POS_LON] = location.longitude.toString()
                    params[BackendApi.TICKET_FILE_POS_ACCURACY] = location.accuracy.toString()
                }

                val pair = BackendApi.getUploadTicketForApplicationId(context, params, applicationId)
                val response = pair.second
                val ticket = getJsonObjectFromString(response)
                if (ticket == null || !ticket.optString("upload_url").contains("://")) {
                    var error = "Could not get ticket for uploading."
                    if (response == null || response.length <= 0)
                        error += " No response from server."
                    else
                        error += " Unexpected server response. http code " + pair.first
                    cb.onError(error)
                    return
                }
                val uploadUrl = ticket.optString("upload_url")

                val requestBody = object : RequestBody() {
                    override fun contentType(): MediaType? {
                        return MediaType.parse(mimeType)
                    }

                    @Throws(IOException::class)
                    override fun contentLength(): Long {
                        return fileSize
                    }

                    @Throws(IOException::class)
                    override fun writeTo(sink: BufferedSink) {
                        var sent: Long = 0
                        val buffer = ByteArray(65536)
                        var bytesRead = 0
                        var running = true
                        bytesRead = inputStream.read(buffer)
                        while (running && (bytesRead) != -1) {
                            sink.write(buffer, 0, bytesRead)
                            sent += bytesRead.toLong()
                            running = cb.onProgress(sent, fileSize)
                        }
                    }
                }

                var responseCode = 0
                try {
                    val request = Request.Builder().url(uploadUrl)
                            .cacheControl(CacheControl.Builder().noCache().noStore().build())
                            .put(requestBody).build()
                    val res = HTTP_CLIENT.newCall(request).execute()
                    responseCode = res.code()
                    res.body()!!.close()
                } catch (e: Exception) {
                    Log.w(LOGTAG, "Exception when doing PUT: $e")
                }

                try {
                    inputStream.close()
                } catch (e: Exception) {
                }

                if (responseCode == 200)
                    cb.onSuccess(fileName)
                else
                    cb.onError("upload of $fileName failed with code $responseCode")
            }
        }.start()
    }

    private fun getDateTaken(cr: ContentResolver, uri: Uri): Long {
        // for modern document Uris, rely on Document.COLUMN_LAST_MODIFIED
        var columnName = DocumentsContract.Document.COLUMN_LAST_MODIFIED
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || ContentResolver.SCHEME_CONTENT == uri.scheme && MediaStore.AUTHORITY == uri.authority) {
            // we have a MediaStore Uri and should query for the DATE_TAKEN column if possible
            val mimeType = cr.getType(uri)
            if (mimeType != null && mimeType.startsWith("image"))
                columnName = MediaStore.Images.ImageColumns.DATE_TAKEN
            else if (mimeType != null && mimeType.startsWith("video"))
                columnName = MediaStore.Video.VideoColumns.DATE_TAKEN
            else
                columnName = MediaStore.MediaColumns.DATE_MODIFIED
        }
        val projection = arrayOf(columnName)
        var dateTaken: Long = 0
        try {
            val cursor = cr.query(uri, projection, null, null, null)
            if (cursor != null) {
                if (cursor.moveToFirst())
                    dateTaken = cursor.getLong(cursor.getColumnIndex(columnName))
                cursor.close()
            }
        } catch (e: Exception) {
        }

        if (MediaStore.MediaColumns.DATE_MODIFIED == columnName)
            dateTaken *= 1000
        return dateTaken
    }

    /** Convenience method for creating a JSONObject from a String.
     * @param string A String, preferably containing JSON.
     * @return A [JSONObject] or null.
     */
    private fun getJsonObjectFromString(string: String?): JSONObject? {
        if (string != null && string.length > 0)
            try {
                return JSONObject(string)
            } catch (e: JSONException) {
                Log.w(LOGTAG, "exception in json parsing: $e")
            }

        return null
    }
}