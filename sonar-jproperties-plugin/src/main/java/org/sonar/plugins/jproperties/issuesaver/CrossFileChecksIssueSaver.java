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
package org.sonar.plugins.jproperties.issuesaver;

import com.google.common.collect.ImmutableList;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.sonar.plugins.jproperties.issuesaver.crossfile.CrossFileCheckIssueSaver;
import org.sonar.plugins.jproperties.issuesaver.crossfile.DuplicatedKeysAcrossFilesCheckIssueSaver;
import org.sonar.plugins.jproperties.issuesaver.crossfile.MissingTranslationsCheckIssueSaver;
import org.sonar.plugins.jproperties.issuesaver.crossfile.MissingTranslationsInDefaultCheckIssueSaver;

public class CrossFileChecksIssueSaver {

  private CrossFileChecksIssueSaver() {
  }

  private static Collection<Class<? extends CrossFileCheckIssueSaver>> getCrossFileCheckIssueSavers() {
    return ImmutableList.of(
      DuplicatedKeysAcrossFilesCheckIssueSaver.class,
      MissingTranslationsCheckIssueSaver.class,
      MissingTranslationsInDefaultCheckIssueSaver.class);
  }

  public static void saveIssues(IssueSaver issueSaver) {
    getCrossFileCheckIssueSavers().forEach(c -> saveIssuesOnCheck(c, issueSaver));
  }

  private static void saveIssuesOnCheck(Class<? extends CrossFileCheckIssueSaver> clazz, IssueSaver issueSaver) {
    try {
      clazz.getConstructor(IssueSaver.class).newInstance(issueSaver).saveIssues();
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException("Cannot save issues on check " + clazz.getName(), e);
    }
  }

}
