package org.jboss.errai.codegen.util;

import org.jboss.errai.codegen.meta.MetaAnnotation;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaEnum;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.StreamSupport;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

public class AnnotationSerializer {

  private AnnotationSerializer() {
  }

  public static String[] getQualifiersPart(final Spliterator<MetaAnnotation> qualifiers) {
    return getQualifiersPart(StreamSupport.stream(qualifiers, false).collect(toList())).toArray(new String[0]);
  }

  public static Set<String> getQualifiersPart(final Collection<MetaAnnotation> qualifiers) {
    Set<String> qualifiersPart = null;
    if (qualifiers != null) {
      for (final MetaAnnotation qualifier : qualifiers) {
        if (qualifiersPart == null) {
          qualifiersPart = new HashSet<>(qualifiers.size());
        }

        qualifiersPart.add(serializeMetaAnnotation(qualifier));
      }
    }
    return qualifiersPart == null ? Collections.emptySet() : qualifiersPart;
  }

  public static String serializeMetaAnnotation(final MetaAnnotation qualifier) {
    final StringBuilder builder = new StringBuilder(qualifier.annotationType().getFullyQualifiedName());
    final Set<String> keys = qualifier.values().keySet();

    if (!keys.isEmpty()) {
      builder.append('(');

      for (final String key : keys.stream().sorted(comparing(s -> s)).collect(toList())) {
        final Object value = qualifier.value(key);
        final String stringValue = value.getClass().isArray() ? serializeArray((Object[]) value) : serializeObject(value);
        builder.append(key).append('=').append(stringValue).append(',');
      }
      builder.replace(builder.length() - 1, builder.length(), ")");
    }

    return serializeObject(builder);
  }

  private static String serializeArray(final Object[] value) {
    return Arrays.toString(Arrays.stream(value).map(AnnotationSerializer::serializeObject).toArray());
  }

  private static String serializeObject(final Object value) {
    if (value instanceof MetaAnnotation) {
      return serializeMetaAnnotation((MetaAnnotation) value);
    } else if (value instanceof MetaClass) {
      return ((MetaClass) value).getFullyQualifiedName();
    } else if (value instanceof MetaEnum) {
      return ((MetaEnum) value).name();
    } else {
      return String.valueOf(value);
    }
  }
}
