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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.pocketworkstation.pckeyboard.DeadKeyHandler.Result;

public class DeadKeyHandlerTest {

    // Same values as android.view.KeyEvent.KEYCODE_*.
    private static final int KEYCODE_1 = 8;
    private static final int KEYCODE_6 = 13;
    private static final int KEYCODE_DPAD_LEFT = 21;
    private static final int KEYCODE_A = 29;
    private static final int KEYCODE_TAB = 61;
    private static final int KEYCODE_ENTER = 66;
    private static final int KEYCODE_GRAVE = 68;
    private static final int KEYCODE_FORWARD_DEL = 112;

    private static final int SHIFT = DeadKeyHandler.MOD_SHIFT;
    private static final int ALT = DeadKeyHandler.MOD_ALT;
    private static final int CTRL = DeadKeyHandler.MOD_CTRL;
    private static final int META = DeadKeyHandler.MOD_META;

    private DeadKeyHandler handler;

    @Before
    public void setUp() {
        handler = new DeadKeyHandler();
    }

    // Key presses on a US layout. The character passed as unicodeChar is the one
    // with Alt cleared, as LatinIME computes it.

    private static int keyCodeOf(char letter) {
        return KEYCODE_A + (letter - 'a');
    }

    private Result letter(char letter, int modifiers) {
        int ch = (modifiers & SHIFT) != 0 ? Character.toUpperCase(letter) : letter;
        return handler.onKeyDown(keyCodeOf(letter), letter, ch, modifiers, 0);
    }

    private Result capsLockLetter(char letter, int modifiers) {
        return handler.onKeyDown(keyCodeOf(letter), letter, Character.toUpperCase(letter),
                modifiers, 0);
    }

    private Result grave(int modifiers) {
        return handler.onKeyDown(KEYCODE_GRAVE, '`', (modifiers & SHIFT) != 0 ? '~' : '`',
                modifiers, 0);
    }

    private Result six(int modifiers) {
        return handler.onKeyDown(KEYCODE_6, '6', (modifiers & SHIFT) != 0 ? '^' : '6',
                modifiers, 0);
    }

    private Result space(int modifiers) {
        return handler.onKeyDown(DeadKeyHandler.KEYCODE_SPACE, ' ', ' ', modifiers, 0);
    }

    private Result key(int keyCode, int ch, int modifiers) {
        return handler.onKeyDown(keyCode, ch, ch, modifiers, 0);
    }

    /** A key on a Cyrillic layout: base character and character are both Cyrillic. */
    private Result cyrillic(char latinKey, char cyrillic, int modifiers) {
        return handler.onKeyDown(keyCodeOf(latinKey), cyrillic, cyrillic, modifiers, 0);
    }

    private static Result commit(String text) {
        return Result.commit(text);
    }

    private static Result commitThenPass(String text) {
        return Result.commitThenPass(text);
    }

    // Combinations

    @Test
    public void altU_deadDiaeresis() {
        assertEquals(Result.CONSUME, letter('u', ALT));
        assertTrue(handler.hasPendingAccent());
        assertEquals(commit("ä"), letter('a', 0));
        assertFalse(handler.hasPendingAccent());
    }

    @Test
    public void altE_deadAcute() {
        assertEquals(Result.CONSUME, letter('e', ALT));
        assertEquals(commit("é"), letter('e', 0));
    }

    @Test
    public void altI_deadCircumflex() {
        assertEquals(Result.CONSUME, letter('i', ALT));
        assertEquals(commit("î"), letter('i', 0));
    }

    @Test
    public void altN_deadTilde() {
        assertEquals(Result.CONSUME, letter('n', ALT));
        assertEquals(commit("ñ"), letter('n', 0));
    }

    @Test
    public void altGrave_deadGrave() {
        assertEquals(Result.CONSUME, grave(ALT));
        assertEquals(commit("à"), letter('a', 0));
    }

    @Test
    public void altShiftGrave_deadTilde() {
        assertEquals(Result.CONSUME, grave(ALT | SHIFT));
        assertEquals(commit("õ"), letter('o', 0));
    }

    @Test
    public void altShiftSix_deadCircumflex() {
        assertEquals(Result.CONSUME, six(ALT | SHIFT));
        assertEquals(commit("ô"), letter('o', 0));
    }

    @Test
    public void altShiftLetterDeadKeys_ignoreShift() {
        assertEquals(Result.CONSUME, letter('u', ALT | SHIFT));
        assertEquals(commit("ü"), letter('u', 0));
        assertEquals(Result.CONSUME, letter('e', ALT | SHIFT));
        assertEquals(commit("á"), letter('a', 0));
        assertEquals(Result.CONSUME, letter('i', ALT | SHIFT));
        assertEquals(commit("ê"), letter('e', 0));
        assertEquals(Result.CONSUME, letter('n', ALT | SHIFT));
        assertEquals(commit("ã"), letter('a', 0));
    }

