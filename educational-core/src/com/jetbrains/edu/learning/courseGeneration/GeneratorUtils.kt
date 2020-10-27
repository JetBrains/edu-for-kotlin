package com.jetbrains.edu.learning.courseGeneration

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.project.modifyModules
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat.HTML
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat.MD
import com.jetbrains.edu.learning.courseFormat.ext.dirName
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.shouldHavePhysicalFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isToEncodeContent
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import org.apache.commons.codec.binary.Base64
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

object GeneratorUtils {

  private val LOG: Logger = Logger.getInstance(GeneratorUtils::class.java)

  private val UNIX_INVALID_SYMBOLS: Regex = "[/:]".toRegex()
  private val WINDOWS_INVALID_SYMBOLS: Regex = "[/\\\\:<>\"?*|;&]".toRegex()

  @Throws(IOException::class)
  @JvmStatic
  fun createCourse(course: Course,
                   baseDir: VirtualFile,
                   indicator: ProgressIndicator) {
    indicator.isIndeterminate = false
    indicator.fraction = 0.0

    val items = course.items
    for ((i, item) in items.withIndex()) {
      indicator.fraction = (i + 1).toDouble() / items.size

      if (item is Lesson) {
        indicator.text = "Generating lesson ${i + 1} of ${items.size}"
        createLesson(item, baseDir)
      }
      else if (item is Section) {
        indicator.text = "Generating section ${i + 1} of ${items.size}"
        createSection(item, baseDir)
      }
    }
    indicator.text = "Generating additional files"
    createAdditionalFiles(course, baseDir)
    EduCounterUsageCollector.studyItemCreated(course)
  }

