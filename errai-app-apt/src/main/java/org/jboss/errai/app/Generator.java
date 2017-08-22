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

package org.jboss.errai.app;

import org.jboss.errai.bus.server.annotations.Remote;
import org.jboss.errai.codegen.apt.APTClass;
import org.jboss.errai.codegen.apt.APTClassUtil;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.impl.java.JavaReflectionClass;
import org.jboss.errai.common.metadata.MetaDataScanner;
import org.jboss.errai.common.metadata.ScannerSingleton;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toCollection;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("xis") //FIXME: tiago
public class Generator extends AbstractProcessor {

  private static final String IMPL_FQCN = "org.jboss.errai.bus.client.local.RpcProxyLoaderImpl";

  private final List<APTClass> remoteInterfaces = new ArrayList<>();
  private final List<APTClass> featureInterceptors = new ArrayList<>();
  private final List<APTClass> standaloneInterceptors = new ArrayList<>();

  private final List<MetaClass> preCompiledRemoteInterfaces = new ArrayList<>();

  @Override
  public synchronized void init(final ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    APTClassUtil.setTypes(processingEnv.getTypeUtils());
    APTClassUtil.setElements(processingEnv.getElementUtils());
    final MetaDataScanner scanner = ScannerSingleton.getOrCreateInstance();
    scanner.getTypesAnnotatedWith(Remote.class)
            .stream()
            .map(JavaReflectionClass::newInstance)
            .collect(toCollection(() -> preCompiledRemoteInterfaces));
  }

  @Override
  public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    return false;
  }

}
