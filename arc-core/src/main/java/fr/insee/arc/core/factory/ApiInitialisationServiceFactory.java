package fr.insee.arc.core.factory;

import fr.insee.arc.core.model.TraitementPhase;
import fr.insee.arc.core.service.global.ApiService;
import fr.insee.arc.core.service.p0initialisation.ApiInitialisationService;

public class ApiInitialisationServiceFactory implements IServiceFactory {

	@Override
	public ApiService get(TraitementPhase phaseService, String executionSchema, Integer capacityParameter, String paramBatch) {
		return new ApiInitialisationService(phaseService, executionSchema, capacityParameter, paramBatch);	
	}

	public static IServiceFactory getInstance() {
		return new ApiInitialisationServiceFactory();
	}

}
