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

import jsinterop.annotations.JsType;
import org.jboss.errai.bus.client.ErraiBus;
import org.jboss.errai.bus.client.api.Subscription;
import org.jboss.errai.codegen.Context;
import org.jboss.errai.codegen.Parameter;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.AnonymousClassStructureBuilder;
import org.jboss.errai.codegen.builder.BlockBuilder;
import org.jboss.errai.codegen.builder.ContextualStatementBuilder;
import org.jboss.errai.codegen.meta.MetaAnnotation;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFinder;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.codegen.util.Refs;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.common.client.api.annotations.LocalEvent;
import org.jboss.errai.common.client.api.annotations.Portable;
import org.jboss.errai.config.ErraiConfiguration;
import org.jboss.errai.enterprise.client.cdi.AbstractCDIEventCallback;
import org.jboss.errai.enterprise.client.cdi.JsTypeEventObserver;
import org.jboss.errai.enterprise.client.cdi.api.CDI;
import org.jboss.errai.ioc.client.api.CodeDecorator;
import org.jboss.errai.ioc.client.container.Factory;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.InjectUtil;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCDecoratorExtension;
import org.jboss.errai.ioc.rebind.ioc.injector.api.Decorable;
import org.jboss.errai.ioc.rebind.ioc.injector.api.FactoryController;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.jboss.errai.codegen.meta.MetaClassFactory.parameterizedAs;
import static org.jboss.errai.codegen.meta.MetaClassFactory.typeParametersOf;
import static org.jboss.errai.codegen.util.Stmt.castTo;
import static org.jboss.errai.codegen.util.Stmt.declareFinalVariable;
import static org.jboss.errai.codegen.util.Stmt.invokeStatic;

/**
 * Generates the boiler plate for @Observes annotations use in GWT clients.<br/>
 * Basically creates a subscription for a CDI event type that invokes on the annotated method.
 *
 * @author Heiko Braun <hbraun@redhat.com>
 * @author Mike Brock <cbrock@redhat.com>
 * @author Christian Sadilek <csadilek@redhat.com>
 * @author Max Barkley <mbarkley@redhat.com>
 */
@CodeDecorator
public class ObservesExtension extends IOCDecoratorExtension<Observes> {
  public ObservesExtension(final Class<Observes> decoratesWith) {
    super(decoratesWith);
  }

