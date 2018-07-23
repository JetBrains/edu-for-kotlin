package com.jetbrains.edu.python.learning;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PlatformUtils;
import com.jetbrains.edu.learning.EduConfigurator;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.python.learning.pycharm.PyTaskCheckerProvider;
import com.jetbrains.python.newProject.PyNewProjectSettings;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class PyConfigurator implements EduConfigurator<PyNewProjectSettings> {

  private static final String COURSE_NAME = "Introduction to Python.zip";

  public static final String PYTHON_3 = "3.x";
  public static final String PYTHON_2 = "2.x";
  public static final String TESTS_PY = "tests.py";
  public static final String TASK_PY = "task.py";

  private final PyCourseBuilder myCourseBuilder = new PyCourseBuilder();

  @NotNull
  @Override
  public EduCourseBuilder<PyNewProjectSettings> getCourseBuilder() {
    return myCourseBuilder;
  }

  @NotNull
  @Override
  public String getTestFileName() {
    return TESTS_PY;
  }

  @Override
  public boolean excludeFromArchive(@NotNull Project project, @NotNull String path) {
    return path.contains("__pycache__") || path.endsWith(".pyc");
  }

  @Override
  public boolean isTestFile(VirtualFile file) {
    return TESTS_PY.equals(file.getName());
  }

  @Override
  public List<String> getBundledCoursePaths() {
    File bundledCourseRoot = EduUtils.getBundledCourseRoot(COURSE_NAME, PyConfigurator.class);
    return Collections.singletonList(FileUtil.join(bundledCourseRoot.getAbsolutePath(), COURSE_NAME));
  }

  @Override
  public boolean isEnabled() {
    return !(PlatformUtils.isPyCharm() || PlatformUtils.isCLion());
  }

  @NotNull
  @Override
  public TaskCheckerProvider getTaskCheckerProvider() {
    return new PyTaskCheckerProvider();
  }
}
