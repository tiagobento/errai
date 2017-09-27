/*
 * Copyright (C) 2011 Red Hat, Inc. and/or its affiliates.
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

package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.codegen.meta.MetaClassFinder;
import org.jboss.errai.common.apt.configuration.ErraiConfiguration;
import org.jboss.errai.common.metadata.RebindUtils;
import org.jboss.errai.common.metadata.ScannerSingleton;
import org.jboss.errai.config.rebind.AbstractAsyncGenerator;
import org.jboss.errai.config.rebind.EnvUtil;
import org.jboss.errai.config.rebind.GenerateAsync;
import org.jboss.errai.config.util.ClassScanner;
import org.jboss.errai.ioc.client.Bootstrapper;
import org.jboss.errai.ioc.client.container.IOCEnvironment;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.configuration.ErraiAppPropertiesConfiguration;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;

import static java.util.stream.Collectors.toList;

/**
 * The main generator class for the Errai IOC framework.
 * <p/>
 * <pre>
 *
 * </pre>
 *
 * @author Mike Brock
 * @author Christian Sadilek <csadilek@redhat.com>
 */
@GenerateAsync(Bootstrapper.class)
public class IOCGenerator extends AbstractAsyncGenerator {

  private final String packageName = "org.jboss.errai.ioc.client";
  private final String classSimpleName = "BootstrapperImpl";

  public IOCGenerator() {
  }

  @Override
  public String generate(final TreeLogger logger, final GeneratorContext context, final String typeName)
          throws UnableToCompleteException {

    logger.log(TreeLogger.INFO, "generating ioc bootstrapping code...");
    return startAsyncGeneratorsAndWaitFor(Bootstrapper.class, context, logger, packageName, classSimpleName);
  }

  @Override
  protected String generate(final TreeLogger logger, final GeneratorContext context) {
    final Set<String> translatablePackages = RebindUtils.findTranslatablePackages(context);
    final MetaClassFinder metaClassFinder = ann -> findMetaClasses(context, translatablePackages, ann);
    final ErraiConfiguration erraiConfiguration = new ErraiAppPropertiesConfiguration();

    return generate(context, metaClassFinder, erraiConfiguration,
            (annotations) -> IocRelevantClassesUtil.findRelevantClasses());
  }

  public String generate(final GeneratorContext context,
          final MetaClassFinder metaClassFinder,
          final ErraiConfiguration erraiConfiguration,
          final IocRelevantClasses relevantClasses) {

    return new IOCBootstrapGenerator(metaClassFinder, context, erraiConfiguration, relevantClasses).generate(
            packageName, classSimpleName);
  }

  private Collection<MetaClass> findMetaClasses(final GeneratorContext context,
          final Set<String> translatablePackages,
          final Class<? extends Annotation> annotation) {

    //FIXME: tiago: this is a terrible workaround
    final Collection<MetaClass> typesAnnotatedWith = ClassScanner.getTypesAnnotatedWith(annotation,
            translatablePackages, context);

    if (!typesAnnotatedWith.isEmpty()) {
      return typesAnnotatedWith;
    }

    return ScannerSingleton.getOrCreateInstance()
            .getTypesAnnotatedWith(annotation)
            .stream()
            .map(MetaClassFactory::get)
            .collect(toList());
  }

  @Override
  protected boolean isCacheValid() {
    // This ensures the logged total build time of factories is reset even if
    // the BootstrapperImpl is not regenerated.
    FactoryGenerator.resetTotalTime();
    Collection<MetaClass> newOrUpdated = MetaClassFactory.getAllNewOrUpdatedClasses();
    // filter out generated IOC environment config
    if (newOrUpdated.size() == 1) {
      MetaClass clazz = newOrUpdated.iterator().next();
      if (clazz.isAssignableTo(IOCEnvironment.class)) {
        newOrUpdated.clear();
      }
    }

    boolean hasAnyChanges = !newOrUpdated.isEmpty() || !MetaClassFactory.getAllDeletedClasses().isEmpty();
    return hasGenerationCache() && (EnvUtil.isProdMode() || !hasAnyChanges);
  }

  public String getPackageName() {
    return packageName;
  }

  public String getClassSimpleName() {
    return classSimpleName;
  }

  @Override
  public boolean alreadyGeneratedSourcesViaAptGenerators(GeneratorContext context) {
    try {
      return context.getTypeOracle().getType(getPackageName() + "." + getClassSimpleName()) != null;
    } catch (final NotFoundException e) {
      return false;
    }
  }
}
