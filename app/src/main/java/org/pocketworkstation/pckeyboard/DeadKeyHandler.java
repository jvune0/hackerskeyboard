/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.pocketworkstation.pckeyboard;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;

/**
 * Alt-based dead keys for hardware keyboards.
 *
 * <p>Has no Android dependencies so it can be unit tested on the JVM. The caller
 * passes only events from physical keyboards, extracts the key code, characters
 * and modifiers from the {@code KeyEvent}, and applies the returned {@link Result}.
 *
 * <p>Combinations are matched by the key's base character in the current layout,
 * so they only apply to Latin layouts:
 * <ul>
 * <li>Alt+U: dead diaeresis (U+0308)
 * <li>Alt+E: dead acute (U+0301)
 * <li>Alt+I, Alt+Shift+6: dead circumflex (U+0302)
 * <li>Alt+N, Alt+Shift+`: dead tilde (U+0303)
 * <li>Alt+`: dead grave (U+0300)
 * <li>Alt+C: ç, or Ç if the key's character is upper case (Shift or Caps Lock)
 * <li>Alt+S: ß
 * </ul>
 * Shift is ignored for Alt+U/E/I/N. Alt+Shift+S and Alt+6 are not handled.
 */
public final class DeadKeyHandler {

    public static final int MOD_SHIFT = 1;
    public static final int MOD_ALT = 1 << 1;
    public static final int MOD_CTRL = 1 << 2;
    public static final int MOD_META = 1 << 3;

    // Same values as android.view.KeyEvent.KEYCODE_*.
    static final int KEYCODE_ALT_LEFT = 57;
    static final int KEYCODE_ALT_RIGHT = 58;
    static final int KEYCODE_SHIFT_LEFT = 59;
    static final int KEYCODE_SHIFT_RIGHT = 60;
    static final int KEYCODE_SPACE = 62;
    static final int KEYCODE_SYM = 63;
    static final int KEYCODE_DEL = 67;
    static final int KEYCODE_ESCAPE = 111;
    static final int KEYCODE_CTRL_LEFT = 113;
    static final int KEYCODE_CTRL_RIGHT = 114;
    static final int KEYCODE_CAPS_LOCK = 115;
    static final int KEYCODE_SCROLL_LOCK = 116;
    static final int KEYCODE_META_LEFT = 117;
    static final int KEYCODE_META_RIGHT = 118;
    static final int KEYCODE_FUNCTION = 119;
    static final int KEYCODE_NUM_LOCK = 143;

    /** Same value as android.view.KeyCharacterMap.COMBINING_ACCENT. */
    static final int COMBINING_ACCENT = 0x80000000;

    private static final char COMBINING_GRAVE = '\u0300';
    private static final char COMBINING_ACUTE = '\u0301';
    private static final char COMBINING_CIRCUMFLEX = '\u0302';
    private static final char COMBINING_TILDE = '\u0303';
    private static final char COMBINING_DIAERESIS = '\u0308';

    /** What the caller should do with a key event. */
    public static final class Result {
        public enum Action {
            /** Not handled: let the existing code process the event. */
            PASS,
            /** Swallow the event. */
            CONSUME,
            /** Commit {@link Result#text} and swallow the event. */
            COMMIT,
            /** Commit {@link Result#text}, then let the existing code process the event. */
            COMMIT_THEN_PASS
        }

        public static final Result PASS = new Result(Action.PASS, null);
        public static final Result CONSUME = new Result(Action.CONSUME, null);

        public final Action action;
        /** Text to commit; non-null for COMMIT and COMMIT_THEN_PASS. */
        public final String text;

        private Result(Action action, String text) {
            this.action = action;
            this.text = text;
        }

        static Result commit(String text) {
            return new Result(Action.COMMIT, text);
        }

