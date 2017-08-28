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

/**
 * Generates the implementation of {@link RpcProxyLoader}.
 *
 * @author Christian Sadilek <csadilek@redhat.com>
 */
@GenerateAsync(RpcProxyLoader.class)
public class RpcProxyLoaderGenerator extends AbstractAsyncGenerator {
  private static final String IOC_MODULE_NAME = "org.jboss.errai.ioc.Container";
  private static final Logger log = LoggerFactory.getLogger(RpcProxyLoaderGenerator.class);

  private final String packageName = "org.jboss.errai.bus.client.framework";
  private final String classSimpleName = "RpcProxyLoaderImpl";

  @Override
  public String generate(final TreeLogger logger, final GeneratorContext context, final String typeName)
          throws UnableToCompleteException {

    return startAsyncGeneratorsAndWaitFor(RpcProxyLoader.class, context, logger, packageName, classSimpleName);
  }

  @Override
  protected String generate(final TreeLogger logger, final GeneratorContext context) {
    final Boolean iocEnabled = RebindUtils.isModuleInherited(context, IOC_MODULE_NAME);
    final AnnotationFilter gwtAnnotationFilter = gwtAnnotationFilter(context);

    return generate(this::getMetaClasses, iocEnabled, gwtAnnotationFilter, context);
  }

  private AnnotationFilter gwtAnnotationFilter(final GeneratorContext context) {
    return (annotation) -> ProxyUtil.packageFilter(RebindUtils.findTranslatablePackages(context)).apply(annotation);
  }

  public String generate(final MetaClassFinder metaClassFinder,
          final boolean iocEnabled,
          final AnnotationFilter annotationFilter,
          final GeneratorContext context) {

    log.info("generating RPC proxy loader class...");

    ClassStructureBuilder<?> classBuilder = ClassBuilder.implement(RpcProxyLoader.class);
    final long time = System.currentTimeMillis();
    final MethodBlockBuilder<?> loadProxies = classBuilder.publicMethod(void.class, "loadProxies",
            Parameter.of(MessageBus.class, "bus", true));

    final Collection<MetaClass> remotes = metaClassFinder.find(context, Remote.class);
    addCacheRelevantClasses(remotes);

    final Collection<MetaClass> featureInterceptors = metaClassFinder.find(context, FeatureInterceptor.class);
    addCacheRelevantClasses(featureInterceptors);

    final Collection<MetaClass> standaloneInterceptors = metaClassFinder.find(context, InterceptsRemoteCall.class);
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
    for (final Annotation annotation : clazz.unsafeGetAnnotations()) {
      if (annotation.annotationType().equals(Remote.class) || annotation.annotationType()
              .equals(FeatureInterceptor.class) || annotation.annotationType().equals(InterceptsRemoteCall.class)) {
        return true;
      }
    }

    return false;
  }

  private Collection<MetaClass> getMetaClasses(final GeneratorContext context,
          final Class<? extends Annotation> annotation) {
    return ClassScanner.getTypesAnnotatedWith(annotation, RebindUtils.findTranslatablePackages(context), context);
  }

  public String getFullQualifiedClassName() {
    return packageName + "." + classSimpleName;
  }

}
