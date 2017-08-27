package org.jboss.errai.common.apt.exportfile;

import org.jboss.errai.common.apt.test.ErraiAptTest;
import org.junit.Test;

import java.lang.annotation.Annotation;

import static java.util.Collections.emptySet;
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
    final ExportFile exportFile = new ExportFile("test", getTypeElement(testAnnotationClass), emptySet());
    final String encodedName = encodeAnnotationNameAsExportFileName(exportFile);
    assertEquals(testAnnotationClass.getCanonicalName(), decodeAnnotationClassNameFromExportFileName(encodedName));
  }

}