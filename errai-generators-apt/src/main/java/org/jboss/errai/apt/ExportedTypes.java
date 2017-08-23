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

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.common.apt.ErraiModuleExportFile;
import org.jboss.errai.common.apt.exportfile.ExportFileName;
import org.jboss.errai.common.apt.exportfile.ExportFilesPackage;
import org.jboss.errai.common.apt.metaclass.APTClass;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.Collection;
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

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
public class ExportedTypes {

  final Map<String, Map<String, Set<TypeMirror>>> exportedClassesByAnnotationClassNameByModuleName;

  public ExportedTypes(final ProcessingEnvironment processingEnvironment) {
    List<? extends Element> exportFiles = processingEnvironment.getElementUtils()
            .getPackageElement(ExportFilesPackage.path())
            .getEnclosedElements();

    exportedClassesByAnnotationClassNameByModuleName = exportFiles.stream()
            .collect(groupingBy(this::exportFileModuleName, groupingBy(this::exportFileAnnotationClassName,
                    flatMapping(this::exportedTypes, Collectors.toSet()))));

    print();
  }

  private void print() {
    exportedClassesByAnnotationClassNameByModuleName.forEach((moduleName, exportedTypesByAnnotationClassName) -> {
      System.out.println("+ " + moduleName);
      exportedTypesByAnnotationClassName.forEach((annotationClassName, e) -> {
        System.out.println(annotationClassName + ": " + e.size());
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

  public Collection<MetaClass> getMetaClasses(String module, Class<? extends Annotation> annotation) {
    return exportedClassesByAnnotationClassNameByModuleName.getOrDefault(module, Collections.emptyMap())
            .getOrDefault(annotation.getName(), Collections.emptySet())
            .stream()
            .map(APTClass::new)
            .collect(Collectors.toList());
  }
}