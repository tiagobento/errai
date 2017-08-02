package org.jboss.errai.common.metadata;

import org.junit.Test;

public class ErraiAppPropertiesFilesTest {

  @Test
  public void saudhf() {
    ClassLoader classLoader = ErraiAppPropertiesFilesTest.class.getClassLoader();
    ErraiAppPropertiesFiles.getUrlsFrom(classLoader);
  }
}
