package fr.insee.arc.core.factory;

import fr.insee.arc.core.service.ApiControleService;
import fr.insee.arc.core.service.ApiService;

public class ApiControleServiceFactory implements IServiceFactory {

	@Override
	/**
	 *
	 * @param aCurrentPhase
	 * @param anParametersEnvironment
	 * @param aEnvExecution
	 * @param aDirectoryRoot
	 * @param aNbEnr
	 */
	public ApiService get(String phaseService, String metaDataSchema, String executionSchema, String directory, Integer capacityParameter, String paramBatch) {
		return new ApiControleService(phaseService, metaDataSchema, executionSchema, directory, capacityParameter, paramBatch);
	}

	public static IServiceFactory getInstance() {
		return new ApiControleServiceFactory();
	}

}
