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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.geode.cache.ExpirationAction;
import org.apache.geode.cache.ExpirationAttributes;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.management.internal.cli.CliUtil;
import org.apache.geode.management.internal.cli.i18n.CliStrings;

/**
 * Used to carry arguments between gfsh region command implementations and the functions that do the
 * work for those commands.
 *
 * @since GemFire 7.0
 */
public class RegionFunctionArgs implements Serializable {
  private static final long serialVersionUID = -5158224572470173267L;

  private final String regionPath;
  private final RegionShortcut regionShortcut;
  private final String useAttributesFrom;
  private final Boolean skipIfExists;
  private final String keyConstraint;
  private final String valueConstraint;
  private final Boolean statisticsEnabled;
  private final boolean isSetStatisticsEnabled;
  private final RegionFunctionArgs.ExpirationAttrs entryExpirationIdleTime;
  private final RegionFunctionArgs.ExpirationAttrs entryExpirationTTL;
  private final RegionFunctionArgs.ExpirationAttrs regionExpirationIdleTime;
  private final RegionFunctionArgs.ExpirationAttrs regionExpirationTTL;
  private final String diskStore;
  private final Boolean diskSynchronous;
  private final boolean isSetDiskSynchronous;
  private final Boolean enableAsyncConflation;
  private final boolean isSetEnableAsyncConflation;
  private final Boolean enableSubscriptionConflation;
  private final boolean isSetEnableSubscriptionConflation;
  private final Set<String> cacheListeners;
  private final String cacheLoader;
  private final String cacheWriter;
  private final Set<String> asyncEventQueueIds;
  private final Set<String> gatewaySenderIds;
  private final Boolean concurrencyChecksEnabled;
  private final boolean isSetConcurrencyChecksEnabled;
  private final Boolean cloningEnabled;
  private final boolean isSetCloningEnabled;
  private final Boolean mcastEnabled;
  private final boolean isSetMcastEnabled;
  private final Integer concurrencyLevel;
  private final boolean isSetConcurrencyLevel;
  private final PartitionArgs partitionArgs;
  private final Integer evictionMax;
  private final String compressor;
  private final boolean isSetCompressor;
  private final Boolean offHeap;
  private final boolean isSetOffHeap;
  private final boolean isPartitionResolver;
  private final String partitionResolver;
  private final RegionAttributes<?, ?> regionAttributes;

  /**
   * Constructor with RegionShortcut instead of RegionAttributes.
   *
   * <p>
   * NOTE: evictionMax and compressor used to be hardcoded to null but are now passed in.
   * RegionAttributes is still null.
   */
  public RegionFunctionArgs(final String regionPath, final RegionShortcut regionShortcut,
      final String useAttributesFrom, final boolean skipIfExists, final String keyConstraint,
      final String valueConstraint, final Boolean statisticsEnabled,
      final RegionFunctionArgs.ExpirationAttrs entryExpirationIdleTime,
      final RegionFunctionArgs.ExpirationAttrs entryExpirationTTL,
      final RegionFunctionArgs.ExpirationAttrs regionExpirationIdleTime,
      final RegionFunctionArgs.ExpirationAttrs regionExpirationTTL, final String diskStore,
      final Boolean diskSynchronous, final Boolean enableAsyncConflation,
      final Boolean enableSubscriptionConflation, final String[] cacheListeners,
      final String cacheLoader, final String cacheWriter, final String[] asyncEventQueueIds,
      final String[] gatewaySenderIds, final Boolean concurrencyChecksEnabled,
      final Boolean cloningEnabled, final Integer concurrencyLevel, final String prColocatedWith,
      final Integer prLocalMaxMemory, final Long prRecoveryDelay, final Integer prRedundantCopies,
      final Long prStartupRecoveryDelay, final Long prTotalMaxMemory,
      final Integer prTotalNumBuckets, final Integer evictionMax, final String compressor,
      final Boolean offHeap, final Boolean mcastEnabled, final String partitionResolver) {

    this(regionPath, regionShortcut, useAttributesFrom, skipIfExists, keyConstraint,
        valueConstraint, statisticsEnabled, entryExpirationIdleTime, entryExpirationTTL,
        regionExpirationIdleTime, regionExpirationTTL, diskStore, diskSynchronous,
        enableAsyncConflation, enableSubscriptionConflation, cacheListeners, cacheLoader,
        cacheWriter, asyncEventQueueIds, gatewaySenderIds, concurrencyChecksEnabled, cloningEnabled,
        concurrencyLevel, prColocatedWith, prLocalMaxMemory, prRecoveryDelay, prRedundantCopies,
        prStartupRecoveryDelay, prTotalMaxMemory, prTotalNumBuckets, evictionMax, compressor,
        offHeap, mcastEnabled, partitionResolver, null);
  }

