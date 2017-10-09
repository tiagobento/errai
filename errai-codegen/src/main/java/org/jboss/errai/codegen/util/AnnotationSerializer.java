package org.jboss.errai.codegen.util;

import org.jboss.errai.codegen.meta.MetaAnnotation;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaEnum;
import org.jboss.errai.common.client.util.SharedAnnotationSerializer;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.StreamSupport;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

public class AnnotationSerializer {

  private AnnotationSerializer() {
  }

  ;

  public static String[] serialize(final Iterator<Annotation> qualifier) {
    final List<String> qualifiers = new ArrayList<String>();
    qualifier.forEachRemaining(a -> qualifiers.add(
            SharedAnnotationSerializer.serialize(a, CDIAnnotationUtils.createDynamicSerializer(a.annotationType()))));

    return qualifiers.toArray(new String[qualifiers.size()]);
  }

  public static String[] serialize(final Spliterator<MetaAnnotation> qualifiers) {
    return serialize(StreamSupport.stream(qualifiers, false).collect(toList())).toArray(new String[0]);
  }

  public static Set<String> serialize(final Collection<MetaAnnotation> qualifiers) {
    Set<String> qualifiersPart = null;
    if (qualifiers != null) {
      for (final MetaAnnotation qualifier : qualifiers) {
        if (qualifiersPart == null) {
          qualifiersPart = new HashSet<>(qualifiers.size());
        }

        qualifiersPart.add(asString(qualifier));
      }
    }
    return qualifiersPart == null ? Collections.emptySet() : qualifiersPart;
  }

  public static String asString(final MetaAnnotation qualifier) {
    final StringBuilder builder = new StringBuilder(qualifier.annotationType().getFullyQualifiedName());
    final Set<String> keys = qualifier.values().keySet();

    if (!keys.isEmpty()) {
      builder.append('(');

      for (final String key : keys.stream().sorted(comparing(s -> s)).collect(toList())) {

        final Object value = qualifier.value(key);
        final String stringValue = value.getClass().isArray() ? serializeArray((Object[]) value) : serialize(value);
        builder.append(key).append('=').append(stringValue).append(',');
      }
      builder.replace(builder.length() - 1, builder.length(), ")");
    }

    return serialize(builder);
  }

  private static String serializeArray(final Object[] value) {
    return Arrays.toString(Arrays.stream(value).map(AnnotationSerializer::serialize).toArray());
  }

  private static String serialize(final Object value) {
    if (value instanceof MetaAnnotation) {
      return asString((MetaAnnotation) value);
    } else if (value instanceof MetaClass) {
      return "class " + ((MetaClass) value).getFullyQualifiedName();
    } else if (value instanceof MetaEnum) {
      return ((MetaEnum) value).name();
    } else {
      return String.valueOf(value);
    }
  }
}
