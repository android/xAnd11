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

package org.monksanctum.xand11.comm

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class PassthroughReader(inputStream: InputStream, private val mOutputStream: OutputStream) : XProtoReader(inputStream) {

    @Throws(XProtoReader.ReadException::class)
    override fun readByte(): Byte {
        val b = super.readByte()
        try {
            mOutputStream.write(b.toInt())
            mOutputStream.flush()
        } catch (e: IOException) {
            throw XProtoReader.ReadException(e)
        }

        return b
    }
}
