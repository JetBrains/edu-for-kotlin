package com.jetbrains.edu.coursecreator.actions;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.OpenApiExtKt;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizerKt;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.learning.EduUtils.addMnemonic;

public class CCChangeCourseInfo extends DumbAwareAction {

  public CCChangeCourseInfo() {
    super(EduCoreBundle.lazyMessage("action.edit.course.information.text"),
          EduCoreBundle.lazyMessage("action.edit.course.information.description"), null);
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    final Project project = event.getProject();
    final Presentation presentation = event.getPresentation();
    if (project == null) {
      return;
    }
    presentation.setEnabledAndVisible(false);
    if (!CCUtils.isCourseCreator(project)) {
      return;
    }
    final IdeView view = event.getData(LangDataKeys.IDE_VIEW);
    if (view == null) {
      return;
    }
    final PsiDirectory[] directories = view.getDirectories();
    if (directories.length == 0) {
      return;
    }
    presentation.setEnabledAndVisible(true);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }

    String configFileName = YamlFormatSynchronizerKt.getConfigFileName(course);
    VirtualFile configFile = OpenApiExtKt.getCourseDir(project).findChild(configFileName);
    if (configFile == null) {
      Logger.getInstance(CCChangeCourseInfo.class).error("Failed to find course config file");
      return;
    }

    FileEditorManager.getInstance(project).openFile(configFile, true);
  }
}
