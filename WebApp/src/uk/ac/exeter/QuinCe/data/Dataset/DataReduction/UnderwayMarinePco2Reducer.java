package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Data Reduction class for underway marine pCO₂
 *
 * @author Steve Jones
 *
 */
public class UnderwayMarinePco2Reducer extends DataReducer {

  private static List<CalculationParameter> calculationParameters = null;

  public UnderwayMarinePco2Reducer(Variable variable,
    Map<String, Properties> properties) {
    super(variable, properties);
  }

  @Override
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws Exception {

    Double intakeTemperature = measurement
      .getMeasurementValue("Intake Temperature").getCalculatedValue();
    Double salinity = measurement.getMeasurementValue("Salinity")
      .getCalculatedValue();
    Double equilibratorTemperature = measurement
      .getMeasurementValue("Equilibrator Temperature").getCalculatedValue();
    Double equilibratorPressure = measurement
      .getMeasurementValue("Equilibrator Pressure").getCalculatedValue();
    Double co2InGas = measurement.getMeasurementValue("xCO₂ (with standards)")
      .getCalculatedValue();

    Double pH2O = Calculators.calcPH2O(salinity, equilibratorTemperature);

    Double pCo2TEWet = Calculators.calcpCO2TEWet(co2InGas, equilibratorPressure,
      pH2O);
    Double fCo2TEWet = Calculators.calcfCO2(pCo2TEWet, co2InGas,
      equilibratorPressure, equilibratorTemperature);

    Double pCO2SST = calcCO2AtSST(pCo2TEWet, equilibratorTemperature,
      intakeTemperature);

    Double fCO2 = calcCO2AtSST(fCo2TEWet, equilibratorTemperature,
      intakeTemperature);

    // Store the calculated values
    record.put("ΔT", Math.abs(intakeTemperature - equilibratorTemperature));
    record.put("pH₂O", pH2O);
    record.put("pCO₂ TE Wet", pCo2TEWet);
    record.put("fCO₂ TE Wet", fCo2TEWet);
    record.put("pCO₂ SST", pCO2SST);
    record.put("fCO₂", fCO2);
  }

  /**
   * Calculates pCO<sub>2</sub> at the intake (sea surface) temperature. From
   * Takahashi et al. (2009)
   *
   * @param pco2TEWet
   *          The pCO<sub>2</sub> at equilibrator temperature
   * @param eqt
   *          The equilibrator temperature
   * @param sst
   *          The intake temperature
   * @return The pCO<sub>2</sub> at intake temperature
   */
  private Double calcCO2AtSST(Double co2AtEquilibrator, Double eqt,
    Double sst) {
    return co2AtEquilibrator
      * Math.exp(0.0423 * (Calculators.kelvin(sst) - Calculators.kelvin(eqt)));
  }

  @Override
  protected String[] getRequiredTypeStrings() {
    return new String[] { "Intake Temperature", "Salinity",
      "Equilibrator Temperature", "Equilibrator Pressure",
      "xCO₂ (with standards)" };
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    if (null == calculationParameters) {
      calculationParameters = new ArrayList<CalculationParameter>(6);

      calculationParameters
        .add(new CalculationParameter(makeParameterId(0), "ΔT",
          "Water-Equilibrator Temperature Difference", "DELTAT", "°C", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(1),
        "pH₂O", "Marine Water Vapour Pressure", "RH2OX0EQ", "hPa", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(2),
        "pCO₂ TE Wet", "pCO₂ In Water - Equilibrator Temperature", "PCO2IG02",
        "μatm", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(3),
        "fCO₂ TE Wet", "fCO₂ In Water - Equilibrator Temperature", "FCO2IG02",
        "μatm", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(4),
        "pCO₂ SST", "pCO₂ In Water", "PCO2TK02", "μatm", true));

      calculationParameters.add(new CalculationParameter(makeParameterId(5),
        "fCO₂", "fCO₂ In Water", "FCO2XXXX", "μatm", true));
    }

    return calculationParameters;
  }
}
