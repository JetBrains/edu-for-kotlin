package com.jetbrains.edu.learning.stepik;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Alarm;
import com.intellij.util.text.DateFormatUtil;
import com.jetbrains.edu.learning.InitializationComponent;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StepikUpdater {
  private static final long CHECK_INTERVAL = DateFormatUtil.DAY;

  private final Runnable myCheckRunnable = () -> updateCourseList().doWhenDone(() -> queueNextCheck(CHECK_INTERVAL));
  private final Alarm myCheckForUpdatesAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);

  public StepikUpdater(@NotNull Application application) {
    scheduleCourseListUpdate(application);
  }

  public void scheduleCourseListUpdate(Application application) {
    if (!checkNeeded()) {
      return;
    }
    application.getMessageBus().connect(application).subscribe(AppLifecycleListener.TOPIC, new AppLifecycleListener() {
      @Override
      public void appFrameCreated(String[] commandLineArgs, @NotNull Ref<Boolean> willOpenProject) {

        long timeToNextCheck = EduSettings.getInstance().getLastTimeChecked() + CHECK_INTERVAL - System.currentTimeMillis();
        if (timeToNextCheck <= 0) {
          myCheckRunnable.run();
        }
        else {
          queueNextCheck(timeToNextCheck);
        }
      }
    });
  }

  private static ActionCallback updateCourseList() {
    ActionCallback callback = new ActionCallback();
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      final List<Course> courses = StepikConnector.getCourses(null);
      EduSettings.getInstance().setLastTimeChecked(System.currentTimeMillis());

      if (!courses.isEmpty()) {
        List<Course> updated = new ArrayList<>();
        for (Course course : courses) {
          if (course instanceof RemoteCourse && ((RemoteCourse)course).getUpdateDate().
                                                after(new Date(EduSettings.getInstance().getLastTimeChecked()))) {
            updated.add(course);
          }
        }
        if (updated.isEmpty()) return;
        final String message;
        final String title;
        if (updated.size() == 1) {
          message = updated.get(0).getName();
          title = "New course available";
        }
        else {
          title = "New courses available";
          message = StringUtil.join(updated, Course::getName, ", ");
        }
        final Notification notification = new Notification("New.course", title, message, NotificationType.INFORMATION);
        notification.notify(null);
      }
    });
    return callback;
  }

  private void queueNextCheck(long interval) {
    myCheckForUpdatesAlarm.addRequest(myCheckRunnable, interval);
  }

  private static boolean checkNeeded() {
    if (!PropertiesComponent.getInstance().isValueSet(InitializationComponent.CONFLICTING_PLUGINS_DISABLED)) {
      return false;
    }
    long timeToNextCheck = EduSettings.getInstance().getLastTimeChecked() + CHECK_INTERVAL - System.currentTimeMillis();
    return timeToNextCheck <= 0;
  }
}
