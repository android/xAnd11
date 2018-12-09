package org.monksanctum.xand11.core

import org.monksanctum.xand11.display.Screen
import org.monksanctum.xand11.fonts.FontManager
import org.monksanctum.xand11.fonts.FontSpec
import org.monksanctum.xand11.graphics.ColorPaintable
import org.monksanctum.xand11.graphics.XDrawable
import org.monksanctum.xand11.windows.XWindow

expect class Canvas(bitmap: Bitmap? = null) {
    open fun save(): Int
    open fun clipRect(innerBounds: Rect): Boolean
    open fun restore()

}

expect class Looper

expect enum class Direction {
    CW
}

expect class Path() {
    fun addRect(rectF: RectF, cw: Direction)
    fun moveTo(toFloat: Float, toFloat1: Float)
    fun rLineTo(toFloat: Float, toFloat1: Float)
    fun lineTo(toFloat: Float, toFloat1: Float)

}

expect class RectF(r: Rect) {

}

expect class Message {
    var what: Int
    var obj: Any

    open fun sendToTarget()

}

expect class HandlerThread(name: String) {
    fun getLooper(): Looper
    fun start()
    open fun quitSafely(): Boolean

}

expect open class Handler(l: Looper? = null) {
    open fun handleMessage(message: Message)

}

expect fun Handler.obtainMessage(what: Int, obj: Any): Message
expect fun Handler.post(function: () -> Unit)

expect class XServer

expect class Socket(host: String, port: Int) {
    fun getInputStream(): InputStream
    fun getOutputStream(): OutputStream
    fun close()
}

expect fun Socket.getHostAddress(): String

expect abstract class InputStream {
    abstract fun read(): Int

}

expect class ServerSocket(port: Int) {
    fun accept(): Socket
    fun close()
}

expect abstract class OutputStream {
    abstract fun write(toInt: Int)
    open fun flush()
}

expect class XActivityManager {
    fun onWindowMapped(child: XWindow)
    fun onWindowUnmapped(child: XWindow)
}

expect fun initCharMap(keyboardMap: IntArray, modifiers: IntArray)

expect open class IOException(message: String? = null, cause: Throwable? = null) : Throwable

expect class Rect(left: Int = 0,
                  top: Int = 0,
                  right: Int = 0,
                  bottom: Int = 0) {
    constructor(other: Rect)

    fun width(): Int
    fun height(): Int
    fun offset(x: Int, y: Int)
    fun set(rect: Rect)

    var left: Int
    var top: Int
    var bottom: Int
    var right: Int

}

expect fun Throwable.printStackTrace()

expect class BlockingQueue() {
    open fun poll(t: Int): Int
    open fun put(what: Int)
}

expect class Bitmap {
    fun getWidth(): Int
    fun getHeight(): Int

    fun getPixel(j: Int, i: Int): Int
    fun setPixel(j: Int, i: Int, any: Int)
}

expect fun createBitmap(width: Int, height: Int, type: Int = 32): Bitmap

expect fun isValidConfigType(type: Int): Boolean

expect fun Canvas.drawBitmap(context: GraphicsContext?, bitmap: Bitmap, src: Rect, bounds: Rect)
expect fun Canvas.drawBitmap(context: GraphicsContext?, bitmap: Bitmap, x: Float, y: Float)

