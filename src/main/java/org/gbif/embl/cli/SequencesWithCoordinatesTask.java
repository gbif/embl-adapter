package org.gbif.embl.cli;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.embl.api.EmblResponse;
import org.gbif.embl.client.EmblClient;
import org.gbif.embl.util.DbConnectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.gbif.embl.util.EmblAdapterConstants.INSERT;

public class SequencesWithCoordinatesTask implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(SequencesWithCoordinatesTask.class);

  private static final int LIMIT = 100;

  private final long numRecords;
  private final AtomicLong offset = new AtomicLong(0);
  private final EmblClient emblClient;

  public SequencesWithCoordinatesTask(long numRecords, EmblClient emblClient) {
    this.numRecords = numRecords;
    this.emblClient = emblClient;
  }

  @Override
  public void run() {
    LOG.info("SequencesWithCoordinatesTask started!");
    while (offset.get() < numRecords) {
      // decide on offset, limit
      LOG.info("Offset: {}, limit: {}", offset, LIMIT);

      // perform request by client
      // increment offset
      List<EmblResponse> emblResponseList =
          emblClient.searchSequencesWithCoordinates(new PagingRequest(offset.getAndAdd(LIMIT), LIMIT));

      // store data to DB
      try (Connection connection = DbConnectionUtils.dataSource.getConnection();
           Statement statement = connection.createStatement();
      ) {
        LOG.info("Start writing DB");
        connection.setAutoCommit(false);
        for (EmblResponse item : emblResponseList) {
          statement.addBatch(String.format(INSERT, item.getAccession(), item.getLocation(),
              item.getCountry().replace("'", "''"), item.getIdentifiedBy(), item.getCollectedBy(),
              item.getCollectionDate(), item.getSpecimenVoucher(), item.getSequenceMd5(), item.getScientificName(),
              item.getTaxId(), item.getAltitude(), item.getSex()));
        }
        statement.executeBatch();
        connection.commit();
        LOG.info("Finish writing DB");
      } catch (SQLException e) {
        // TODO: 23/11/2020 process or log somehow
        e.printStackTrace();
      }

      // TODO: 23/11/2020 notify about finish?
      LOG.info("SequencesWithCoordinatesTask finished!");
    }
  }
}
