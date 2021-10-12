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

import java.util.StringJoiner;

import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;

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
        .add("maximumPoolSize=" + maximumPoolSize)
        .add("connectionTimeout=" + connectionTimeout)
        .toString();
  }
}