expect class Font internal constructor(mSpec: FontSpec, name: String?) {
    fun measureText(s: String): Float
    fun paintGetTextBounds(text: String, start: Int, length: Int, bounds: Rect)
    fun getTextBounds(str: String, x: Int, y: Int, rect: Rect)
    fun drawText(drawable: XDrawable, context: GraphicsContext, str: String, x: Int, y: Int, rect: Rect)

    val chars: List<CharInfo>
    val fontProperties: MutableList<FontProperty>
    var maxBounds: CharInfo
    var minBounds: CharInfo

    val minCharOrByte2: Char
    val defaultChar: Char
    var maxCharOrByte2: Char
    var fontAscent: Int
    var fontDescent: Int

    var isRtl: Boolean

    val minByte1: Byte
    val maxByte1: Byte

    var allCharsExist: Boolean

    class CharInfo {
        var leftSideBearing: Int
        var rightSideBearing: Int
        var characterWidth: Int
        var ascent: Int
        var descent: Int
        var attributes: Int
    }

    class FontProperty {
        var name: Int
        var value: Int
    }

    companion object {
        val LEFT_TO_RIGHT: Byte
        val RIGHT_TO_LEFT: Byte

        internal val DEFAULT: String
        internal val FIXED: String
    }
}

expect class GraphicsContext(mId: Int) {

    var drawable: Int
    var function: Byte
    var planeMask: Int
    var foreground : Int
    var background : Int
    var lineWidth: Int // Card16
    var lineStyle: Byte
    var capStyle: Byte
    var joinStyle: Byte
    var fillStyle: Byte
    var fillRule: Byte
    var tile: Int // PIXMAP
    var stipple: Int // PIXMAP
    var tileStippleX: Int // Card16
    var tileStippleY: Int // Card16
    var font: Int // Font
    var subwindowMode: Byte
    var graphicsExposures: Boolean
    var clipX: Int // Card16
    var clipY: Int // Card16
    var clipMask: Int // PIXMAP
    var dashOffset: Int // Card16
    var dashes: Byte
    var arcMode: Byte

    fun drawRect(canvas: Canvas, rect: Rect, stroke: Boolean = false)
    fun drawText(canvas: Canvas, f: Font?, v: String, x: Int, y: Int, bounds: Rect)
    fun drawPath(canvas: Canvas, p: Path, fill: Boolean = false)
    fun drawLine(canvas: Canvas, fx: Float, fy: Float, sx: Float, sy: Float)
    fun drawBitmap(canvas: Canvas, bitmap: Bitmap, x: Float, y: Float)
    fun createPaint(fontManager: FontManager)
    fun setClipPath(p: Path)
    fun init(c: Canvas)
}

expect class Platform {

    companion object {
        fun currentTimeMillis(): Long
        fun isTextEmpty(text: String): Boolean
        fun createScreen(server: XServer): Screen

        fun logd(tag: String, s: String, t: Throwable? = null)
        fun loge(tag: String, s: String, t: Throwable? = null)
        fun logw(tag: String, s: String, t: Throwable? = null)

        fun Handler(sHandlerThread: HandlerThread): Handler
        fun intToHexString(i: Int): String
        fun startThread(run: () -> Unit)
        fun join(delim: String, list: Collection<Any?>): String

        fun rgb(r: Int, g: Int, b: Int): Int
        fun red(color: Int): Int
        fun green(color: Int): Int
        fun blue(color: Int): Int

        fun drawColorPaintable(drawable: XDrawable, bounds: Rect, context: GraphicsContext?, color: ColorPaintable)
    }

}

expect fun getMaxKeyCode(): Int

object KeyEvent {
    val ACTION_DOWN = 0
    val ACTION_MULTIPLE = 2
    val ACTION_UP = 1

    val FLAG_CANCELED = 32
    val FLAG_CANCELED_LONG_PRESS = 256
    val FLAG_EDITOR_ACTION = 16
    val FLAG_FALLBACK = 1024
    val FLAG_FROM_SYSTEM = 8
    val FLAG_KEEP_TOUCH_MODE = 4
    val FLAG_LONG_PRESS = 128
    val FLAG_SOFT_KEYBOARD = 2
    val FLAG_TRACKING = 512
    val FLAG_VIRTUAL_HARD_KEY = 64

