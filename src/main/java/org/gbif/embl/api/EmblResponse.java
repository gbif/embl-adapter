package org.gbif.embl.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.StringJoiner;

public class EmblResponse {

  private String accession;
  private String location;
  private String country;
  private String identifiedBy;
  private String collectedBy;
  private String collectionDate;
  private String specimenVoucher;
  private String sequenceMd5;
  private String scientificName;
  private String taxId;
  private String altitude;
  private String sex;

  public String getAccession() {
    return accession;
  }

  public void setAccession(String accession) {
    this.accession = accession;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  @JsonProperty("identified_by")
  public String getIdentifiedBy() {
    return identifiedBy;
  }

  public void setIdentifiedBy(String identifiedBy) {
    this.identifiedBy = identifiedBy;
  }

  @JsonProperty("collected_by")
  public String getCollectedBy() {
    return collectedBy;
  }

  public void setCollectedBy(String collectedBy) {
    this.collectedBy = collectedBy;
  }

  @JsonProperty("collection_date")
  public String getCollectionDate() {
    return collectionDate;
  }

  public void setCollectionDate(String collectionDate) {
    this.collectionDate = collectionDate;
  }

  @JsonProperty("specimen_voucher")
  public String getSpecimenVoucher() {
    return specimenVoucher;
  }

  public void setSpecimenVoucher(String specimenVoucher) {
    this.specimenVoucher = specimenVoucher;
  }

  @JsonProperty("sequence_md5")
  public String getSequenceMd5() {
    return sequenceMd5;
  }

  public void setSequenceMd5(String sequenceMd5) {
    this.sequenceMd5 = sequenceMd5;
  }

  @JsonProperty("scientific_name")
  public String getScientificName() {
    return scientificName;
  }

  public void setScientificName(String scientificName) {
    this.scientificName = scientificName;
  }

  @JsonProperty("tax_id")
  public String getTaxId() {
    return taxId;
  }

  public void setTaxId(String taxId) {
    this.taxId = taxId;
  }

  public String getAltitude() {
    return altitude;
  }

  public void setAltitude(String altitude) {
    this.altitude = altitude;
  }

  public String getSex() {
    return sex;
  }

  public void setSex(String sex) {
    this.sex = sex;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", EmblResponse.class.getSimpleName() + "[", "]")
        .add("accession='" + accession + "'")
        .add("location='" + location + "'")
        .add("country='" + country + "'")
        .add("identifiedBy='" + identifiedBy + "'")
        .add("collectedBy='" + collectedBy + "'")
        .add("collectionDate='" + collectionDate + "'")
        .add("specimenVoucher='" + specimenVoucher + "'")
        .add("sequenceMd5='" + sequenceMd5 + "'")
        .add("scientificName='" + scientificName + "'")
        .add("taxId='" + taxId + "'")
        .add("altitude='" + altitude + "'")
        .add("sex='" + sex + "'")
        .toString();
  }
}
