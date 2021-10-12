/*
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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

public class TaxonomyConfiguration {

  @ParametersDelegate @Valid @NotNull public DbConfiguration db = new DbConfiguration();

  @NotNull
  @Parameter(names = "--start-time")
  public String startTime;

  @NotNull
  @Parameter(names = "--frequency-in-days")
  public Integer frequencyInDays = 7;

  @NotNull public String archiveUrl;

  @NotNull public String archiveName;

  @NotNull public String tempDir;

  @NotNull public String extractedDir;

  @Override
  public String toString() {
    return new StringJoiner(", ", TaxonomyConfiguration.class.getSimpleName() + "[", "]")
        .add("db='" + db + "'")
        .add("startTime='" + startTime + "'")
        .add("frequencyInDays='" + frequencyInDays + "'")
        .add("archiveUrl='" + archiveUrl + "'")
        .add("archiveName='" + archiveName + "'")
        .add("tempDir='" + tempDir + "'")
        .add("extractedDir='" + extractedDir + "'")
        .toString();
  }
}
