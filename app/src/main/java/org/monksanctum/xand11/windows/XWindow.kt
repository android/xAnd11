// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.monksanctum.xand11.windows

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.util.SparseArray
import org.monksanctum.xand11.Client
import org.monksanctum.xand11.Utils
import org.monksanctum.xand11.atoms.AtomManager
import org.monksanctum.xand11.comm.Event
import org.monksanctum.xand11.comm.Event.EventInfo
import org.monksanctum.xand11.errors.AtomError
import org.monksanctum.xand11.errors.WindowError
import org.monksanctum.xand11.graphics.GraphicsContext
import org.monksanctum.xand11.graphics.XDrawable
import org.monksanctum.xand11.graphics.XPaintable
import org.monksanctum.xand11.windows.XWindowManager.Companion.DEBUG
import java.util.*

class XWindow(override val id: Int, width: Int, height: Int, val windowClass: Byte, private val mWindowManager: XWindowManager) : XDrawable {

    private val mProperties = SparseArray<Property>()
    private val mChildren = ArrayList<XWindow>()
    private val mCallbacks = ArrayList<EventCallback>()

    override var parent: XWindow? = null
        private set

    private val mBounds = Rect()
    override var borderWidth: Int = 0
        private set
    var background: XPaintable? = null
        set(background) {
            field = background
            initBackground()
        }
    var border: XPaintable? = null
        set(border) {
            field = border
            initBackground()
        }

    var bitmap: Bitmap? = null
        private set
    private var mCanvas: Canvas? = null

    var bitGravity: Byte = 0
    var winGravity: Byte = 0
    var backing: Byte = 0
    var backingPlanes: Int = 0
    var backingPixels: Int = 0
    var isOverrideRedirect: Boolean = false
        set(overrideRedirect) {
            if (isOverrideRedirect == overrideRedirect) return
            field = overrideRedirect
        }
    var isSaveUnder: Boolean = false
    var doNotPropogate: Int = 0
        private set
    var colorMap: Int = 0
    var cursor: Int = 0

    var visibility: Int = 0
        private set
    private var mCallback: WindowCallback? = null
    val innerBounds = Rect()
    private var mDrawingBackground: Boolean = false

    override val depth: Int
        get() = 32

    override val x: Int
        get() = mBounds.left

    override val y: Int
        get() = mBounds.top

    override val width: Int
        get() = mBounds.width()

    override val height: Int
        get() = mBounds.height()

    val childCountLocked: Int
        get() = mChildren.size

    val bounds: Rect
        get() = Rect(mBounds)

    val eventMask: Int
        get() {
            var mask = 0
            for (i in mCallbacks.indices) {
                mask = mask or mCallbacks[i].mask
            }
            return mask
        }

    init {
        setBounds(Rect(0, 0, width, height))
        initBackground()
    }

    override fun lockCanvas(gc: GraphicsContext?): Canvas {
        // TODO: Track whether this is available.
        // TODO: Figure out a way to not back these and draw them directly into the view if possible
        mCanvas!!.save()
        if (!mDrawingBackground) {
            mCanvas!!.clipRect(innerBounds)
            //mCanvas.translate(mBorderWidth, mBorderWidth);
        }
        gc?.init(mCanvas!!)
        return mCanvas!!
    }

    override fun unlockCanvas() {
        mCanvas!!.restore()
        if (mCallback != null) {
            mCallback!!.onContentChanged()
        }
    }

    fun getPropertyLocked(atom: Int, delete: Boolean): Property? {
        val property = mProperties.get(atom)
        if (delete) {
            mProperties.remove(atom)
            Event.obtainInfo(Event.PROPERTY_NOTIFY, id, atom, true).use { info -> sendEvent(info, EVENT_MASK_PROPERTY_CHANGE) }
        }
        return property
    }

    fun addPropertyLocked(atom: Int, property: Property) {
        mProperties.put(atom, property)
    }

    fun removePropertyLocked(atom: Int) {
        mProperties.remove(atom)
    }

