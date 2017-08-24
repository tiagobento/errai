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

package org.jboss.errai.apt.bus;

import org.jboss.errai.apt.ExportedTypes;
import org.jboss.errai.bus.rebind.RpcProxyLoaderGenerator;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import static org.jboss.errai.common.apt.exportfile.ExportFileModule.BUS;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
public class AptRpcProxyLoaderGenerator {

  private final ExportedTypes exportedTypes;
  private final RpcProxyLoaderGenerator rpcProxyLoaderGenerator;
  private final Boolean iocEnabled;

  public AptRpcProxyLoaderGenerator(final ExportedTypes exportedTypes) {
    this.iocEnabled = true; //FIXME: tiago:
    this.exportedTypes = exportedTypes;
    this.rpcProxyLoaderGenerator = new RpcProxyLoaderGenerator();
  }

  public String generate() {
    return rpcProxyLoaderGenerator.generate((context, annotation) -> exportedTypes.getMetaClasses(BUS, annotation),
            iocEnabled, this::annotationFilter, null);
  }

  private Annotation[] annotationFilter(final Annotation[] annotations) {
    return Arrays.stream(annotations)
            .filter(s -> !s.annotationType().getPackage().getName().contains("server"))
            .toArray(Annotation[]::new);
  }

  public String className() {
    return rpcProxyLoaderGenerator.getFullQualifiedClassName();
  }
}
