package fr.insee.arc.core.service.global;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import fr.insee.arc.core.dataobjects.ArcDatabase;
import fr.insee.arc.core.dataobjects.ArcPreparedStatementBuilder;
import fr.insee.arc.core.dataobjects.ColumnEnum;
import fr.insee.arc.core.dataobjects.ViewEnum;
import fr.insee.arc.core.model.TraitementEtat;
import fr.insee.arc.core.model.TraitementPhase;
import fr.insee.arc.core.service.global.bo.Sandbox;
import fr.insee.arc.core.service.global.dao.DatabaseConnexionConfiguration;
import fr.insee.arc.core.service.global.dao.PilotageOperations;
import fr.insee.arc.core.service.global.dao.TableNaming;
import fr.insee.arc.core.service.global.scalability.ScalableConnection;
import fr.insee.arc.core.service.global.util.Patch;
import fr.insee.arc.core.util.StaticLoggerDispatcher;
import fr.insee.arc.utils.dao.UtilitaireDao;
import fr.insee.arc.utils.exception.ArcException;
import fr.insee.arc.utils.exception.ArcExceptionMessage;
import fr.insee.arc.utils.ressourceUtils.SpringApplicationContext;
import fr.insee.arc.utils.structure.GenericBean;
import fr.insee.arc.utils.textUtils.IConstanteNumerique;
import fr.insee.arc.utils.utils.FormatSQL;
import fr.insee.arc.utils.utils.LoggerHelper;

@Component
public abstract class ApiService implements IConstanteNumerique {

	protected static final Logger LOGGER_APISERVICE = LogManager.getLogger(ApiService.class);

	protected int maxParallelWorkers;

	// anti-spam delay when thread chain error
	protected static final int PREVENT_ERROR_SPAM_DELAY = 100;

	protected ScalableConnection connexion;

	protected Sandbox coordinatorSandbox;

	protected String envExecution;
	protected String tablePrevious;

	protected TraitementPhase previousPhase;
	protected TraitementPhase currentPhase;

	protected String tablePil;
	protected String tablePilTemp;

	private Integer nbEnr;
	protected String paramBatch = null;

	// made to report the number of object processed by the phase
	private int reportNumberOfObject = 0;

	protected String idSource;

	protected boolean todo = false;

	protected Map<String, List<String>> tabIdSource;

	public ApiService() {
		super();
		springInit();
	}

	protected ApiService(TraitementPhase aCurrentPhase, String aEnvExecution, Integer aNbEnr,
			String paramBatch) {

		StaticLoggerDispatcher.info(LOGGER_APISERVICE, "** initialiserVariable **");

		this.envExecution = Patch.normalizeSchemaName(aEnvExecution);

		try {
			this.connexion = new ScalableConnection(
					UtilitaireDao.get(ArcDatabase.COORDINATOR.getIndex()).getDriverConnexion());
			this.coordinatorSandbox = new Sandbox(this.connexion.getCoordinatorConnection(), this.envExecution);
		} catch (Exception ex) {
			LoggerHelper.error(LOGGER_APISERVICE, ApiService.class, "Error in initializing connexion");
		}

		// current phase and compute the previous phase
		this.currentPhase = aCurrentPhase;
		this.previousPhase = this.getCurrentPhase().previousPhase();

		// number of object to be proceed
		this.nbEnr = aNbEnr;

		// indicate if api is triggered by batch or not
		this.paramBatch = paramBatch;

		// inputTables
		this.tablePrevious = TableNaming.phaseDataTableName(this.envExecution, this.getPreviousPhase(),
				TraitementEtat.OK);

		// Tables de pilotage et pilotage temporaire
		this.tablePil = ViewEnum.PILOTAGE_FICHIER.getFullName(this.envExecution);
		this.tablePilTemp = TableNaming.temporaryTableName(this.envExecution, aCurrentPhase, ViewEnum.PILOTAGE_FICHIER);

		StaticLoggerDispatcher.info(LOGGER_APISERVICE, "** Fin constructeur ApiService **");
	}