        static Result commitThenPass(String text) {
            return new Result(Action.COMMIT_THEN_PASS, text);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Result)) return false;
            Result other = (Result) o;
            return action == other.action
                    && (text == null ? other.text == null : text.equals(other.text));
        }

        @Override
        public int hashCode() {
            return action.hashCode() * 31 + (text == null ? 0 : text.hashCode());
        }

        @Override
        public String toString() {
            return text == null ? action.toString() : action + "(" + text + ")";
        }
    }

    /** Pending combining mark, or 0. */
    private char mPendingAccent;
    /** Keys whose keyDown was consumed; their auto-repeats and keyUp are consumed too. */
    private final Set<Integer> mConsumedKeys = new HashSet<Integer>();

    /**
     * @param keyCode     event.getKeyCode()
     * @param baseChar    event.getUnicodeChar(0): the key's unmodified character
     * @param unicodeChar event.getUnicodeChar(metaState with Alt bits cleared):
     *                    honors Shift and Caps Lock; 0 if the key is not printable
     * @param modifiers   MOD_* flags
     * @param repeatCount event.getRepeatCount()
     */
    public Result onKeyDown(int keyCode, int baseChar, int unicodeChar, int modifiers,
            int repeatCount) {
        if (repeatCount > 0 && mConsumedKeys.contains(keyCode)) {
            return Result.CONSUME;
        }
        Result result = handleKeyDown(keyCode, baseChar, unicodeChar, modifiers);
        if (result.action == Result.Action.CONSUME || result.action == Result.Action.COMMIT) {
            mConsumedKeys.add(keyCode);
        }
        return result;
    }

    /** CONSUME if this key's keyDown was consumed (then forgets it), otherwise PASS. */
    public Result onKeyUp(int keyCode) {
        return mConsumedKeys.remove(keyCode) ? Result.CONSUME : Result.PASS;
    }

    /** Drops the pending accent and the consumed-key set (onStartInput / onFinishInput). */
    public void reset() {
        mPendingAccent = 0;
        mConsumedKeys.clear();
    }

    public boolean hasPendingAccent() {
        return mPendingAccent != 0;
    }

    private Result handleKeyDown(int keyCode, int baseChar, int unicodeChar, int modifiers) {
        if (isModifierKey(keyCode)) {
            return Result.PASS;
        }
        boolean shift = (modifiers & MOD_SHIFT) != 0;
        boolean ctrlOrMeta = (modifiers & (MOD_CTRL | MOD_META)) != 0;

        if ((modifiers & MOD_ALT) != 0 && !ctrlOrMeta) {
            char accent = deadAccentFor(baseChar, shift);
            if (accent != 0) {
                String previous = takePendingSpacing();
                mPendingAccent = accent;
                return previous.isEmpty() ? Result.CONSUME : Result.commit(previous);
            }
            String text = altCharFor(baseChar, unicodeChar, shift);
            if (text != null) {
                return Result.commit(takePendingSpacing() + text);
            }
        }

        if (mPendingAccent == 0) {
            return Result.PASS;
        }
        if (keyCode == KEYCODE_DEL || keyCode == KEYCODE_ESCAPE) {
            mPendingAccent = 0;
            return Result.CONSUME;
        }
        if (ctrlOrMeta) {
            return Result.commitThenPass(takePendingSpacing());
        }
        if (keyCode == KEYCODE_SPACE || unicodeChar == ' ') {
            return Result.commit(takePendingSpacing());
        }
        if (!isPrintable(unicodeChar)) {
            return Result.commitThenPass(takePendingSpacing());
        }
        return Result.commit(compose(unicodeChar));
    }

    private static char deadAccentFor(int baseChar, boolean shift) {
        switch (baseChar) {
            case 'u': return COMBINING_DIAERESIS;
            case 'e': return COMBINING_ACUTE;
            case 'i': return COMBINING_CIRCUMFLEX;
            case 'n': return COMBINING_TILDE;
            case '`': return shift ? COMBINING_TILDE : COMBINING_GRAVE;
            case '6': return shift ? COMBINING_CIRCUMFLEX : 0;
            default: return 0;
        }
    }

    private static String altCharFor(int baseChar, int unicodeChar, boolean shift) {
        switch (baseChar) {
            case 'c': return unicodeChar == 'C' ? "Ç" : "ç";
            case 's': return shift ? null : "ß";
            default: return null;
        }
    }

    /** The character composed with the pending accent, or spacing accent + character. */
    private String compose(int ch) {
        String character = new String(Character.toChars(ch));
        String composed = Normalizer.normalize(character + mPendingAccent, Normalizer.Form.NFC);
        String spacing = takePendingSpacing();
        if (composed.codePointCount(0, composed.length()) == 1) {
            return composed;
        }
        return spacing + character;
    }

    /** Clears the pending accent and returns its spacing form, or "" if none. */
    private String takePendingSpacing() {
        String spacing = spacingAccent(mPendingAccent);
        mPendingAccent = 0;
        return spacing;
    }

    private static String spacingAccent(char accent) {
        switch (accent) {
            case COMBINING_GRAVE: return "`";
            case COMBINING_ACUTE: return "´";
            case COMBINING_CIRCUMFLEX: return "^";
            case COMBINING_TILDE: return "~";
            case COMBINING_DIAERESIS: return "¨";
            default: return "";
        }
    }

    /** False for 0, control characters and values flagged COMBINING_ACCENT. */
    private static boolean isPrintable(int ch) {
        return ch > 0 && Character.isValidCodePoint(ch) && !Character.isISOControl(ch);
    }

    private static boolean isModifierKey(int keyCode) {
        switch (keyCode) {
            case KEYCODE_ALT_LEFT:
            case KEYCODE_ALT_RIGHT:
            case KEYCODE_SHIFT_LEFT:
            case KEYCODE_SHIFT_RIGHT:
            case KEYCODE_SYM:
            case KEYCODE_CTRL_LEFT:
            case KEYCODE_CTRL_RIGHT:
            case KEYCODE_CAPS_LOCK:
            case KEYCODE_SCROLL_LOCK:
            case KEYCODE_META_LEFT:
            case KEYCODE_META_RIGHT:
            case KEYCODE_FUNCTION:
            case KEYCODE_NUM_LOCK:
                return true;
            default:
                return false;
        }
    }
}
