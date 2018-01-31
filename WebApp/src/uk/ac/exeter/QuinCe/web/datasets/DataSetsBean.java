package uk.ac.exeter.QuinCe.web.datasets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;
import uk.ac.exeter.QuinCe.jobs.files.DataReductionJob;
import uk.ac.exeter.QuinCe.jobs.files.ExtractDataSetJob;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Bean for handling the creation and management of data sets
 * @author Steve Jones
 */
@ManagedBean
@SessionScoped
public class DataSetsBean extends BaseManagedBean {

	/**
	 * Navigation string for the New Dataset page
	 */
	private static final String NAV_NEW_DATASET = "new_dataset";
	
	/**
	 * Navigation string for the datasets list
	 */
	private static final String NAV_DATASET_LIST = "dataset_list";
	
	/**
	 * The data sets for the current instrument
	 */
	private List<DataSet> dataSets;
	
	/**
	 * The file definitions for the current instrument in JSON format for the timeline
	 */
	private String fileDefinitionsJson;

	/**
	 * The data sets and data files for the current instrument in JSON format
	 */
	private String timelineEntriesJson;
	
	/**
	 * The data set being created
	 */
	private DataSet newDataSet;

	/**
	 * The ID of the data set being processed
	 */
	private long datasetId;

	/**
	 * Plaform code of the instrument for this dataset
	 */
	private String platformCode = null;
		
