package org.gbif.embl.cli;

import org.gbif.embl.api.EmblResponse;
import org.gbif.embl.util.DbConnectionUtils;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.gbif.embl.util.EmblAdapterConstants.INSERT;

public abstract class SequencesTask implements Runnable {

  public static final int LIMIT = 100;

  private final long numRecords;

  public SequencesTask(long numRecords) {
    this.numRecords = numRecords;
  }

  @Override
  public void run() {
    getLog().info("Task started");
    getLog().info("Records to retrieve: {}. Offset initial: {}", numRecords, getOffset().get());
    while (getOffset().get() < numRecords) {
      // decide on offset, limit
      getLog().info("Iteration: Offset: {}, limit: {}", getOffset().get(), LIMIT);

      // perform request by client
      // increment offset
      List<EmblResponse> emblResponseList = getEmblData();

      // store data to DB
      try (Connection connection = DbConnectionUtils.dataSource.getConnection();
           Statement statement = connection.createStatement()) {
        getLog().debug("Start writing DB");
        connection.setAutoCommit(false);
        for (EmblResponse item : emblResponseList) {
          statement.addBatch(String.format(INSERT, item.getAccession(), item.getLocation(),
              item.getCountry().replace("'", "''"), item.getIdentifiedBy(), item.getCollectedBy(),
              item.getCollectionDate(), item.getSpecimenVoucher().replace("'", "''"),
              item.getSequenceMd5(), item.getScientificName().replace("'", "''"),
              item.getTaxId(), item.getAltitude(), item.getSex()));
        }
        statement.executeBatch();
        connection.commit();
        getLog().debug("Finish writing DB");
      } catch (SQLException e) {
        e.printStackTrace();
        getLog().error("Exception", e);
      }
    }

    // TODO: 23/11/2020 notify about finish?
    getOffset().set(0L);
    getLog().info("Task finished, offset reset to {}", getOffset().get());
  }

  protected abstract List<EmblResponse> getEmblData();

  protected abstract AtomicLong getOffset();

  protected abstract Logger getLog();
}
