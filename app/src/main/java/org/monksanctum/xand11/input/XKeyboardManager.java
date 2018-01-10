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

package org.monksanctum.xand11.input;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;

public class XKeyboardManager {


    private int mKeysPerSym;
    private int[] mKeymap;

    private byte[] mModifiers = new byte[]{
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT,
            0, 0,
            0, 0,
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
    };
    private boolean[] mState = new boolean[mModifiers.length];

    public XKeyboardManager() {
        int[] modifiers = new int[]{
                0,
                KeyEvent.META_SHIFT_ON,
                KeyEvent.META_ALT_ON,
        };
        KeyCharacterMap map = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        mKeysPerSym = modifiers.length;

        int maxKeyCode = KeyEvent.getMaxKeyCode();
        int index = 0;
        mKeymap = new int[modifiers.length * maxKeyCode];
        for (int i = 0; i < maxKeyCode; i++) {
            for (int j = 0; j < modifiers.length; j++) {
                mKeymap[index++] = map.get(i, modifiers[j]);
            }
        }
        mKeymap[mKeysPerSym * KeyEvent.KEYCODE_DEL] = 127;
        for (int i = 0; i < mModifiers.length; i++) {
            mModifiers[i] = (byte) translate(mModifiers[i]);
        }
    }

    public byte[] getModifiers() {
        return mModifiers;
    }

    public int getKeysPerSym() {
        return mKeysPerSym;
    }

    public int[] getKeyboardMap() {
        return mKeymap;
    }

    public int translate(int keyCode) {
        return keyCode + 8;
    }

    public int getState() {
        int state = 0;
        for (int i = 0; i < mState.length; i++) {
            if (mState[i]) {
                state |= (1 << (i / 2));
            }
        }
        return state;
    }

    private int modIndex(int keyCode) {
        for (int i = 0; i < mModifiers.length; i++) {
            if (mModifiers[i] == keyCode) {
                return i;
            }
        }
        return -1;
    }

    public void onKeyDown(int keyCode) {
        int modIndex = modIndex(translate(keyCode));
        if (modIndex < 0) return;
        mState[modIndex] = true;
    }

    public void onKeyUp(int keyCode) {
        int modIndex = modIndex(translate(keyCode));
        if (modIndex < 0) return;
        mState[modIndex] = false;
    }
}
