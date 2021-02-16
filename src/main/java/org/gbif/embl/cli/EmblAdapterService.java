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

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractIdleService;

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

  public EmblAdapterService(EmblAdapterConfiguration config) {
    this.config = config;
    this.scheduler = Executors.newScheduledThreadPool(3);
    this.frequencyInDays = ObjectUtils.defaultIfNull(config.frequencyInDays, DEFAULT_FREQUENCY);

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
    scheduleTask(
        new ArchiveGeneratorTask(
            "datasets_for_edna",
            config.datasetForEdnaRequestUrl,
            config.datasetForEdnaArchiveName,
            config.datasetForEdnaRawDataFile,
            config.workingDirectory,
            config.datasetForEdnaMetadataFile));
    scheduleTask(
        new ArchiveGeneratorTask(
            "datasets_for_organism_sequenced",
            config.datasetForOrganismSequencedRequestUrl,
            config.datasetForOrganismSequencedArchiveName,
            config.datasetForOrganismSequencedRawDataFile,
            config.workingDirectory,
            config.datasetForOrganismSequencedMetadataFile));
    scheduleTask(
        new ArchiveGeneratorTask(
            "datasets_with_hosts",
            config.datasetWithHostsRequestUrl,
            config.datasetWithHostsArchiveName,
            config.datasetWithHostsRawDataFile,
            config.workingDirectory,
            config.datasetWithHostsMetadataFile));
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
