package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.EduNames

/**
 * Base on a class from intellij plugin from Stepik
 *
 * @see <a href="https://github.com/StepicOrg/intellij-plugins/blob/develop/stepik-union/src/main/java/org/stepik/core/SupportedLanguages.kt"> SupportedLanguages.kt</a>
 *
 */
enum class StepikLanguages(val id: String?, val langName: String?) {
  JAVA(EduNames.JAVA, "java8"),
  JAVA11(EduNames.JAVA, "java11"),
  KOTLIN(EduNames.KOTLIN, "kotlin"),
  PYTHON(EduNames.PYTHON, "python3"),
  JAVASCRIPT(EduNames.JAVASCRIPT, "javascript"),
  SCALA(EduNames.SCALA, "scala"),
  CPP(EduNames.CPP, "c++"),
  GO(EduNames.GO, "go"),
  PLAINTEXT("TEXT", "TEXT"), // added for tests
  INVALID(null, null);


  override fun toString(): String = id ?: ""

  companion object {
    private val nameMap: Map<String?, StepikLanguages> by lazy {
      values().associateBy { it.langName }
    }

    private val titleMap: Map<String?, StepikLanguages> by lazy {
      values().associateBy { it.id }
    }

    @JvmStatic
    fun langOfName(lang: String): StepikLanguages = nameMap.getOrElse(lang, { INVALID })

    @JvmStatic
    fun langOfId(lang: String): StepikLanguages = titleMap.getOrElse(lang, { INVALID })
  }
}