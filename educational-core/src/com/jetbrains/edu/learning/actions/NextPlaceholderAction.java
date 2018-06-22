package com.jetbrains.edu.learning.actions;

import com.intellij.icons.AllIcons;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * move caret to next task window
 */
public class NextPlaceholderAction extends PlaceholderNavigationAction {
  public static final String ACTION_ID = "Educational.NextPlaceholder";
  public static final String SHORTCUT = "ctrl shift pressed PERIOD";
  public static final String SHORTCUT2 = "ctrl pressed ENTER";

  public NextPlaceholderAction() {
    super("Navigate to the Next Answer Placeholder", "Navigate to the next answer placeholder", AllIcons.Actions.Forward);
  }

  @Nullable
  @Override
  protected AnswerPlaceholder getTargetPlaceholder(@NotNull final TaskFile taskFile, int offset) {
    final AnswerPlaceholder selectedAnswerPlaceholder = taskFile.getAnswerPlaceholder(offset);
    final List<AnswerPlaceholder> placeholders = taskFile.getAnswerPlaceholders();
    int startIndex = selectedAnswerPlaceholder != null ? selectedAnswerPlaceholder.getIndex() + 1 : 0;
    if (!EduUtils.indexIsValid(startIndex, placeholders)) return null;

    for (AnswerPlaceholder placeholder : placeholders.subList(startIndex, placeholders.size())) {
      if (placeholder.getOffset() > offset && placeholder.isVisible()) {
        return placeholder;
      }
    }

    return null;
  }

  @NotNull
  @Override
  public String getActionId() {
    return ACTION_ID;
  }

  @Nullable
  @Override
  public String[] getShortcuts() {
    return new String[]{SHORTCUT, SHORTCUT2};
  }
}