    val KEYCODE_0 = 7
    val KEYCODE_1 = 8
    val KEYCODE_11 = 227
    val KEYCODE_12 = 228
    val KEYCODE_2 = 9
    val KEYCODE_3 = 10
    val KEYCODE_3D_MODE = 206
    val KEYCODE_4 = 11
    val KEYCODE_5 = 12
    val KEYCODE_6 = 13
    val KEYCODE_7 = 14
    val KEYCODE_8 = 15
    val KEYCODE_9 = 16
    val KEYCODE_A = 29
    val KEYCODE_ALL_APPS = 284
    val KEYCODE_ALT_LEFT = 57
    val KEYCODE_ALT_RIGHT = 58
    val KEYCODE_APOSTROPHE = 75
    val KEYCODE_APP_SWITCH = 187
    val KEYCODE_ASSIST = 219
    val KEYCODE_AT = 77
    val KEYCODE_AVR_INPUT = 182
    val KEYCODE_AVR_POWER = 181
    val KEYCODE_B = 30
    val KEYCODE_BACK = 4
    val KEYCODE_BACKSLASH = 73
    val KEYCODE_BOOKMARK = 174
    val KEYCODE_BREAK = 121
    val KEYCODE_BRIGHTNESS_DOWN = 220
    val KEYCODE_BRIGHTNESS_UP = 221
    val KEYCODE_BUTTON_1 = 188
    val KEYCODE_BUTTON_10 = 197
    val KEYCODE_BUTTON_11 = 198
    val KEYCODE_BUTTON_12 = 199
    val KEYCODE_BUTTON_13 = 200
    val KEYCODE_BUTTON_14 = 201
    val KEYCODE_BUTTON_15 = 202
    val KEYCODE_BUTTON_16 = 203
    val KEYCODE_BUTTON_2 = 189
    val KEYCODE_BUTTON_3 = 190
    val KEYCODE_BUTTON_4 = 191
    val KEYCODE_BUTTON_5 = 192
    val KEYCODE_BUTTON_6 = 193
    val KEYCODE_BUTTON_7 = 194
    val KEYCODE_BUTTON_8 = 195
    val KEYCODE_BUTTON_9 = 196
    val KEYCODE_BUTTON_A = 96
    val KEYCODE_BUTTON_B = 97
    val KEYCODE_BUTTON_C = 98
    val KEYCODE_BUTTON_L1 = 102
    val KEYCODE_BUTTON_L2 = 104
    val KEYCODE_BUTTON_MODE = 110
    val KEYCODE_BUTTON_R1 = 103
    val KEYCODE_BUTTON_R2 = 105
    val KEYCODE_BUTTON_SELECT = 109
    val KEYCODE_BUTTON_START = 108
    val KEYCODE_BUTTON_THUMBL = 106
    val KEYCODE_BUTTON_THUMBR = 107
    val KEYCODE_BUTTON_X = 99
    val KEYCODE_BUTTON_Y = 100
    val KEYCODE_BUTTON_Z = 101
    val KEYCODE_C = 31
    val KEYCODE_CALCULATOR = 210
    val KEYCODE_CALENDAR = 208
    val KEYCODE_CALL = 5
    val KEYCODE_CAMERA = 27
    val KEYCODE_CAPS_LOCK = 115
    val KEYCODE_CAPTIONS = 175
    val KEYCODE_CHANNEL_DOWN = 167
    val KEYCODE_CHANNEL_UP = 166
    val KEYCODE_CLEAR = 28
    val KEYCODE_COMMA = 55
    val KEYCODE_CONTACTS = 207
    val KEYCODE_COPY = 278
    val KEYCODE_CTRL_LEFT = 113
    val KEYCODE_CTRL_RIGHT = 114
    val KEYCODE_CUT = 277
    val KEYCODE_D = 32
    val KEYCODE_DEL = 67
    val KEYCODE_DPAD_CENTER = 23
    val KEYCODE_DPAD_DOWN = 20
    val KEYCODE_DPAD_DOWN_LEFT = 269
    val KEYCODE_DPAD_DOWN_RIGHT = 271
    val KEYCODE_DPAD_LEFT = 21
    val KEYCODE_DPAD_RIGHT = 22
    val KEYCODE_DPAD_UP = 19
    val KEYCODE_DPAD_UP_LEFT = 268
    val KEYCODE_DPAD_UP_RIGHT = 270
    val KEYCODE_DVR = 173
    val KEYCODE_E = 33
    val KEYCODE_EISU = 212
    val KEYCODE_ENDCALL = 6
    val KEYCODE_ENTER = 66
    val KEYCODE_ENVELOPE = 65
    val KEYCODE_EQUALS = 70
    val KEYCODE_ESCAPE = 111
    val KEYCODE_EXPLORER = 64
    val KEYCODE_F = 34
    val KEYCODE_F1 = 131
    val KEYCODE_F10 = 140
    val KEYCODE_F11 = 141
    val KEYCODE_F12 = 142
    val KEYCODE_F2 = 132
    val KEYCODE_F3 = 133
    val KEYCODE_F4 = 134
    val KEYCODE_F5 = 135
    val KEYCODE_F6 = 136
    val KEYCODE_F7 = 137
    val KEYCODE_F8 = 138
    val KEYCODE_F9 = 139
    val KEYCODE_FOCUS = 80
    val KEYCODE_FORWARD = 125
    val KEYCODE_FORWARD_DEL = 112
    val KEYCODE_FUNCTION = 119
    val KEYCODE_G = 35
    val KEYCODE_GRAVE = 68
    val KEYCODE_GUIDE = 172
    val KEYCODE_H = 36
    val KEYCODE_HEADSETHOOK = 79
    val KEYCODE_HELP = 259
    val KEYCODE_HENKAN = 214
    val KEYCODE_HOME = 3
    val KEYCODE_I = 37
    val KEYCODE_INFO = 165
    val KEYCODE_INSERT = 124
    val KEYCODE_J = 38
    val KEYCODE_K = 39
    val KEYCODE_KANA = 218
    val KEYCODE_KATAKANA_HIRAGANA = 215
    val KEYCODE_L = 40
    val KEYCODE_LANGUAGE_SWITCH = 204
    val KEYCODE_LAST_CHANNEL = 229
    val KEYCODE_LEFT_BRACKET = 71
    val KEYCODE_M = 41
    val KEYCODE_MANNER_MODE = 205
    val KEYCODE_MEDIA_AUDIO_TRACK = 222
    val KEYCODE_MEDIA_CLOSE = 128
    val KEYCODE_MEDIA_EJECT = 129
    val KEYCODE_MEDIA_FAST_FORWARD = 90
    val KEYCODE_MEDIA_NEXT = 87
    val KEYCODE_MEDIA_PAUSE = 127
    val KEYCODE_MEDIA_PLAY = 126
    val KEYCODE_MEDIA_PLAY_PAUSE = 85
    val KEYCODE_MEDIA_PREVIOUS = 88
    val KEYCODE_MEDIA_RECORD = 130
    val KEYCODE_MEDIA_REWIND = 89
    val KEYCODE_MEDIA_SKIP_BACKWARD = 273
    val KEYCODE_MEDIA_SKIP_FORWARD = 272
    val KEYCODE_MEDIA_STEP_BACKWARD = 275
    val KEYCODE_MEDIA_STEP_FORWARD = 274
    val KEYCODE_MEDIA_STOP = 86
    val KEYCODE_MEDIA_TOP_MENU = 226
    val KEYCODE_MENU = 82
    val KEYCODE_META_LEFT = 117
    val KEYCODE_META_RIGHT = 118
    val KEYCODE_MINUS = 69
    val KEYCODE_MOVE_END = 123
    val KEYCODE_MOVE_HOME = 122
    val KEYCODE_MUHENKAN = 213
    val KEYCODE_MUSIC = 209
    val KEYCODE_MUTE = 91
    val KEYCODE_N = 42
    val KEYCODE_NAVIGATE_IN = 262
    val KEYCODE_NAVIGATE_NEXT = 261
    val KEYCODE_NAVIGATE_OUT = 263
    val KEYCODE_NAVIGATE_PREVIOUS = 260
    val KEYCODE_NOTIFICATION = 83
    val KEYCODE_NUM = 78
    val KEYCODE_NUMPAD_0 = 144
    val KEYCODE_NUMPAD_1 = 145
    val KEYCODE_NUMPAD_2 = 146
    val KEYCODE_NUMPAD_3 = 147
    val KEYCODE_NUMPAD_4 = 148
    val KEYCODE_NUMPAD_5 = 149
    val KEYCODE_NUMPAD_6 = 150
    val KEYCODE_NUMPAD_7 = 151
    val KEYCODE_NUMPAD_8 = 152
    val KEYCODE_NUMPAD_9 = 153
    val KEYCODE_NUMPAD_ADD = 157
    val KEYCODE_NUMPAD_COMMA = 159
    val KEYCODE_NUMPAD_DIVIDE = 154
    val KEYCODE_NUMPAD_DOT = 158
    val KEYCODE_NUMPAD_ENTER = 160
    val KEYCODE_NUMPAD_EQUALS = 161
    val KEYCODE_NUMPAD_LEFT_PAREN = 162
    val KEYCODE_NUMPAD_MULTIPLY = 155
    val KEYCODE_NUMPAD_RIGHT_PAREN = 163
    val KEYCODE_NUMPAD_SUBTRACT = 156
    val KEYCODE_NUM_LOCK = 143
    val KEYCODE_O = 43
    val KEYCODE_P = 44
    val KEYCODE_PAGE_DOWN = 93
    val KEYCODE_PAGE_UP = 92
    val KEYCODE_PAIRING = 225
    val KEYCODE_PASTE = 279
    val KEYCODE_PERIOD = 56
    val KEYCODE_PICTSYMBOLS = 94
    val KEYCODE_PLUS = 81
    val KEYCODE_POUND = 18
    val KEYCODE_POWER = 26
    val KEYCODE_PROG_BLUE = 186
    val KEYCODE_PROG_GREEN = 184
    val KEYCODE_PROG_RED = 183
    val KEYCODE_PROG_YELLOW = 185
    val KEYCODE_Q = 45
    val KEYCODE_R = 46
    val KEYCODE_REFRESH = 285
    val KEYCODE_RIGHT_BRACKET = 72
    val KEYCODE_RO = 217
    val KEYCODE_S = 47
    val KEYCODE_SCROLL_LOCK = 116
    val KEYCODE_SEARCH = 84
    val KEYCODE_SEMICOLON = 74
    val KEYCODE_SETTINGS = 176
    val KEYCODE_SHIFT_LEFT = 59
    val KEYCODE_SHIFT_RIGHT = 60
    val KEYCODE_SLASH = 76
    val KEYCODE_SLEEP = 223
    val KEYCODE_SOFT_LEFT = 1
    val KEYCODE_SOFT_RIGHT = 2
    val KEYCODE_SOFT_SLEEP = 276
    val KEYCODE_SPACE = 62
    val KEYCODE_STAR = 17
    val KEYCODE_STB_INPUT = 180
    val KEYCODE_STB_POWER = 179
    val KEYCODE_STEM_1 = 265
    val KEYCODE_STEM_2 = 266
    val KEYCODE_STEM_3 = 267
    val KEYCODE_STEM_PRIMARY = 264
    val KEYCODE_SWITCH_CHARSET = 95
    val KEYCODE_SYM = 63
    val KEYCODE_SYSRQ = 120
    val KEYCODE_SYSTEM_NAVIGATION_DOWN = 281
    val KEYCODE_SYSTEM_NAVIGATION_LEFT = 282
    val KEYCODE_SYSTEM_NAVIGATION_RIGHT = 283
    val KEYCODE_SYSTEM_NAVIGATION_UP = 280
    val KEYCODE_T = 48
    val KEYCODE_TAB = 61
    val KEYCODE_TV = 170
    val KEYCODE_TV_ANTENNA_CABLE = 242
    val KEYCODE_TV_AUDIO_DESCRIPTION = 252
    val KEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN = 254
    val KEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP = 253
    val KEYCODE_TV_CONTENTS_MENU = 256
    val KEYCODE_TV_DATA_SERVICE = 230
    val KEYCODE_TV_INPUT = 178
    val KEYCODE_TV_INPUT_COMPONENT_1 = 249
    val KEYCODE_TV_INPUT_COMPONENT_2 = 250
    val KEYCODE_TV_INPUT_COMPOSITE_1 = 247
    val KEYCODE_TV_INPUT_COMPOSITE_2 = 248
    val KEYCODE_TV_INPUT_HDMI_1 = 243
    val KEYCODE_TV_INPUT_HDMI_2 = 244
    val KEYCODE_TV_INPUT_HDMI_3 = 245
    val KEYCODE_TV_INPUT_HDMI_4 = 246
    val KEYCODE_TV_INPUT_VGA_1 = 251
    val KEYCODE_TV_MEDIA_CONTEXT_MENU = 257
    val KEYCODE_TV_NETWORK = 241
    val KEYCODE_TV_NUMBER_ENTRY = 234
    val KEYCODE_TV_POWER = 177
    val KEYCODE_TV_RADIO_SERVICE = 232
    val KEYCODE_TV_SATELLITE = 237
    val KEYCODE_TV_SATELLITE_BS = 238
    val KEYCODE_TV_SATELLITE_CS = 239
    val KEYCODE_TV_SATELLITE_SERVICE = 240
    val KEYCODE_TV_TELETEXT = 233
    val KEYCODE_TV_TERRESTRIAL_ANALOG = 235
    val KEYCODE_TV_TERRESTRIAL_DIGITAL = 236
    val KEYCODE_TV_TIMER_PROGRAMMING = 258
    val KEYCODE_TV_ZOOM_MODE = 255
    val KEYCODE_U = 49
    val KEYCODE_UNKNOWN = 0
    val KEYCODE_V = 50
    val KEYCODE_VOICE_ASSIST = 231
    val KEYCODE_VOLUME_DOWN = 25
    val KEYCODE_VOLUME_MUTE = 164
    val KEYCODE_VOLUME_UP = 24
    val KEYCODE_W = 51
    val KEYCODE_WAKEUP = 224
    val KEYCODE_WINDOW = 171
    val KEYCODE_X = 52
    val KEYCODE_Y = 53
    val KEYCODE_YEN = 216
    val KEYCODE_Z = 54
    val KEYCODE_ZENKAKU_HANKAKU = 211
    val KEYCODE_ZOOM_IN = 168
    val KEYCODE_ZOOM_OUT = 169

    val META_ALT_LEFT_ON = 16
    val META_ALT_MASK = 50
    val META_ALT_ON = 2
    val META_ALT_RIGHT_ON = 32
    val META_CAPS_LOCK_ON = 1048576
    val META_CTRL_LEFT_ON = 8192
    val META_CTRL_MASK = 28672
    val META_CTRL_ON = 4096
    val META_CTRL_RIGHT_ON = 16384
    val META_FUNCTION_ON = 8
    val META_META_LEFT_ON = 131072
    val META_META_MASK = 458752
    val META_META_ON = 65536
    val META_META_RIGHT_ON = 262144
    val META_NUM_LOCK_ON = 2097152
    val META_SCROLL_LOCK_ON = 4194304
    val META_SHIFT_LEFT_ON = 64
    val META_SHIFT_MASK = 193
    val META_SHIFT_ON = 1
    val META_SHIFT_RIGHT_ON = 128
    val META_SYM_ON = 4
}