/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.play.tasks

import org.gradle.play.integtest.fixtures.PlayMultiVersionIntegrationTest
import org.gradle.test.fixtures.archive.JarTestFixture
import org.gradle.util.VersionNumber
import org.junit.Assume
import spock.lang.Unroll

import static org.gradle.play.integtest.fixtures.Repositories.PLAY_REPOSITORIES
import static org.hamcrest.Matchers.containsString

class TwirlCompileIntegrationTest extends PlayMultiVersionIntegrationTest {

    def destinationDir = file("build/src/play/binary/twirlTemplatesScalaSources/views")

    def setup() {
        settingsFile << """ rootProject.name = 'twirl-play-app' """
        buildFile << """
            plugins {
                id 'play-application'
            }

            ${PLAY_REPOSITORIES}

            model {
                components {
                    play {
                        targetPlatform "play-${version}"
                    }
                }
            }
        """
    }

    @Unroll
    def "can run TwirlCompile with #format template"() {
        given:
        twirlTemplate("test.scala.${format}") << template
        when:
        succeeds("compilePlayBinaryScala")
        then:
        def generatedFile = destinationDir.file("${format}/test.template.scala")
        generatedFile.assertIsFile()
        generatedFile.assertContents(containsString("import views.${format}._;"))
        generatedFile.assertContents(containsString(templateFormat))

        where:
        format | templateFormat     | template
        "js"   | 'JavaScriptFormat' | '@(username: String) alert(@helper.json(username));'
        "xml"  | 'XmlFormat'        | '@(username: String) <xml> <username> @username </username>'
        "txt"  | 'TxtFormat'        | '@(username: String) @username'
        "html" | 'HtmlFormat'       | '@(username: String) <html> <body> <h1>Hello @username</h1> </body> </html>'
    }

    def "can compile custom Twirl templates"() {
        given:
        twirlTemplate("test.scala.csv") << """
            @(username: String)(content: Csv)
            # generated by @username
            @content
        """

        addCsvFormat()

        when:
        succeeds("compilePlayBinaryScala")
        then:
        def generatedFile = destinationDir.file("csv/test.template.scala")
        generatedFile.assertIsFile()
        generatedFile.assertContents(containsString("import views.formats.csv._;"))
        generatedFile.assertContents(containsString("CsvFormat"))

        // Modifying user templates causes TwirlCompile to be out-of-date
        when:
        buildFile << """
            model {
                components {
                    play {
                        sources {                        
                            withType(TwirlSourceSet) {
                                addUserTemplateFormat("unused", "views.formats.unused.UnusedFormat")
                            }
                        }
                    }
                }
            }
        """
        and:
        succeeds("compilePlayBinaryScala")
        then:
        result.assertTasksNotSkipped(":compilePlayBinaryPlayTwirlTemplates", ":compilePlayBinaryScala")
    }

    def "runs compiler incrementally"() {
        when:
        withTwirlTemplate("input1.scala.html")
        then:
        succeeds("compilePlayBinaryPlayTwirlTemplates")
        and:
        destinationDir.assertHasDescendants("html/input1.template.scala")
        def input1FirstCompileSnapshot = destinationDir.file("html/input1.template.scala").snapshot()

        when:
        succeeds("compilePlayBinaryPlayTwirlTemplates")
        then:
        skipped(":compilePlayBinaryPlayTwirlTemplates")

        when:
        withTwirlTemplate("input2.scala.html")
        and:
        succeeds("compilePlayBinaryPlayTwirlTemplates")
        then:
        destinationDir.assertHasDescendants("html/input1.template.scala", "html/input2.template.scala")
        and:
        destinationDir.file("html/input1.template.scala").assertHasNotChangedSince(input1FirstCompileSnapshot)

        when:
        file("app/views/input2.scala.html").delete()
        then:
        succeeds("compilePlayBinaryPlayTwirlTemplates")
        and:
        destinationDir.assertHasDescendants("html/input1.template.scala")
    }

    def "removes stale output files in incremental compile"(){
        given:
        withTwirlTemplate("input1.scala.html")
        withTwirlTemplate("input2.scala.html")
        succeeds("compilePlayBinaryPlayTwirlTemplates")

        and:
        destinationDir.assertHasDescendants("html/input1.template.scala", "html/input2.template.scala")
        def input1FirstCompileSnapshot = destinationDir.file("html/input1.template.scala").snapshot()

        when:
        file("app/views/input2.scala.html").delete()

        then:
        succeeds("compilePlayBinaryPlayTwirlTemplates")
        and:
        destinationDir.assertHasDescendants("html/input1.template.scala")
        destinationDir.file("html/input1.template.scala").assertHasNotChangedSince(input1FirstCompileSnapshot)
        destinationDir.file("html/input2.template.scala").assertDoesNotExist()
    }

