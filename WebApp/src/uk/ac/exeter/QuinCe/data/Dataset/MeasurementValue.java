package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Holds the calculated value of a given {@link SensorType} for a measurement.
 *
 * <p>
 * The calculated value is derived from one or more {@link SensorValue}s,
 * depending on the configuration of the instrument, the relative time of the
 * measurement and available {@link SensorValues}, and whether bad/questionable
 * values are being ignored.
 * </p>
 *
 * <p>
 * This class implements the most common calculation of a measurement value.
 * Some {@link SensorTypes} require more complex calculations (e.g. xCO2 with
 * standards, which also requires xH2O). These can be implemented by overriding
 * classes.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class MeasurementValue implements PlotPageTableValue {

  /**
   * The {@link SensorType} that this measurement value is for.
   */
  private final SensorType sensorType;

  /**
   * The IDs of the {@link SensorValue}s used to calculate the value.
   *
   * <p>
   * Note that the sensor values may not all belong to the specified
   * {@link #sensorType}: some {@link SensorType}s require calculation based on
   * other sensors (e.g. CO2 requires xH2O).
   * </p>
   */
  private List<Long> sensorValueIds;

  /**
   * The IDs of {@link SensorValue}s used to support the value calculation.
   *
   * <p>
   * This can be used for values that aren't directly used in the calculation,
   * such as those used in calibrations.
   * </p>
   */
  private List<Long> supportingSensorValueIds;

  /**
   * The number of calculations used to build this value.
   *
   * <p>
   * This is useful for combining multiple values, where a weighted mean is
   * often required.
   * </p>
   */
  private int memberCount = 0;

  /**
   * The calculated value for the sensor type.
   */
  private Double calculatedValue = Double.NaN;

  /**
   * The QC flag for this value, derived from the contributing
   * {@link SensorValues}.
   */
  private Flag flag = Flag.ASSUMED_GOOD;

  /**
   * The QC message for this value, derived from the contributing
   * {@link SensorValues}.
   */
  private List<String> qcMessage;

  /**
   * Miscellaneous properties
   */
  private Properties properties;

  /**
   * Creates a stub value with no assigned {@link SensorValue}s.
   *
   * @param sensorType
   *          The sensor type that the value is calculated for.
   */
  public MeasurementValue(SensorType sensorType) {
    this.sensorType = sensorType;
    this.sensorValueIds = new ArrayList<Long>();
    this.supportingSensorValueIds = new ArrayList<Long>();
    this.qcMessage = new ArrayList<String>();
    this.properties = new Properties();
  }

  /**
   * Construct a MeasurementValue using a full set of fields.
   *
   * @param sensorTypeId
   * @param sensorValueIds
   * @param supportingSensorValueIds
   * @param calculatedValue
   * @param flag
   * @param qcComments
   * @param properties
   * @throws SensorTypeNotFoundException
   */
  public MeasurementValue(long sensorTypeId, List<Long> sensorValueIds,
    List<Long> supportingSensorValueIds, int memberCount,
    Double calculatedValue, Flag flag, List<String> qcComments,
    Properties properties) throws SensorTypeNotFoundException {

    this.sensorType = ResourceManager.getInstance().getSensorsConfiguration()
      .getSensorType(sensorTypeId);
    this.sensorValueIds = sensorValueIds;
    this.supportingSensorValueIds = supportingSensorValueIds;
    this.memberCount = memberCount;
    this.calculatedValue = calculatedValue;
    this.flag = flag;
    this.qcMessage = qcComments;
    this.properties = properties;
  }

  /**
   * Construct a MeasurementValue, calculating the QC details from the supplied
   * {@link SensorValue}s.
   *
   * @param sensorType
   * @param sensorValues
   * @param calculatedValue
   * @param memberCount
   */
  public MeasurementValue(SensorType sensorType, List<SensorValue> sensorValues,
    Double calculatedValue, int memberCount) {

    this.sensorType = sensorType;
    this.sensorValueIds = new ArrayList<Long>();
    this.memberCount = memberCount;
    this.supportingSensorValueIds = new ArrayList<Long>();
    this.calculatedValue = calculatedValue;
    this.memberCount = memberCount;
    this.qcMessage = new ArrayList<String>();
    this.properties = new Properties();
    addSensorValues(sensorValues, false);
  }

  public void addSensorValues(Collection<SensorValue> values,
    boolean incrMemberCount) {
    values.forEach(v -> addSensorValue(v, incrMemberCount));
  }

  /**
   * Add the sensor values used in the specified {@code MeasurementValue}s.
   *
   * @param sourceValues
   *          The source {@code MeasurementValue}s.
   */
  public void addSensorValues(Collection<MeasurementValue> sourceValues,
    DatasetSensorValues allSensorValues, boolean incrMemberCount) {

    sourceValues
      .forEach(x -> addSensorValues(x, allSensorValues, incrMemberCount));
  }

  /**
   * Add the sensor values used in the specified {@code MeasurementValue}.
   *
   * @param sourceValues
   *          The source {@code MeasurementValue}.
   */
  public void addSensorValues(MeasurementValue sourceValue,
    DatasetSensorValues allSensorValues, boolean incrMemberCount) {
    for (Long sensorValueId : sourceValue.getSensorValueIds()) {
      addSensorValue(allSensorValues.getById(sensorValueId), incrMemberCount);
    }
  }

  /**
   * Add a {@link SensorValue} to the value.
   *
   * <p>
   * Adds the {@link SensorValue}'s ID and updates the QC information.
   * </p>
   *
   * @param value
   *          The value to add.
   * @param incrMemberCount
   *          Indicates whether or not this value should contribute to the
   *          member count.
   */
  public void addSensorValue(SensorValue value, boolean incrMemberCount) {
    if (!sensorValueIds.contains(value.getId())) {
      sensorValueIds.add(value.getId());

      Flag valueFlag = value.getDisplayFlag().getSimpleFlag();

      if (valueFlag.equals(flag)) {
        if (value.getUserQCMessage().trim().length() > 0) {
          qcMessage.add(value.getUserQCMessage());
        }
      } else if (valueFlag.moreSignificantThan(flag)) {
        flag = valueFlag;
        qcMessage.clear();

        if (value.getUserQCMessage().trim().length() > 0) {
          qcMessage.add(value.getUserQCMessage());
        }
      }

      if (incrMemberCount) {
        memberCount++;
      }
    }
  }

  public void addSupportingSensorValue(SensorValue value) {
    supportingSensorValueIds.add(value.getId());
  }

  public void addSupportingSensorValues(Collection<SensorValue> values) {
    values.forEach(this::addSupportingSensorValue);
  }

  public List<Long> getSupportingSensorValueIds() {
    return supportingSensorValueIds;
  }

  public void setCalculatedValue(Double caluclatedValue) {
    this.calculatedValue = caluclatedValue;
  }

  public Double getCalculatedValue() {
    return calculatedValue;
  }

  public Flag getQcFlag() {
    return flag;
  }

  public List<String> getQcMessages() {
    return qcMessage;
  }

  public SensorType getSensorType() {
    return sensorType;
  }

  public boolean hasValue() {
    return sensorValueIds.size() > 0;
  }

  public int getMemberCount() {
    return memberCount;
  }

  public List<Long> getSensorValueIds() {
    return sensorValueIds;
  }

  protected Properties getProperties() {
    return properties;
  }

  public void setProperty(String key, String value) {
    properties.setProperty(key, value);
  }

  public String getProperty(String key) {
    return properties.getProperty(key);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
      + ((calculatedValue == null) ? 0 : calculatedValue.hashCode());
    result = prime * result
      + ((sensorType == null) ? 0 : sensorType.hashCode());
    result = prime * result
      + ((sensorValueIds == null) ? 0 : sensorValueIds.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MeasurementValue other = (MeasurementValue) obj;
    if (calculatedValue == null) {
      if (other.calculatedValue != null)
        return false;
    } else if (!calculatedValue.equals(other.calculatedValue))
      return false;
    if (sensorType == null) {
      if (other.sensorType != null)
        return false;
    } else if (!sensorType.equals(other.sensorType))
      return false;
    if (sensorValueIds == null) {
      if (other.sensorValueIds != null)
        return false;
    } else if (!sensorValueIds.equals(other.sensorValueIds))
      return false;
    return true;
  }

  @Override
  public long getId() {
    return DatabaseUtils.NO_DATABASE_RECORD;
  }

  @Override
  public String getValue() {
    return calculatedValue.isNaN() ? ""
      : StringUtils.formatNumber(calculatedValue);
  }

  @Override
  public String getQcMessage() {
    return StringUtils.collectionToDelimited(qcMessage, ";");
  }

  @Override
  public boolean getFlagNeeded() {
    return false;
  }

  @Override
  public boolean isNull() {
    return calculatedValue.isNaN();
  }

  @Override
  public char getType() {
    return memberCount > 1 ? PlotPageTableValue.INTERPOLATED_TYPE
      : PlotPageTableValue.MEASURED_TYPE;
  }

  @Override
  public String toString() {
    return sensorType.getShortName() + " = " + calculatedValue;
  }
}
