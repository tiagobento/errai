package org.jboss.errai.common.client.logging.util;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

import static org.jboss.errai.common.client.logging.util.ReflectableJSOUtils.nativeCreate;

/**
 * A utility class for accessing arbitrary properties of native Javascript types.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@JsType(isNative = true, namespace = "org.jboss.errai") public class ReflectableJSO {

  private ReflectableJSO(final Object wrapped) {
  }

  @JsOverlay public static final ReflectableJSO create(final Object wrapped) {
    nativeCreate();
    return new ReflectableJSO(wrapped);
  }

  @JsOverlay public final boolean hasProperty(final String name) {
    return get(name) != null;
  }

  public final native Object get(final String name);

  public final native void set(final String name, final Object value);

  public final native String[] properties();

  public final native Object unwrap();
}