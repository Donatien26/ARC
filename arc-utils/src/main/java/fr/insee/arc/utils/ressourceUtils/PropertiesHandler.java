package fr.insee.arc.utils.ressourceUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.checkerframework.common.returnsreceiver.qual.This;
import org.springframework.stereotype.Service;

import fr.insee.arc.utils.exception.ArcException;
import fr.insee.arc.utils.exception.ArcExceptionMessage;
import fr.insee.arc.utils.utils.ManipString;

@Service("properties")
public class PropertiesHandler {

	/* Database */
	private String databasePoolName;
	private String databaseRestrictedUsername;
	private String databaseUrl;
	private String databaseUsername;
	private String databasePassword;
	private String databaseDriverClassName;
	private String databaseSchema;

	/**
	 * List of connection attributes
	 */
	private List<ConnectionAttribute> connectionProperties;

	/* Log */
	private String logDirectory;
	private String logLevel;
	private String logFileName;
	private String logConfiguration;
	/* Batch */
	private String batchParametersDirectory;
	private String batchExecutionEnvironment;
	/* Miscellaneous */
	private String version;
	private String versionDate;
	private String application;
	/* Directories */
	private String registrationDirectory;
	private String loadingDirectory;
	private String storageDirectory;

	private String authorizedRoles;
	private String disableDebugGui;

	private String gitCommitId;
	
	/* Kubernetes */
	private String kubernetesApiUri;
	private String kubernetesApiNamespace;
	private String kubernetesApiToken;
	private int kubernetesExecutorNumber;
	private String kubernetesExecutorLabel;
	
	private Boolean kubernetesActive;
	
	private String s3InputApiUri;
	private String s3InputBucket;
	private String s3InputAccess;
	private String s3InputSecret;
	
	private String s3OutputApiUri;
	private String s3OutputBucket;
	private String s3OutputAccess;
	private String s3OutputSecret;

	private static PropertiesHandler instanceOfPropertiesHandler;

	public void initializeLog() {
		LogConfigurator logConf = new LogConfigurator(logConfiguration);

		// if logDirectory (fr.insee.arc.log.directory) is set
		if (logDirectory != null && !logDirectory.trim().isEmpty()) {
			logConf.configureRollingFileAppender(logDirectory, logFileName);
		}

		if (logLevel != null && !logLevel.trim().isEmpty()) {
			logConf.configureLogLevel(logLevel);
		}
	}

	public static PropertiesHandler getInstance() {
		if (instanceOfPropertiesHandler == null) {
			try {
				instanceOfPropertiesHandler = (PropertiesHandler) SpringApplicationContext.getBean("properties");
			} catch (NullPointerException e) {
				ArcException ex = new ArcException(ArcExceptionMessage.SPRING_BEAN_PROPERTIES_NOTFOUND);
				ex.logMessageException();
				// create a blank instance singleton
				instanceOfPropertiesHandler = new PropertiesHandler();
			}
		}
		return instanceOfPropertiesHandler;
	}

	public String getDatabasePoolName() {
		return databasePoolName;
	}

	public void setDatabasePoolName(String databasePoolName) {
		this.databasePoolName = databasePoolName;
	}

	public String getDatabaseUrl() {
		return databaseUrl;
	}

	public void setDatabaseUrl(String databaseUrl) {
		this.databaseUrl = databaseUrl;
		// reset the connection getter
		this.connectionProperties=null;
	}

	public String getDatabaseUsername() {
		return databaseUsername;
	}

	public void setDatabaseUsername(String databaseUsername) {
		this.databaseUsername = databaseUsername;
		// reset the connection getter
		this.connectionProperties=null;
	}

	public String getDatabasePassword() {
		return databasePassword;
	}

	public void setDatabasePassword(String databasePassword) {
		this.databasePassword = databasePassword;
		// reset the connection getter
		this.connectionProperties=null;
	}

	public String getDatabaseDriverClassName() {
		return databaseDriverClassName;
	}

	public void setDatabaseDriverClassName(String databaseDriverClassName) {
		this.databaseDriverClassName = databaseDriverClassName;
		// reset the connection getter
		this.connectionProperties=null;
	}

	public String getDatabaseSchema() {
		return databaseSchema;
	}

	public void setDatabaseSchema(String databaseSchema) {
		this.databaseSchema = databaseSchema;
	}

	public String getLogDirectory() {
		return logDirectory;
	}

	public void setLogDirectory(String logDirectory) {
		this.logDirectory = logDirectory;
	}

