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

package org.monksanctum.xand11.display;

import org.monksanctum.xand11.comm.XProtoReader;
import org.monksanctum.xand11.comm.XProtoReader.ReadException;
import org.monksanctum.xand11.comm.XProtoWriter;
import org.monksanctum.xand11.comm.XSerializable;

public class Format implements XSerializable {

    public byte depth;
    public byte bitsPerPixel;
    public byte scanlinePad;

    public Format() {
        depth = 32;
        bitsPerPixel = 24;
        scanlinePad = 8;
    }

    public void write(XProtoWriter writer) throws XProtoWriter.WriteException {
        writer.writeByte(depth);
        writer.writeByte(bitsPerPixel);
        writer.writeByte(scanlinePad);
        writer.writePadding(5);
    }

    @Override
    public void read(XProtoReader reader) throws ReadException {
        depth = reader.readByte();
        bitsPerPixel = reader.readByte();
        scanlinePad = reader.readByte();
        reader.readPadding(5);
    }

    @Override
    public String toString() {
        return "(" + depth + "," + bitsPerPixel + "," + scanlinePad + ")";
    }
}
