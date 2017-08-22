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

import org.jboss.errai.bus.rebind.RpcProxyLoaderGenerator;
import org.jboss.errai.common.apt.ErraiModuleExportFile;
import org.jboss.errai.common.apt.exportfile.ExportFileName;
import org.jboss.errai.common.apt.exportfile.ExportFilesPackage;
import org.jboss.errai.common.apt.metaclass.APTClass;
import org.jboss.errai.common.apt.metaclass.APTClassUtil;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static javax.tools.Diagnostic.Kind.ERROR;
import static org.jboss.errai.app.SupportedAnnotationTypes.ERRAI_APP;
import static org.jboss.errai.app.SupportedAnnotationTypes.ERRAI_MODULE_EXPORT_FILE;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({ ERRAI_APP, ERRAI_MODULE_EXPORT_FILE })
public class Generator extends AbstractProcessor {

  @Override
  public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {

    if (roundEnv.processingOver()) {
      System.out.println("===== BEGIN");

      APTClassUtil.setTypes(processingEnv.getTypeUtils());
      APTClassUtil.setElements(processingEnv.getElementUtils());

      List<? extends Element> exportFiles = processingEnv.getElementUtils()
              .getPackageElement(ExportFilesPackage.path())
              .getEnclosedElements();

      Map<String, Map<String, Set<TypeMirror>>> exportedClassesByAnnotationClassNameByModuleName = exportFiles.stream()
              .collect(groupingBy(this::exportFileModuleName, groupingBy(this::exportFileAnnotationClassName,
                      flatMapping(this::exportedTypes, Collectors.toSet()))));

      print(exportedClassesByAnnotationClassNameByModuleName);

      generateRpcProxyLoaderImpl(exportedClassesByAnnotationClassNameByModuleName);

      System.out.println("===== END");
    }

    return true;
  }

  private void generateRpcProxyLoaderImpl(Map<String, Map<String, Set<TypeMirror>>> exportedClassesByAnnotationClassNameByModuleName) {
    String generatedSource = new RpcProxyLoaderGenerator().generate(
            (context, annotation) -> exportedClassesByAnnotationClassNameByModuleName.getOrDefault("bus",
                    Collections.emptyMap())
                    .getOrDefault(annotation.getName(), Collections.emptySet())
                    .stream()
                    .map(APTClass::new)
                    .collect(Collectors.toList()), false, Function.identity(), null);

    saveSourceFile(generatedSource);
  }

  private void saveSourceFile(String generatedSource) {
    try {
      final Filer filer = processingEnv.getFiler();
      final FileObject sourceFile = filer.createSourceFile("org.jboss.errai.bus.client.local.RpcProxyLoaderImpl");
      try (Writer writer = sourceFile.openWriter()) {
        writer.write(generatedSource);
      }
    } catch (final IOException e) {
      final Messager messager = processingEnv.getMessager();
      messager.printMessage(ERROR, String.format("Unable to generate RpcProxyLoaderImpl. Error: %s", e.getMessage()));
    }
  }

  private void print(final Map<String, Map<String, Set<TypeMirror>>> exportedClassesByAnnotationClassNameByModuleName) {
    exportedClassesByAnnotationClassNameByModuleName.forEach((moduleName, exportedTypesByAnnotationClassName) -> {
      System.out.println("+ " + moduleName);
      exportedTypesByAnnotationClassName.forEach((annotationClassName, exportedTypes) -> {
        System.out.println(annotationClassName + ": " + exportedTypes.size());
      });
    });
  }

  private String exportFileAnnotationClassName(final Element e) {
    return ExportFileName.getAnnotationClassNameFromExportFileName(e);
  }

  private String exportFileModuleName(final Element exportFile) {
    return exportFile.getAnnotation(ErraiModuleExportFile.class).value();
  }

  private Stream<TypeMirror> exportedTypes(final Element exportFile) {
    return exportFile.getEnclosedElements().stream().filter(x -> x.getKind().isField()).map(Element::asType);
  }

  // Java 9 will implement this method, so when it's released and we upgrade, this can be removed.
  private static <T, U, A, R> Collector<T, ?, R> flatMapping(Function<? super T, ? extends Stream<? extends U>> mapper,
          Collector<? super U, A, R> downstream) {

    BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();
    return Collector.of(downstream.supplier(), (r, t) -> {
              try (Stream<? extends U> result = mapper.apply(t)) {
                if (result != null) {
                  result.sequential().forEach(u -> downstreamAccumulator.accept(r, u));
                }
              }
            }, downstream.combiner(), downstream.finisher(),
            downstream.characteristics().toArray(new Collector.Characteristics[0]));
  }

}