    fun notifyPropertyChanged(atom: Int) {
        if (DEBUG) {
            try {
                Log.d(TAG, "notifyPropertyChanged " + AtomManager.instance.getString(atom))
            } catch (atomError: AtomError) {
            }

        }
        Event.obtainInfo(Event.PROPERTY_NOTIFY, id, atom, false).use { info -> sendEvent(info, EVENT_MASK_PROPERTY_CHANGE) }
    }

    fun addChildLocked(w: XWindow) {
        if (DEBUG)
            Log.d(TAG, "addChild " + Integer.toHexString(w.id) + " " + Integer.toHexString(id))
        synchronized(w) {
            if (w.parent != null) {
                // TODO: Something other than runtime...
                throw RuntimeException("Already child of " + w.parent!!)
            }
            mChildren.add(w)
            w.parent = this
        }
        if (mCallback != null) {
            mCallback!!.onChildAdded(w)
        }
    }

    internal fun removeChildLocked(w: XWindow) {
        synchronized(w) {
            mChildren.remove(w)
            w.parent = null
        }
        Event.obtainInfo(Event.DESTROY_NOTIFY, id, w.id).use { info -> sendEvent(info, EVENT_MASK_SUBSTRUCTURE_NOTIFY) }
        if (mCallback != null) {
            mCallback!!.onChildRemoved(w)
        }
    }

    fun getChildAtLocked(i: Int): XWindow {
        return mChildren[i]
    }

    fun destroyLocked() {
        // TODO: Free/remove anything.
        setVisibilityFlag(FLAG_MAPPED, 0.toByte())
        destroyChildrenLocked()
        parent?.let {
            synchronized(it) {
                it.removeChildLocked(this)
            }
        }
        Event.obtainInfo(Event.DESTROY_NOTIFY, id, id).use { info -> sendEvent(info, EVENT_MASK_STRUCTURE_NOTIFY) }
    }

    fun destroyChildrenLocked() {
        for (i in mChildren.indices.reversed()) {
            val child = mChildren[i]
            synchronized(child) {
                child.destroyLocked()
            }
        }
    }

    fun setBorderWidth(borderWidth: Int): Boolean {
        if (this.borderWidth == borderWidth) return false
        this.borderWidth = borderWidth
        innerBounds.left = this.borderWidth
        innerBounds.top = this.borderWidth
        innerBounds.right = mBounds.width() - this.borderWidth
        innerBounds.bottom = mBounds.height() - this.borderWidth
        initBackground()
        return true
    }

    fun setBounds(rect: Rect): Boolean {
        if (mBounds == rect) return false
        mBounds.set(rect)
        innerBounds.left = borderWidth
        innerBounds.top = borderWidth
        innerBounds.right = mBounds.width() - borderWidth
        innerBounds.bottom = mBounds.height() - borderWidth
        if (bitmap != null && (mBounds.width() != bitmap!!.width || mBounds.height() != bitmap!!.height)) {
            notifySizeChanged()
        }
        notifyLocationChanged()
        return true
    }

    private fun notifyLocationChanged() {
        if (mCallback != null) {
            mCallback!!.onLocationChanged()
        }
    }

