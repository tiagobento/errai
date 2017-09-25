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
import org.jboss.errai.common.apt.ErraiAptGenerators;
import org.jboss.errai.common.apt.configuration.module.AptErraiModulesConfiguration;
import org.jboss.errai.databinding.client.api.Bindable;
import org.jboss.errai.databinding.rebind.BindableProxyLoaderGenerator;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;

/**
 * IMPORTANT: Do not move this class. ErraiAppAptGenerator depends on it being in this exact package.
 *
 * @author Tiago Bento <tfernand@redhat.com>
 */
public class BindableProxyLoaderAptGenerator extends ErraiAptGenerators.SingleFile {

  private final BindableProxyLoaderGenerator bindableProxyLoaderGenerator;
  private final AptErraiModulesConfiguration erraiModuleConfiguration;

  // IMPORTANT: Do not remove. ErraiAppAptGenerator depends on this constructor
  public BindableProxyLoaderAptGenerator(final ErraiAptExportedTypes exportedTypes) {
    super(exportedTypes);
    this.erraiModuleConfiguration = new AptErraiModulesConfiguration(this::findAnnotatedMetaClasses);
    this.bindableProxyLoaderGenerator = new BindableProxyLoaderGenerator();
  }

  @Override
  public String generate() {
    return bindableProxyLoaderGenerator.generate(this::findAnnotatedMetaClasses);
  }

  @Override
  public String getPackageName() {
    return bindableProxyLoaderGenerator.getPackageName();
  }

  @Override
  public String getClassSimpleName() {
    return bindableProxyLoaderGenerator.getClassSimpleName();
  }

  @Override
  public Collection<MetaClass> findAnnotatedMetaClasses(final Class<? extends Annotation> annotation) {

    final Collection<MetaClass> annotatedMetaClasses = new HashSet<>(super.findAnnotatedMetaClasses(annotation));

    if (annotation.equals(Bindable.class)) {
      annotatedMetaClasses.addAll(erraiModuleConfiguration.getBindableTypes());
    }

    return annotatedMetaClasses;
  }

}
