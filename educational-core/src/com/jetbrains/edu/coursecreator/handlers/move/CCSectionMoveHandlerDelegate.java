package com.jetbrains.edu.coursecreator.handlers.move;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.move.MoveCallback;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.OpenApiExtKt;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.VirtualFileExt;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Section;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.jetbrains.edu.coursecreator.StudyItemType.SECTION_TYPE;

public class CCSectionMoveHandlerDelegate extends CCStudyItemMoveHandlerDelegate {

  public CCSectionMoveHandlerDelegate() {
    super(SECTION_TYPE);
  }

  @Override
  protected boolean isAvailable(@NotNull PsiDirectory directory) {
    return VirtualFileExt.isSectionDirectory(directory.getVirtualFile(), directory.getProject());
  }

  @Override
  public void doMove(final Project project,
                     PsiElement[] elements,
                     @Nullable PsiElement targetDirectory,
                     @Nullable MoveCallback callback) {
    if (!(targetDirectory instanceof PsiDirectory)) {
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }
    final PsiDirectory sourceDirectory = (PsiDirectory)elements[0];
    final Section sourceSection = course.getSection(sourceDirectory.getName());
    if (sourceSection == null) {
      throw new IllegalStateException("Failed to find section for `sourceVFile` directory");
    }

    StudyItem targetItem = course.getItem(((PsiDirectory)targetDirectory).getName());
    if (targetItem == null) {
      Messages.showInfoMessage(EduCoreBundle.message("dialog.message.incorrect.movement.section"),
                               EduCoreBundle.message("dialog.title.incorrect.target.for.move"));
      return;
    }

    final Integer delta = getDelta(project, targetItem);
    if (delta == null) {
      return;
    }

    int sourceSectionIndex = sourceSection.getIndex();
    sourceSection.setIndex(-1);

    final VirtualFile[] itemDirs = OpenApiExtKt.getCourseDir(project).getChildren();
    CCUtils.updateHigherElements(itemDirs, file -> course.getItem(file.getName()), sourceSectionIndex, -1);

    final int newItemIndex = targetItem.getIndex() + delta;
    CCUtils.updateHigherElements(itemDirs, file -> course.getItem(file.getName()), newItemIndex - 1, 1);

    sourceSection.setIndex(newItemIndex);
    course.sortItems();
    ProjectView.getInstance(project).refresh();
    YamlFormatSynchronizer.saveItem(course);
  }
}
