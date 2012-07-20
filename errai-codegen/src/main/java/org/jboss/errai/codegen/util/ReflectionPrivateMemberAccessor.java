package org.jboss.errai.codegen.util;

import org.jboss.errai.codegen.Cast;
import org.jboss.errai.codegen.DefParameters;
import org.jboss.errai.codegen.Modifier;
import org.jboss.errai.codegen.Parameter;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.BlockBuilder;
import org.jboss.errai.codegen.builder.CatchBlockBuilder;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.builder.ContextualStatementBuilder;
import org.jboss.errai.codegen.builder.MethodCommentBuilder;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.codegen.meta.MetaConstructor;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mike Brock
 */
public class ReflectionPrivateMemberAccessor implements PrivateMemberAccessor {

  private static final String JAVA_REFL_FLD_UTIL_METH = "_getAccessibleField";
  private static final String JAVA_REFL_METH_UTIL_METH = "_getAccessibleMethod";
  private static final String JAVA_REFL_CONSTRUCTOR_UTIL_METH = "_getAccessibleConstructor";

  public static void createJavaReflectionFieldInitializerUtilMethod(final ClassStructureBuilder<?> classBuilder) {

    if (classBuilder.getClassDefinition().getMethod(JAVA_REFL_FLD_UTIL_METH, Class.class, Field.class) != null) {
      return;
    }

    classBuilder.privateMethod(Field.class, JAVA_REFL_FLD_UTIL_METH).modifiers(Modifier.Static)
            .parameters(DefParameters.of(Parameter.of(Class.class, "cls"), Parameter.of(String.class, "name")))
            .body()
            ._(Stmt.try_()
                    ._(Stmt.declareVariable("fld", Stmt.loadVariable("cls").invoke("getDeclaredField",
                            Stmt.loadVariable("name"))))
                    ._(Stmt.loadVariable("fld").invoke("setAccessible", true))
                    ._(Stmt.loadVariable("fld").returnValue())
                    .finish()
                    .catch_(Throwable.class, "e")
                    ._(Stmt.loadVariable("e").invoke("printStackTrace"))
                    ._(Stmt.throw_(RuntimeException.class, Refs.get("e")))
                    .finish())
            .finish();
  }

  public static void createJavaReflectionMethodInitializerUtilMethod(
          final ClassStructureBuilder<?> classBuilder) {

    if (classBuilder.getClassDefinition().getMethod(JAVA_REFL_METH_UTIL_METH, Class.class, String.class, Class[].class) != null) {
      return;
    }

    classBuilder.privateMethod(Method.class, JAVA_REFL_METH_UTIL_METH).modifiers(Modifier.Static)
            .parameters(DefParameters.of(Parameter.of(Class.class, "cls"), Parameter.of(String.class, "name"),
                    Parameter.of(Class[].class, "parms")))
            .body()
            ._(Stmt.try_()
                    ._(Stmt.declareVariable("meth", Stmt.loadVariable("cls").invoke("getDeclaredMethod",
                            Stmt.loadVariable("name"), Stmt.loadVariable("parms"))))
                    ._(Stmt.loadVariable("meth").invoke("setAccessible", true))
                    ._(Stmt.loadVariable("meth").returnValue())
                    .finish()
                    .catch_(Throwable.class, "e")
                    ._(Stmt.loadVariable("e").invoke("printStackTrace"))
                    ._(Stmt.throw_(RuntimeException.class, Refs.get("e")))
                    .finish())
            .finish();
  }

  public static void createJavaReflectionConstructorInitializerUtilMethod(
          final ClassStructureBuilder<?> classBuilder) {

    if (classBuilder.getClassDefinition().getMethod(JAVA_REFL_CONSTRUCTOR_UTIL_METH, Class.class,
            Class[].class) != null) {
      return;
    }

    classBuilder.privateMethod(Constructor.class, JAVA_REFL_CONSTRUCTOR_UTIL_METH).modifiers(Modifier.Static)
            .parameters(DefParameters.of(Parameter.of(Class.class, "cls"), Parameter.of(Class[].class, "parms")))
            .body()
            ._(Stmt.try_()
                    ._(Stmt.declareVariable("cons", Stmt.loadVariable("cls").invoke("getDeclaredConstructor",
                            Stmt.loadVariable("parms"))))
                    ._(Stmt.loadVariable("cons").invoke("setAccessible", true))
                    ._(Stmt.loadVariable("cons").returnValue())
                    .finish()
                    .catch_(Throwable.class, "e")
                    ._(Stmt.loadVariable("e").invoke("printStackTrace"))
                    ._(Stmt.throw_(RuntimeException.class, Refs.get("e")))
                    .finish())
            .finish();
  }