    private fun notifySizeChanged() {
        if (mCallback != null) {
            mCallback!!.onSizeChanged()
        }
        val oldBitmap = bitmap
        // TODO: Need to handle copying the content based on gravity.
        bitmap = Bitmap.createBitmap(Math.max(mBounds.width(), 1),
                Math.max(mBounds.height(), 1), Config.ARGB_8888)
        mCanvas = Canvas(bitmap!!)
        initBackground()
        val p = Paint()
        when (winGravity) {
            GRAVITY_UNMAP -> requestUnmap()
            GRAVITY_NORTHWEST -> mCanvas!!.drawBitmap(oldBitmap!!, 0f, 0f, p)
            GRAVITY_NORTH -> mCanvas!!.drawBitmap(oldBitmap!!, ((bitmap!!.width - oldBitmap.width) / 2).toFloat(),
                    0f, p)
            GRAVITY_NORTHEAST -> mCanvas!!.drawBitmap(oldBitmap!!, (bitmap!!.width - oldBitmap.width).toFloat(), 0f, p)
            GRAVITY_WEST -> mCanvas!!.drawBitmap(oldBitmap!!, 0f, ((bitmap!!.height - oldBitmap.height) / 2).toFloat(),
                    p)
            GRAVITY_CENTER -> mCanvas!!.drawBitmap(oldBitmap!!, ((bitmap!!.width - oldBitmap.width) / 2).toFloat(),
                    ((bitmap!!.height - oldBitmap.height) / 2).toFloat(), p)
            GRAVITY_EAST -> mCanvas!!.drawBitmap(oldBitmap!!, (bitmap!!.width - oldBitmap.width).toFloat(),
                    ((bitmap!!.height - oldBitmap.height) / 2).toFloat(), p)
            GRAVITY_SOUTHWEST -> mCanvas!!.drawBitmap(oldBitmap!!, 0f, (bitmap!!.height - oldBitmap.height).toFloat(),
                    p)
            GRAVITY_SOUTH -> mCanvas!!.drawBitmap(oldBitmap!!, ((bitmap!!.width - oldBitmap.width) / 2).toFloat(),
                    (bitmap!!.height - oldBitmap.height).toFloat(), p)
            GRAVITY_SOUTHEAST -> mCanvas!!.drawBitmap(oldBitmap!!, (bitmap!!.width - oldBitmap.width).toFloat(),
                    (bitmap!!.height - oldBitmap.height).toFloat(), p)
            GRAVITY_STATIC -> throw RuntimeException("Static gravity unsupported")
        }
    }

    fun setBackgroundParent() {
        if (parent == null) {
            throw RuntimeException("Unsupported parent")
        } else {
            background = parent!!.background
        }
    }

