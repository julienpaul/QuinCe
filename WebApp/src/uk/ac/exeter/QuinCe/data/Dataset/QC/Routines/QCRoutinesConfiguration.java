package uk.ac.exeter.QuinCe.data.Dataset.QC.Routines;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

public class QCRoutinesConfiguration {

  /**
   * The name of the package in which all routine classes will be stored
   */
  private static final String ROUTINE_CLASS_ROOT = "uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.";

  /**
   * All routine class names must end with the same text
   */
  private static final String ROUTINE_CLASS_TAIL = "Routine";

  /**
   * The QC routines
   */
  private Map<SensorType, List<Routine>> routines;

  /**
   * Main constructor - parses supplied config file and builds all Routine
   * objects.
   *
   * @param configFile
   *          The configuration file
   * @throws QCRoutinesConfigurationException
   *           If the configuration is invalid
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public QCRoutinesConfiguration(SensorsConfiguration sensorsConfig,
    String configFile)
    throws QCRoutinesConfigurationException, MissingParamException {

    MissingParam.checkMissing(configFile, "configFile");
    routines = new HashMap<SensorType, List<Routine>>();
    init(sensorsConfig, configFile);
  }

  /**
   * Add a Routine to the configuration
   *
   * @param sensorType
   *          The target sensor type
   * @param routine
   *          The routine
   */
  private void addRoutine(SensorType sensorType, Routine routine) {
    if (!routines.containsKey(sensorType)) {
      routines.put(sensorType, new ArrayList<Routine>());
    }

    routines.get(sensorType).add(routine);
  }

  /**
   * Initialise the configuration from the supplied file
   *
   * @param configFilename
   *          The filename
   * @throws QCRoutinesConfigurationException
   *           If the configuration is invalid
   */
  private void init(SensorsConfiguration sensorsConfig, String configFilename)
    throws QCRoutinesConfigurationException {

    try (Reader in = new FileReader(configFilename)) {
      Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(in);

      for (CSVRecord record : records) {
        String routineName = record.get("Class");
        String sensorTypeName = record.get("Sensor Type");
        List<String> parameters = new ArrayList<String>(record.size() - 2);
        for (int i = 2; i < record.size(); i++) {
          parameters.add(record.get(i));
        }

        String fullClassName = getFullClassName(routineName);

        if (routineName.equalsIgnoreCase("")) {
          throw new QCRoutinesConfigurationException(configFilename,
            record.getRecordNumber(), "Routine class name cannot be empty");
        } else if (sensorTypeName.equalsIgnoreCase("")) {
          throw new QCRoutinesConfigurationException(configFilename,
            record.getRecordNumber(), "Sensor Type name cannot be empty");
        } else {
          try {

            SensorType sensorType = sensorsConfig.getSensorType(sensorTypeName);

            // Instantiate the routine class
            Class<?> routineClass = Class.forName(fullClassName);

            @SuppressWarnings("unchecked")
            Constructor<Routine> constructor = (Constructor<Routine>) routineClass
              .getConstructor(List.class);

            Routine instance = constructor.newInstance(parameters);

            addRoutine(sensorType, instance);

          } catch (SensorTypeNotFoundException e) {
            throw new QCRoutinesConfigurationException(configFilename,
              record.getRecordNumber(),
              "Sensor Type '" + sensorTypeName + "' does not exist");
          } catch (ClassNotFoundException e) {
            throw new QCRoutinesConfigurationException(configFilename,
              record.getRecordNumber(),
              "Routine check class '" + fullClassName + "' does not exist");
          } catch (Exception e) {
            throw new QCRoutinesConfigurationException(configFilename,
              record.getRecordNumber(), "Error creating Routine check class",
              e);
          }
        }
      }
    } catch (IOException e) {
      throw new QCRoutinesConfigurationException(configFilename,
        "I/O Error while reading file", e);
    }
  }

  /**
   * Get the QC routines for a given sensor type
   *
   * @param sensorType
   *          The sensor type
   * @return The routines to be run
   */
  public List<Routine> getRoutines(SensorType sensorType) {
    List<Routine> result = routines.get(sensorType);
    if (null == result) {
      result = new ArrayList<Routine>();
    }

    return result;
  }

  /**
   * Get the full class name from a routine name
   *
   * @param routineName
   *          The routine name
   * @return The full class name
   */
  private static String getFullClassName(String routineName) {
    return ROUTINE_CLASS_ROOT + routineName + ROUTINE_CLASS_TAIL;
  }

  /**
   * Get the shortcut name of a concrete Routine class, for storage in the
   * database.
   *
   * This is the class name without the package prefix, and with the word
   * 'Routine' stripped off the end
   *
   * @param clazz
   *          The class
   * @return The shortcut name.
   */
  public static String getRoutineName(Class<? extends Routine> clazz) {
    return clazz.getSimpleName().replaceAll("Routine$", "");
  }

  /**
   * Get the shortcut name of a concrete Routine instance, for storage in the
   * database.
   *
   * This is the class name without the package prefix, and with the word
   * 'Routine' stripped off the end
   *
   * @param routine
   *          The instance
   * @return The shortcut name.
   */
  public static String getRoutineName(Routine routine) {
    return getRoutineName(routine.getClass());
  }

  /**
   * Get the Class object for a routine given its name
   *
   * @param routineName
   *          The routine name
   * @return The Routine class
   * @throws ClassNotFoundException
   *           If the class does not exist
   */
  @SuppressWarnings("unchecked")
  public static Class<? extends Routine> getRoutineClass(String routineName)
    throws ClassNotFoundException {
    return (Class<? extends Routine>) Class
      .forName(getFullClassName(routineName));
  }
}
