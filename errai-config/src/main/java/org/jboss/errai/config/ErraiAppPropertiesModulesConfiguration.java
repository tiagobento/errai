/*
 * Copyright (C) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.config;

import com.google.common.collect.Multimap;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.common.metadata.MetaDataScanner;
import org.jboss.errai.common.metadata.ScannerSingleton;
import org.jboss.errai.config.rebind.EnvUtil;
import org.jboss.errai.reflections.util.SimplePackageFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
public class ErraiAppPropertiesModulesConfiguration implements ErraiModulesConfiguration {

  public static final String SERIALIZABLE_TYPES = "errai.marshalling.serializableTypes";
  public static final String NONSERIALIZABLE_TYPES = "errai.marshalling.nonserializableTypes";
  private static final String QUALIFYING_METADATA_FACTORY = "errai.ioc.QualifyingMetaDataFactory";
  public static final String IOC_ENABLED_ALTERNATIVES = "errai.ioc.enabled.alternatives";
  private static final String IOC_WHITELIST_PROPERTY = "errai.ioc.whitelist";
  private static final String IOC_BLACKLIST_PROPERTY = "errai.ioc.blacklist";
  public static final String BINDABLE_TYPES = "errai.ui.bindableTypes";

  private static final Logger log = LoggerFactory.getLogger(ErraiAppPropertiesModulesConfiguration.class);

  public ErraiAppPropertiesModulesConfiguration() {
    final MetaDataScanner scanner = ScannerSingleton.getOrCreateInstance();
    final Multimap<String, String> props = scanner.getErraiProperties();

    if (props != null) {
      log.info("Checking ErraiApp.properties for configured types ...");

      //FIXME: tiago: unused property?
      final Collection<String> qualifyingMetadataFactoryProperties = props.get(QUALIFYING_METADATA_FACTORY);

      if (qualifyingMetadataFactoryProperties.size() > 1) {
        throw new RuntimeException("the property '" + QUALIFYING_METADATA_FACTORY + "' is set in more than one place");
      }
    }
  }

  //FIXME: tiago: wildcards doesn't work on this implementation

  @Override
  public Set<MetaClass> getIocEnabledAlternatives() {
    return PropertiesUtil.getPropertyValues(IOC_ENABLED_ALTERNATIVES, "\\s")
            .stream()
            .map(String::trim)
            .filter(s -> !s.contains("*"))
            .map(MetaClassFactory::get)
            .collect(toSet());
  }

  @Override
  public Set<MetaClass> getIocBlacklist() {
    return PropertiesUtil.getPropertyValues(IOC_BLACKLIST_PROPERTY, "\\s")
            .stream()
            .map(String::trim)
            .filter(s -> !s.contains("*"))
            .map(MetaClassFactory::get)
            .collect(toSet());
  }

  @Override
  public Set<MetaClass> getIocWhitelist() {
    return PropertiesUtil.getPropertyValues(IOC_WHITELIST_PROPERTY, "\\s")
            .stream()
            .map(String::trim)
            .filter(s -> !s.contains("*"))
            .map(MetaClassFactory::get)
            .collect(toSet());
  }

  @Override
  public Set<MetaClass> getBindableTypes() {
    final Set<MetaClass> bindableTypes = new HashSet<>();

    for (final URL url : EnvUtil.getErraiAppPropertiesFilesUrls()) {
      InputStream inputStream = null;
      try {
        log.debug("Checking " + url.getFile() + " for bindable types...");
        inputStream = url.openStream();

        final ResourceBundle props = new PropertyResourceBundle(inputStream);
        for (final String key : props.keySet()) {
          if (key.equals(ErraiAppPropertiesModulesConfiguration.BINDABLE_TYPES)) {
            final Set<String> patterns = new LinkedHashSet<>();

            for (final String s : props.getString(key).split(" ")) {
              final String singleValue = s.trim();
              if (singleValue.endsWith("*")) {
                patterns.add(singleValue);
              }
              else {
                try {
                  bindableTypes.add(MetaClassFactory.get(s.trim()));
                } catch (final Exception e) {
                  throw new RuntimeException("Could not find class defined in ErraiApp.properties as bindable type: " + s);
                }
              }
            }

            if (!patterns.isEmpty()) {
              final SimplePackageFilter filter = new SimplePackageFilter(patterns);
              MetaClassFactory
                      .getAllCachedClasses()
                      .stream()
                      .filter(mc -> filter.apply(mc.getFullyQualifiedName()) && validateWildcard(mc))
                      .collect(toCollection(() -> bindableTypes));
            }
            break;
          }
        }
      } catch (final IOException e) {
        throw new RuntimeException("Error reading ErraiApp.properties", e);
      } finally {
        if (inputStream != null) {
          try {
            inputStream.close();
          } catch (final IOException e) {
            log.warn("Failed to close input stream", e);
          }
        }
      }
    }

    return bindableTypes;
  }



  @Override
  public Set<MetaClass> getSerializableTypes() {
    final Set<MetaClass> serializableTypes = new HashSet<>();
    serializableTypes.addAll(EnvUtil.getEnvironmentConfig().getExposedClasses());
    serializableTypes.addAll(EnvUtil.getEnvironmentConfig().getPortableSuperTypes());
    return serializableTypes;
  }

  @Override
  public Set<MetaClass> getNonSerializableTypes() {
    return Collections.emptySet();
  }

  private static boolean validateWildcard(MetaClass bindable) {
    if (bindable.isFinal()) {
      log.debug("@Bindable types cannot be final, ignoring: {}", bindable.getFullyQualifiedName());
      return false;
    }
    return true;
  }
}