    fun initBackground() {
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(mBounds.width(), mBounds.height(), Config.ARGB_8888)
            mCanvas = Canvas(bitmap!!)
        }
        val r = Rect(0, 0, mBounds.width(), mBounds.height())
        synchronized(this) {
            mDrawingBackground = true
            if (border != null) {
                border!!.draw(this, r, null)
            }
            mDrawingBackground = false
            if (background != null) {
                background!!.draw(this, r, null)
            }
        }
    }

    fun clearArea(x: Int, y: Int, width: Int, height: Int) {
        if (background != null) {
            background!!.draw(this, Rect(x, y, x + width, y + height), null)
        }
    }

    fun setEventMask(client: Client, eventMask: Int) {
        val state = getOrAddState(client)
        state.setEventMask(eventMask)
        if (eventMask == 0) {
            removeCallback(state)
        }
        // TODO: Handle redirect restrictions
    }

    fun addCallback(callback: EventCallback) {
        mCallbacks.add(callback)
    }

    fun removeCallback(callback: EventCallback) {
        mCallbacks.remove(callback)
    }

    fun getEventMask(client: Client): Int {
        val state = getOrAddState(client)
        return state.mask
    }

    private fun getOrAddState(client: Client): ClientWindowCallback {
        for (i in mCallbacks.indices) {
            val callback = mCallbacks[i] as? ClientWindowCallback ?: continue
            if (callback.client === client) {
                return callback
            }
        }
        val state = ClientWindowCallback(client)
        mCallbacks.add(state)
        return state
    }

    fun setDoNotPropagate(doNotPropagate: Int) {
        doNotPropogate = doNotPropagate
    }

    fun requestMap() {
        if (DEBUG) Log.d(TAG, "requestMap " + Integer.toHexString(id))
        setVisibilityFlag(FLAG_MAPPED, FLAG_MAPPED)
    }

    fun setVisibily(visible: Boolean) {
        setVisibilityFlag(FLAG_VISIBLE, if (visible) FLAG_VISIBLE else 0)
    }

    fun requestUnmap() {
        setVisibilityFlag(FLAG_MAPPED, 0.toByte())
    }

    private fun setVisibilityFlag(mask: Byte, value: Byte) {
        if (visibility and mask.toInt() != value.toInt()) {
            visibility = visibility and mask.toInt().inv() or value.toInt()
            if (mask.toInt() and FLAG_MAPPED.toInt() != 0) {
                parent?.let {
                    synchronized(it) {
                        it.notifyMappingChanged(this)
                    }
                }
                if (value.toInt() and FLAG_MAPPED.toInt() != 0) {
                    Event.obtainInfo(Event.MAP_NOTIFY, id, id, false).use { info -> sendEvent(info, EVENT_MASK_STRUCTURE_NOTIFY) }
                } else {
                    Event.obtainInfo(Event.UNMAP_NOTIFY, id, id, false).use { info -> sendEvent(info, EVENT_MASK_STRUCTURE_NOTIFY) }
                }
            }
            if (mask.toInt() and FLAG_VISIBLE.toInt() != 0) {
                if (mCallback != null) {
                    mCallback!!.onVisibilityChanged()
                }
                if (value.toInt() and FLAG_VISIBLE.toInt() != 0) {
                    Event.obtainInfo(Event.EXPOSE, id, mBounds, false).use { info -> sendEvent(info, EVENT_MASK_EXPOSE) }
                }
            }
        }
    }

    fun notifyConfigureWindow() {
        Event.obtainInfo(Event.CONFIGURE_NOTIFY, id, id,
                if (parent != null) parent!!.getUnderSibling(this) else 0, borderWidth, mBounds,
                isOverrideRedirect).use { info -> sendEvent(info, EVENT_MASK_STRUCTURE_NOTIFY) }
        parent?.let {
            Utils.sBgHandler.post {
                synchronized(it) {
                    it.notifyConfigureWindow(this)
                }
            }
        }
    }

    private fun notifyConfigureWindow(child: XWindow) {
        Event.obtainInfo(Event.CONFIGURE_NOTIFY, child.id, id,
                getUnderSibling(child), borderWidth, mBounds,
                isOverrideRedirect).use { info -> sendEvent(info, EVENT_MASK_STRUCTURE_NOTIFY) }
    }

    private fun getUnderSibling(child: XWindow): Int {
        val index = mChildren.indexOf(child)
        return if (index > 0) {
            mChildren[index - 1].id
        } else 0
    }

    fun requestSubwindowMap() {
        if (DEBUG) Log.d(TAG, "requestSubwindowMap " + Integer.toHexString(id))
        for (i in mChildren.indices) {
            mChildren[i].requestMap()
            mChildren[i].requestSubwindowMap()
        }
    }

    fun requestSubwindowUnmap() {
        if (DEBUG) Log.d(TAG, "requestSubwindowUnmap " + Integer.toHexString(id))
        for (i in mChildren.indices) {
            mChildren[i].requestUnmap()
            mChildren[i].requestSubwindowUnmap()
        }
    }

    private fun notifyMappingChanged(child: XWindow) {
        if (child.visibility and FLAG_MAPPED.toInt() != 0) {
            Event.obtainInfo(Event.MAP_NOTIFY, id, child.id, false).use { info -> sendEvent(info, EVENT_MASK_SUBSTRUCTURE_NOTIFY) }
        } else {
            Event.obtainInfo(Event.UNMAP_NOTIFY, id, child.id, false).use { info -> sendEvent(info, EVENT_MASK_SUBSTRUCTURE_NOTIFY) }
        }
        if (mCallback != null) {
            mCallback!!.onChildMappingChanged(child)
        }
    }

    fun setWindowCallback(windowCallback: WindowCallback) {
        mCallback = windowCallback
    }

    @Throws(WindowError::class)
    fun stackWindowLocked(window: XWindow, sibling: XWindow, stackMode: Int) {
        mChildren.remove(window)
        when (stackMode.toByte()) {
            STACK_ABOVE -> mChildren.add(mChildren.indexOf(sibling), window)
            STACK_BELOW -> mChildren.add(mChildren.indexOf(sibling) + 1, window)
            STACK_TOP_IF, STACK_BOTTOM_IF, STACK_OPPOSITE -> {
                Log.d(TAG, "Unsupported stackWindow $stackMode")
                throw WindowError(stackMode)
            }
        }
        if (mCallback != null) {
            mCallback!!.windowOrderChanged()
        }
    }

    @Throws(WindowError::class)
    fun stackWindowLocked(window: XWindow, stackMode: Int) {
        mChildren.remove(window)
        when (stackMode.toByte()) {
            STACK_ABOVE -> mChildren.add(0, window)
            STACK_BELOW -> mChildren.add(window)
            STACK_TOP_IF, STACK_BOTTOM_IF, STACK_OPPOSITE -> {
                Log.d(TAG, "Unsupported stackWindow $stackMode")
                throw WindowError(stackMode)
            }
        }
        if (mCallback != null) {
            mCallback!!.windowOrderChanged()
        }
    }

    fun notifyKeyDown(keyCode: Int) {
        mWindowManager.inputManager.onKeyDown(keyCode)
        Event.obtainInfo(Event.KEY_PRESS, id, width / 2,
                height / 2, mWindowManager.inputManager.translate(keyCode),
                mWindowManager.inputManager.state).use { info -> sendEvent(info, EVENT_MASK_KEY_PRESS) }
    }

    fun notifyKeyUp(keyCode: Int) {
        mWindowManager.inputManager.onKeyUp(keyCode)
        Event.obtainInfo(Event.KEY_RELEASE, id, width / 2,
                height / 2, mWindowManager.inputManager.translate(keyCode),
                mWindowManager.inputManager.state).use { info -> sendEvent(info, EVENT_MASK_KEY_RELEASE) }
    }

    fun notifyEnter() {
        Event.obtainInfo(Event.ENTER_NOTIFY, id,
                width / 2, height / 2).use { info -> sendEvent(info, EVENT_MASK_ENTER_WINDOW) }
    }

    fun notifyLeave() {
        Event.obtainInfo(Event.LEAVE_NOTIFY, id,
                width / 2, height / 2).use { info -> sendEvent(info, EVENT_MASK_ENTER_WINDOW) }
    }

    fun sendSelectionRequest(timestamp: Int, owner: Int, requester: Int, selection: Int,
                             target: Int, property: Int) {
        Event.obtainInfo(Event.SELECTION_REQUEST, timestamp, owner, requester,
                selection, target, property).use { info -> sendEvent(info, 0) }
    }

    fun sendSelectionNotify(timestamp: Int, requestor: Int, selection: Int, target: Int,
                            property: Int) {
        Event.obtainInfo(Event.SELECTION_NOTIFY, timestamp, requestor,
                selection, target, property).use { info -> sendEvent(info, 0) }
    }

    fun sendSelectionClear(timestamp: Int, owner: Int, sel: Int) {
        Event.obtainInfo(Event.SELECTION_CLEAR, timestamp, owner, sel).use { info -> sendEvent(info, 0) }
    }

    fun notifyChildCreated(w: XWindow) {
        Event.obtainInfo(Event.CREATE_NOTIFY, id, w.id,
                w.x, w.y, w.width, w.height, w.borderWidth,
                w.isOverrideRedirect).use { info -> sendEvent(info, EVENT_MASK_SUBSTRUCTURE_NOTIFY) }
    }

    private fun sendEvent(info: EventInfo, mask: Int) {
        for (i in mCallbacks.indices) {
            val callback = mCallbacks[i]
            if (callback.mask and mask != mask) {
                continue
            }
            callback.onEvent(info)
        }
    }

    override fun read(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int) {
        val c = Canvas(bitmap)
        c.drawBitmap(this.bitmap!!, Rect(x, y, width, height), Rect(0, 0, width, height),
                Paint())
    }

    interface EventCallback {

        val mask: Int
        fun onEvent(info: EventInfo)
    }

    interface WindowCallback {
        fun onVisibilityChanged()
        fun onChildMappingChanged(child: XWindow)
        fun onLocationChanged()
        fun onSizeChanged()
        fun onContentChanged()
        fun windowOrderChanged()

        fun onChildAdded(w: XWindow)
        fun onChildRemoved(w: XWindow)
    }

    class Property(val key: Int) {
        var typeAtom = TYPE_NONE
        var value: ByteArray = byteArrayOf()
        var format = FORMAT_NONE.toByte()

        fun change(mode: Byte, bytes: ByteArray) {
            when (mode) {
                MODE_REPLACE -> this.value = bytes
                MODE_APPEND -> {
                    val append = ByteArray(value.size + bytes.size)
                    for (i in value.indices) {
                        append[i] = value[i]
                    }
                    for (i in bytes.indices) {
                        append[i + value.size] = bytes[i]
                    }
                    value = append
                }
                MODE_PREPEND -> {
                    val prepend = ByteArray(value.size + bytes.size)
                    for (i in bytes.indices) {
                        prepend[i] = bytes[i]
                    }
                    for (i in value.indices) {
                        prepend[i + bytes.size] = value[i]
                    }
                    value = prepend
                }
            }
        }

        companion object {
            val TYPE_NONE = 0

            val FORMAT_NONE = 0
            val FORMAT_N: Byte = 8
            val FORMAT_N_2: Byte = 16
            val FORMAT_N_4: Byte = 32

            val MODE_REPLACE: Byte = 0
            val MODE_PREPEND: Byte = 1
            val MODE_APPEND: Byte = 2
        }
    }

    companion object {

        private val TAG = "XWindow"

        val INPUT_OUTPUT: Byte = 1
        val INPUT_ONLY: Byte = 2
        val COPY_FROM_PARENT: Byte = 3

        val BACKGROUND_NONE = 0
        val BACKGROUND_PARENT_RELATIVE = 1

        val BORDER_COPY_PARENT = 0

        val BACKING_NOT_USEFUL = 0
        val BACKING_WHEN_MAPPED = 1
        val BACKING_ALWAYS = 2

        val COLORMAP_COPY_PARENT = 0

        val FLAG_MAPPED: Byte = 0x01
        val FLAG_VISIBLE: Byte = 0x02

        val STACK_ABOVE: Byte = 0x00
        val STACK_BELOW: Byte = 0x01
        val STACK_TOP_IF: Byte = 0x02
        val STACK_BOTTOM_IF: Byte = 0x03
        val STACK_OPPOSITE: Byte = 0x04

        val GRAVITY_UNMAP: Byte = 0x00
        val GRAVITY_NORTHWEST: Byte = 0x01
        val GRAVITY_NORTH: Byte = 0x02
        val GRAVITY_NORTHEAST: Byte = 0x03
        val GRAVITY_WEST: Byte = 0x04
        val GRAVITY_CENTER: Byte = 0x05
        val GRAVITY_EAST: Byte = 0x06
        val GRAVITY_SOUTHWEST: Byte = 0x07
        val GRAVITY_SOUTH: Byte = 0x08
        val GRAVITY_SOUTHEAST: Byte = 0x09
        val GRAVITY_STATIC: Byte = 0x0A

        val EVENT_MASK_KEY_PRESS = 0x1
        val EVENT_MASK_KEY_RELEASE = 0x2
        val EVENT_MASK_BUTTON_PRESS = 0x4
        val EVENT_MASK_BUTTON_RELEASE = 0x8
        val EVENT_MASK_ENTER_WINDOW = 0x10
        val EVENT_MASK_LEAVE_WINDOW = 0x20
        val EVENT_MASK_POINTER_MOTION = 0x40
        val EVENT_MASK_POINTER_MOTION_HINT = 0x80
        val EVENT_MASK_BUTTON_1_MOTION = 0x100
        val EVENT_MASK_BUTTON_2_MOTION = 0x200
        val EVENT_MASK_BUTTON_3_MOTION = 0x400
        val EVENT_MASK_BUTTON_4_MOTION = 0x800
        val EVENT_MASK_BUTTON_5_MOTION = 0x1000
        val EVENT_MASK_BUTTON_MOTION = 0x2000
        val EVENT_MASK_KEYMAP_STATE = 0x4000
        val EVENT_MASK_EXPOSE = 0x8000
        val EVENT_MASK_VISIBILITY_CHANGE = 0x10000
        val EVENT_MASK_STRUCTURE_NOTIFY = 0x20000
        val EVENT_MASK_RESIZE_REDIRECT = 0x40000
        val EVENT_MASK_SUBSTRUCTURE_NOTIFY = 0x80000
        val EVENT_MASK_SUBSTRUCTURE_REDIRECT = 0x100000
        val EVENT_MASK_FOCUS_CHANGE = 0x200000
        val EVENT_MASK_PROPERTY_CHANGE = 0x400000
        val EVENT_MASK_COLORMAP_CHANGE = 0x800000
        val EVENT_MASK_OWNER_GRAB_BUTTON = 0x1000000
    }
}