  @Override
  public void generateDecorator(final Decorable decorable, final FactoryController controller) {

    final ErraiConfiguration erraiConfiguration = decorable.getInjectionContext().getProcessingContext().erraiConfiguration();
    final MetaClassFinder metaClassFinder = decorable.getInjectionContext().getProcessingContext().metaClassFinder();

    final Context ctx = decorable.getCodegenContext();
    final MetaParameter parm = decorable.getAsParameter();
    final MetaMethod method = (MetaMethod) parm.getDeclaringMember();

    controller.ensureMemberExposed(parm);

    final MetaClass eventType = parm.getType().asBoxed();
    final String parmClassName = eventType.getFullyQualifiedName();
    final List<MetaAnnotation> qualifiers = InjectUtil.extractQualifiers(parm);
    final Set<String> qualifierNames = new HashSet<>(getMetaQualifiersPart(qualifiers));
    final boolean isEnclosingTypeDependent = decorable.isEnclosingTypeDependent();

    if (qualifierNames.contains(Any.class.getName())) {
      qualifierNames.remove(Any.class.getName());
    }

    final MetaClass callBackType = parameterizedAs(AbstractCDIEventCallback.class, typeParametersOf(eventType));

    AnonymousClassStructureBuilder callBack = Stmt.newObject(callBackType).extend();

    BlockBuilder<AnonymousClassStructureBuilder> callBackBlock;

    if (!qualifierNames.isEmpty()) {
      callBackBlock = callBack.initialize();
      for (final String qualifierName : qualifierNames) {
        callBackBlock.append(Stmt.loadClassMember("qualifierSet").invoke("add", qualifierName));
      }
      callBack = callBackBlock.finish();
    }

    final List<Statement> callbackStatements = new ArrayList<>();
    if (!isEnclosingTypeDependent) {
      callbackStatements
              .add(declareFinalVariable("instance", decorable.getDecorableDeclaringType(), castTo(decorable.getEnclosingInjectable().getInjectedType(),
                      invokeStatic(Factory.class, "maybeUnwrapProxy", controller.contextGetInstanceStmt()))));
    }
    callbackStatements.add(decorable.call(Refs.get("event")));

    callBackBlock = callBack.publicOverridesMethod("fireEvent", Parameter.finalOf(eventType, "event"))
        .appendAll(callbackStatements)
        .finish()
        .publicOverridesMethod("toString")
        ._(Stmt.load("Observer: " + parmClassName + " " + Arrays.toString(qualifiers.toArray())).returnValue());

    final List<Statement> initStatements = new ArrayList<>();
    final List<Statement> destroyStatements = new ArrayList<>();
    final String subscrVar = method.getName() + "Subscription";

    final String subscribeMethod;

    if (eventType.isAnnotationPresent(JsType.class)) {
      subscribeMethod = "subscribeJsType";
      callBackBlock = getJsTypeSubscriptionCallback(decorable, controller);
    }
    else if (isPortableType(metaClassFinder, erraiConfiguration, eventType) && !eventType.isAnnotationPresent(LocalEvent.class)) {
      subscribeMethod = "subscribe";
      callBackBlock = getSubscriptionCallback(decorable, controller);
    }
    else {
      subscribeMethod = "subscribeLocal";
      callBackBlock = getSubscriptionCallback(decorable, controller);
    }

    final Statement subscribeStatement = Stmt.create(ctx).invokeStatic(CDI.class, subscribeMethod, parmClassName,
            callBackBlock.finish().finish());

    if (isEnclosingTypeDependent) {
      initStatements.add(controller.setReferenceStmt(subscrVar, subscribeStatement));
      destroyStatements.add(controller.getReferenceStmt(subscrVar, Subscription.class).invoke("remove"));
    } else {
      initStatements.add(subscribeStatement);
    }

    for (final MetaClass cls : allPortableConcreteSubtypes(erraiConfiguration,
            metaClassFinder, eventType)) {
      if (!cls.isAnnotationPresent(LocalEvent.class)) {
        final ContextualStatementBuilder routingSubStmt = Stmt.invokeStatic(ErraiBus.class, "get").invoke("subscribe",
                CDI.getSubjectNameByType(cls.getFullyQualifiedName()), Stmt.loadStatic(CDI.class, "ROUTING_CALLBACK"));
        if (isEnclosingTypeDependent) {
          final String subscrHandle = subscrVar + "For" + cls.getName();
          initStatements.add(controller.setReferenceStmt(subscrHandle, routingSubStmt));
          destroyStatements.add(
                  Stmt.nestedCall(controller.getReferenceStmt(subscrHandle, Subscription.class)).invoke("remove"));
        } else {
          initStatements.add(routingSubStmt);
        }
      }
    }

    if (isEnclosingTypeDependent) {
      controller.addInitializationStatements(initStatements);
      controller.addDestructionStatements(destroyStatements);
    } else {
      controller.addFactoryInitializationStatements(initStatements);
    }
  }

  private BlockBuilder<AnonymousClassStructureBuilder> getSubscriptionCallback(final Decorable decorable, final FactoryController controller) {

    final MetaParameter parm = decorable.getAsParameter();
    final MetaClass eventType = parm.getType().asBoxed();
    final String parmClassName = eventType.getFullyQualifiedName();
    final List<MetaAnnotation> qualifiers = InjectUtil.extractQualifiers(parm);
    final Set<String> qualifierNames = new HashSet<>(getMetaQualifiersPart(qualifiers));

    final MetaClass callBackType = parameterizedAs(AbstractCDIEventCallback.class, typeParametersOf(eventType));
    AnonymousClassStructureBuilder callBack = Stmt.newObject(callBackType).extend();
    BlockBuilder<AnonymousClassStructureBuilder> callBackBlock;

    if (!qualifierNames.isEmpty()) {
      callBackBlock = callBack.initialize();
      for (final String qualifierName : qualifierNames) {
        callBackBlock.append(Stmt.loadClassMember("qualifierSet").invoke("add", qualifierName));
      }
      callBack = callBackBlock.finish();
    }

    final List<Statement> fireEventStmts = new ArrayList<>();
    if (!decorable.isEnclosingTypeDependent()) {
      fireEventStmts.add(Stmt.declareFinalVariable("instance", decorable.getEnclosingInjectable().getInjectedType(),
              Stmt.invokeStatic(Factory.class, "maybeUnwrapProxy", controller.contextGetInstanceStmt())));
    }
    fireEventStmts.add(decorable.call(Refs.get("event")));

    callBackBlock = callBack.publicOverridesMethod("fireEvent", Parameter.finalOf(eventType, "event"))
        .appendAll(fireEventStmts)
        .finish()
        .publicOverridesMethod("toString")
        ._(Stmt.load("Observer: " + parmClassName + " " + Arrays.toString(qualifiers.toArray())).returnValue());

    return callBackBlock;
  }

