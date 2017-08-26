package org.jboss.errai.common.apt.exportfile;

import org.jboss.errai.common.apt.test.ErraiAptTest;
import org.junit.Test;

import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;

import static org.jboss.errai.common.apt.exportfile.ExportFileName.decodeAnnotationClassNameFromExportFileName;
import static org.jboss.errai.common.apt.exportfile.ExportFileName.encodeAnnotationNameAsExportFileName;
import static org.junit.Assert.assertEquals;

public class ExportFileNameTest extends ErraiAptTest {

  @interface TestAnnotationInner {
  }

  @Test
  public void testEncodeDecode() {
    testEncodeDecode(TestAnnotation.class);
  }

  @Test
  public void testEncodeDecodeInner() {
    testEncodeDecode(TestAnnotationInner.class);
  }

  private void testEncodeDecode(final Class<? extends Annotation> testAnnotationClass) {
    final TypeElement testAnnotationTypeElement = getTypeElement(testAnnotationClass);
    final String encodedName = encodeAnnotationNameAsExportFileName(testAnnotationTypeElement);
    assertEquals(testAnnotationClass.getCanonicalName(), decodeAnnotationClassNameFromExportFileName(encodedName));
  }

}