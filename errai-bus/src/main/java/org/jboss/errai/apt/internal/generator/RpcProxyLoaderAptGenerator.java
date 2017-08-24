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

import org.jboss.errai.bus.rebind.RpcProxyLoaderGenerator;
import org.jboss.errai.common.apt.ErraiAptGenerator;
import org.jboss.errai.common.apt.ExportedTypes;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import static org.jboss.errai.common.apt.exportfile.ExportFileModule.BUS;

/**
 * IMPORTANT: Do not move this class. ErraiAppGenerator depends on it being in this exact package.
 *
 * @author Tiago Bento <tfernand@redhat.com>
 */
public final class RpcProxyLoaderAptGenerator implements ErraiAptGenerator {

  private final RpcProxyLoaderGenerator rpcProxyLoaderGenerator;
  private final Boolean iocEnabled;

  public RpcProxyLoaderAptGenerator() {
    this.iocEnabled = true; //FIXME: tiago:
    this.rpcProxyLoaderGenerator = new RpcProxyLoaderGenerator();
  }

  @Override
  public String generate() {
    return rpcProxyLoaderGenerator.generate((context, annotation) -> ExportedTypes.getMetaClasses(BUS, annotation),
            iocEnabled, this::annotationFilter, null);
  }

  @Override
  public String className() {
    return rpcProxyLoaderGenerator.getFullQualifiedClassName();
  }

  private Annotation[] annotationFilter(final Annotation[] annotations) {
    return Arrays.stream(annotations)
            .filter(s -> !s.annotationType().getPackage().getName().contains("server")) //FIXME: tiago:  is that it?
            .toArray(Annotation[]::new);
  }
}