	public String getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}

	public String getLogFileName() {
		return logFileName;
	}

	public void setLogFileName(String logFileName) {
		this.logFileName = logFileName;
	}

	public String getLogConfiguration() {
		return logConfiguration;
	}

	public void setLogConfiguration(String logConfiguration) {
		this.logConfiguration = logConfiguration;
		initializeLog();
	}

	public String getBatchParametersDirectory() {
		return batchParametersDirectory;
	}

	public void setBatchParametersDirectory(String batchParametersDirectory) {
		this.batchParametersDirectory = batchParametersDirectory;
	}

	public String getBatchExecutionEnvironment() {
		return batchExecutionEnvironment;
	}

	public void setBatchExecutionEnvironment(String batchExecutionEnvironment) {
		this.batchExecutionEnvironment = batchExecutionEnvironment;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = ManipString.substringAfterFirst(version, "version-");
	}

	public String getVersionDate() {
		return versionDate;
	}

	public void setVersionDate(String versionDate) {
		this.versionDate = versionDate;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getRegistrationDirectory() {
		return registrationDirectory;
	}

	public void setRegistrationDirectory(String registrationDirectory) {
		this.registrationDirectory = registrationDirectory;
	}

	public String getLoadingDirectory() {
		return loadingDirectory;
	}

	public void setLoadingDirectory(String loadingDirectory) {
		this.loadingDirectory = loadingDirectory;
	}

	public String getStorageDirectory() {
		return storageDirectory;
	}

	public void setStorageDirectory(String storageDirectory) {
		this.storageDirectory = storageDirectory;
	}

	public void setAuthorizedRoles(String authorizedRoles) {
		this.authorizedRoles = authorizedRoles;
	}

	public String[] getAuthorizedRoles() {
		if (authorizedRoles == null || authorizedRoles.isEmpty()) {
			return new String[0];
		}
		return authorizedRoles.split(",");
	}

	public String getDatabaseRestrictedUsername() {
		return databaseRestrictedUsername;
	}

	public void setDatabaseRestrictedUsername(String databaseRestrictedUsername) {
		this.databaseRestrictedUsername = databaseRestrictedUsername;
	}

	public String getDisableDebugGui() {
		return disableDebugGui;
	}

	public void setDisableDebugGui(String disableDebugGui) {
		this.disableDebugGui = disableDebugGui;
	}

	public String getGitCommitId() {
		return gitCommitId;
	}

	public void setGitCommitId(String gitCommitId) {
		this.gitCommitId = gitCommitId;
	}

	
	public String getKubernetesApiUri() {
		return kubernetesApiUri;
	}

	public void setKubernetesApiUri(String kubernetesApiUri) {
		this.kubernetesApiUri = kubernetesApiUri;
	}

	public String getKubernetesApiToken() {
		return kubernetesApiToken;
	}
	
	public String getKubernetesApiNamespace() {
		return kubernetesApiNamespace;
	}

	public void setKubernetesApiNamespace(String kubernetesApiNamespace) {
		this.kubernetesApiNamespace = kubernetesApiNamespace;
	}

	public void setKubernetesApiToken(String kubernetesApiToken) {
		this.kubernetesApiToken = kubernetesApiToken;
	}

	public int getKubernetesExecutorNumber() {
		return kubernetesExecutorNumber;
	}

	public void setKubernetesExecutorNumber(int kubernetesExecutorNumber) {
		this.kubernetesExecutorNumber = kubernetesExecutorNumber;
	}

	public String getKubernetesExecutorLabel() {
		return kubernetesExecutorLabel;
	}

	public void setKubernetesExecutorLabel(String kubernetesExecutorLabel) {
		this.kubernetesExecutorLabel = kubernetesExecutorLabel;
	}
	
	public boolean isKubernetesActive() {
		if (this.kubernetesActive==null)
		{
			this.kubernetesActive =  this.kubernetesApiToken!=null && kubernetesExecutorNumber>0;
		}
		return kubernetesActive;
	}

	public String getS3InputApiUri() {
		return s3InputApiUri;
	}

	public void setS3InputApiUri(String s3InputApiUri) {
		this.s3InputApiUri = s3InputApiUri;
	}

	public String getS3InputBucket() {
		return s3InputBucket;
	}

	public void setS3InputBucket(String s3InputBucket) {
		this.s3InputBucket = s3InputBucket;
	}

	public String getS3InputAccess() {
		return s3InputAccess;
	}

	public void setS3InputAccess(String s3InputAccess) {
		this.s3InputAccess = s3InputAccess;
	}

	public String getS3InputSecret() {
		return s3InputSecret;
	}

	public void setS3InputSecret(String s3InputSecret) {
		this.s3InputSecret = s3InputSecret;
	}

	public String getS3OutputApiUri() {
		return s3OutputApiUri;
	}

	public void setS3OutputApiUri(String s3OutputApiUri) {
		this.s3OutputApiUri = s3OutputApiUri;
	}

	public String getS3OutputBucket() {
		return s3OutputBucket;
	}

	public void setS3OutputBucket(String s3OutputBucket) {
		this.s3OutputBucket = s3OutputBucket;
	}

	public String getS3OutputAccess() {
		return s3OutputAccess;
	}

	public void setS3OutputAccess(String s3OutputAccess) {
		this.s3OutputAccess = s3OutputAccess;
	}

	public String getS3OutputSecret() {
		return s3OutputSecret;
	}

	public void setS3OutputSecret(String s3OutputSecret) {
		this.s3OutputSecret = s3OutputSecret;
	}

	public Map<String, String> fullVersionInformation() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("version", getVersion());
		map.put("buildDate", getVersionDate());
		map.put("gitCommitId", getGitCommitId());
		return map;
	}


	/**
	 * Unserialize the connection data found in properties
	 * 
	 * @return
	 */
	public List<ConnectionAttribute> getConnectionProperties() {
		
		if (this.connectionProperties == null) {
			connectionProperties = new ArrayList<>();

			String[] databaseUrls = ConnectionAttribute.unserialize(this.databaseUrl);
			String[] databaseUsernames = ConnectionAttribute.unserialize(this.databaseUsername);
			String[] databasePasswords = ConnectionAttribute.unserialize(this.databasePassword);
			String[] databaseDriverClassNames = ConnectionAttribute.unserialize(this.databaseDriverClassName);

			for (int tokenIndex = 0; tokenIndex < databaseUrls.length; tokenIndex++) {
				connectionProperties
						.add(new ConnectionAttribute(databaseUrls[tokenIndex], databaseUsernames[tokenIndex],
								databasePasswords[tokenIndex], databaseDriverClassNames[tokenIndex]));
			}
		}

		return this.connectionProperties;
	}

	public void setConnectionProperties(List<ConnectionAttribute> connectionProperties) {
		this.connectionProperties = connectionProperties;
	}
	
	

}
