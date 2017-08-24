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

package org.jboss.errai.common.apt;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.common.apt.exportfile.ExportFileName;
import org.jboss.errai.common.apt.generator.AbstractErraiModuleExportFileGenerator;
import org.jboss.errai.common.apt.metaclass.APTClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jboss.errai.common.apt.exportfile.ErraiAptPackages.exportFilesPackagePath;
import static org.jboss.errai.common.apt.exportfile.ErraiAptPackages.exportedAnnotationsPackagePath;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
public class ExportedTypes {

  private static final Logger log = LoggerFactory.getLogger(ExportedTypes.class);

  private static Map<String, Set<TypeMirror>> exportedClassesByAnnotationClassNameByModuleName;
  private static RoundEnvironment roundEnv;
  private static ProcessingEnvironment processingEnvironment;

  private ExportedTypes() {
  }

  public static void init(final RoundEnvironment roundEnv, final ProcessingEnvironment processingEnvironment) {
    ExportedTypes.roundEnv = roundEnv;
    ExportedTypes.processingEnvironment = processingEnvironment;

    // Loads all exported types from ErraiModuleExportFiles
    exportedClassesByAnnotationClassNameByModuleName = processingEnvironment.getElementUtils()
            .getPackageElement(exportFilesPackagePath())
            .getEnclosedElements()
            .stream()
            .collect(groupingBy(ExportedTypes::annotationName, flatMapping(ExportedTypes::exportedTypes, toSet())));

    // Because annotation processors execution order is random we have to look for local exportable types one more time
    processingEnvironment.getElementUtils()
            .getPackageElement(exportedAnnotationsPackagePath())
            .getEnclosedElements()
            .stream()
            .flatMap(ExportedTypes::exportedTypes)
            .collect(groupingBy(TypeMirror::toString, flatMapping(ExportedTypes::exportableLocalTypes, toSet())))
            .entrySet()
            .stream()
            .filter(s -> !s.getValue().isEmpty())
            .forEach(e -> exportedClassesByAnnotationClassNameByModuleName.get(e.getKey()).addAll(e.getValue()));

    print();
  }

  private static Stream<TypeMirror> exportableLocalTypes(final TypeMirror element) {
    final TypeElement annotation = (TypeElement) processingEnvironment.getTypeUtils().asElement(element);
    return roundEnv.getElementsAnnotatedWith(annotation).stream().map(Element::asType);
  }

  private static void print() {
    exportedClassesByAnnotationClassNameByModuleName.forEach((annotationClassName, e) -> System.out.println(
            annotationClassName + ": " + e.size())); //FIXME: change for log.debug
  }

  private static String annotationName(final Element e) {
    return ExportFileName.getAnnotationClassNameFromExportFileName(e);
  }

  private static Stream<TypeMirror> exportedTypes(final Element exportFile) {
    return exportFile.getEnclosedElements().stream().filter(x -> x.getKind().isField()).map(Element::asType);
  }

  public static Collection<MetaClass> getMetaClasses(final Class<? extends Annotation> annotation) {
    return exportedClassesByAnnotationClassNameByModuleName.getOrDefault(annotation.getName(), emptySet())
            .stream()
            .map(APTClass::new)
            .collect(toList());
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
