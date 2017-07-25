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

import java.io.Serializable;

import org.apache.logging.log4j.Logger;

import org.apache.geode.cache.AttributesFactory;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheListener;
import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheWriter;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.internal.InternalEntity;
import org.apache.geode.internal.logging.LogService;
import org.apache.geode.management.internal.cli.i18n.CliStrings;

/**
 * @since GemFire 7.0
 */
public class FetchRegionAttributesFunction implements Function, InternalEntity {
  private static final long serialVersionUID = 4366812590788342070L;
  private static final Logger logger = LogService.getLogger();

  @Override
  public boolean isHA() {
    return false;
  }

  @Override
  public void execute(final FunctionContext context) {
    try {
      String regionPath = (String) context.getArguments();
      if (regionPath == null) {
        throw new IllegalArgumentException(
            CliStrings.CREATE_REGION__MSG__SPECIFY_VALID_REGION_PATH);
      }
      FetchRegionAttributesFunctionResult<?, ?> result =
          getRegionAttributes(context.getCache(), regionPath);
      context.getResultSender().lastResult(result);

    } catch (IllegalArgumentException e) {
      if (logger.isDebugEnabled()) {
        logger.debug(e.getMessage(), e);
      }
      context.getResultSender().lastResult(e);
    }
  }

  public static <K, V> FetchRegionAttributesFunctionResult<K, V> getRegionAttributes(
      final Cache cache, final String regionPath) {
    Region<K, V> foundRegion = cache.getRegion(regionPath);

    if (foundRegion == null) {
      throw new IllegalArgumentException(CliStrings.format(
          CliStrings.CREATE_REGION__MSG__SPECIFY_VALID_REGION_PATH_FOR_0_REGIONPATH_1_NOT_FOUND,
          new Object[] {CliStrings.CREATE_REGION__USEATTRIBUTESFROM, regionPath}));
    }

    // Using AttributesFactory to get the serializable RegionAttributes
    // Is there a better way?
    AttributesFactory<K, V> afactory = new AttributesFactory<K, V>(foundRegion.getAttributes());
    FetchRegionAttributesFunctionResult<K, V> result =
        new FetchRegionAttributesFunctionResult<K, V>(afactory);
    return result;
  }

  /**
   * Result of executing FetchRegionAttributesFunction.
   */
  public static class FetchRegionAttributesFunctionResult<K, V> implements Serializable {
    private static final long serialVersionUID = -3970828263897978845L;

    private final RegionAttributes<K, V> regionAttributes;
    private final String[] cacheListenerClasses;
    private final String cacheLoaderClass;
    private final String cacheWriterClass;

    public FetchRegionAttributesFunctionResult(final AttributesFactory<K, V> afactory) {
      RegionAttributes<K, V> regionAttributes = afactory.create();

      CacheListener<K, V>[] cacheListeners = regionAttributes.getCacheListeners();
      if (cacheListeners != null && cacheListeners.length != 0) {
        cacheListenerClasses = new String[cacheListeners.length];
        for (int i = 0; i < cacheListeners.length; i++) {
          cacheListenerClasses[i] = cacheListeners[i].getClass().getName();
        }
        afactory.initCacheListeners(null);
      } else {
        cacheListenerClasses = null;
      }

      CacheLoader<K, V> cacheLoader = regionAttributes.getCacheLoader();
      if (cacheLoader != null) {
        cacheLoaderClass = cacheLoader.getClass().getName();
        afactory.setCacheLoader(null);
      } else {
        cacheLoaderClass = null;
      }

      CacheWriter<K, V> cacheWriter = regionAttributes.getCacheWriter();
      if (cacheWriter != null) {
        cacheWriterClass = cacheWriter.getClass().getName();
        afactory.setCacheWriter(null);
      } else {
        cacheWriterClass = null;
      }

      // recreate attributes
      this.regionAttributes = afactory.create();
    }

    public RegionAttributes<K, V> getRegionAttributes() {
      return regionAttributes;
    }

    public String[] getCacheListenerClasses() {
      return cacheListenerClasses;
    }

    public String getCacheLoaderClass() {
      return cacheLoaderClass;
    }

    public String getCacheWriterClass() {
      return cacheWriterClass;
    }
  }

}
