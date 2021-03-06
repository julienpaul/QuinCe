package uk.ac.exeter.QuinCe.data.Dataset.QC.Routines;

import java.lang.reflect.Method;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;

/**
 * A QC flag generated by a QC routine
 *
 * @author Steve Jones
 *
 */
public class RoutineFlag extends Flag {

  /**
   * The routine that generated this flag
   */
  private String routineName;

  /**
   * The value required by the routine
   */
  private String requiredValue;

  /**
   * The actual value
   */
  private String actualValue;

  /**
   * Basic constructor
   *
   * @param routine
   *          The routine that generated this flag
   * @param flag
   *          The flag
   */
  public RoutineFlag(Routine routine, Flag flag, String requiredValue,
    String actualValue) {
    super(flag);
    this.routineName = QCRoutinesConfiguration.getRoutineName(routine);
    this.requiredValue = requiredValue;
    this.actualValue = actualValue;
  }

  /**
   * Get the Class for the routine that raised this flag
   *
   * @return The Routine Class
   * @throws ClassNotFoundException
   *           If the class does not exist
   */
  private Class<? extends Routine> getRoutineClass()
    throws ClassNotFoundException {
    return QCRoutinesConfiguration.getRoutineClass(routineName);
  }

  /**
   * Get the short message for the routine attached to this flag
   *
   * @return The message
   * @throws RoutineException
   *           If the message cannot be retrieved
   */
  public String getShortMessage() throws RoutineException {
    try {
      Method method = getRoutineClass().getMethod("getShortMessage");
      return (String) method.invoke(null);
    } catch (Exception e) {
      throw new RoutineException("Cannot call getShortMessage", e);
    }
  }

  /**
   * Get the short message for the routine attached to this flag
   *
   * @return The message
   * @throws RoutineException
   *           If the message cannot be retrieved
   */
  public String getLongMessage() throws RoutineException {
    try {
      Method method = getRoutineClass().getMethod("getLongMessage",
        String.class, String.class);
      return (String) method.invoke(null, requiredValue, actualValue);
    } catch (Exception e) {
      throw new RoutineException("Cannot call getLongMessage", e);
    }
  }

  @Override
  public Flag getSimpleFlag() {
    try {
      return new Flag(this.flagValue);
    } catch (InvalidFlagException e) {
      // I don't know how this would happen, but just in case...
      throw new RuntimeException(e);
    }
  }
}
