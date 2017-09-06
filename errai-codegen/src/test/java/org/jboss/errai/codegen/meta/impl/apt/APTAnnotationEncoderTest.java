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

import org.jboss.errai.codegen.apt.test.ErraiAptTest;
import org.jboss.errai.codegen.meta.MetaAnnotation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
public class APTAnnotationEncoderTest extends ErraiAptTest {

  @Test
  public void testEncodeAnnotationOfTestAnnotatedClass1() {
    final Optional<MetaAnnotation> annotation = new APTClass(
            getTypeElement(TestAnnotatedClass1.class).asType()).getAnnotation(TestAnnotation.class);

    Assert.assertTrue(annotation.isPresent());

    final String expected = "new org.jboss.errai.codegen.test.meta.apt.TestAnnotation() {"
            + "  public Class annotationType() {"
            + "    return org.jboss.errai.codegen.test.meta.apt.TestAnnotation.class;"
            + "  }"
            + "  public String toString() {"
            + "    return \"org.jboss.errai.codegen.test.meta.apt.TestAnnotation\";"
            + "  }"
            + "  public String value() {"
            + "    return \"\";"
            + "  }"
            + " }";

    assertEquals(expected, encode(annotation.get()));
  }

  @Test
  public void testEncodeAnnotationOfTestAnnotatedClass2() {
    final Optional<MetaAnnotation> annotation = new APTClass(
            getTypeElement(TestAnnotatedClass2.class).asType()).getAnnotation(TestAnnotation.class);

    Assert.assertTrue(annotation.isPresent());

    final String expected = "new org.jboss.errai.codegen.test.meta.apt.TestAnnotation() {"
            + "  public Class annotationType() {"
            + "    return org.jboss.errai.codegen.test.meta.apt.TestAnnotation.class;"
            + "  }"
            + "  public String toString() {"
            + "    return \"org.jboss.errai.codegen.test.meta.apt.TestAnnotation\";"
            + "  }"
            + "  public String value() {"
            + "    return \"foo\";"
            + "  }"
            + " }";

    assertEquals(expected, encode(annotation.get()));
  }

  @Test
  public void testEncodeAnnotationOfTestAnnotatedClass3() {
    final Optional<MetaAnnotation> annotation = new APTClass(
            getTypeElement(TestAnnotatedClass3.class).asType()).getAnnotation(TestAnnotationWithArrayProperties.class);

    Assert.assertTrue(annotation.isPresent());

    final String expected = "new org.jboss.errai.codegen.test.meta.apt.TestAnnotationWithArrayProperties() {"
            + "  public Class annotationType() {"
            + "    return org.jboss.errai.codegen.test.meta.apt.TestAnnotationWithArrayProperties.class;"
            + "  }"
            + "  public Class[] classes() {"
            + "    return new Class[] { String.class, Long.class };"
            + "  }"
            + "  public String toString() {"
            + "    return \"org.jboss.errai.codegen.test.meta.apt.TestAnnotationWithArrayProperties\";"
            + "  }"
            + "  public String[] value() {"
            + "    return new String[] { \"foo\" };"
            + "  }"
            + " }";

    assertEquals(expected, encode(annotation.get()));
  }

  private String encode(final MetaAnnotation metaAnnotation) {
    return APTAnnotationEncoder.encode((APTAnnotation) metaAnnotation).generate(null);
  }

}
