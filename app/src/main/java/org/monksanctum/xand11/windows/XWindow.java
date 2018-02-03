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

package org.monksanctum.xand11.windows;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.util.SparseArray;

import org.monksanctum.xand11.Client;
import org.monksanctum.xand11.atoms.AtomManager;
import org.monksanctum.xand11.comm.Event;
import org.monksanctum.xand11.comm.Event.EventInfo;
import org.monksanctum.xand11.errors.AtomError;
import org.monksanctum.xand11.errors.WindowError;
import org.monksanctum.xand11.graphics.GraphicsContext;
import org.monksanctum.xand11.graphics.XDrawable;
import org.monksanctum.xand11.graphics.XPaintable;

import java.util.ArrayList;
import java.util.List;

import static org.monksanctum.xand11.windows.XWindowManager.DEBUG;

public class XWindow implements XDrawable {

    private static final String TAG = "XWindow";

    public static final byte INPUT_OUTPUT = 1;
    public static final byte INPUT_ONLY = 2;
    public static final byte COPY_FROM_PARENT = 3;

    public static final int BACKGROUND_NONE = 0;
    public static final int BACKGROUND_PARENT_RELATIVE = 1;

    public static final int BORDER_COPY_PARENT = 0;

    public static final int BACKING_NOT_USEFUL = 0;
    public static final int BACKING_WHEN_MAPPED = 1;
    public static final int BACKING_ALWAYS = 2;

    public static final int COLORMAP_COPY_PARENT = 0;

    public static final byte FLAG_MAPPED  = 0x01;
    public static final byte FLAG_VISIBLE = 0x02;

    public static final byte STACK_ABOVE = 0x00;
    public static final byte STACK_BELOW = 0x01;
    public static final byte STACK_TOP_IF = 0x02;
    public static final byte STACK_BOTTOM_IF = 0x03;
    public static final byte STACK_OPPOSITE = 0x04;

    private final SparseArray<Property> mProperties = new SparseArray<>();
    private final List<XWindow> mChildren = new ArrayList<>();
    private final List<EventCallback> mCallbacks = new ArrayList<>();
    private final XWindowManager mWindowManager;
    private XWindow parent;

    private final byte mClass;
    private int mId;

    private Rect mBounds;
    private int mBorderWidth;
    private XPaintable mBackground;
    private XPaintable mBorder;

    private Bitmap mBitmap;
    private Canvas mCanvas;

    private byte mBitGravity;
    private byte mWinGravity;
    private byte mBacking;
    private int mBackingPlanes;
    private int mBackingPixels;
    private boolean mOverrideRedirect;
    private boolean mSaveUnder;
    private int mDoNotPropogate;
    private int mColorMap;
    private int mCursor;

    private int mVisibility;
    private WindowCallback mCallback;
    private final Rect mInnerBounds = new Rect();
    private boolean mDrawingBackground;

    public XWindow(int id, int width, int height, byte windowClass, XWindowManager windowManager) {
        mId = id;
        mClass = windowClass;
        mWindowManager = windowManager;
        setBounds(new Rect(0, 0, width, height));
        initBackground();
    }

    public byte getWindowClass() {
        return mClass;
    }

    public Canvas lockCanvas(GraphicsContext gc) {
        // TODO: Track whether this is available.
        // TODO: Figure out a way to not back these and draw them directly into the view if possible
        if (!mDrawingBackground) {
            mCanvas.clipRect(mInnerBounds);
            mCanvas.translate(mBorderWidth, mBorderWidth);
        }
        mCanvas.save();
        if (gc != null) {
            gc.init(mCanvas);
        }
        return mCanvas;
    }

    @Override
    public void unlockCanvas() {
        mCanvas.restore();
        if (mCallback != null) {
            mCallback.onContentChanged();
        }
    }

    @Override
    public int getDepth() {
        return 32;
    }

    @Override
    public int getX() {
        return mInnerBounds.left;
    }

    @Override
    public int getY() {
        return mInnerBounds.top;
    }

    @Override
    public int getBorderWidth() {
        return mBorderWidth;
    }

    @Override
    public int getWidth() {
        return mInnerBounds.width();
    }

    @Override
    public int getHeight() {
        return mInnerBounds.height();
    }

