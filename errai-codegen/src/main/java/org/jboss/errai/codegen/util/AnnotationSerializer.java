package org.jboss.errai.codegen.util;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.jboss.errai.codegen.meta.MetaAnnotation;
import org.jboss.errai.common.client.util.SharedAnnotationSerializer;

public class AnnotationSerializer {
  
  private AnnotationSerializer() {};  

  public static String[] serialize(final Iterator<Annotation> qualifier) {
    final List<String> qualifiers = new ArrayList<String>();
    qualifier.forEachRemaining(a -> qualifiers.add(SharedAnnotationSerializer.serialize(a, CDIAnnotationUtils.createDynamicSerializer(a.annotationType()))));
    
    return qualifiers.toArray(new String[qualifiers.size()]);
  }

  public static String[] serialize(final Spliterator<MetaAnnotation> qualifiers) {
    return serialize(StreamSupport.stream(qualifiers, false).collect(Collectors.toList())).toArray(new String[0]);
  }

  public static Set<String> serialize(final Collection<MetaAnnotation> qualifiers) {
    Set<String> qualifiersPart = null;
    if (qualifiers != null) {
      for (final MetaAnnotation qualifier : qualifiers) {
        if (qualifiersPart == null)
          qualifiersPart = new HashSet<>(qualifiers.size());

        qualifiersPart.add(asString(qualifier));
      }
    }
    return qualifiersPart == null ? Collections.emptySet() : qualifiersPart;
  }

  private static String asString(final MetaAnnotation qualifier) {
    final StringBuilder builder = new StringBuilder(qualifier.annotationType().getFullyQualifiedName());
    final Map<String, Object> values = qualifier.values();

    if (values.isEmpty()) {
      builder.append('(');
      for (final Map.Entry<String, Object> e : values.entrySet()) {
        builder.append(e.getKey()).append('=').append(e.getValue()).append(',');
      }
      builder.replace(builder.length() - 1, builder.length(), ")");
    }

    return builder.toString();
  }
}
