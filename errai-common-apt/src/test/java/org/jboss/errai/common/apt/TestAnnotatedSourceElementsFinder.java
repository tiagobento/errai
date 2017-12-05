/*
 * Copyright (C) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.common.apt;

import org.jboss.errai.common.apt.generator.AnnotatedSourceElementsFinder;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
public class TestAnnotatedSourceElementsFinder implements AnnotatedSourceElementsFinder {

  private final Set<? extends Element> elements;

  public TestAnnotatedSourceElementsFinder(final Element... elements) {
    this.elements = Arrays.stream(elements).collect(toSet());
  }

  @Override
  public Set<? extends Element> findSourceElementsAnnotatedWith(final TypeElement typeElement) {
    return elements;
  }
}
