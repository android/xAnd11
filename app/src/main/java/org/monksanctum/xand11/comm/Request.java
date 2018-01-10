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

package org.monksanctum.xand11.comm;

import org.monksanctum.xand11.extension.Extension;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Request {
    public static final byte CREATE_WINDOW = 1;
    public static final byte CHANGE_WINDOW_ATTRIBUTES = 2;
    public static final byte GET_WINDOW_ATTRIBUTES = 3;
    public static final byte DESTROY_WINDOW = 4;
    public static final byte DESTROY_SUBWINDOWS = 5;
    public static final byte CHANGE_SAVE_SET = 6;
    public static final byte REPARENT_WINDOW = 7;
    public static final byte MAP_WINDOW = 8;
    public static final byte MAP_SUBWINDOWS = 9;
    public static final byte UNMAP_WINDOW = 10;
    public static final byte UNMAP_SUBWINDOWS = 11;
    public static final byte CONFIGURE_WINDOW = 12;
    public static final byte CIRCULATE_WINDOW = 13;
    public static final byte GET_GEOMETRY = 14;
    public static final byte QUERY_TREE = 15;

    public static final byte INTERN_ATOM = 16;
    public static final byte GET_ATOM_NAME = 17;

    public static final byte CHANGE_PROPERTY = 18;
    public static final byte DELETE_PROPERTY = 19;
    public static final byte GET_PROPERTY = 20;
    public static final byte LIST_PROPERTIES = 21;

    public static final byte SET_SELECTION_OWNER = 22;
    public static final byte GET_SELECTION_OWNER = 23;
    public static final byte CONVERT_SELECTION = 24;

    public static final byte SEND_EVENT = 25;

    public static final byte GRAB_POINTER = 26;
    public static final byte UNGRAB_POINTER = 27;
    public static final byte GRAB_BUTTON = 28;
    public static final byte UNGRAB_BUTTON = 29;
    public static final byte CHANGE_ACTIVE_POINTER_GRAB = 30;
    public static final byte GRAB_KEYBOARD = 31;
    public static final byte UNGRAB_KEYBOARD = 32;
    public static final byte GRAB_KEY = 33;
    public static final byte UNGRAB_KEY = 34;
    public static final byte ALLOW_EVENTS = 35;
    public static final byte GRAB_SERVER = 36;
    public static final byte UNGRAB_SERVER = 37;

    public static final byte QUERY_POINTER = 38;
    public static final byte GET_MOTION_EVENTS = 39;
    public static final byte TRANSLATE_COORDINATES = 40;
    public static final byte WRAP_POINTER = 41;
    public static final byte SET_INPUT_FOCUS = 42;
    public static final byte GET_INPUT_FOCUS = 43;

    public static final byte QUERY_KEYMAP = 44;
    public static final byte OPEN_FONT = 45;
    public static final byte CLOSE_FONT = 46;
    public static final byte QUERY_FONT = 47;
    public static final byte QUERY_TEXT_EXTENTS = 48;
    public static final byte LIST_FONTS = 49;
    public static final byte LIST_FONTS_WITH_INFO = 50;
    public static final byte SET_FONT_PATH = 51;
    public static final byte GET_FONT_PATH = 52;

    public static final byte CREATE_PIXMAP = 53;
    public static final byte FREE_PIXMAP = 54;
    public static final byte CREATE_GC = 55;
    public static final byte CHANGE_GC = 56;
    public static final byte COPY_GC = 57;
    public static final byte SET_DASHES = 58;
    public static final byte SET_CLIP_RECTANGLES = 59;
    public static final byte FREE_GC = 60;

    public static final byte CLEAR_AREA = 61;
    public static final byte COPY_AREA = 62;
    public static final byte COPY_PLANE = 63;
    public static final byte POLY_POINT = 64;
    public static final byte POLY_LINE = 65;
    public static final byte POLY_SEGMENT = 66;
    public static final byte POLY_RECTANGLE = 67;
    public static final byte POLY_ARC = 68;
    public static final byte FILL_POLY = 69;
    public static final byte POLY_FILL_RECTANGLE = 70;
    public static final byte POLY_FILL_ARC = 71;

    public static final byte PUT_IMAGE = 72;
    public static final byte GET_IMAGE = 73;
    public static final byte POLY_TEXT_8 = 74;
    public static final byte POLY_TEXT_16 = 75;
    public static final byte IMAGE_TEXT_8 = 76;
    public static final byte IMAGE_TEXT_16 = 77;

    public static final byte CREATE_COLORMAP = 78;
    public static final byte FREE_COLORMAP = 79;
    public static final byte COPY_COLORMAP_AND_FREE = 80;
    public static final byte INSTALL_COLORMAP = 81;
    public static final byte UNINSTALL_COLORMAP = 82;
    public static final byte LIST_INSTALLED_COLORMAP = 83;
    public static final byte ALLOC_COLOR = 84;
    public static final byte ALLOC_NAMED_COLOR = 85;
    public static final byte ALLOC_COLOR_CELLS = 86;
    public static final byte ALLOC_COLOR_PLANES = 87;
    public static final byte FREE_COLORS = 88;
    public static final byte STORE_COLORS = 89;
    public static final byte STORE_NAMED_COLOR = 90;
    public static final byte QUERY_COLORS = 91;
    public static final byte LOOKUP_COLOR = 92;

    public static final byte CREATE_CURSOR = 93;
    public static final byte CREATE_GLYPH_CURSOR = 94;
    public static final byte FREE_CURSOR = 95;
    public static final byte RECOLOR_CURSOR = 96;
    public static final byte QUERY_BEST_SIZE = 97;

    public static final byte QUERY_EXTENSION = 98;
    public static final byte LIST_EXTENSIONS = 99;

    public static final byte CHANGE_KEYBOARD_MAPPING = 100;
    public static final byte GET_KEYBOARD_MAPPING = 101;
    public static final byte CHANGE_KEYBOARD_CONTROL = 102;
    public static final byte GET_KEYBOARD_CONTROL = 103;
    public static final byte BELL = 104;

    public static final byte CHANGE_POINTER_CONTROL = 105;
    public static final byte GET_POINTER_CONTROL = 106;

    public static final byte SET_SCREEN_SAVER = 107;
    public static final byte GET_SCREEN_SAVER = 108;

    public static final byte CHANGE_HOSTS = 109;
    public static final byte LIST_HOSTS = 110;
    public static final byte SET_ACCESS_CONTROL = 111;
    public static final byte SET_CLOSE_DOWN_MODE = 112;
    public static final byte KILL_CLIENT = 113;

    public static final byte ROTATE_PROPERTIES = 114;

    public static final byte FORCE_SCREEN_SAVER = 115;

    public static final byte SET_POINTER_MAPPING = 116;
    public static final byte GET_POINTER_MAPPING = 117;
    public static final byte SET_MODIFIER_MAPPING = 118;
    public static final byte GET_MODIFIER_MAPPING = 119;

    public static final byte NO_OP = 127;

    private static final String[] sNames = new String[256];

    public static void populateNames() {
        Class<Request> cls = Request.class;
        for (Field field : cls.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == byte.class) {
                try {
                    byte index = (byte) field.get(null);
                    sNames[index] = field.getName();
                } catch (IllegalAccessException e) {
                }
            }
        }
    }

    public static String getName(int what) {
        if (sNames[what] == null) {
            return "Unknown(" + what + ")";
        }
        return sNames[what];
    }
}
