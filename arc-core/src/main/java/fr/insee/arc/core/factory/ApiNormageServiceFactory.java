package fr.insee.arc.core.factory;

import fr.insee.arc.core.model.TraitementPhase;
import fr.insee.arc.core.service.global.ApiService;
import fr.insee.arc.core.service.p3normage.ApiNormageService;

public class ApiNormageServiceFactory implements IServiceFactory {

	@Override
	/**
	 *
	 * @param aCurrentPhase
	 * @param anParametersEnvironment
	 * @param aEnvExecution
	 * @param aDirectoryRoot
	 * @param aNbEnr
	 */
	public ApiService get(TraitementPhase phaseService, String executionSchema, String directory, Integer capacityParameter, String paramBatch) {
		return new ApiNormageService(phaseService, executionSchema, directory, capacityParameter, paramBatch);	
	}

	public static IServiceFactory getInstance() {
		return new ApiNormageServiceFactory();
	}

}
