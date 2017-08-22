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

package org.jboss.errai.bus.rebind;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import org.jboss.errai.bus.client.api.messaging.MessageBus;
import org.jboss.errai.bus.client.framework.RpcProxyLoader;
import org.jboss.errai.bus.server.annotations.Remote;
import org.jboss.errai.codegen.InnerClass;
import org.jboss.errai.codegen.Parameter;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.builder.MethodBlockBuilder;
import org.jboss.errai.codegen.builder.impl.ClassBuilder;
import org.jboss.errai.codegen.builder.impl.ObjectBuilder;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.util.ProxyUtil;
import org.jboss.errai.codegen.util.ProxyUtil.InterceptorProvider;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.common.client.api.interceptor.FeatureInterceptor;
import org.jboss.errai.common.client.api.interceptor.InterceptsRemoteCall;
import org.jboss.errai.common.client.framework.ProxyProvider;
import org.jboss.errai.common.client.framework.RemoteServiceProxyFactory;
import org.jboss.errai.common.metadata.RebindUtils;
import org.jboss.errai.config.rebind.AbstractAsyncGenerator;
import org.jboss.errai.config.rebind.GenerateAsync;
import org.jboss.errai.config.util.ClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Generates the implementation of {@link RpcProxyLoader}.
 *
 * @author Christian Sadilek <csadilek@redhat.com>
 */
@GenerateAsync(RpcProxyLoader.class)
public class RpcProxyLoaderGenerator extends AbstractAsyncGenerator {
  private static final String IOC_MODULE_NAME = "org.jboss.errai.ioc.Container";
  private static final Logger log = LoggerFactory.getLogger(RpcProxyLoaderGenerator.class);
  private final String packageName = RpcProxyLoader.class.getPackage().getName();
  private final String className = RpcProxyLoader.class.getSimpleName() + "Impl";

  @Override
  public String generate(final TreeLogger logger, final GeneratorContext context, final String typeName)
          throws UnableToCompleteException {

    return startAsyncGeneratorsAndWaitFor(RpcProxyLoader.class, context, logger, packageName, className);
  }

  @Override
  protected String generate(final TreeLogger logger, final GeneratorContext context) {
    log.info("generating RPC proxy loader class...");
    final boolean iocEnabled = RebindUtils.isModuleInherited(context, IOC_MODULE_NAME);
    final Function<Annotation[], Annotation[]> annoFilter = ProxyUtil.packageFilter(
            RebindUtils.findTranslatablePackages(context));

    return generate(this::getClassesAnnotatedWith, iocEnabled, annoFilter, context);
  }

  public String generate(final BiFunction<GeneratorContext, Class<? extends Annotation>, Collection<MetaClass>> annotationUsageFinder,
          final boolean iocEnabled,
          final Function<Annotation[], Annotation[]> annotationFilter,
          GeneratorContext context) {

    ClassStructureBuilder<?> classBuilder = ClassBuilder.implement(RpcProxyLoader.class);
    final long time = System.currentTimeMillis();
    final MethodBlockBuilder<?> loadProxies = classBuilder.publicMethod(void.class, "loadProxies",
            Parameter.of(MessageBus.class, "bus", true));

    final Collection<MetaClass> remotes = annotationUsageFinder.apply(context, Remote.class);
    addCacheRelevantClasses(remotes);

    final Collection<MetaClass> featureInterceptors = annotationUsageFinder.apply(context, FeatureInterceptor.class);
    addCacheRelevantClasses(featureInterceptors);

    final Collection<MetaClass> standaloneInterceptors = annotationUsageFinder.apply(context,
            InterceptsRemoteCall.class);
    addCacheRelevantClasses(standaloneInterceptors);

    final InterceptorProvider interceptorProvider = new InterceptorProvider(featureInterceptors,
            standaloneInterceptors);

    for (final MetaClass remote : remotes) {
      if (remote.isInterface()) {
        // create the remote proxy for this interface
        final ClassStructureBuilder<?> remoteProxy = new RpcProxyGenerator(remote, interceptorProvider,
                annotationFilter, iocEnabled).generate();
        loadProxies.append(new InnerClass(remoteProxy.getClassDefinition()));

        // create the proxy provider
        final Statement proxyProvider = ObjectBuilder.newInstanceOf(ProxyProvider.class)
                .extend()
                .publicOverridesMethod("getProxy")
                .append(Stmt.nestedCall(Stmt.newObject(remoteProxy.getClassDefinition())).returnValue())
                .finish()
                .finish();

        loadProxies.append(Stmt.invokeStatic(RemoteServiceProxyFactory.class, "addRemoteProxy", remote, proxyProvider));
      }
    }

    classBuilder = (ClassStructureBuilder<?>) loadProxies.finish();

    final String gen = classBuilder.toJavaString();
    log.info("generated RPC proxy loader class in " + (System.currentTimeMillis() - time) + "ms.");
    return gen;
  }

  @Override
  protected boolean isRelevantClass(final MetaClass clazz) {
    for (final Annotation annotation : clazz.getAnnotations()) {
      if (annotation.annotationType().equals(Remote.class) || annotation.annotationType()
              .equals(FeatureInterceptor.class) || annotation.annotationType().equals(InterceptsRemoteCall.class)) {
        return true;
      }
    }

    return false;
  }

  private Collection<MetaClass> getClassesAnnotatedWith(final GeneratorContext context,
          final Class<? extends Annotation> annotation) {
    return ClassScanner.getTypesAnnotatedWith(annotation, RebindUtils.findTranslatablePackages(context), context);
  }

}
