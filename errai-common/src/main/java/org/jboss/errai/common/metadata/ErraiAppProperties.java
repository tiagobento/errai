package org.jboss.errai.common.metadata;

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

public class ErraiAppProperties {

  public static final String STUB_NAME = "ErraiApp.properties";

  public static List<URL> getUrlsFrom(final ClassLoader... classLoaders) {
    return Stream.of(classLoaders)
            .map(ErraiAppProperties::getUrlsFrom)
            .flatMap(e -> Collections.list(e).stream())
            .collect(Collectors.toList());
  }

  private static Enumeration<URL> getUrlsFrom(final ClassLoader classLoader) {
    try {
      return classLoader.getResources(ErraiAppProperties.STUB_NAME);
    } catch (final IOException e) {
      throw new RuntimeException("failed to load " + STUB_NAME + " from classloader", e);
    }
  }

  public static List<URL> getConfigUrls() {
    return getConfigUrls(ErraiAppProperties.class.getClassLoader());
  }

  static List<URL> getConfigUrls(ClassLoader... classLoader) {
    try {
      final List<URL> configTargets = ErraiAppProperties.getUrlsFrom(classLoader);
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
        urlString = urlString.substring(0, urlString.indexOf(ErraiAppProperties.STUB_NAME));
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