    @Test
    public void altC_cedilla() {
        assertEquals(commit("ç"), letter('c', ALT));
        assertFalse(handler.hasPendingAccent());
    }

    @Test
    public void altShiftC_capitalCedilla() {
        assertEquals(commit("Ç"), letter('c', ALT | SHIFT));
    }

    @Test
    public void altCWithCapsLock_capitalCedilla() {
        assertEquals(commit("Ç"), capsLockLetter('c', ALT));
    }

    @Test
    public void altS_sharpS() {
        assertEquals(commit("ß"), letter('s', ALT));
        assertEquals(commit("ß"), capsLockLetter('s', ALT));
    }

    // Capitals

    @Test
    public void shiftAfterDeadKey_givesCapital() {
        letter('u', ALT);
        assertEquals(commit("Ä"), letter('a', SHIFT));
        letter('u', ALT);
        assertEquals(commit("Ü"), letter('u', SHIFT));
        letter('e', ALT);
        assertEquals(commit("É"), letter('e', SHIFT));
        letter('n', ALT);
        assertEquals(commit("Ñ"), letter('n', SHIFT));
    }

    @Test
    public void capsLockAfterDeadKey_givesCapital() {
        letter('u', ALT);
        assertEquals(commit("Ö"), capsLockLetter('o', 0));
    }

    @Test
    public void altHeldForNextLetter_isIgnored() {
        letter('u', ALT);
        assertEquals(commit("ö"), letter('o', ALT));
    }

    // Keys that are not handled

    @Test
    public void unmappedAltCombinations_pass() {
        assertEquals(Result.PASS, letter('s', ALT | SHIFT));
        assertEquals(Result.PASS, six(ALT));
        assertEquals(Result.PASS, letter('a', ALT));
        assertEquals(Result.PASS, letter('x', ALT | SHIFT));
        assertEquals(Result.PASS, space(ALT));
        assertEquals(Result.PASS, key(KEYCODE_1, '1', ALT));
        assertFalse(handler.hasPendingAccent());
    }

    @Test
    public void keysWithoutAlt_pass() {
        assertEquals(Result.PASS, letter('u', 0));
        assertEquals(Result.PASS, letter('u', SHIFT));
        assertEquals(Result.PASS, letter('c', SHIFT));
        assertEquals(Result.PASS, grave(0));
        assertEquals(Result.PASS, six(SHIFT));
        assertEquals(Result.PASS, space(0));
        assertEquals(Result.PASS, key(DeadKeyHandler.KEYCODE_DEL, 0, 0));
        assertEquals(Result.PASS, key(DeadKeyHandler.KEYCODE_ESCAPE, 0, 0));
        assertEquals(Result.PASS, key(KEYCODE_ENTER, '\n', 0));
        assertFalse(handler.hasPendingAccent());
    }

    // Composition

    @Test
    public void nonComposableCharacter_commitsSpacingAccentAndCharacter() {
        letter('e', ALT);
        assertEquals(commit("´x"), letter('x', 0));
        letter('u', ALT);
        assertEquals(commit("¨q"), letter('q', 0));
        six(ALT | SHIFT);
        assertEquals(commit("^k"), letter('k', 0));
        letter('n', ALT);
        assertEquals(commit("~B"), letter('b', SHIFT));
        grave(ALT);
        assertEquals(commit("`1"), key(KEYCODE_1, '1', 0));
        assertFalse(handler.hasPendingAccent());
    }

    @Test
    public void space_commitsSpacingAccent() {
        letter('u', ALT);
        assertEquals(commit("¨"), space(0));
        letter('e', ALT);
        assertEquals(commit("´"), space(0));
        letter('i', ALT);
        assertEquals(commit("^"), space(SHIFT));
        letter('n', ALT);
        assertEquals(commit("~"), space(0));
        grave(ALT);
        assertEquals(commit("`"), space(0));
        assertFalse(handler.hasPendingAccent());
    }

    @Test
    public void repeatedDeadKey_commitsPreviousAccentAndBecomesPending() {
        letter('u', ALT);
        assertEquals(commit("¨"), letter('e', ALT));
        assertTrue(handler.hasPendingAccent());
        assertEquals(commit("á"), letter('a', 0));

        letter('u', ALT);
        assertEquals(commit("¨"), letter('u', ALT));
        assertEquals(commit("ö"), letter('o', 0));

        grave(ALT);
        assertEquals(commit("`"), grave(ALT | SHIFT));
        assertEquals(commit("ñ"), letter('n', 0));
    }

