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

package org.monksanctum.xand11.hosts;

import java.util.ArrayList;
import java.util.List;

// TODO: Hook up to access control.
public class HostsManager {
    private final List<Host> mHosts = new ArrayList<>();

    public List<Host> getHosts() {
        synchronized (mHosts) {
            return new ArrayList<>(mHosts);
        }
    }

    public void addHost(Host host) {
        synchronized (mHosts) {
            mHosts.add(host);
        }
    }

    public void remHost(Host host) {
        synchronized (mHosts) {
            mHosts.remove(host);
        }
    }
}
