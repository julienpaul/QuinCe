package junit.uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneOffset;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorConfigurationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

public class SensorTypeTest extends BaseTest {

  /**
   * Create a simple, valid SensorType
   * @return The SensorType
   * @throws Exception If it can't be built
   */
  private SensorType getBasicSensorType() throws Exception {
    return new SensorType(1L, "Name", null, null, null, false, false);
  }

  @ParameterizedTest
  @ValueSource(longs = {0L, -1L})
  public void invalidIdsTest(long id) {
    assertThrows(MissingParamException.class, () -> {
      new SensorType(id, "Name", null, null, null, false, false);
    });
  }

  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void invalidNamesTest(String name) {
    assertThrows(MissingParamException.class, () -> {
      new SensorType(1L, name, null, null, null, false, false);
    });
  }

  @SuppressWarnings("unused")
  private static Stream<Long> createInvalidReferences() {
    return Stream.of(0L, -1L);
  }

  @ParameterizedTest
  @MethodSource("createInvalidReferences")
  public void invalidParentTest(Long parent) {
    assertThrows(MissingParamException.class, () -> {
      new SensorType(1, "Name", parent, null, null, false, false);
    });
  }

  @ParameterizedTest
  @MethodSource("createInvalidReferences")
  public void invalidDependsOnTest(Long dependsOn) {
    assertThrows(MissingParamException.class, () -> {
      new SensorType(1, "Name", null, dependsOn, null, false, false);
    });
  }

  @Test
  public void idTest() throws Exception {
    SensorType type = getBasicSensorType();
    assertEquals(1L, type.getId());
  }

  @Test
  public void nameTest() throws Exception {
    SensorType type = getBasicSensorType();
    assertEquals("Name", type.getName());
  }

  @Test
  public void nullParentTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", null, null, null, false, false);
    assertEquals(SensorType.NO_PARENT, type.getParent());
    assertFalse(type.hasParent());
  }

  @Test
  public void ownParentTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      new SensorType(1L, "Name", 1L, null, null, true, false);
    });
  }

  @Test
  public void validParentTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", 2L, null, null, false, false);
    assertEquals(2L, type.getParent());
    assertTrue(type.hasParent());
  }

  @Test
  public void nullDependsOnTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", null, null, null, false, false);
    assertEquals(SensorType.NO_DEPENDS_ON, type.getDependsOn());
    assertFalse(type.dependsOnOtherType());
  }

  @Test
  public void ownDependsOnTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      new SensorType(1L, "Name", null, 1L, null, true, false);
    });
  }

  @Test
  public void dependsOnTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", null, 2L, null, false, false);
    assertEquals(2L, type.getDependsOn());
    assertTrue(type.dependsOnOtherType());
  }

  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void nullDependsQuestionTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", null, null, null, false, false);
    assertNull(type.getDependsQuestion());
    assertFalse(type.hasDependsQuestion());
  }

  @Test
  public void dependsQuestionWithoutDependsOnTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      new SensorType(1L, "Name", null, null, "Question?", false, false);
    });
  }

  @Test
  public void dependsQuestionTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", null, 2L, "Question?", false, false);
    assertNotNull(type.getDependsQuestion());
    assertTrue(type.hasDependsQuestion());
  }

  @Test
  public void notDiagnosticTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", null, null, null, false, false);
    assertFalse(type.isDiagnostic());
  }

  @Test
  public void isDiagnosticTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", null, null, null, false, true);
    assertTrue(type.isDiagnostic());
  }

  @Test
  public void notInternalCalibrationTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", null, null, null, false, false);
    assertFalse(type.hasInternalCalibration());
  }

  @Test
  public void internalCalibrationTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", null, null, null, true, false);
    assertTrue(type.hasInternalCalibration());
  }

  @Test
  public void equalsRandomObjectTest() throws Exception {
    SensorType type = getBasicSensorType();
    assertFalse(type.equals(ZoneOffset.UTC));
  }

  @SuppressWarnings("unlikely-arg-type")
  @Test
  public void equalsWrongNameStringTest() throws Exception {
    SensorType type = getBasicSensorType();
    assertFalse(type.equals("Flurble"));
  }

  @SuppressWarnings("unlikely-arg-type")
  @Test
  public void equalsActualNameStringTest() throws Exception {
    SensorType type = getBasicSensorType();
    assertFalse(type.equals("Name"));
  }

  @Test
  public void equalsSensorTypeTest() throws Exception {
    SensorType type = getBasicSensorType();
    SensorType type2 = getBasicSensorType();
    assertTrue(type.equals(type2));
  }

  @Test
  public void notEqualsSensorTypeTest() throws Exception {
    SensorType type = getBasicSensorType();
    SensorType type2 = new SensorType(4L, "Flurble", null, null, null, false, false);
    assertFalse(type.equals(type2));
  }

  @Test
  public void compareToTest() throws Exception {
    SensorType type = getBasicSensorType();
    SensorType type2 = new SensorType(4L, "Flurble", null, null, null, false, false);
    assertTrue(type2.compareTo(type) < 0);
    assertTrue(type.compareTo(type2) > 0);
    assertTrue(type.compareTo(type) == 0);
  }

  @Test
  public void toStringTest() throws Exception {
    SensorType type = getBasicSensorType();
    assertEquals("Sensor Type: Name", type.toString());
  }
}