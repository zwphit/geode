package org.apache.geode.internal.logging.log4j;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.status.StatusLogger;

import org.apache.geode.internal.logging.LogService;
import org.apache.geode.internal.logging.log4j.custom.BasicAppender;
import org.apache.geode.test.junit.rules.serializable.SerializableExternalResource;

public class LoggingConfigurationRule extends SerializableExternalResource {

  private String beforeConfigFileProp;
  private Level beforeLevel;

  /**
   * @param name name of the resource to use as logging config
   */
  public LoggingConfigurationRule(final String name) {
    assertThat(getClass().getResource(name)).isNotNull();
    return new Configuration(getClass().getResource(name), name).createConfigFileIn(this.temporaryFolder.getRoot());
  }

  @Override
  protected void before() throws Throwable {
    Configurator.shutdown();
    BasicAppender.clearInstance();

    this.beforeConfigFileProp = System.getProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
    this.beforeLevel = StatusLogger.getLogger().getLevel();
  }

  @Override
  protected void after() {
    Configurator.shutdown();

    System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
    if (this.beforeConfigFileProp != null) {
      System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, this.beforeConfigFileProp);
    }
    StatusLogger.getLogger().setLevel(this.beforeLevel);

    LogService.reconfigure();
    assertThat(LogService.isUsingGemFireDefaultConfig()).as(LogService.getConfigInformation()).isTrue();

    BasicAppender.clearInstance();
  }
}
