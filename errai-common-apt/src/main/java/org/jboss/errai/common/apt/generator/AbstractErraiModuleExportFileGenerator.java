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

package org.jboss.errai.common.apt.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.errai.common.apt.AnnotatedElementsFinder;
import org.jboss.errai.common.apt.AptAnnotatedElementsFinder;
import org.jboss.errai.common.apt.exportfile.ExportFile;
import org.jboss.errai.common.apt.exportfile.ExportFileName;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.jboss.errai.common.apt.ErraiAptPackages.exportFilesPackagePath;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
public abstract class AbstractErraiModuleExportFileGenerator extends AbstractProcessor {

  @Override
  public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {

    try {
      generateAndSaveExportFiles(annotations, new AptAnnotatedElementsFinder(roundEnv));
    } catch (final Exception e) {
      System.out.println("Error generating export files");
      e.printStackTrace();
    }

    return false;
  }

  void generateAndSaveExportFiles(final Set<? extends TypeElement> annotations,
          final AnnotatedElementsFinder annotatedElementsFinder) {

    generateExportFilesTypeSpecs(annotations, annotatedElementsFinder).forEach(this::save);
  }

  private Set<TypeSpec> generateExportFilesTypeSpecs(final Set<? extends TypeElement> annotations,
          final AnnotatedElementsFinder annotatedElementsFinder) {

    return generateExportFiles(annotations, annotatedElementsFinder).stream()
            .map(this::newExportFileTypeSpec)
            .collect(toSet());
  }

  Set<ExportFile> generateExportFiles(final Set<? extends TypeElement> annotations,
          final AnnotatedElementsFinder annotatedElementsFinder) {

    return annotations.stream()
            .map(a -> new ExportFile(getModuleName(), a, annotatedClassesAndInterfaces(annotatedElementsFinder, a)))
            .filter(ExportFile::hasExportedTypes)
            .collect(toSet());
  }

  Set<? extends Element> annotatedClassesAndInterfaces(final AnnotatedElementsFinder annotatedElementsFinder,
          final TypeElement annotationTypeElement) {

    return annotatedElementsFinder.getElementsAnnotatedWith(annotationTypeElement)
            .stream()
            .filter(e -> e.getKind().isClass() || e.getKind().isInterface())
            .collect(toSet());
  }

  private TypeSpec newExportFileTypeSpec(final ExportFile exportFile) {
    return TypeSpec.classBuilder(ExportFileName.encodeAnnotationNameAsExportFileName(exportFile))
            .addModifiers(PUBLIC, FINAL)
            .addFields(buildFields(exportFile.exportedTypes))
            .build();
  }

  private List<FieldSpec> buildFields(final Set<? extends Element> exportedElements) {
    return exportedElements.stream().map(this::newFieldSpec).collect(toList());
  }

  private FieldSpec newFieldSpec(final Element exportedElement) {

    final TypeElement typeElement = (TypeElement) exportedElement;
    final QualifiedNameable enclosingElement = (QualifiedNameable) exportedElement.getEnclosingElement();
    final String enclosingElementName = enclosingElement.getQualifiedName().toString();
    final String classSimpleName = typeElement.getSimpleName().toString();

    return FieldSpec.builder(ClassName.get(enclosingElementName, classSimpleName),
            RandomStringUtils.randomAlphabetic(6)).addModifiers(PUBLIC).build();
  }

  private void save(final TypeSpec exportFileTypeSpec) {
    try {
      JavaFile.builder(exportFilesPackagePath(), exportFileTypeSpec).build().writeTo(processingEnv.getFiler());
      System.out.println("Successfully generated export file [" + exportFileTypeSpec.name + "]");
    } catch (final IOException e) {
      throw new RuntimeException("Error writing generated export file", e);
    }
  }

  protected abstract String getModuleName();
}
