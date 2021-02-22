/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.embl.cli;

import org.gbif.embl.util.DwcArchiveBuilder;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractIdleService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@SuppressWarnings("UnstableApiUsage")
public class EmblAdapterService extends AbstractIdleService {

  private static final Logger LOG = LoggerFactory.getLogger(EmblAdapterService.class);

  private static final int DEFAULT_START_HOUR = 0;
  private static final int DEFAULT_START_MINUTE = 0;
  private static final int DEFAULT_FREQUENCY = 7;

  private final ScheduledExecutorService scheduler;

  private final Integer frequencyInDays;
  private final Long initialDelay;
  private final EmblAdapterConfiguration config;
  private final DataSource dataSource;
  private final DwcArchiveBuilder archiveBuilder;

  public EmblAdapterService(EmblAdapterConfiguration config) {
    this.config = config;
    this.scheduler = Executors.newScheduledThreadPool(3);
    this.frequencyInDays = ObjectUtils.defaultIfNull(config.frequencyInDays, DEFAULT_FREQUENCY);

    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(config.db.url);
    hikariConfig.setUsername(config.db.user);
    hikariConfig.setPassword(config.db.password);
    hikariConfig.setMaximumPoolSize(config.db.maximumPoolSize);
    hikariConfig.setConnectionTimeout(config.db.connectionTimeout);

    this.dataSource = new HikariDataSource(hikariConfig);
    this.archiveBuilder = new DwcArchiveBuilder(dataSource, config.workingDirectory);

    Integer startHour;
    Integer startMinute;
    if (StringUtils.contains(config.startTime, ":")) {
      String[] timeParts = config.startTime.split(":");
      startHour = NumberUtils.toInt(timeParts[0], DEFAULT_START_HOUR);
      startMinute = NumberUtils.toInt(timeParts[1], DEFAULT_START_MINUTE);
    } else {
      startHour = null;
      startMinute = null;
    }

    long initialDelay = 0;
    if (startHour != null) {
      LocalTime t = LocalTime.of(startHour, startMinute);
      initialDelay = LocalTime.now().until(t, ChronoUnit.MINUTES);
    }

    // if the delay is passed then start it next day
    if (initialDelay < 0) {
      this.initialDelay = initialDelay + ChronoUnit.DAYS.getDuration().toMinutes();
    } else {
      this.initialDelay = initialDelay;
    }
  }

  @Override
  protected void startUp() {
    LOG.info("Service started");
    for (Task task : config.tasks) {
      scheduleTask(
          new ArchiveGeneratorFileSourceTask(
              task.name,
              task.requestUrl,
              task.archiveName,
              task.rawDataFile,
              config.workingDirectory,
              task.metadataFile,
              archiveBuilder));
    }
  }

  @Override
  protected void shutDown() {
    scheduler.shutdown();
  }

  private void scheduleTask(Runnable runnable) {
    scheduler.scheduleAtFixedRate(
        runnable,
        initialDelay,
        frequencyInDays * (ChronoUnit.DAYS.getDuration().toMinutes()),
        TimeUnit.MINUTES);
  }
}
