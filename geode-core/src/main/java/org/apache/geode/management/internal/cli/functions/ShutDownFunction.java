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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.Logger;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.distributed.DistributedSystem;
import org.apache.geode.internal.InternalEntity;
import org.apache.geode.internal.logging.LogService;
import org.apache.geode.internal.tcp.ConnectionTable;

/**
 * Class for Shutdown function
 */
public class ShutDownFunction implements Function, InternalEntity {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogService.getLogger();

  @Override
  public void execute(final FunctionContext context) {
    try {
      Cache cache = context.getCache();
      String memberName = cache.getDistributedSystem().getDistributedMember().getId();

      logger.info("Received GFSH shutdown. Shutting down member " + memberName);

      disconnectInNonDaemonThread(cache.getDistributedSystem());

      context.getResultSender().lastResult("SUCCESS: succeeded in shutting down " + memberName);

    } catch (Exception ex) {
      logger.warn("Error during shutdown", ex);
      context.getResultSender().lastResult("FAILURE: failed in shutting down " + ex.getMessage());
    }
  }

  /**
   * The shutdown is performed in a separate, non-daemon thread so that the JVM does not shut down
   * prematurely before the full process has completed.
   */
  private void disconnectInNonDaemonThread(final DistributedSystem system)
      throws InterruptedException, ExecutionException {
    ExecutorService exec = Executors.newSingleThreadExecutor();
    Future future = exec.submit(() -> {
      ConnectionTable.threadWantsSharedResources();
      if (system.isConnected()) {
        system.disconnect();
      }
    });
    try {
      future.get();
    } finally {
      exec.shutdown();
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
