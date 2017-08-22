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

import org.jboss.errai.common.apt.ErraiModuleExportFile;
import org.jboss.errai.common.apt.exportfile.ExportFileName;
import org.jboss.errai.common.apt.exportfile.ExportFilePackage;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.jboss.errai.app.SupportedAnnotationTypes.ERRAI_APP;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(ERRAI_APP)
public class Generator extends AbstractProcessor {

  private static final String IMPL_FQCN = "org.jboss.errai.bus.client.local.RpcProxyLoaderImpl";

  @Override
  public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {

    for (TypeElement _erraiAppAnnotation : annotations) {
      System.out.println("===== BEGIN");

      List<? extends Element> exportFiles = processingEnv.getElementUtils()
              .getPackageElement(ExportFilePackage.path())
              .getEnclosedElements();

      Map<String, Map<String, List<TypeMirror>>> exportedClassesByAnnotationClassNameByModuleName = exportFiles.stream()
              .collect(groupingBy(this::exportFileModuleName,
                      groupingBy(this::exportFileAnnotationClassName, flatMapping(this::exportedTypes, toList()))));

      print(exportedClassesByAnnotationClassNameByModuleName);

      System.out.println("===== END");
    }

    return true;
  }

  private void print(Map<String, Map<String, List<TypeMirror>>> exportedClassesByAnnotationClassNameByModuleName) {
    exportedClassesByAnnotationClassNameByModuleName.forEach((moduleName, exportedTypesByAnnotationClassName) -> {
      System.out.println("+ " + moduleName);
      exportedTypesByAnnotationClassName.forEach((annotationClassName, exportedTypes) -> {
        System.out.println(annotationClassName + ": " + exportedTypes.size());
      });
    });
  }

  private String exportFileAnnotationClassName(Element e) {
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