    @Test
    public void altCOrAltSWhilePending_commitsSpacingAccentFirst() {
        letter('e', ALT);
        assertEquals(commit("´ç"), letter('c', ALT));
        letter('u', ALT);
        assertEquals(commit("¨ß"), letter('s', ALT));
        assertFalse(handler.hasPendingAccent());
    }

    @Test
    public void composesWithCyrillicCharacters() {
        letter('u', ALT);
        assertEquals(commit("ё"), cyrillic('t', 'е', 0));
        letter('e', ALT);
        assertEquals(commit("´ф"), cyrillic('a', 'ф', 0));
    }

    // Non-printable keys while an accent is pending

    @Test
    public void backspace_cancelsPendingAccent() {
        letter('u', ALT);
        assertEquals(Result.CONSUME, key(DeadKeyHandler.KEYCODE_DEL, 0, 0));
        assertFalse(handler.hasPendingAccent());
        assertEquals(Result.PASS, letter('a', 0));
    }

    @Test
    public void escape_cancelsPendingAccent() {
        letter('e', ALT);
        assertEquals(Result.CONSUME, key(DeadKeyHandler.KEYCODE_ESCAPE, 0, 0));
        assertFalse(handler.hasPendingAccent());
        assertEquals(Result.PASS, letter('a', 0));
    }

    @Test
    public void enterTabArrows_commitSpacingAccentThenPass() {
        letter('u', ALT);
        assertEquals(commitThenPass("¨"), key(KEYCODE_ENTER, '\n', 0));
        assertFalse(handler.hasPendingAccent());
        assertEquals(Result.PASS, handler.onKeyUp(KEYCODE_ENTER));

        letter('e', ALT);
        assertEquals(commitThenPass("´"), key(KEYCODE_TAB, '\t', 0));
        assertEquals(Result.PASS, handler.onKeyUp(KEYCODE_TAB));

        letter('i', ALT);
        assertEquals(commitThenPass("^"), key(KEYCODE_DPAD_LEFT, 0, SHIFT));
        assertEquals(Result.PASS, handler.onKeyUp(KEYCODE_DPAD_LEFT));
    }

    @Test
    public void otherNonPrintableKeys_commitSpacingAccentThenPass() {
        letter('n', ALT);
        assertEquals(commitThenPass("~"), key(KEYCODE_FORWARD_DEL, 0, 0));
        letter('u', ALT);
        assertEquals(commitThenPass("¨"),
                handler.onKeyDown(keyCodeOf('e'), 'e', DeadKeyHandler.COMBINING_ACCENT | 0x0301,
                        0, 0));
        assertFalse(handler.hasPendingAccent());
    }

    @Test
    public void loneModifiers_keepPendingAccent() {
        letter('u', ALT);
        assertEquals(Result.PASS, key(DeadKeyHandler.KEYCODE_SHIFT_LEFT, 0, SHIFT));
        assertEquals(Result.PASS, handler.onKeyUp(DeadKeyHandler.KEYCODE_SHIFT_LEFT));
        assertEquals(Result.PASS, key(DeadKeyHandler.KEYCODE_ALT_RIGHT, 0, ALT));
        assertEquals(Result.PASS, key(DeadKeyHandler.KEYCODE_CTRL_LEFT, 0, CTRL));
        assertEquals(Result.PASS, key(DeadKeyHandler.KEYCODE_CAPS_LOCK, 0, 0));
        assertEquals(Result.PASS, key(DeadKeyHandler.KEYCODE_META_LEFT, 0, META));
        assertTrue(handler.hasPendingAccent());
        assertEquals(commit("Ä"), letter('a', SHIFT));
    }

    // Cyrillic layouts and shortcuts

    @Test
    public void cyrillicLayout_passesThrough() {
        // Russian layout: U=г, E=у, I=ш, N=т, C=с, S=ы, `=ё; right Alt gives Latin.
        assertEquals(Result.PASS, cyrillic('u', 'г', ALT));
        assertEquals(Result.PASS, cyrillic('e', 'у', ALT));
        assertEquals(Result.PASS, cyrillic('i', 'ш', ALT));
        assertEquals(Result.PASS, cyrillic('n', 'т', ALT));
        assertEquals(Result.PASS, cyrillic('c', 'с', ALT | SHIFT));
        assertEquals(Result.PASS, cyrillic('s', 'ы', ALT));
        assertEquals(Result.PASS,
                handler.onKeyDown(KEYCODE_GRAVE, 'ё', 'ё', ALT, 0));
        assertEquals(Result.PASS,
                handler.onKeyDown(KEYCODE_GRAVE, 'ё', 'Ё', ALT | SHIFT, 0));
        assertFalse(handler.hasPendingAccent());
    }

