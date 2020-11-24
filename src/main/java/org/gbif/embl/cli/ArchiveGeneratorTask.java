package org.gbif.embl.cli;

import org.gbif.embl.api.EmblResponse;
import org.gbif.embl.util.DwcArchiveBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ArchiveGeneratorTask implements Runnable {

  private static final String ARCHIVE_NAME = "embl-archive_%s.zip";

  public ArchiveGeneratorTask() {
  }

  public void joinSequences() {
    // TODO: 24/11/2020 D data
    List<EmblResponse> result = new ArrayList<>();

    DwcArchiveBuilder dwcArchiveBuilder = new DwcArchiveBuilder();
    dwcArchiveBuilder.buildArchive(
        new File("output", String.format(ARCHIVE_NAME, new Date().getTime())), result);
  }

  @Override
  public void run() {

  }
}