    public Property getPropertyLocked(int atom, boolean delete) {
        Property property = mProperties.get(atom);
        if (delete) {
            mProperties.remove(atom);
            try (EventInfo info = Event.obtainInfo(Event.PROPERTY_NOTIFY, mId, atom, true)) {
                sendEvent(info, EVENT_MASK_PROPERTY_CHANGE);
            }
        }
        return property;
    }

    public void addPropertyLocked(int atom, Property property) {
        mProperties.put(atom, property);
    }

    public void removePropertyLocked(int atom) {
        mProperties.remove(atom);
    }

    public void notifyPropertyChanged(int atom) {
        if (DEBUG) {
            try {
                Log.d(TAG, "notifyPropertyChanged "
                        + AtomManager.getInstance().getString(atom));
            } catch (AtomError atomError) {
            }
        }
        try (EventInfo info = Event.obtainInfo(Event.PROPERTY_NOTIFY, mId, atom, false)) {
            sendEvent(info, EVENT_MASK_PROPERTY_CHANGE);
        }
    }

    public void addChildLocked(XWindow w) {
        if (DEBUG) Log.d(TAG, "addChild " + Integer.toHexString(w.mId) + " " + Integer.toHexString
                (mId));
        synchronized (w) {
            if (w.parent != null) {
                // TODO: Something other than runtime...
                throw new RuntimeException("Already child of " + w.parent);
            }
            mChildren.add(w);
            w.parent = this;
        }
        if (mCallback != null) {
            mCallback.onChildAdded(w);
        }
    }

    void removeChildLocked(XWindow w) {
        synchronized (w) {
            mChildren.remove(w);
            w.parent = null;
        }
        try (EventInfo info = Event.obtainInfo(Event.DESTROY_NOTIFY, mId, w.mId)) {
            sendEvent(info, EVENT_MASK_SUBSTRUCTURE_NOTIFY);
        }
        if (mCallback != null) {
            mCallback.onChildRemoved(w);
        }
    }

    public int getChildCountLocked() {
        return mChildren.size();
    }

    public XWindow getChildAtLocked(int i) {
        return mChildren.get(i);
    }

    public void destroyLocked() {
        // TODO: Free/remove anything.
        setVisibilityFlag(FLAG_MAPPED, (byte) 0);
        destroyChildrenLocked();
        if (parent != null) {
            synchronized (parent) {
                parent.removeChildLocked(this);
            }
        }
        try (EventInfo info = Event.obtainInfo(Event.DESTROY_NOTIFY, mId, mId)) {
            sendEvent(info, EVENT_MASK_STRUCTURE_NOTIFY);
        }
    }

    public void destroyChildrenLocked() {
        for (int i = mChildren.size() - 1; i >= 0; i--) {
            XWindow child = mChildren.get(i);
            synchronized (child) {
                child.destroyLocked();
            }
        }
    }

    public boolean setBorderWidth(int borderWidth) {
        if (mBorderWidth == borderWidth) return false;
        mBorderWidth = borderWidth;
        if (mInnerBounds != null) {
            setBounds(mInnerBounds);
        }
        return true;
    }

    public boolean setBounds(Rect rect) {
        if ((mInnerBounds.left == rect.left + mBorderWidth)
                && (mInnerBounds.top == rect.top + mBorderWidth)
                && (mInnerBounds.right == rect.right + mBorderWidth)
                && (mInnerBounds.bottom == rect.bottom + mBorderWidth)) return false;
        mInnerBounds.set(rect);
        mBounds = new Rect(mInnerBounds.left, mInnerBounds.top,
                mInnerBounds.right + 2 * mBorderWidth, mInnerBounds.bottom + 2 * mBorderWidth);
        mInnerBounds.offset(mBorderWidth, mBorderWidth);
        //if (mBounds.bottom == mBounds.top) mBounds.bottom = mBounds.top + 1;
        //if (mBounds.right == mBounds.left) mBounds.right = mBounds.left + 1;
        if (mBitmap != null && (mBounds.width() != mBitmap.getWidth()
                || mBounds.height() != mBitmap.getHeight())) {
            notifySizeChanged();
        }
        notifyLocationChanged();
        return true;
    }

    public Rect getBounds() {
        return new Rect(mInnerBounds);
    }

    private void notifyLocationChanged() {
        if (mCallback != null) {
            mCallback.onLocationChanged();
        }
    }

