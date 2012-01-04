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
package org.gradle.tooling;

import org.gradle.tooling.internal.consumer.ConnectorServices;

import java.io.File;
import java.net.URI;

/**
 * <p>A {@code GradleConnector} is the main entry point to the Gradle tooling API. You use this API as follows:</p>
 *
 * <ol>
 *
 * <li>Call {@link #newConnector()} to create a new connector instance.</li>
 *
 * <li>Configure the connector. You must call {@link #forProjectDirectory(java.io.File)} to specify which project you wish to connect to. Other methods are optional.</li>
 *
 * <li>Call {@link #connect()} to create the connection to a project.</li>
 *
 * <li>Optionally reuse the connector to create additional connections.</li>
 *
 * <li>When finished with the connection, call {@link ProjectConnection#close()} to clean up.</li>
 *
 * </ol>
 *
 * <p>{@code GradleConnector} instances are not thread-safe. If you want to use a {@code GradleConnector} concurrently you *must* always create a
 * new instance for each thread using {@link #newConnector()}.</p>
 *
 * <p>Note, however, the {@link ProjectConnection} instances that a connector creates are completely thread-safe.</p>
 */
public abstract class GradleConnector {

    /**
     * Creates a new connector instance.
     *
     * @return The instance. Never returns null.
     */
    public static GradleConnector newConnector() {
        return new ConnectorServices().createConnector();
    }

    /**
     * Specifies which Gradle installation to use. This replaces any value specified using {@link #useDistribution(java.net.URI)} or {@link #useGradleVersion(String)}. Defaults to a project-specific
     * Gradle version.
     *
     * @param gradleHome The Gradle installation directory.
     * @return this
     */
    public abstract GradleConnector useInstallation(File gradleHome);

    /**
     * Specifies which Gradle version to use. The appropriate distribution is downloaded and installed into the user's Gradle home directory. This replaces any value specified using {@link
     * #useInstallation(java.io.File)} or {@link #useDistribution(java.net.URI)}. Defaults to a project-specific Gradle version.
     *
     * @param gradleVersion The version to use.
     * @return this
     */
    public abstract GradleConnector useGradleVersion(String gradleVersion);

    /**
     * Specifies which Gradle distribution to use. The appropriate distribution is downloaded and installed into the user's Gradle home directory. This replaces any value specified using {@link
     * #useInstallation(java.io.File)} or {@link #useGradleVersion(String)}. Defaults to a project-specific Gradle version.
     *
     * @param gradleDistribution The distribution to use.
     * @return this
     */
    public abstract GradleConnector useDistribution(URI gradleDistribution);

    /**
     * Specifies the working directory to use.
     *
     * @param projectDir The working directory.
     * @return this
     */
    public abstract GradleConnector forProjectDirectory(File projectDir);

    /**
     * Specifies the user's Gradle home directory to use. Defaults to {@code ~/.gradle}
     *
     * @param gradleUserHomeDir The user's Gradle home directory to use.
     * @return this
     */
    public abstract GradleConnector useGradleUserHomeDir(File gradleUserHomeDir);

    /**
     * If the target gradle version supports it you can use this setting
     * to specify the java home directory to use for the gradle process.
     * If the target gradle version does not support it your setting is ignored and Gradle uses reasonable defaults.
     * <p>
     * {@link org.gradle.tooling.model.build.BuildEnvironment} model contains information such as java or gradle environment.
     * If you want to get hold of this information you can ask tooling API to build this model
     * <p>
     * If not configured or null passed the sensible default will be used
     *
     * @param javaHome to use for the gradle process
     * @return this
     */
    public abstract GradleConnector hintJavaHome(File javaHome);

    /**
     * If the target gradle version supports it you can use this setting
     * to specify the java vm arguments to use for gradle process.
     * If the target gradle version does not support it your setting is ignored and Gradle uses reasonable defaults.
     * <p>
     * {@link org.gradle.tooling.model.build.BuildEnvironment} model contains information such as java or gradle environment.
     * If you want to get hold of this information you can ask tooling API to build this model
     * <p>
     * If not configured or null passed the sensible default will be used
     *
     * @param jvmArguments to use for the gradle process
     * @return this
     */
    public abstract GradleConnector hintJvmArguments(String... jvmArguments);

    /**
     * Creates a connection to the project in the specified project directory. You should call {@link org.gradle.tooling.ProjectConnection#close()} when you are finished with the connection.
     *
     * @return The connection. Never return null.
     * @throws UnsupportedVersionException When the target Gradle version does not support this version of the tooling API.
     * @throws GradleConnectionException On failure to establish a connection with the target Gradle version.
     */
    public abstract ProjectConnection connect() throws GradleConnectionException;

}
