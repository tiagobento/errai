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

package org.jboss.errai.ioc.rebind.ioc.test.harness;


import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.dev.javac.testing.GeneratorContextBuilder;
import org.jboss.errai.codegen.util.ClassChangeUtil;
import org.jboss.errai.common.client.api.Assert;
import org.jboss.errai.common.metadata.RebindUtils;
import org.jboss.errai.config.util.ClassScanner;
import org.jboss.errai.ioc.client.Bootstrapper;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.IOCBootstrapGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Set;

/**
 * @author Mike Brock <cbrock@redhat.com>
 */
public class MockIOCGenerator {

  private final Set<String> packages;

  public MockIOCGenerator(final Set<String> packages) {
    this.packages = Assert.notNull(packages);
  }

  public Class<? extends Bootstrapper> generate() {
    ClassScanner.setReflectionsScanning(true);

    final String packageName = Bootstrapper.class.getPackage().getName();
    final String className = "MockBootstrapperImpl";
    final GeneratorContext context = GeneratorContextBuilder.newCoreBasedBuilder().buildGeneratorContext();

    final IOCBootstrapGenerator bootstrapGenerator = new IOCBootstrapGenerator(
            ann -> ClassScanner.getTypesAnnotatedWith(ann, packages, context), context, packages, true);


    final String classStr = bootstrapGenerator.generate(packageName, className);

    RebindUtils.writeStringToJavaSourceFileInErraiCacheDir(packageName, className, classStr);

    try {
      final File directory =
              new File(RebindUtils.getTempDirectory() + "/ioc/classes/" + packageName.replaceAll("\\.", "/"));

      final File sourceFile = new File(directory.getAbsolutePath() + "/" + className + ".java");
      final File outFile = new File(directory.getAbsolutePath() + "/" + className + ".class");

      if (sourceFile.exists()) {
        sourceFile.delete();
        outFile.delete();
      }

      directory.mkdirs();

      final FileOutputStream outputStream = new FileOutputStream(sourceFile);

      outputStream.write(classStr.getBytes("UTF-8"));
      outputStream.flush();
      outputStream.close();

      System.out.println("wrote file: " + sourceFile.getAbsolutePath());

      @SuppressWarnings("unchecked")
      final Class<? extends Bootstrapper> bsClass =
          (Class<? extends Bootstrapper>) ClassChangeUtil.compileAndLoad(sourceFile, packageName, className);

      return bsClass;

    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    finally {
      ClassScanner.setReflectionsScanning(false);
    }
  }
}
