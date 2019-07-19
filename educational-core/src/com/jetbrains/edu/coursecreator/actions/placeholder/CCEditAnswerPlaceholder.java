package com.jetbrains.edu.coursecreator.actions.placeholder;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import org.jetbrains.annotations.NotNull;

public class CCEditAnswerPlaceholder extends CCAnswerPlaceholderAction {

  public CCEditAnswerPlaceholder() {
    super("Edit", "Edit answer placeholder");
  }

  @Override
  protected void performAnswerPlaceholderAction(@NotNull CCState state) {
    final Project project = state.getProject();
    PsiFile file = state.getFile();
    final PsiDirectory taskDir = file.getContainingDirectory();
    final PsiDirectory lessonDir = taskDir.getParent();
    if (lessonDir == null) return;
    AnswerPlaceholder answerPlaceholder = state.getAnswerPlaceholder();
    if (answerPlaceholder == null) {
      return;
    }
    performEditPlaceholder(project, answerPlaceholder);
  }

  public static void performEditPlaceholder(@NotNull Project project, @NotNull AnswerPlaceholder answerPlaceholder) {
    CCCreateAnswerPlaceholderDialog dlg = new CCCreateAnswerPlaceholderDialog(project, answerPlaceholder.getPlaceholderText(), true);

    if (dlg.showAndGet()) {
      final String answerPlaceholderText = dlg.getTaskText();
      answerPlaceholder.setPlaceholderText(answerPlaceholderText);
      YamlFormatSynchronizer.saveItem(answerPlaceholder.getTaskFile().getTask());
    }
  }

  @Override
  public void update(AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(false);
    CCState state = getState(e);
    if (state == null || state.getAnswerPlaceholder() == null) {
      return;
    }
    presentation.setEnabledAndVisible(true);
  }
}