  public static String initCachedField(final ClassStructureBuilder<?> classBuilder, final MetaField f) {
    createJavaReflectionFieldInitializerUtilMethod(classBuilder);

    final String fieldName = PrivateAccessUtil.getPrivateFieldInjectorName(f) + "_fld";

    if (classBuilder.getClassDefinition().getField(fieldName) != null) {
      return fieldName;
    }

    classBuilder.privateField(fieldName, Field.class).modifiers(Modifier.Static)
            .initializesWith(Stmt.invokeStatic(classBuilder.getClassDefinition(), JAVA_REFL_FLD_UTIL_METH,
                    f.getDeclaringClass(), f.getName())).finish();

    return fieldName;
  }

  public static String initCachedMethod(final ClassStructureBuilder<?> classBuilder, final MetaMethod m) {
    createJavaReflectionMethodInitializerUtilMethod(classBuilder);

    final String fieldName = PrivateAccessUtil.getPrivateMethodName(m) + "_meth";

    classBuilder.privateField(fieldName, Method.class).modifiers(Modifier.Static)
            .initializesWith(Stmt.invokeStatic(classBuilder.getClassDefinition(), JAVA_REFL_METH_UTIL_METH,
                    m.getDeclaringClass(), m.getName(), MetaClassFactory.asClassArray(m.getParameters()))).finish();

    return fieldName;
  }

  public static String initCachedMethod(final ClassStructureBuilder<?> classBuilder, final MetaConstructor c) {
    createJavaReflectionConstructorInitializerUtilMethod(classBuilder);

    final String fieldName = PrivateAccessUtil.getPrivateMethodName(c) + "_meth";

    classBuilder.privateField(fieldName, Constructor.class).modifiers(Modifier.Static)
            .initializesWith(Stmt.invokeStatic(classBuilder.getClassDefinition(), JAVA_REFL_CONSTRUCTOR_UTIL_METH,
                    c.getDeclaringClass(), MetaClassFactory.asClassArray(c.getParameters()))).finish();

    return fieldName;
  }


  @Override
  public void createWritableField(MetaClass type,
                                  ClassStructureBuilder<?> classBuilder,
                                  MetaField field,
                                  Modifier[] modifiers) {

    final String cachedField = initCachedField(classBuilder, field);
    final String setterName = PrivateAccessUtil.getReflectionFieldSetterName(field);

    final MethodCommentBuilder<? extends ClassStructureBuilder<?>> methodBuilder =
            classBuilder.privateMethod(void.class, PrivateAccessUtil.getPrivateFieldInjectorName(field));

    if (!field.isStatic()) {
      methodBuilder
              .parameters(DefParameters.fromParameters(Parameter.of(field.getDeclaringClass(), "instance"),
                      Parameter.of(field.getType(), "value")));
    }

    methodBuilder.modifiers(modifiers)
            .body()
            ._(Stmt.try_()
                    ._(Stmt.loadVariable(cachedField).invoke(setterName, Refs.get("instance"), Refs.get("value")))
                    .finish()
                    .catch_(Throwable.class, "e")
                    ._(Stmt.loadVariable("e").invoke("printStackTrace"))
                    ._(Stmt.throw_(RuntimeException.class, Refs.get("e")))
                    .finish())
            .finish();
  }


