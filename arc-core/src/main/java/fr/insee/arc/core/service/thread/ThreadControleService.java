package fr.insee.arc.core.service.thread;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.insee.arc.core.dataobjects.ArcPreparedStatementBuilder;
import fr.insee.arc.core.dataobjects.ColumnEnum;
import fr.insee.arc.core.model.JeuDeRegle;
import fr.insee.arc.core.model.TraitementEtat;
import fr.insee.arc.core.service.api.ApiControleService;
import fr.insee.arc.core.service.api.ApiService;
import fr.insee.arc.core.service.api.query.ServiceHashFileName;
import fr.insee.arc.core.service.api.query.ServiceTableNaming;
import fr.insee.arc.core.service.api.query.ServiceTableOperation;
import fr.insee.arc.core.service.engine.controle.ServiceJeuDeRegle;
import fr.insee.arc.core.service.engine.controle.ServiceRequeteSqlRegle;
import fr.insee.arc.core.util.StaticLoggerDispatcher;
import fr.insee.arc.utils.dao.UtilitaireDao;
import fr.insee.arc.utils.exception.ArcException;
import fr.insee.arc.utils.utils.FormatSQL;
import fr.insee.arc.utils.utils.Sleep;

/**
 * Comme pour le normage, on parallélise en controlant chaque
 * fichier dans des threads séparés.
 * 
 * @author S4LWO8
 *
 */
public class ThreadControleService extends ApiControleService implements Runnable, ArcThread<ApiControleService> {

	private static final Logger LOGGER = LogManager.getLogger(ThreadControleService.class);

	private Thread t = null;
	
	private int indice;

	private String tableControleDataTemp;
	private String tableControlePilTemp;
	private String tableOutOkTemp = "tableOutOkTemp";
	private String tableOutKoTemp = "tableOutKoTemp";
	private String tableOutOk;
	private String tableOutKo;

	private ServiceJeuDeRegle sjdr;

	private JeuDeRegle jdr;

	private String structure;
	
    private ArcThreadGenericDao arcThreadGenericDao;
 

	@Override
	public void configThread(ScalableConnection connexion, int currentIndice, ApiControleService theApi) {

		this.indice = currentIndice;
		this.setEnvExecution(theApi.getEnvExecution());
		this.idSource = theApi.getTabIdSource().get(ColumnEnum.ID_SOURCE.getColumnName()).get(indice);
		this.connexion = connexion;
		this.setTablePil(theApi.getTablePil());
		this.tablePilTemp = theApi.getTablePilTemp();

		this.setPreviousPhase(theApi.getPreviousPhase());
		this.setCurrentPhase(theApi.getCurrentPhase());

		this.setNbEnr(theApi.getNbEnr());

		this.setTablePrevious(theApi.getTablePrevious());
		this.setTabIdSource(theApi.getTabIdSource());

		this.setTableNorme(theApi.getTableNorme());
		this.setTableNormageRegle(theApi.getTableNormageRegle());

		this.setParamBatch(theApi.getParamBatch());

		this.setTableJeuDeRegle(theApi.getTableJeuDeRegle());
		this.setTableControleRegle(theApi.getTableControleRegle());

		this.sjdr = new ServiceJeuDeRegle(theApi.getTableControleRegle());
		this.jdr = new JeuDeRegle();

		// Nom des tables temporaires
		this.tableControleDataTemp = FormatSQL.temporaryTableName("controle_data_temp");
		this.tableControlePilTemp = FormatSQL.temporaryTableName("controle_pil_temp");

		// tables finales
		this.tableOutOk = ServiceTableNaming.dbEnv(this.getEnvExecution()) + this.getCurrentPhase() + "_" + TraitementEtat.OK;
		this.tableOutKo = ServiceTableNaming.dbEnv(this.getEnvExecution()) + this.getCurrentPhase() + "_" + TraitementEtat.KO;
		
		// arc thread dao
		arcThreadGenericDao=new ArcThreadGenericDao(connexion, tablePil, tablePilTemp, tableControlePilTemp, tablePrevious, paramBatch, idSource);
	}

	@Override
	public void run() {
		try {

			preparation();

			execute();

			insertionFinale();

		} catch (ArcException e) {
			StaticLoggerDispatcher.error("Error in control Thread", LOGGER);
			try {
				this.repriseSurErreur(this.connexion.getExecutorConnection(), this.getCurrentPhase(), this.tablePil,
						this.idSource, e, "aucuneTableADroper");
			} catch (ArcException e2) {
				StaticLoggerDispatcher.error(e2, LOGGER);
			}
			Sleep.sleep(PREVENT_ERROR_SPAM_DELAY);
		}
	}

	public void start() {
		StaticLoggerDispatcher.debug("Starting ThreadControleService", LOGGER);
		t = new Thread(this);
	    t.start();
	}