    def "builds multiple twirl source sets as part of play build" () {
        withExtraSourceSets()
        withTemplateSource(file("app", "views", "index.scala.html"))
        withTemplateSource(file("otherSources", "templates", "other.scala.html"))
        withTemplateSource(file("extraSources", "extra.scala.html"))

        when:
        succeeds "assemble"

        then:
        executedAndNotSkipped(
                ":compilePlayBinaryPlayTwirlTemplates",
                ":compilePlayBinaryPlayExtraTwirl",
                ":compilePlayBinaryPlayOtherTwirl"
        )

        and:
        destinationDir.assertHasDescendants("html/index.template.scala")
        file("build/src/play/binary/otherTwirlScalaSources").assertHasDescendants("templates/html/other.template.scala")
        file("build/src/play/binary/extraTwirlScalaSources").assertHasDescendants("html/extra.template.scala")

        and:
        jar("build/playBinary/lib/twirl-play-app.jar")
            .containsDescendants("views/html/index.class", "templates/html/other.class", "html/extra.class")
    }

    def "can build twirl source set with default Java imports" () {
        Assume.assumeTrue(versionNumber < VersionNumber.parse("2.6.2"))
        withTwirlJavaSourceSets()
        withTemplateSourceExpectingJavaImports(file("twirlJava", "javaTemplate.scala.html"))
        validateThatPlayJavaDependencyIsAdded()

        when:
        succeeds "assemble"

        then:
        executedAndNotSkipped ":compilePlayBinaryPlayTwirlJava"

        and:
        jar("build/playBinary/lib/twirl-play-app.jar")
            .containsDescendants("html/javaTemplate.class")
    }

    def "can build twirl source sets both with and without default Java imports" () {
        Assume.assumeTrue(versionNumber < VersionNumber.parse("2.6.2"))
        withTwirlJavaSourceSets()
        withTemplateSource(file("app", "views", "index.scala.html"))
        withTemplateSourceExpectingJavaImports(file("twirlJava", "javaTemplate.scala.html"))

        when:
        succeeds "assemble"

        then:
        executedAndNotSkipped(
            ":compilePlayBinaryPlayTwirlTemplates",
            ":compilePlayBinaryPlayTwirlJava"
        )

        and:
        jar("build/playBinary/lib/twirl-play-app.jar")
            .containsDescendants("html/javaTemplate.class", "views/html/index.class")
    }

    def "twirl source sets default to Scala imports" () {
        withTemplateSource(file("app", "views", "index.scala.html"))
        validateThatPlayJavaDependencyIsNotAdded()
        validateThatSourceSetsDefaultToScalaImports()

        when:
        succeeds "assemble"

        then:
        executedAndNotSkipped ":compilePlayBinaryPlayTwirlTemplates"
    }

    def "extra sources appear in the component report"() {
        withExtraSourceSets()

        when:
        succeeds "components"

        then:
        output.contains """
Play Application 'play'
-----------------------

Source sets
    Java source 'play:java'
        srcDir: app
        includes: **/*.java
    JVM resources 'play:resources'
        srcDir: conf
    Routes source 'play:routes'
        srcDir: conf
        includes: routes, *.routes
    Scala source 'play:scala'
        srcDir: app
        includes: **/*.scala
    Twirl template source 'play:extraTwirl'
        srcDir: extraSources
    Twirl template source 'play:otherTwirl'
        srcDir: otherSources
    Twirl template source 'play:twirlTemplates'
        srcDir: app
        includes: **/*.scala.*

Binaries
"""

    }

    @Unroll
    def "has reasonable error if Twirl template is configured incorrectly with (#template)"() {
        buildFile << """
            model {
                components {
                    play {
                        sources {                        
                            withType(TwirlSourceSet) {
                                addUserTemplateFormat($template)
                            }
                        }
                    }
                }
            }
        """

        when:
        fails("components")
        then:
        result.error.contains(errorMessage)

        where:
        template                      | errorMessage
        "null, 'CustomFormat'"        | "Custom template extension cannot be null."
        "'.ext', 'CustomFormat'"      | "Custom template extension should not start with a dot."
        "'ext', null"                 | "Custom template format type cannot be null."
    }

    def "has reasonable error if Twirl template cannot be found"() {
        twirlTemplate("test.scala.custom") << "@(username: String) Custom template, @username!"
        when:
        fails("compilePlayBinaryScala")
        then:
        result.error.contains("Twirl compiler could not find a matching template for 'test.scala.custom'.")
    }

    def withTemplateSource(File templateFile) {
        templateFile << """@(message: String)

            <h1>@message</h1>

        """
    }

    def twirlTemplate(String fileName) {
        file("app", "views", fileName)
    }

    def withTwirlTemplate(String fileName = "index.scala.html") {
        def templateFile = file("app", "views", fileName)
        templateFile.createFile()
        withTemplateSource(templateFile)
    }

