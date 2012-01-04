/*
 * Copyright 2011 the original author or authors.
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

package org.gradle.integtests.tooling.m7

import org.gradle.integtests.tooling.fixture.MinTargetGradleVersion
import org.gradle.integtests.tooling.fixture.MinToolingApiVersion
import org.gradle.integtests.tooling.fixture.ToolingApiSpecification
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.Project
import spock.lang.Timeout

@MinToolingApiVersion('1.0-milestone-7')
@MinTargetGradleVersion('1.0-milestone-7')
class ConsumingStandardInputIntegrationTest extends ToolingApiSpecification {

    def setup() {
        //since this test treats with standard input I will not run it for embedded daemon for safety.
        toolingApi.isEmbedded = false
    }

    @Timeout(10)
    def "consumes input when building model"() {
        given:
        dist.file('build.gradle')  << """
description = System.in.text
"""
        when:
        Project model = (Project) withConnection { ProjectConnection connection ->
            def model = connection.model(Project.class)
            model.standardInput = new ByteArrayInputStream("Cool project".bytes)
            model.get()
        }

        then:
        model.description == 'Cool project'
    }

    @Timeout(10)
    def "works well if the standard input configured with null"() {
        given:
        dist.file('build.gradle')  << """
description = System.in.text
"""
        when:
        Project model = (Project) withConnection { ProjectConnection connection ->
            def model = connection.model(Project.class)
            model.standardInput = null
            model.get()
        }

        then:
        model.description == null
    }

    @Timeout(10)
    def "does not consume input when not explicitly provided"() {
        given:
        dist.file('build.gradle')  << """
description = "empty" + System.in.text
"""
        when:
        Project model = (Project) withConnection { ProjectConnection connection ->
            def model = connection.model(Project.class)
            model.get()
        }

        then:
        model.description == 'empty'
    }

    @Timeout(10)
    def "consumes input when running tasks"() {
        given:
        dist.file('build.gradle') << """
task createFile << {
    file('input.txt') << System.in.text
}
"""
        when:
        withConnection { ProjectConnection connection ->
            def build = connection.newBuild()
            build.standardInput = new ByteArrayInputStream("Hello world!".bytes)
            build.forTasks('createFile')
            build.run()
        }

        then:
        dist.file('input.txt').text == "Hello world!"
    }
}
