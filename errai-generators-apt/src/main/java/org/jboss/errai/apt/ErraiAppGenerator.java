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

package org.jboss.errai.apt;

import org.jboss.errai.apt.bus.AptRpcProxyLoaderGenerator;
import org.jboss.errai.common.apt.ErraiApp;
import org.jboss.errai.common.apt.metaclass.APTClassUtil;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.tools.Diagnostic.Kind.ERROR;
import static org.jboss.errai.apt.SupportedAnnotationTypes.ERRAI_APP;
import static org.jboss.errai.apt.SupportedAnnotationTypes.ERRAI_MODULE_EXPORT_FILE;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({ ERRAI_APP, ERRAI_MODULE_EXPORT_FILE })
public class ErraiAppGenerator extends AbstractProcessor {

  @Override
  public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {

    if (isLastRound(annotations)) {

      System.out.println("===== BEGIN");

      final ExportedTypes exportedTypes = new ExportedTypes(processingEnv);

      APTClassUtil.setTypes(processingEnv.getTypeUtils());
      APTClassUtil.setElements(processingEnv.getElementUtils());

      generateRpcProxyLoaderImpl(exportedTypes);

      System.out.println("===== END");
    }

    return true;
  }

  private boolean isLastRound(Set<? extends TypeElement> annotations) {
    final List<String> currentAnnotations = annotations.stream()
            .map(typeElement -> typeElement.getQualifiedName().toString())
            .collect(Collectors.toList());

    return currentAnnotations.size() == 1 && currentAnnotations.contains(ErraiApp.class.getName());
  }

  private void generateRpcProxyLoaderImpl(final ExportedTypes exportedTypes) {
    final AptRpcProxyLoaderGenerator generator = new AptRpcProxyLoaderGenerator(exportedTypes);
    saveSourceFile(generator.generate(), generator.fileName());
  }

  private void saveSourceFile(final String generatedSource, final String fileName) {
    try {
      final Filer filer = processingEnv.getFiler();
      final FileObject sourceFile = filer.createSourceFile(fileName);
      try (Writer writer = sourceFile.openWriter()) {
        writer.write(generatedSource);
      }
    } catch (final IOException e) {
      final Messager messager = processingEnv.getMessager();
      messager.printMessage(ERROR, String.format("Unable to generate %s. Error: %s", fileName, e.getMessage()));
    }
  }
}