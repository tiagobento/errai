package org.jboss.errai.common.client.logging.util;

public class ReflectableJSOUtils {

  public static native void nativeCreate() /*-{
      $wnd.org = {
          jboss: {
              errai: {
                  ReflectableJSO: function (wrapped) {
                      this.get = function (name) {
                          return wrapped[name];
                      };
                      this.set = function (name, value) {
                          wrapped[name] = value;
                      };
                      this.properties = function () {
                          var retVal = [];
                          for (key in wrapped) {
                              retVal.push(key);
                          }
                          return retVal;
                      };
                      this.unwrap = function () {
                          return wrapped;
                      }
                  }
              }
          }
      }
  }-*/;
}