    private void notifySizeChanged() {
        if (mCallback != null) {
            mCallback.onSizeChanged();
        }
        if (mBitmap != null) {
            // TODO: Need to handle copying the content based on gravity.
            mBitmap = Bitmap.createBitmap(Math.max(mBounds.width(), 1),
                    Math.max(mBounds.height(), 1), Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            initBackground();
        }
    }

    public void setBackground(XPaintable background) {
        mBackground = background;
    }

    public void setBackgroundParent() {
        // TODO: Generate custom XPaintable here.
    }

    public void setBorder(XPaintable border) {
        mBorder = border;
    }

    public void initBackground() {
        if (mBitmap == null) {
            mBitmap = Bitmap.createBitmap(mBounds.width(), mBounds.height(), Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }
        if (mBorder != null) {
            mDrawingBackground = true;
            mBorder.draw(this, mBounds, null);
            mDrawingBackground = false;
        }
        if (mBackground != null) {
            mBackground.draw(this, mInnerBounds, null);
        }
    }

    public void clearArea(int x, int y, int width, int height) {
        if (mBackground != null) {
            mBackground.draw(this, new Rect(x, y, x + width, y + height), null);
        }
    }

    public XPaintable getBorder() {
        return mBorder;
    }

    public XWindow getParent() {
        return parent;
    }

    public void setBitGravity(byte bitGravity) {
        mBitGravity = bitGravity;
    }

    public void setWinGravity(byte winGravity) {
        mWinGravity = winGravity;
    }

    public void setBacking(byte backing) {
        mBacking = backing;
    }

    public void setBackingPlanes(int backingPlanes) {
        mBackingPlanes = backingPlanes;
    }

    public void setBackingPixels(int backingPixels) {
        mBackingPixels = backingPixels;
    }

    public void setOverrideRedirect(boolean overrideRedirect) {
        if (mOverrideRedirect == overrideRedirect) return;
        mOverrideRedirect = overrideRedirect;
    }

    public void setSaveUnder(boolean saveUnder) {
        mSaveUnder = saveUnder;
    }

    public void setEventMask(Client client, int eventMask) {
        ClientWindowCallback state = getOrAddState(client);
        state.setEventMask(eventMask);
        if (eventMask == 0) {
            removeCallback(state);
        }
        // TODO: Handle redirect restrictions
    }

    public void addCallback(EventCallback callback) {
        mCallbacks.add(callback);
    }

    public void removeCallback(EventCallback callback) {
        mCallbacks.remove(callback);
    }

    public int getEventMask(Client client) {
        ClientWindowCallback state = getOrAddState(client);
        return state.getMask();
    }

    public int getEventMask() {
        int mask = 0;
        for (int i = 0; i < mCallbacks.size(); i++) {
            mask |= mCallbacks.get(i).getMask();
        }
        return mask;
    }

    private ClientWindowCallback getOrAddState(Client client) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            EventCallback callback = mCallbacks.get(i);
            if (!(callback instanceof ClientWindowCallback)) continue;
            if (((ClientWindowCallback) callback).getClient() == client) {
                return (ClientWindowCallback) callback;
            }
        }
        ClientWindowCallback state = new ClientWindowCallback(client);
        mCallbacks.add(state);
        return state;
    }

    public void setDoNotPropagate(int doNotPropagate) {
        mDoNotPropogate = doNotPropagate;
    }

    public void setColorMap(int colorMap) {
        mColorMap = colorMap;
    }

    public int getColorMap() {
        return mColorMap;
    }

    public void setCursor(int cursor) {
        mCursor = cursor;
    }

    public void requestMap() {
        if (DEBUG) Log.d(TAG, "requestMap " + Integer.toHexString(mId));
        setVisibilityFlag(FLAG_MAPPED, FLAG_MAPPED);
    }

    public void setVisibily(boolean visible) {
        setVisibilityFlag(FLAG_VISIBLE, visible ? FLAG_VISIBLE : 0);
    }

    public void requestUnmap() {
        setVisibilityFlag(FLAG_MAPPED, (byte) 0);
    }

    public int getVisibility() {
        return mVisibility;
    }

