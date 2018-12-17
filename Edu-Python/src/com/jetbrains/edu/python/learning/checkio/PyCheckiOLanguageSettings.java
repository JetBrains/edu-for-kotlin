package com.jetbrains.edu.python.learning.checkio;

import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiOSdkValidator;
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettings;
import org.jetbrains.annotations.Nullable;

public class PyCheckiOLanguageSettings extends PyLanguageSettings {
  @Nullable
  @Override
  public String validate(@Nullable Course course) {
    return PyCheckiOSdkValidator.validateSdk(getSettings().getSdk());
  }
}
