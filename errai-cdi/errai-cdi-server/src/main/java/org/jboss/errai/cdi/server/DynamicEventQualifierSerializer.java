/**
 * Copyright (C) 2016 Red Hat, Inc. and/or its affiliates.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.cdi.server;

import org.jboss.errai.codegen.util.CDIAnnotationUtils;
import org.jboss.errai.common.client.util.AnnotationPropertyAccessor;
import org.jboss.errai.common.client.util.AnnotationPropertyAccessorBuilder;
import org.jboss.errai.enterprise.client.cdi.EventQualifierSerializer;

import javax.enterprise.util.Nonbinding;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static java.lang.reflect.Modifier.isPublic;

/**
 * A specialization of {@link EventQualifierSerializer} that uses the Java Reflection API to serialize qualifiers.
 *
 * This implementation is used in scenarios where a statically generated {@link EventQualifierSerializer} has not been
 * packaged in a deployment and cannot be generated when the application bootstraps.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class DynamicEventQualifierSerializer extends EventQualifierSerializer {

  @Override
  public String serialize(final Annotation qualifier) {
    if (!serializers.containsKey(qualifier.annotationType().getName())) {
      final AnnotationPropertyAccessor serializer = createDynamicSerializer(qualifier.annotationType());
      serializers.put(qualifier.annotationType().getName(), serializer);
    }

    return super.serialize(qualifier);
  }

  private static AnnotationPropertyAccessor createDynamicSerializer(final Class<? extends Annotation> annotationType) {
    final AnnotationPropertyAccessorBuilder builder = AnnotationPropertyAccessorBuilder.create();

    final Collection<Method> annoAttrs = getAnnotationAttributes(annotationType);
    for (final Method attr : annoAttrs) {
      builder.with(attr.getName(), anno -> {
        try {
          final String retVal;
          final Function<Object, String> toString = componentToString(
                  attr.getReturnType().isArray() ? attr.getReturnType().getComponentType() : attr.getReturnType());
          if (attr.getReturnType().isArray()) {
            final StringBuilder sb = new StringBuilder();
            final Object[] array = (Object[]) attr.invoke(anno);
            sb.append("[");
            for (final Object obj : array) {
              sb.append(toString.apply(obj)).append(",");
            }
            sb.replace(sb.length() - 1, sb.length(), "]");
            retVal = sb.toString();
          } else {
            retVal = toString.apply(attr.invoke(anno));
          }
          return retVal;
        } catch (final Exception e) {
          throw new RuntimeException(
                  String.format("Could not access '%s' property while serializing %s.", attr.getName(),
                          anno.annotationType()), e);
        }
      });
    }

    return builder.build();
  }

  private static Collection<Method> getAnnotationAttributes(final Class<? extends Annotation> annotationClass) {
    return CDIAnnotationUtils.filterAnnotationMethods(Arrays.stream(annotationClass.getDeclaredMethods()),
            method -> !method.isAnnotationPresent(Nonbinding.class)
                    && isPublic(method.getModifiers())
                    && !method.getName().equals("equals")
                    && !method.getName().equals("hashCode"));
  }

  private static Function<Object, String> componentToString(final Class<?> returnType) {
    if (Class.class.equals(returnType)) {
      return o -> ((Class<?>) o).getName();
    } else {
      return String::valueOf;
    }
  }

}
