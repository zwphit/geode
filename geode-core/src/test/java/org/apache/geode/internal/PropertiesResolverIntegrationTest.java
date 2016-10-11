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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

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
      try {
        FileUtils.forceDelete(file);
      } catch (Exception ignored) {}
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
  public void searchesCurrentDirFirst() throws Exception {
    geodeDefaultFileInCurrentDir();
    geodeDefaultFileInHomeDir();
    geodeInJarAsClasspathResource();

    assertThat(findPropertiesFileLocation()).isEqualTo(geodeDefaultFileInCurrentDir().toURI());
  }

  @Test
  public void searchesHomeDirSecond() throws Exception {
    geodeDefaultFileInHomeDir();
    geodeInJarAsClasspathResource();

    assertThat(findPropertiesFileLocation()).isEqualTo(geodeDefaultFileInHomeDir().toURI());
  }

  @Test
  public void searchesJarOnClasspathThird() throws Exception {
    System.setProperty(PropertiesResolver.GEODE_PROPERTIES_FILE_PROPERTY, "geodeInJar.properties");

    URL url = propsFileInJarOnClasspath();

    assertThat(findPropertiesFileLocation()).isEqualTo(url.toURI());
  }

  @Test
  public void searchesDirOnClasspathThird() throws Exception {
    System.setProperty(PropertiesResolver.GEODE_PROPERTIES_FILE_PROPERTY, "geodeInDir.properties");

    URL url = propsFileInDirOnClasspath(); // TODO

    assertThat(findPropertiesFileLocation()).isEqualTo(url.toURI());
  }

  @Test
  public void searchReturnsNullLast() throws Exception {
    assertThat(findPropertiesFileLocation()).isNull();
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

  public File createJar(String jarName, File inputFile) throws IOException
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