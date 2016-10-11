/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geode.distributed;

import static java.util.concurrent.TimeUnit.*;
import static org.apache.geode.distributed.ConfigurationProperties.*;
import static org.assertj.core.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.core.ConditionFactory;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.TemporaryFolder;

import org.apache.geode.distributed.AbstractLauncher.Status;
import org.apache.geode.distributed.LocatorLauncher.Builder;
import org.apache.geode.distributed.internal.SharedConfiguration;
import org.apache.geode.internal.PropertiesResolver;
import org.apache.geode.internal.lang.StringUtils;
import org.apache.geode.internal.util.IOUtils;

public class LocatorLauncherPropertiesIntegrationTest {

  private File propsFile;

  private LocatorLauncher launcher;
  private String workingDirectory;
  private String clusterConfigDirectory;

  @Rule
  public RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void before() throws Exception {
    this.workingDirectory = this.temporaryFolder.getRoot().getCanonicalPath();
    this.clusterConfigDirectory = this.temporaryFolder.newFolder(SharedConfiguration.CLUSTER_CONFIG_DISK_DIR_PREFIX).getCanonicalPath();
  }

  @After
  public void after() throws Exception {
    if (this.launcher != null) {
      this.launcher.stop();
      this.launcher = null;
    }
    try {
      FileUtils.forceDelete(propsFile);
    } catch (Exception ignored) {
    }
  }

  @Test
  public void usesNameInGemFireProperties() throws Throwable {
    String memberName = "myLocatorName";
    createPropertiesFile("myGemfire.properties", memberName);

    this.launcher = new Builder().setPort(0).set(CLUSTER_CONFIGURATION_DIR, this.clusterConfigDirectory).build();
    this.launcher.start();
    awaitLocator(this.launcher);

    System.out.println(this.launcher.status());
    assertThat(this.launcher.status().getMemberName()).isEqualTo(memberName);

    assertThat(this.launcher.stop().getStatus()).isEqualTo(Status.STOPPED);
  }

  private void createPropertiesFile(String filename, String memberName) throws IOException {
    System.setProperty(PropertiesResolver.GEMFIRE_PROPERTIES_FILE_PROPERTY, filename);

    propsFile = new File(System.getProperty("user.home"), filename);
    propsFile.deleteOnExit();

    Properties properties = new Properties();
    properties.setProperty(NAME, memberName);
    properties.store(new FileOutputStream(propsFile), null);
  }

  private void awaitLocator(LocatorLauncher launcher) throws Exception {
    await().until(() -> assertThat(Status.ONLINE.equals(launcher.status().getStatus())).isTrue());
  }

  private void awaitLocator(int port) throws Exception {
    awaitLocator(new Builder().setPort(port).build());
  }

  private ConditionFactory await() {
    return Awaitility.await().atMost(10, MINUTES);
  }

  private int readPid(final File pidFile) throws IOException {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(pidFile));
      return Integer.parseInt(StringUtils.trim(reader.readLine()));
    } finally {
      IOUtils.close(reader);
    }
  }
}
