/*
 * Copyright 2011 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.ioc.rebind.ioc.graph;

import com.google.gwt.dev.util.collect.IdentityHashSet;
import org.jboss.errai.codegen.meta.MetaClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A sort unit is a logical sorting element for the IOC container on which to order operations in order to correctly
 * render the bootstrapping code. A SortUnit is essentially a node in a directed graph and may have cycles.
 *
 * @author Mike Brock
 */
public class SortUnit implements Comparable<SortUnit> {

  private final MetaClass type;
  private final List<Object> items;
  private final Set<SortUnit> dependencies;

  protected SortUnit(MetaClass type, List<Object> items, Set<SortUnit> dependencies) {
    this.type = type.getErased();
    this.items = Collections.unmodifiableList(items);
    this.dependencies = Collections.unmodifiableSet(dependencies);
  }

  public static SortUnit create(MetaClass type, Collection<Object> items, Collection<SortUnit> dependencies) {
    return new SortUnit(type, new ArrayList<Object>(items), new HashSet<SortUnit>(dependencies));
  }

  /**
   * Returns the type which this sort unit represents.
   *
   * @return
   */
  public MetaClass getType() {
    return type;
  }

  /**
   * Returns the list of arbitrary items associated with this sort unit. There is no contract on what this should be,
   * although the order in which items are represented in the return List are guaranteed to be the same order in which
   * they were added.
   * <p>
   * Typically items are units of work used by the container to orchestrate the generation of code in the correct
   * order.
   *
   * @return an unmodifiable list of arbitrary items.
   */
  public List<Object> getItems() {
    return items;
  }

  /**
   * Returns a list of SortUnits which are depended on by this SortUnit.
   *
   * @return an unmodifiable set of SortUnits which are depended on by this SortUnit.
   */
  public Set<SortUnit> getDependencies() {
    return dependencies;
  }

  /**
   * Determines whether or not the specified SortUnit is a direct or indirect dependency of this SortUnit.
   *
   * @param unit the SortUnit to check against
   * @return true if the specified SortUnit is a direct or indirect dependency of this SortUnit.
   */
  public boolean hasDependency(final SortUnit unit) {
    return _hasDependency(new HashSet<String>(), this, unit);
  }

  private static boolean _hasDependency(final Set<String> visited, final SortUnit from, final SortUnit to) {
    String fromType = from.getType().getFullyQualifiedName();
    if (visited.contains(fromType)) {
      return false;
    }
    visited.add(fromType);

    if (!from.getDependencies().contains(to)) {
      for (SortUnit dep : from.getDependencies()) {
        if (_hasDependency(visited, dep, to)) return true;
      }
      return false;
    }
    else {
      return true;
    }
  }

  /**
   * Returns the outward graph depth of this SortUnit to the outermost leaf or cycle.
   *
   * @return the outward depth of the graph from this SortUnit.
   */
  public int getDepth() {
    int depth = 0;
    for (SortUnit su : getDependencies()) {
      if (su.equals(this)) continue;

      int d = _getDepth(new IdentityHashSet<SortUnit>(), this, 1, su);
      if (d > depth) {
        depth = d;
      }
    }
    return depth;
  }


  private static int _getDepth(Set<SortUnit> visited, SortUnit outer, int depth, SortUnit su) {
    if (visited.contains(su)) {
      return 0;
    }

    visited.add(su);

    for (SortUnit dep : su.getDependencies()) {
      if (dep.equals(outer)) continue;

      int d = _getDepth(visited, outer, depth + 1, dep);
      if (d > depth) {
        depth = d;
      }
    }
    return depth;

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SortUnit)) return false;

    SortUnit sortUnit = (SortUnit) o;

    return type != null && type.getFullyQualifiedName().equals(sortUnit.type.getFullyQualifiedName());
  }

  @Override
  public int compareTo(SortUnit o) {
    if (o.hasDependency(this) && hasDependency(o)) {
      return o.getDepth() - getDepth();
    }
    else {
      return 0;
    }
  }

  @Override
  public int hashCode() {
    return type != null ? type.getFullyQualifiedName().hashCode() * 37 : 0;
  }

  @Override
  public String toString() {
    return _toString(new HashSet<SortUnit>());
  }

  private String _toString(Set<SortUnit> visited) {
    return "(depth:" + getDepth() + ")" + type.toString()
            + " => " + _renderDependencyTree(visited, this);
  }


  private static String _renderDependencyTree(Set<SortUnit> visited, SortUnit visit) {
    if (visited.contains(visit)) {
      return "<CYCLE ON: " + visit.getType().getFullyQualifiedName() + ">";
    }
    visited.add(visit);

    StringBuilder sb = new StringBuilder("[");
    Iterator<SortUnit> iter = visit.getDependencies().iterator();
    while (iter.hasNext()) {
      sb.append(iter.next()._toString(visited));
      if (iter.hasNext()) {
        sb.append(", ");
      }
    }

    return sb.append("]").toString();
  }
}
