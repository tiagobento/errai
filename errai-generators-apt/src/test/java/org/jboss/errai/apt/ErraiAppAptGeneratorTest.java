package org.jboss.errai.apt;

import org.jboss.errai.common.apt.AnnotatedElementsFinder;
import org.junit.Assert;
import org.junit.Test;

import javax.lang.model.element.TypeElement;
import java.util.Set;

import static java.util.Collections.emptySet;

public class ErraiAppAptGeneratorTest {

  @Test
  public void testExceptionDoesNotBreakIt() {
    Assert.assertFalse(new TestGenerator() {
      @Override
      void generateAndSaveSourceFiles(Set<? extends TypeElement> annotations,
              AnnotatedElementsFinder annotatedElementsFinder) {
        throw new TestException();
      }
    }.process(null, null));
  }

  @Test
  public void testProcessForEmptyAnnotationsSet() {
    Assert.assertFalse(new TestGenerator().process(emptySet(), null));
  }

  @Test
  public void testFindGenerators() {

    try {
      new TestGenerator().findGenerators(null);
    } catch (Exception e) {
      return;
    }

    Assert.fail("Exception was not thrown");
  }
}