	/**
	 * Initialisation des variable et des noms de table
	 *
	 * @param aEnvExecution
	 * @param aPreviousPhase
	 * @param aCurrentPhase
	 * @param aNbEnr
	 */
	private boolean initialiser() {
		LoggerHelper.info(LOGGER_APISERVICE, "** initialiser **");
		// Vérifie si y'a des sources à traiter
		if (this.todo) {
			try {
				UtilitaireDao.get(0).executeBlock(this.connexion.getCoordinatorConnection(),
						DatabaseConnexionConfiguration.configConnection(this.getEnvExecution()));
			} catch (ArcException ex) {
				LoggerHelper.error(LOGGER_APISERVICE, ApiService.class, "initialiser()", ex);
			}
			register(this.connexion.getCoordinatorConnection(), this.getPreviousPhase(), this.getCurrentPhase(),
					this.getTablePil(), this.tablePilTemp, this.nbEnr);
		}

		return this.todo;
	}

	/** Manually autowire the factory-produced instance. */
	private void springInit() {
		try {
			SpringApplicationContext.autowire(this);
		} catch (Exception e) {
			//
		}
	}

	/**
	 * Vérifier si y'a des fichiers à traiter on teste dans la phase précédente si
	 * on trouve des fichiers OK avec etape=1
	 *
	 * @param tablePil
	 * @param phaseAncien
	 * @return
	 */
	private boolean checkTodo(String tablePil, TraitementPhase phaseAncien) {
		ArcPreparedStatementBuilder requete = new ArcPreparedStatementBuilder();
		boolean checkTodoResult = false;
		requete.append("SELECT 1 FROM " + tablePil + " a ");
		requete.append("WHERE phase_traitement=" + requete.quoteText(phaseAncien.toString()) + " AND "
				+ requete.quoteText(TraitementEtat.OK.toString()) + "=ANY(etat_traitement) ");
		requete.append("and etape=1 ");
		requete.append("limit 1 ");
		try {
			checkTodoResult = UtilitaireDao.get(0).hasResults(this.connexion.getCoordinatorConnection(), requete);
		} catch (Exception ex) {
			LoggerHelper.error(LOGGER_APISERVICE, ApiService.class, "checkTodo()", ex);
		}
		return checkTodoResult;
	}

	/**
	 * Marque dans la table de pilotage les id_source qui vont être traités dans la
	 * phase Si des id_source sont déjà en traitement, la méthode en selectionnera
	 * de nouveaux Copie la table de pilotage pour les id_source selectionnés. Cette
	 * table sera mis à jour pendant l'éxécution de la phase.
	 * 
	 * @param connexion
	 * @param phaseIn
	 * @param phase
	 * @param tablePil
	 * @param tablePilTemp
	 * @param nbEnr
	 * @throws ArcException
	 */
	private void register(Connection connexion, TraitementPhase phaseIn, TraitementPhase phase, String tablePil, String tablePilTemp,
			Integer nbEnr) {
		LoggerHelper.info(LOGGER_APISERVICE, "** register **");
		try {
			UtilitaireDao.get(0).executeBlock(connexion,
					PilotageOperations.queryCopieTablePilotage(tablePil, tablePilTemp, phaseIn, phase, nbEnr));
		} catch (Exception ex) {
			LoggerHelper.error(LOGGER_APISERVICE, ApiService.class, "register()", ex);
		}
	}

	public abstract void executer() throws ArcException;

	/**
	 * Finalise l'appel d'une phase Marque dans la table de pilotage globale les
	 * id_source qui ont été traités (recopie des état de la table de pilotage
	 * temporaire de la phase vers la table de pilotage globale) Efface les objets
	 * temporaires (tables, type, ...)
	 */
	public void finaliser() {
		LoggerHelper.info(LOGGER_APISERVICE, "finaliser");

		try {
			if (Boolean.TRUE.equals(this.todo)) {

				StringBuilder requete = new StringBuilder();
				requete.append(FormatSQL.dropTable(this.tablePilTemp));
				try {
					UtilitaireDao.get(0).executeBlock(this.connexion.getCoordinatorConnection(), requete);
				} catch (Exception ex) {
					LoggerHelper.error(LOGGER_APISERVICE, ApiService.class, "finaliser()", ex);
				}
			}
		} finally {
			try {
				if (this.connexion.getCoordinatorConnection() != null) {
					this.connexion.getCoordinatorConnection().close();
					this.connexion.setCoordinatorConnection(null);
				}
			} catch (Exception ex) {
				LoggerHelper.error(LOGGER_APISERVICE, ApiService.class, "finaliser()", ex);
			}

		}
	}

