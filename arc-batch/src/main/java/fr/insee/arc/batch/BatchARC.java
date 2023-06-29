package fr.insee.arc.batch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import fr.insee.arc.batch.threadrunners.PhaseParameterKeys;
import fr.insee.arc.batch.threadrunners.PhaseThreadFactory;
import fr.insee.arc.core.dataobjects.ArcDatabase;
import fr.insee.arc.core.dataobjects.ArcPreparedStatementBuilder;
import fr.insee.arc.core.model.TraitementEtat;
import fr.insee.arc.core.model.TraitementPhase;
import fr.insee.arc.core.service.api.ApiReceptionService;
import fr.insee.arc.core.service.api.ApiService;
import fr.insee.arc.core.service.api.query.ServiceDatabaseMaintenance;
import fr.insee.arc.core.util.BDParameters;
import fr.insee.arc.core.util.StaticLoggerDispatcher;
import fr.insee.arc.utils.batch.IReturnCode;
import fr.insee.arc.utils.dao.UtilitaireDao;
import fr.insee.arc.utils.exception.ArcException;
import fr.insee.arc.utils.exception.ArcExceptionMessage;
import fr.insee.arc.utils.files.FileUtilsArc;
import fr.insee.arc.utils.ressourceUtils.PropertiesHandler;
import fr.insee.arc.utils.structure.GenericBean;
import fr.insee.arc.utils.utils.FormatSQL;
import fr.insee.arc.utils.utils.LoggerHelper;
import fr.insee.arc.utils.utils.ManipString;
import fr.insee.arc.utils.utils.Sleep;

/**
 * Classe lanceur de l'application Accueil Reception Contrôle 07/08/2015 Version
 * pour les tests de performance et pré-production
 * 
 * @author Manu
 * 
 */
class BatchARC implements IReturnCode {
	private static final Logger LOGGER = LogManager.getLogger(BatchARC.class);
	private static HashMap<String, String> mapParam = new HashMap<>();

	/**
	 * variable dateInitialisation si vide (ou si date du jour+1 depassé à 20h), je
	 * lance initialisation et j'initialise dateInitialisation à la nouvelle date du
	 * jour puis une fois terminé, je lancent la boucle des batchs si date du jour+1
	 * depassé a 20h, - j'indique aux autre batchs de s'arreter - une fois arretés,
	 * je met tempo à la date du jour - je lance initialisation etc.
	 */

	private @Autowired PropertiesHandler properties;

	// the sandbox schema where batch process runs
	private String envExecution;

	// file directory
	private String repertoire;

	// fréquence à laquelle les phases sont démarrées
	private int poolingDelay;

	// heure d'initalisation en production
	private int hourToTriggerInitializationInProduction;

	// interval entre chaque initialisation en nb de jours
	private Integer intervalForInitializationInDay;

	// nombre d'iteration de la boucle batch entre chaque routine de maintenance de
	// la base de données
	private Integer numberOfIterationBewteenDatabaseMaintenanceRoutine;

	// nombre d'iteration de la boucle batch entre chaque routine de vérification du
	// reste à faire
	private Integer numberOfIterationBewteenCheckTodo;

	// nombre de pods utilisés par ARC
	private Integer numberOfPods;

	// true = the batch will resume the process from a formerly interrupted batch
	// false = the batch will proceed to a new load
	// Maintenance initialization process can only occur in this case
	private boolean dejaEnCours;

	// Array of phases
	private ArrayList<TraitementPhase> phases = new ArrayList<>();
	// Map of thread by phase
	private HashMap<TraitementPhase, ArrayList<PhaseThreadFactory>> pool = new HashMap<>();
	// delay between phase start
	private int delay;
	
	// loop attribute
	Thread maintenance=new Thread();
	private boolean productionOn;
	private boolean exit=false;
	private int iteration = 0;

	private static void message(String msg) {
		StaticLoggerDispatcher.warn(msg, LOGGER);
	}

