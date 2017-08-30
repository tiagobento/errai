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

package org.jboss.errai.codegen.meta.impl.apt;

import org.apache.commons.lang3.ClassUtils;
import org.jboss.errai.codegen.meta.MetaAnnotation;
import org.jboss.errai.codegen.meta.MetaClass;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.jboss.errai.codegen.meta.impl.apt.APTClassUtil.elements;
import static org.jboss.errai.codegen.meta.impl.apt.APTClassUtil.throwUnsupportedTypeError;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
public class APTAnnotation extends MetaAnnotation {

  private final Map<String, Object> values;
  private final AnnotationMirror annotationMirror;

  public APTAnnotation(final AnnotationMirror annotationMirror) {
    this.annotationMirror = annotationMirror;
    this.values = elements.getElementValuesWithDefaults(annotationMirror)
            .entrySet()
            .stream()
            .collect(toMap(e -> e.getKey().getSimpleName().toString(), e -> e.getValue().getValue()));
  }

  @Override
  public Object value(final String attributeName) {
    return convertValue(values.get(attributeName));
  }

  @Override
  public MetaClass annotationType() {
    return new APTClass(annotationMirror.getAnnotationType());
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
      final Class<?> enumClass = unsafeLoadClass(var.asType()); //FIXME: tiago: remove this (MetaEnum?)
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
  @Deprecated
  private static Class<?> unsafeLoadClass(final TypeMirror value) {
    switch (value.getKind()) {
    case ARRAY: {
      TypeMirror cur = value;
      int dim = 0;
      do {
        cur = ((ArrayType) cur).getComponentType();
        dim += 1;
      } while (cur.getKind().equals(TypeKind.ARRAY));
      final Class<?> componentClazz = unsafeLoadClass(cur);
      final int[] dims = new int[dim];
      final Object array = Array.newInstance(componentClazz, dims);

      return array.getClass();
    }
    case DECLARED:
      final String fqcn = ((TypeElement) ((DeclaredType) value).asElement()).getQualifiedName().toString();
      try {
        return Class.forName(fqcn);
      } catch (final ClassNotFoundException e) {
        throw new IllegalArgumentException(format("Cannot load class object for [%s].", fqcn));
      }
    case BOOLEAN:
      return boolean.class;
    case BYTE:
      return byte.class;
    case CHAR:
      return char.class;
    case DOUBLE:
      return double.class;
    case FLOAT:
      return float.class;
    case INT:
      return int.class;
    case LONG:
      return long.class;
    case SHORT:
      return short.class;
    case VOID:
      return void.class;
    default:
      return throwUnsupportedTypeError(value);
    }
  }

  public AnnotationMirror annotationMirror() {
    return annotationMirror;
  }
}
