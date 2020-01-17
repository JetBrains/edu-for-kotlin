package com.jetbrains.edu.scala.taskDescription

import com.intellij.lang.Language
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionHighlightingTestBase
import org.jetbrains.plugins.scala.ScalaLanguage

class ScalaTaskDescriptionHighlightingTest : TaskDescriptionHighlightingTestBase() {

  override val language: Language = ScalaLanguage.INSTANCE
  override val environment: String = "Gradle"
  override val settings: Any get() = JdkProjectSettings.emptySettings()

  fun `test markdown description highlighting`() = doMarkdownTest("""
    Code block with default language:
    ```
      def main(args: Array[String]): Unit = {
        println("Hello!")
      }
    ```

    Code block with specific language:
    ```scala
      def main(args: Array[String]): Unit = {
        println("Hello!")
      }
    ```

    Inline code `if (condition) {} else {}`
  """, """
    <html>
     <head></head>
     <body>
      <p>Code block with default language:</p>
      <span class="code-block"><pre>  <span style="...">def </span>main(args: Array[String]): Unit = {
        println(<span style="...">"Hello!"</span>)
      }
    </pre> </span>
      <p>Code block with specific language:</p>
      <span class="code-block"><pre>  <span style="...">def </span>main(args: Array[String]): Unit = {
        println(<span style="...">"Hello!"</span>)
      }
    </pre> </span>
      <p>Inline code <span class="code"><span style="...">if </span>(condition) {} <span style="...">else </span>{}</span></p>
     </body>
    </html>
  """)

  fun `test html description highlighting`() = doHtmlTest("""
    <html>
    <p>Code block with default language:</p>
    <pre><code>
      def main(args: Array[String]): Unit = {
        println("Hello!")
      }
    </code></pre>
    <p>Code block with specific language:</p>
    <pre><code data-lang="text/x-kotlin">
      def main(args: Array[String]): Unit = {
        println("Hello!")
      }
    </code></pre>
    <p>Inline code <code>if (condition) {} else {}</code></p>
    </html>
  """, """
    <html>
     <head></head>
     <body>
      <p>Code block with default language:</p>
      <span class="code-block"><pre>
      <span style="...">def </span>main(args: Array[String]): Unit = {
        println(<span style="...">"Hello!"</span>)
      }
    </pre> </span>
      <p>Code block with specific language:</p>
      <span class="code-block"><pre>
      <span style="...">def </span>main(args: Array[String]): Unit = {
        println(<span style="...">"Hello!"</span>)
      }
    </pre> </span>
      <p>Inline code <span class="code"><span style="...">if </span>(condition) {} <span style="...">else </span>{}</span></p>
     </body>
    </html>
  """)
}
