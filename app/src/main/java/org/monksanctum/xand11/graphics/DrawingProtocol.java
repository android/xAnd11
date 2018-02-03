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
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.util.Pair;

import org.monksanctum.xand11.Client;
import org.monksanctum.xand11.Dispatcher.PacketHandler;
import org.monksanctum.xand11.comm.PacketReader;
import org.monksanctum.xand11.comm.PacketWriter;
import org.monksanctum.xand11.comm.Request;
import org.monksanctum.xand11.comm.XProtoReader;
import org.monksanctum.xand11.comm.XProtoWriter;
import org.monksanctum.xand11.comm.XSerializable;
import org.monksanctum.xand11.errors.DrawableError;
import org.monksanctum.xand11.errors.GContextError;
import org.monksanctum.xand11.errors.XError;
import org.monksanctum.xand11.fonts.Font;
import org.monksanctum.xand11.fonts.FontManager;

import java.util.ArrayList;
import java.util.List;

import static org.monksanctum.xand11.graphics.GraphicsManager.DEBUG;

public class DrawingProtocol implements PacketHandler {

    private static final String TAG = "DrawingProtocol";

    private static final byte[] HANDLED_OPS = {
            Request.POLY_SEGMENT,
            Request.FILL_POLY,
            Request.POLY_LINE,
            Request.POLY_TEXT_8,
            Request.POLY_FILL_RECTANGLE,
            Request.SET_CLIP_RECTANGLES,
    };

    private final GraphicsManager mGraphics;
    private final FontManager mFontManager;
    private final Rect mBounds = new Rect();

    public DrawingProtocol(GraphicsManager manager, FontManager fontManager) {
        mGraphics = manager;
        mFontManager = fontManager;
    }

    @Override
    public byte[] getOpCodes() {
        return HANDLED_OPS;
    }

    @Override
    public void handleRequest(Client client, PacketReader reader, PacketWriter writer)
            throws XError {
        switch (reader.getMajorOpCode()) {
            case Request.POLY_LINE:
                handlePolyLine(reader);
                break;
            case Request.FILL_POLY:
                handleFillPoly(reader);
                break;
            case Request.POLY_SEGMENT:
                handlePolySegment(reader);
                break;
            case Request.POLY_TEXT_8:
                handlePolyText8(reader);
                break;
            case Request.POLY_FILL_RECTANGLE:
                handlePolyFillRectangle(reader);
                break;
            case Request.SET_CLIP_RECTANGLES:
                handleSetClipRectangles(reader);
                break;
        }
    }

    private void handlePolyFillRectangle(PacketReader reader) throws XError {
        XDrawable drawable = mGraphics.getDrawable(reader.readCard32());
        GraphicsContext gContext = mGraphics.getGc(reader.readCard32());
        Rectangle r = new Rectangle();
        Paint p = gContext.getPaint();
        p.setStyle(Style.FILL);
        synchronized (drawable) {
            Canvas canvas = drawable.lockCanvas(gContext);
            while (reader.getRemaining() != 0) {
                try {
                    r.read(reader);
                    canvas.drawRect(r.r, p);
                } catch (XProtoReader.ReadException e) {
                    // Not possible here.
                    throw new RuntimeException(e);
                }
            }
            drawable.unlockCanvas();
        }
    }

    private void handlePolyText8(PacketReader reader) throws XError {
        XDrawable drawable = mGraphics.getDrawable(reader.readCard32());
        GraphicsContext gContext = mGraphics.getGc(reader.readCard32());
        int x = reader.readCard16();
        int y = reader.readCard16();
        Canvas c = drawable.lockCanvas(gContext);
        Paint paint = gContext.getPaint();
        TextItem8 item = new TextItem8();
        while (reader.getRemaining() > 1) {
            try {
                item.read(reader);
            } catch (XProtoReader.ReadException e) {
                // Not possible here.
                throw new RuntimeException(e);
            }
            if (item.mFontShift) {
                int font = item.mFont;
                Font f = mFontManager.getFont(font);
                paint = gContext.applyToPaint(f.getPaint());
            } else {
                x += item.mDelta;
                Font.getTextBounds(item.mValue, paint, x, y, mBounds);

                paint.setColor(gContext.foreground);
                c.drawText(item.mValue, x, y, paint);
                x += mBounds.width();
            }
        }
        if (reader.getRemaining() != 0) {
            // Just to avoid the warnings, read the last byte.
            reader.readPadding(1);
        }
        drawable.unlockCanvas();
    }

    private void handlePolyLine(PacketReader reader) throws DrawableError, GContextError {
        XDrawable drawable = mGraphics.getDrawable(reader.readCard32());
        GraphicsContext gContext = mGraphics.getGc(reader.readCard32());
        byte mode = reader.getMinorOpCode();
        Path p = readPath(mode, reader);

        synchronized (drawable) {
            Canvas c = drawable.lockCanvas(gContext);
            if (DEBUG) Log.d(TAG, "Poly line");
            Paint paint = gContext.getPaint();
            paint.setStyle(Style.STROKE);
            c.drawPath(p, paint);
            drawable.unlockCanvas();
        }
    }

