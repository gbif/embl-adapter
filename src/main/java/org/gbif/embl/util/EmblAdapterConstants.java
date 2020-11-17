package org.gbif.embl.util;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public final class EmblAdapterConstants {

  public static final String METADATA_FILENAME = "metadata.xml";
  public static final String CORE_FILENAME = "occurrence.txt";
  public static final String DESCRIPTOR_FILENAME = "meta.xml";

  public static final Pattern LOCATION_PATTERN = Pattern.compile("([0-9.]+\\s+\\w)\\s+([0-9.]+\\s+\\w)");

  public static final String DEFAULT_DELIMITER = "\t";
  public static final String COUNTRY_DELIMITER = ":";
  public static final String TAXON_ID_PREFIX = "ASV:";

  public static final String PRESERVED_SPECIMEN = "PreservedSpecimen";
  public static final String MATERIAL_SAMPLE = "MaterialSample";

  // TODO: 16/11/2020 no such term 'references'
  public static final List<Term> TERMS = Arrays.asList(DwcTerm.occurrenceID, DwcTerm.associatedSequences,
      DwcTerm.decimalLatitude, DwcTerm.decimalLongitude, DwcTerm.country, DwcTerm.locality, DwcTerm.identifiedBy,
      DwcTerm.recordedBy, DwcTerm.eventDate, DwcTerm.catalogNumber, DwcTerm.basisOfRecord, DwcTerm.taxonID,
      DwcTerm.scientificName, DwcTerm.taxonConceptID, DwcTerm.minimumElevationInMeters,
      DwcTerm.maximumElevationInMeters, DwcTerm.sex);

  private EmblAdapterConstants() {
  }
}
