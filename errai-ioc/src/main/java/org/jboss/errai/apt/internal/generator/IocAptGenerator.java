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

import org.jboss.errai.common.apt.ErraiAptExportedTypes;
import org.jboss.errai.common.apt.ErraiAptGenerator;
import org.jboss.errai.common.apt.configuration.ErraiAptModuleConfiguration;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.IOCGenerator;

/**
 * IMPORTANT: Do not move this class. ErraiAppAptGenerator depends on it being in this exact package.
 *
 * @author Tiago Bento <tfernand@redhat.com>
 */
public class IocAptGenerator extends ErraiAptGenerator {

  private final IOCGenerator iocGenerator;

  // IMPORTANT: Do not remove. ErraiAppAptGenerator depends on this constructor
  public IocAptGenerator(final ErraiAptExportedTypes exportedTypes) {
    super(exportedTypes);
    this.iocGenerator = new IOCGenerator();
  }

  @Override
  public String generate() {
    return iocGenerator.generate(null, this::findAnnotatedMetaClasses, getIocModuleConfiguration());
  }

  private ErraiAptModuleConfiguration getIocModuleConfiguration() {
    return new ErraiAptModuleConfiguration(this::findAnnotatedMetaClasses);
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
