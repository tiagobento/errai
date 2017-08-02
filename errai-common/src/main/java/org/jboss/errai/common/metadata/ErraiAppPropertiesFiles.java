package org.jboss.errai.common.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ErraiAppPropertiesFiles {

  private static final Logger log = LoggerFactory.getLogger(ErraiAppPropertiesFiles.class);

  public static final String FILE_NAME = "ErraiApp.properties";
  private static final String META_INF_FILE_NAME = "META-INF/ErraiApp.properties";

  public static List<URL> getUrlsFrom(final ClassLoader... classLoaders) {
    return Stream.of(classLoaders)
            .map(ErraiAppPropertiesFiles::getUrlsFrom)
            .flatMap(e -> Collections.list(e).stream())
            .collect(Collectors.toList());
  }

  private static Enumeration<URL> getUrlsFrom(final ClassLoader classLoader) {
    try {
      Enumeration<URL> resources = classLoader.getResources(FILE_NAME);
      Enumeration<URL> metaInfResources = classLoader.getResources(META_INF_FILE_NAME);

      return resources;
    } catch (final IOException e) {
      throw new RuntimeException("failed to load " + FILE_NAME + " from classloader", e);
    }
  }

  public static List<URL> getDirUrls() {
    return getDirUrls(ErraiAppPropertiesFiles.class.getClassLoader());
  }

  static List<URL> getDirUrls(ClassLoader... classLoader) {
    try {
      final List<URL> configTargets = getUrlsFrom(classLoader);
      final List<URL> urls = new ArrayList<>();

      for (URL url : configTargets) {

        try {
          try (InputStream stream = url.openStream()) {
            new Properties().load(stream);
          }
        } catch (final IOException e) {
          System.err.println("could not read properties file");
          e.printStackTrace();
        }

        String urlString = url.toExternalForm();
        urlString = urlString.substring(0, urlString.indexOf(FILE_NAME));
        // URLs returned by the classloader are UTF-8 encoded. The URLDecoder assumes
        // a HTML form encoded String, which is why we escape the plus symbols here.
        // Otherwise, they would be decoded into space characters.
        // The pound character still must not appear anywhere in the path!
        urls.add(new URL(URLDecoder.decode(urlString.replaceAll("\\+", "%2b"), "UTF-8")));
      }

      return urls;
    } catch (final IOException e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to scan configuration Url's", e);
    }
  }
}
