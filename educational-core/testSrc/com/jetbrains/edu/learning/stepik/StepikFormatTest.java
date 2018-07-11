package com.jetbrains.edu.learning.stepik;

import com.google.gson.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.FeedbackLink;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.edu.learning.stepik.StepikNames.PYCHARM_PREFIX;
import static org.junit.Assert.*;

public class StepikFormatTest {
  @Rule
  public TestName name = new TestName();

  @Test
  public void fromFirstVersion() throws IOException {
    doStepOptionsCreationTest();
  }

  @Test
  public void fromSecondVersion() throws IOException {
    doStepOptionsCreationTest();
  }

  @Test
  public void fromThirdVersion() throws IOException {
    doStepOptionsCreationTest();
  }

  @Test
  public void testAdditionalMaterialsLesson() throws IOException {
    String responseString = loadJsonText();
    Lesson lesson =
        StepikClient.deserializeStepikResponse(StepikWrappers.LessonContainer.class, responseString).lessons.get(0);
    assertEquals(EduNames.ADDITIONAL_MATERIALS, lesson.getName());
  }

  @Test
  public void testAdditionalMaterialsStep() throws IOException {
    String responseString = loadJsonText();
    StepikWrappers.StepSource step =
        StepikClient.deserializeStepikResponse(StepikWrappers.StepContainer.class, responseString).steps.get(0);
    assertEquals(EduNames.ADDITIONAL_MATERIALS, step.block.options.title);
  }

  @Test
  public void testAvailableCourses() throws IOException {
    String responseString = loadJsonText();
    StepikWrappers.CoursesContainer container =
      StepikClient.deserializeStepikResponse(StepikWrappers.CoursesContainer.class, responseString);
    assertNotNull(container.courses);
    assertEquals("Incorrect number of courses", 4, container.courses.size());
  }

  @Test
  public void testPlaceholderSerialization() throws IOException {
    final Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
    AnswerPlaceholder answerPlaceholder = new AnswerPlaceholder();
    answerPlaceholder.setOffset(1);
    answerPlaceholder.setLength(10);
    answerPlaceholder.setPlaceholderText("type here");
    answerPlaceholder.setPossibleAnswer("answer1");
    answerPlaceholder.setHints(ContainerUtil.list("hint 1", "hint 2"));
    final String placeholderSerialization = gson.toJson(answerPlaceholder);
    String expected  = loadJsonText();
    JsonObject object = new JsonParser().parse(expected).getAsJsonObject();
    assertEquals(gson.toJson(gson.fromJson(object, AnswerPlaceholder.class)), placeholderSerialization);

  }

