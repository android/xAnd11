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

package org.monksanctum.xand11.graphics;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import org.monksanctum.xand11.errors.ValueError;

import static org.monksanctum.xand11.graphics.GraphicsManager.DEBUG;

public class Pixmap implements XDrawable, XPaintable {

    private static final String TAG = "Pixmap";
    public static final SparseArray<Config> mConfigs = new SparseArray<>();

    static {
        // These all make sense.
        mConfigs.put(12, Config.ARGB_4444);
        mConfigs.put(24, Config.ARGB_8888);
        mConfigs.put(32, Config.ARGB_8888);
        mConfigs.put(16, Config.RGB_565);
        // These need better handling somehow.
        mConfigs.put(1, Config.ALPHA_8);
        mConfigs.put(4, Config.ALPHA_8);
        mConfigs.put(8, Config.ALPHA_8);
    }

    private final byte mDepth;
    private final Bitmap mBitmap;
    private final Canvas mCanvas;
    private final int mId;
    private final XDrawable mParent;

    public Pixmap(byte depth, int width, int height, int id, XDrawable parent) throws ValueError {
        if (mConfigs.indexOfKey(depth) < 0) {
            throw new ValueError(depth);
        }
        if (width == 0 || height == 0) {
            throw new ValueError(0);
        }
        mId = id;
        mParent = parent;
        mDepth = depth;
        mBitmap = Bitmap.createBitmap(width, height, mConfigs.get(mDepth));
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    public void draw(XDrawable drawable, Rect bounds, @Nullable GraphicsContext context) {
        synchronized (drawable) {
            Canvas c = drawable.lockCanvas();
            Paint paint = context != null ? context.getPaint() : new Paint();
            Rect src = new Rect(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
            if (DEBUG) Log.d(TAG, "Drawing pixmap " + bounds);
            c.drawBitmap(mBitmap, src, bounds, paint);
            drawable.unlockCanvas();
        }
    }

    @Override
    public Canvas lockCanvas() {
        return mCanvas;
    }

    @Override
    public void unlockCanvas() {
        // Don't care?
    }

    @Override
    public XDrawable getParent() {
        return mParent;
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public int getY() {
        return 0;
    }

    @Override
    public int getBorderWidth() {
        return 0;
    }

    @Override
    public int getWidth() {
        return mBitmap.getWidth();
    }

    @Override
    public int getHeight() {
        return mBitmap.getHeight();
    }

    public int getDepth() {
        return mDepth;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }
}
