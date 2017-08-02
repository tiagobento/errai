package org.jboss.errai.common.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ErraiAppPropertiesFiles {

  private static final Logger log = LoggerFactory.getLogger(ErraiAppPropertiesFiles.class);

  static final String FILE_NAME = "ErraiApp.properties";

  private static final String META_INF_FILE_NAME = "META-INF/" + FILE_NAME;

  /**
   * Returns the URLs of all ErraiApp.properties and META-INF/ErraiApp.properties.
   */
  public static List<URL> getUrls(final ClassLoader... classLoaders) {
    return Stream.of(classLoaders).flatMap(ErraiAppPropertiesFiles::getUrls).collect(Collectors.toList());
  }

  private static Stream<URL> getUrls(final ClassLoader classLoader) {
    try {

      final List<URL> rootDirResources = Collections.list(classLoader.getResources(FILE_NAME));
      final List<URL> metaInfResources = Collections.list(classLoader.getResources(META_INF_FILE_NAME));

      final List<URL> allResources = new ArrayList<>();
      allResources.addAll(rootDirResources);
      allResources.addAll(metaInfResources);

      logModulesWithTwoErraiAppPropertiesFiles(allResources);

      return allResources.stream();
    } catch (final IOException e) {
      throw new RuntimeException("failed to load " + FILE_NAME + " from classloader", e);
    }
  }

  public static List<URL> getModulesUrls() {
    return getModulesUrls(ErraiAppPropertiesFiles.class.getClassLoader());
  }

  //tests only
  static List<URL> getModulesUrls(final ClassLoader... classLoader) {
    return getUrls(classLoader).stream()
            .peek(ErraiAppPropertiesFiles::logUnreadablePropertiesFiles)
            .map(ErraiAppPropertiesFiles::getModuleDir)
            .distinct() //due to modules containing files both in classpath:/ and classpath:META-INF/
            .map(ErraiAppPropertiesFiles::decodeUrl)
            .collect(Collectors.toList());
  }

  private static void logUnreadablePropertiesFiles(final URL url) {
    try {
      try (InputStream stream = url.openStream()) {
        new Properties().load(stream);
      }
    } catch (final IOException e) {
      System.err.println("could not read properties file");
      e.printStackTrace();
    }
  }

  static String getModuleDir(final URL url) {
    final String urlString = url.toExternalForm();

    //will contain -1 if META_INF_FILE_NAME is not present
    final int metaInfIndex = urlString.indexOf(META_INF_FILE_NAME);

    return urlString.substring(0, metaInfIndex > -1 ? metaInfIndex : urlString.indexOf(FILE_NAME));
  }

  private static URL decodeUrl(final String moduleUrlString) {
    try {
      return new URL(URLDecoder.decode(moduleUrlString.replaceAll("\\+", "%2b"), "UTF-8"));
    } catch (final IOException e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to scan configuration Url's", e);
    }
  }

  private static void logModulesWithTwoErraiAppPropertiesFiles(final List<URL> concat) {

    final Map<String, Long> occurrencesByDir = concat.stream()
            .map(ErraiAppPropertiesFiles::getModuleDir)
            .collect(Collectors.groupingBy(dir -> dir, Collectors.counting()));

    occurrencesByDir.entrySet()
            .stream()
            .filter(e -> e.getValue() > 1L)
            .map(Map.Entry::getKey)
            .forEach(m -> log.warn("Module {} contains both /{} and /{} files. Please consider using only {}", m,
                    FILE_NAME, META_INF_FILE_NAME, META_INF_FILE_NAME));
  }

}