	/**
	 * Préparation des données et implémentation des jeux de règles utiles
	 *
	 * @param this.connexion
	 *
	 * @param tableIn         la table issue du chargement-normage
	 *
	 * @param env             l'environnement d'execution
	 * @param tableControle   la table temporaire à controler
	 * @param tablePilTemp    la table temporaire listant les fichiers en cours de
	 *                        traitement
	 * @param tableJeuDeRegle la table des jeux de règles
	 * @param tableRegleC     la table des règles de controles
	 * @throws ArcException
	 */
	private void preparation() throws ArcException {
		StaticLoggerDispatcher.info("** preparation **", LOGGER);

    	ArcPreparedStatementBuilder query= arcThreadGenericDao.preparationDefaultDao();

		// Marquage du jeux de règles appliqué
		StaticLoggerDispatcher.info("Marquage du jeux de règles appliqués ", LOGGER);
		query.append(marqueJeuDeRegleApplique(this.tableControlePilTemp, TraitementEtat.OK.toString()));

		// Fabrication de la table de controle temporaire
		StaticLoggerDispatcher.info("Fabrication de la table de controle temporaire ", LOGGER);
		query.append(ServiceTableOperation.createTableTravailIdSource(this.getTablePrevious(), this.tableControleDataTemp, this.idSource,
				"'" + ServiceRequeteSqlRegle.RECORD_WITH_NOERROR
						+ "'::text collate \"C\" as controle, null::text[] collate \"C\" as brokenrules"));

		UtilitaireDao.get("arc").executeBlock(this.connexion.getExecutorConnection(), query.getQueryWithParameters());

		// Récupération des Jeux de règles associés
		this.sjdr.fillRegleControle(this.connexion.getExecutorConnection(), jdr, this.getTableControleRegle(),
				this.tableControleDataTemp);
		this.structure = UtilitaireDao.get("arc").getString(this.connexion.getExecutorConnection(),
				new ArcPreparedStatementBuilder("SELECT jointure FROM " + this.tableControlePilTemp));
	}

	/**
	 * Méthode pour controler une table
	 *
	 * @param connexion
	 *
	 * @param tableControle la table à controler
	 *
	 * @throws ArcException
	 */
	private void execute() throws ArcException {
		StaticLoggerDispatcher.info("** execute CONTROLE sur la table : " + this.tableControleDataTemp + " **", LOGGER);

		this.sjdr.executeJeuDeRegle(this.connexion.getExecutorConnection(), jdr, this.tableControleDataTemp,
				this.structure);

	}

	/**
	 * Méthode à passer après les controles
	 *
	 * @param connexion
	 *
	 * @param tableIn    la table temporaire avec les marquage du controle
	 * @param tableOutOk la table permanente sur laquelle on ajoute les bons
	 *                   enregistrements de tableIn
	 * @param tableOutKo la table permanente sur laquelle on ajoute les mauvais
	 *                   enregistrements de tableIn
	 * @param tablePil   la table de pilotage des fichiers
	 * @param tableSeuil la table des seuils
	 * @throws ArcException
	 */
	private StringBuilder calculSeuilControle() throws ArcException {
		StaticLoggerDispatcher.info("finControle", LOGGER);

		StringBuilder blocFin = new StringBuilder();
		// Creation des tables temporaires ok et ko
		StaticLoggerDispatcher.info("Creation des tables temporaires ok et ko", LOGGER);

		// Execution à mi parcours du bloc de requete afin que les tables tempo soit
		// bien créées
		// ensuite dans le java on s'appuie sur le dessin de ces tables pour ecrire du
		// SQL
		blocFin.append(ServiceTableOperation.creationTableResultat(this.tableControleDataTemp, tableOutOkTemp));
		blocFin.append(ServiceTableOperation.creationTableResultat(this.tableControleDataTemp, tableOutKoTemp));

		// Marquage des résultat du control dans la table de pilotage
		StaticLoggerDispatcher.info("Marquage dans la table de pilotage", LOGGER);
		blocFin.append(marquagePilotage());

		// insert in OK when
		// etat traitement in OK or OK,KO
		// AND records which have no error or errors that can be kept
		StaticLoggerDispatcher.info("Insertion dans OK", LOGGER);
		blocFin.append(ajoutTableControle(this.tableControleDataTemp, tableOutOkTemp, this.tableControlePilTemp,
				"etat_traitement in ('{" + TraitementEtat.OK + "}','{" + TraitementEtat.OK + "," + TraitementEtat.KO
						+ "}') ",
				"controle in ('" + ServiceRequeteSqlRegle.RECORD_WITH_NOERROR + "','"
						+ ServiceRequeteSqlRegle.RECORD_WITH_ERROR_TO_KEEP + "') AND "));

		// insert in OK when
		// etat traitement in KO
		// OR records which have errors that must be excluded
		StaticLoggerDispatcher.info("Insertion dans KO", LOGGER);
		blocFin.append(ajoutTableControle(this.tableControleDataTemp, tableOutKoTemp, this.tableControlePilTemp,
				"etat_traitement ='{" + TraitementEtat.KO + "}' ",
				"controle='" + ServiceRequeteSqlRegle.RECORD_WITH_ERROR_TO_EXCLUDE + "' OR "));

		return blocFin;
	}

