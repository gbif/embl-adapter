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

@SuppressWarnings("PublicField")
public class TaskConfiguration {

  @NotNull public String name;

  @NotNull public String metadataFile;

  @NotNull public RequestConfiguration request1;

  @NotNull public RequestConfiguration request2;

  @NotNull public String rawDataFile1;

  @NotNull public String rawDataFile2;

  @NotNull public String archiveName;

  @NotNull public String tableName;

  @NotNull public String query;

  @Override
  public String toString() {
    return new StringJoiner(", ", TaskConfiguration.class.getSimpleName() + "[", "]")
        .add("name='" + name + "'")
        .add("metadataFile='" + metadataFile + "'")
        .add("request1='" + request1 + "'")
        .add("request2='" + request2 + "'")
        .add("rawDataFile1='" + rawDataFile1 + "'")
        .add("rawDataFile2='" + rawDataFile2 + "'")
        .add("archiveName='" + archiveName + "'")
        .add("tableName='" + tableName + "'")
        .add("query='" + query + "'")
        .toString();
  }
}
