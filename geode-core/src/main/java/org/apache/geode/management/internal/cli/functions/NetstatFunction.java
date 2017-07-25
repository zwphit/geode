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
 */
package org.apache.geode.management.internal.cli.functions;

import static org.apache.geode.internal.lang.SystemUtils.getOsArchitecture;
import static org.apache.geode.internal.lang.SystemUtils.getOsName;
import static org.apache.geode.internal.lang.SystemUtils.getOsVersion;
import static org.apache.geode.internal.lang.SystemUtils.isLinux;
import static org.apache.geode.internal.lang.SystemUtils.isMacOSX;
import static org.apache.geode.internal.lang.SystemUtils.isSolaris;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.distributed.DistributedSystem;
import org.apache.geode.internal.InternalEntity;
import org.apache.geode.internal.logging.LogService;
import org.apache.geode.management.internal.cli.CliUtil;
import org.apache.geode.management.internal.cli.CliUtil.DeflaterInflaterData;
import org.apache.geode.management.internal.cli.GfshParser;
import org.apache.geode.management.internal.cli.i18n.CliStrings;

/**
 * Executes 'netstat' OS command & returns the result as compressed bytes.
 * 
 * @since GemFire 7.0
 */
public class NetstatFunction implements Function, InternalEntity {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogService.getLogger();

  private static final String NETSTAT_COMMAND = "netstat";
  private static final String LSOF_COMMAND = "lsof";

  @Override
  public void execute(final FunctionContext context) {
    DistributedSystem ds = context.getCache().getDistributedSystem();
    if (ds == null || !ds.isConnected()) {
      return;
    }

    String host = ds.getDistributedMember().getHost();
    NetstatFunctionArgument args = (NetstatFunctionArgument) context.getArguments();
    boolean withLsof = args.isWithLsof();
    String lineSeparator = args.getLineSeparator();

    String netstatOutput = executeCommand(lineSeparator, withLsof);

    StringBuilder netstatInfo = new StringBuilder();

    // {0} will be replaced on Manager
    addMemberHostHeader(netstatInfo, "{0}", host, lineSeparator);

    NetstatFunctionResult result = new NetstatFunctionResult(host, netstatInfo.toString(),
        CliUtil.compressBytes(netstatOutput.getBytes()));

    context.getResultSender().lastResult(result);
  }

  @Override
  public boolean hasResult() {
    return true;
  }

  @Override
  public boolean optimizeForWrite() {
    return false;
  }

  @Override
  public boolean isHA() {
    return false;
  }

  private static void addMemberHostHeader(final StringBuilder netstatInfo, final String id,
      final String host, final String lineSeparator) {

    String osInfo = getOsName() + " " + getOsVersion() + " " + getOsArchitecture();

    StringBuilder memberPlatFormInfo = new StringBuilder();
    memberPlatFormInfo.append(CliStrings.format(CliStrings.NETSTAT__MSG__FOR_HOST_1_OS_2_MEMBER_0,
        id, host, osInfo, lineSeparator));

    int nameIdLength = Math.max(Math.max(id.length(), host.length()), osInfo.length()) * 2;

    StringBuilder netstatInfoBottom = new StringBuilder();
    for (int i = 0; i < nameIdLength; i++) {
      netstatInfo.append("#");
      netstatInfoBottom.append("#");
    }

    netstatInfo.append(lineSeparator).append(memberPlatFormInfo.toString()).append(lineSeparator)
        .append(netstatInfoBottom.toString()).append(lineSeparator);
  }

  private static void addNetstatDefaultOptions(final List<String> cmdOptionsList) {
    if (isLinux()) {
      cmdOptionsList.add("-v");
      cmdOptionsList.add("-a");
      cmdOptionsList.add("-e");
    } else {
      cmdOptionsList.add("-v");
      cmdOptionsList.add("-a");
    }
  }