    @Test
    public void ctrlOrMeta_passThrough() {
        assertEquals(Result.PASS, letter('u', ALT | CTRL));
        assertEquals(Result.PASS, letter('e', ALT | META));
        assertEquals(Result.PASS, letter('c', ALT | CTRL | SHIFT));
        assertEquals(Result.PASS, letter('s', ALT | META));
        assertEquals(Result.PASS, grave(ALT | CTRL));
        assertEquals(Result.PASS, space(CTRL));
        assertEquals(Result.PASS, letter('v', CTRL));
        assertFalse(handler.hasPendingAccent());
    }

    @Test
    public void ctrlShortcutWhilePending_commitsSpacingAccentThenPasses() {
        letter('u', ALT);
        assertEquals(commitThenPass("¨"), letter('v', CTRL));
        assertEquals(Result.PASS, handler.onKeyUp(keyCodeOf('v')));
        letter('e', ALT);
        assertEquals(commitThenPass("´"), space(CTRL));
        assertEquals(Result.PASS, handler.onKeyUp(DeadKeyHandler.KEYCODE_SPACE));
    }

    // Key-up and auto-repeat

    @Test
    public void keyUpOfConsumedKey_isConsumedOnce() {
        letter('u', ALT);
        assertEquals(Result.CONSUME, handler.onKeyUp(keyCodeOf('u')));
        assertEquals(Result.PASS, handler.onKeyUp(keyCodeOf('u')));

        letter('c', ALT);
        assertEquals(Result.CONSUME, handler.onKeyUp(keyCodeOf('c')));

        letter('e', ALT);
        letter('a', 0);
        assertEquals(Result.CONSUME, handler.onKeyUp(keyCodeOf('e')));
        assertEquals(Result.CONSUME, handler.onKeyUp(keyCodeOf('a')));

        letter('i', ALT);
        space(0);
        assertEquals(Result.CONSUME, handler.onKeyUp(keyCodeOf('i')));
        assertEquals(Result.CONSUME, handler.onKeyUp(DeadKeyHandler.KEYCODE_SPACE));

        letter('n', ALT);
        key(DeadKeyHandler.KEYCODE_DEL, 0, 0);
        assertEquals(Result.CONSUME, handler.onKeyUp(keyCodeOf('n')));
        assertEquals(Result.CONSUME, handler.onKeyUp(DeadKeyHandler.KEYCODE_DEL));
    }

    @Test
    public void keyUpOfPassedKeys_passes() {
        letter('a', ALT);
        assertEquals(Result.PASS, handler.onKeyUp(keyCodeOf('a')));
        assertEquals(Result.PASS, handler.onKeyUp(DeadKeyHandler.KEYCODE_ALT_RIGHT));
        assertEquals(Result.PASS, handler.onKeyUp(keyCodeOf('x')));
    }

    @Test
    public void autoRepeatOfDeadKey_isConsumedWithoutEffect() {
        int u = keyCodeOf('u');
        assertEquals(Result.CONSUME, handler.onKeyDown(u, 'u', 'u', ALT, 0));
        assertEquals(Result.CONSUME, handler.onKeyDown(u, 'u', 'u', ALT, 1));
        assertEquals(Result.CONSUME, handler.onKeyDown(u, 'u', 'u', ALT, 2));
        assertEquals(Result.CONSUME, handler.onKeyUp(u));
        assertEquals(commit("ä"), letter('a', 0));
    }

    @Test
    public void autoRepeatOfCommittedKey_isConsumedWithoutEffect() {
        int s = keyCodeOf('s');
        assertEquals(commit("ß"), handler.onKeyDown(s, 's', 's', ALT, 0));
        assertEquals(Result.CONSUME, handler.onKeyDown(s, 's', 's', ALT, 1));
        assertEquals(Result.CONSUME, handler.onKeyUp(s));

        letter('u', ALT);
        int a = keyCodeOf('a');
        assertEquals(commit("ä"), handler.onKeyDown(a, 'a', 'a', 0, 0));
        assertEquals(Result.CONSUME, handler.onKeyDown(a, 'a', 'a', 0, 1));
        assertEquals(Result.CONSUME, handler.onKeyUp(a));
        assertEquals(Result.PASS, handler.onKeyDown(a, 'a', 'a', 0, 0));
    }

    // Reset

    @Test
    public void reset_clearsPendingAccentAndConsumedKeys() {
        letter('u', ALT);
        handler.reset();
        assertFalse(handler.hasPendingAccent());
        assertEquals(Result.PASS, handler.onKeyUp(keyCodeOf('u')));
        assertEquals(Result.PASS, letter('a', 0));
    }
}
