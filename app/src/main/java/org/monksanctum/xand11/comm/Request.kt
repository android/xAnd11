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

import org.monksanctum.xand11.extension.Extension

import java.lang.reflect.Field
import java.lang.reflect.Modifier

object Request {
    val CREATE_WINDOW: Byte = 1
    val CHANGE_WINDOW_ATTRIBUTES: Byte = 2
    val GET_WINDOW_ATTRIBUTES: Byte = 3
    val DESTROY_WINDOW: Byte = 4
    val DESTROY_SUBWINDOWS: Byte = 5
    val CHANGE_SAVE_SET: Byte = 6
    val REPARENT_WINDOW: Byte = 7
    val MAP_WINDOW: Byte = 8
    val MAP_SUBWINDOWS: Byte = 9
    val UNMAP_WINDOW: Byte = 10
    val UNMAP_SUBWINDOWS: Byte = 11
    val CONFIGURE_WINDOW: Byte = 12
    val CIRCULATE_WINDOW: Byte = 13
    val GET_GEOMETRY: Byte = 14
    val QUERY_TREE: Byte = 15

    val INTERN_ATOM: Byte = 16
    val GET_ATOM_NAME: Byte = 17

    val CHANGE_PROPERTY: Byte = 18
    val DELETE_PROPERTY: Byte = 19
    val GET_PROPERTY: Byte = 20
    val LIST_PROPERTIES: Byte = 21

    val SET_SELECTION_OWNER: Byte = 22
    val GET_SELECTION_OWNER: Byte = 23
    val CONVERT_SELECTION: Byte = 24

    val SEND_EVENT: Byte = 25

    val GRAB_POINTER: Byte = 26
    val UNGRAB_POINTER: Byte = 27
    val GRAB_BUTTON: Byte = 28
    val UNGRAB_BUTTON: Byte = 29
    val CHANGE_ACTIVE_POINTER_GRAB: Byte = 30
    val GRAB_KEYBOARD: Byte = 31
    val UNGRAB_KEYBOARD: Byte = 32
    val GRAB_KEY: Byte = 33
    val UNGRAB_KEY: Byte = 34
    val ALLOW_EVENTS: Byte = 35
    val GRAB_SERVER: Byte = 36
    val UNGRAB_SERVER: Byte = 37

    val QUERY_POINTER: Byte = 38
    val GET_MOTION_EVENTS: Byte = 39
    val TRANSLATE_COORDINATES: Byte = 40
    val WRAP_POINTER: Byte = 41
    val SET_INPUT_FOCUS: Byte = 42
    val GET_INPUT_FOCUS: Byte = 43

    val QUERY_KEYMAP: Byte = 44
    val OPEN_FONT: Byte = 45
    val CLOSE_FONT: Byte = 46
    val QUERY_FONT: Byte = 47
    val QUERY_TEXT_EXTENTS: Byte = 48
    val LIST_FONTS: Byte = 49
    val LIST_FONTS_WITH_INFO: Byte = 50
    val SET_FONT_PATH: Byte = 51
    val GET_FONT_PATH: Byte = 52

    val CREATE_PIXMAP: Byte = 53
    val FREE_PIXMAP: Byte = 54
    val CREATE_GC: Byte = 55
    val CHANGE_GC: Byte = 56
    val COPY_GC: Byte = 57
    val SET_DASHES: Byte = 58
    val SET_CLIP_RECTANGLES: Byte = 59
    val FREE_GC: Byte = 60

    val CLEAR_AREA: Byte = 61
    val COPY_AREA: Byte = 62
    val COPY_PLANE: Byte = 63
    val POLY_POINT: Byte = 64
    val POLY_LINE: Byte = 65
    val POLY_SEGMENT: Byte = 66
    val POLY_RECTANGLE: Byte = 67
    val POLY_ARC: Byte = 68
    val FILL_POLY: Byte = 69
    val POLY_FILL_RECTANGLE: Byte = 70
    val POLY_FILL_ARC: Byte = 71

    val PUT_IMAGE: Byte = 72
    val GET_IMAGE: Byte = 73
    val POLY_TEXT_8: Byte = 74
    val POLY_TEXT_16: Byte = 75
    val IMAGE_TEXT_8: Byte = 76
    val IMAGE_TEXT_16: Byte = 77

    val CREATE_COLORMAP: Byte = 78
    val FREE_COLORMAP: Byte = 79
    val COPY_COLORMAP_AND_FREE: Byte = 80
    val INSTALL_COLORMAP: Byte = 81
    val UNINSTALL_COLORMAP: Byte = 82
    val LIST_INSTALLED_COLORMAP: Byte = 83
    val ALLOC_COLOR: Byte = 84
    val ALLOC_NAMED_COLOR: Byte = 85
    val ALLOC_COLOR_CELLS: Byte = 86
    val ALLOC_COLOR_PLANES: Byte = 87
    val FREE_COLORS: Byte = 88
    val STORE_COLORS: Byte = 89
    val STORE_NAMED_COLOR: Byte = 90
    val QUERY_COLORS: Byte = 91
    val LOOKUP_COLOR: Byte = 92

    val CREATE_CURSOR: Byte = 93
    val CREATE_GLYPH_CURSOR: Byte = 94
    val FREE_CURSOR: Byte = 95
    val RECOLOR_CURSOR: Byte = 96
    val QUERY_BEST_SIZE: Byte = 97

    val QUERY_EXTENSION: Byte = 98
    val LIST_EXTENSIONS: Byte = 99

    val CHANGE_KEYBOARD_MAPPING: Byte = 100
    val GET_KEYBOARD_MAPPING: Byte = 101
    val CHANGE_KEYBOARD_CONTROL: Byte = 102
    val GET_KEYBOARD_CONTROL: Byte = 103
    val BELL: Byte = 104

    val CHANGE_POINTER_CONTROL: Byte = 105
    val GET_POINTER_CONTROL: Byte = 106

    val SET_SCREEN_SAVER: Byte = 107
    val GET_SCREEN_SAVER: Byte = 108

    val CHANGE_HOSTS: Byte = 109
    val LIST_HOSTS: Byte = 110
    val SET_ACCESS_CONTROL: Byte = 111
    val SET_CLOSE_DOWN_MODE: Byte = 112
    val KILL_CLIENT: Byte = 113

    val ROTATE_PROPERTIES: Byte = 114

    val FORCE_SCREEN_SAVER: Byte = 115

    val SET_POINTER_MAPPING: Byte = 116
    val GET_POINTER_MAPPING: Byte = 117
    val SET_MODIFIER_MAPPING: Byte = 118
    val GET_MODIFIER_MAPPING: Byte = 119

    val NO_OP: Byte = 127

    private val sNames = arrayOfNulls<String>(256)

    fun populateNames() {
        val cls = Request::class.java
        for (field in cls!!.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == Byte::class.javaPrimitiveType) {
                try {
                    val index = field.get(null) as Byte
                    sNames[index.toInt()] = field.getName()
                } catch (e: IllegalAccessException) {
                }

            }
        }
    }

    fun getName(what: Int): String? {
        return if (sNames[what] == null) {
            "Unknown($what)"
        } else sNames[what]
    }
}
