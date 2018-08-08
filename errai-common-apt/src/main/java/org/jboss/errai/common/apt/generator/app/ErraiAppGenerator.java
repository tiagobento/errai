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

package org.jboss.errai.common.apt.generator.app;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.common.apt.ErraiAptGenerators;
import org.jboss.errai.common.apt.configuration.AptErraiAppConfiguration;
import org.jboss.errai.common.apt.exportfile.ExportedTypesFromExportFiles;
import org.jboss.errai.common.apt.generator.ErraiAptGeneratedSourceFile;
import org.jboss.errai.config.apt.api.ErraiGenerator;
import org.jboss.errai.config.apt.api.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static javax.tools.StandardLocation.SOURCE_OUTPUT;
import static org.jboss.errai.config.apt.api.Target.GWT;
import static org.jboss.errai.config.apt.api.Target.JAVA;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
class ErraiAppGenerator {

  private static final Logger log = LoggerFactory.getLogger(ErraiAptGenerator.class);

  private static final String GWT_XML = ".gwt.xml";

  private final ExportedTypesFromExportFiles exportedTypesFromExportFiles;
  private final Filer filer;

  ErraiAppGenerator(final ExportedTypesFromExportFiles exportedTypesFromExportFiles, final Filer filer) {
    this.exportedTypesFromExportFiles = exportedTypesFromExportFiles;
    this.filer = filer;
  }

  void generateFiles() {

    log.info("Processing {}", erraiAppConfiguration().erraiAppMetaClass());

    if (erraiAppConfiguration().target().equals(GWT)) {
      generateAptCompatibleGwtModuleFile();
    }

    createGenerators().filter(this::generatorsOfErraiAppTarget)
            .flatMap(this::generateFilesOfGenerator)
            .forEach(this::saveFile);
  }

  private Stream<ErraiAptGenerators.Any> createGenerators() {
    return exportedTypesFromExportFiles.findAnnotatedMetaClasses(ErraiGenerator.class)
            .stream()
            .map(this::loadGeneratorClass)
            .map(this::newGenerator)
            .sorted(comparing(ErraiAptGenerators.Any::priority).thenComparing(g -> g.getClass().getSimpleName()));
  }

  @SuppressWarnings("unchecked")
  private Class<? extends ErraiAptGenerators.Any> loadGeneratorClass(final MetaClass metaClass) {
    try {
      // Because we're sure generators will always be pre-compiled, it's safe to get their classes using Class.forName
      return (Class<? extends ErraiAptGenerators.Any>) Class.forName(metaClass.getFullyQualifiedName());
    } catch (final ClassNotFoundException e) {
      throw new RuntimeException(metaClass.getFullyQualifiedName() + " is not an ErraiAptGenerator", e);
    }
  }

  private ErraiAptGenerators.Any newGenerator(final Class<? extends ErraiAptGenerators.Any> generatorClass) {
    try {
      final Constructor<? extends ErraiAptGenerators.Any> constructor = generatorClass.getConstructor(
              ExportedTypesFromExportFiles.class);
      constructor.setAccessible(true);
      return constructor.newInstance(exportedTypesFromExportFiles);
    } catch (final Exception e) {
      throw new RuntimeException("Class " + generatorClass.getName() + " couldn't be instantiated.", e);
    }
  }

  private void generateAptCompatibleGwtModuleFile() {

    final String gwtModuleName = erraiAppConfiguration().gwtModuleName();
    final String path = gwtModuleName.replace(".", "/") + GWT_XML;

    exportedTypesFromExportFiles.resourceFilesFinder()
            .getResource(path)
            .map(resource -> ((AptCodeGenResource) resource))
            .map(AptCodeGenResource::getFile)
            .map(this::newAptCompatibleGwtModuleFile)
            .ifPresent(file -> saveFile(file, gwtModuleName));
  }

  private boolean generatorsOfErraiAppTarget(final ErraiAptGenerators.Any generator) {
    final Target[] targets = generator.getClass().getAnnotation(ErraiGenerator.class).targets();
    return stream(targets).anyMatch(generator.erraiConfiguration().app().target()::equals);
  }

  private AptCompatibleGwtModuleFile newAptCompatibleGwtModuleFile(final File file) {
    return new AptCompatibleGwtModuleFile(file, exportedTypesFromExportFiles);
  }

  private Stream<ErraiAptGeneratedSourceFile> generateFilesOfGenerator(final ErraiAptGenerators.Any generator) {
    try {
      return generator.files().stream();
    } catch (final Exception e) {
      // Continues to next generator even when errors occur
      e.printStackTrace();
      return Stream.of();
    }
  }

  private void saveFile(final AptCompatibleGwtModuleFile file, final String gwtModuleName) {

    final int lastDot = gwtModuleName.lastIndexOf(".");
    final String fileName = gwtModuleName.substring(lastDot + 1) + GWT_XML;
    final String packageName = gwtModuleName.substring(0, lastDot);

    try {
      // By writing to CLASS_OUTPUT we overwrite the original .gwt.xml file
      final FileObject sourceFile = filer.createResource(CLASS_OUTPUT, packageName, fileName);
      final String newGwtModuleFileContent = file.generate();

      try (final Writer writer = sourceFile.openWriter()) {
        writer.write(newGwtModuleFileContent);
      }
    } catch (final IOException e) {
      throw new RuntimeException("Unable to write file " + gwtModuleName);
    }
  }

  private void saveFile(final ErraiAptGeneratedSourceFile file) {
    try {
      try (final Writer writer = getFileObject(file).openWriter()) {
        writer.write(file.getSourceCode());
      }
    } catch (final IOException e) {
      throw new RuntimeException("Could not write generated file", e);
    }
  }

  private FileObject getFileObject(final ErraiAptGeneratedSourceFile file) throws IOException {

    final String pkg = file.getPackageName();
    final String classSimpleName = file.getClassSimpleName();

    if (erraiAppConfiguration().target().equals(GWT)) {
      // By saving .java source files as resources we skip javac compilation. This behavior is
      // desirable since generated client code will be compiled by the GWT/J2CL compiler.
      return filer.createResource(SOURCE_OUTPUT, pkg, classSimpleName + ".java");
    }

    if (erraiAppConfiguration().target().equals(JAVA)) {
      return filer.createSourceFile(pkg + "." + classSimpleName);
    }

    throw new RuntimeException("Unsupported target " + erraiAppConfiguration().target());
  }

  private AptErraiAppConfiguration erraiAppConfiguration() {
    return exportedTypesFromExportFiles.erraiAppConfiguration();
  }
}