  /**
   * Constructor with RegionAttributes instead of RegionShortcut.
   *
   * <p>
   * Note: regionShortcut, evictionMax and compressor are hardcoded to null.
   */
  public RegionFunctionArgs(final String regionPath, final String useAttributesFrom,
      final boolean skipIfExists, final String keyConstraint, final String valueConstraint,
      final Boolean statisticsEnabled,
      final RegionFunctionArgs.ExpirationAttrs entryExpirationIdleTime,
      final RegionFunctionArgs.ExpirationAttrs entryExpirationTTL,
      final RegionFunctionArgs.ExpirationAttrs regionExpirationIdleTime,
      final RegionFunctionArgs.ExpirationAttrs regionExpirationTTL, final String diskStore,
      final Boolean diskSynchronous, final Boolean enableAsyncConflation,
      final Boolean enableSubscriptionConflation, final String[] cacheListeners,
      final String cacheLoader, final String cacheWriter, final String[] asyncEventQueueIds,
      final String[] gatewaySenderIds, final Boolean concurrencyChecksEnabled,
      final Boolean cloningEnabled, final Integer concurrencyLevel, final String prColocatedWith,
      final Integer prLocalMaxMemory, final Long prRecoveryDelay, final Integer prRedundantCopies,
      final Long prStartupRecoveryDelay, final Long prTotalMaxMemory,
      final Integer prTotalNumBuckets, final Boolean offHeap, final Boolean mcastEnabled,
      final String partitionResolver, final RegionAttributes<?, ?> regionAttributes) {

    this(regionPath, null, useAttributesFrom, skipIfExists, keyConstraint, valueConstraint,
        statisticsEnabled, entryExpirationIdleTime, entryExpirationTTL, regionExpirationIdleTime,
        regionExpirationTTL, diskStore, diskSynchronous, enableAsyncConflation,
        enableSubscriptionConflation, cacheListeners, cacheLoader, cacheWriter, asyncEventQueueIds,
        gatewaySenderIds, concurrencyChecksEnabled, cloningEnabled, concurrencyLevel,
        prColocatedWith, prLocalMaxMemory, prRecoveryDelay, prRedundantCopies,
        prStartupRecoveryDelay, prTotalMaxMemory, prTotalNumBuckets, null, null, offHeap,
        mcastEnabled, partitionResolver, regionAttributes);
  }

