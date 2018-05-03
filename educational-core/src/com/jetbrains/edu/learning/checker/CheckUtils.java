package com.jetbrains.edu.learning.checker;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.jetbrains.edu.learning.EduState;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.editor.EduEditor;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import com.jetbrains.edu.learning.ui.OutputToolWindowFactory;
import com.jetbrains.edu.learning.ui.OutputToolWindowFactoryKt;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class CheckUtils {
  public static final String STUDY_PREFIX = "#educational_plugin";
  public static final List<String> COMPILATION_ERRORS = CollectionsKt.listOf("Compilation failed", "Compilation error");
  public static final String COMPILATION_FAILED_MESSAGE = "Compilation failed";
  public static final String NOT_RUNNABLE_MESSAGE = "Solution isn't runnable";
  public static final String LOGIN_NEEDED_MESSAGE = "Please, login to Stepik to check the task";
  public static final String FAILED_TO_CHECK_MESSAGE = "Failed to launch checking";

  private static final Logger LOG = Logger.getInstance(CheckUtils.class);

  private CheckUtils() {
  }

  public static void navigateToFailedPlaceholder(@NotNull final EduState eduState,
                                                 @NotNull final Task task,
                                                 @NotNull final VirtualFile taskDir,
                                                 @NotNull final Project project) {
    TaskFile selectedTaskFile = eduState.getTaskFile();
    if (selectedTaskFile == null) return;
    Editor editor = eduState.getEditor();
    TaskFile taskFileToNavigate = selectedTaskFile;
    VirtualFile fileToNavigate = eduState.getVirtualFile();
    final StudyTaskManager studyTaskManager = StudyTaskManager.getInstance(project);
    if (!studyTaskManager.hasFailedAnswerPlaceholders(selectedTaskFile)) {
      for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
        TaskFile taskFile = entry.getValue();
        if (studyTaskManager.hasFailedAnswerPlaceholders(taskFile)) {
          taskFileToNavigate = taskFile;
          VirtualFile virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir);
          if (virtualFile == null) {
            continue;
          }
          FileEditor fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile);
          if (fileEditor instanceof EduEditor) {
            EduEditor eduEditor = (EduEditor)fileEditor;
            editor = eduEditor.getEditor();
          }
          fileToNavigate = virtualFile;
          break;
        }
      }
    }
    if (fileToNavigate != null) {
      FileEditorManager.getInstance(project).openFile(fileToNavigate, true);
    }
    if (editor == null) {
      return;
    }
    final Editor editorToNavigate = editor;
    ApplicationManager.getApplication().invokeLater(
      () -> IdeFocusManager.getInstance(project).requestFocus(editorToNavigate.getContentComponent(), true));

    NavigationUtils.navigateToFirstFailedAnswerPlaceholder(editor, taskFileToNavigate);
  }


  public static void showTestResultPopUp(@NotNull final String text, Color color, @NotNull final Project project) {
    String escapedText = StringUtil.escapeXml(text);
    BalloonBuilder balloonBuilder =
      JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(escapedText, null, color, null);
    final Balloon balloon = balloonBuilder.createBalloon();
    EduUtils.showCheckPopUp(project, balloon);
  }


  public static void flushWindows(@NotNull final Task task, @NotNull final VirtualFile taskDir) {
    for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
      TaskFile taskFile = entry.getValue();
      VirtualFile virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir);
      if (virtualFile == null) {
        continue;
      }
      EduUtils.flushWindows(taskFile, virtualFile);
    }
  }

  public static void showOutputToolWindow(@NotNull final Project project, @NotNull final String message) {
    showToolWindow(project, OutputToolWindowFactoryKt.OUTPUT_TOOLWINDOW_ID, message, ConsoleViewContentType.NORMAL_OUTPUT);
  }

  public static void showTestResultsToolWindow(@NotNull final Project project, @NotNull final String message) {
    showToolWindow(project, OutputToolWindowFactoryKt.TEST_RESULTS_ID, message, ConsoleViewContentType.ERROR_OUTPUT);
  }

  private static void showToolWindow(
          @NotNull final Project project,
          @NotNull String id,
          @NotNull final String message,
          @NotNull ConsoleViewContentType type
  ) {
    ApplicationManager.getApplication().invokeLater(() -> {
      final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
      ToolWindow window = toolWindowManager.getToolWindow(id);
      if (window == null) {
        toolWindowManager.registerToolWindow(id, true, ToolWindowAnchor.BOTTOM);
        window = toolWindowManager.getToolWindow(id);
        new OutputToolWindowFactory().createToolWindowContent(project, window);
      }

      final Content[] contents = window.getContentManager().getContents();
      for (Content content : contents) {
        final JComponent component = content.getComponent();
        if (component instanceof ConsoleViewImpl) {
          ((ConsoleViewImpl)component).clear();
          ((ConsoleViewImpl)component).print(message, type);
          window.setAvailable(true,null);
          window.show(null);
        }
      }
    });
  }

  public static TestsOutputParser.TestsOutput getTestOutput(@NotNull Process testProcess,
                                                            @NotNull String commandLine,
                                                            boolean isAdaptive) {
    final CapturingProcessHandler handler = new CapturingProcessHandler(testProcess, null, commandLine);
    final ProcessOutput output = ProgressManager.getInstance().hasProgressIndicator() ? handler
      .runProcessWithProgressIndicator(ProgressManager.getInstance().getProgressIndicator()) :
                                 handler.runProcess();
    final TestsOutputParser.TestsOutput testsOutput = TestsOutputParser.getTestsOutput(output, isAdaptive);
    String stderr = output.getStderr();
    if (!stderr.isEmpty() && output.getStdout().isEmpty()) {
      LOG.info("#educational " + stderr);
      return new TestsOutputParser.TestsOutput(false, stderr);
    }
    return testsOutput;
  }

  public static void hideTestResultsToolWindow(@NotNull Project project) {
    ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(OutputToolWindowFactoryKt.TEST_RESULTS_ID);
    if (toolWindow != null) {
      toolWindow.hide(() -> {});
    }
  }

  @Nullable
  public static RunnerAndConfigurationSettings createDefaultRunConfiguration(@NotNull Project project) {
    return ApplicationManager.getApplication().runReadAction((Computable<RunnerAndConfigurationSettings>) () -> {
      Editor editor = EduUtils.getSelectedEditor(project);
      if (editor == null) return null;
      JComponent editorComponent = editor.getComponent();
      DataContext dataContext = DataManager.getInstance().getDataContext(editorComponent);
      return ConfigurationContext.getFromContext(dataContext).getConfiguration();
    });
  }

  public static boolean hasCompilationErrors(ProcessOutput processOutput) {
    for (String error : COMPILATION_ERRORS) {
      if (processOutput.getStderr().contains(error)) return true;
    }
    return false;
  }
}