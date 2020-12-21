package org.gbif.embl.cli;

import com.google.common.util.concurrent.Service;
import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import org.kohsuke.MetaInfServices;

@SuppressWarnings({"UnstableApiUsage", "unused"})
@MetaInfServices(Command.class)
public class EmblAdapterCommand extends ServiceCommand {

  private final EmblAdapterConfiguration config = new EmblAdapterConfiguration();

  public EmblAdapterCommand() {
    super("embl-adapter");
  }

  @Override
  protected Service getService() {
    return new EmblAdapterService(config);
  }

  @Override
  protected Object getConfigurationObject() {
    return config;
  }
}
