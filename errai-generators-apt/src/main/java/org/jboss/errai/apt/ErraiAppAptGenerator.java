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

import org.jboss.errai.common.apt.ErraiAptGenerator;
import org.jboss.errai.common.apt.ExportedTypes;
import org.jboss.errai.common.apt.metaclass.APTClassUtil;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static javax.tools.Diagnostic.Kind.ERROR;
import static org.jboss.errai.common.apt.exportfile.ErraiAptPackages.generatorsPackagePackageElement;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({ "org.jboss.errai.common.apt.ErraiApp" })
public class ErraiAppAptGenerator extends AbstractProcessor {

  @Override
  public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {

    try {
      for (final TypeElement erraiAppAnnotation : annotations) {
        generateFiles(roundEnv);
      }
    } catch(final Exception e) {
      System.out.println("Error generating files");
      e.printStackTrace();
    }

    return false;
  }

  private void generateFiles(final RoundEnvironment roundEnv) {
    System.out.println("Generating files using Errai APT Generators..");

    APTClassUtil.init(processingEnv.getElementUtils(), processingEnv.getTypeUtils());
    ExportedTypes.init(roundEnv, processingEnv);

    findGenerators().forEach(this::generateAndSaveSourceFile);
  }

  private List<ErraiAptGenerator> findGenerators() {
    return generatorsPackagePackageElement(processingEnv).map(
            p -> p.getEnclosedElements().stream().map(this::loadClass).map(this::newGenerators).collect(toList()))
            .orElseGet(ArrayList::new);
  }

  private void generateAndSaveSourceFile(final ErraiAptGenerator generator) {
    final String generatedSourceCode = generator.generate();
    final String fileName = generator.className();

    try {
      final JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(fileName);
      try (final Writer writer = sourceFile.openWriter()) {
        writer.write(generatedSourceCode);
      }
    } catch (final IOException e) {
      //FIXME: tiago: see how errors work in apt
      final Messager messager = processingEnv.getMessager();
      messager.printMessage(ERROR, String.format("Unable to generate %s. Error: %s", fileName, e.getMessage()));
    }
  }

  @SuppressWarnings("unchecked")
  private Class<? extends ErraiAptGenerator> loadClass(final Element element) {
    try {
      return (Class<? extends ErraiAptGenerator>) Class.forName(element.asType().toString());
    } catch (final ClassNotFoundException e) {
      throw new RuntimeException("Class " + element.asType().toString() + " is not an ErraiAptGenerator", e);
    }
  }

  private ErraiAptGenerator newGenerators(final Class<? extends ErraiAptGenerator> generatorClass) {
    try {
      final Constructor<? extends ErraiAptGenerator> constructor = generatorClass.getConstructor();
      constructor.setAccessible(true);
      return constructor.newInstance();
    } catch (final Exception e) {
      throw new RuntimeException("Class " + generatorClass.getName() + " couldn't be instantiated.", e);
    }
  }
}
