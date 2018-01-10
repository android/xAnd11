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

public class XInputManager {

    private final XKeyboardManager mKeyboardManager = new XKeyboardManager();
    private final SelectionManager mSelectionManager = new SelectionManager();

    public FocusState getFocusState() {
        return new FocusState();
    }

    public int translate(int keyCode) {
        return mKeyboardManager.translate(keyCode);
    }

    public XKeyboardManager getKeyboardManager() {
        return mKeyboardManager;
    }

    public int getState() {
        return mKeyboardManager.getState();
    }

    public void onKeyDown(int keyCode) {
        mKeyboardManager.onKeyDown(keyCode);
    }

    public void onKeyUp(int keyCode) {
        mKeyboardManager.onKeyUp(keyCode);
    }

    public SelectionManager getSelection() {
        return mSelectionManager;
    }
}