  @Override
  public void createReadableField(MetaClass type,
                                  ClassStructureBuilder<?> classBuilder,
                                  MetaField field,
                                  Modifier[] modifiers) {

    final String cachedField = initCachedField(classBuilder, field);
    final String getterName = PrivateAccessUtil.getReflectionFieldGetterName(field);

    final MethodCommentBuilder<? extends ClassStructureBuilder<?>> methodBuilder =
            classBuilder.privateMethod(field.getType(), PrivateAccessUtil.getPrivateFieldInjectorName(field));

    if (!field.isStatic()) {
      methodBuilder
              .parameters(DefParameters.fromParameters(Parameter.of(field.getDeclaringClass(), "instance")));
    }

    methodBuilder.modifiers(modifiers)
            .body()
            ._(Stmt.try_()
                    ._(Stmt.nestedCall(Cast.to(field.getType(), Stmt.loadVariable(cachedField)
                            .invoke(getterName, field.isStatic() ? null : Refs.get("instance")))).returnValue())
                    .finish()
                    .catch_(Throwable.class, "e")
                    ._(Stmt.loadVariable("e").invoke("printStackTrace"))
                    ._(Stmt.throw_(RuntimeException.class, Refs.get("e")))
                    .finish())
            .finish();
  }

  @Override
  public void makeMethodAccessible(ClassStructureBuilder<?> classBuilder,
                                   MetaMethod method,
                                   Modifier[] modifiers) {

    final List<Parameter> wrapperDefParms = new ArrayList<Parameter>();

    if (!method.isStatic()) {
      wrapperDefParms.add(Parameter.of(method.getDeclaringClass().getErased(), "instance"));
    }

    final List<Parameter> methodDefParms = DefParameters.from(method).getParameters();
    wrapperDefParms.addAll(methodDefParms);

    final Object[] args = new Object[methodDefParms.size()];

    int i = 0;
    for (final Parameter p : methodDefParms) {
      args[i++] = Refs.get(p.getName());
    }

    final String cachedMethod = initCachedMethod(classBuilder, method);


    final BlockBuilder<? extends ClassStructureBuilder> body
            = classBuilder.publicMethod(method.getReturnType(),
            PrivateAccessUtil.getPrivateMethodName(method))
            .parameters(DefParameters.fromParameters(wrapperDefParms))
            .modifiers(modifiers)
            .body();

    final BlockBuilder<CatchBlockBuilder> tryBuilder = Stmt.try_();

    final ContextualStatementBuilder statementBuilder = Stmt.loadVariable(cachedMethod)
            .invoke("invoke", method.isStatic() ? null : Refs.get("instance"), args);

    if (method.getReturnType().isVoid()) {
      tryBuilder._(statementBuilder);
    }
    else {
      tryBuilder._(statementBuilder.returnValue());
    }

    body._(tryBuilder
            .finish()
            .catch_(Throwable.class, "e")
            ._(Stmt.loadVariable("e").invoke("printStackTrace"))
            ._(Stmt.throw_(RuntimeException.class, Refs.get("e")))
            .finish())
            .finish();
  }

  @Override
  public void makeConstructorAccessible(ClassStructureBuilder<?> classBuilder, MetaConstructor constructor) {

    final DefParameters methodDefParms = DefParameters.from(constructor);


    final String cachedMethod = initCachedMethod(classBuilder, constructor);
    final Object[] args = new Object[methodDefParms.getParameters().size()];
    int i = 0;
    for (final Parameter p : methodDefParms.getParameters()) {
      args[i++] = Refs.get(p.getName());
    }

    final BlockBuilder<? extends ClassStructureBuilder> body = classBuilder.publicMethod(constructor.getReturnType(),
             PrivateAccessUtil.getPrivateMethodName(constructor))
            .parameters(methodDefParms)
            .modifiers(Modifier.Static)
            .body();

    final Statement tryBuilder =
            Stmt.try_()
                    .append(
                            Stmt.nestedCall(
                                    Stmt.castTo(constructor.getReturnType(),
                                            Stmt.loadVariable(cachedMethod).invoke("newInstance",
                                            (Object) args))).returnValue())
                    .finish()
                    .catch_(Throwable.class, "e")
                    .append(Stmt.loadVariable("e").invoke("printStackTrace"))
                    .append(Stmt.throw_(RuntimeException.class, Refs.get("e")))
                    .finish();

    body.append(tryBuilder).finish();
  }
}
