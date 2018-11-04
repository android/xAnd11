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

class XInputManager {

    val keyboardManager = XKeyboardManager()
    val selection = SelectionManager()

    val focusState: FocusState
        get() = FocusState()

    val state: Int
        get() = keyboardManager.state

    fun translate(keyCode: Int): Int {
        return keyboardManager.translate(keyCode)
    }

    fun onKeyDown(keyCode: Int) {
        keyboardManager.onKeyDown(keyCode)
    }

    fun onKeyUp(keyCode: Int) {
        keyboardManager.onKeyUp(keyCode)
    }
}