	/**
	 * liste les id_source pour une phase et un etat donnée dans une table de
	 * pilotage
	 *
	 * @param tablePilotage
	 * @param aCurrentPhase
	 * @param etat
	 * @return
	 */
	public Map<String, List<String>> pilotageListIdsource(String tablePilotage, TraitementPhase aCurrentPhase, String etat) {
		LoggerHelper.info(LOGGER_APISERVICE, "pilotageListIdsource");
		ArcPreparedStatementBuilder requete = new ArcPreparedStatementBuilder();
		requete.append("SELECT container, " + ColumnEnum.ID_SOURCE.getColumnName() + " FROM " + tablePilotage + " ");
		requete.append("WHERE phase_traitement=" + requete.quoteText(aCurrentPhase.toString()) + " ");
		requete.append("AND " + requete.quoteText(etat) + "=ANY(etat_traitement); ");
		try {
			return new GenericBean(
					UtilitaireDao.get(0).executeRequest(this.connexion.getCoordinatorConnection(), requete))
					.mapContent();
		} catch (ArcException ex) {
			LoggerHelper.error(LOGGER_APISERVICE, ApiService.class, "pilotageListIdSource()", ex);
		}
		return new HashMap<>();
	}

	/**
	 * Marque la phase et l'état d'un idsource dans une table de pilotage
	 *
	 * @param tablePilotage
	 * @param idSource
	 * @param phaseNew
	 * @param etatNew
	 * @return
	 */
	public static StringBuilder pilotageMarkIdsource(String tablePilotage, String idSource, TraitementPhase phaseNew,
			String etatNew, String rapport, String... jointure) {
		StringBuilder requete = new StringBuilder();
		requete.append("UPDATE " + tablePilotage + " ");
		requete.append("SET phase_traitement= '" + phaseNew + "' ");
		requete.append(", etat_traitement= '{" + etatNew + "}' ");
		if (rapport == null) {
			requete.append(", rapport= null ");
		} else {
			requete.append(", rapport= '" + rapport + "' ");
		}

		if (jointure.length > 0) {
			requete.append(", jointure= '" + jointure[0] + "'");
		}

		requete.append("WHERE " + ColumnEnum.ID_SOURCE.getColumnName() + "='" + idSource + "';\n");
		return requete;
	}

	/**
	 * Requête de sélection de la liste des colonnes des tables métier associée à
	 * une norme
	 *
	 * @param listeTable
	 * @return
	 */
	public static ArcPreparedStatementBuilder listeColonneTableMetierSelonFamilleNorme(String idFamille) {
		ArcPreparedStatementBuilder requete = new ArcPreparedStatementBuilder();

		requete.append("SELECT DISTINCT nom_variable_metier, type_variable_metier\n")
				.append("  FROM " + ViewEnum.IHM_MOD_VARIABLE_METIER.getFullName() + " \n")
				.append("  WHERE lower(id_famille)=lower(" + requete.quoteText(idFamille) + ")");

		return requete;
	}

	/**
	 *
	 * @return le temps d'execution
	 */
	public ServiceReporting invokeApi() {
		double start = System.currentTimeMillis();

		LoggerHelper.info(LOGGER_APISERVICE, "****** Execution " + this.getCurrentPhase() + " *******");
		try {

			if (this.getCurrentPhase().equals(TraitementPhase.INITIALISATION)
					|| this.getCurrentPhase().equals(TraitementPhase.RECEPTION)) {
				this.todo = true;
			} else {
				this.todo = checkTodo(this.getTablePil(), this.getPreviousPhase());
			}
			LoggerHelper.info(LOGGER_APISERVICE, "A faire - " + this.getCurrentPhase() + " : " + this.todo);

			if (this.initialiser()) {
				try {
					this.executer();
				} catch (ArcException ex) {
					LoggerHelper.error(LOGGER_APISERVICE, "Erreur dans " + this.getCurrentPhase());
					ex.logFullException();
					try {
						this.repriseSurErreur(this.connexion.getCoordinatorConnection(), this.getCurrentPhase(),
								this.getTablePil(), ex);
					} catch (ArcException ex2) {
						LoggerHelper.error(LOGGER_APISERVICE, "Error in ApiService.invokeApi.repriseSurErreur");
						ex2.logFullException();
					}
				}
			}
		} finally {
			this.finaliser();
		}

		LoggerHelper.info(LOGGER_APISERVICE, "****** Fin " + this.getCurrentPhase() + " *******");

		return new ServiceReporting(this.reportNumberOfObject, System.currentTimeMillis() - start);

	}

