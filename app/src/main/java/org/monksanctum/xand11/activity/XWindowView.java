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

package org.monksanctum.xand11.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.monksanctum.xand11.Time;
import org.monksanctum.xand11.windows.XWindow;
import org.monksanctum.xand11.windows.XWindow.WindowCallback;

import static org.monksanctum.xand11.Time.t;

public class XWindowView extends ViewGroup implements WindowCallback {

    private static final String TAG = "XWindowView";
    protected final XWindow mWindow;

    public XWindowView(Context context, XWindow window) {
        super(context);
        mWindow = window;
        synchronized (mWindow) {
            for (int i = 0; i < mWindow.getChildCountLocked(); i++) {
                addView(new XWindowView(context, mWindow.getChildAtLocked(i)));
            }
        }
        mWindow.setWindowCallback(this);
        setWillNotDraw(false);
    }

    @Override
    public void addView(View child) {
        if (!(child instanceof XWindowView)) {
            throw new RuntimeException("Invalid view " + child);
        }
        super.addView(child);
    }

    // ----- View stuff ------

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mWindow.setVisibily(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mWindow.setVisibily(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width, height;
        synchronized (mWindow) {
            Rect bounds = mWindow.getBounds();
            width = bounds.width();
            height = bounds.height();
            setMeasuredDimension(width, height);
        }

        for (int i = 0; i < getChildCount(); i++) {
            XWindowView child = (XWindowView) getChildAt(i);
            synchronized (child.mWindow) {
                child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            XWindowView child = (XWindowView) getChildAt(i);
            synchronized (child.mWindow) {
                Rect bounds = child.mWindow.getBounds();
                child.layout(bounds.left, bounds.top, bounds.right, bounds.bottom);
            }
        }
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        try (Time ignored = t(String.format("Draw(%x)", mWindow.getId()))) {
            // TODO: Probably really really don't want to be synchronizing here, figure out if needed
            // or if way around it.
            synchronized (mWindow) {
                Bitmap bitmap = mWindow.getBitmap();
                canvas.drawBitmap(bitmap, mWindow.getBounds(), mWindow.getBounds(), new Paint());
            }
        }
    }

    // ----- XServer stuff ------

    @Override
    public void onContentChanged() {
        postInvalidate();
    }

    @Override
    public void windowOrderChanged() {
        synchronized (mWindow) {
            for (int i = 0; i < getChildCount(); i++) {
                while (mWindow.getChildAtLocked(i) != ((XWindowView) getChildAt(i)).mWindow) {
                    bringChildToFront(getChildAt(i));
                }
            }
        }
    }

    @Override
    public void onChildAdded(XWindow w) {
    }

    @Override
    public void onChildRemoved(XWindow w) {
    }

    @Override
    public void onVisibilityChanged() {
        // Don't care?
    }

    @Override
    public void onChildMappingChanged(XWindow child) {
        post(() -> {
            if ((child.getVisibility() & XWindow.FLAG_MAPPED) != 0) {
                addView(new XWindowView(getContext(), child));
            } else {
                for (int i = 0; i <= getChildCount(); i++) {
                    if (((XWindowView) getChildAt(i)).mWindow == child) {
                        removeViewAt(i);
                        return;
                    }
                }
            }
        });
    }

    @Override
    public void onLocationChanged() {
        postRequestLayout();
    }

    @Override
    public void onSizeChanged() {
        postRequestLayout();
    }

    protected void postRequestLayout() {
        post(() -> requestLayout());
    }
}
