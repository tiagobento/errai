package org.jboss.errai.common.apt.exportfile;

import org.jboss.errai.apt.internal.export.annotation.ErraiCommonAptExportedAnnotations;
import org.jboss.errai.apt.internal.export.test_hYbKGF_ExportFile_org_jboss_errai_common_apt_test_TestAnnotation;
import org.jboss.errai.apt.internal.generator.TestGenerator;
import org.jboss.errai.common.apt.ErraiAptPackages;
import org.jboss.errai.codegen.apt.test.ErraiAptTest;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ErraiAptPackagesTest extends ErraiAptTest {

  @Test
  public void testPackagesExist() {
    assertTrue(ErraiAptPackages.exportFilesPackageElement(elements).isPresent());
    assertTrue(ErraiAptPackages.exportedAnnotationsPackageElement(elements).isPresent());
    assertTrue(ErraiAptPackages.generatorsPackageElement(elements).isPresent());
  }

  @Test
  public void testElementsInPackage() {
    assertTrue(ErraiAptPackages.exportFilesPackageElement(elements)
            .map(p -> p.getEnclosedElements()
                    .contains(getTypeElement(
                            test_hYbKGF_ExportFile_org_jboss_errai_common_apt_test_TestAnnotation.class)))
            .orElse(false));

    assertTrue(ErraiAptPackages.exportedAnnotationsPackageElement(elements)
            .map(p -> p.getEnclosedElements().contains(getTypeElement(ErraiCommonAptExportedAnnotations.class)))
            .orElse(false));

    assertTrue(ErraiAptPackages.generatorsPackageElement(elements)
            .map(p -> p.getEnclosedElements().contains(getTypeElement(TestGenerator.class)))
            .orElse(false));
  }
}