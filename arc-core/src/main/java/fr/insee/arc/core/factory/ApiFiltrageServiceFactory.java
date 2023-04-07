package fr.insee.arc.core.factory;

import fr.insee.arc.core.service.ApiFiltrageService;
import fr.insee.arc.core.service.ApiService;

public class ApiFiltrageServiceFactory implements IServiceFactory {

	/**
	 * 
	 * @param aCurrentPhase
	 * @param anParametersEnvironment
	 * @param aEnvExecution
	 * @param aDirectoryRoot
	 * @param aNbEnr
	 */
	@Override
	public ApiService get(String phaseService, String metaDataSchema, String executionSchema, String directory, Integer capacityParameter, String paramBatch) {
		return new ApiFiltrageService(phaseService, metaDataSchema, executionSchema, directory, capacityParameter, paramBatch);
	}

	public static IServiceFactory getInstance() {
		return new ApiFiltrageServiceFactory();
	}

}
