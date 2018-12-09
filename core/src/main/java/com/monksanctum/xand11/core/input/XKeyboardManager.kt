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

package org.monksanctum.xand11.input

import org.monksanctum.xand11.core.KeyEvent
import org.monksanctum.xand11.core.getMaxKeyCode
import org.monksanctum.xand11.core.initCharMap

class XKeyboardManager {


    val keysPerSym: Int
    val keyboardMap: IntArray

    val modifiers = byteArrayOf(KeyEvent.KEYCODE_SHIFT_LEFT.toByte(),
            KeyEvent.KEYCODE_SHIFT_RIGHT.toByte(),
            0,
            0,
            0,
            0,
            KeyEvent.KEYCODE_ALT_LEFT.toByte(),
            KeyEvent.KEYCODE_ALT_RIGHT.toByte(),
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0)

    private val mState = BooleanArray(modifiers.size)

    val state: Int
        get() {
            var state = 0
            for (i in mState.indices) {
                if (mState[i]) {
                    state = state or (1 shl i / 2)
                }
            }
            return state
        }

    init {
        val modifiers = intArrayOf(0, KeyEvent.META_SHIFT_ON, KeyEvent.META_ALT_ON)
        keysPerSym = modifiers.size

        val maxKeyCode = getMaxKeyCode()
        keyboardMap = IntArray(modifiers.size * maxKeyCode)
        initCharMap(keyboardMap, modifiers)
        keyboardMap[keysPerSym * KeyEvent.KEYCODE_DEL] = 127
        for (i in this.modifiers.indices) {
            this.modifiers[i] = translate(this.modifiers[i].toInt()).toByte()
        }
    }

    fun translate(keyCode: Int): Int {
        return keyCode + 8
    }

    private fun modIndex(keyCode: Int): Int {
        for (i in modifiers.indices) {
            if (modifiers[i].toInt() == keyCode) {
                return i
            }
        }
        return -1
    }

    fun onKeyDown(keyCode: Int) {
        val modIndex = modIndex(translate(keyCode))
        if (modIndex < 0) return
        mState[modIndex] = true
    }

    fun onKeyUp(keyCode: Int) {
        val modIndex = modIndex(translate(keyCode))
        if (modIndex < 0) return
        mState[modIndex] = false
    }
}