	private void initParameters() {

		BDParameters bdParameters=new BDParameters(ArcDatabase.COORDINATOR);
		
		boolean keepInDatabase = Boolean
				.parseBoolean(bdParameters.getString(null, "LanceurARC.keepInDatabase", "false"));

		// pour le batch en cours, l'ensemble des enveloppes traitées ne peut pas
		// excéder une certaine taille
		int tailleMaxReceptionEnMb = bdParameters.getInt(null, "LanceurARC.tailleMaxReceptionEnMb", 10);

		// Maximum number of files to load
		int maxFilesToLoad = bdParameters.getInt(null, "LanceurARC.maxFilesToLoad", 101);

		// Maximum number of files processed in each phase iteration
		int maxFilesPerPhase = bdParameters.getInt(null, "LanceurARC.maxFilesPerPhase", 1000000);

		// fréquence à laquelle les phases sont démarrées
		this.poolingDelay = bdParameters.getInt(null, "LanceurARC.poolingDelay", 1000);

		// heure d'initalisation en production
		hourToTriggerInitializationInProduction = bdParameters.getInt(null,
				"ApiService.HEURE_INITIALISATION_PRODUCTION", 22);

		// interval entre chaque initialisation en nb de jours
		intervalForInitializationInDay = bdParameters.getInt(null, "LanceurARC.INTERVAL_JOUR_INITIALISATION", 7);

		// nombre d'iteration de la boucle batch entre chaque routine de maintenance de
		// la base de données
		numberOfIterationBewteenDatabaseMaintenanceRoutine = bdParameters.getInt(null,
				"LanceurARC.DATABASE_MAINTENANCE_ROUTINE_INTERVAL", 500);

		// nombre d'iteration de la boucle batch entre chaque routine de vérification du
		// reste à faire
		numberOfIterationBewteenCheckTodo = bdParameters.getInt(null, "LanceurARC.DATABASE_CHECKTODO_ROUTINE_INTERVAL",
				10);

		// the number of executor nods declared for scalability
		numberOfPods = ArcDatabase.numberOfExecutorNods();

		// the metadata schema
		String env;

		// either we take env and envExecution from database or properties
		// default is from properties
		if (Boolean.parseBoolean(bdParameters.getString(null, "LanceurARC.envFromDatabase", "false"))) {
			env = bdParameters.getString(null, "LanceurARC.env", ApiService.IHM_SCHEMA);
			envExecution = bdParameters.getString(null, "LanceurARC.envExecution", "arc_prod");
		} else {
			env = properties.getBatchArcEnvironment();
			envExecution = properties.getBatchExecutionEnvironment();
		}

		envExecution = envExecution.replace(".", "_");

		repertoire = properties.getBatchParametersDirectory();

		mapParam.put(PhaseParameterKeys.KEY_FOR_DIRECTORY_LOCATION, repertoire);
		mapParam.put(PhaseParameterKeys.KEY_FOR_BATCH_CHUNK_ID, new SimpleDateFormat("yyyyMMddHH").format(new Date()));
		mapParam.put(PhaseParameterKeys.KEY_FOR_METADATA_ENVIRONMENT, env);
		mapParam.put(PhaseParameterKeys.KEY_FOR_EXECUTION_ENVIRONMENT, envExecution);
		mapParam.put(PhaseParameterKeys.KEY_FOR_MAX_SIZE_RECEPTION, String.valueOf(tailleMaxReceptionEnMb));
		mapParam.put(PhaseParameterKeys.KEY_FOR_MAX_FILES_TO_LOAD, String.valueOf(maxFilesToLoad));
		mapParam.put(PhaseParameterKeys.KEY_FOR_MAX_FILES_PER_PHASE, String.valueOf(maxFilesPerPhase));
		mapParam.put(PhaseParameterKeys.KEY_FOR_KEEP_IN_DATABASE, String.valueOf(keepInDatabase));

		message(mapParam.toString());

	}

	/**
	 * Lanceur MAIN arc
	 * 
	 * @param args
	 */
	void execute() {

		// fill the parameters
		initParameters();

		message("Main");

		message("Batch ARC " + properties.fullVersionInformation().toString());

		try {
			this.productionOn = productionOn();

			if (!this.productionOn) {

				message("La production est arretée !");

			} else {

				message("Traitement Début");
				
				resetPendingFilesInPilotageTable(envExecution);

				// database maintenance on pilotage table so that index won't bloat
				maintenanceTablePilotageBatch();

				// delete work directories and move back files that were pending but not finished
				resetWorkDirectory();

				// initialize. Phase d'initialisation
				initialize();

				// register file. Phase de reception.
				executePhaseReception(envExecution);

				// execute the main batch process. return false if user had interrupted the process
				executeLoopOverPhases();

				// Delete entry files if no interruption or no problems
				effacerRepertoireChargement(repertoire, envExecution);

				message("Traitement Fin");
				System.exit(STATUS_SUCCESS);
			}

		} catch (Exception ex) {
			LoggerHelper.errorGenTextAsComment(BatchARC.class, "main()", LOGGER, ex);
			System.exit(STATUS_FAILURE_TECHNICAL_WARNING);
		}

		message("Fin du batch");

	}

