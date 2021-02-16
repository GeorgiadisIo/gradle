/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.internal.execution.impl;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import org.gradle.api.internal.DocumentationRegistry;
import org.gradle.internal.execution.WorkValidationContext;
import org.gradle.internal.reflect.MessageFormattingTypeValidationContext;
import org.gradle.internal.reflect.validation.Severity;
import org.gradle.internal.reflect.validation.TypeValidationContext;
import org.gradle.internal.reflect.validation.TypeValidationProblem;
import org.gradle.internal.reflect.validation.TypeValidationProblemRenderer;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DefaultWorkValidationContext implements WorkValidationContext {
    private final Set<Class<?>> types = new HashSet<>();
    private final List<TypeValidationProblem> problems = Lists.newArrayList();
    private final DocumentationRegistry documentationRegistry;

    public DefaultWorkValidationContext(DocumentationRegistry documentationRegistry) {
        this.documentationRegistry = documentationRegistry;
    }

    @Override
    public TypeValidationContext forType(Class<?> type, boolean cacheable) {
        types.add(type);
        return new MessageFormattingTypeValidationContext(documentationRegistry, null) {
            @Override
            protected void recordProblem(TypeValidationProblem problem) {
                Severity severity = problem.getSeverity();
                if (severity == Severity.CACHEABILITY_WARNING && !cacheable) {
                    return;
                }
                problems.add(problem);
            }
        };
    }

    public ImmutableMultimap<Severity, String> getProblems() {
        ImmutableMultimap.Builder<Severity, String> builder = ImmutableMultimap.builder();
        problems.forEach(problem -> {
            Severity kind = problem.getSeverity().toReportableSeverity();
            String message = TypeValidationProblemRenderer.renderMinimalInformationAbout(problem);
            builder.put(kind, message);
        });
        return builder.build();
    }

    public ImmutableSortedSet<Class<?>> getValidatedTypes() {
        return ImmutableSortedSet.copyOf(Comparator.comparing(Class::getName), types);
    }
}
