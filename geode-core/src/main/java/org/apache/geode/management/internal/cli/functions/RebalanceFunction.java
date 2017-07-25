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

import java.util.Set;
import java.util.concurrent.CancellationException;

import org.apache.logging.log4j.Logger;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.control.RebalanceFactory;
import org.apache.geode.cache.control.RebalanceOperation;
import org.apache.geode.cache.control.RebalanceResults;
import org.apache.geode.cache.control.ResourceManager;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.partition.PartitionRebalanceInfo;
import org.apache.geode.internal.InternalEntity;
import org.apache.geode.internal.logging.LogService;

public class RebalanceFunction implements Function, InternalEntity {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogService.getLogger();

  @Override
  public void execute(final FunctionContext context) {
    Cache cache = context.getCache();
    ResourceManager manager = cache.getResourceManager();
    Object[] args = (Object[]) context.getArguments();
    String simulate = ((String) args[0]);
    Set<String> includeRegionNames = (Set<String>) args[1];
    Set<String> excludeRegionNames = (Set<String>) args[2];
    RebalanceFactory rbFactory = manager.createRebalanceFactory();
    rbFactory.excludeRegions(excludeRegionNames);
    rbFactory.includeRegions(includeRegionNames);
    RebalanceResults results = null;

    RebalanceOperation op;
    if (simulate.equals("true")) {
      op = rbFactory.simulate();
    } else {
      op = rbFactory.start();
    }

    try {
      results = op.getResults();
      logger.info("Starting RebalanceFunction got results = {}", results);
      StringBuilder sb = new StringBuilder();
      sb.append(results.getTotalBucketCreateBytes()).append(",")
          .append(results.getTotalBucketCreateTime()).append(",")
          .append(results.getTotalBucketCreatesCompleted()).append(",")
          .append(results.getTotalBucketTransferBytes()).append(",")
          .append(results.getTotalBucketTransferTime()).append(",")
          .append(results.getTotalBucketTransfersCompleted()).append(",")
          .append(results.getTotalPrimaryTransferTime()).append(",")
          .append(results.getTotalPrimaryTransfersCompleted()).append(",")
          .append(results.getTotalTime()).append(",");

      Set<PartitionRebalanceInfo> regns1 = results.getPartitionRebalanceDetails();
      for (PartitionRebalanceInfo rgn : regns1) {
        sb.append(rgn.getRegionPath()).append(",");
      }

      logger.info("Starting RebalanceFunction with {}", sb);
      context.getResultSender().lastResult(sb.toString());

    } catch (CancellationException e) {
      logger.info("Starting RebalanceFunction CancellationException: {}", e.getMessage(), e);
      context.getResultSender().lastResult("CancellationException1 " + e.getMessage());

    } catch (InterruptedException e) {
      logger.info("Starting RebalanceFunction InterruptedException: {}", e.getMessage(), e);
      context.getResultSender().lastResult("InterruptedException2 " + e.getMessage());
    }
  }

  @Override
  public boolean hasResult() {
    return true;
  }

  @Override
  public boolean optimizeForWrite() {
    // no need of optimization since read-only.
    return false;
  }

  @Override
  public boolean isHA() {
    return false;
  }

}
