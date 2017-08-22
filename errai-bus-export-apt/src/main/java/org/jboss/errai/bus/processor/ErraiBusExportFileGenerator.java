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

package org.jboss.errai.bus.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.apache.commons.lang3.RandomStringUtils;
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
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.jboss.errai.bus.processor.SupportedAnnotationTypes.FEATURE_INTERCEPTOR;
import static org.jboss.errai.bus.processor.SupportedAnnotationTypes.INTERCEPTED_CALL;
import static org.jboss.errai.bus.processor.SupportedAnnotationTypes.REMOTE;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({ REMOTE, INTERCEPTED_CALL, FEATURE_INTERCEPTOR })
public class ErraiBusExportFileGenerator extends AbstractProcessor {

  @Override
  public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {

    final Map<TypeElement, Set<? extends Element>> elementsByItsAnnotations = annotations.stream()
            .collect(toMap(e -> e, roundEnv::getElementsAnnotatedWith));

    elementsByItsAnnotations.entrySet()
            .stream()
            .map((e) -> generateExportFile(e.getKey(), e.getValue()))
            .forEach(this::saveExportFile);

    return false;
  }

  private TypeSpec generateExportFile(final TypeElement annotation, Set<? extends Element> elements) {

    final AnnotationSpec erraiModuleExportFileAnnotationSpec = AnnotationSpec.builder(ErraiModuleExportFile.class)
            .addMember("value", "$S", getModuleName())
            .build();

    return TypeSpec.classBuilder(ExportFileName.buildExportFileNameForAnnotation(annotation))
            .addAnnotation(erraiModuleExportFileAnnotationSpec)
            .addModifiers(PUBLIC, FINAL)
            .addFields(buildFields(elements))
            .build();
  }

  private List<FieldSpec> buildFields(final Set<? extends Element> elements) {
    return elements.stream().map(this::buildField).collect(Collectors.toList());
  }

  private FieldSpec buildField(final Element element) {
    return FieldSpec.builder(TypeName.get(element.asType()), RandomStringUtils.randomAlphabetic(6))
            .addModifiers(PUBLIC)
            .build();
  }

  private void saveExportFile(final TypeSpec exportFileTypeSpec) {
    try {
      JavaFile.builder(ExportFilePackage.path(), exportFileTypeSpec).build().writeTo(processingEnv.getFiler());
    } catch (IOException e) {
      throw new RuntimeException("Error writing generated export file", e);
    }
  }

  private String getModuleName() {
    return "bus";
  }

}