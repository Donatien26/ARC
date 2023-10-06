package fr.insee.arc.ws.services.restServices.execute;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.insee.arc.core.dataobjects.ColumnEnum;
import fr.insee.arc.core.dataobjects.ViewEnum;
import fr.insee.arc.core.model.TraitementPhase;
import fr.insee.arc.core.service.global.bo.JeuDeRegle;
import fr.insee.arc.core.service.global.bo.JeuDeRegleDao;
import fr.insee.arc.core.service.p2chargement.engine.ChargeurXmlComplexe;
import fr.insee.arc.core.service.p3normage.engine.NormageEngine;
import fr.insee.arc.core.service.p4controle.engine.ServiceJeuDeRegle;
import fr.insee.arc.core.service.p4controle.engine.dao.ServiceRequeteSqlRegle;
import fr.insee.arc.core.service.p5mapping.engine.RegleMappingFactory;
import fr.insee.arc.core.service.p5mapping.engine.RequeteMapping;
import fr.insee.arc.core.service.p5mapping.engine.ServiceMapping;
import fr.insee.arc.core.util.LoggerDispatcher;
import fr.insee.arc.utils.dao.UtilitaireDao;
import fr.insee.arc.ws.services.restServices.execute.pojo.ExecuteParameterPojo;
import fr.insee.arc.ws.services.restServices.execute.view.ReturnView;

@RestController
public class ExecuteEngineController {
	
	@Autowired
	private LoggerDispatcher loggerDispatcher;
	
    private static final Logger LOGGER = LogManager.getLogger(ExecuteEngineController.class);
	