  private BlockBuilder<AnonymousClassStructureBuilder> getJsTypeSubscriptionCallback(final Decorable decorable, final FactoryController controller) {

    final MetaParameter parm = decorable.getAsParameter();
    final MetaClass eventType = parm.getType().asBoxed();
    final String parmClassName = eventType.getFullyQualifiedName();

    final MetaClass callBackType = parameterizedAs(JsTypeEventObserver.class, typeParametersOf(eventType));
    final AnonymousClassStructureBuilder callBack = Stmt.newObject(callBackType).extend();
    BlockBuilder<AnonymousClassStructureBuilder> callBackBlock;

    final List<Statement> fireEventStmts = new ArrayList<>();
    if (!decorable.isEnclosingTypeDependent()) {
      fireEventStmts.add(Stmt.declareFinalVariable("instance", decorable.getEnclosingInjectable().getInjectedType(),
              Stmt.invokeStatic(Factory.class, "maybeUnwrapProxy", controller.contextGetInstanceStmt())));
    }
    fireEventStmts.add(decorable.call(Refs.get("event")));

    callBackBlock = callBack.publicOverridesMethod("onEvent", Parameter.finalOf(eventType, "event"))
        .appendAll(fireEventStmts)
        .finish()
        .publicOverridesMethod("toString")
        ._(Stmt.load("JsTypeObserver: " + parmClassName).returnValue());

    return callBackBlock;
  }

  private static Set<String> getMetaQualifiersPart(final Collection<MetaAnnotation> qualifiers) {
    Set<String> qualifiersPart = null;
    if (qualifiers != null) {
      for (final MetaAnnotation qualifier : qualifiers) {
        if (qualifiersPart == null)
          qualifiersPart = new HashSet<>(qualifiers.size());

        qualifiersPart.add(asString(qualifier));
      }
    }
    return qualifiersPart == null ? Collections.emptySet() : qualifiersPart;
  }

  private static String asString(final MetaAnnotation qualifier) {
    final StringBuilder builder = new StringBuilder(qualifier.annotationType().getName());
    final Map<String, Object> values = qualifier.values();

    if (values.isEmpty()) {
      builder.append('(');
      for (final Map.Entry<String, Object> e : values.entrySet()) {
        builder.append(e.getKey()).append('=').append(e.getValue()).append(',');
      }
      builder.replace(builder.length() - 1, builder.length(), ")");
    }

    return builder.toString();
  }


  private Set<MetaClass> allPortableConcreteSubtypes(final ErraiConfiguration erraiConfiguration,
          final MetaClassFinder metaClassFinder,
          final MetaClass metaClass) {

    return allPortableTypes(metaClassFinder, erraiConfiguration)
            .stream()
            .filter(s -> !s.isInterface())
            .filter(s -> s.isAssignableTo(metaClass))
            .collect(toSet());
  }

  private boolean isPortableType(final MetaClassFinder metaClassFinder, final ErraiConfiguration erraiConfiguration,
          final MetaClass metaClass) {

    return metaClass.instanceOf(String.class) || allPortableTypes(metaClassFinder, erraiConfiguration).contains(metaClass) || isBuiltinPortable(metaClass);
  }

  private Set<MetaClass> allPortableTypes(final MetaClassFinder metaClassFinder,
          final ErraiConfiguration erraiConfiguration) {

    //FIXME: tiago: are @Remote types also portable? See RpcTypesProvider
    return metaClassFinder.extend(Portable.class, erraiConfiguration.modules()::getSerializableTypes)
            .findAnnotatedWith(Portable.class);
  }

  private boolean isBuiltinPortable(final MetaClass metaClass) {
    return false; //FIXME: tiago: implement
  }
}
