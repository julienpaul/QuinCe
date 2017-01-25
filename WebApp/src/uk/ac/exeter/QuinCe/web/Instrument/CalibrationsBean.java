package uk.ac.exeter.QuinCe.web.Instrument;

import java.util.Date;
import java.util.List;

import uk.ac.exeter.QuinCe.data.CalibrationCoefficients;
import uk.ac.exeter.QuinCe.data.CalibrationStub;
import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.InstrumentStub;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.Instrument.CalibrationDB;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;
import uk.ac.exeter.QuinCe.web.validator.ExistingDateValidator;

/**
 * Bean for handling viewing and entry of sensor calibrations
 * @author Steve Jones
 *
 */
public class CalibrationsBean extends BaseManagedBean {

	/**
	 * Navigation to the calibration editor
	 */
	private static final String PAGE_CALIBRATION_EDITOR = "calibrationEditor";
	
	/**
	 * The list of calibration coefficients to be entered.
	 * There will be one entry for each of the instrument's sensors
	 */
	private List<CalibrationCoefficients> coefficients;
	
	/**
	 * The date of the calibration
	 */
	private Date calibrationDate;
	
	/**
	 * The ID of the calibration record being edited. If this is a new
	 * calibration, this will be NO_DATABASE_RECORD.
	 */
	private long chosenCalibration = DatabaseUtils.NO_DATABASE_RECORD;
	
	/**
	 * Empty constructor
	 */
	public CalibrationsBean() {
		// Do nothing
	}
	
	/**
	 * Clear the bean's data
	 */
	private void clearData() {
		calibrationDate = null;
		chosenCalibration = DatabaseUtils.NO_DATABASE_RECORD;
		coefficients = null;
		getSession().removeAttribute(ExistingDateValidator.ATTR_ALLOWED_DATE);
	}
	
	/**
	 * Set up a new, empty calibration and navigate to the editor
	 * and set the date to today.
	 * @return The navigation result
	 */
	public String newCalibration() {
		try {
			clearData();
			chosenCalibration = DatabaseUtils.NO_DATABASE_RECORD;
			calibrationDate = new Date();
			InstrumentStub instrStub = (InstrumentStub) getSession().getAttribute(InstrumentListBean.ATTR_CURRENT_INSTRUMENT);
			Instrument instrument = instrStub.getFullInstrument();
			coefficients = CalibrationCoefficients.initCalibrationCoefficients(instrument);
		} catch (Exception e) {
			internalError(e);
		}

		return PAGE_CALIBRATION_EDITOR;
	}
	
	/**
	 * Store a calibration in the database
	 * @return The navigation back to the calibrations list
	 */
	public String saveCalibration() {
		String result = InstrumentListBean.PAGE_CALIBRATIONS;
		
		try {
			if (chosenCalibration == DatabaseUtils.NO_DATABASE_RECORD) {
				CalibrationDB.addCalibration(ServletUtils.getDBDataSource(), getCurrentInstrumentID(), calibrationDate, coefficients);
			} else {
				CalibrationDB.updateCalibration(ServletUtils.getDBDataSource(), chosenCalibration, calibrationDate, coefficients);
			}
		} catch (Exception e) {
			result = internalError(e);
		} finally {
			try {
				clearData();
			} catch (Exception e) {
				return internalError(e);
			}
		}

		return result;
	}
	
	/**
	 * Cancels an edit action and returns to the calibrations list
	 * @return The navigation result
	 */
	public String cancelEdit() {
		try {
			clearData();
		} catch (Exception e) {
			return internalError(e);
		}
		return InstrumentListBean.PAGE_CALIBRATIONS;
	}
	
	/**
	 * Begin editing an existing calibration
	 * @return The navigation to the calibration editor page
	 */
	public String editCalibration() {
		try {
			CalibrationStub stub = CalibrationDB.getCalibrationStub(ServletUtils.getDBDataSource(), chosenCalibration);
			calibrationDate = stub.getDate();
			
			// Store the date in the session so the date validator knows to skip it
			getSession().setAttribute(ExistingDateValidator.ATTR_ALLOWED_DATE, calibrationDate);
			
			coefficients = CalibrationDB.getCalibrationCoefficients(ServletUtils.getDBDataSource(), stub);
			
		} catch (Exception e) {
			return internalError(e);
		}

		return PAGE_CALIBRATION_EDITOR;
	}
	
	public String deleteCalibration() {
		try {
			CalibrationDB.deleteCalibration(ServletUtils.getDBDataSource(), chosenCalibration);
			clearData();
		} catch (Exception e) {
			return internalError(e);
		}
		
		return InstrumentListBean.PAGE_CALIBRATIONS;
	}
	
	//////// *** GETTERS AND SETTERS *** ////////
	
	/**
	 * Returns the list of calibration coefficient objects
	 * @return The list of calibration coefficient objects
	 */
	public List<CalibrationCoefficients> getCoefficients() {
		return coefficients;
	}
	
	/**
	 * Returns the calibration date
	 * @return The calibration date
	 */
	public Date getCalibrationDate() {
		return calibrationDate;
	}
	
	/**
	 * Sets the calibration date
	 * @param calibrationDate The calibration date
	 */
	public void setCalibrationDate(Date calibrationDate) {
		this.calibrationDate = calibrationDate;
	}
	
	/**
	 * Returns a Date object representing today. Used
	 * to limit the calibration date picker.
	 * @return A Date object representing today
	 */
	public Date getToday() {
		return new Date();
	}
	
	/**
	 * Returns the list of calibrations
	 * @return The list of calibrations
	 * @throws ResourceException 
	 * @throws DatabaseException 
	 * @throws MissingParamException 
	 */
	public List<CalibrationStub> getCalibrationsList() throws MissingParamException, DatabaseException, ResourceException {
		return CalibrationDB.getCalibrationList(ServletUtils.getDBDataSource(), getCurrentInstrumentID());
	}

	/**
	 * Retrieve the ID of the current instrument from the session
	 * @return The current instrument ID
	 */
	private long getCurrentInstrumentID() {
		InstrumentStub instrStub = (InstrumentStub) getSession().getAttribute(InstrumentListBean.ATTR_CURRENT_INSTRUMENT);
		return instrStub.getId();
	}
	
	/**
	 * Returns the database ID of the chosen calibration
	 * @return The calibration ID
	 */
	public long getChosenCalibration() {
		return chosenCalibration;
	}
	
	/**
	 * Sets the database ID of the chosen calibration
	 * @param chosenCalibration The calibration ID
	 */
	public void setChosenCalibration(long chosenCalibration) {
		this.chosenCalibration = chosenCalibration;
	}
}