	@RequestMapping(value = "/execute/engine/{serviceName}/{serviceId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ReturnView> executeEngineClient(
			@PathVariable String serviceName,
			@PathVariable int serviceId,
			@RequestBody(required = true) ExecuteParameterPojo bodyPojo
	)
	{
		Date firstContactDate=new Date();
		ReturnView returnView=new ReturnView();
		
		String identifiantLog = "(" + serviceName + ", " + serviceId + ")";
		
		loggerDispatcher.info(identifiantLog + " received", LOGGER);

		try (Connection connection = UtilitaireDao.get(0).getDriverConnexion()) {
			
			ExecuteRulesDao.fillRules(connection, bodyPojo, serviceName, serviceId);
			
			StringBuilder requete;
			
			loggerDispatcher.info(identifiantLog + " launching phases", LOGGER);
			String env = bodyPojo.sandbox;


			String structure = "";
				for (int i = 2; i <= TraitementPhase.getPhase(bodyPojo.targetPhase).getOrdre(); i++) {

					switch (TraitementPhase.getPhase(i)) {
					case CHARGEMENT:
						// register file

						try (InputStream inputStream = new ByteArrayInputStream(
								bodyPojo.fileContent.getBytes(StandardCharsets.UTF_8));) {
							ChargeurXmlComplexe chargeur = new ChargeurXmlComplexe(connection, bodyPojo.fileName, inputStream, currentTemporaryTable(i),
									bodyPojo.norme, bodyPojo.periodicite, bodyPojo.validite, env);
							chargeur.executeEngine();
							structure = chargeur.jointure.replace("''", "'");
						}
						break;
					case NORMAGE:
						Map<String, List<String>> pil = new HashMap<>();
						pil.put(ColumnEnum.ID_SOURCE.getColumnName(), new ArrayList<>(Arrays.asList(bodyPojo.fileName)));
						pil.put("id_norme", new ArrayList<>(Arrays.asList(bodyPojo.norme)));
						pil.put("validite", new ArrayList<>(Arrays.asList(bodyPojo.validite)));
						pil.put("periodicite", new ArrayList<>(Arrays.asList(bodyPojo.periodicite)));
						pil.put("jointure", new ArrayList<>(Arrays.asList(structure)));

						Map<String, List<String>> regle = new HashMap<>();
						regle.put("id_regle", new ArrayList<>());
						regle.put("id_norme", new ArrayList<>());
						regle.put("periodicite", new ArrayList<>());
						regle.put("validite_inf", new ArrayList<>());
						regle.put("validite_sup", new ArrayList<>());
						regle.put("id_classe", new ArrayList<>());
						regle.put("rubrique", new ArrayList<>());
						regle.put("rubrique_nmcl", new ArrayList<>());

						Map<String, List<String>> rubriqueUtiliseeDansRegles = new HashMap<>();
						rubriqueUtiliseeDansRegles.put("var", new ArrayList<>());

						NormageEngine normage = new NormageEngine(connection, pil, regle, rubriqueUtiliseeDansRegles, previousTemporaryTable(i), currentTemporaryTable(i),
								null);
						normage.executeEngine();
						break;
					case CONTROLE:
						requete = new StringBuilder();
						requete.append(
								"CREATE TEMPORARY TABLE "+currentTemporaryTable(i)+" as select *, '0'::text collate \"C\" as controle, null::text[] collate \"C\" as brokenrules from "+previousTemporaryTable(i)+";");
						UtilitaireDao.get(0).executeImmediate(connection, requete);

						ServiceJeuDeRegle sjdr = new ServiceJeuDeRegle();

						// Récupération des règles de controles associées aux jeux de règle
						JeuDeRegle jdr = new JeuDeRegle();

						sjdr.fillRegleControle(connection, jdr, ViewEnum.CONTROLE_REGLE.getFullName(env), currentTemporaryTable(i));
						sjdr.executeJeuDeRegle(connection, jdr, currentTemporaryTable(i));
						break;
					case MAPPING:
						UtilitaireDao.get(0).executeImmediate(connection, "CREATE TEMPORARY TABLE "+currentTemporaryTable(i)+" as select * from "+previousTemporaryTable(i)+" WHERE controle IN ('"+ServiceRequeteSqlRegle.RECORD_WITH_NOERROR+"','"+ServiceRequeteSqlRegle.RECORD_WITH_ERROR_TO_KEEP+"');");
						String tableTempControleOk = previousTemporaryTable(i);
						List<JeuDeRegle> listeJeuxDeRegles = JeuDeRegleDao.recupJeuDeRegle(connection, tableTempControleOk, env + ".mapping_regle");
						ServiceMapping serviceMapping = new ServiceMapping();
						RegleMappingFactory regleMappingFactory = serviceMapping.construireRegleMappingFactory(connection, env, tableTempControleOk, "v_");
						String idFamille = serviceMapping.fetchIdFamille(connection, listeJeuxDeRegles.get(0), env + ".norme");

			            RequeteMapping requeteMapping = new RequeteMapping(connection, regleMappingFactory, idFamille, listeJeuxDeRegles.get(0),
			                        env, tableTempControleOk, 0);
			            requeteMapping.construire();
			            UtilitaireDao.get(0).executeBlock(connection, requeteMapping.requeteCreationTablesTemporaires());

			            StringBuilder req = new StringBuilder();
			            req.append(requeteMapping.getRequete(bodyPojo.fileName));
			            UtilitaireDao.get(0).executeBlock(connection,"set enable_nestloop=off;"+req.toString()+"set enable_nestloop=on;");
			            req.setLength(0);

		                StringBuilder requeteMAJFinale = new StringBuilder();
		                requeteMAJFinale.append(requeteMapping.requeteTransfertVersTablesMetierDefinitives());
		                UtilitaireDao.get(0).executeBlock(connection, requeteMAJFinale);

		                UtilitaireDao.get(0).dropTable(connection, requeteMapping.tableauNomsTablesTemporaires());
						break;

					default:
						
						break;

					}

				}
				
				ExecuteRulesDao.buildResponse(connection, bodyPojo, returnView, firstContactDate);

		} catch (Exception e) {
			loggerDispatcher.error(identifiantLog, e, LOGGER);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(returnView);
		}
		loggerDispatcher.info(identifiantLog + " done", LOGGER);
		return ResponseEntity.status(HttpStatus.OK).body(returnView);

	}
	
	@RequestMapping(value = "/execute/engine/{serviceName}/{serviceId}/{sandbox}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ReturnView> executeEngineClient(
			@PathVariable String serviceName,
			@PathVariable int serviceId,
			@PathVariable int sandbox,
			@RequestBody(required = true) ExecuteParameterPojo p
	)
	{
		p.sandbox="arc_"+sandbox;
		return  executeEngineClient(serviceName,serviceId,p);
	}

	
	// les tables temporaires des phases respectives valent a,b,c,d ...
	// important de rester sur un octet pour les performances
	private static int temporaryTableAsciiStart=97;

	
	private String generateTemporaryTableName(int i)
	{
		return String.valueOf(((char) (temporaryTableAsciiStart + i)));
	}
	
	private String currentTemporaryTable(int i)
	{
		return generateTemporaryTableName(i);
	}
	
	
	private String previousTemporaryTable(int i)
	{
		return generateTemporaryTableName(i-1);
	}

}