/*
 * Copyright (C) 2015 Red Hat, Inc. and/or its affiliates.
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

package org.jboss.errai.ioc.apt;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.common.apt.exportfile.ExportedTypesFromExportFiles;
import org.jboss.errai.common.apt.ErraiAptGenerators;
import org.jboss.errai.common.apt.generator.ErraiAptGeneratedSourceFile;
import org.jboss.errai.common.configuration.ErraiGenerator;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.QualifierEqualityFactoryGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Qualifier;
import java.util.Collection;

import static java.util.Collections.singleton;
import static org.jboss.errai.common.apt.generator.ErraiAptGeneratedSourceFile.Type.CLIENT;
import static org.jboss.errai.common.apt.generator.ErraiAptGeneratedSourceFile.Type.SHARED;
import static org.jboss.errai.common.configuration.Target.GWT;
import static org.jboss.errai.common.configuration.Target.JAVA;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
@ErraiGenerator(targets = { JAVA, GWT })
public class QualifierEqualityFactoryAptGenerator extends ErraiAptGenerators.SingleFile {

  private static final Logger log = LoggerFactory.getLogger(QualifierEqualityFactoryAptGenerator.class);

  private final QualifierEqualityFactoryGenerator qualifierEqualityFactoryGenerator;

  // IMPORTANT: Do not remove. ErraiAppAptGenerator depends on this constructor
  public QualifierEqualityFactoryAptGenerator(final ExportedTypesFromExportFiles exportedTypes) {
    super(exportedTypes);
    this.qualifierEqualityFactoryGenerator = new QualifierEqualityFactoryGenerator();
  }

  @Override
  public String generate() {
    log.info("Generating {}...", getClassSimpleName());
    final String generatedSource = qualifierEqualityFactoryGenerator.generate(qualifiers(),
            getResolvedFullyQualifiedClassName());
    log.info("Generated {}", getClassSimpleName());
    return generatedSource;
  }

  private Collection<MetaClass> qualifiers() {
    return metaClassFinder().extend(Qualifier.class, () -> singleton(MetaClassFactory.get(Named.class)))
            .findAnnotatedWith(Qualifier.class);
  }

  @Override
  public String getPackageName() {
    return QualifierEqualityFactoryGenerator.PACKAGE_NAME;
  }

  @Override
  public String getClassSimpleName() {
    return QualifierEqualityFactoryGenerator.CLASS_NAME;
  }

  @Override
  public ErraiAptGeneratedSourceFile.Type getType() {
    return erraiConfiguration().app().target().equals(GWT) ? CLIENT : SHARED;
  }
}