    private void setVisibilityFlag(byte mask, byte value) {
        if ((mVisibility & mask) != value) {
            mVisibility = (mVisibility & ~mask) | value;
            if ((mask & FLAG_MAPPED) != 0) {
                if (parent != null) {
                    synchronized (parent) {
                        parent.notifyMappingChanged(this);
                    }
                }
                if ((value & FLAG_MAPPED) != 0) {
                    try (EventInfo info = Event.obtainInfo(Event.MAP_NOTIFY, mId, mId, false)) {
                        sendEvent(info, EVENT_MASK_STRUCTURE_NOTIFY);
                    }
                } else {
                    try (EventInfo info = Event.obtainInfo(Event.UNMAP_NOTIFY, mId, mId, false)) {
                        sendEvent(info, EVENT_MASK_STRUCTURE_NOTIFY);
                    }
                }
            }
            if ((mask & FLAG_VISIBLE) != 0) {
                if (mCallback != null) {
                    mCallback.onVisibilityChanged();
                }
                if ((value & FLAG_VISIBLE) != 0) {
                    try (EventInfo info = Event.obtainInfo(Event.EXPOSE, mId, mBounds, false)) {
                        sendEvent(info, EVENT_MASK_EXPOSE);
                    }
                }
            }
        }
    }

    public void notifyConfigureWindow() {
        try (EventInfo info = Event.obtainInfo(Event.CONFIGURE_NOTIFY, mId, mId,
                parent != null ? parent.getUnderSibling(this) : 0, mBorderWidth, mBounds,
                mOverrideRedirect)) {
            sendEvent(info, EVENT_MASK_STRUCTURE_NOTIFY);
        }
        if (parent != null) {
            synchronized (parent) {
                parent.notifyConfigureWindow(this);
            }
        }
    }

    private void notifyConfigureWindow(XWindow child) {
        try (EventInfo info = Event.obtainInfo(Event.CONFIGURE_NOTIFY, child.mId, mId,
                getUnderSibling(child), mBorderWidth, mBounds,
                mOverrideRedirect)) {
            sendEvent(info, EVENT_MASK_STRUCTURE_NOTIFY);
        }
    }

    private int getUnderSibling(XWindow child) {
        int index = mChildren.indexOf(child);
        if (index > 0) {
            return mChildren.get(index - 1).mId;
        }
        return 0;
    }

    public void requestSubwindowMap() {
        if (DEBUG) Log.d(TAG, "requestSubwindowMap " + Integer.toHexString(mId));
        for (int i = 0; i < mChildren.size(); i++) {
            mChildren.get(i).requestMap();
            mChildren.get(i).requestSubwindowMap();
        }
    }

    public void requestSubwindowUnmap() {
        if (DEBUG) Log.d(TAG, "requestSubwindowUnmap " + Integer.toHexString(mId));
        for (int i = 0; i < mChildren.size(); i++) {
            mChildren.get(i).requestUnmap();
            mChildren.get(i).requestSubwindowUnmap();
        }
    }

    private void notifyMappingChanged(XWindow child) {
        if ((child.getVisibility() & FLAG_MAPPED) != 0) {
            try (EventInfo info = Event.obtainInfo(Event.MAP_NOTIFY, mId, child.mId, false)) {
                sendEvent(info, EVENT_MASK_SUBSTRUCTURE_NOTIFY);
            }
        } else {
            try (EventInfo info = Event.obtainInfo(Event.UNMAP_NOTIFY, mId, child.mId, false)) {
                sendEvent(info, EVENT_MASK_SUBSTRUCTURE_NOTIFY);
            }
        }
        if (mCallback != null) {
            mCallback.onChildMappingChanged(child);
        }
    }

    public void setWindowCallback(WindowCallback windowCallback) {
        mCallback = windowCallback;
    }

    public int getId() {
        return mId;
    }

