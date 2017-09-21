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

package org.jboss.errai.common.apt.configuration;

import org.jboss.errai.codegen.meta.MetaClassFinder;
import org.jboss.errai.common.apt.configuration.app.ErraiAppConfiguration;
import org.jboss.errai.common.apt.configuration.app.AptErraiAppConfiguration;
import org.jboss.errai.common.apt.configuration.module.ErraiModulesConfiguration;
import org.jboss.errai.common.apt.configuration.module.AptErraiModulesConfiguration;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
public class ErraiAptConfiguration implements ErraiConfiguration {

  private final ErraiModulesConfiguration modules;
  private final ErraiAppConfiguration app;

  public ErraiAptConfiguration(final MetaClassFinder metaClassFinder) {
    this.modules = new AptErraiModulesConfiguration(metaClassFinder);
    this.app = new AptErraiAppConfiguration(metaClassFinder);
  }

  @Override
  public ErraiModulesConfiguration modules() {
    return modules;
  }

  @Override
  public ErraiAppConfiguration app() {
    return app;
  }
}
