package org.jboss.errai.common.metadata;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ErraiAppPropertiesFilesTest {

  @Test
  public void testGetUrls() {
    ClassLoader classLoader = ErraiAppPropertiesFilesTest.class.getClassLoader();
    List<URL> urls = ErraiAppPropertiesFiles.getUrls(classLoader);

    // Errai Common has a properties this and Errai Common Test has other two.
    assertEquals(3, urls.size());
  }

  @Test
  public void testGetModuleUrls() {
    List<URL> moduleUrls = ErraiAppPropertiesFiles.getModulesUrls();

    // Errai Common has a properties this and Errai Common Test has other two,
    // but since they together make two modules, only two URLs should be returned.
    assertEquals(2, moduleUrls.size());
  }

  @Test
  public void testGetModuleDirRootFile() throws MalformedURLException {
    URL url = new URL("file:/foo/bar/ErraiApp.properties/");
    String moduleDir = ErraiAppPropertiesFiles.getModuleDir(url);

    assertEquals("file:/foo/bar/", moduleDir);
  }

  @Test
  public void testGetModuleDirMetaInfFile() throws MalformedURLException {
    URL url = new URL("file:/foo/bar/META-INF/ErraiApp.properties/");
    String moduleDir = ErraiAppPropertiesFiles.getModuleDir(url);

    assertEquals("file:/foo/bar/", moduleDir);
  }
}