  private static void executeNetstat(final StringBuilder netstatInfo, final String lineSeparator) {
    List<String> cmdOptionsList = new ArrayList<>();
    cmdOptionsList.add(NETSTAT_COMMAND);
    addNetstatDefaultOptions(cmdOptionsList);

    if (logger.isDebugEnabled()) {
      logger.debug("NetstatFunction executing {}", cmdOptionsList);
    }

    ProcessBuilder processBuilder = new ProcessBuilder(cmdOptionsList);
    Process netstat = null;
    try {
      netstat = processBuilder.start();
      InputStream is = netstat.getInputStream();
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
      String line;

      while ((line = bufferedReader.readLine()) != null) {
        netstatInfo.append(line).append(lineSeparator);
      }

    } catch (IOException e) {
      // TODO: change this to keep the full stack trace
      netstatInfo.append(CliStrings.format(CliStrings.NETSTAT__MSG__COULD_NOT_EXECUTE_0_REASON_1,
          NETSTAT_COMMAND, e.getMessage()));

    } finally {
      if (netstat != null) {
        netstat.destroy();
      }

      netstatInfo.append(lineSeparator); // additional new line
    }
  }

  private static void executeLsof(final StringBuilder existingNetstatInfo,
      final String lineSeparator) {
    existingNetstatInfo.append("################ ").append(LSOF_COMMAND)
        .append(" output ###################").append(lineSeparator);

    if (isLinux() || isMacOSX() || isSolaris()) {
      ProcessBuilder procBuilder = new ProcessBuilder(LSOF_COMMAND);
      Process lsof = null;
      try {
        lsof = procBuilder.start();
        InputStreamReader reader = new InputStreamReader(lsof.getInputStream());
        BufferedReader breader = new BufferedReader(reader);
        String line = "";

        while ((line = breader.readLine()) != null) {
          existingNetstatInfo.append(line).append(lineSeparator);
        }

      } catch (IOException e) {
        // TODO: change this to keep the full stack trace
        String message = e.getMessage();
        if (message.contains("error=2, No such file or directory")) {
          existingNetstatInfo
              .append(CliStrings.format(CliStrings.NETSTAT__MSG__COULD_NOT_EXECUTE_0_REASON_1,
                  LSOF_COMMAND, CliStrings.NETSTAT__MSG__LSOF_NOT_IN_PATH));
        } else {
          existingNetstatInfo.append(CliStrings.format(
              CliStrings.NETSTAT__MSG__COULD_NOT_EXECUTE_0_REASON_1, LSOF_COMMAND, e.getMessage()));
        }

      } finally {
        if (lsof != null) {
          lsof.destroy();
        }

        existingNetstatInfo.append(lineSeparator); // additional new line
      }

    } else {
      existingNetstatInfo.append(CliStrings.NETSTAT__MSG__NOT_AVAILABLE_FOR_WINDOWS)
          .append(lineSeparator);
    }
  }

  private static String executeCommand(final String lineSeparator, final boolean withlsof) {
    StringBuilder netstatInfo = new StringBuilder();

    executeNetstat(netstatInfo, lineSeparator);

    if (withlsof) {
      executeLsof(netstatInfo, lineSeparator);
    }

    return netstatInfo.toString();
  }

  /**
   * Java main, probably for manual testing?
   */
  public static void main(final String[] args) {
    String netstat = executeCommand(GfshParser.LINE_SEPARATOR, true);
    System.out.println(netstat);
  }

  /**
   * Argument for NetstatFunction.
   */
  public static class NetstatFunctionArgument implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String lineSeparator;
    private final boolean withLsof;

    public NetstatFunctionArgument(final String lineSeparator, final boolean withLsof) {
      this.lineSeparator = lineSeparator;
      this.withLsof = withLsof;
    }

    public String getLineSeparator() {
      return lineSeparator;
    }

    public boolean isWithLsof() {
      return withLsof;
    }
  }

  /**
   * Result of executing NetstatFunction.
   */
  public static class NetstatFunctionResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String host;
    private final String headerInfo;
    private final DeflaterInflaterData compressedBytes;

    protected NetstatFunctionResult(final String host, final String headerInfo,
        final DeflaterInflaterData compressedBytes) {
      this.host = host;
      this.headerInfo = headerInfo;
      this.compressedBytes = compressedBytes;
    }

    public String getHost() {
      return host;
    }

    public String getHeaderInfo() {
      return headerInfo;
    }

    public DeflaterInflaterData getCompressedBytes() {
      return compressedBytes;
    }
  }

}