  /**
   * Constructor with everything.
   */
  RegionFunctionArgs(final String regionPath, final RegionShortcut regionShortcut,
      final String useAttributesFrom, final boolean skipIfExists, final String keyConstraint,
      final String valueConstraint, final Boolean statisticsEnabled,
      final RegionFunctionArgs.ExpirationAttrs entryExpirationIdleTime,
      final RegionFunctionArgs.ExpirationAttrs entryExpirationTTL,
      final RegionFunctionArgs.ExpirationAttrs regionExpirationIdleTime,
      final RegionFunctionArgs.ExpirationAttrs regionExpirationTTL, final String diskStore,
      final Boolean diskSynchronous, final Boolean enableAsyncConflation,
      final Boolean enableSubscriptionConflation, final String[] cacheListeners,
      final String cacheLoader, final String cacheWriter, final String[] asyncEventQueueIds,
      final String[] gatewaySenderIds, final Boolean concurrencyChecksEnabled,
      final Boolean cloningEnabled, final Integer concurrencyLevel, final String prColocatedWith,
      final Integer prLocalMaxMemory, final Long prRecoveryDelay, final Integer prRedundantCopies,
      final Long prStartupRecoveryDelay, final Long prTotalMaxMemory,
      final Integer prTotalNumBuckets, final Integer evictionMax, final String compressor,
      final Boolean offHeap, final Boolean mcastEnabled, final String partitionResolver,
      final RegionAttributes<?, ?> regionAttributes) {

    this.regionPath = regionPath;
    this.regionShortcut = regionShortcut;
    this.useAttributesFrom = useAttributesFrom;
    this.skipIfExists = skipIfExists;
    this.keyConstraint = keyConstraint;
    this.valueConstraint = valueConstraint;
    this.evictionMax = evictionMax;

    this.isSetStatisticsEnabled = statisticsEnabled != null;
    if (this.isSetStatisticsEnabled) {
      this.statisticsEnabled = statisticsEnabled;
    } else {
      this.statisticsEnabled = null;
    }

    this.entryExpirationIdleTime = entryExpirationIdleTime;
    this.entryExpirationTTL = entryExpirationTTL;
    this.regionExpirationIdleTime = regionExpirationIdleTime;
    this.regionExpirationTTL = regionExpirationTTL;
    this.diskStore = diskStore;

    this.isSetDiskSynchronous = diskSynchronous != null;
    if (this.isSetDiskSynchronous) {
      this.diskSynchronous = diskSynchronous;
    } else {
      this.diskSynchronous = null;
    }

    this.isSetEnableAsyncConflation = enableAsyncConflation != null;
    if (this.isSetEnableAsyncConflation) {
      this.enableAsyncConflation = enableAsyncConflation;
    } else {
      this.enableAsyncConflation = null;
    }

    this.isSetEnableSubscriptionConflation = enableSubscriptionConflation != null;
    if (this.isSetEnableSubscriptionConflation) {
      this.enableSubscriptionConflation = enableSubscriptionConflation;
    } else {
      this.enableSubscriptionConflation = null;
    }

    if (cacheListeners != null) {
      this.cacheListeners = new LinkedHashSet<>();
      this.cacheListeners.addAll(Arrays.asList(cacheListeners));
    } else {
      this.cacheListeners = null;
    }

    this.cacheLoader = cacheLoader;
    this.cacheWriter = cacheWriter;

    if (asyncEventQueueIds != null) {
      this.asyncEventQueueIds = new LinkedHashSet<>();
      this.asyncEventQueueIds.addAll(Arrays.asList(asyncEventQueueIds));
    } else {
      this.asyncEventQueueIds = null;
    }

    if (gatewaySenderIds != null) {
      this.gatewaySenderIds = new LinkedHashSet<>();
      this.gatewaySenderIds.addAll(Arrays.asList(gatewaySenderIds));
    } else {
      this.gatewaySenderIds = null;
    }

    this.isSetConcurrencyChecksEnabled = concurrencyChecksEnabled != null;
    if (this.isSetConcurrencyChecksEnabled) {
      this.concurrencyChecksEnabled = concurrencyChecksEnabled;
    } else {
      this.concurrencyChecksEnabled = null;
    }

    this.isSetCloningEnabled = cloningEnabled != null;
    if (this.isSetCloningEnabled) {
      this.cloningEnabled = cloningEnabled;
    } else {
      this.cloningEnabled = null;
    }

    this.isSetMcastEnabled = mcastEnabled != null;
    if (isSetMcastEnabled) {
      this.mcastEnabled = mcastEnabled;
    } else {
      this.mcastEnabled = null;
    }

    this.isSetConcurrencyLevel = concurrencyLevel != null;
    if (this.isSetConcurrencyLevel) {
      this.concurrencyLevel = concurrencyLevel;
    } else {
      this.concurrencyLevel = null;
    }

    this.partitionArgs =
        new PartitionArgs(prColocatedWith, prLocalMaxMemory, prRecoveryDelay, prRedundantCopies,
            prStartupRecoveryDelay, prTotalMaxMemory, prTotalNumBuckets, partitionResolver);

    this.isSetCompressor = (compressor != null);
    if (this.isSetCompressor) {
      this.compressor = compressor;
    } else {
      this.compressor = null;
    }

    this.isSetOffHeap = (offHeap != null);
    if (this.isSetOffHeap) {
      this.offHeap = offHeap;
    } else {
      this.offHeap = null;
    }

    this.isPartitionResolver = (partitionResolver != null);
    if (this.isPartitionResolver) {
      this.partitionResolver = partitionResolver;
    } else {
      this.partitionResolver = null;
    }

    this.regionAttributes = regionAttributes;
  }

