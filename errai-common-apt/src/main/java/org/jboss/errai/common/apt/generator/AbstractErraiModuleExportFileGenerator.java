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

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.errai.common.apt.exportfile.ExportFileName;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.jboss.errai.common.apt.exportfile.ErraiAptPackages.exportFilesPackagePath;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
public abstract class AbstractErraiModuleExportFileGenerator extends AbstractProcessor {

  @Override
  public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {

    //    if (!processingEnv.getOptions().containsKey("errai.useAptGenerators")) {
    //      return false;
    //    }

    generateExportFiles(annotations, roundEnv);
    return false;
  }

  private void generateExportFiles(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    annotations.stream()
            .collect(toMap(identity(), annotation -> annotatedClassesAndInterfaces(roundEnv, annotation)))
            .entrySet()
            .stream()
            .map(e -> generateExportFile(e.getKey(), e.getValue()))
            .forEach(this::saveExportFile);
  }

  private Set<? extends Element> annotatedClassesAndInterfaces(final RoundEnvironment roundEnv,
          final TypeElement typeElement) {

    return roundEnv.getElementsAnnotatedWith(typeElement)
            .stream()
            .filter(e -> e.getKind().isClass() || e.getKind().isInterface())
            .collect(Collectors.toSet());
  }

  private TypeSpec generateExportFile(final TypeElement annotation, Set<? extends Element> elements) {
    return TypeSpec.classBuilder(ExportFileName.buildExportFileNameForAnnotation(annotation))
            .addModifiers(PUBLIC, FINAL)
            .addFields(buildFields(elements))
            .build();
  }

  private FieldSpec buildField(final Element element) {
    return FieldSpec.builder(TypeName.get(element.asType()), RandomStringUtils.randomAlphabetic(6))
            .addModifiers(PUBLIC)
            .build();
  }

  private List<FieldSpec> buildFields(final Set<? extends Element> elements) {
    return elements.stream().map(this::buildField).collect(toList());
  }

  private void saveExportFile(final TypeSpec exportFileTypeSpec) {
    try {
      JavaFile.builder(exportFilesPackagePath(), exportFileTypeSpec).build().writeTo(processingEnv.getFiler());
      System.out.println("Successfully generated export file [" + exportFileTypeSpec.name + "]");
    } catch (IOException e) {
      throw new RuntimeException("Error writing generated export file", e);
    }
  }
}
