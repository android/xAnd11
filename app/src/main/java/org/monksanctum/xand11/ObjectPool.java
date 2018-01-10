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

package org.monksanctum.xand11;

import org.monksanctum.xand11.ObjectPool.Recycleable;

import java.util.ArrayList;

public abstract class ObjectPool<T extends Recycleable, V> {

    public ArrayList<T> mAvailable = new ArrayList<>();

    public T obtain(V... arg) {
        synchronized (mAvailable) {
            for (int i = 0; i < mAvailable.size(); i++) {
                if (validate(mAvailable.get(i), arg)) {
                    return mAvailable.remove(i);
                }
            }
        }
        T t = create(arg);
        t.mPool = this;
        return t;
    }

    private void recycle(T recycleable) {
        synchronized (mAvailable) {
            mAvailable.add(recycleable);
        }
    }

    protected boolean validate(T inst, V... arg) {
        return true;
    }

    protected abstract T create(V... arg);

    public static class Recycleable implements AutoCloseable {
        protected ObjectPool mPool;

        @Override
        public void close() {
            recycle();
        }

        public void recycle() {
            mPool.recycle(this);
        }
    }
}
