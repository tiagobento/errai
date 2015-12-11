/*
 * Copyright 2015 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.ioc.rebind.ioc.extension.builtin;

import static org.jboss.errai.codegen.util.Stmt.nestedCall;
import static org.jboss.errai.codegen.util.Stmt.newObject;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.common.rebind.CacheUtil;
import org.jboss.errai.config.rebind.WidgetSubtypeCache;
import org.jboss.errai.ioc.client.api.IOCExtension;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.AbstractBodyGenerator;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.FactoryBodyGenerator;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.IOCProcessingContext;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCExtensionConfigurator;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.rebind.ioc.graph.api.ProvidedInjectable.InjectionSite;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.InjectableHandle;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectableProvider;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;

import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Creates injectables for all {@link Widget} subtypes. Without this extension,
 * injecting some widget types (such as {@link TextBox}) would result in an
 * ambigous resolution because of subtypes (such as {@link PasswordTextBox})
 * that are also type injectable.
 *
 * This extension creates a new instance of the exact {@link Widget} subtype for every injection point.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@IOCExtension
public class WidgetIOCExtension implements IOCExtensionConfigurator {

  @Override
  public void configure(IOCProcessingContext context, InjectionContext injectionContext) {
  }

  @Override
  public void afterInitialization(IOCProcessingContext context, InjectionContext injectionContext) {
    final Collection<MetaClass> widgetSubtypes = CacheUtil.getCache(WidgetSubtypeCache.class).get();

    for (final MetaClass widgetType : widgetSubtypes) {
      if (widgetType.isPublic() && widgetType.isDefaultInstantiable()) {
        injectionContext.registerExactTypeInjectableProvider(
                new InjectableHandle(widgetType, injectionContext.getQualifierFactory().forDefault()),
                new InjectableProvider() {

                  @Override
                  public FactoryBodyGenerator getGenerator(final InjectionSite injectionSite) {
                    return new AbstractBodyGenerator() {
                      @Override
                      protected List<Statement> generateCreateInstanceStatements(
                              ClassStructureBuilder<?> bodyBlockBuilder, Injectable injectable, DependencyGraph graph,
                              InjectionContext injectionContext) {
                        return Collections
                                .<Statement> singletonList(nestedCall(newObject(widgetType, new Object[0])).returnValue());
                      }
                    };
                  }
                });
      }
    }
  }
}