	public String getTablePilTemp() {
		return this.tablePilTemp;
	}

	public void setTablePilTemp(String tablePilTemp) {
		this.tablePilTemp = tablePilTemp;
	}

	/**
	 * Remise dans l'état juste avant le lancement des controles et insertion dans
	 * une table d'erreur
	 *
	 * @param connexion
	 * @param phase
	 * @param tablePil
	 * @param exception
	 * @param tableDrop
	 * @throws ArcException
	 */
	private void repriseSurErreur(Connection connexion, TraitementPhase phase, String tablePil, ArcException exception) throws ArcException {
		// nettoyage de la connexion
		// comme on arrive ici à cause d'une erreur, la base de donnée attend une fin de
		// la transaction
		// si on lui renvoie une requete SQL, il la refuse avec le message
		// ERROR: current transaction is aborted, commands ignored until end of
		// transaction block
		try {
			this.connexion.getCoordinatorConnection().setAutoCommit(false);
			this.connexion.getCoordinatorConnection().rollback();
		} catch (SQLException rollbackException) {
			throw new ArcException(rollbackException, ArcExceptionMessage.DATABASE_ROLLBACK_FAILED);
		}
		StringBuilder requete = new StringBuilder();
		requete.append("WITH t0 AS ( ");
		requete.append(PilotageOperations.queryUpdatePilotageError(phase, tablePil, exception));
		requete.append("\n RETURNING " + ColumnEnum.ID_SOURCE.getColumnName() + ") ");

		requete.append(PilotageOperations.queryResetPreviousPhaseMark(tablePil, null, "t0"));

		UtilitaireDao.get(0).executeBlock(connexion, requete);
	}

	/**
	 * permet de récupérer un tableau de la forme id_source | id1 , id2, id3 ...
	 * type_comp | comp1,comp2, comp3 ...
	 * 
	 * @return
	 * @throws ArcException
	 */
	protected Map<String, List<String>> recuperationIdSource() throws ArcException {

		ArcPreparedStatementBuilder query = new ArcPreparedStatementBuilder();
		query.append("SELECT p." + ColumnEnum.ID_SOURCE.getColumnName() + " ");
		query.append("FROM " + this.getTablePilTemp() + " p ");
		query.append("ORDER BY " + ColumnEnum.ID_SOURCE.getColumnName());
		query.append(";");

		Map<String, List<String>> pil = new GenericBean(
				UtilitaireDao.get(0).executeRequest(this.connexion.getCoordinatorConnection(), query)).mapContent();

		return (pil);

	}

	public String getEnvExecution() {
		return envExecution;
	}

	public Map<String, List<String>> getTabIdSource() {
		return tabIdSource;
	}

	public String getTablePil() {
		return tablePil;
	}

	public TraitementPhase getPreviousPhase() {
		return previousPhase;
	}

	public TraitementPhase getCurrentPhase() {
		return currentPhase;
	}

	public String getTablePrevious() {
		return tablePrevious;
	}

	public String getParamBatch() {
		return paramBatch;
	}

	public ScalableConnection getConnexion() {
		return connexion;
	}

	/**
	 * @return the idSource
	 */
	public String getIdSource() {
		return idSource;
	}

	public int getReportNumberOfObject() {
		return reportNumberOfObject;
	}

	public void setReportNumberOfObject(int reportNumberOfObject) {
		this.reportNumberOfObject = reportNumberOfObject;
	}

	public Sandbox getCoordinatorSandbox() {
		return coordinatorSandbox;
	}

	public Integer getNbEnr() {
		return nbEnr;
	}

}
