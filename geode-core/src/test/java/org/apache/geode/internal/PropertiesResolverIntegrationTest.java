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

import static org.apache.geode.distributed.ConfigurationProperties.*;
import static org.apache.geode.internal.PropertiesResolver.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import org.apache.geode.distributed.AbstractLauncher;
import org.apache.geode.distributed.internal.DistributionConfig;
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

  @Rule
  public TestName testName = new TestName();

  @Before
  public void createFiles() throws Exception {
    this.geodeCustomProperties = this.temporaryFolder.newFile("geodeCustom.properties");
    this.gemfireCustomProperties = this.temporaryFolder.newFile("gemfireCustom.properties");
  }

  @After
  public void cleanup() throws Exception {
    for (File file: files) {
      try {
        FileUtils.forceDelete(file);
      } catch (Exception ignored) {}
    }
  }

  @Test
  public void canSpecifyGeodePropertiesFileAbsolutePath() throws Exception {
    System.setProperty(GEODE_PROPERTIES_FILE_KEY, this.geodeCustomProperties.getCanonicalPath());
    assertThat(findPropertiesFile()).isEqualTo(this.geodeCustomProperties.getCanonicalFile().toURL());
  }

  @Test
  public void canSpecifyGeodePropertiesFileInCurrentDir() throws Exception {
    System.setProperty(GEODE_PROPERTIES_FILE_KEY, geodeCustomFileInCurrentDir().getName());
    assertThat(findPropertiesFile()).isEqualTo(geodeCustomFileInCurrentDir().getCanonicalFile().toURL());
  }

 @Test
  public void canSpecifyGeodePropertiesFileInUserHomeDir() throws Exception {
    System.setProperty(GEODE_PROPERTIES_FILE_KEY, geodeCustomFileInHomeDir().getName());
    assertThat(findPropertiesFile()).isEqualTo(geodeCustomFileInHomeDir().getCanonicalFile().toURL());
  }

  @Test
  public void canSpecifyGemFirePropertiesFileAbsolutePath() throws Exception {
    System.setProperty(GEMFIRE_PROPERTIES_FILE_KEY, this.gemfireCustomProperties.getCanonicalPath());
    assertThat(findPropertiesFile()).isEqualTo(this.gemfireCustomProperties.getCanonicalFile().toURL());
  }

  @Test
  public void canSpecifyGemFirePropertiesFileInCurrentDir() throws Exception {
    System.setProperty(GEMFIRE_PROPERTIES_FILE_KEY, gemfireCustomFileInCurrentDir().getName());
    assertThat(findPropertiesFile()).isEqualTo(gemfireCustomFileInCurrentDir().getCanonicalFile().toURL());
  }

  @Test
  public void canSpecifyGemFirePropertiesFileInUserHomeDir() throws Exception {
    System.setProperty(GEMFIRE_PROPERTIES_FILE_KEY, gemfireCustomFileInHomeDir().getName());
    assertThat(findPropertiesFile()).isEqualTo(gemfireCustomFileInHomeDir().getCanonicalFile().toURL());
  }

  @Test
  public void searchesCurrentDirFirst() throws Exception {
    geodeDefaultFileInCurrentDir();
    geodeDefaultFileInHomeDir();
    geodeInJarAsClasspathResource();

    assertThat(findPropertiesFile()).isEqualTo(geodeDefaultFileInCurrentDir().toURL());
  }

  @Test
  public void searchesHomeDirSecond() throws Exception {
    geodeDefaultFileInHomeDir();
    geodeInJarAsClasspathResource();

    assertThat(findPropertiesFile()).isEqualTo(geodeDefaultFileInHomeDir().toURL());
  }

  @Test
  public void searchesJarOnClasspathThird() throws Exception {
    System.setProperty(PropertiesResolver.GEODE_PROPERTIES_FILE_KEY, "geodeInJar.properties");

    URL url = propsFileInJarOnClasspath();

    assertThat(findPropertiesFile()).isEqualTo(url);
  }

  @Test
  public void searchesDirOnClasspathThird() throws Exception {
    System.setProperty(PropertiesResolver.GEODE_PROPERTIES_FILE_KEY, "geodeInDir.properties");

    URL url = propsFileInDirOnClasspath(); // TODO

    assertThat(findPropertiesFile()).isEqualTo(url);
  }

  @Test
  public void searchReturnsNullLast() throws Exception {
    assertThat(findPropertiesFile()).isNull();
  }

  private URL propsFileInJarOnClasspath() throws IOException, URISyntaxException {
    //Create jar containing our properties file
    File jar = geodeInJarAsClasspathResource();
    
    //Create classloader pointing to our jar
    URLClassLoader ourClassLoader = new URLClassLoader(new URL[]{new URL("file://" + jar.getCanonicalPath())});
    assertThat(ourClassLoader.getURLs()).hasSize(1);

    //Make sure we can load the properties file from our jar
    URL stream = ourClassLoader.getResource("geodeInJar.properties");
    assertThat(stream).isNotNull();

    //Add our classloader to Geode
    ClassPathLoader.getLatest().addOrReplaceAndSetLatest(ourClassLoader);
    assertThat(ClassPathLoader.getLatest().getClassLoaders()).contains(ourClassLoader);

    //Get URL for properties file inside jar
    URL url = ClassPathLoader.getLatest().getResource("geodeInJar.properties");
    assertThat(url).isNotNull();
    return url;
  }

  private URL propsFileInDirOnClasspath() throws IOException, URISyntaxException {
    File propsFile = temporaryFolder.newFile("geodeInDir.properties");

    //Create classloader pointing to our dir
    URLClassLoader ourClassLoader = new URLClassLoader(new URL[]{temporaryFolder.getRoot().toURL()});
    assertThat(ourClassLoader.getURLs()).hasSize(1);

    //Make sure we can load the properties file from our jar
    URL stream = ourClassLoader.getResource("geodeInDir.properties");
    assertThat(stream).isNotNull();

    //Add our classloader to Geode
    ClassPathLoader.getLatest().addOrReplaceAndSetLatest(ourClassLoader);
    assertThat(ClassPathLoader.getLatest().getClassLoaders()).contains(ourClassLoader);

    //Get URL for properties file inside jar
    URL url = ClassPathLoader.getLatest().getResource("geodeInDir.properties");
    assertThat(url).isNotNull();
    return url;
  }

  @Test
  public void findPrefersGeodePropertiesFileFirst() throws Exception {
    System.setProperty(GEODE_PROPERTIES_FILE_KEY, this.geodeCustomProperties.getCanonicalPath());
    System.setProperty(GEMFIRE_PROPERTIES_FILE_KEY, this.gemfireCustomProperties.getCanonicalPath());
    geodeDefaultFileInCurrentDir();
    gemfireDefaultFileInCurrentDir();

    assertThat(findPropertiesFile()).isEqualTo(this.geodeCustomProperties.getCanonicalFile().toURL());
  }

  @Test
  public void findPrefersGemFirePropertiesFileSecond() throws Exception {
    System.setProperty(GEMFIRE_PROPERTIES_FILE_KEY, this.gemfireCustomProperties.getCanonicalPath());
    geodeDefaultFileInCurrentDir();
    gemfireDefaultFileInCurrentDir();

    assertThat(findPropertiesFile()).isEqualTo(this.gemfireCustomProperties.getCanonicalFile().toURL());
  }

  @Test
  public void findPrefersGeodeDefaultThird() throws Exception {
    geodeDefaultFileInCurrentDir();
    gemfireDefaultFileInCurrentDir();

    assertThat(findPropertiesFile()).isEqualTo(geodeDefaultFileInCurrentDir().getCanonicalFile().toURL());
  }

  @Test
  public void findPrefersGemFireDefaultFourth() throws Exception {
    gemfireDefaultFileInCurrentDir();

    assertThat(findPropertiesFile()).isEqualTo(gemfireDefaultFileInCurrentDir().getCanonicalFile().toURL());
  }

  @Test
  public void getPropertyPrefersGeodeSystemPropertyFirst() throws Exception {
    System.setProperty(GEODE_PREFIX + NAME, "nameFromGeodeSystemProperty");
    System.setProperty(GEMFIRE_PREFIX + NAME, "nameFromGemFireSystemProperty");

    File gemfirePropertiesFile = this.temporaryFolder.newFile(GEODE_PREFIX + "properties");
    Properties expectedGemfireProperties = new Properties();
    expectedGemfireProperties.setProperty(NAME, "nameFromPropertiesFile");
    expectedGemfireProperties.store(new FileWriter(gemfirePropertiesFile, false), this.testName.getMethodName());
    assertThat(gemfirePropertiesFile).isNotNull().exists().isFile();

    PropertiesResolver propertiesResolver = new PropertiesResolver(gemfirePropertiesFile.toURL());

    assertThat(propertiesResolver.getProperty(NAME)).isEqualTo("nameFromGeodeSystemProperty");
  }

  @Test
  public void getPropertyPrefersGemfireSystemPropertySecond() throws Exception {
    System.clearProperty(GEODE_PREFIX + NAME);
    System.setProperty(GEMFIRE_PREFIX + NAME, "nameFromGemFireSystemProperty");

    File gemfirePropertiesFile = this.temporaryFolder.newFile(GEODE_PREFIX + "properties"); // TODO: gemfire.properties and geode.properties
    Properties expectedGemfireProperties = new Properties();
    expectedGemfireProperties.setProperty(NAME, "nameFromPropertiesFile");
    expectedGemfireProperties.store(new FileWriter(gemfirePropertiesFile, false), this.testName.getMethodName());
    assertThat(gemfirePropertiesFile).isNotNull().exists().isFile();

    PropertiesResolver propertiesResolver = new PropertiesResolver(gemfirePropertiesFile.toURL());

    assertThat(propertiesResolver.getProperty(NAME)).isEqualTo("nameFromGemFireSystemProperty");
  }

  @Test
  public void getPropertyUsesPropertiesFileLast() throws Exception {
    System.clearProperty(GEODE_PREFIX + NAME);
    System.clearProperty(GEMFIRE_PREFIX + NAME);

    File gemfirePropertiesFile = this.temporaryFolder.newFile(GEODE_PREFIX + "properties"); // TODO: gemfire.properties and geode.properties
    Properties expectedGemfireProperties = new Properties();
    expectedGemfireProperties.setProperty(NAME, "nameFromPropertiesFile");
    expectedGemfireProperties.store(new FileWriter(gemfirePropertiesFile, false), this.testName.getMethodName());
    assertThat(gemfirePropertiesFile).isNotNull().exists().isFile();

    PropertiesResolver propertiesResolver = new PropertiesResolver(gemfirePropertiesFile.toURL());

    assertThat(propertiesResolver.getProperty(NAME)).isEqualTo("nameFromPropertiesFile");
  }

  /**
   * Extracted from AbstractLauncherTest
   */
  @Ignore
  @Test
  public void testLoadGemFirePropertiesWithNullURL() {
    PropertiesResolver propertiesResolver = new PropertiesResolver(null);
    //assertThat(properties).isEmpty(); TODO
  }

  /**
   * Extracted from AbstractLauncherTest
   */
  @Ignore
  @Test
  public void testLoadGemFirePropertiesWithNonExistingURL() throws MalformedURLException {
    PropertiesResolver propertiesResolver = new PropertiesResolver(new URL("file:///path/to/non_existing/gemfire.properties"));
    //assertThat(properties).isEmpty(); TODO
  }

  private File geodeInJarAsClasspathResource() throws IOException {
    File existingJar = new File(this.temporaryFolder.getRoot().getCanonicalPath(), "ourJar.jar");
    if (existingJar.exists()){
      return existingJar;
    }

    File tempFile = createFile(this.temporaryFolder.getRoot().getCanonicalPath(), "geodeInJar.properties");
    File jar = createJar("ourJar.jar", tempFile);
    FileUtils.forceDelete(tempFile);

    return jar;
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

  private File createJar(String jarName, File inputFile) throws IOException
  {
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    File jarFile = createFile(this.temporaryFolder.getRoot().getCanonicalPath(), jarName);
    JarOutputStream target = new JarOutputStream(new FileOutputStream(jarFile), manifest);
    add(inputFile, target);
    target.close();
    return jarFile;
  }

  private void add(File source, JarOutputStream target) throws IOException
  {
    BufferedInputStream in = null;
    try
    {
      JarEntry entry = new JarEntry(source.getName().replace("\\", "/"));
      entry.setTime(source.lastModified());
      target.putNextEntry(entry);
      in = new BufferedInputStream(new FileInputStream(source));

      byte[] buffer = new byte[1024];
      while (true)
      {
        int count = in.read(buffer);
        if (count == -1)
          break;
        target.write(buffer, 0, count);
      }
      target.closeEntry();
    }
    finally
    {
      if (in != null)
        in.close();
    }
  }
}