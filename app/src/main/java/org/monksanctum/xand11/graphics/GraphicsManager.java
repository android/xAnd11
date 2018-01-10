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

import android.util.SparseArray;

import org.monksanctum.xand11.XService;
import org.monksanctum.xand11.errors.DrawableError;
import org.monksanctum.xand11.errors.GContextError;
import org.monksanctum.xand11.errors.PixmapError;
import org.monksanctum.xand11.errors.ValueError;

public class GraphicsManager {

    public static final boolean DEBUG = XService.GRAPHICS_DEBUG;

    private final SparseArray<GraphicsContext> mGcs = new SparseArray<>();
    private final SparseArray<Pixmap> mPixmaps = new SparseArray<>();
    private final SparseArray<XDrawable> mDrawableLookup = new SparseArray<>();

    private final ColorMaps mColorMaps = new ColorMaps();

    public ColorMaps getColorMap() {
        return mColorMaps;
    }

    public void addDrawable(int id, XDrawable drawable) {
        synchronized (mDrawableLookup) {
            mDrawableLookup.put(id, drawable);
        }
    }

    public void removeDrawable(int id) {
        synchronized (mDrawableLookup) {
            mDrawableLookup.remove(id);
        }
    }

    public XDrawable getDrawable(int id) throws DrawableError {
        synchronized (mDrawableLookup) {
            XDrawable drawable = mDrawableLookup.get(id);
            if (drawable == null) {
                throw new DrawableError(id);
            }
            return drawable;
        }
    }

    public GraphicsContext createGc(int id) {
        GraphicsContext graphicsContext = new GraphicsContext(id);
        synchronized (mGcs) {
            mGcs.put(id, graphicsContext);
        }
        return graphicsContext;
    }

    public GraphicsContext getGc(int id) throws GContextError {
        synchronized (mGcs) {
            GraphicsContext graphicsContext = mGcs.get(id);
            if (graphicsContext == null) {
                throw new GContextError(id);
            }
            return graphicsContext;
        }
    }

    public void freeGc(int id) {
        synchronized (mGcs) {
            mGcs.remove(id);
        }
    }

    public Pixmap createPixmap(int id, byte depth, int width, int height, XDrawable drawable)
            throws ValueError {
        Pixmap pixmap = new Pixmap(depth, width, height, id, drawable);
        synchronized (mPixmaps) {
            mPixmaps.put(id, pixmap);
        }
        addDrawable(id, pixmap);
        return pixmap;
    }

    public void freePixmap(int id) {
        synchronized (mPixmaps) {
            mPixmaps.remove(id);
        }
        removeDrawable(id);
    }

    public Pixmap getPixmap(int pixmap) throws PixmapError {
        synchronized (mPixmaps) {
            Pixmap p = mPixmaps.get(pixmap);
            if (p == null) {
                throw new PixmapError(pixmap);
            }
            return p;
        }
    }
}
