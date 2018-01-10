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

package org.monksanctum.xand11.fonts;

import android.graphics.Rect;
import android.util.Log;

import org.monksanctum.xand11.Client;
import org.monksanctum.xand11.Dispatcher;
import org.monksanctum.xand11.comm.Event;
import org.monksanctum.xand11.comm.PacketReader;
import org.monksanctum.xand11.comm.PacketWriter;
import org.monksanctum.xand11.comm.Request;
import org.monksanctum.xand11.errors.GContextError;
import org.monksanctum.xand11.errors.XError;
import org.monksanctum.xand11.fonts.Font.CharInfo;
import org.monksanctum.xand11.fonts.Font.FontProperty;
import org.monksanctum.xand11.graphics.GraphicsManager;

import java.util.List;

import static org.monksanctum.xand11.fonts.FontManager.DEBUG;

public class FontProtocol implements Dispatcher.PacketHandler {

    private static final byte[] HANDLED_OPS = new byte[] {
            Request.OPEN_FONT,
            Request.QUERY_FONT,
            Request.CLOSE_FONT,
            Request.LIST_FONTS,
            Request.LIST_FONTS_WITH_INFO,
            Request.QUERY_TEXT_EXTENTS,
    };

    private final FontManager mFontManager;
    private final GraphicsManager mGraphicsManager;

    public FontProtocol(FontManager manager, GraphicsManager graphicsManager) {
        mFontManager = manager;
        mGraphicsManager = graphicsManager;
    }

    @Override
    public byte[] getOpCodes() {
        return HANDLED_OPS;
    }

    @Override
    public void handleRequest(Client client, PacketReader reader, PacketWriter writer)
            throws XError {
        switch (reader.getMajorOpCode()) {
            case Request.OPEN_FONT:
                handleOpenFont(reader);
                break;
            case Request.CLOSE_FONT:
                handleCloseFont(reader);
                break;
            case Request.QUERY_FONT:
                handleQueryFont(reader, writer);
                break;
            case Request.LIST_FONTS:
                handleListFonts(reader, writer);
                break;
            case Request.LIST_FONTS_WITH_INFO:
                handleListFontsWithInfo(client, reader, writer);
                break;
            case Request.QUERY_TEXT_EXTENTS:
                handleQueryTextExtents(reader, writer);
                break;
        }
    }

    private void handleListFontsWithInfo(Client client, PacketReader reader, PacketWriter writer) {
        int max = reader.readCard16();
        int n = reader.readCard16();
        String pattern = reader.readPaddedString(n);
        List<Font> fonts = mFontManager.getFontsMatching(pattern, max);
        final int N = fonts.size();

        for (int i = 0; i < N; i++) {
            PacketWriter pWriter = new PacketWriter(client.getClientListener().getWriter());
            Font font = fonts.get(i);
            String name = font.toString();
            pWriter.setMinorOpCode((byte) name.length());
            writeFont(pWriter, font, N - i);
            pWriter.writePaddedString(name);
            try (PacketReader r = pWriter.copyToReader()) {
                logInfo("FontProtocol", r);
            }
            client.getClientListener().sendPacket(Event.REPLY, pWriter);
        }

        writer.writePadding(52);
    }

