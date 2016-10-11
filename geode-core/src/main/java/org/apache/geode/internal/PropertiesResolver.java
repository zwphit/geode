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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.geode.distributed.DistributedSystem;
import org.apache.geode.internal.util.IOUtils;

public class PropertiesResolver {

  static final String GEODE_PROPERTIES_FILE_PROPERTY = "geodePropertyFile";
  static final String GEMFIRE_PROPERTIES_FILE_PROPERTY = DistributedSystem.PROPERTIES_FILE_PROPERTY;
  static final String DEFAULT_GEODE_PROPERTIES_FILE_NAME = "geode.properties";
  static final String DEFAULT_GEMFIRE_PROPERTIES_FILE_NAME = "gemfire.properties";

  private static URI propertiesFileURL = findPropertiesFileLocation();

  static URI findPropertiesFileLocation() {
    List<URI> possibleUrls = new ArrayList<>();
    possibleUrls.add(getFileURI(System.getProperty(GEODE_PROPERTIES_FILE_PROPERTY)));
    possibleUrls.add(getFileURI(System.getProperty(GEMFIRE_PROPERTIES_FILE_PROPERTY)));
    possibleUrls.add(getFileURI(DEFAULT_GEODE_PROPERTIES_FILE_NAME));
    possibleUrls.add(getFileURI(DEFAULT_GEMFIRE_PROPERTIES_FILE_NAME));

    return possibleUrls.stream().filter(s -> s != null).findFirst().orElse(null);
  }

  static boolean hasProperty(final String key) {
    return System.getProperties().containsKey(DistributedSystem.PROPERTIES_FILE_PROPERTY);
  }

  private static URI getFileURI(String fileName) {
    if (fileName == null) {
      return null;
    }

    File file = new File(fileName); // absolute path or current dir

    if (file.exists()) {
        return IOUtils.tryGetCanonicalFileElseGetAbsoluteFile(file).toURI();
    }

    file = new File(System.getProperty("user.home"), fileName); // user home

    if (file.exists()) {
        return IOUtils.tryGetCanonicalFileElseGetAbsoluteFile(file).toURI();
    }

    return getResourceOrNull(fileName); // resource
  }

  private static URI getResourceOrNull(final String name) {
    try {
      URL url = ClassPathLoader.getLatest().getResource(DistributedSystem.class, name); // resource
      return (url == null) ? null : url.toURI();
    } catch (URISyntaxException ignore) {
      return null;
    }
  }

}
