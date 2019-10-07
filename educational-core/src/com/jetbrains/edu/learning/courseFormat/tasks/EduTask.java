package com.jetbrains.edu.learning.courseFormat.tasks;

import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Original Edu plugin tasks with local tests and answer placeholders
 */
public class EduTask extends Task {

  public EduTask() {
  }

  public EduTask(@NotNull String name) {
    super(name);
  }

  @Override
  public String getItemType() {
    return "edu";
  }

  @Override
  public boolean isToSubmitToStepik() {
    return myStatus != CheckStatus.Unchecked;
  }

  @Override
  public boolean supportSubmissions() {
    return true;
  }
}
