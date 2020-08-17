package org.jboss.errai.ui.rebind.ioc.element;

import com.google.gwt.dom.client.TagName;
import jsinterop.annotations.JsType;
import org.jboss.errai.codegen.meta.MetaAnnotation;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.common.client.api.annotations.ClassNames;
import org.jboss.errai.common.client.api.annotations.Element;
import org.jboss.errai.common.client.api.annotations.Properties;
import org.jboss.errai.common.client.api.annotations.Property;
import org.jboss.errai.ioc.client.api.IOCExtension;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.IOCProcessingContext;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCExtensionConfigurator;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Qualifier;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.InjectableHandle;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

/**
 * Satisfies injection points for DOM elements.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 * @author Tiago Bento <tfernand@redhat.com>
 */
@IOCExtension public class ElementProviderExtension implements IOCExtensionConfigurator {

  private static final MetaClass ELEMENTAL_ELEMENT_META_CLASS = MetaClassFactory.get(elemental2.dom.Element.class);

  private static final MetaClass GWT_ELEMENT_META_CLASS = MetaClassFactory.get(com.google.gwt.dom.client.Element.class);

  @Override public void configure(final IOCProcessingContext context, final InjectionContext injectionContext) {
  }

  @Override public void afterInitialization(final IOCProcessingContext context,
          final InjectionContext injectionContext) {
    injectionContext.registerExtensionTypeCallback(type -> {
      try {
        register(getElementTags(type), injectionContext, type);
      } catch (final Throwable t) {
        final String typeName = type.getFullyQualifiedName();
        final String className = ElementProviderExtension.class.getSimpleName();
        final String msg = String.format("Error occurred while processing [%s] in %s.", typeName, className);
        throw new RuntimeException(msg, t);
      }
    });
  }

  private void register(final Collection<String> tags, final InjectionContext injectionContext, final MetaClass type) {

    for (final String tag : tags) {
      final Qualifier qualifier = injectionContext.getQualifierFactory().forSource(new HasNamedAnnotation(tag));

      final InjectableHandle handle = new InjectableHandle(type, qualifier);

      final ElementInjectionBodyGenerator injectionBodyGenerator = new ElementInjectionBodyGenerator(type, tag,
              getProperties(type), getClassNames(type));

      final ElementProvider elementProvider = new ElementProvider(handle, injectionBodyGenerator);

      injectionContext.registerExactTypeInjectableProvider(handle, elementProvider);
    }
  }

  private Collection<String> getElementTags(final MetaClass type) {
    if (type.isAssignableTo(ELEMENTAL_ELEMENT_META_CLASS)) {
      return elemental2ElementTags(type);
    }

    if (type.isAssignableTo(GWT_ELEMENT_META_CLASS)) {
      return gwtElementTags(type);
    }

    return getCustomElementTags(type);
  }

  static Collection<String> elemental2ElementTags(final MetaClass type) {
    final Collection<String> customElementTags = getCustomElementTags(type);

    if (!customElementTags.isEmpty()) {
      return customElementTags;
    }

    return Elemental2TagMapping.getTags(type);
  }

  private static List<String> gwtElementTags(MetaClass type) {
    return type.getAnnotation(TagName.class).map(a -> Arrays.asList(a.valueAsArray(String[].class)))
            .orElse(emptyList());
  }

  private static Collection<String> getCustomElementTags(final MetaClass type) {

    final Optional<MetaAnnotation> elementAnnotation = type.getAnnotation(Element.class);
    if (!elementAnnotation.isPresent()) {
      return Collections.emptyList();
    }

    final Optional<MetaAnnotation> jsTypeAnnotation = type.getAnnotation(JsType.class);
    if (!jsTypeAnnotation.isPresent() || !jsTypeAnnotation.get().<Boolean>value("isNative")) {
      final String element = Element.class.getSimpleName();
      final String jsType = JsType.class.getSimpleName();
      throw new RuntimeException(element + " is only valid on native " + jsType + "s.");
    }

    return Arrays.asList(elementAnnotation.get().valueAsArray(String[].class));
  }

  private static Set<Property> getProperties(final MetaClass type) {
    final Set<Property> properties = new HashSet<>();

    final Optional<MetaAnnotation> declaredProperty = type.getAnnotation(Property.class);
    final Optional<MetaAnnotation> declaredProperties = type.getAnnotation(Properties.class);

    declaredProperty.map(ElementProviderExtension::newProperty).ifPresent(properties::add);

    declaredProperties.map(a -> Arrays.stream(a.valueAsArray(MetaAnnotation[].class))).orElse(Stream.of())
            .map(ElementProviderExtension::newProperty).forEach(properties::add);

    return properties;
  }

  @SuppressWarnings("unchecked") private static Property newProperty(final MetaAnnotation m) {
    return new Property() {

      @Override public Class<? extends Annotation> annotationType() {
        return Property.class;
      }

      @Override public String name() {
        return m.value("name");
      }

      @Override public String value() {
        return m.value("value");
      }
    };
  }

  private List<String> getClassNames(final MetaClass type) {
    return type.getAnnotation(ClassNames.class).map(a -> Arrays.asList(a.valueAsArray(String[].class)))
            .orElse(emptyList());
  }
}