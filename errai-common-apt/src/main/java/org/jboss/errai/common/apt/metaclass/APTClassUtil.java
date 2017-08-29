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

import net.florianschoppmann.java.reflect.ReflectionTypes;
import org.apache.commons.lang3.AnnotationUtils;
import org.apache.commons.lang3.ClassUtils;
import org.jboss.errai.codegen.meta.MetaAnnotation;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.codegen.meta.MetaType;
import org.jboss.errai.codegen.meta.MetaTypeVariable;
import org.jboss.errai.codegen.meta.impl.AbstractMetaClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
public final class APTClassUtil {

  private static Logger logger = LoggerFactory.getLogger(APTClassUtil.class);

  static Types types;
  static Elements elements;

  private APTClassUtil() {
  }

  public static void init(final Types types, final Elements elements) {
    APTClassUtil.types = types;
    APTClassUtil.elements = elements;
  }

  static MetaType fromTypeMirror(final TypeMirror mirror) {
    switch (mirror.getKind()) {
    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DOUBLE:
    case FLOAT:
    case INT:
    case LONG:
    case SHORT:
    case VOID:
      return new APTClass(mirror);
    case DECLARED:
      final DeclaredType dType = (DeclaredType) mirror;
      if (dType.getTypeArguments().isEmpty()) {
        return new APTClass(dType);
      } else {
        return new APTParameterizedType(dType);
      }
    case TYPEVAR:
      return new APTMetaTypeVariable((TypeParameterElement) ((TypeVariable) mirror).asElement());
    case WILDCARD:
      return new APTWildcardType((WildcardType) mirror);
    default:
      throw new UnsupportedOperationException(
              format("Don't know how to get a MetaType for %s [%s].", mirror.getKind(), mirror));
    }
  }

  static MetaTypeVariable[] getTypeParameters(final Parameterizable target) {
    return target.getTypeParameters().stream().map(APTMetaTypeVariable::new).toArray(MetaTypeVariable[]::new);
  }

  static MetaParameter[] getParameters(final ExecutableElement target) {
    return target.getParameters().stream().map(APTParameter::new).toArray(MetaParameter[]::new);
  }

  static MetaType[] getGenericParameterTypes(final ExecutableElement target) {
    return target.getParameters()
            .stream()
            .map(Element::asType)
            .map(APTClassUtil::fromTypeMirror)
            .toArray(MetaType[]::new);
  }

  static MetaClass[] getCheckedExceptions(final ExecutableElement target) {
    return target.getThrownTypes().stream().map(APTClass::new).toArray(APTClass[]::new);
  }

  static <T> T throwUnsupportedTypeError(final TypeMirror type) {
    throw new UnsupportedOperationException(format("Unsupported TypeMirror %s [%s].", type.getKind(), type));
  }

  static MetaClass eraseOrReturn(final TypeMirror type) {
    final TypeMirror erased = APTClassUtil.types.erasure(type);
    return new APTClass(erased);
  }

  static boolean sameTypes(final Iterator<? extends TypeMirror> iter1, final Iterator<? extends TypeMirror> iter2) {
    while (iter1.hasNext() && iter2.hasNext()) {
      if (!APTClassUtil.types.isSameType(iter1.next(), iter2.next())) {
        return false;
      }
    }

    return iter1.hasNext() == iter2.hasNext();
  }

  static String getSimpleName(final TypeMirror mirror) {
    switch (mirror.getKind()) {
    case DECLARED:
    case TYPEVAR:
      final Element element = types.asElement(mirror);
      return element.getSimpleName().toString();
    case ARRAY:
      return getSimpleName(((ArrayType) mirror).getComponentType()) + "[]";
    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DOUBLE:
    case FLOAT:
    case INT:
    case LONG:
    case SHORT:
    case VOID:
      return mirror.getKind().toString().toLowerCase();
    default:
      return throwUnsupportedTypeError(mirror);
    }
  }

  ////

  @Deprecated
  private static Object unsafeCreateAnnotationProxy(final AnnotationMirror mirror) throws ClassNotFoundException {
    final DeclaredType type = mirror.getAnnotationType();
    final TypeElement element = (TypeElement) APTClassUtil.types.asElement(type);
    final String fqcn = element.getQualifiedName().toString();
    final Map<? extends ExecutableElement, ? extends AnnotationValue> values = APTClassUtil.elements.getElementValuesWithDefaults(
            mirror);
    return unsafeCreateAnnotationProxy(fqcn, Class.forName(fqcn), values);
  }

