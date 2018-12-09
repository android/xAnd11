package org.monksanctum.xand11.core

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.util.SparseArray
import android.view.KeyCharacterMap
import android.view.WindowManager
import org.monksanctum.xand11.display.Screen
import org.monksanctum.xand11.graphics.ColorPaintable
import org.monksanctum.xand11.graphics.XDrawable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

actual typealias Canvas = android.graphics.Canvas

actual typealias Looper = android.os.Looper
actual typealias HandlerThread = android.os.HandlerThread

actual typealias Handler = android.os.Handler
actual typealias Message = android.os.Message

actual typealias Rect = android.graphics.Rect

actual typealias InputStream = java.io.InputStream
actual typealias OutputStream = java.io.OutputStream

actual typealias Socket = java.net.Socket

actual typealias ServerSocket = java.net.ServerSocket

actual class BlockingQueue actual constructor() {
    val q = ArrayBlockingQueue<Integer>(20)

    actual open fun poll(t: Int): Int {
        return q.poll(t.toLong(), TimeUnit.MILLISECONDS).toInt()
    }

    actual open fun put(what: Int) {
        return q.put(Integer(what))
    }

}

actual typealias Bitmap = android.graphics.Bitmap

actual typealias Direction = android.graphics.Path.Direction
actual typealias Path = android.graphics.Path
actual typealias RectF = android.graphics.RectF

actual typealias XActivityManager = org.monksanctum.xand11.activity.XActivityManager

val mConfigs = SparseArray<android.graphics.Bitmap.Config>().also {
    // These all make sense.
    it.put(12, android.graphics.Bitmap.Config.ARGB_4444)
    it.put(24, android.graphics.Bitmap.Config.ARGB_8888)
    it.put(32, android.graphics.Bitmap.Config.ARGB_8888)
    it.put(16, android.graphics.Bitmap.Config.RGB_565)
    // These need better handling somehow.
    it.put(1, android.graphics.Bitmap.Config.ALPHA_8)
    it.put(4, android.graphics.Bitmap.Config.ALPHA_8)
    it.put(8, android.graphics.Bitmap.Config.ALPHA_8)
}

actual inline fun createBitmap(width: Int, height: Int, type: Int): Bitmap {
    return Bitmap.createBitmap(width, height, mConfigs[type]);
}

actual inline fun isValidConfigType(type: Int): Boolean {
    return mConfigs.indexOfKey(type) < 0
}

actual inline fun Socket.getHostAddress(): String {
    return inetAddress.hostAddress
}

actual open class IOException actual constructor(message: String?, cause: Throwable?)
    : Throwable(message, cause)

actual inline fun Handler.obtainMessage(what: Int, obj: Any): Message {
    return obtainMessage(what, 0, 0, obj)
}

actual fun Handler.post(function: () -> Unit) {
    this.post(function::invoke)
}

actual class XServer(val context: Context) {

}

actual inline fun Throwable.printStackTrace() {
    // TODO: How to call?
}

actual inline fun getMaxKeyCode(): Int {
    return android.view.KeyEvent.getMaxKeyCode()
}

actual inline fun initCharMap(keyboardMap: IntArray, modifiers: IntArray) {
    var index = 0
    val map = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
    for (i in 0 until getMaxKeyCode()) {
        for (j in modifiers.indices) {
            keyboardMap[index++] = map.get(i, modifiers[j])
        }
    }
}

actual class Platform {
    actual companion object {
        inline actual fun currentTimeMillis(): Long {
            return System.currentTimeMillis()
        }

        inline actual fun isTextEmpty(text: String): Boolean {
            return TextUtils.isEmpty(text)
        }

        inline actual fun createScreen(server: XServer): Screen {
            val display = (server.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                    .defaultDisplay
            val metrics = DisplayMetrics()
            display.getMetrics(metrics)
            return Screen(metrics.widthPixels, metrics.heightPixels)
        }

        actual fun logd(tag: String, s: String, t: Throwable?) {
            Log.d(tag, s, t)
        }

        actual fun loge(tag: String, s: String, t: Throwable?) {
            Log.e(tag, s, t)
        }

        actual fun logw(tag: String, s: String, t: Throwable?) {
            Log.w(tag, s, t)
        }

        inline actual fun Handler(sHandlerThread: HandlerThread): Handler {
            return Handler(sHandlerThread.looper)
        }

        inline actual fun intToHexString(i: Int): String {
            return Integer.toHexString(i)
        }

        actual fun startThread(run: () -> Unit) {
            Thread(run).start()
        }

        actual inline fun join(delim: String, list: Collection<Any?>): String {
            return TextUtils.join(delim, list)
        }

        actual inline fun rgb(r: Int, g: Int, b: Int): Int {
            return Color.rgb(r, g, b)
        }

        actual inline fun red(color: Int): Int {
            return Color.red(color)
        }

        actual inline fun green(color: Int): Int {
            return Color.green(color)
        }

        actual inline fun blue(color: Int): Int {
            return Color.blue(color)
        }

        actual fun drawColorPaintable(drawable: XDrawable, bounds: Rect, context: GraphicsContext?, color: ColorPaintable) {
            val canvas = drawable.lockCanvas(context!!)
            val p = if (context != null) Paint(context.paint) else Paint()
            p.color = color.mColor
            p.style = Paint.Style.FILL
            if (DEBUG) Platform.logd(ColorPaintable.TAG, "Drawing 0x" + intToHexString(color.mColor) + " on " + bounds)
            canvas.drawRect(bounds, p)
            drawable.unlockCanvas()
        }
    }
}
