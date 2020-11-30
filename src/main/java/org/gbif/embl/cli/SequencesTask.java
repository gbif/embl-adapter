package org.gbif.embl.cli;

import org.gbif.embl.api.EmblResponse;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

import static org.gbif.embl.util.EmblAdapterConstants.INSERT;

public abstract class SequencesTask implements Runnable {

  private final DataSource dataSource;
  private final CyclicBarrier barrier;
  private final long numRecords;
  private final int limit;

  public SequencesTask(
      DataSource dataSource,
      CyclicBarrier barrier,
      long numRecords,
      int limit) {
    this.dataSource = dataSource;
    this.barrier = barrier;
    this.numRecords = numRecords;
    this.limit = limit;
  }

  @Override
  public void run() {
    getLog().info("Task started");
    getLog().debug("Records to retrieve: {}. Offset initial: {}", numRecords, getOffset().get());
    while (getOffset().get() < numRecords) {
      getLog().debug("Iteration: Offset: {}, limit: {}", getOffset().get(), limit);

      // perform request by client
      // increment offset
      List<EmblResponse> emblResponseList = getEmblData();

      // store data to DB
      try (Connection connection = dataSource.getConnection();
           Statement statement = connection.createStatement()) {
        getLog().debug("Start writing DB");
        connection.setAutoCommit(false);
        for (EmblResponse item : emblResponseList) {
          statement.addBatch(String.format(INSERT,
              item.getAccession(),
              escapeApostrophes(item.getLocation()),
              escapeApostrophes(item.getCountry()),
              escapeApostrophes(item.getIdentifiedBy()),
              escapeApostrophes(item.getCollectedBy()),
              item.getCollectionDate(),
              escapeApostrophes(item.getSpecimenVoucher()),
              item.getSequenceMd5(),
              escapeApostrophes(item.getScientificName()),
              item.getTaxId(),
              item.getAltitude(),
              item.getSex()));
        }
        statement.executeBatch();
        connection.commit();
        getLog().debug("Finish writing DB");
      } catch (SQLException e) {
        getLog().error("Exception", e);
      }
    }

//    getOffset().set(0L);
    getLog().info("Task finished, reset offset to {}", getOffset().get());

    try {
      String threadName = Thread.currentThread().getName();
      getLog().info(
          "thread {} barrier waiting; total number of waiting tasks: {}", threadName, barrier.getNumberWaiting());
      barrier.await();
    } catch (InterruptedException | BrokenBarrierException e) {
      getLog().error("Exception while waiting other tasks", e);
    }
  }

  private String escapeApostrophes(String str) {
    return str.replaceAll("'", "''");
  }

  protected abstract List<EmblResponse> getEmblData();

  protected abstract AtomicLong getOffset();

  protected abstract Logger getLog();
}
