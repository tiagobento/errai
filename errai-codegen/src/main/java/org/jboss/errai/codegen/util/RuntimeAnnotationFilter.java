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

package org.jboss.errai.codegen.util;

import org.jboss.errai.codegen.meta.MetaAnnotation;
import org.jboss.errai.codegen.meta.RuntimeMetaAnnotation;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
public class RuntimeAnnotationFilter implements AnnotationFilter {
  private final Set<String> translatablePackages;

  public RuntimeAnnotationFilter(final Set<String> translatablePackages) {
    this.translatablePackages = translatablePackages;
  }

  @Override
  public Annotation[] apply(final Annotation[] metaAnnotations) {
    return packageFilter(translatablePackages, metaAnnotations);
  }

  private Annotation[] packageFilter(final Set<String> packages, final Annotation[] annotations) {
    final Annotation[] firstPass = new Annotation[annotations.length];
    int j = 0;
    for (int i = 0; i < annotations.length; i++) {
      if (packages.contains(annotations[i].annotationType().getPackage().getName())) {
        firstPass[j++] = annotations[i];
      }
    }

    final Annotation[] retVal = new Annotation[j];
    for (int i = 0; i < j; i++) {
      retVal[i] = firstPass[i];
    }

    return retVal;
  }
}