	/**
	 * Créer la table de pilotage batch si elle n'existe pas déjà
	 * 
	 * @throws ArcException
	 */
	private void maintenanceTablePilotageBatch() throws ArcException {

		// création de la table si elle n'existe pas
		ArcPreparedStatementBuilder requete = new ArcPreparedStatementBuilder();
		requete.append("\n CREATE TABLE IF NOT EXISTS arc.pilotage_batch (last_init text, operation text); ");
		requete.append(
				"\n insert into arc.pilotage_batch select '1900-01-01:00','O' where not exists (select 1 from arc.pilotage_batch); ");
		UtilitaireDao.get(ArcDatabase.COORDINATOR.getIndex()).executeRequest(null, requete);

		for (int poolIndex = 0; poolIndex <= numberOfPods; poolIndex++) {
			// Maintenance full du catalog
			ServiceDatabaseMaintenance.maintenancePgCatalog(poolIndex, null, FormatSQL.VACUUM_OPTION_FULL);
			// maintenance des tables métier de la base de données
			ServiceDatabaseMaintenance.maintenanceDatabaseClassic(poolIndex, null, envExecution);
		}
	}

	/**
	 * exit loop condition
	 * 
	 * @param envExecution
	 * @return
	 */
	private boolean isNothingLeftToDo(String envExecution) {
		boolean isNothingLeftToDo = false;
		if (UtilitaireDao.get(ArcDatabase.COORDINATOR.getIndex()).getInt(null, new ArcPreparedStatementBuilder("select count(*) from (select 1 from "
				+ envExecution + ".pilotage_fichier where etape=1 limit 1) ww")) == 0) {
			isNothingLeftToDo = true;
		}
		return isNothingLeftToDo;
	}

	/**
	 * test si la chaine batch est arrétée
	 * 
	 * @return
	 * @throws ArcException
	 */
	private static boolean productionOn() throws ArcException {
		return UtilitaireDao.get(ArcDatabase.COORDINATOR.getIndex()).hasResults(null,
				new ArcPreparedStatementBuilder("select 1 from arc.pilotage_batch where operation='O'"));
	}

	/**
	 * Effacer les répertoires de chargement OK KO et ENCOURS
	 * 
	 * @param directory
	 * @param envExecution
	 * @throws IOException
	 */
	private void effacerRepertoireChargement(String directory, String envExecution) throws ArcException {
		if (!this.productionOn) {
			return;
		}
		
		// Effacer les fichiers des répertoires OK et KO
		String envDirectory = envExecution.replace(".", "_").toUpperCase();

		cleanDirectory(directory, envExecution, envDirectory, TraitementEtat.OK);

		cleanDirectory(directory, envExecution, envDirectory, TraitementEtat.KO);

		cleanDirectory(directory, envExecution, envDirectory, TraitementEtat.ENCOURS);

	}

	private static void cleanDirectory(String directory, String envExecution, String envDirectory, TraitementEtat etat)
			throws ArcException {
		File f = Paths.get(ApiReceptionService.directoryReceptionEtat(directory, envDirectory, etat)).toFile();
		if (!f.exists()) {
			return;
		}
		File[] fs = f.listFiles();
		for (File z : fs) {
			if (z.isDirectory()) {
				FileUtilsArc.deleteDirectory(z);
			} else {
				deleteIfArchived(directory, envExecution, z);
			}
		}
	}

	/**
	 * If the file has already been moved in the archive directory by ARC it is safe
	 * to delete it else save it to the archive directory
	 * 
	 * @param repertoire
	 * @param envExecution
	 * @param z
	 * @return
	 * @throws IOException
	 */
	private static void deleteIfArchived(String repertoire, String envExecution, File z) throws ArcException {

		String entrepot = ManipString.substringBeforeFirst(z.getName(), "_");
		String filename = ManipString.substringAfterFirst(z.getName(), "_");

		// ajout d'un garde fou : si le fichier n'est pas archivé : pas touche
		File fCheck = Paths
				.get(ApiReceptionService.directoryReceptionEntrepotArchive(repertoire, envExecution, entrepot),
						filename)
				.toFile();

		if (fCheck.exists()) {
			FileUtilsArc.delete(z);
		} else {
			FileUtilsArc.renameTo(z, fCheck);
		}
	}