	/**
	 * Initialise/Reset the bean
	 */
	@PostConstruct
	public void initialise() {
		try {
			initialiseInstruments();
			if (null == currentFullInstrument && -1 != getCurrentInstrument()) {
				currentFullInstrument = InstrumentDB.getInstrument(getDataSource(), getCurrentInstrument(), ServletUtils.getResourceManager().getSensorsConfiguration(), ServletUtils.getResourceManager().getRunTypeCategoryConfiguration());
			}
			loadDataSets();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Start the dataset definition procedure
	 * @return The navigation to the dataset definition page
	 */
	public String startNewDataset() {
		initialise();
		newDataSet = new DataSet(currentFullInstrument.getDatabaseId());
		fileDefinitionsJson = null;
		timelineEntriesJson = null;

		return NAV_NEW_DATASET;
	}
	
	/**
	 * Navigate to the datasets list
	 * @return The navigation string
	 */
	public String goToList() {
		return NAV_DATASET_LIST;
	}
	
	/**
	 * Get the data sets for the current instrument
	 * @return The data sets
	 */
	public List<DataSet> getDataSets() {
		try {
			loadDataSets();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataSets;
	}
	
	/**
	 * Load the list of data sets for the instrument from the database
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 */
	private void loadDataSets() throws MissingParamException, DatabaseException {
		if (null != currentFullInstrument) {
			dataSets = DataSetDB.getDataSets(getDataSource(), currentFullInstrument.getDatabaseId());
		} else {
			dataSets = null;
		}
	}
	
	/**
	 * Get the data files for the current instrument in JSON format
	 * for the timeline
	 * @return The data files JSON
	 */
	public String getTimelineEntriesJson() {
		if (null == timelineEntriesJson) {
			buildTimelineJson();
		}
		
		return timelineEntriesJson;
	}
	
	/**
	 * Get the file definitions for the current instrument in JSON format
	 * for the timeline
	 * @return The file definitions JSON
	 */
	public String getFileDefinitionsJson() {
		if (null == timelineEntriesJson) {
			buildTimelineJson();
		}
		
		return fileDefinitionsJson;
	}
	
	/**
	 * Build the timeline JSON string for the data files
	 */
	private void buildTimelineJson() {
		try {
			if (null == currentFullInstrument) {
				currentFullInstrument = InstrumentDB.getInstrument(getDataSource(), getCurrentInstrument(), ServletUtils.getResourceManager().getSensorsConfiguration(), ServletUtils.getResourceManager().getRunTypeCategoryConfiguration());
			}

			// Make the list of file definitions
			Map<String, Integer> definitionIds = new HashMap<String, Integer>();
			
			
			StringBuilder fdJson = new StringBuilder();
			
			fdJson.append('[');

			// Add a fake definition for the data sets, so they can be seen on the timeline
			fdJson.append("{\"id\":-1000,\"content\":\"File Type:\",\"order\":-1000},");
			
			for (int i = 0; i < currentFullInstrument.getFileDefinitions().size(); i++) {
				FileDefinition definition = currentFullInstrument.getFileDefinitions().get(i);
				
				// Store the definition number for use when building the files JSON below
				definitionIds.put(definition.getFileDescription(), i);
				
				fdJson.append('{');
				fdJson.append("\"id\":");
				fdJson.append(i);
				fdJson.append(",\"content\":\"");
				fdJson.append(definition.getFileDescription());
				fdJson.append("\",\"order\":");
				fdJson.append(i);
				fdJson.append('}');
				
				if (i < currentFullInstrument.getFileDefinitions().size() - 1) {
					fdJson.append(',');
				}
			}
			
			fdJson.append(']');
			
			fileDefinitionsJson = fdJson.toString();

			// Now the actual files
			List<DataFile> dataFiles = DataFileDB.getUserFiles(getDataSource(), getAppConfig(), getUser(), currentFullInstrument.getDatabaseId());

			StringBuilder entriesJson = new StringBuilder();
			entriesJson.append('[');
			
			for (int i = 0; i < dataFiles.size(); i++) {
				DataFile file = dataFiles.get(i);
				
				entriesJson.append('{');
				entriesJson.append("\"type\":\"range\", \"group\":");
				entriesJson.append(definitionIds.get(file.getFileDefinition().getFileDescription()));
				entriesJson.append(",\"start\":\"");
				entriesJson.append(DateTimeUtils.toJsonDate(file.getStartDate()));
				entriesJson.append("\",\"end\":\"");
				entriesJson.append(DateTimeUtils.toJsonDate(file.getEndDate()));
				entriesJson.append("\",\"content\":\"");
				entriesJson.append(file.getFilename());
				entriesJson.append("\",\"title\":\"");
				entriesJson.append(file.getFilename());
				entriesJson.append("\"}");
				
				if (i < dataFiles.size() - 1) {
					entriesJson.append(',');
				}
			}
			
			if (dataSets.size() > 0) {
				entriesJson.append(',');

				for (int i = 0; i < dataSets.size(); i++) {
					DataSet dataSet = dataSets.get(i);
					
					entriesJson.append('{');
					entriesJson.append("\"type\":\"background\",");
					entriesJson.append("\"start\":\"");
					entriesJson.append(DateTimeUtils.toJsonDate(dataSet.getStart()));
					entriesJson.append("\",\"end\":\"");
					entriesJson.append(DateTimeUtils.toJsonDate(dataSet.getEnd()));
					entriesJson.append("\",\"content\":\"");
					entriesJson.append(dataSet.getName());
					entriesJson.append("\",\"title\":\"");
					entriesJson.append(dataSet.getName());
					entriesJson.append("\",\"className\":\"timelineDataSet\"}");

					if (i < dataSets.size() - 1) {
						entriesJson.append(',');
					}
				}
			}
			
			entriesJson.append(']');
			
			timelineEntriesJson = entriesJson.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the new data set
	 * @return The new data set
	 */
	public DataSet getNewDataSet() {
		return newDataSet;
	}
	
	/**
	 * Get the names of all data sets for the instrument as a JSON string
	 * @return The data set names
	 */
	public String getDataSetNamesJson() {
		StringBuilder json = new StringBuilder();
		
		json.append('[');
		
		for (int i = 0; i < dataSets.size(); i++) {
			json.append('"');
			json.append(dataSets.get(i).getName());
			json.append('"');
			
			if (i < dataSets.size() - 1) {
				json.append(',');
			}
		}
		
		json.append(']');

		return json.toString();
	}
	
	/**
	 * Store the newly defined data set
	 * @return Navigation to the data set list
	 */
	public String addDataSet() {
		
		try {
			DataSetDB.addDataSet(getDataSource(), newDataSet);
			
			Map<String, String> params = new HashMap<String, String>();
			params.put(ExtractDataSetJob.ID_PARAM, String.valueOf(newDataSet.getId()));
			
			JobManager.addJob(getDataSource(), getUser(), ExtractDataSetJob.class.getCanonicalName(), params);
			
			loadDataSets();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return NAV_DATASET_LIST;
	}

	/**
	 * @return the platformCode
	 */
	public String getPlatformCode() {
		if (platformCode == null) {
			if (newDataSet == null) {
				return null;
			}
			ResourceManager rm = ResourceManager.getInstance();
			try {
				Instrument i = InstrumentDB.getInstrument(
					rm.getDBDataSource(),
					getNewDataSet().getInstrumentId(),
					rm.getSensorsConfiguration(),
					rm.getRunTypeCategoryConfiguration()
				);
				platformCode = i.getPlatformCode();
				return platformCode;
			} catch (MissingParamException | DatabaseException | RecordNotFoundException | InstrumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return platformCode;
	}

	/**
	 * @param platformCode the platformCode to set
	 */
	public void setPlatformCode(String platformCode) {
		this.platformCode = platformCode;
	}

	/**
	 * Get the dataset ID
	 * @return The dataset ID
	 */
	public long getDatasetId() {
		return datasetId;
	}
	
	/**
	 * Set the dataset ID
	 * @param datasetId The dataset ID
	 */
	public void setDatasetId(long datasetId) {
		this.datasetId = datasetId;
	}

	/**
	 * Submit the data set for data reduction
	 */
	public void submitDataReductionJob() {
		try {
			Map<String, String> jobParams = new HashMap<String, String>();
			jobParams.put(DataReductionJob.ID_PARAM, String.valueOf(datasetId));
			JobManager.addJob(getDataSource(), getUser(), DataReductionJob.class.getCanonicalName(), jobParams);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Submit the data set for automatic QC
	 */
	public void submitAutoQcJob() {
		try {
			Map<String, String> jobParams = new HashMap<String, String>();
			jobParams.put(AutoQCJob.ID_PARAM, String.valueOf(datasetId));
			jobParams.put(AutoQCJob.PARAM_ROUTINES_CONFIG, ResourceManager.QC_ROUTINES_CONFIG);
			JobManager.addJob(getDataSource(), getUser(), AutoQCJob.class.getCanonicalName(), jobParams);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}