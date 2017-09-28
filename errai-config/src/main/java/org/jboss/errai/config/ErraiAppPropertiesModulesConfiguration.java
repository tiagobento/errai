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
import org.jboss.errai.common.client.api.annotations.NonPortable;
import org.jboss.errai.common.client.api.annotations.Portable;
import org.jboss.errai.common.metadata.MetaDataScanner;
import org.jboss.errai.common.metadata.ScannerSingleton;
import org.jboss.errai.config.util.ClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
    return PropertiesUtil.getPropertyValues(BINDABLE_TYPES, "\\s")
            .stream()
            .map(String::trim)
            .filter(s -> !s.contains("*"))
            .map(MetaClassFactory::get)
            .collect(toSet());
  }

  @Override
  public Set<MetaClass> getSerializableTypes() {
    final Set<MetaClass> serializableTypes = new HashSet<>(ClassScanner.getTypesAnnotatedWith(Portable.class));
    serializableTypes.addAll(PropertiesUtil.getPropertyValues(SERIALIZABLE_TYPES, "\\s")
            .stream()
            .map(String::trim)
            .filter(s -> !s.contains("*"))
            .map(MetaClassFactory::get)
            .collect(toSet()));

    return serializableTypes;
  }

  @Override
  public Set<MetaClass> getNonSerializableTypes() {
    final Set<MetaClass> nonSerializableTypes = new HashSet<>(ClassScanner.getTypesAnnotatedWith(NonPortable.class));
    nonSerializableTypes.addAll(PropertiesUtil.getPropertyValues(NONSERIALIZABLE_TYPES, "\\s")
            .stream()
            .map(String::trim)
            .filter(s -> !s.contains("*"))
            .map(MetaClassFactory::get)
            .collect(toSet()));

    return nonSerializableTypes;
  }
}
