/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.apache.geode.management.internal.cli.commands;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.io.FileUtils;
import org.apache.geode.cache.Region;
import org.apache.geode.distributed.ConfigurationProperties;
import org.apache.geode.internal.logging.LogService;
import org.apache.geode.management.internal.cli.functions.ExportLogsFunction;
import org.apache.geode.management.internal.cli.result.CommandResult;
import org.apache.geode.management.internal.configuration.utils.ZipUtils;
import org.apache.geode.test.dunit.IgnoredException;
import org.apache.geode.test.dunit.rules.GfshShellConnectionRule;
import org.apache.geode.test.dunit.rules.Locator;
import org.apache.geode.test.dunit.rules.LocatorServerStartupRule;
import org.apache.geode.test.dunit.rules.Member;
import org.apache.geode.test.dunit.rules.Server;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;


public class ExportLogsDUnit {

  private static final String ERROR_LOG_PREFIX = "[IGNORE]";

  @Rule
  public LocatorServerStartupRule lsRule = new LocatorServerStartupRule();

  @Rule
  public GfshShellConnectionRule gfshConnector = new GfshShellConnectionRule();

  private Locator locator;
  private Server server1;
  private Server server2;

  Map<Member, List<LogLine>> expectedMessages;

  @Before
  public void setup() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(ConfigurationProperties.LOG_LEVEL, "debug");

    locator = lsRule.startLocatorVM(0, properties);
    server1 = lsRule.startServerVM(1, properties, locator.getPort());
    server2 = lsRule.startServerVM(2, properties, locator.getPort());

    IgnoredException.addIgnoredException(ERROR_LOG_PREFIX);

    expectedMessages = new HashMap<>();
    expectedMessages.put(locator, listOfLogLines(locator.getName(), "info", "error", "debug"));
    expectedMessages.put(server1, listOfLogLines(server1.getName(), "info", "error", "debug"));
    expectedMessages.put(server2, listOfLogLines(server2.getName(), "info", "error", "debug"));

    // log the messages in each of the members
    for (Member member : expectedMessages.keySet()) {
      List<LogLine> logLines = expectedMessages.get(member);

      member.invoke(() -> {
        Logger logger = LogService.getLogger();
        logLines.forEach((LogLine logLine) -> logLine.writeLog(logger));
      });
    }

