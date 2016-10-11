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
package org.apache.geode.internal;

import static org.apache.geode.internal.PropertiesResolver.*;
import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import org.apache.geode.test.junit.categories.IntegrationTest;

@Category(IntegrationTest.class)
public class PropertiesResolverIntegrationTest {

  private File geodeCustomProperties;
  private File gemfireCustomProperties;

  List<File> files = new ArrayList<>();


  @Rule
  public RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void createFiles() throws Exception {
    this.geodeCustomProperties = this.temporaryFolder.newFile("geodeCustom.properties");
    this.gemfireCustomProperties = this.temporaryFolder.newFile("gemfireCustom.properties");
  }

  @After
  public void cleanup() throws Exception {
    for (File file: files) {
      FileUtils.forceDelete(file);
    }
  }

  @Test
  public void canSpecifyGeodePropertiesFileAbsolutePath() throws Exception {
    System.setProperty(GEODE_PROPERTIES_FILE_PROPERTY, this.geodeCustomProperties.getCanonicalPath());
    assertThat(findPropertiesFileLocation()).isEqualTo(this.geodeCustomProperties.getCanonicalFile().toURI());
  }

  @Test
  public void canSpecifyGeodePropertiesFileInCurrentDir() throws Exception {
    System.setProperty(GEODE_PROPERTIES_FILE_PROPERTY, geodeCustomFileInCurrentDir().getName());
    assertThat(findPropertiesFileLocation()).isEqualTo(geodeCustomFileInCurrentDir().getCanonicalFile().toURI());
  }

 @Test
  public void canSpecifyGeodePropertiesFileInUserHomeDir() throws Exception {
    System.setProperty(GEODE_PROPERTIES_FILE_PROPERTY, geodeCustomFileInHomeDir().getName());
    assertThat(findPropertiesFileLocation()).isEqualTo(geodeCustomFileInHomeDir().getCanonicalFile().toURI());
  }

  @Test
  public void canSpecifyGemFirePropertiesFileAbsolutePath() throws Exception {
    System.setProperty(GEMFIRE_PROPERTIES_FILE_PROPERTY, this.gemfireCustomProperties.getCanonicalPath());
    assertThat(findPropertiesFileLocation()).isEqualTo(this.gemfireCustomProperties.getCanonicalFile().toURI());
  }

  @Test
  public void canSpecifyGemFirePropertiesFileInCurrentDir() throws Exception {
    System.setProperty(GEMFIRE_PROPERTIES_FILE_PROPERTY, gemfireCustomFileInCurrentDir().getName());
    assertThat(findPropertiesFileLocation()).isEqualTo(gemfireCustomFileInCurrentDir().getCanonicalFile().toURI());
  }

  @Test
  public void canSpecifyGemFirePropertiesFileInUserHomeDir() throws Exception {
    System.setProperty(GEMFIRE_PROPERTIES_FILE_PROPERTY, gemfireCustomFileInHomeDir().getName());
    assertThat(findPropertiesFileLocation()).isEqualTo(gemfireCustomFileInHomeDir().getCanonicalFile().toURI());
  }

  @Test
  public void findPrefersGeodePropertiesFileFirst() throws Exception {
    System.setProperty(GEODE_PROPERTIES_FILE_PROPERTY, this.geodeCustomProperties.getCanonicalPath());
    System.setProperty(GEMFIRE_PROPERTIES_FILE_PROPERTY, this.gemfireCustomProperties.getCanonicalPath());
    geodeDefaultFileInCurrentDir();
    gemfireDefaultFileInCurrentDir();

    assertThat(findPropertiesFileLocation()).isEqualTo(this.geodeCustomProperties.getCanonicalFile().toURI());
  }

  @Test
  public void findPrefersGemFirePropertiesFileSecond() throws Exception {
    System.setProperty(GEMFIRE_PROPERTIES_FILE_PROPERTY, this.gemfireCustomProperties.getCanonicalPath());
    geodeDefaultFileInCurrentDir();
    gemfireDefaultFileInCurrentDir();

    assertThat(findPropertiesFileLocation()).isEqualTo(this.gemfireCustomProperties.getCanonicalFile().toURI());
  }

  @Test
  public void findPrefersGeodeDefaultThird() throws Exception {
    geodeDefaultFileInCurrentDir();
    gemfireDefaultFileInCurrentDir();

    assertThat(findPropertiesFileLocation()).isEqualTo(geodeDefaultFileInCurrentDir().getCanonicalFile().toURI());
  }

  @Test
  public void findPrefersGemFireDefaultFourth() throws Exception {
    gemfireDefaultFileInCurrentDir();

    assertThat(findPropertiesFileLocation()).isEqualTo(gemfireDefaultFileInCurrentDir().getCanonicalFile().toURI());
  }

  private File geodeDefaultFileInHomeDir() {
    return createFileInHomeDir("geode.properties");
  }

  private File geodeDefaultFileInCurrentDir() {
    return createFileInCurrentDir("geode.properties");
  }
  private File gemfireDefaultFileInHomeDir() {
    return createFileInHomeDir("gemfire.properties");
  }

  private File gemfireDefaultFileInCurrentDir() {
    return createFileInCurrentDir("gemfire.properties");
  }
  private File gemfireCustomFileInHomeDir() {
    return createFileInHomeDir("gemfireCustomFileInHomeDir.properties");
  }

  private File gemfireCustomFileInCurrentDir() {
    return createFileInCurrentDir("gemfireCustomFileInCurrentDir.properties");
  }

  private File geodeCustomFileInHomeDir() {
    return createFileInHomeDir("geodeCustomFileInHomeDir.properties");
  }
  private File geodeCustomFileInCurrentDir()  {
    return createFileInCurrentDir("geodeCustomFileInCurrentDir.properties");
  }
  
  private File createFileInCurrentDir(String name) {
    return createFile(System.getProperty("user.dir"), name);
  }
  private File createFileInHomeDir(String name){
    return createFile(System.getProperty("user.home"), name);
  }

  private File createFile(String parent, String child) {
    File file = new File(parent, child);

    if (file.exists()) {
      return file;
    }
    try {
      assertThat(file.createNewFile()).isTrue();
      files.add(file);
      file.deleteOnExit();
    } catch(IOException e) {
      throw new AssertionError(e);
    }
    return file;
  }
}