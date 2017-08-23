/*
 * Copyright (C) 2017 Red Hat, Inc. and/or its affiliates.
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

package org.jboss.errai.common.apt.metaclass;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaConstructor;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.codegen.meta.MetaType;
import org.jboss.errai.codegen.meta.MetaTypeVariable;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class APTConstructor extends MetaConstructor implements APTMember {

  private final ExecutableElement ctor;

  public APTConstructor(final ExecutableElement ctor) {
    this.ctor = ctor;
  }

  @Override
  public Element getMember() {
    return ctor;
  }

  @Override
  public MetaTypeVariable[] getTypeParameters() {
    return APTClassUtil.getTypeParameters(ctor);
  }

  @Override
  public MetaParameter[] getParameters() {
    return APTClassUtil.getParameters(ctor);
  }

  @Override
  public MetaType[] getGenericParameterTypes() {
    return APTClassUtil.getGenericParameterTypes(ctor);
  }

  @Override
  public boolean isVarArgs() {
    return ctor.isVarArgs();
  }

  @Override
  public MetaClass getReturnType() {
    return new APTClass(ctor.getReturnType());
  }

  @Override
  public MetaType getGenericReturnType() {
    return APTClassUtil.fromTypeMirror(ctor.getReturnType());
  }

  @Override
  public MetaClass[] getCheckedExceptions() {
    return APTClassUtil.getCheckedExceptions(ctor);
  }

}