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

package org.jboss.errai.ioc.util;

import org.jboss.errai.codegen.meta.impl.java.JavaReflectionAnnotation;
import org.jboss.errai.ioc.client.util.AnnotationAttrAnnotation;
import org.jboss.errai.ioc.client.util.AbstractAnnotationSerializerTest;
import org.jboss.errai.ioc.client.util.OneAttrAnnotation;
import org.junit.Test;

import java.lang.annotation.Annotation;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
public class MetaAnnotationSerializerTest extends AbstractAnnotationSerializerTest {

  @Override
  public String serialize(final Annotation annotation) {
    return MetaAnnotationSerializer.serialize(new JavaReflectionAnnotation(annotation));
  }

  @Test
  public void annotationWithAnnotationAttr() throws Exception {
    final OneAttrAnnotation oneAttrAnnotation = new OneAttrAnnotation() {

      @Override
      public Class<? extends Annotation> annotationType() {
        return OneAttrAnnotation.class;
      }

      @Override
      public int num() {
        return 42;
      }
    };

    final AnnotationAttrAnnotation annotation = new AnnotationAttrAnnotation() {

      @Override
      public OneAttrAnnotation ann() {
        return oneAttrAnnotation;
      }

      @Override
      public Class<? extends Annotation> annotationType() {
        return AnnotationAttrAnnotation.class;
      }
    };

    assertEquals(
            format("%s(ann=%s(num=%d))", AnnotationAttrAnnotation.class.getName(), OneAttrAnnotation.class.getName(),
                    42), serialize(annotation));
  }
}
