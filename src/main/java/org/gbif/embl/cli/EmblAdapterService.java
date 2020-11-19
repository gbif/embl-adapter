package org.gbif.embl.cli;

import com.google.common.util.concurrent.AbstractIdleService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnstableApiUsage")
public class EmblAdapterService extends AbstractIdleService {

  private static final int DEFAULT_START_HOUR = 0;
  private static final int DEFAULT_START_MINUTE = 0;
  private static final int DEFAULT_FREQUENCY = 7;

  private final ScheduledExecutorService scheduler;
  private final EmblAdapter emblAdapter;

  private final Integer frequencyInDays;
  private final Integer startHour;
  private final Integer startMinute;

  public EmblAdapterService(EmblAdapterConfiguration config) {
    this.scheduler = Executors.newScheduledThreadPool(1);
    this.frequencyInDays = ObjectUtils.defaultIfNull(config.frequencyInDays, DEFAULT_FREQUENCY);
    this.emblAdapter = new EmblAdapter();
    if (StringUtils.contains(config.startTime, ":")) {
      String[] timeParts = config.startTime.split(":");
      this.startHour = NumberUtils.toInt(timeParts[0], DEFAULT_START_HOUR);
      this.startMinute = NumberUtils.toInt(timeParts[1], DEFAULT_START_MINUTE);
    } else {
      this.startHour = null;
      this.startMinute = null;
    }
  }

  @Override
  protected void startUp() {
    long initialDelay = 0;
    if (startHour != null && startMinute != null) {
      LocalTime t = LocalTime.of(startHour, startMinute);
      initialDelay = LocalTime.now().until(t, ChronoUnit.MINUTES);
    }

    // if the delay is passed then start it next day
    if (initialDelay < 0) {
      initialDelay = initialDelay + ChronoUnit.DAYS.getDuration().toMinutes();
    }

    scheduler.scheduleAtFixedRate(
        emblAdapter::joinSequences,
        initialDelay,
        frequencyInDays * (ChronoUnit.DAYS.getDuration().toMinutes()),
        TimeUnit.MINUTES);
  }

  @Override
  protected void shutDown() {
    scheduler.shutdown();
  }
}