	/**
	 * Insertion dans les tables finales
	 * 
	 * @throws ArcException
	 */
	private void insertionFinale() throws ArcException {

		// calcul des seuils pour finalisation
		ArcPreparedStatementBuilder query = new ArcPreparedStatementBuilder();
		query.append(calculSeuilControle());

		// promote the application user account to full right
		query.append(switchToFullRightRole());

		// Créer les tables héritées
		String tableIdSourceOK = ServiceHashFileName.tableOfIdSource(tableOutOk, this.idSource);
		query.append(ServiceTableOperation.createTableInherit(tableOutOkTemp, tableIdSourceOK));
		String tableIdSourceKO = ServiceHashFileName.tableOfIdSource(tableOutKo, this.idSource);
		query.append(ServiceTableOperation.createTableInherit(tableOutKoTemp, tableIdSourceKO));

		// mark file as done in the pilotage table
		arcThreadGenericDao.marquageFinalDefaultDao(query);
		
	}

	/**
	 * Marque les résultats des contrôles dans la table de pilotage
	 * 
	 * @return
	 */
	private String marquagePilotage() {
		StringBuilder blocFin = new StringBuilder();
		blocFin.append("\n UPDATE " + this.tableControlePilTemp + " ");
		blocFin.append("\n SET etat_traitement= ");
		blocFin.append("\n case ");
		blocFin.append("\n when exists (select from " + ServiceRequeteSqlRegle.TABLE_TEMP_META
				+ " where blocking) then '{" + TraitementEtat.KO + "}'::text[] ");
		blocFin.append("\n when exists (select from " + ServiceRequeteSqlRegle.TABLE_TEMP_META + " where controle='"
				+ ServiceRequeteSqlRegle.RECORD_WITH_ERROR_TO_EXCLUDE + "') then '{" + TraitementEtat.OK + ","
				+ TraitementEtat.KO + "}'::text[] ");
		blocFin.append("\n else '{OK}'::text[] ");
		blocFin.append("\n end ");
		blocFin.append(
				"\n , rapport='Control failed on : '||(select array_agg(brokenrules||case when blocking then ' (blocking rules)' else '' end||case when controle='"
						+ ServiceRequeteSqlRegle.RECORD_WITH_ERROR_TO_EXCLUDE
						+ "' then ' (exclusion rules)' else '' end)::text from "
						+ ServiceRequeteSqlRegle.TABLE_TEMP_META + ") ");
		blocFin.append("\n WHERE exists (select from " + ServiceRequeteSqlRegle.TABLE_TEMP_META + ") ");
		blocFin.append(";");
		return blocFin.toString();
	}

	/**
	 * Insertion des données d'une table dans une autre avec un critère de sélection
	 *
	 * @param listColTableIn
	 *
	 * @param phase
	 *
	 * @param tableIn              la table des données à insérer
	 * @param tableOut             la table réceptacle
	 * @param tableControlePilTemp la table de pilotage des fichiers
	 * @param etatNull             pour sélectionner certains fichiers
	 * @param condEnregistrement   la condition pour filtrer la recopie
	 * @return
	 */
	private String ajoutTableControle(String tableIn, String tableOut, String tableControlePilTemp, String condFichier,
			String condEnregistrement) {

		StringBuilder requete = new StringBuilder();
		requete.append("\n INSERT INTO " + tableOut + " ");
		requete.append("\n SELECT * ");
		requete.append("\n FROM " + tableIn + " a ");
		requete.append("\n WHERE " + condEnregistrement + " ");
		requete.append("\n EXISTS (select 1 from  " + tableControlePilTemp + " b where " + condFichier + ") ");
		requete.append(";");
		return requete.toString();
	}

	// Getter et Setter
	public ServiceJeuDeRegle getSjdr() {
		return this.sjdr;
	}

	public void setSjdr(ServiceJeuDeRegle sjdr) {
		this.sjdr = sjdr;
	}

	@Override
	public Thread getT() {
		return t;
	}

	@Override
	public ScalableConnection getConnexion() {
		return connexion;
	}

	public void setConnexion(ScalableConnection connexion) {
		this.connexion = connexion;
	}

}
