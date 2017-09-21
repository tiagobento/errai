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

package org.jboss.errai.ioc.rebind.ioc.bootstrapper.configuration;

import com.google.common.collect.Multimap;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.common.apt.configuration.module.ErraiModulesConfiguration;
import org.jboss.errai.common.metadata.MetaDataScanner;
import org.jboss.errai.common.metadata.ScannerSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.jboss.errai.ioc.util.PropertiesUtil.getPropertyValues;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
public class ErraiAppPropertiesModulesConfiguration implements ErraiModulesConfiguration {

  private static final String QUALIFYING_METADATA_FACTORY_PROPERTY = "errai.ioc.QualifyingMetaDataFactory";
  private static final String ENABLED_ALTERNATIVES_PROPERTY = "errai.ioc.enabled.alternatives";
  private static final String WHITELIST_PROPERTY = "errai.ioc.whitelist";
  private static final String BLACKLIST_PROPERTY = "errai.ioc.blacklist";

  private static final Logger log = LoggerFactory.getLogger(ErraiAppPropertiesModulesConfiguration.class);

  public ErraiAppPropertiesModulesConfiguration() {
    final MetaDataScanner scanner = ScannerSingleton.getOrCreateInstance();
    final Multimap<String, String> props = scanner.getErraiProperties();

    if (props != null) {
      log.info("Checking ErraiApp.properties for configured types ...");

      //FIXME: tiago: unused property?
      final Collection<String> qualifyingMetadataFactoryProperties = props.get(QUALIFYING_METADATA_FACTORY_PROPERTY);

      if (qualifyingMetadataFactoryProperties.size() > 1) {
        throw new RuntimeException(
                "the property '" + QUALIFYING_METADATA_FACTORY_PROPERTY + "' is set in more than one place");
      }
    }
  }

  //FIXME: tiago: wildcard doens't work on this implementation

  @Override
  public Set<MetaClass> getIocAlternatives() {
    return getPropertyValues(ENABLED_ALTERNATIVES_PROPERTY, "\\s").stream()
            .map(String::trim)
            .filter(s -> !s.contains("*"))
            .map(MetaClassFactory::get)
            .collect(toSet());
  }

  @Override
  public Set<MetaClass> getIocBlacklist() {
    return getPropertyValues(BLACKLIST_PROPERTY, "\\s").stream()
            .map(String::trim)
            .filter(s -> !s.contains("*"))
            .map(MetaClassFactory::get)
            .collect(toSet());
  }

  @Override
  public Set<MetaClass> getIocWhitelist() {
    return getPropertyValues(WHITELIST_PROPERTY, "\\s").stream()
            .map(String::trim)
            .filter(s -> !s.contains("*"))
            .map(MetaClassFactory::get)
            .collect(toSet());
  }

  @Override
  public Set<MetaClass> getBindableTypes() {
    return Collections.emptySet(); //FIXME: tiago: implement
  }

  @Override
  public Set<MetaClass> getSerializableTypes() {
    return Collections.emptySet(); //FIXME: tiago: implement
  }

  @Override
  public Set<MetaClass> getNonSerializableTypes() {
    return Collections.emptySet();  //FIXME: tiago: implement
  }
}