    private void handleListFonts(PacketReader reader, PacketWriter writer) {
        int max = reader.readCard16();
        int n = reader.readCard16();
        String pattern = reader.readPaddedString(n);
        List<Font> fonts = mFontManager.getFontsMatching(pattern, max);
        final int N = fonts.size();
        writer.writeCard16(N);
        writer.writePadding(22);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < N; i++) {
            String fontName = fonts.get(i).toString();
            builder.append((char) (byte) fontName.length());
            builder.append(fontName);
        }
        writer.writePaddedString(builder.toString());
    }

    private void handleOpenFont(PacketReader reader) {
        int fid = reader.readCard32();
        int length = reader.readCard16();
        reader.readPadding(2);
        String name = reader.readPaddedString(length);
        mFontManager.openFont(fid, name);
    }

    private void handleCloseFont(PacketReader reader) {
        int fid = reader.readCard32();
        mFontManager.closeFont(fid);
    }

    private void handleQueryFont(PacketReader reader, PacketWriter writer) {
        int fid = reader.readCard32();
        Font font = mFontManager.getFont(fid);
        if (font != null) {
            writeFont(writer, font, font.getChars().size());
            writeChars(writer, font);
            try (PacketReader r = writer.copyToReader()) {
                logInfo("FontManager", r);
            }
        } else {
            Log.d("FontManager", "Sending empty font " + Integer.toHexString(fid));
            writer.writePadding(28);
        }
    }

    private void handleQueryTextExtents(PacketReader reader, PacketWriter writer)
            throws GContextError {
        boolean odd = reader.getMinorOpCode() != 0;
        int fid = reader.readCard32();
        Font font = mFontManager.getFont(fid);
        if (font == null) {
            font = mFontManager.getFont(mGraphicsManager.getGc(fid).font);
        }
        int length = (reader.getRemaining() / 2) - (odd ? 1 : 0);
        String s = reader.readString16(length);
        Rect bounds = new Rect();
        int width = (int) font.getPaint().measureText(s);
        font.getPaint().getTextBounds(s, 0, s.length(), bounds);

        writer.writeCard16(font.maxBounds.ascent);
        writer.writeCard16(font.maxBounds.descent);
        writer.writeCard16((short) -bounds.top);
        writer.writeCard16((short) bounds.bottom);
        writer.writeCard32(width);
        writer.writeCard32(bounds.left);
        writer.writeCard32(bounds.right);
        writer.writePadding(4);
    }

    private void writeFont(PacketWriter writer, Font font, int sizeField) {
        writeChar(writer, font.minBounds);
        writer.writePadding(4);
        writeChar(writer, font.maxBounds);
        writer.writePadding(4);

        writer.writeCard16(font.minCharOrByte2);
        writer.writeCard16(font.maxCharOrByte2);
        writer.writeCard16(font.defaultChar);
        final int size = font.mFontProperties.size();
        writer.writeCard16(size);

        writer.writeByte(font.isRtl ? Font.RIGHT_TO_LEFT : Font.LEFT_TO_RIGHT);
        writer.writeByte(font.minByte1);
        writer.writeByte(font.maxByte1);
        writer.writeByte((byte) (font.allCharsExist ? 1 : 0));

        writer.writeCard16(font.fontAscent);
        writer.writeCard16(font.fontDescent);
        writer.writeCard32(sizeField);
        for (int i = 0; i < size; i++) {
            writeProperty(writer, font.mFontProperties.get(i));
        }
    }

    private void writeChars(PacketWriter writer, Font font) {
        List<CharInfo> chars = font.getChars();
        final int N = chars.size();
        for (int i = 0; i < N; i++) {
            writeChar(writer, chars.get(i));
        }
    }

    private void writeProperty(PacketWriter writer, FontProperty property) {
        writer.writeCard32(property.name);
        writer.writeCard32(property.value);
    }

    private void writeChar(PacketWriter writer, Font.CharInfo charInfo) {
        writer.writeCard16(charInfo.leftSideBearing);
        writer.writeCard16(charInfo.rightSideBearing);
        writer.writeCard16(charInfo.characterWidth);
        writer.writeCard16(charInfo.ascent);
        writer.writeCard16(charInfo.descent);
        writer.writeCard16(charInfo.attributes);
    }

    public static void logInfo(String tag, PacketReader r) {
        if (!DEBUG) return;
        Log.d(tag, "minBounds");
        logChar(tag, r);
        Log.d(tag, "");
        Log.d(tag, "maxBounds");
        logChar(tag, r);
        Log.d(tag, "");

        Log.d(tag, "minChar=" + r.readCard16() + " maxChar=" + r.readCard16());
        Log.d(tag, "defaultChar=" + r.readCard16());
        int numProps = r.readCard16();

        Log.d(tag, "");
        Log.d(tag, "isRtl:" + (r.readByte() == Font.RIGHT_TO_LEFT));
        Log.d(tag, "minByte=" + r.readByte() + " maxByte=" + r.readByte());
        Log.d(tag, "allChars:" + (r.readByte() != 0));
        Log.d(tag, "ascent=" + r.readCard16() + " descent=" + r.readCard16());
        Log.d(tag, "");
        Log.d(tag, "Remaining hint: " + r.readCard32());
        for (int i = 0; i < numProps; i++) {
            Log.d(tag, "Prop: " + r.readCard32() + " " + r.readCard32());
        }
        Log.d(tag, "Name: " + r.readPaddedString(r.getMinorOpCode()));
    }

    private static void logChar(String tag, PacketReader r) {
        Log.d(tag, "leftSideBearing=" + r.readCard16()
                + " rightSideBearing=" + r.readCard16());
        Log.d(tag, "width=" + r.readCard16());
        Log.d(tag, "ascent=" + r.readCard16() + " descent=" + r.readCard16());
        Log.d(tag, "attributes=" + r.readCard16());
        r.readPadding(4);
    }
}
