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
package org.sonar.jproperties.checks.verifier;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.sonar.jproperties.parser.JavaPropertiesParserBuilder;
import org.sonar.jproperties.visitors.CharsetAwareVisitor;
import org.sonar.jproperties.visitors.JavaPropertiesVisitorContext;
import org.sonar.plugins.jproperties.api.JavaPropertiesCheck;
import org.sonar.plugins.jproperties.api.tree.PropertiesTree;
import org.sonar.plugins.jproperties.api.tree.SyntaxToken;
import org.sonar.plugins.jproperties.api.tree.SyntaxTrivia;
import org.sonar.plugins.jproperties.api.tree.Tree;
import org.sonar.plugins.jproperties.api.visitors.SubscriptionVisitorCheck;
import org.sonar.plugins.jproperties.api.visitors.issue.*;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * To unit test checks.
 */
public class JavaPropertiesCheckVerifier extends SubscriptionVisitorCheck {

  private final List<TestIssue> expectedIssues = new ArrayList<>();

  /**
   * Check issues
   * @param check Check to test
   * @param file File to test
   *
   * Example:
   * <pre>
   * JavaPropertiesCheckVerifier.issues(new MyCheck(), myFile)
   *    .next().atLine(2).withMessage("This is message for line 2.")
   *    .next().atLine(3).withMessage("This is message for line 3.").withCost(2.)
   *    .next().atLine(8)
   *    .noMore();
   * </pre>
   */
  public static CheckMessagesVerifier issues(JavaPropertiesCheck check, File file) {
    return issues(check, file, Charsets.ISO_8859_1);
  }

  /**
   * See {@link JavaPropertiesCheckVerifier#issues(JavaPropertiesCheck, File)}
   * @param charset Charset of the file to test.
   */
  public static CheckMessagesVerifier issues(JavaPropertiesCheck check, File file, Charset charset) {
    if (check instanceof CharsetAwareVisitor) {
      ((CharsetAwareVisitor) check).setCharset(charset);
    }
    return CheckMessagesVerifier.verify(TreeCheckTest.getIssues(file.getAbsolutePath(), check, charset));
  }

  /**
   * To unit tests checks. Expected issues should be provided as comments in the source file.
   * Expected issue details should be provided the line prior to the actual issue.
   * For example:
   * <pre>
   * # Noncompliant {{Error message for the issue on the next line}}
   * MyProperty=abc
   *
   * # Noncompliant [[sc=2;ec=6;secondary=+2,+4]] {{Error message}}
   * ...
   * </pre>
   *
   * How to write these comments:
   * <ul>
   *   <li>Put a comment starting with "Noncompliant" if you expect an issue on the next line.</li>
   *   <li>Optional - In double brackets provide the precise issue location <code>sl, sc, ec, el</code> keywords respectively for start line, start column, end column and end line. <code>sl=+1</code> by default.</li>
   *   <li>Optional - In double brackets provide secondary locations with the <code>secondary</code> keyword.</li>
   *   <li>Optional - In double brackets provide expected effort to fix (cost) with the <code>effortToFix</code> keyword.</li>
   *   <li>Optional - In double curly braces <code>{{MESSAGE}}</code> provide the expected message.</li>
   *   <li>To specify the line you can use relative location by putting <code>+</code> or <code>-</code>.</li>
   *   <li>Note that the order matters: Noncompliant => Parameters => Error message</li>
   * </ul>
   *
   * Example of call:
   * <pre>
   * JavaPropertiesCheckVerifier.verify(new MyCheck(), myFile));
   * </pre>
   */
  public static void verify(JavaPropertiesCheck check, File file) {
    verify(check, file, Charsets.ISO_8859_1);
  }

  /**
   * See {@link JavaPropertiesCheckVerifier#verify(JavaPropertiesCheck, File)}
   * @param charset Charset of the file to test.
   */
  public static void verify(JavaPropertiesCheck check, File file, Charset charset) {
    PropertiesTree propertiesTree = (PropertiesTree) JavaPropertiesParserBuilder.createParser(charset).parse(file);
    JavaPropertiesVisitorContext context = new JavaPropertiesVisitorContext(propertiesTree, file);

    JavaPropertiesCheckVerifier checkVerifier = new JavaPropertiesCheckVerifier();
    checkVerifier.scanFile(context);

    List<TestIssue> expectedIssues = checkVerifier.expectedIssues
      .stream()
      .sorted((i1, i2) -> Integer.compare(i1.line(), i2.line()))
      .collect(Collectors.toList());

    if (check instanceof CharsetAwareVisitor) {
      ((CharsetAwareVisitor) check).setCharset(charset);
    }
    Iterator<Issue> actualIssues = getActualIssues(check, context);

    for (TestIssue expected : expectedIssues) {
      if (actualIssues.hasNext()) {
        verifyIssue(expected, actualIssues.next());
      } else {
        throw new AssertionError("Missing issue at line " + expected.line());
      }
    }

    if (actualIssues.hasNext()) {
      Issue issue = actualIssues.next();
      throw new AssertionError("Unexpected issue at line " + line(issue) + ": \"" + message(issue) + "\"");
    }
  }

  private static Iterator<Issue> getActualIssues(JavaPropertiesCheck check, JavaPropertiesVisitorContext context) {
    List<Issue> issues = check.scanFile(context);
    List<Issue> sortedIssues = Ordering.natural().onResultOf(new IssueToLine()).sortedCopy(issues);
    return sortedIssues.iterator();
  }

