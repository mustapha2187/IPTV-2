package com.musta.iptv.ui.enigma

import android.view.KeyEvent

/** Remote control key codes. Colored keys also answer to 1 2 3 4 for remotes that have no colored keys. */
object Rc {
    const val UP = KeyEvent.KEYCODE_DPAD_UP
    const val DOWN = KeyEvent.KEYCODE_DPAD_DOWN
    const val LEFT = KeyEvent.KEYCODE_DPAD_LEFT
    const val RIGHT = KeyEvent.KEYCODE_DPAD_RIGHT
    const val CENTER = KeyEvent.KEYCODE_DPAD_CENTER
    const val ENTER = KeyEvent.KEYCODE_ENTER
    const val MENU = KeyEvent.KEYCODE_MENU
    const val INFO = KeyEvent.KEYCODE_INFO
    const val CH_UP = KeyEvent.KEYCODE_CHANNEL_UP
    const val CH_DOWN = KeyEvent.KEYCODE_CHANNEL_DOWN

    fun isRed(code: Int) = code == KeyEvent.KEYCODE_PROG_RED || code == KeyEvent.KEYCODE_1
    fun isGreen(code: Int) = code == KeyEvent.KEYCODE_PROG_GREEN || code == KeyEvent.KEYCODE_2
    fun isYellow(code: Int) = code == KeyEvent.KEYCODE_PROG_YELLOW || code == KeyEvent.KEYCODE_3
    fun isBlue(code: Int) = code == KeyEvent.KEYCODE_PROG_BLUE || code == KeyEvent.KEYCODE_4
    fun isOk(code: Int) = code == CENTER || code == ENTER
    fun isActions(code: Int) = code == MENU || code == INFO
}
