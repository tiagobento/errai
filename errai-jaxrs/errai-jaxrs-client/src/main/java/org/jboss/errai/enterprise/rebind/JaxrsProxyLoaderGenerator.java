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

package org.jboss.errai.enterprise.rebind;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.jboss.errai.codegen.InnerClass;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.builder.MethodBlockBuilder;
import org.jboss.errai.codegen.builder.impl.ClassBuilder;
import org.jboss.errai.codegen.builder.impl.ObjectBuilder;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.util.ProxyUtil.InterceptorProvider;
import org.jboss.errai.codegen.util.ProxyUtil;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.common.client.api.interceptor.FeatureInterceptor;
import org.jboss.errai.common.client.api.interceptor.InterceptsRemoteCall;
import org.jboss.errai.common.client.framework.ProxyProvider;
import org.jboss.errai.common.client.framework.RemoteServiceProxyFactory;
import org.jboss.errai.common.metadata.RebindUtils;
import org.jboss.errai.config.rebind.AbstractAsyncGenerator;
import org.jboss.errai.config.rebind.GenerateAsync;
import org.jboss.errai.config.util.ClassScanner;
import org.jboss.errai.enterprise.client.jaxrs.JaxrsProxyLoader;
import org.jboss.errai.enterprise.shared.api.annotations.MapsFrom;

import com.google.common.collect.Multimap;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * Generates the JAX-RS proxy loader.
 *
 * @author Christian Sadilek <csadilek@redhat.com>
 */
@GenerateAsync(JaxrsProxyLoader.class)
public class JaxrsProxyLoaderGenerator extends AbstractAsyncGenerator {
  private static final String IOC_MODULE_NAME = "org.jboss.errai.ioc.Container";
  private final String packageName = JaxrsProxyLoader.class.getPackage().getName();
  private final String className = JaxrsProxyLoader.class.getSimpleName() + "Impl";

  @Override
  public String generate(final TreeLogger logger, final GeneratorContext context, final String typeName)
      throws UnableToCompleteException {

    return startAsyncGeneratorsAndWaitFor(JaxrsProxyLoader.class, context, logger, packageName, className);
  }

  @Override
  protected String generate(final TreeLogger logger, final GeneratorContext context) {
    ClassStructureBuilder<?> classBuilder = ClassBuilder.implement(JaxrsProxyLoader.class);
    final MethodBlockBuilder<?> loadProxies = classBuilder.publicMethod(void.class, "loadProxies");

    final InterceptorProvider interceptorProvider = getInterceptorProvider(context);
    final Multimap<MetaClass, MetaClass> exceptionMappers = getClientExceptionMappers(context);

    final Collection<MetaClass> remotes = ClassScanner.getTypesAnnotatedWith(Path.class,
        RebindUtils.findTranslatablePackages(context), context);
    addCacheRelevantClasses(remotes);

    for (final MetaClass remote : remotes) {
      if (remote.isInterface()) {
        final Set<String> translatablePackages = RebindUtils.findTranslatablePackages(context);
        final Function<Annotation[], Annotation[]> annoFilter = ProxyUtil.packageFilter(translatablePackages);
        final boolean iocEnabled = RebindUtils.isModuleInherited(context, IOC_MODULE_NAME);
        // create the remote proxy for this interface
        final ClassStructureBuilder<?> remoteProxy =
            new JaxrsProxyGenerator(remote, interceptorProvider, exceptionMappers, annoFilter, iocEnabled).generate();
        loadProxies.append(new InnerClass(remoteProxy.getClassDefinition()));

        // create the proxy provider
        final Statement proxyProvider = ObjectBuilder.newInstanceOf(ProxyProvider.class)
            .extend()
            .publicOverridesMethod("getProxy")
            .append(Stmt.nestedCall(Stmt.newObject(remoteProxy.getClassDefinition())).returnValue())
            .finish()
            .finish();

        // create the call that registers the proxy provided for the generated proxy
        loadProxies.append(Stmt.invokeStatic(RemoteServiceProxyFactory.class, "addRemoteProxy", remote, proxyProvider));
      }
    }
    classBuilder = (ClassStructureBuilder<?>) loadProxies.finish();
    return classBuilder.toJavaString();
  }

  /**
   * Returns an {@link InterceptorProvider} that can be used to retrieve interceptors for the remote
   * interface/method.
   */
  private InterceptorProvider getInterceptorProvider(final GeneratorContext context) {
    final Collection<MetaClass> featureInterceptors = ClassScanner.getTypesAnnotatedWith(FeatureInterceptor.class,
        RebindUtils.findTranslatablePackages(context), context);
    addCacheRelevantClasses(featureInterceptors);

    final Collection<MetaClass> standaloneInterceptors = ClassScanner.getTypesAnnotatedWith(InterceptsRemoteCall.class,
        RebindUtils.findTranslatablePackages(context), context);
    addCacheRelevantClasses(standaloneInterceptors);

    return new InterceptorProvider(featureInterceptors, standaloneInterceptors);
  }

  /**
   * Returns a {@link Multimap} of client side exception mappers to remote interfaces they apply to
   * (see {@link MapsFrom}). If a generic (default) exception mapper is found the value collection
   * of the map will contain null.
   */
  private Multimap<MetaClass, MetaClass> getClientExceptionMappers(final GeneratorContext context) {
    final Collection<MetaClass> providers = ClassScanner.getTypesAnnotatedWith(Provider.class,
        RebindUtils.findTranslatablePackages(context), context);
    addCacheRelevantClasses(providers);

    return Utils.getClientExceptionMappers(providers);
  }

  @Override
  protected boolean isRelevantClass(final MetaClass clazz) {
    for (final Annotation anno : clazz.unsafeGetAnnotations()) {
      if (anno.annotationType().equals(Path.class) || anno.annotationType().equals(FeatureInterceptor.class)
              || anno.annotationType().equals(InterceptsRemoteCall.class) || anno.annotationType().equals(Provider.class)) {
        return true;
      }
    }

    return false;
  }
}
