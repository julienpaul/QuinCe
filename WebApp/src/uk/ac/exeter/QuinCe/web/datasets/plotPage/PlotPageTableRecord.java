package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.RoutineException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * A representation of a record in the plot page table.
 *
 * <p>
 * Columns are added using the {@code addColumn} methods. The class assumes that
 * all columns are added in display order.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class PlotPageTableRecord {

  /**
   * The key for the row ID
   */
  protected static final String ID_KEY = "DT_RowId";

  /**
   * The record ID
   */
  private final String id;

  /**
   * The record's columns
   */
  private Map<Integer, PlotPageTableColumn> columns = new HashMap<Integer, PlotPageTableColumn>();

  /**
   * The column index to give to the next added column.
   */
  private int nextColumnIndex = 0;

  /**
   * Simple constructor with a direct ID
   *
   * @param id
   *          The record ID
   */
  public PlotPageTableRecord(String id) {
    this.id = id;
  }

  public PlotPageTableRecord(LocalDateTime id) {
    this.id = String.valueOf(DateTimeUtils.dateToLong(id));
  }

  /**
   * Add a timestamp column
   *
   * @param value
   *          The time value.
   * @param used
   *          Whether or not the value is used in a calculation.
   * @param qcFlag
   *          The QC flag.
   * @param qcMessage
   *          The QC message.
   * @param flagNeeded
   *          Indicates whether or not a user QC flag is needed.
   */
  public void addColumn(LocalDateTime value, boolean used, Flag qcFlag,
    String qcMessage, boolean flagNeeded) {

    addColumn(DateTimeUtils.formatDateTime(value), used, qcFlag, qcMessage,
      flagNeeded);
  }

  public void addColumn(SensorValue sensorValue, boolean used)
    throws RoutineException {
    addColumn(sensorValue.getValue(), used, sensorValue.getDisplayFlag(),
      sensorValue.getDisplayQCMessage(), sensorValue.flagNeeded());
  }

  /**
   * Add a column.
   *
   * @param value
   *          The time value.
   * @param used
   *          Whether or not the value is used in a calculation.
   * @param qcFlag
   *          The QC flag.
   * @param qcMessage
   *          The QC message.
   * @param flagNeeded
   *          Indicates whether or not a user QC flag is needed.
   */
  public void addColumn(String value, boolean used, Flag qcFlag,
    String qcMessage, boolean flagNeeded) {

    columns.put(nextColumnIndex,
      new PlotPageTableColumn(value, used, qcFlag, qcMessage, flagNeeded));
    nextColumnIndex++;
  }

  public void addBlankColumn() {
    addColumn("", false, Flag.GOOD, null, false);
  }

  public void addBlankColumns(int count) {
    for (int i = 0; i < count; i++) {
      addBlankColumn();
    }
  }

  protected String getId() {
    return id;
  }

  protected Map<Integer, PlotPageTableColumn> getColumns() {
    return columns;
  }
}