  public String getRegionPath() {
    return this.regionPath;
  }

  public RegionShortcut getRegionShortcut() {
    return this.regionShortcut;
  }

  public String getUseAttributesFrom() {
    return this.useAttributesFrom;
  }

  /**
   * @return true if need to use specified region attributes
   */
  public Boolean isSetUseAttributesFrom() {
    return this.regionShortcut == null && this.useAttributesFrom != null
        && this.regionAttributes != null;
  }

  public Boolean isSkipIfExists() {
    return this.skipIfExists;
  }

  public String getKeyConstraint() {
    return this.keyConstraint;
  }

  public String getValueConstraint() {
    return this.valueConstraint;
  }

  public Boolean isStatisticsEnabled() {
    return this.statisticsEnabled;
  }

  public Boolean isSetStatisticsEnabled() {
    return this.isSetStatisticsEnabled;
  }

  public RegionFunctionArgs.ExpirationAttrs getEntryExpirationIdleTime() {
    return this.entryExpirationIdleTime;
  }

  public RegionFunctionArgs.ExpirationAttrs getEntryExpirationTTL() {
    return this.entryExpirationTTL;
  }

  public RegionFunctionArgs.ExpirationAttrs getRegionExpirationIdleTime() {
    return this.regionExpirationIdleTime;
  }

  public RegionFunctionArgs.ExpirationAttrs getRegionExpirationTTL() {
    return this.regionExpirationTTL;
  }

  public String getDiskStore() {
    return this.diskStore;
  }

  public Boolean isDiskSynchronous() {
    return this.diskSynchronous;
  }

  public Boolean isSetDiskSynchronous() {
    return this.isSetDiskSynchronous;
  }

  public Boolean isOffHeap() {
    return this.offHeap;
  }

  public Boolean isSetOffHeap() {
    return this.isSetOffHeap;
  }

  public Boolean isEnableAsyncConflation() {
    return this.enableAsyncConflation;
  }

  public Boolean isSetEnableAsyncConflation() {
    return this.isSetEnableAsyncConflation;
  }

  public Boolean isEnableSubscriptionConflation() {
    return this.enableSubscriptionConflation;
  }

  public Boolean isSetEnableSubscriptionConflation() {
    return this.isSetEnableSubscriptionConflation;
  }

  public Set<String> getCacheListeners() {
    if (this.cacheListeners == null) {
      return null;
    }
    return Collections.unmodifiableSet(this.cacheListeners);
  }

  public String getCacheLoader() {
    return this.cacheLoader;
  }

  public String getCacheWriter() {
    return this.cacheWriter;
  }

  public Set<String> getAsyncEventQueueIds() {
    if (this.asyncEventQueueIds == null) {
      return null;
    }
    return Collections.unmodifiableSet(this.asyncEventQueueIds);
  }

  public Set<String> getGatewaySenderIds() {
    if (this.gatewaySenderIds == null) {
      return null;
    }
    return Collections.unmodifiableSet(this.gatewaySenderIds);
  }

  public String getPartitionResolver() {
    return this.partitionResolver;
  }

  public Boolean isPartitionResolverSet() {
    return this.isPartitionResolver;
  }

  public Boolean isConcurrencyChecksEnabled() {
    return this.concurrencyChecksEnabled;
  }

  public Boolean isSetConcurrencyChecksEnabled() {
    return this.isSetConcurrencyChecksEnabled;
  }

  public Boolean isCloningEnabled() {
    return this.cloningEnabled;
  }

  public Boolean isSetCloningEnabled() {
    return this.isSetCloningEnabled;
  }

  public Boolean isMcastEnabled() {
    return this.mcastEnabled;
  }

  public Boolean isSetMcastEnabled() {
    return this.isSetMcastEnabled;
  }

  public Integer getConcurrencyLevel() {
    return this.concurrencyLevel;
  }

  public Boolean isSetConcurrencyLevel() {
    return this.isSetConcurrencyLevel;
  }

  public boolean withPartitioning() {
    return hasPartitionAttributes()
        || (this.regionShortcut != null && this.regionShortcut.name().startsWith("PARTITION"));
  }

