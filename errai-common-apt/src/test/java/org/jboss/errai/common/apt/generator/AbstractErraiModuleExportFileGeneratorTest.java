package org.jboss.errai.common.apt.generator;

import org.jboss.errai.common.apt.AnnotatedElementsFinder;
import org.jboss.errai.common.apt.exportfile.ExportFile;
import org.jboss.errai.common.apt.test.ErraiAptTest;
import org.junit.Assert;
import org.junit.Test;

import javax.lang.model.element.TypeElement;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

public class AbstractErraiModuleExportFileGeneratorTest extends ErraiAptTest {

  @Test
  public void testThrownExceptionDoesNotBreakIt() {
    Assert.assertFalse(new TestGenerator() {
      @Override
      void generateAndSaveExportFiles(final Set<? extends TypeElement> annotations,
              final AnnotatedElementsFinder annotatedElementsFinder) {
        throw new TestException();
      }
    }.process(null, null));
  }

  @Test
  public void testProcessForEmptyAnnotationsSet() {
    Assert.assertFalse(new TestGenerator().process(emptySet(), null));
  }

  @Test
  public void testGenerateExportFilesForUsedAnnotation() {
    final TypeElement testAnnotation = getTypeElement(TestAnnotation.class);
    final TypeElement testExportedType = getTypeElement(TestExportedType.class);

    final Set<ExportFile> exportFiles = new TestGenerator().generateExportFiles(singleton(testAnnotation),
            new TestAnnotatedElementsFinder(testExportedType));

    Assert.assertEquals(1, exportFiles.size());
    final ExportFile exportFile = exportFiles.stream().findFirst().get();
    Assert.assertEquals(testAnnotation, exportFile.annotation);
    Assert.assertEquals(singleton(testExportedType), exportFile.exportedTypes);
  }

  @Test
  public void testGenerateExportFilesForUnusedAnnotation() {
    final Set<TypeElement> annotations = singleton(getTypeElement(TestUnusedAnnotation.class));
    final Set<ExportFile> exportFiles = new TestGenerator().generateExportFiles(annotations,
            new TestAnnotatedElementsFinder());

    Assert.assertEquals(0, exportFiles.size());
  }

  @Test
  public void testGenerateExportFilesForEmptySetOfAnnotations() {
    final Set<ExportFile> exportFiles = new TestGenerator().generateExportFiles(emptySet(), null);
    Assert.assertEquals(0, exportFiles.size());
  }

  @Test
  public void testAnnotatedClassesAndInterfaces() {
      //FIXME: tiago: implement
  }
}