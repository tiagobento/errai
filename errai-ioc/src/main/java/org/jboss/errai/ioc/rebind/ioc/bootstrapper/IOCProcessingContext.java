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

package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.jboss.errai.codegen.Context;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.BlockBuilder;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFinder;
import org.jboss.errai.codegen.meta.impl.build.BuildMetaClass;
import org.jboss.errai.common.client.api.Assert;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;

/**
 * @author Mike Brock <cbrock@redhat.com>
 */
public class IOCProcessingContext {
  protected final Set<String> packages;

  protected final Context context;
  protected final BuildMetaClass bootstrapClass;
  protected final ClassStructureBuilder bootstrapBuilder;

  protected final Stack<BlockBuilder<?>> blockBuilder;

  protected final List<Statement> appendToEnd;
  protected final Set<MetaClass> discovered = new HashSet<MetaClass>();

  protected final GeneratorContext generatorContext;

  protected final boolean gwtTarget;
  protected final MetaClassFinder metaClassFinder;

  private IOCProcessingContext(final Builder builder) {
    this.generatorContext = builder.generatorContext;
    this.context = builder.context;
    this.bootstrapClass = builder.bootstrapClassInstance;
    this.bootstrapBuilder = builder.bootstrapBuilder;
    this.metaClassFinder = builder.metaClassFinder;

    this.blockBuilder = new Stack<BlockBuilder<?>>();
    this.blockBuilder.push(builder.blockBuilder);

    this.appendToEnd = new ArrayList<Statement>();
    this.packages = builder.packages;
    this.gwtTarget = builder.gwtTarget;
  }

  public static class Builder {
    private GeneratorContext generatorContext;
    private Context context;
    private BuildMetaClass bootstrapClassInstance;
    private ClassStructureBuilder bootstrapBuilder;
    private BlockBuilder<?> blockBuilder;
    private Set<String> packages;
    private boolean gwtTarget;
    public MetaClassFinder metaClassFinder;

    public static Builder create() {
      return new Builder();
    }

    public Builder generatorContext(final GeneratorContext generatorContext) {
      this.generatorContext = generatorContext;
      return this;
    }

    public Builder metaClassFinder(final MetaClassFinder metaClassFinder) {
      this.metaClassFinder = metaClassFinder;
      return this;
    }

    public Builder context(final Context context) {
      this.context = context;
      return this;
    }

    public Builder bootstrapClassInstance(final BuildMetaClass bootstrapClassInstance) {
      this.bootstrapClassInstance = bootstrapClassInstance;
      return this;
    }

    public Builder bootstrapBuilder(final ClassStructureBuilder classStructureBuilder) {
      this.bootstrapBuilder = classStructureBuilder;
      return this;
    }

    public Builder blockBuilder(final BlockBuilder<?> blockBuilder) {
      this.blockBuilder = blockBuilder;
      return this;
    }

    public Builder packages(final Set<String> packages) {
      this.packages = packages;
      return this;
    }

    public Builder gwtTarget(final boolean gwtTarget) {
      this.gwtTarget = gwtTarget;
      return this;
    }

    public IOCProcessingContext build() {
      // Assert.notNull("treeLogger cannot be null", treeLogger);
      // Assert.notNull("sourceWriter cannot be null", sourceWriter);
      // Assert.notNull("context cannot be null", context);
      // Assert.notNull("packages cannot be null", packages);
      Assert.notNull("bootstrapClassInstance cannot be null", bootstrapClassInstance);
      Assert.notNull("bootstrapBuilder cannot be null", bootstrapBuilder);
      Assert.notNull("blockBuilder cannot be null", blockBuilder);

      return new IOCProcessingContext(this);
    }
  }

  public BlockBuilder<?> getBlockBuilder() {
    return blockBuilder.peek();
  }

  public BlockBuilder<?> append(final Statement statement) {
    return getBlockBuilder().append(statement);
  }

  public void insertBefore(final Statement statement) {
     getBlockBuilder().insertBefore(statement);
  }


  public void pushBlockBuilder(final BlockBuilder<?> blockBuilder) {
    this.blockBuilder.push(blockBuilder);
  }

  public void popBlockBuilder() {
    this.blockBuilder.pop();

    if (this.blockBuilder.size() == 0) {
      throw new AssertionError("block builder was over popped! something is wrong.");
    }
  }

  public void appendToEnd(final Statement statement) {
    appendToEnd.add(statement);
  }

  public List<Statement> getAppendToEnd() {
    return appendToEnd;
  }

  public BuildMetaClass getBootstrapClass() {
    return bootstrapClass;
  }

  public ClassStructureBuilder getBootstrapBuilder() {
    return bootstrapBuilder;
  }

  public Context getContext() {
    return context;
  }

  public MetaClassFinder metaClassFinder() {
    return metaClassFinder;
  }

  public Set<String> getPackages() {
    return packages;
  }

  public GeneratorContext getGeneratorContext() {
    return generatorContext;
  }

  public boolean isGwtTarget() {
    return gwtTarget;
  }
}