  public boolean hasPartitionAttributes() {
    return this.partitionArgs != null && this.partitionArgs.hasPartitionAttributes();
  }

  public PartitionArgs getPartitionArgs() {
    return this.partitionArgs;
  }

  public Integer getEvictionMax() {
    return this.evictionMax;
  }

  public String getCompressor() {
    return this.compressor;
  }

  public boolean isSetCompressor() {
    return this.isSetCompressor;
  }

  public <K, V> RegionAttributes<K, V> getRegionAttributes() {
    return (RegionAttributes<K, V>) this.regionAttributes;
  }

  public static class ExpirationAttrs implements Serializable {
    private static final long serialVersionUID = 1474255033398008062L;

    private final ExpirationFor type;
    private final Integer time;
    private final ExpirationAction action;

    public ExpirationAttrs(final ExpirationFor type, final Integer time, final String action) {
      this.type = type;
      this.time = time;
      if (action == null) {
        this.action = null;
      } else {
        this.action = getExpirationAction(action);
      }
    }

    public ExpirationAttributes convertToExpirationAttributes() {
      ExpirationAttributes expirationAttr;
      if (action != null) {
        expirationAttr = new ExpirationAttributes(time, action);
      } else {
        expirationAttr = new ExpirationAttributes(time);
      }
      return expirationAttr;
    }

    public ExpirationFor getType() {
      return type;
    }

    public Integer getTime() {
      return time;
    }

    public ExpirationAction getAction() {
      return action;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append(ExpirationAttrs.class.getSimpleName() + " [type=");
      builder.append(type);
      builder.append(", time=");
      builder.append(time);
      builder.append(", action=");
      builder.append(action);
      builder.append("]");
      return builder.toString();
    }

    private static ExpirationAction getExpirationAction(String action) {
      if (action == null) {
        return ExpirationAttributes.DEFAULT.getAction();
      }
      action = action.replace('-', '_');
      if (action.equalsIgnoreCase(ExpirationAction.DESTROY.toString())) {
        return ExpirationAction.DESTROY;
      } else if (action.equalsIgnoreCase(ExpirationAction.INVALIDATE.toString())) {
        return ExpirationAction.INVALIDATE;
      } else if (action.equalsIgnoreCase(ExpirationAction.LOCAL_DESTROY.toString())) {
        return ExpirationAction.LOCAL_DESTROY;
      } else if (action.equalsIgnoreCase(ExpirationAction.LOCAL_INVALIDATE.toString())) {
        return ExpirationAction.LOCAL_INVALIDATE;
      } else {
        throw new IllegalArgumentException(
            CliStrings.format(CliStrings.CREATE_REGION__MSG__EXPIRATION_ACTION_0_IS_NOT_VALID,
                new Object[] {action}));
      }
    }

    public enum ExpirationFor {
      REGION_IDLE, REGION_TTL, ENTRY_IDLE, ENTRY_TTL
    }
  }

  // TODO: make PartitionArgs immutable
  public static class PartitionArgs implements Serializable {
    private static final long serialVersionUID = 5907052187323280919L;

    private final String prColocatedWith;
    private int prLocalMaxMemory;
    private final boolean isSetPRLocalMaxMemory;
    private long prRecoveryDelay;
    private final boolean isSetPRRecoveryDelay;
    private int prRedundantCopies;
    private final boolean isSetPRRedundantCopies;
    private long prStartupRecoveryDelay;
    private final boolean isSetPRStartupRecoveryDelay;
    private long prTotalMaxMemory;
    private final boolean isSetPRTotalMaxMemory;
    private int prTotalNumBuckets;
    private final boolean isSetPRTotalNumBuckets;
    private final boolean isPartitionResolver;
    private String partitionResolver;

    private boolean hasPartitionAttributes;
    private final Set<String> userSpecifiedPartitionAttributes = new HashSet<>();

