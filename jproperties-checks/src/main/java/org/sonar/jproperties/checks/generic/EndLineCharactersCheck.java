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
package org.sonar.jproperties.checks.generic;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Files;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.jproperties.checks.CheckUtils;
import org.sonar.jproperties.checks.Tags;
import org.sonar.jproperties.visitors.CharsetAwareVisitor;
import org.sonar.plugins.jproperties.api.tree.PropertiesTree;
import org.sonar.plugins.jproperties.api.visitors.DoubleDispatchVisitorCheck;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;

@Rule(
  key = "end-line-characters",
  name = "End-line characters should be consistent",
  priority = Priority.MINOR,
  tags = {Tags.CONVENTION})
@SqaleConstantRemediation("5min")
public class EndLineCharactersCheck extends DoubleDispatchVisitorCheck implements CharsetAwareVisitor {

  private static final String DEFAULT_FORMAT = "LF";
  private Charset charset;

  @RuleProperty(
    key = "End-line character",
    description = "Allowed values: 'CR', 'CRLF', 'LF'",
    defaultValue = DEFAULT_FORMAT)
  private String endLineCharacters = DEFAULT_FORMAT;

  @Override
  public void visitProperties(PropertiesTree tree) {
    if (fileContainsIllegalEndLineCharacters()) {
      addFileIssue("Set all end-line characters to '" + endLineCharacters + "' in this file.");
    }
    super.visitProperties(tree);
  }

  @Override
  public void validateParameters() {
    if (!Arrays.asList("CRLF", "CR", "LF").contains(endLineCharacters)) {
      throw new IllegalStateException(paramErrorMessage());
    }
  }

  @Override
  public void setCharset(Charset charset) {
    this.charset = charset;
  }

  @VisibleForTesting
  void setEndLineCharacters(String endLineCharacters) {
    this.endLineCharacters = endLineCharacters;
  }

  private boolean fileContainsIllegalEndLineCharacters() {
    try {
      String fileContent = Files.toString(getContext().getFile(), charset);
      return "CR".equals(endLineCharacters) && Pattern.compile("(?s)\n").matcher(fileContent).find()
        || "LF".equals(endLineCharacters) && Pattern.compile("(?s)\r").matcher(fileContent).find()
        || "CRLF".equals(endLineCharacters) && Pattern.compile("(?s)(\r(?!\n)|(?<!\r)\n)").matcher(fileContent).find();
    } catch (IOException e) {
      throw new IllegalStateException("Check jproperties:" + this.getClass().getAnnotation(Rule.class).key() + ": File cannot be read.", e);
    }
  }

  private String paramErrorMessage() {
    return CheckUtils.paramErrorMessage(
        this.getClass(),
        "endLineCharacters parameter is not valid.\nActual: '" + endLineCharacters + "'\nExpected: 'CR' or 'CRLF' or 'LF'");
  }

}
