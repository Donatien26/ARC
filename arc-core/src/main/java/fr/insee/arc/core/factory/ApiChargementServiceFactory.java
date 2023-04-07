package fr.insee.arc.core.factory;

import fr.insee.arc.core.service.ApiChargementService;
import fr.insee.arc.core.service.ApiService;

public class ApiChargementServiceFactory implements IServiceFactory {

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
		return new ApiChargementService(phaseService, metaDataSchema, executionSchema, directory, capacityParameter, paramBatch);
	}
	
	public static IServiceFactory getInstance() {
		return new ApiChargementServiceFactory();
	}

}
