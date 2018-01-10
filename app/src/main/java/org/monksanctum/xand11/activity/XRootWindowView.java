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

    public XRootWindowView(Context context, XWindow window) {
        super(context, window);
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
        Utils.sBgHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mWindow) {
                    mWindow.notifyKeyDown(keyCode);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onKeyUp(final int keyCode, KeyEvent event) {
        Utils.sBgHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mWindow) {
                    mWindow.notifyKeyUp(keyCode);
                }
            }
        });
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        Rect bounds = mWindow.getBounds();
        int border = mWindow.getBorderWidth();
        if (bounds.right != width || bounds.bottom != height || bounds.left != 0 || bounds
                .right != 0) {
            if (mWindow.setBounds(new Rect(0, 0, width - 2 * border, height - 2 * border))) {
                Utils.sBgHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mWindow.notifyConfigureWindow();
                    }
                });
            }
        }
        setMeasuredDimension(width, height);
    }
}
