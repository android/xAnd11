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
import android.graphics.Rect;
import android.view.KeyEvent;

import org.monksanctum.xand11.Utils;
import org.monksanctum.xand11.windows.XWindow;

public class XRootWindowView extends XWindowView {

    private static final boolean RESIZE_TO_VIEW = false;

    public XRootWindowView(Context context, XWindow window) {
        super(context, window);
        setBackgroundColor(0xffffffff);
    }

    @Override
    public void onLocationChanged() {
        super.onLocationChanged();
    }

    @Override
    public void onSizeChanged() {
        super.onSizeChanged();
    }

    @Override
    public void onVisibilityChanged() {
        super.onVisibilityChanged();
    }

    @Override
    public boolean onKeyDown(final int keyCode, KeyEvent event) {
        Utils.sBgHandler.post(() -> {
            synchronized (mWindow) {
                mWindow.notifyKeyDown(keyCode);
            }
        });
        return true;
    }

    @Override
    public boolean onKeyUp(final int keyCode, KeyEvent event) {
        Utils.sBgHandler.post(() -> {
            synchronized (mWindow) {
                mWindow.notifyKeyUp(keyCode);
            }
        });
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (RESIZE_TO_VIEW) {
            Rect bounds = mWindow.getBounds();
            int border = mWindow.getBorderWidth();
            if (bounds.right != width || bounds.bottom != height || bounds.left != 0 || bounds
                    .right != 0) {
                if (mWindow.setBounds(new Rect(0, 0, width, height))) {
                    Utils.sBgHandler.post(() -> mWindow.notifyConfigureWindow());
                }
            }
        } else {
            width = mWindow.getBounds().width();
            height = mWindow.getBounds().height();
        }
        setMeasuredDimension(width, height);
    }
}
