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

package org.jboss.errai.apt.internal.generator;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.common.apt.ErraiAptExportedTypes;
import org.jboss.errai.common.apt.ErraiAptGenerator;
import org.jboss.errai.common.apt.configuration.ErraiAptConfiguration;
import org.jboss.errai.common.apt.configuration.ErraiConfiguration;
import org.jboss.errai.ioc.client.api.EntryPoint;
import org.jboss.errai.ioc.client.api.IOCProvider;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.IOCGenerator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * IMPORTANT: Do not move this class. ErraiAppAptGenerator depends on it being in this exact package.
 *
 * @author Tiago Bento <tfernand@redhat.com>
 */
public class IocAptGenerator extends ErraiAptGenerator {

  private final IOCGenerator iocGenerator;
  private final ErraiAptConfiguration erraiModuleConfiguration;

  // IMPORTANT: Do not remove. ErraiAppAptGenerator depends on this constructor
  public IocAptGenerator(final ErraiAptExportedTypes exportedTypes) {
    super(exportedTypes);
    this.iocGenerator = new IOCGenerator();
    this.erraiModuleConfiguration = new ErraiAptConfiguration(this::findAnnotatedMetaClasses);
  }

  @Override
  public String generate() {
    final ErraiConfiguration erraiConfiguration = new ErraiAptConfiguration(this::findAnnotatedMetaClasses);
    return iocGenerator.generate(null, this::findAnnotatedMetaClasses, erraiConfiguration, findRelevantClasses());
  }

  private Collection<MetaClass> findRelevantClasses() {
    final Collection<MetaClass> metaClasses = new HashSet<>(findAnnotatedMetaClasses(Inject.class));
    metaClasses.addAll(findAnnotatedMetaClasses(com.google.inject.Inject.class));
    metaClasses.addAll(findAnnotatedMetaClasses(IOCProvider.class));
    metaClasses.addAll(findAnnotatedMetaClasses(Dependent.class));
    metaClasses.addAll(findAnnotatedMetaClasses(ApplicationScoped.class));
    metaClasses.addAll(findAnnotatedMetaClasses(Alternative.class));
    metaClasses.addAll(findAnnotatedMetaClasses(Singleton.class));
    metaClasses.addAll(findAnnotatedMetaClasses(EntryPoint.class));
    return metaClasses;
  }

  @Override
  protected Collection<MetaClass> findAnnotatedMetaClasses(Class<? extends Annotation> annotation) {
    final Collection<MetaClass> annotatedMetaClasses = new HashSet<>(super.findAnnotatedMetaClasses(annotation));

    if (annotation.equals(Alternative.class)) {
      annotatedMetaClasses.addAll(erraiModuleConfiguration.modules().getIocEnabledAlternatives());
    }

    return annotatedMetaClasses;
  }

  @Override
  public String getPackageName() {
    return iocGenerator.getPackageName();
  }

  @Override
  public String getClassSimpleName() {
    return iocGenerator.getClassName();
  }
}
