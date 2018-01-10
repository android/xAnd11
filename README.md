# xAnd11

xAnd11 is an X Window Protocol implementation for Android. Unlike other
X implementations, xAnd11 aims to integrate with the Android system as much
as possible. This requires performanig both the role of an X server and a
window manager at times.

Each top-level window will create its own activity/task (allowing them to
appear in Android's overview). Map/unmap events will control the creation/
destruction of these activities. The activity's resumed state controls the
exposing of the window.

## Status

Currently only some requests are implemented and if non-supported request
is received then the server may freeze.

The server has been tested with xclock and xterm and both are able to start
and xterm is able to run some commands.
