package com.jetbrains.edu.learning.yaml

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeContent
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.MAPPER
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.mapper
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.saveItem
import com.jetbrains.edu.learning.yaml.YamlLoader.loadItem
import com.jetbrains.edu.learning.yaml.errorHandling.*
import com.jetbrains.edu.learning.yaml.format.getChangeApplierForItem

/**
 *  Get fully-initialized [StudyItem] object from yaml config file.
 *  Uses [YamlDeserializer.deserializeItem] to deserialize object, than applies changes to existing object, see [loadItem].
 */
object YamlLoader {

  fun loadItem(project: Project, configFile: VirtualFile) {
    val editor = configFile.getEditor(project)
    if (editor != null) {
      if (editor.headerComponent is InvalidFormatPanel) {
        editor.headerComponent = null
      }
    }
    try {
      doLoad(project, configFile)
    }
    catch (e: Exception) {
      when (e) {
        is YamlLoadingException -> YamlDeserializer.showError(project, e, configFile, e.message)
        else -> throw e
      }
    }
  }

  @VisibleForTesting
  fun doLoad(project: Project, configFile: VirtualFile) {
    // for null course we load course again so no need to pass mode specific mapper here
    val mapper = StudyTaskManager.getInstance(project).course?.mapper ?: MAPPER

    val existingItem = getStudyItemForConfig(project, configFile)
    val deserializedItem = YamlDeserializer.deserializeItem(project, configFile, mapper) ?: return
    deserializedItem.ensureChildrenExist(configFile.parent)

    if (existingItem == null) {
      // tis code is called if item wasn't loaded because of broken config
      // and now if config fixed, we'll add item to a parent
      if (deserializedItem is Course) {
        StudyTaskManager.getInstance(project).course = YamlDeepLoader.loadCourse(project)
        return
      }

      val itemDir = configFile.parent
      deserializedItem.name = itemDir.name
      val parentItem = deserializedItem.getParentItem(project, itemDir.parent)
      val parentConfig = parentItem.getDir(project).findChild(parentItem.configFileName) ?: return
      val deserializedParent = YamlDeserializer.deserializeItem(project, parentConfig, mapper) as? ItemContainer ?: return
      if (deserializedParent.items.map { it.name }.contains(itemDir.name)) {
        parentItem.addItemAsNew(project, deserializedItem)
        reopenEditors(project)
        // new item is added at the end, so we should save parent item to update items order in config file
        saveItem(parentItem)
      }
      return
    }

    existingItem.applyChanges(project, deserializedItem)
  }

  /**
   * For items that are added as new we have to reopen editors, because `EduEditor` wasn't created
   * for files that aren't task files.
   */
  private fun reopenEditors(project: Project) {
    val selectedEditor = FileEditorManager.getInstance(project).selectedEditor
    val files = FileEditorManager.getInstance(project).openFiles
      .filter { EduUtils.getTaskFile(project, it) != null }
    for (virtualFile in files) {
      FileEditorManager.getInstance(project).closeFile(virtualFile)
      FileEditorManager.getInstance(project).openFile(virtualFile, false)
    }

    // restore selection
    val file = selectedEditor?.file
    if (file != null) {
      FileEditorManager.getInstance(project).openFile(file, true)
    }
  }

  fun ItemContainer.addItemAsNew(project: Project, deserializedItem: StudyItem) {
    deserializedItem.deserializeChildrenIfNeeded(project, course)
    addItem(deserializedItem)
    sortItems()
    init(course, this.parent, false)
  }

  fun StudyItem.deserializeChildrenIfNeeded(project: Project, course: Course) {
    if (this !is ItemContainer) {
      return
    }
    init(course, this, false)
    val mapper = course.mapper
    items = deserializeContent(project, items, mapper)
    // set parent to deserialize content correctly
    items.forEach { it.init(course, this, false) }
    items.filterIsInstance(ItemContainer::class.java).forEach {
      it.items = it.deserializeContent(project, it.items, mapper)
    }
  }

  private fun StudyItem.getParentItem(project: Project, parentDir: VirtualFile): ItemContainer {
    val course = StudyTaskManager.getInstance(project).course
    return when (this) {
             is Section -> course
             is Lesson -> {
               val section = course?.let { EduUtils.getSection(parentDir, course) }
               section ?: course
             }
             is Task -> course?.let { EduUtils.getLesson(parentDir, course) }
             else -> loadingError(
               "Unexpected item type. Expected: 'Section', 'Lesson' or 'Task'. Was '${itemType}'")
           } ?: loadingError(notFoundMessage("parent", "for item '${name}'"))
  }

  private fun <T : StudyItem> T.applyChanges(project: Project, deserializedItem: T) {
    getChangeApplierForItem(project, deserializedItem).applyChanges(this, deserializedItem)
  }

  private fun getStudyItemForConfig(project: Project, configFile: VirtualFile): StudyItem? {
    val name = configFile.name
    val itemDir = configFile.parent ?: error(notFoundMessage("containing item dir", name))
    val course = StudyTaskManager.getInstance(project).course ?: return null
    return when (name) {
      YamlFormatSettings.COURSE_CONFIG -> course
      YamlFormatSettings.SECTION_CONFIG -> EduUtils.getSection(itemDir, course)
      YamlFormatSettings.LESSON_CONFIG -> EduUtils.getLesson(itemDir, course)
      YamlFormatSettings.TASK_CONFIG -> EduUtils.getTask(itemDir, course)
      else -> loadingError(unknownConfigMessage(name))
    }
  }

  fun VirtualFile.getEditor(project: Project): Editor? {
    val selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor(this)
    return if (selectedEditor is TextEditor) selectedEditor.editor else null
  }
}

private fun StudyItem.ensureChildrenExist(itemDir: VirtualFile) {
  when (this) {
    is ItemContainer -> {
      items.forEach {
        val itemTypeName = if (it is Task) EduNames.TASK else EduNames.ITEM
        itemDir.findChild(it.name) ?: loadingError(noDirForItemMessage(it.name, itemTypeName))
      }
    }
    is Task -> {
      taskFiles.forEach { (name, _) ->
        itemDir.findFileByRelativePath(name) ?: loadingError("No file for `$name`")
      }
    }
  }
}