    public XPaintable getBackground() {
        return mBackground;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public byte getBitGravity() {
        return mBitGravity;
    }

    public byte getWinGravity() {
        return mWinGravity;
    }

    public byte getBacking() {
        return mBacking;
    }

    public int getBackingPlanes() {
        return mBackingPlanes;
    }

    public int getBackingPixels() {
        return mBackingPixels;
    }

    public boolean isOverrideRedirect() {
        return mOverrideRedirect;
    }

    public boolean isSaveUnder() {
        return mSaveUnder;
    }

    public int getDoNotPropogate() {
        return mDoNotPropogate;
    }

    public int getCursor() {
        return mCursor;
    }

    public void stackWindowLocked(XWindow window, XWindow sibling, int stackMode) throws
            WindowError {
        mChildren.remove(window);
        switch (stackMode) {
            case STACK_ABOVE:
                mChildren.add(mChildren.indexOf(sibling), window);
                break;
            case STACK_BELOW:
                mChildren.add(mChildren.indexOf(sibling) + 1, window);
                break;
            case STACK_TOP_IF:
            case STACK_BOTTOM_IF:
            case STACK_OPPOSITE:
                Log.d(TAG, "Unsupported stackWindow " + stackMode);
                throw new WindowError(stackMode);
        }
        if (mCallback != null) {
            mCallback.windowOrderChanged();
        }
    }

    public void stackWindowLocked(XWindow window, int stackMode) throws WindowError {
        mChildren.remove(window);
        switch (stackMode) {
            case STACK_ABOVE:
                mChildren.add(0, window);
                break;
            case STACK_BELOW:
                mChildren.add(window);
                break;
            case STACK_TOP_IF:
            case STACK_BOTTOM_IF:
            case STACK_OPPOSITE:
                Log.d(TAG, "Unsupported stackWindow " + stackMode);
                throw new WindowError(stackMode);
        }
        if (mCallback != null) {
            mCallback.windowOrderChanged();
        }
    }

    public void notifyKeyDown(int keyCode) {
        mWindowManager.getInputManager().onKeyDown(keyCode);
        try (EventInfo info = Event.obtainInfo(Event.KEY_PRESS, mId, getWidth() / 2,
                getHeight() / 2, mWindowManager.getInputManager().translate(keyCode),
                mWindowManager.getInputManager().getState())) {
            sendEvent(info, EVENT_MASK_KEY_PRESS);
        }
    }

    public void notifyKeyUp(int keyCode) {
        mWindowManager.getInputManager().onKeyUp(keyCode);
        try (EventInfo info = Event.obtainInfo(Event.KEY_RELEASE, mId, getWidth() / 2,
                getHeight() / 2, mWindowManager.getInputManager().translate(keyCode),
                mWindowManager.getInputManager().getState())) {
            sendEvent(info, EVENT_MASK_KEY_RELEASE);
        }
    }

    public void notifyEnter() {
        try (EventInfo info = Event.obtainInfo(Event.ENTER_NOTIFY, mId,
                getWidth() / 2, getHeight() / 2)) {
            sendEvent(info, EVENT_MASK_ENTER_WINDOW);
        }
    }

    public void notifyLeave() {
        try (EventInfo info = Event.obtainInfo(Event.LEAVE_NOTIFY, mId,
                getWidth() / 2, getHeight() / 2)) {
            sendEvent(info, EVENT_MASK_ENTER_WINDOW);
        }
    }

    public void sendSelectionRequest(int timestamp, int owner, int requester, int selection,
            int target, int property) {
        try (EventInfo info = Event.obtainInfo(Event.SELECTION_REQUEST, timestamp, owner, requester,
                selection, target, property)) {
            sendEvent(info, 0);
        }
    }

    public void sendSelectionNotify(int timestamp, int requestor, int selection, int target,
            int property) {
        try (EventInfo info = Event.obtainInfo(Event.SELECTION_NOTIFY, timestamp, requestor,
                selection, target, property)) {
            sendEvent(info, 0);
        }
    }

    public void sendSelectionClear(int timestamp, int owner, int sel) {
        try (EventInfo info = Event.obtainInfo(Event.SELECTION_CLEAR, timestamp, owner, sel)) {
            sendEvent(info, 0);
        }
    }

    public void notifyChildCreated(XWindow w) {
        try (EventInfo info = Event.obtainInfo(Event.CREATE_NOTIFY, mId, w.getId(),
                w.getX(), w.getY(), w.getWidth(), w.getHeight(), w.getBorderWidth(),
                w.mOverrideRedirect)) {
            sendEvent(info, EVENT_MASK_SUBSTRUCTURE_NOTIFY);
        }
    }

    private void sendEvent(EventInfo info, int mask) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            EventCallback callback = mCallbacks.get(i);
            if ((callback.getMask() & mask) != mask) {
                continue;
            }
            callback.onEvent(info);
        }
    }

    @Override
    public void read(Bitmap bitmap, int x, int y, int width, int height) {
        Canvas c = new Canvas(bitmap);
        c.drawBitmap(mBitmap, new Rect(x, y, width, height), new Rect(0, 0, width, height),
                new Paint());
    }

    public interface EventCallback {
        void onEvent(EventInfo info);

        int getMask();
    }

    public interface WindowCallback {
        void onVisibilityChanged();
        void onChildMappingChanged(XWindow child);
        void onLocationChanged();
        void onSizeChanged();
        void onContentChanged();
        void windowOrderChanged();

        void onChildAdded(XWindow w);
        void onChildRemoved(XWindow w);
    }

    public static final int EVENT_MASK_KEY_PRESS             =       0x1;
    public static final int EVENT_MASK_KEY_RELEASE           =       0x2;
    public static final int EVENT_MASK_BUTTON_PRESS          =       0x4;
    public static final int EVENT_MASK_BUTTON_RELEASE        =       0x8;
    public static final int EVENT_MASK_ENTER_WINDOW          =      0x10;
    public static final int EVENT_MASK_LEAVE_WINDOW          =      0x20;
    public static final int EVENT_MASK_POINTER_MOTION        =      0x40;
    public static final int EVENT_MASK_POINTER_MOTION_HINT   =      0x80;
    public static final int EVENT_MASK_BUTTON_1_MOTION       =     0x100;
    public static final int EVENT_MASK_BUTTON_2_MOTION       =     0x200;
    public static final int EVENT_MASK_BUTTON_3_MOTION       =     0x400;
    public static final int EVENT_MASK_BUTTON_4_MOTION       =     0x800;
    public static final int EVENT_MASK_BUTTON_5_MOTION       =    0x1000;
    public static final int EVENT_MASK_BUTTON_MOTION         =    0x2000;
    public static final int EVENT_MASK_KEYMAP_STATE          =    0x4000;
    public static final int EVENT_MASK_EXPOSE                =    0x8000;
    public static final int EVENT_MASK_VISIBILITY_CHANGE     =   0x10000;
    public static final int EVENT_MASK_STRUCTURE_NOTIFY      =   0x20000;
    public static final int EVENT_MASK_RESIZE_REDIRECT       =   0x40000;
    public static final int EVENT_MASK_SUBSTRUCTURE_NOTIFY   =   0x80000;
    public static final int EVENT_MASK_SUBSTRUCTURE_REDIRECT =  0x100000;
    public static final int EVENT_MASK_FOCUS_CHANGE          =  0x200000;
    public static final int EVENT_MASK_PROPERTY_CHANGE       =  0x400000;
    public static final int EVENT_MASK_COLORMAP_CHANGE       =  0x800000;
    public static final int EVENT_MASK_OWNER_GRAB_BUTTON     = 0x1000000;

    public static class Property {
        public static final int TYPE_NONE = 0;

        public static final int FORMAT_NONE = 0;
        public static final byte FORMAT_N = 8;
        public static final byte FORMAT_N_2 = 16;
        public static final byte FORMAT_N_4 = 32;

        public static final byte MODE_REPLACE = 0;
        public static final byte MODE_PREPEND = 1;
        public static final byte MODE_APPEND = 2;

        public final int key;
        public int typeAtom = TYPE_NONE;
        public byte[] value;
        public byte format = FORMAT_NONE;

        public Property(int key) {
            this.key = key;
        }

        public void change(byte mode, byte[] bytes) {
            switch (mode) {
                case MODE_REPLACE:
                    this.value = bytes;
                    break;
                case MODE_APPEND:
                    byte[] append = new byte[value.length + bytes.length];
                    for (int i = 0; i < value.length; i++) {
                        append[i] = value[i];
                    }
                    for (int i = 0; i < bytes.length; i++) {
                        append[i + value.length] = bytes[i];
                    }
                    value = append;
                    break;
                case MODE_PREPEND:
                    byte[] prepend = new byte[value.length + bytes.length];
                    for (int i = 0; i < bytes.length; i++) {
                        prepend[i] = bytes[i];
                    }
                    for (int i = 0; i < value.length; i++) {
                        prepend[i + bytes.length] = value[i];
                    }
                    value = prepend;
                    break;
            }
        }
    }
}