    def withTemplateSourceExpectingJavaImports(File templateFile) {
        templateFile << """
            <!DOCTYPE html>
            <html>
                <body>
                  <p>@UUID.randomUUID().toString()</p>
                </body>
            </html>
        """
    }

    def withExtraSourceSets() {
        buildFile << """
            model {
                components {
                    play {
                        sources {
                            extraTwirl(TwirlSourceSet) {
                                source.srcDir "extraSources"
                            }
                            otherTwirl(TwirlSourceSet) {
                                source.srcDir "otherSources"
                            }
                        }
                    }
                }
            }
        """
    }

    def withTwirlJavaSourceSets() {
        buildFile << """
            model {
                components {
                    play {
                        sources {
                            twirlJava(TwirlSourceSet) {
                                defaultImports = TwirlImports.JAVA
                                source.srcDir "twirlJava"
                            }
                        }
                    }
                }
            }
        """
    }

    def validateThatPlayJavaDependencyIsAdded() {
        validateThatPlayJavaDependency(true)
    }

    def validateThatPlayJavaDependencyIsNotAdded() {
        validateThatPlayJavaDependency(false)
    }

    def validateThatPlayJavaDependency(boolean shouldBePresent) {
        buildFile << """
            model {
                components {
                    play {
                        binaries.all { binary ->
                            tasks.withType(TwirlCompile) {
                                doFirst {
                                    assert ${shouldBePresent ? "" : "!"} configurations.play.dependencies.any {
                                        it.group == "com.typesafe.play" &&
                                        it.name == "play-java_\${binary.targetPlatform.scalaPlatform.scalaCompatibilityVersion}" &&
                                        it.version == binary.targetPlatform.playVersion
                                    }
                                }
                            }
                        }
                    }
                }
            }
        """
    }

    def validateThatSourceSetsDefaultToScalaImports() {
        buildFile << """
            model {
                components {
                    play {
                        binaries.all { binary ->
                            tasks.withType(TwirlCompile) {
                                doFirst {
                                    assert defaultImports == TwirlImports.SCALA
                                    assert binary.inputs.withType(TwirlSourceSet).every {
                                        it.defaultImports == TwirlImports.SCALA
                                    }
                                }
                            }
                        }
                    }
                }
            }
        """
    }

    JarTestFixture jar(String fileName) {
        new JarTestFixture(file(fileName))
    }

    private void addCsvFormat() {
        buildFile << """
            model {
                components {
                    play {
                        sources {                        
                            withType(TwirlSourceSet) {
                                addUserTemplateFormat("csv", "views.formats.csv.CsvFormat", "views.formats.csv._")
                            }
                        }
                    }
                }
            }
        """

        if (versionNumber < VersionNumber.parse("2.3.0")) {
            file("app/views/formats/csv/Csv.scala") << """
package views.formats.csv

import play.api.http.ContentTypeOf
import play.api.mvc.Codec
import play.api.templates.BufferedContent
import play.templates.Format

class Csv(buffer: StringBuilder) extends BufferedContent[Csv](buffer) {
  val contentType = Csv.contentType
}

object Csv {
  val contentType = "text/csv"
  implicit def contentTypeCsv(implicit codec: Codec): ContentTypeOf[Csv] = ContentTypeOf[Csv](Some(Csv.contentType))

  def apply(text: String): Csv = new Csv(new StringBuilder(text))
  
  def empty: Csv = new Csv(new StringBuilder)
}

object CsvFormat extends Format[Csv] {
  def raw(text: String): Csv = Csv(text)
  def escape(text: String): Csv = Csv(text)
}
"""
        } else {
            file("app/views/formats/csv/Csv.scala") << """
package views.formats.csv

import scala.collection.immutable
import play.twirl.api.BufferedContent
import play.twirl.api.Format

class Csv private (elements: immutable.Seq[Csv], text: String) extends BufferedContent[Csv](elements, text) {
  def this(text: String) = this(Nil, if (text eq null) "" else text)
  def this(elements: immutable.Seq[Csv]) = this(elements, "")

  /**
   * Content type of CSV.
   */
  def contentType = "text/csv"
}

/**
 * Helper for CSV utility methods.
 */
object Csv {

  /**
   * Creates an CSV fragment with initial content specified.
   */
  def apply(text: String): Csv = {
    new Csv(text)
  }

}

/**
 * Formatter for CSV content.
 */
object CsvFormat extends Format[Csv] {

  /**
   * Creates a CSV fragment.
   */
  def raw(text: String) = Csv(text)

  /**
   * Creates an escaped CSV fragment.
   */
  def escape(text: String) = Csv(text)

  /**
   * Generate an empty CSV fragment
   */
  val empty: Csv = new Csv("")

  /**
   * Create a CSV Fragment that holds other fragments.
   */
  def fill(elements: immutable.Seq[Csv]): Csv = new Csv(elements)
}
        """
        }
    }
}
