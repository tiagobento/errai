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

package org.jboss.errai.common.apt.metaclass;

import org.apache.commons.lang3.ClassUtils;
import org.jboss.errai.codegen.meta.MetaAnnotation;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.jboss.errai.common.apt.metaclass.APTClassUtil.elements;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
public class APTAnnotation extends MetaAnnotation {

  private final Map<String, Object> values;

  public APTAnnotation(final AnnotationMirror annotationMirror) {
    this.values = elements.getElementValuesWithDefaults(annotationMirror)
            .entrySet()
            .stream()
            .collect(toMap(e -> e.getKey().getSimpleName().toString(), e -> e.getValue().getValue()));
  }

  @Override
  public Object value(final String attributeName) {
    return convertValue(values.get(attributeName));
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static Object convertValue(final Object value) {
    if (value == null) {
      return null;
    } else if (value instanceof String) {
      return value;
    } else if (value instanceof TypeMirror) {
      return new APTClass((TypeMirror) value);
    } else if (value instanceof VariableElement) {
      final VariableElement var = (VariableElement) value;
      final Class<?> enumClass = APTClassUtil.unsafeLoadClass(var.asType()); //FIXME: tiago: remove this (MetaEnum?)
      return Enum.valueOf((Class) enumClass, var.getSimpleName().toString());
    } else if (value instanceof AnnotationMirror) {
      return new APTAnnotation((AnnotationMirror) value);
    } else if (value instanceof List) {
      return convertToArrayValue(value);
    } else if (ClassUtils.isPrimitiveWrapper(value.getClass())) {
      return value;
    } else {
      throw new IllegalArgumentException(
              format("Unrecognized annotation module [%s] of type [%s].", value, value.getClass()));
    }
  }

  private static List<?> convertToArrayValue(final Object value) {
    final List<?> list = (List<?>) value;
    return list.stream()
            .map(av -> ((AnnotationValue) av).getValue())
            .map(APTAnnotation::convertValue)
            .collect(Collectors.toList());
  }
}
