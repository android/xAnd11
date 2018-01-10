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
import android.util.Log;
import android.util.Pair;

import org.monksanctum.xand11.Client;
import org.monksanctum.xand11.Dispatcher.PacketHandler;
import org.monksanctum.xand11.comm.PacketReader;
import org.monksanctum.xand11.comm.PacketWriter;
import org.monksanctum.xand11.comm.Request;
import org.monksanctum.xand11.errors.DrawableError;
import org.monksanctum.xand11.errors.GContextError;
import org.monksanctum.xand11.errors.XError;

import java.util.ArrayList;
import java.util.List;

import static org.monksanctum.xand11.graphics.GraphicsManager.DEBUG;

public class DrawingProtocol implements PacketHandler {

    private static final String TAG = "DrawingProtocol";

    private static final byte[] HANDLED_OPS = {
            Request.POLY_SEGMENT,
            Request.FILL_POLY,
            Request.POLY_LINE,
    };

    private final GraphicsManager mGraphics;

    public DrawingProtocol(GraphicsManager manager) {
        mGraphics = manager;
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
        }
    }

    private void handlePolyLine(PacketReader reader) throws DrawableError, GContextError {
        XDrawable drawable = mGraphics.getDrawable(reader.readCard32());
        GraphicsContext gContext = mGraphics.getGc(reader.readCard32());
        byte mode = reader.getMinorOpCode();
        Path p = readPath(mode, reader);

        synchronized (drawable) {
            Canvas c = drawable.lockCanvas();
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
            Canvas c = drawable.lockCanvas();
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
            Canvas c = drawable.lockCanvas();
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
}