  private static void verifyIssue(TestIssue expected, Issue actual) {
    if (line(actual) > expected.line()) {
      fail("Missing issue at line " + expected.line());
    }
    if (line(actual) < expected.line()) {
      fail("Unexpected issue at line " + line(actual) + ": \"" + message(actual) + "\"");
    }
    if (expected.message() != null) {
      assertThat(message(actual)).as("Bad message at line " + expected.line()).isEqualTo(expected.message());
    }
    if (expected.effortToFix() != null) {
      assertThat(actual.cost()).as("Bad effortToFix at line " + expected.line()).isEqualTo(expected.effortToFix());
    }
    if (expected.startColumn() != null) {
      assertThat(((PreciseIssue) actual).primaryLocation().startLineOffset() + 1).as("Bad start column at line " + expected.line()).isEqualTo(expected.startColumn());
    }
    if (expected.endColumn() != null) {
      assertThat(((PreciseIssue) actual).primaryLocation().endLineOffset() + 1).as("Bad end column at line " + expected.line()).isEqualTo(expected.endColumn());
    }
    if (expected.endLine() != null) {
      assertThat(((PreciseIssue) actual).primaryLocation().endLine()).as("Bad end line at line " + expected.line()).isEqualTo(expected.endLine());
    }
    if (expected.secondaryLines() != null) {
      assertThat(secondary(actual)).as("Bad secondary locations at line " + expected.line()).isEqualTo(expected.secondaryLines());
    }
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.TOKEN);
  }

  @Override
  public void visitNode(Tree tree) {
    SyntaxToken token = (SyntaxToken) tree;
    for (SyntaxTrivia trivia : token.trivias()) {

      String text = trivia.text().substring(2).trim();
      String marker = "Noncompliant";

      if (text.startsWith(marker)) {
        TestIssue issue = issue(null, trivia.line() + 1);
        String paramsAndMessage = text.substring(marker.length()).trim();
        if (paramsAndMessage.startsWith("[[")) {
          int endIndex = paramsAndMessage.indexOf("]]");
          addParams(issue, paramsAndMessage.substring(2, endIndex));
          paramsAndMessage = paramsAndMessage.substring(endIndex + 2).trim();
        }
        if (paramsAndMessage.startsWith("{{")) {
          int endIndex = paramsAndMessage.indexOf("}}");
          String message = paramsAndMessage.substring(2, endIndex);
          issue.message(message);
        }
        expectedIssues.add(issue);
      }
    }
  }

  private static void addParams(TestIssue issue, String params) {
    for (String param : Splitter.on(';').split(params)) {
      int equalIndex = param.indexOf('=');
      if (equalIndex == -1) {
        throw new IllegalStateException("Invalid param at line 1: " + param);
      }
      String name = param.substring(0, equalIndex);
      String value = param.substring(equalIndex + 1);

      if ("effortToFix".equalsIgnoreCase(name)) {
        issue.effortToFix(Integer.valueOf(value));

      } else if ("sc".equalsIgnoreCase(name)) {
        issue.startColumn(Integer.valueOf(value));

      } else if ("sl".equalsIgnoreCase(name)) {
        issue.startLine(lineValue(issue.line() - 1, value));

      } else if ("ec".equalsIgnoreCase(name)) {
        issue.endColumn(Integer.valueOf(value));

      } else if ("el".equalsIgnoreCase(name)) {
        issue.endLine(lineValue(issue.line(), value));

      } else if ("secondary".equalsIgnoreCase(name)) {
        addSecondaryLines(issue, value);

      } else {
        throw new IllegalStateException("Invalid param at line 1: " + name);
      }
    }
  }

  private static void addSecondaryLines(TestIssue issue, String value) {
    List<Integer> secondaryLines = new ArrayList<>();
    if (!"".equals(value)) {
      for (String secondary : Splitter.on(',').split(value)) {
        secondaryLines.add(lineValue(issue.line(), secondary));
      }
    }
    issue.secondary(secondaryLines);
  }

  private static int lineValue(int baseLine, String shift) {
    if (shift.startsWith("+")) {
      return baseLine + Integer.valueOf(shift.substring(1));
    }
    if (shift.startsWith("-")) {
      return baseLine - Integer.valueOf(shift.substring(1));
    }
    return Integer.valueOf(shift);
  }

  private static TestIssue issue(@Nullable String message, int lineNumber) {
    return TestIssue.create(message, lineNumber);
  }

  private static class IssueToLine implements Function<Issue, Integer> {
    @Override
    public Integer apply(Issue issue) {
      return line(issue);
    }
  }

  private static int line(Issue issue) {
    if (issue instanceof PreciseIssue) {
      return ((PreciseIssue) issue).primaryLocation().startLine();
    } else if (issue instanceof FileIssue) {
      return 0;
    } else if (issue instanceof LineIssue) {
      return ((LineIssue) issue).line();
    } else {
      throw new IllegalStateException("Unknown type of issue.");
    }
  }

  private static String message(Issue issue) {
    if (issue instanceof PreciseIssue) {
      return ((PreciseIssue) issue).primaryLocation().message();
    } else if (issue instanceof FileIssue) {
      return ((FileIssue) issue).message();
    } else if (issue instanceof LineIssue) {
      return ((LineIssue) issue).message();
    } else {
      throw new IllegalStateException("Unknown type of issue.");
    }
  }

  private static List<Integer> secondary(Issue issue) {
    List<Integer> result = new ArrayList<>();

    if (issue instanceof PreciseIssue) {
      for (IssueLocation issueLocation : ((PreciseIssue) issue).secondaryLocations()) {
        result.add(issueLocation.startLine());
      }
    } else if (issue instanceof FileIssue) {
      for (IssueLocation issueLocation : ((FileIssue) issue).secondaryLocations()) {
        result.add(issueLocation.startLine());
      }
    }
    return Ordering.natural().sortedCopy(result);
  }

}
