/*
 * SonarQube Java Properties Analyzer
 * Copyright (C) 2015-2016 David RACODON
 * david.racodon@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.jproperties.tree.impl;

import com.google.common.collect.Iterators;
import org.sonar.plugins.jproperties.api.tree.PropertiesTree;
import org.sonar.plugins.jproperties.api.tree.PropertyTree;
import org.sonar.plugins.jproperties.api.tree.SyntaxToken;
import org.sonar.plugins.jproperties.api.tree.Tree;
import org.sonar.plugins.jproperties.api.visitors.DoubleDispatchVisitor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PropertiesTreeImpl extends JavaPropertiesTree implements PropertiesTree {

  private final SyntaxToken byteOrderMark;
  private final SyntaxToken eof;
  private final List<PropertyTree> properties;

  public PropertiesTreeImpl(@Nullable SyntaxToken byteOrderMark, @Nullable List<PropertyTree> properties, SyntaxToken eof) {
    this.byteOrderMark = byteOrderMark;
    this.eof = eof;
    if (properties != null) {
      this.properties = properties;
    } else {
      this.properties = new ArrayList<>();
    }
  }

  @Override
  public Kind getKind() {
    return Kind.PROPERTIES;
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.concat(
      Iterators.singletonIterator(byteOrderMark),
      properties.iterator(),
      Iterators.singletonIterator(eof));
  }

  @Override
  public boolean hasByteOrderMark() {
    return byteOrderMark != null;
  }

  @Override
  public List<PropertyTree> properties() {
    return properties;
  }

  @Override
  public void accept(DoubleDispatchVisitor visitor) {
    visitor.visitProperties(this);
  }

}