    gfshConnector.connectAndVerify(locator);
  }

  @Test
  public void testExportWithThresholdLogLevelFilter() throws Exception {

    CommandResult result = gfshConnector.executeAndVerifyCommand(
        "export logs --log-level=info --only-log-level=false --dir=" + lsRule.getTempFolder()
            .getRoot().getCanonicalPath());

    File unzippedLogFileDir = unzipExportedLogs();
    Set<String> acceptedLogLevels = Stream.of("info", "error").collect(toSet());
    verifyZipFileContents(unzippedLogFileDir, acceptedLogLevels);

  }


  @Test
  public void testExportWithExactLogLevelFilter() throws Exception {
    CommandResult result = gfshConnector.executeAndVerifyCommand(
        "export logs --log-level=info --only-log-level=true --dir=" + lsRule.getTempFolder()
            .getRoot().getCanonicalPath());

    File unzippedLogFileDir = unzipExportedLogs();

    Set<String> acceptedLogLevels = Stream.of("info").collect(toSet());
    verifyZipFileContents(unzippedLogFileDir, acceptedLogLevels);
  }

  @Test
  public void testExportWithNoFilters() throws Exception {
    CommandResult result = gfshConnector.executeAndVerifyCommand(
        "export logs  --dir=" + "someDir" /*  lsRule.getTempFolder().getRoot().getCanonicalPath() */);

    File unzippedLogFileDir = unzipExportedLogs();
    Set<String> acceptedLogLevels = Stream.of("info", "error", "debug").collect(toSet());
    verifyZipFileContents(unzippedLogFileDir, acceptedLogLevels);

    // Ensure export logs region does not accumulate data
    server1.invoke(() -> {
      Region exportLogsRegion = ExportLogsFunction.createOrGetExistingExportLogsRegion();
      assertThat(exportLogsRegion.size()).isEqualTo(0);
    });
    server2.invoke(() -> {
      Region exportLogsRegion = ExportLogsFunction.createOrGetExistingExportLogsRegion();
      assertThat(exportLogsRegion.size()).isEqualTo(0);
    });
    locator.invoke(() -> {
      Region exportLogsRegion = ExportLogsFunction.createOrGetExistingExportLogsRegion();
      assertThat(exportLogsRegion.size()).isEqualTo(0);
    });
  }


  public void verifyZipFileContents(File unzippedLogFileDir, Set<String> acceptedLogLevels)
      throws IOException {
    Set<File> dirsFromZipFile =
        Stream.of(unzippedLogFileDir.listFiles()).filter(File::isDirectory).collect(toSet());
    assertThat(dirsFromZipFile).hasSize(expectedMessages.keySet().size());

    Set<String> expectedDirNames =
        expectedMessages.keySet().stream().map(Member::getName).collect(toSet());
    Set<String> actualDirNames = dirsFromZipFile.stream().map(File::getName).collect(toSet());
    assertThat(actualDirNames).isEqualTo(expectedDirNames);

    System.out.println("Unzipped artifacts:");
    for (File dir : dirsFromZipFile) {
      verifyLogFileContents(acceptedLogLevels, dir);
    }
  }

  public void verifyLogFileContents(Set<String> acceptedLogLevels, File dirForMember)
      throws IOException {

    String memberName = dirForMember.getName();
    Member member = expectedMessages.keySet().stream()
        .filter((Member aMember) -> aMember.getName().equals(memberName))
        .findFirst()
        .get();

    assertThat(member).isNotNull();

    Set<String> fileNamesInDir =
        Stream.of(dirForMember.listFiles()).map(File::getName).collect(toSet());

    System.out.println(dirForMember.getCanonicalPath() + " : " + fileNamesInDir);

    File logFileForMember = new File(dirForMember, memberName + ".log");
    assertThat(logFileForMember).exists();
    assertThat(fileNamesInDir).hasSize(1);

    String logFileContents =
        FileUtils.readLines(logFileForMember, Charset.defaultCharset()).stream()
            .collect(joining("\n"));

    for (LogLine logLine : expectedMessages.get(member)) {
      boolean shouldExpectLogLine = acceptedLogLevels.contains(logLine.level);

      if (shouldExpectLogLine) {
        assertThat(logFileContents).contains(logLine.getMessage());
      } else {
        assertThat(logFileContents).doesNotContain(logLine.getMessage());
      }
    }

  }

  private File unzipExportedLogs() throws IOException {
    File locatorWorkingDir = locator.getWorkingDir();
    List<File> filesInDir = Stream.of(locatorWorkingDir.listFiles()).collect(toList());
    assertThat(filesInDir).isNotEmpty();


    List<File> zipFilesInDir = Stream.of(locatorWorkingDir.listFiles())
        .filter(f -> f.getName().endsWith(".zip")).collect(toList());
    assertThat(zipFilesInDir).describedAs(filesInDir.stream().map(File::getAbsolutePath).collect(joining(","))).hasSize(1);

    File unzippedLogFileDir = lsRule.getTempFolder().newFolder("unzippedLogs");
    ZipUtils.unzip(zipFilesInDir.get(0).getCanonicalPath(), unzippedLogFileDir.getCanonicalPath());
    return unzippedLogFileDir;
  }

  private List<LogLine> listOfLogLines(String memberName, String... levels) {
    return Stream.of(levels).map(level -> new LogLine(level, memberName)).collect(toList());
  }


  public static class LogLine implements Serializable {
    String level;
    String message;

    public LogLine(String level, String memberName) {
      this.level = level;
      this.message = buildMessage(memberName);
    }

    public String getMessage() {
      return message;
    }

    private String buildMessage(String memberName) {
      StringBuilder stringBuilder = new StringBuilder();
      if (Objects.equals(level, "error")) {
        stringBuilder.append(ERROR_LOG_PREFIX);
      }
      stringBuilder.append(level);

      return stringBuilder.append(memberName).toString();
    }


    public void writeLog(Logger logger) {
      switch (this.level) {
        case "info":
          logger.info(getMessage());
          break;
        case "error":
          logger.error(getMessage());
          break;
        case "debug":
          logger.debug(getMessage());
      }
    }
  }
}