    private void handleFillPoly(PacketReader reader) throws DrawableError, GContextError {
        XDrawable drawable = mGraphics.getDrawable(reader.readCard32());
        GraphicsContext gContext = mGraphics.getGc(reader.readCard32());
        // TODO: Really need to use these.
        byte shape = reader.readByte();
        byte mode = reader.readByte();
        reader.readPadding(2);
        Path p = readPath(mode, reader);

        synchronized (drawable) {
            Canvas c = drawable.lockCanvas(gContext);
            Paint paint = new Paint(gContext.getPaint());
            paint.setStyle(Style.FILL);
            if (DEBUG) Log.d(TAG, "Filling poly");
            c.drawPath(p, paint);
            drawable.unlockCanvas();
        }
    }

    private void handlePolySegment(PacketReader reader) throws DrawableError, GContextError {
        XDrawable drawable = mGraphics.getDrawable(reader.readCard32());
        GraphicsContext gContext = mGraphics.getGc(reader.readCard32());
        List<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> segments = new ArrayList<>();
        while (reader.getRemaining() != 0) {
            Pair<Integer, Integer> start = new Pair<>(reader.readCard16(), reader.readCard16());
            Pair<Integer, Integer> end = new Pair<>(reader.readCard16(), reader.readCard16());
            segments.add(new Pair<>(start, end));
        }
        final int N = segments.size();
        synchronized (drawable) {
            Canvas c = drawable.lockCanvas(gContext);
            for (int i = 0; i < N; i++) {
                Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> segment = segments.get(i);
                Pair<Integer, Integer> start = segment.first;
                Pair<Integer, Integer> end = segment.second;
                c.drawLine(start.first, start.second, end.first, end.second, gContext.getPaint());
            }
            if (DEBUG) Log.d(TAG, "Poly segment");
            drawable.unlockCanvas();
        }
    }

    private void handleSetClipRectangles(PacketReader reader) throws XError {
        GraphicsContext gContext = mGraphics.getGc(reader.readCard32());
        int x = reader.readInt16();
        int y = reader.readInt16();
        Rectangle r = new Rectangle();
        Path p = new Path();
        while (reader.getRemaining() != 0) {
            try {
                r.read(reader);
            } catch (XProtoReader.ReadException e) {
                // Not possible here.
                throw new RuntimeException(e);
            }
            p.addRect(new RectF(r.r), Path.Direction.CW);
        }
        gContext.setClipPath(p);
    }

    public static final byte COORDINATES_ABSOLUTE = 0;
    public static final byte COORDINATES_RELATIVE = 1;

    private Path readPath(byte mode, PacketReader reader)  {
        Path p = new Path();
        int startX = reader.readCard16();
        int startY = reader.readCard16();
        p.moveTo(startX, startY);
        while (reader.getRemaining() != 0) {
            int x = (short) reader.readCard16();
            int y = (short) reader.readCard16();
            if (mode == COORDINATES_RELATIVE) {
                p.rLineTo(x, y);
            } else {
                p.lineTo(x, y);
            }
        }
        //p.lineTo(startX, startY);
        return p;
    }

    static class Rectangle implements XSerializable {
        private final Rect r = new Rect();

        @Override
        public void write(XProtoWriter writer) throws XProtoWriter.WriteException {
            writer.writeCard16(r.left);
            writer.writeCard16(r.top);
            writer.writeCard16(r.width());
            writer.writeCard16(r.height());
        }

        @Override
        public void read(XProtoReader reader) throws XProtoReader.ReadException {
            r.left = reader.readCard16();
            r.top = reader.readCard16();
            r.right = r.left + reader.readCard16();
            r.bottom = r.top + reader.readCard16();
        }
    }

    static class TextItem8 implements XSerializable {

        private byte mDelta;
        private String mValue;
        private boolean mFontShift;
        private int mFont;

        @Override
        public void read(XProtoReader reader) throws XProtoReader.ReadException {
            int len = reader.readByte();
            if (len == 255) {
                mFontShift = true;
                int f3 = reader.readByte();
                int f2 = reader.readByte();
                int f1 = reader.readByte();
                mFont = (f3 << 24) | (f2 << 16) | (f1 << 8) | reader.readByte();
            } else {
                mFontShift = false;
                mDelta = reader.readByte();
                mValue = reader.readString(len);
            }
        }

        @Override
        public void write(XProtoWriter writer) throws XProtoWriter.WriteException {
            if (mFontShift) {
                writer.writeByte((byte) 255);
                writer.writeByte((byte) (mFont >> 24));
                writer.writeByte((byte) (mFont >> 16));
                writer.writeByte((byte) (mFont >> 8));
                writer.writeByte((byte) (mFont >> 0));
            } else {
                writer.writeByte((byte) mValue.length());
                writer.writeByte(mDelta);
                writer.writeString(mValue);
            }
        }
    }
}
