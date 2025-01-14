/*
 * Copyright (C) 2023 Google Inc.
 *
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

package com.google.android.accessibility.braille.common;

import static android.view.accessibility.AccessibilityNodeInfo.FOCUS_ACCESSIBILITY;
import static android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT;

import android.content.Context;
import android.widget.EditText;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils.SpellingSuggestion;
import com.google.android.accessibility.utils.FocusFinder;
import com.google.android.accessibility.utils.FocusFinder.FocusType;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Arrays;

/**
 * Handles typo correction and provide necessary result for user selecting for TalkBack corrects it.
 */
public class BrailleTypoFinder {
  private static final int NONE_INITIALIZED = -1;
  private final FocusFinder focusFinder;
  private AccessibilityNodeInfoCompat targetNode;
  private SpellingSuggestion spellingSuggestion;
  private String[] suggestionCandidates;
  private int suggestionSpanFlag;
  private int index;

  public BrailleTypoFinder(FocusFinder focusFinder) {
    this.focusFinder = focusFinder;
  }

  /**
   * Updates the typo correction from given focus type.
   *
   * @param focusType defines find typo correction from which focus.
   * @return true if there has typo correction candidate.
   */
  @CanIgnoreReturnValue
  public boolean updateTypoCorrectionFrom(Context context, @FocusType int focusType) {
    clear();
    switch (focusType) {
      case FOCUS_ACCESSIBILITY:
        AccessibilityNodeInfoCompat node = focusFinder.findAccessibilityFocus();
        if (node != null
            && node.isFocused()
            && AccessibilityNodeInfoUtils.nodeMatchesClassByType(node, EditText.class)) {
          return obtainSuggestions(context, node);
        }
        return false;
      case FOCUS_INPUT:
        return obtainSuggestions(context, focusFinder.findFocusCompat(FOCUS_INPUT));
      default:
        return false;
    }
  }

  /** Gets the next targeted suggestion candidate. */
  public String nextSuggestion() throws NoTypoFocusFoundException {
    if (suggestionCandidates == null) {
      throw new NoTypoFocusFoundException();
    }
    if (index == NONE_INITIALIZED) {
      index = 0;
    } else {
      index++;
      if (index >= suggestionCandidates.length) {
        index = 0;
      }
    }
    return suggestionCandidates[index];
  }

  /** Gets the previous targeted suggestion candidate. */
  public String previousSuggestion() throws NoTypoFocusFoundException {
    if (suggestionCandidates == null) {
      throw new NoTypoFocusFoundException();
    }
    if (index == NONE_INITIALIZED) {
      index = 0;
    } else {
      index--;
      if (index < 0) {
        index = suggestionCandidates.length - 1;
      }
    }
    return suggestionCandidates[index];
  }

  /** Clears the cache fields. Usually used when typo suggestion has confirmed. */
  public void clear() {
    index = NONE_INITIALIZED;
    targetNode = null;
    spellingSuggestion = null;
    suggestionCandidates = null;
  }

  /** Gets the current targeted suggestion candidate. */
  public String getCurrentSuggestionCandidate() throws NoTypoFocusFoundException {
    if (suggestionCandidates == null) {
      throw new NoTypoFocusFoundException();
    }
    if (index == NONE_INITIALIZED) {
      return "";
    }
    return suggestionCandidates[index];
  }

  /** Gets the suggestion candidates. */
  public ImmutableList<String> getSuggestionCandidates() throws NoTypoFocusFoundException {
    if (suggestionCandidates == null) {
      throw new NoTypoFocusFoundException();
    }
    return ImmutableList.copyOf(Arrays.asList(suggestionCandidates));
  }

  /**
   * Gets the {@link AccessibilityNodeInfoCompat} found from called {@link
   * BrailleTypoFinder#updateTypoCorrectionFrom(Context, int)}.
   */
  public AccessibilityNodeInfoCompat getTargetNode() {
    return targetNode;
  }

  /**
   * Gets the closest {@link SpellingSuggestion} found from the {@link AccessibilityNodeInfoCompat}.
   */
  public SpellingSuggestion getSpellingSuggestion() {
    return spellingSuggestion;
  }

  /** Returns the SuggestionSpanFlag of current suggestion. */
  public int getSuggestionSpanFlag() {
    return suggestionSpanFlag;
  }

  private boolean obtainSuggestions(Context context, AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return false;
    }
    ImmutableList<SpellingSuggestion> spellingSuggestions =
        AccessibilityNodeInfoUtils.getSpellingSuggestions(context, node);
    if (spellingSuggestions.isEmpty()) {
      return false;
    }
    SpellingSuggestion spellingSuggestion = spellingSuggestions.get(/* index= */ 0);
    if (spellingSuggestion.suggestionSpan() == null) {
      return false;
    }
    targetNode = node;
    this.spellingSuggestion = spellingSuggestion;
    suggestionCandidates = spellingSuggestion.suggestionSpan().getSuggestions();
    suggestionSpanFlag = spellingSuggestion.suggestionSpan().getFlags();
    return suggestionCandidates.length != 0;
  }

  /**
   * An {@link Exception} throws when the method called before interact with a typo or the focused
   * typo was handled.
   */
  public static class NoTypoFocusFoundException extends Exception {
    public NoTypoFocusFoundException() {
      super("You have to call updateTypoCorrectionFrom(int) in advance!");
    }
  }
}
