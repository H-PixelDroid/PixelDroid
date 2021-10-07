package org.pixeldroid.app.postCreation

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.*

class ProgressRequestBody(private val mFile: InputStream, private val length: Long) : RequestBody() {

    private val getProgressSubject: PublishSubject<Float> = PublishSubject.create()

    val progressSubject: Observable<Float>
        get() {
            return getProgressSubject
        }

    override fun contentType(): MediaType? {
        return "image/png".toMediaTypeOrNull()
    }

    @Throws(IOException::class)
    override fun contentLength(): Long {
        return length
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val fileLength = contentLength()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var uploaded: Long = 0

        mFile.use {
            var read: Int
            var lastProgressPercentUpdate = 0.0f
            read = it.read(buffer)
            while (read != -1) {

                uploaded += read.toLong()
                sink.write(buffer, 0, read)
                read = it.read(buffer)

                val progress = (uploaded.toFloat() / fileLength.toFloat()) * 100f
                //prevent publishing too many updates, which slows upload, by checking if the upload has progressed by at least 1 percent
                if (progress - lastProgressPercentUpdate > 1 || progress == 100f) {
                    // publish progress
                    getProgressSubject.onNext(progress)
                    lastProgressPercentUpdate = progress
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 2048
    }
}