  fun createSection(item: Section, baseDir: VirtualFile): VirtualFile {
    val sectionDir = createUniqueDir(baseDir, item)

    for (lesson in item.lessons) {
      createLesson(lesson, sectionDir)
    }
    EduCounterUsageCollector.studyItemCreated(item)
    return sectionDir
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createLesson(lesson: Lesson, courseDir: VirtualFile): VirtualFile {
    val lessonDir = createUniqueDir(courseDir, lesson)
    val taskList = lesson.taskList
    for (task in taskList) {
      createTask(task, lessonDir)
    }
    EduCounterUsageCollector.studyItemCreated(lesson)
    return lessonDir
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createTask(task: Task, lessonDir: VirtualFile): VirtualFile {
    val isFirstInFrameworkLesson = task.parent is FrameworkLesson && task.index == 1
    val isStudyCourse = task.course.isStudy
    val (contentDir, configDir) = if (isStudyCourse && isFirstInFrameworkLesson) {
      // create config dir for yaml files and task description files
      val configDir = createUniqueDir(lessonDir, task)
      // create content dir with specific for framework lesson task name
      val contentDir = createUniqueDir(lessonDir, task, task.dirName, false)
      contentDir to configDir
    }
    else {
      val taskDir = createUniqueDir(lessonDir, task)
      taskDir to taskDir
    }

    if (!isStudyCourse || task.parent !is FrameworkLesson || isFirstInFrameworkLesson) {
      createTaskContent(task, contentDir)
    }

    createDescriptionFile(configDir, task)
    EduCounterUsageCollector.studyItemCreated(task)
    return contentDir
  }

  @Throws(IOException::class)
  fun createTaskContent(task: Task, taskDir: VirtualFile) {
    val taskFiles = task.taskFiles.filter { task.shouldHavePhysicalFile(it.key) }

    for ((path, file) in taskFiles) {
      createChildFile(taskDir, path, file.text)
    }
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createDescriptionFile(taskDir: VirtualFile, task: Task): VirtualFile? {
    val descriptionFileName = when (task.descriptionFormat) {
      HTML -> EduNames.TASK_HTML
      MD -> EduNames.TASK_MD
      else -> {
        LOG.warn("Description format for task `${task.name}` is null. Use html format")
        EduNames.TASK_HTML
      }
    }

    return createChildFile(taskDir, descriptionFileName, task.descriptionText)
  }

  @Throws(IOException::class)
  fun createAdditionalFiles(course: Course, courseDir: VirtualFile) {
    for (file in course.additionalFiles) {
      createChildFile(courseDir, file.name, file.text)
    }
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createChildFile(parentDir: VirtualFile, path: String, text: String): VirtualFile? {
    return runInWriteActionAndWait(ThrowableComputable {
      var newDirectories: String? = null
      var fileName = path
      var dir: VirtualFile? = parentDir
      if (path.contains("/")) {
        val pos = path.lastIndexOf("/")
        fileName = path.substring(pos + 1)
        newDirectories = path.substring(0, pos)
      }
      if (newDirectories != null) {
        dir = VfsUtil.createDirectoryIfMissing(parentDir, newDirectories)
      }
      if (dir != null) {
        val virtualTaskFile = dir.findOrCreateChildData(parentDir, fileName)
        if (virtualTaskFile.isToEncodeContent()) {
          virtualTaskFile.setBinaryContent(Base64.decodeBase64(text))
        }
        else {
          VfsUtil.saveText(virtualTaskFile, text)
        }
        virtualTaskFile
      }
      else {
        null
      }
    })
  }

  @Throws(IOException::class)
  @JvmStatic
  fun <T> runInWriteActionAndWait(action: ThrowableComputable<T, IOException>): T {
    val application = ApplicationManager.getApplication()
    val resultRef = AtomicReference<T>()
    val exceptionRef = AtomicReference<IOException>()
    application.invokeAndWait {
      application.runWriteAction {
        try {
          resultRef.set(action.compute())
        }
        catch (e: IOException) {
          exceptionRef.set(e)
        }
      }
    }
    return if (exceptionRef.get() != null) {
      throw IOException(exceptionRef.get())
    }
    else {
      resultRef.get()
    }
  }

  @JvmStatic
  fun initializeCourse(project: Project, course: Course) {
    course.init(null, null, false)
    StudyTaskManager.getInstance(project).course = course
  }

  /**
   * Non unique lesson/task/section names can be received from stepik
   */
  @JvmStatic
  fun getUniqueValidName(parentDir: VirtualFile, name: String): String {
    val validName = name.convertToValidName()
    var index = 0
    var candidateName = validName
    while (parentDir.findChild(candidateName) != null) {
      index++
      candidateName = "$validName ($index)"
    }
    return candidateName
  }

  @JvmStatic
  fun String.convertToValidName(): String {
    val invalidSymbols = if (SystemInfo.isWindows) WINDOWS_INVALID_SYMBOLS else UNIX_INVALID_SYMBOLS
    var validName = replace(invalidSymbols, " ").trim()
    if (SystemInfo.isWindows && endsWith(".")) {
      validName = validName.trim('.').trim()
    }
    // Small hack to avoid https://youtrack.jetbrains.com/issue/IDEA-253884
    if (validName.endsWith("!")) {
      validName = validName.replaceRange(validName.lastIndex, validName.lastIndex + 1, "_")
    }
    return validName
  }

  private fun createUniqueDir(parentDir: VirtualFile,
                              item: StudyItem,
                              baseDirName: String = item.name,
                              needUpdateItem: Boolean = true): VirtualFile {
    val uniqueDirName = getUniqueValidName(parentDir, baseDirName)
    if (uniqueDirName != baseDirName && needUpdateItem) {
      item.customPresentableName = item.name
      item.name = uniqueDirName
    }

    return runInWriteActionAndWait(ThrowableComputable {
      VfsUtil.createDirectoryIfMissing(parentDir, uniqueDirName)
    })
  }

  @JvmStatic
  fun joinPaths(prefix: String?, suffix: String): String {
    return if (prefix.isNullOrEmpty()) suffix else "$prefix${VfsUtilCore.VFS_SEPARATOR_CHAR}$suffix"
  }

  @JvmStatic
  @JvmOverloads
  fun getInternalTemplateText(templateName: String, templateVariables: Map<String, Any> = emptyMap()): String =
    FileTemplateManager.getDefaultInstance().getInternalTemplate(templateName).getText(templateVariables)

  @Throws(IOException::class)
  fun evaluateExistingTemplate(child: VirtualFile, templateVariables: Map<String, Any>) {
    val rawContent = VfsUtil.loadText(child)
    val content = FileTemplateUtil.mergeTemplate(templateVariables, rawContent, false)
    invokeAndWaitIfNeeded { runWriteAction { VfsUtil.saveText(child, content) } }
  }

  /**
   * Checks if file exists in [baseDir] by given [path].
   * If it doesn't exist, creates a new file from internal [templateName] template.
   * Otherwise, substitutes all template variables in file text
   */
  @Throws(IOException::class)
  fun createFileFromTemplate(baseDir: VirtualFile, path: String, templateName: String, templateVariables: Map<String, Any>) {
    val file = baseDir.findFileByRelativePath(path)
    if (file == null) {
      val configText = getInternalTemplateText(templateName, templateVariables)
      createChildFile(baseDir, path, configText)
    }
    else {
      evaluateExistingTemplate(file, templateVariables)
    }
  }

  /**
   * Removes [module] from [project].
   * It should be used when external build system like Gradle, sbt, etc. creates modules itself
   * and initial base module is unexpected while import
   */
  fun removeModule(project: Project, module: Module) {
    if (!isUnitTestMode || (project as? ProjectEx)?.isLight == false) {
      project.modifyModules { disposeModule(module) }
    }
  }

  /**
   * Reformat the code so that learners do not see tons of IDE highlighting.
   * Should be used for third-party sources of courses when language style guide is systematically ignored.
   * */
  @JvmStatic
  fun reformatCodeInAllTaskFiles(project: Project, course: Course) {
    course.visitTasks {
      for ((_, file) in it.taskFiles) {
        val virtualFile = file.getVirtualFile(project) ?: continue
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: continue
        runInEdt {
          WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(psiFile)
          }
        }
      }
    }
  }

  private val INVALID_SYMBOLS = "[ /\\\\:<>\"?*|()]".toRegex()
  private val LEADING_AND_TRAILING_DOTS = "(^[.]+)|([.]+\$)".toRegex()

  // Should be the same as `sanitizeName` in `resources/fileTemplates/internal/settings.gradle.ft`
  /**
   * Replaces ' ', '/', '\', ':', '<', '>', '"', '?', '*', '|', '(', ')' symbols with '_' as they are invalid in gradle module names
   * Also removes leading and trailing dots, because gradle project name must not start or end with a '.'
   */
  fun sanitizeName(name: String): String = name.replace(INVALID_SYMBOLS, "_").replace(LEADING_AND_TRAILING_DOTS, "")
}