  @Test
  public void testTokenUptoDate() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepikWrappers.AuthorWrapper wrapper = gson.fromJson(jsonText, StepikWrappers.AuthorWrapper.class);
    assertNotNull(wrapper);
    assertFalse(wrapper.users.isEmpty());
    StepicUser user = wrapper.users.get(0);
    assertNotNull(user);
    assertFalse(user.isGuest());
  }

  @Test
  public void testCourseAuthor() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepikWrappers.AuthorWrapper wrapper = gson.fromJson(jsonText, StepikWrappers.AuthorWrapper.class);
    assertNotNull(wrapper);
    assertFalse(wrapper.users.isEmpty());
    StepicUser user = wrapper.users.get(0);
    assertNotNull(user);
    assertFalse(user.isGuest());
  }

  @Test
  public void testSections() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepikWrappers.SectionContainer sectionContainer = gson.fromJson(jsonText, StepikWrappers.SectionContainer.class);
    assertNotNull(sectionContainer);
    assertEquals(1, sectionContainer.sections.size());
    List<Integer> unitIds = sectionContainer.sections.get(0).units;
    assertEquals(10, unitIds.size());
  }

  @Test
  public void testUnit() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepikWrappers.UnitContainer unit = gson.fromJson(jsonText, StepikWrappers.UnitContainer.class);
    assertNotNull(unit);
    assertEquals(1, unit.units.size());
    final int lesson = unit.units.get(0).lesson;
    assertEquals(13416, lesson);
  }

  @Test
  public void testLesson() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepikWrappers.LessonContainer lessonContainer = gson.fromJson(jsonText, StepikWrappers.LessonContainer.class);
    assertNotNull(lessonContainer);
    assertEquals(1, lessonContainer.lessons.size());
    final Lesson lesson = lessonContainer.lessons.get(0);
    assertNotNull(lesson);
  }

  @Test
  public void testStep() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepikWrappers.StepContainer stepContainer = gson.fromJson(jsonText, StepikWrappers.StepContainer.class);
    assertNotNull(stepContainer);
    final StepikWrappers.StepSource step = stepContainer.steps.get(0);
    assertNotNull(step);
  }

  @Test
  public void testStepBlock() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepikWrappers.StepContainer stepContainer = gson.fromJson(jsonText, StepikWrappers.StepContainer.class);
    final StepikWrappers.StepSource step = stepContainer.steps.get(0);
    final StepikWrappers.Step block = step.block;
    assertNotNull(block);
    assertNotNull(block.options);
    assertTrue(block.name.startsWith(PYCHARM_PREFIX));
  }

  @Test
  public void testStepBlockOptions() throws IOException {
    final StepikWrappers.StepOptions options = getStepOptions();
    assertNotNull(options);
  }

  @Test
  public void testUpdateDate() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepikWrappers.StepContainer stepContainer = gson.fromJson(jsonText, StepikWrappers.StepContainer.class);
    final StepikWrappers.StepSource step = stepContainer.steps.get(0);
    assertNotNull(step.update_date);
  }

  @Test
  public void testOptionsTitle() throws IOException {
    final StepikWrappers.StepOptions options = getStepOptions();
    assertEquals("Our first program", options.title);
  }

  @Test
  public void testOptionsTest() throws IOException {
    final StepikWrappers.StepOptions options = getStepOptions();
    final List<StepikWrappers.FileWrapper> testWrapper = options.test;
    assertNotNull(testWrapper);
    assertEquals(1, testWrapper.size());
    assertEquals("tests.py", testWrapper.get(0).name);
    assertNotNull(testWrapper.get(0).text);
  }

  @Test
  public void testOptionsDescription() throws IOException {
    final StepikWrappers.StepOptions options = getStepOptions();

    assertEquals("\n" +
        "Traditionally the first program you write in any programming language is <code>\"Hello World!\"</code>.\n" +
        "<br><br>\n" +
        "Introduce yourself to the World.\n" +
        "<br><br>\n" +
        "Hint: To run a script сhoose 'Run &lt;name&gt;' on the context menu. <br>\n" +
        "For more information visit <a href=\"https://www.jetbrains.com/help/pycharm/running-and-rerunning-applications.html\">our help</a>.\n" +
        "\n" +
        "<br>\n", options.descriptionText);
  }

  @Test
  public void  testOptionsFeedbackLinks() throws IOException {
    StepikWrappers.StepOptions stepOptions = getStepOptions();
    assertEquals(FeedbackLink.LinkType.CUSTOM, stepOptions.myFeedbackLink.getType());
  }

  @Test
  public void testOptionsFiles() throws IOException {
    final StepikWrappers.StepOptions options = getStepOptions();

    final List<TaskFile> files = options.files;
    assertEquals(1, files.size());
    final TaskFile taskFile = files.get(0);
    assertEquals("hello_world.py", taskFile.name);
    assertEquals("print(\"Hello, world! My name is type your name\")\n", taskFile.text);
  }

  private StepikWrappers.StepOptions getStepOptions() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepikWrappers.StepContainer stepContainer = gson.fromJson(jsonText, StepikWrappers.StepContainer.class);
    final StepikWrappers.StepSource step = stepContainer.steps.get(0);
    final StepikWrappers.Step block = step.block;
    return block.options;
  }

  @Test
  public void testOptionsPlaceholder() throws IOException {
    final StepikWrappers.StepOptions options = getStepOptions();
    final List<TaskFile> files = options.files;
    final TaskFile taskFile = files.get(0);

    final List<AnswerPlaceholder> placeholders = taskFile.getAnswerPlaceholders();
    assertEquals(1, placeholders.size());
    final int offset = placeholders.get(0).getOffset();
    assertEquals(32, offset);
    final int length = placeholders.get(0).getLength();
    assertEquals(14, length);
    assertEquals("type your name", taskFile.text.substring(offset, offset + length));
  }

  @Test
  public void testTaskStatuses() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    StepikWrappers.ProgressContainer progressContainer = gson.fromJson(jsonText, StepikWrappers.ProgressContainer.class);
    assertNotNull(progressContainer);
    List<StepikWrappers.ProgressContainer.Progress> progressList = progressContainer.progresses;
    assertNotNull(progressList);
    final Boolean[] statuses = progressList.stream().map(progress -> progress.isPassed).toArray(Boolean[]::new);
    assertNotNull(statuses);
    assertEquals(50, statuses.length);
  }

  @Test
  public void testLastSubmission() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    StepikWrappers.SubmissionsWrapper submissionsWrapper = gson.fromJson(jsonText, StepikWrappers.SubmissionsWrapper.class);
    assertNotNull(submissionsWrapper);
    assertNotNull(submissionsWrapper.submissions);
    assertEquals(20, submissionsWrapper.submissions.length);
    final StepikWrappers.Reply reply = submissionsWrapper.submissions[0].reply;
    assertNotNull(reply);
    List<StepikWrappers.SolutionFile> solutionFiles = reply.solution;
    assertEquals(1, solutionFiles.size());
    assertEquals("hello_world.py", solutionFiles.get(0).name);
    assertEquals("print(\"Hello, world! My name is type your name\")\n", solutionFiles.get(0).text);
  }

  @Test
  public void testNonEduTasks() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepikWrappers.StepContainer stepContainer = gson.fromJson(jsonText, StepikWrappers.StepContainer.class);
    assertNotNull(stepContainer);
    assertNotNull(stepContainer.steps);
    assertEquals(3, stepContainer.steps.size());
  }

  @NotNull
  private static Gson getGson() {
    return StepikClient.createGson();
  }

  @NotNull
  private String loadJsonText() throws IOException {
    return FileUtil.loadFile(new File(getTestDataPath(), getTestFile()));
  }

  @NotNull
  private static String getTestDataPath() {
    return FileUtil.join("testData/stepik");
  }

  private StepikWrappers.StepOptions doStepOptionsCreationTest() throws IOException {
    String responseString = loadJsonText();
    StepikWrappers.StepSource stepSource =
        StepikClient.deserializeStepikResponse(StepikWrappers.StepContainer.class, responseString).steps.get(0);
    StepikWrappers.StepOptions options = stepSource.block.options;
    List<TaskFile> files = options.files;
    assertEquals("Wrong number of task files", 1, files.size());
    List<AnswerPlaceholder> placeholders = files.get(0).getAnswerPlaceholders();
    assertEquals("Wrong number of placeholders", 1, placeholders.size());
    AnswerPlaceholder placeholder = placeholders.get(0);
    assertEquals(Collections.singletonList("Type your name here."), placeholder.getHints());
    assertEquals("Liana", placeholder.getPossibleAnswer());
    assertEquals("Description", options.descriptionText);
    return options;
  }

  @NotNull
  private String getTestFile() {
    final String methodName = name.getMethodName();
    String fileName = methodName.substring("test".length());
    fileName = Character.toLowerCase(fileName.charAt(0)) + fileName.substring(1);
    return fileName + ".json";
  }
}