  @Deprecated
  private static Object unsafeCreateAnnotationProxy(final String fqcn,
          final Class<?> annoClazz,
          final Map<? extends ExecutableElement, ? extends AnnotationValue> values) {

    final Map<String, Object> valueLookup = values.entrySet()
            .stream()
            .collect(toMap(e -> e.getKey().getSimpleName().toString(), e -> e.getValue().getValue()));

    return Proxy.newProxyInstance(annoClazz.getClassLoader(), new Class<?>[] { annoClazz }, (proxy, method, args) -> {
      if ("equals".equals(method.getName())) {
        return AnnotationUtils.equals((Annotation) proxy, (Annotation) args[0]);
      } else if ("annotationType".equals(method.getName())) {
        return annoClazz;
      } else if ("getClass".equals(method.getName())) {
        return annoClazz;
      } else if ("toString".equals(method.getName())) {
        return AnnotationUtils.toString((Annotation) proxy);
      } else if ("hashCode".equals(method.getName())) {
        return AnnotationUtils.hashCode((Annotation) proxy);
      } else if (valueLookup.containsKey(method.getName())) {
        final Object value = valueLookup.get(method.getName());
        return unsafeConvertValue(value, method.getReturnType());
      } else {
        throw new IllegalArgumentException(
                format("Unrecognized attribute [%s] for annotation type [%s].", method.getName(), fqcn));
      }
    });
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Deprecated
  private static Object unsafeConvertValue(final Object value, final Class<?> typeHint) {
    if (value instanceof String) {
      return value;
    } else if (value instanceof TypeMirror) {
      return unsafeLoadClass((TypeMirror) value);
    } else if (value instanceof VariableElement) {
      final VariableElement var = (VariableElement) value;
      final Class<?> enumClass = unsafeLoadClass(var.asType()); //FIXME: tiago: remove this (MetaEnum?)
      return Enum.valueOf((Class) enumClass, var.getSimpleName().toString());
    } else if (value instanceof AnnotationMirror) {
      try {
        return unsafeCreateAnnotationProxy((AnnotationMirror) value);
      } catch (final ClassNotFoundException e) {
        throw new IllegalArgumentException(format("Unable to convert annotation mirror to annotation: [%s].", value));
      }
    } else if (value instanceof List) {
      if (typeHint != null) {
        return unsafeConvertToArrayValue(value, typeHint);
      } else {
        throw new IllegalArgumentException(
                format("Cannot convert module [%s] to array without knowing array type.", value));
      }
    } else if (ClassUtils.isPrimitiveWrapper(value.getClass())) {
      return value;
    } else {
      throw new IllegalArgumentException(
              format("Unrecognized annotation module [%s] of type [%s].", value, value.getClass()));
    }
  }

  @Deprecated
  private static Object unsafeConvertToArrayValue(final Object value, final Class<?> arrayClass) {
    final List<?> list = (List<?>) value;
    return list.stream()
            .map(av -> ((AnnotationValue) av).getValue())
            .map(v -> unsafeConvertValue(v, null))
            .toArray(n -> (Object[]) Array.newInstance(arrayClass.getComponentType(), n));
  }

  @Deprecated
  static Class<?> unsafeLoadClass(final TypeMirror value) {
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

  @Deprecated
  public static Annotation[] unsafeGetAnnotations(Element target) {
    final List<? extends AnnotationMirror> annotationMirrors = elements.getAllAnnotationMirrors(target);
    return annotationMirrors.stream().flatMap(mirror -> {
      try {
        return Stream.of((Annotation) unsafeCreateAnnotationProxy(mirror));
      } catch (ClassNotFoundException e) {
        logger.warn(String.format("Unable to lookup Class for annotation type [%s]. Ignoring annotation on [%s].",
                mirror.getAnnotationType(), target));
      } catch (Throwable t) {
        logger.warn(String.format("Unable to proxy annotation for mirror [%s].", mirror), t);
      }
      return Stream.of();
    }).toArray(Annotation[]::new);
  }

  //

  public static Collection<MetaAnnotation> getAnnotations(final Element element) {
    return element.getAnnotationMirrors().stream().map(APTAnnotation::new).collect(toSet());
  }

  public static Collection<MetaAnnotation> getAnnotations(final TypeMirror mirror) {
    return getAnnotations(types.asElement(mirror));
  }

  public static Optional<MetaAnnotation> getAnnotation(final Element element,
          final Class<? extends Annotation> annotationClass) {

    final TypeMirror annotationTypeMirror = ReflectionTypes.getInstance().typeElement(annotationClass).asType();
    return element.getAnnotationMirrors()
            .stream()
            .filter(s -> s.getAnnotationType().toString().equals(annotationTypeMirror.toString()))
            .map(annotationMirror -> (MetaAnnotation) new APTAnnotation(annotationMirror))
            .findFirst();
  }

  public static Optional<MetaAnnotation> getAnnotation(final TypeMirror typeMirror,
          final Class<? extends Annotation> annotationClass) {
    return getAnnotation(types.asElement(typeMirror), annotationClass);
  }

  public static boolean isAnnotationPresent(final Element element, final MetaClass metaClass) {
    return element.getAnnotationMirrors()
            .stream()
            .map(annotationMirror -> annotationMirror.getAnnotationType().toString())
            .anyMatch(type -> type.equals(((AbstractMetaClass) metaClass).getEnclosedMetaObject().toString()));
  }
}