	/**
	 * 
	 * @param envExecution
	 * @throws ArcException
	 */
	private static void resetPendingFilesInPilotageTable(String envExecution) throws ArcException {
		// delete files that are en cours
		StringBuilder query = new StringBuilder();
		query.append("\n DELETE FROM " + envExecution + ".pilotage_fichier ");
		query.append("\n WHERE etape=1 AND etat_traitement='{" + TraitementEtat.ENCOURS + "}' ");
		query.append(";");

		// update these files to etape=1
		query.append("\n UPDATE " + envExecution + ".pilotage_fichier ");
		query.append("\n set etape=1 ");
		query.append("\n WHERE etape=3");
		query.append(";");

		UtilitaireDao.get(ArcDatabase.COORDINATOR.getIndex()).executeBlock(null, query);

	}

	/**
	 * si c'est une reprise de batch déjà en cours, on remet les fichiers en_cours à
	 * l'état précédent dans la table de piltoage
	 * 
	 * @param envExecution
	 * @param repriseEnCOurs
	 * @param recevoir
	 * @throws ArcException
	 */
	private void executePhaseReception(String envExecution) throws ArcException {
		if (dejaEnCours) {
			message("Reprise des fichiers en cours de traitement");
			resetPendingFilesInPilotageTable(envExecution);
		} else {
			message("Reception de nouveaux fichiers");
			PhaseThreadFactory recevoir = new PhaseThreadFactory(mapParam, TraitementPhase.RECEPTION);
			recevoir.execute();
			message("Reception : " + recevoir.getReport().getNbObject() + " objets enregistrés en "
					+ recevoir.getReport().getDuree() + " ms");
		}

	}

	private void initialize() throws ArcException {
		message("Traitement Début");

		PhaseThreadFactory initialiser = new PhaseThreadFactory(mapParam, TraitementPhase.INITIALISATION);

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd:HH");

		String lastInitialize = null;
		lastInitialize = UtilitaireDao.get(ArcDatabase.COORDINATOR.getIndex()).getString(null,
				new ArcPreparedStatementBuilder("select last_init from arc.pilotage_batch "));

		Date dNow = new Date();
		Date dLastInitialize;

		try {
			dLastInitialize = dateFormat.parse(lastInitialize);
		} catch (ParseException dateParseException) {
			throw new ArcException(dateParseException, ArcExceptionMessage.BATCH_INITIALIZATION_DATE_PARSE_FAILED);
		}

		// la nouvelle initialisation se lance si la date actuelle est postérieure à la
		// date programmée d'initialisation (last_init)
		// on ne la lance que s'il n'y a rien en cours (pas essentiel mais plus
		// sécurisé)
		if ((!dejaEnCours && dLastInitialize.compareTo(dNow) < 0)) {

			message("Initialisation en cours");

			initialiser.execute();

			message("Initialisation terminée : " + initialiser.getReport().getDuree() + " ms");

			UtilitaireDao.get(ArcDatabase.COORDINATOR.getIndex()).executeRequest(null,
					new ArcPreparedStatementBuilder(
							"update arc.pilotage_batch set last_init=to_char(current_date+interval '"
									+ intervalForInitializationInDay + " days','yyyy-mm-dd')||':"
									+ hourToTriggerInitializationInProduction
									+ "' , operation=case when operation='R' then 'O' else operation end;"));
		}
	}

	/**
	 * Copy the files from the archive directory to ok directory
	 * 
	 * @param envExecution
	 * @param repertoire
	 * @param aBouger
	 * @throws IOException
	 */
	private void copyFileFromArchiveDirectoryToOK(String envExecution, String repertoire, ArrayList<String> aBouger)
			throws IOException {

		for (String container : aBouger) {
			String entrepotContainer = ManipString.substringBeforeFirst(container, "_");
			String originalContainer = ManipString.substringAfterFirst(container, "_");

			File fIn = Paths.get(
					ApiReceptionService.directoryReceptionEntrepotArchive(repertoire, envExecution, entrepotContainer),
					originalContainer).toFile();

			File fOut = Paths.get(ApiReceptionService.directoryReceptionEtatOK(repertoire, envExecution), container)
					.toFile();

			Files.copy(fIn.toPath(), fOut.toPath());
		}
	}

	private void deplacerFichiersNonTraites() throws ArcException, IOException {

		ArrayList<String> aBouger = new GenericBean(UtilitaireDao.get(ArcDatabase.COORDINATOR.getIndex()).executeRequest(null,
				new ArcPreparedStatementBuilder(
						"select distinct container from " + envExecution + ".pilotage_fichier where etape=1")))
				.mapContent().get("container");

		dejaEnCours = (aBouger != null);

		// si oui, on essaie de recopier les archives dans chargement OK
		if (dejaEnCours) {
			copyFileFromArchiveDirectoryToOK(envExecution, repertoire, aBouger);
		}
	}

