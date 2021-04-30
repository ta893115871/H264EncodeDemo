package com.bj.gxz.h264encodedemo

import android.hardware.display.DisplayManager
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.os.Environment
import android.util.Log
import java.io.FileOutputStream
import kotlin.experimental.and


/**
 * Created by guxiuzhong on 2020/12/30.
 */
class H264EncodeThread(private val mediaProjection: MediaProjection) : Thread("encode-h264") {

    companion object {
        const val TAG = "H264EncodeThread"
        const val WIDTH = 720
        const val HEIGHT = 1280
    }

    var mediaCodec: MediaCodec

    @Volatile
    var isStop: Boolean = false

    private val info: MediaCodec.BufferInfo = MediaCodec.BufferInfo()

    private val fos = FileOutputStream(
        Environment.getExternalStorageDirectory().absolutePath + "/screen.h264"
    )

    init {
        // H264/avc的编码器
        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val mediaFormat =
            MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT)

        mediaFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        // 码率
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, WIDTH * HEIGHT)
        // i帧间隔,MediaCodec一般会按照KEY_FRAME_RATE输出
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        // 帧率，每25帧后一次关键帧，即使画面不动
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25)

        // 第4个参数需要设置为CONFIGURE_FLAG_ENCODE=1
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        // surface，录屏和编码的进行关联
        val surface = mediaCodec.createInputSurface()
        mediaProjection.createVirtualDisplay(
            "screen-h264", WIDTH, HEIGHT, 2,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null
        )
    }


    override fun run() {
        super.run()
        try {
            Log.d(TAG, "run")
            while (!isStop) {
                mediaCodec.queueInputBuffer()
                val outIndex = mediaCodec.dequeueOutputBuffer(info, 10_1000)
                if (outIndex >= 0) {
                    // 取出编码后的H264数据
                    val byteBuffer = mediaCodec.getOutputBuffer(outIndex)
                    val data = ByteArray(info.size)
                    byteBuffer?.get(data)

                    check(data)
                    // to file
                    fos.write(data)

                    // 编码。给false 不需要渲染
                    mediaCodec.releaseOutputBuffer(outIndex, false);
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            fos.flush()
            fos.close()
            mediaProjection.stop()
            mediaCodec.stop()
            mediaCodec.release()
            Log.d(TAG, "Encode done")
        }
    }

    private fun check(data: ByteArray) {
        var index = 4  // 00 00 00 01
        if (data[2].toInt() == 0X1) { // 00 00 01
            index = 3
        }
        // NALU的数据类型,header 1个字节的后五位
        val naluType = (data[index].and(0x1F)).toInt()
        if (naluType == 7) {
            Log.d(TAG, "SPS")
        } else if (naluType == 8) {
            Log.d(TAG, "PPS")
        } else if (naluType == 5) {
            Log.d(TAG, "IDR")
        } else {
            Log.d(TAG, "非IDR=" + naluType)
        }
    }


    fun startEncode() {
        Log.d(TAG, "startEncode")
        isStop = false
        mediaCodec.start()
        start()
    }

    fun stopEncode() {
        Log.d(TAG, "stopEncode")
        isStop = true
    }
}
