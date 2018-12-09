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

enum class Request(val code: Byte) {
    CREATE_WINDOW(1),
    CHANGE_WINDOW_ATTRIBUTES(2),
    GET_WINDOW_ATTRIBUTES(3),
    DESTROY_WINDOW(4),
    DESTROY_SUBWINDOWS(5),
    CHANGE_SAVE_SET(6),
    REPARENT_WINDOW(7),
    MAP_WINDOW(8),
    MAP_SUBWINDOWS(9),
    UNMAP_WINDOW(10),
    UNMAP_SUBWINDOWS(11),
    CONFIGURE_WINDOW(12),
    CIRCULATE_WINDOW(13),
    GET_GEOMETRY(14),
    QUERY_TREE(15),

    INTERN_ATOM(16),
    GET_ATOM_NAME(17),

    CHANGE_PROPERTY(18),
    DELETE_PROPERTY(19),
    GET_PROPERTY(20),
    LIST_PROPERTIES(21),

    SET_SELECTION_OWNER(22),
    GET_SELECTION_OWNER(23),
    CONVERT_SELECTION(24),

    SEND_EVENT(25),

    GRAB_POINTER(26),
    UNGRAB_POINTER(27),
    GRAB_BUTTON(28),
    UNGRAB_BUTTON(29),
    CHANGE_ACTIVE_POINTER_GRAB(30),
    GRAB_KEYBOARD(31),
    UNGRAB_KEYBOARD(32),
    GRAB_KEY(33),
    UNGRAB_KEY(34),
    ALLOW_EVENTS(35),
    GRAB_SERVER(36),
    UNGRAB_SERVER(37),

    QUERY_POINTER(38),
    GET_MOTION_EVENTS(39),
    TRANSLATE_COORDINATES(40),
    WRAP_POINTER(41),
    SET_INPUT_FOCUS(42),
    GET_INPUT_FOCUS(43),

    QUERY_KEYMAP(44),
    OPEN_FONT(45),
    CLOSE_FONT(46),
    QUERY_FONT(47),
    QUERY_TEXT_EXTENTS(48),
    LIST_FONTS(49),
    LIST_FONTS_WITH_INFO(50),
    SET_FONT_PATH(51),
    GET_FONT_PATH(52),

    CREATE_PIXMAP(53),
    FREE_PIXMAP(54),
    CREATE_GC(55),
    CHANGE_GC(56),
    COPY_GC(57),
    SET_DASHES(58),
    SET_CLIP_RECTANGLES(59),
    FREE_GC(60),

    CLEAR_AREA(61),
    COPY_AREA(62),
    COPY_PLANE(63),
    POLY_POINT(64),
    POLY_LINE(65),
    POLY_SEGMENT(66),
    POLY_RECTANGLE(67),
    POLY_ARC(68),
    FILL_POLY(69),
    POLY_FILL_RECTANGLE(70),
    POLY_FILL_ARC(71),

    PUT_IMAGE(72),
    GET_IMAGE(73),
    POLY_TEXT_8(74),
    POLY_TEXT_16(75),
    IMAGE_TEXT_8(76),
    IMAGE_TEXT_16(77),

    CREATE_COLORMAP(78),
    FREE_COLORMAP(79),
    COPY_COLORMAP_AND_FREE(80),
    INSTALL_COLORMAP(81),
    UNINSTALL_COLORMAP(82),
    LIST_INSTALLED_COLORMAP(83),
    ALLOC_COLOR(84),
    ALLOC_NAMED_COLOR(85),
    ALLOC_COLOR_CELLS(86),
    ALLOC_COLOR_PLANES(87),
    FREE_COLORS(88),
    STORE_COLORS(89),
    STORE_NAMED_COLOR(90),
    QUERY_COLORS(91),
    LOOKUP_COLOR(92),

    CREATE_CURSOR(93),
    CREATE_GLYPH_CURSOR(94),
    FREE_CURSOR(95),
    RECOLOR_CURSOR(96),
    QUERY_BEST_SIZE(97),

    QUERY_EXTENSION(98),
    LIST_EXTENSIONS(99),

    CHANGE_KEYBOARD_MAPPING(100),
    GET_KEYBOARD_MAPPING(101),
    CHANGE_KEYBOARD_CONTROL(102),
    GET_KEYBOARD_CONTROL(103),
    BELL(104),

    CHANGE_POINTER_CONTROL(105),
    GET_POINTER_CONTROL(106),

    SET_SCREEN_SAVER(107),
    GET_SCREEN_SAVER(108),

    CHANGE_HOSTS(109),
    LIST_HOSTS(110),
    SET_ACCESS_CONTROL(111),
    SET_CLOSE_DOWN_MODE(112),
    KILL_CLIENT(113),

    ROTATE_PROPERTIES(114),

    FORCE_SCREEN_SAVER(115),

    SET_POINTER_MAPPING(116),
    GET_POINTER_MAPPING(117),
    SET_MODIFIER_MAPPING(118),
    GET_MODIFIER_MAPPING(119),

    NO_OP(127);

    companion object {


        private val sNames = arrayOfNulls<String>(256)

        fun populateNames() {
            enumValues<Request>().forEach {
                sNames[it.code.toInt()] = it.name
            }
        }

        fun getName(what: Int): String? {
            return if (sNames[what] == null) {
                "Unknown($what)"
            } else sNames[what]
        }
    }
}
