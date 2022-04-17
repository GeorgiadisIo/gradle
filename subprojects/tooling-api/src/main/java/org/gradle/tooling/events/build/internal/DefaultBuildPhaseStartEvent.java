/*
 * Copyright 2022 the original author or authors.
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

package org.gradle.tooling.events.build.internal;

import org.gradle.tooling.events.build.BuildPhaseOperationDescriptor;
import org.gradle.tooling.events.build.BuildPhaseStartEvent;
import org.gradle.tooling.events.internal.DefaultStartEvent;

public class DefaultBuildPhaseStartEvent extends DefaultStartEvent implements BuildPhaseStartEvent {

    public DefaultBuildPhaseStartEvent(long eventTime, String displayName, BuildPhaseOperationDescriptor descriptor) {
        super(eventTime, displayName, descriptor);
    }

    @Override
    public BuildPhaseOperationDescriptor getDescriptor() {
        return (BuildPhaseOperationDescriptor) super.getDescriptor();
    }
}
