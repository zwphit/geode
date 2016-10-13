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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.geode.GemFireIOException;
import org.apache.geode.distributed.DistributedSystem;
import org.apache.geode.internal.i18n.LocalizedStrings;
import org.apache.geode.internal.lang.StringUtils;
import org.apache.geode.internal.util.IOUtils;

/**
 * Order of preference:
 * <ul>
 * <li>system property specified value
 * <li>builder specified value (note: this becomes a system property)
 * <li>overridden default value
 * <li>default value
 * </ul>
 */
public class PropertiesResolver {

  public static final String GEODE_PREFIX = "geode.";
  public static final String GEMFIRE_PREFIX = "gemfire.";
  public static final String GEODE_PROPERTIES_FILE_KEY = "geodePropertyFile";
  public static final String GEMFIRE_PROPERTIES_FILE_KEY = "gemfirePropertyFile";
  public static final String DEFAULT_GEODE_PROPERTIES_FILE_VALUE = "geode.properties";
  public static final String DEFAULT_GEMFIRE_PROPERTIES_FILE_VALUE = "gemfire.properties";
  public static final String DEFAULT_PROPERTIES_FILE_VALUE = DEFAULT_GEODE_PROPERTIES_FILE_VALUE;

  private Properties properties;

  public PropertiesResolver() {
    this(findPropertiesFile());
  }

  protected PropertiesResolver(URL url) {
    properties = loadPropertiesFromURL(url);
  }

  private Properties loadPropertiesFromURL(URL url) {
    Properties properties = new Properties();
    try {
      properties.load(new FileReader(new File(url.toURI())));
    } catch (URISyntaxException | IOException e) {
      throw new GemFireIOException(LocalizedStrings.DistributionConfigImpl_FAILED_READING_0.toLocalizedString(url), e);
    }
    return properties;
  }

  public String getSpecifiedPropertiesFileName() {
    if (hasSystemProperty(GEODE_PROPERTIES_FILE_KEY)) {
      return System.getProperty(GEODE_PROPERTIES_FILE_KEY);
    } else if (hasSystemProperty(GEMFIRE_PROPERTIES_FILE_KEY)) {
      return System.getProperty(GEMFIRE_PROPERTIES_FILE_KEY);
    } else {
      return DEFAULT_PROPERTIES_FILE_VALUE;
    }
  }

  public Map<String, ConfigSource> propertiesWithSource() {
    // add props from gemfire system props
    // add props from geode system props
    //add props from file
    // add props from security file

    return null;
  }

  public static URL findPropertiesFile() {
    List<URL> possibleUrls = new ArrayList<>();
    possibleUrls.add(getFileURI(System.getProperty(GEODE_PROPERTIES_FILE_KEY)));
    possibleUrls.add(getFileURI(System.getProperty(GEMFIRE_PROPERTIES_FILE_KEY)));
    possibleUrls.add(getFileURI(DEFAULT_GEODE_PROPERTIES_FILE_VALUE));
    possibleUrls.add(getFileURI(DEFAULT_GEMFIRE_PROPERTIES_FILE_VALUE));

    //TODO: Should we make a list of Strings instead of URLs and turn it into a URL after picking one?
    return possibleUrls.stream().filter(s -> s != null).findFirst().orElse(null);
  }

  static boolean hasSystemProperty(final String key) {
    return System.getProperties().containsKey(DistributedSystem.PROPERTIES_FILE_PROPERTY);
  }

  public boolean hasProperty(String key) {
    return getProperty(key) != null;
  }

  public boolean hasNonBlankPropertyValue(String key) {
    return !StringUtils.isBlank(getProperty(key));
  }

  //TODO: Should we fall back to default instead of null?
  // What should this do for blank properties?
  public String getProperty(final String key) {
    List<String> possibleValues = new ArrayList<>();
    possibleValues.add(System.getProperty(GEODE_PREFIX + key));
    possibleValues.add(System.getProperty(GEMFIRE_PREFIX + key));
    possibleValues.add(properties.getProperty(key));

    return possibleValues.stream().filter( s -> s!=null).findFirst().orElse(null);
  }

  private static URL getFileURI(String fileName) {
    if (fileName == null) {
      return null;
    }

    File file = new File(fileName); // absolute path or current dir

    if (file.exists()) {
      URL url = getFileURL(file);
      if (url != null) {
        return url;
      }
    }

    file = new File(System.getProperty("user.home"), fileName); // user home

    if (file.exists()) {
      URL url = getFileURL(file);
      if (url != null) {
        return url;
      }
    }

    return getResourceOrNull(fileName); // resource
  }

  private static URL getFileURL(File file) {
    try {
      return IOUtils.tryGetCanonicalFileElseGetAbsoluteFile(file).toURI().toURL();
    } catch (MalformedURLException e) {
      return null;
    }
  }


  private static URL getResourceOrNull(final String name) {
    return ClassPathLoader.getLatest().getResource(name); // resource
  }

}
