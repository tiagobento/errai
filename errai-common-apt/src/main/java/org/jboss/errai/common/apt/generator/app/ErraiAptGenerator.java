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
import org.jboss.errai.common.apt.exportfile.ExportedTypesFromExportFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.Filer;
import javax.lang.model.util.Elements;
import java.util.Set;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
public class ErraiAptGenerator {

  private static final Logger log = LoggerFactory.getLogger(ErraiAptGenerator.class);

  private final Filer filer;
  private final Elements elements;
  private final ResourceFilesFinder resourceFilesFinder;

  public ErraiAptGenerator(final Elements elements, final Filer filer) {
    this.filer = filer;
    this.elements = elements;
    this.resourceFilesFinder = new AptResourceFilesFinder(filer);
  }

  public void generateAndSaveSourceFiles(final Set<MetaClass> erraiApps) {

    final long start = System.currentTimeMillis();
    log.info("Generating files using Errai APT Generators..");

    erraiApps.stream()
            .map(this::newExportedTypesFromExportFiles)
            .map(this::newErraiAppGenerator)
            .forEach(ErraiAppGenerator::generateFiles);

    log.info("Successfully generated files using Errai APT Generators in {}ms", System.currentTimeMillis() - start);
  }

  private ErraiAppGenerator newErraiAppGenerator(final ExportedTypesFromExportFiles exportedTypesFromExportFiles) {
    return new ErraiAppGenerator(exportedTypesFromExportFiles, filer);
  }

  private ExportedTypesFromExportFiles newExportedTypesFromExportFiles(final MetaClass erraiApp) {
    return new ExportedTypesFromExportFiles(erraiApp, resourceFilesFinder, elements);
  }
}
