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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.Log;

import static org.monksanctum.xand11.graphics.GraphicsManager.DEBUG;

public class ColorPaintable implements XPaintable {

    private static final String TAG = "ColorPaintable";
    private final int mColor;

    public ColorPaintable(int color) {
        mColor = color;
    }

    @Override
    public void draw(XDrawable drawable, Rect bounds, @Nullable GraphicsContext context) {
        synchronized (drawable) {
            Canvas canvas = drawable.lockCanvas();
            Paint p = context != null ? new Paint(context.getPaint()) : new Paint();
            p.setColor(mColor);
            if (DEBUG) Log.d(TAG, "Drawing 0x" + Integer.toHexString(mColor) + " on " + bounds);
            canvas.drawRect(bounds, p);
            drawable.unlockCanvas();
        }
    }
}