	private void resetWorkDirectory() throws ArcException, IOException {

		message("Déplacements de fichiers");

		// on vide les repertoires de chargement OK, KO, ENCOURS
		effacerRepertoireChargement(repertoire, envExecution);

		// des archives n'ont elles pas été traitées jusqu'au bout ?
		deplacerFichiersNonTraites();

		message("Fin des déplacements de fichiers");
	}

	/**
	 * initalize the arraylist of phases to be looped over and the thread pool per
	 * phase
	 * calculated sleep delay between phase
	 * 
	 * @param phases
	 * @param pool
	 * @return
	 */
	private int initializeBatchLoop(ArrayList<TraitementPhase> phases,
			HashMap<TraitementPhase, ArrayList<PhaseThreadFactory>> pool) {
		int stepNumber = (TraitementPhase.MAPPING.getOrdre() - TraitementPhase.CHARGEMENT.getOrdre()) + 2;
		int delay = poolingDelay / stepNumber;

		message("Initialisation boucle Chargement->Mapping");

		// initialiser le tableau de phase
		int startingPhase = TraitementPhase.CHARGEMENT.getOrdre();

		for (TraitementPhase phase : TraitementPhase.values()) {
			if (phase.getOrdre() >= startingPhase) {
				phases.add(phase.getOrdre() - startingPhase, phase);
			}
		}

		// initialiser le pool de thread
		for (TraitementPhase phase : phases) {
			pool.put(phase, new ArrayList<>());
		}
		return delay;
	}

	/**
	 * start paralell thread 
	 * @throws ArcException
	 */
	private void executeLoopOverPhases() throws ArcException
	{
		this.productionOn=productionOn();
		
		// test if production is running or exit
		if (!productionOn)
		{
			return;
		}
		
		this.delay = initializeBatchLoop(phases, pool);

		// boucle de chargement
		message("Début de la boucle d'itération");
		
		do {

			this.iteration++;

			updateThreadPoolStatus();
			
			startPhaseThread();
			
			startMaintenanceThread();

			updateProductionOn();
			
			updateExit();
			
			waitAndClear();

		} while (!exit);
		
	}

	
	private void waitAndClear() {
		Sleep.sleep(delay);
		System.gc();
	}

	private void updateProductionOn() throws ArcException {
		if (iteration % numberOfIterationBewteenCheckTodo == 0) {
			// check if production on
			this.productionOn = productionOn();
		}
	}

	// updtate the thread pool by phase by deleting the dead and finished thread
	private void updateThreadPoolStatus() {
		// delete dead thread i.e. keep only living thread in the pool
					HashMap<TraitementPhase, ArrayList<PhaseThreadFactory>> poolToKeep = new HashMap<>();
					for (TraitementPhase phase : phases) {
						poolToKeep.put(phase, new ArrayList<>());
						if (!pool.get(phase).isEmpty()) {
							for (PhaseThreadFactory thread : pool.get(phase)) {
								if (thread.isAlive()) {
									poolToKeep.get(phase).add(thread);
								}
							}
						}
					}
		this.pool=poolToKeep;
	}

	// build and start a new phase thread if the former created thread has died
	private void startPhaseThread()
	{
		// add new thread and start
		for (TraitementPhase phase : phases) {
			// if no thread in phase, start one
			if (pool.get(phase).isEmpty()) {
				PhaseThreadFactory a = new PhaseThreadFactory(mapParam, phase);
				a.start();
				pool.get(phase).add(a);
			}
			// delay between phases not to overload
			Sleep.sleep(delay);
		}
	}
	
	private void startMaintenanceThread() {
		if (iteration % numberOfIterationBewteenDatabaseMaintenanceRoutine == 0) {
			if (!maintenance.isAlive()) {
				message(iteration + ": database maintenance started");
				maintenance = new Thread() {
					@Override
					public void run() {
						for (int poolIndex = 0; poolIndex <= numberOfPods; poolIndex++) {
							ServiceDatabaseMaintenance.maintenanceDatabaseClassic(poolIndex, null,
									envExecution);
						}
					}
				};
				maintenance.start();
			}
		}
	}
	
	private void updateExit() {
		if (iteration % numberOfIterationBewteenCheckTodo == 0) {
			// check if batch must exit loop
			// exit if nothing left to do or if the production had been turned OFF
			exit = isNothingLeftToDo(envExecution) || !productionOn;
		}
	}
	
}
