package com.jetbrains.edu.learning.courseFormat;

import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.coursecreator.ui.CCCreateCoursePreviewDialog;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduVersions;
import com.jetbrains.edu.learning.stepik.StepikNames;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class EduCourse extends Course {
  public static final String ENVIRONMENT_SEPARATOR = "#";

  // Fields from stepik:
  private boolean isCompatible = true;
  private boolean isAdaptive = false;
  //course type in format "pycharm<version> <language> <version>$ENVIRONMENT_SEPARATOR<environment>"
  protected String myType =
    String.format("%s%d %s", StepikNames.PYCHARM_PREFIX, EduVersions.JSON_FORMAT_VERSION, getLanguage());
  // in CC mode is used to store top-level lessons section id
  List<Integer> sectionIds = new ArrayList<>();
  List<Integer> instructors = new ArrayList<>();
  private Date myCreateDate = new Date(0);
  @Transient private boolean isUpToDate = true;
  boolean isPublic;
  @Transient private String myAdminsGroup;
  private int learnersCount = 0;

  private int reviewSummary;
  private double reviewScore;

  public String getType() {
    return myType;
  }

  @Override
  public void setLanguage(@NotNull final String language) {
    super.setLanguage(language);
    updateType(language);
  }

  public List<Integer> getSectionIds() {
    return sectionIds;
  }

  public void setSectionIds(List<Integer> sectionIds) {
    this.sectionIds = sectionIds;
  }

  public void setInstructors(List<Integer> instructors) {
    this.instructors = instructors;
  }

  public List<Integer> getInstructors() {
    return instructors;
  }

  @NotNull
  @Override
  public List<Tag> getTags() {
    final List<Tag> tags = super.getTags();
    if (getVisibility() instanceof CourseVisibility.FeaturedVisibility) {
      tags.add(new FeaturedTag());
    }
    if (getVisibility() instanceof CourseVisibility.InProgressVisibility) {
      tags.add(new InProgressTag());
    }
    return tags;
  }

  public Date getCreateDate() { return myCreateDate; }

  public void setCreateDate(Date createDate) { myCreateDate = createDate; }

  public int getId() {
    return myId;
  }

  private void updateType(String language) {
    int formatVersion = getFormatVersion();

    String environment = getEnvironment();
    if (!environment.equals(EduNames.DEFAULT_ENVIRONMENT)) {
      setType(String.format("%s%s %s%s%s", StepikNames.PYCHARM_PREFIX, formatVersion, language, ENVIRONMENT_SEPARATOR, environment));
    }
    else {
      setType(String.format("%s%s %s", StepikNames.PYCHARM_PREFIX, formatVersion, language));
    }
  }

  public int getFormatVersion() {
    final int languageSeparator = myType.indexOf(" ");
    if (languageSeparator != -1 && myType.contains(StepikNames.PYCHARM_PREFIX)) {
      String formatVersion = myType.substring(StepikNames.PYCHARM_PREFIX.length(), languageSeparator);
      try {
        return Integer.parseInt(formatVersion);
      } catch(NumberFormatException | NullPointerException e) {
        return EduVersions.JSON_FORMAT_VERSION;
      }
    }
    return EduVersions.JSON_FORMAT_VERSION;
  }

  public void setType(String type) {
    myType = type;
  }

  public boolean isPublic() {
    return isPublic;
  }

  public void setPublic(boolean isPublic) {
    this.isPublic = isPublic;
  }

  public boolean isCompatible() {
    return isCompatible;
  }

  public void setCompatible(boolean compatible) {
    isCompatible = compatible;
  }

  public boolean isAdaptive() {
    return isAdaptive;
  }

  public void setAdaptive(boolean adaptive) {
    isAdaptive = adaptive;
  }

  @Transient
  public String getAdminsGroup() {
    return myAdminsGroup;
  }

  @Transient
  public void setAdminsGroup(String adminsGroup) {
    myAdminsGroup = adminsGroup;
  }

  public boolean isRemote() {
    return getId() != 0;
  }

  public void convertToLocal() {
    isPublic = false;
    isCompatible = true;
    setId(0);
    setUpdateDate(new Date(0));
    myCreateDate = new Date(0);
    sectionIds = new ArrayList<>();
    instructors = new ArrayList<>();
    myType = String.format("%s%d %s", StepikNames.PYCHARM_PREFIX, EduVersions.JSON_FORMAT_VERSION, getLanguage());
  }

  @Transient
  public boolean isUpToDate() {
    return isUpToDate;
  }

  public void setUpToDate(boolean isUpToDateValue) {
    isUpToDate = isUpToDateValue;
  }

  @Override
  public boolean isViewAsEducatorEnabled() {
    return getUserData(CCCreateCoursePreviewDialog.IS_COURSE_PREVIEW_KEY) != Boolean.TRUE;
  }

  public int getLearnersCount() {
    return learnersCount;
  }

  public void setLearnersCount(int learnersCount) {
    this.learnersCount = learnersCount;
  }

  public double getReviewScore() {
    return reviewScore;
  }

  public void setReviewScore(double reviewScore) {
    this.reviewScore = reviewScore;
  }

  public int getReviewSummary() {
    return reviewSummary;
  }

  public void setReviewSummary(int reviewSummary) {
    this.reviewSummary = reviewSummary;
  }
}
