package org.gbif.embl.cli;

import com.beust.jcommander.Parameter;

import javax.validation.constraints.NotNull;
import java.util.StringJoiner;

@SuppressWarnings("PublicField")
public class DbConfiguration {

  @NotNull
  @Parameter(names = "--db-url")
  public String url;

  @NotNull
  @Parameter(names = "--db-user")
  public String user;

  @NotNull
  @Parameter(names = "--db-password", password = true)
  public String password;

  @Parameter(names = "--db-maximumPoolSize")
  public int maximumPoolSize = 3;

  @Parameter(names = "--db-connectionTimeout")
  public int connectionTimeout = 3000;

  @Override
  public String toString() {
    return new StringJoiner(", ", DbConfiguration.class.getSimpleName() + "[", "]")
        .add("url='" + url + "'")
        .add("user='" + user + "'")
        .add("password='" + password + "'")
        .add("maximumPoolSize=" + maximumPoolSize)
        .add("connectionTimeout=" + connectionTimeout)
        .toString();
  }
}