    public PartitionArgs(final String prColocatedWith, final Integer prLocalMaxMemory,
        final Long prRecoveryDelay, final Integer prRedundantCopies,
        final Long prStartupRecoveryDelay, final Long prTotalMaxMemory,
        final Integer prTotalNumBuckets, final String partitionResolver) {
      this.prColocatedWith = prColocatedWith;
      if (this.prColocatedWith != null) {
        this.hasPartitionAttributes = true;
        userSpecifiedPartitionAttributes.add(CliStrings.CREATE_REGION__COLOCATEDWITH);
      }
      this.isSetPRLocalMaxMemory = prLocalMaxMemory != null;
      if (this.isSetPRLocalMaxMemory) {
        this.prLocalMaxMemory = prLocalMaxMemory;
        this.hasPartitionAttributes = true;
        userSpecifiedPartitionAttributes.add(CliStrings.CREATE_REGION__LOCALMAXMEMORY);
      }
      this.isSetPRRecoveryDelay = prRecoveryDelay != null;
      if (this.isSetPRRecoveryDelay) {
        this.prRecoveryDelay = prRecoveryDelay;
        this.hasPartitionAttributes = true;
        userSpecifiedPartitionAttributes.add(CliStrings.CREATE_REGION__RECOVERYDELAY);
      }
      this.isSetPRRedundantCopies = prRedundantCopies != null;
      if (this.isSetPRRedundantCopies) {
        this.prRedundantCopies = prRedundantCopies;
        this.hasPartitionAttributes = true;
        userSpecifiedPartitionAttributes.add(CliStrings.CREATE_REGION__REDUNDANTCOPIES);
      }
      this.isSetPRStartupRecoveryDelay = prStartupRecoveryDelay != null;
      if (this.isSetPRStartupRecoveryDelay) {
        this.prStartupRecoveryDelay = prStartupRecoveryDelay;
        this.hasPartitionAttributes = true;
        userSpecifiedPartitionAttributes.add(CliStrings.CREATE_REGION__STARTUPRECOVERYDDELAY);
      }
      this.isSetPRTotalMaxMemory = prTotalMaxMemory != null;
      if (this.isSetPRTotalMaxMemory) {
        this.prTotalMaxMemory = prTotalMaxMemory;
        this.hasPartitionAttributes = true;
        userSpecifiedPartitionAttributes.add(CliStrings.CREATE_REGION__TOTALMAXMEMORY);
      }
      this.isSetPRTotalNumBuckets = prTotalNumBuckets != null;
      if (this.isSetPRTotalNumBuckets) {
        this.prTotalNumBuckets = prTotalNumBuckets;
        this.hasPartitionAttributes = true;
        userSpecifiedPartitionAttributes.add(CliStrings.CREATE_REGION__TOTALNUMBUCKETS);
      }
      this.isPartitionResolver = partitionResolver != null;
      if (this.isPartitionResolver) {
        this.partitionResolver = partitionResolver;
        this.hasPartitionAttributes = true;
        userSpecifiedPartitionAttributes.add(CliStrings.CREATE_REGION__PARTITION_RESOLVER);
      }
    }

    public Boolean hasPartitionAttributes() {
      return hasPartitionAttributes;
    }

    public String getUserSpecifiedPartitionAttributes() {
      return CliUtil.collectionToString(userSpecifiedPartitionAttributes, -1);
    }

    public String getPrColocatedWith() {
      return prColocatedWith;
    }

    public Integer getPrLocalMaxMemory() {
      return prLocalMaxMemory;
    }

    public Boolean isSetPRLocalMaxMemory() {
      return isSetPRLocalMaxMemory;
    }

    public Long getPrRecoveryDelay() {
      return prRecoveryDelay;
    }

    public Boolean isSetPRRecoveryDelay() {
      return isSetPRRecoveryDelay;
    }

    public Integer getPrRedundantCopies() {
      return prRedundantCopies;
    }

    public Boolean isSetPRRedundantCopies() {
      return isSetPRRedundantCopies;
    }

    public Long getPrStartupRecoveryDelay() {
      return prStartupRecoveryDelay;
    }

    public Boolean isSetPRStartupRecoveryDelay() {
      return isSetPRStartupRecoveryDelay;
    }

    public Long getPrTotalMaxMemory() {
      return prTotalMaxMemory;
    }

    public Boolean isSetPRTotalMaxMemory() {
      return isSetPRTotalMaxMemory;
    }

    public Integer getPrTotalNumBuckets() {
      return prTotalNumBuckets;
    }

    public Boolean isSetPRTotalNumBuckets() {
      return isSetPRTotalNumBuckets;
    }
  }

}
