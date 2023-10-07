package fr.insee.arc.core.service.p3normage.thread;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.insee.arc.core.dataobjects.ArcPreparedStatementBuilder;
import fr.insee.arc.core.dataobjects.ColumnEnum;
import fr.insee.arc.core.dataobjects.ViewEnum;
import fr.insee.arc.core.model.TraitementEtat;
import fr.insee.arc.core.service.global.dao.DatabaseConnexionConfiguration;
import fr.insee.arc.core.service.global.dao.HashFileNameConversion;
import fr.insee.arc.core.service.global.dao.PilotageOperations;
import fr.insee.arc.core.service.global.dao.RulesOperations;
import fr.insee.arc.core.service.global.dao.TableNaming;
import fr.insee.arc.core.service.global.dao.TableOperations;
import fr.insee.arc.core.service.global.dao.ThreadOperations;
import fr.insee.arc.core.service.global.scalability.ScalableConnection;
import fr.insee.arc.core.service.global.thread.IThread;
import fr.insee.arc.core.service.p3normage.ApiNormageService;
import fr.insee.arc.core.service.p3normage.engine.NormageEngine;
import fr.insee.arc.core.util.StaticLoggerDispatcher;
import fr.insee.arc.utils.dao.UtilitaireDao;
import fr.insee.arc.utils.exception.ArcException;
import fr.insee.arc.utils.utils.FormatSQL;
import fr.insee.arc.utils.utils.Sleep;


/**
 * ThreadNormageService
 *
 *      1- créer la table des données à traiter dans le module</br>
 *      2- calcul de la norme, validité, periodicité sur chaque ligne de la table de donnée</br>
 *      3- déterminer pour chaque fichier si le normage s'est bien déroulé et marquer sa norme, sa validité et sa périodicité</br>
 *      4- créer les tables OK et KO; marquer les info de normage(norme, validité, périodicité) sur chaque ligne de donnée</br>
 *      5- transformation de table de donnée; mise à plat du fichier; suppression et relation</br>
 *      6- mettre à jour le nombre d'enregistrement par fichier après sa transformation</br>
 *      7- sortir les données du module vers l'application</br>
 *
 * @author Manuel SOULIER
 *
 */
public class ThreadNormageService extends ApiNormageService implements Runnable, IThread<ApiNormageService> {

    private static final Logger LOGGER = LogManager.getLogger(ThreadNormageService.class);

    private Thread t;
        
    private String tableNormageDataTemp;
    private String tableNormagePilTemp;

    private String tableNormageOKTemp;
    private String tableNormageKOTemp;
    
    private String tableNormageOK;
    private String tableNormageKO;

    private String structure;

	private ThreadOperations arcThreadGenericDao;

    @Override
    public void configThread(ScalableConnection connexion, int currentIndice, ApiNormageService theApi) {
    	
        this.idSource = theApi.getTabIdSource().get(ColumnEnum.ID_SOURCE.getColumnName()).get(currentIndice);
        this.connexion = connexion;

        // tables du thread
        
        this.tableNormageDataTemp = FormatSQL.temporaryTableName("normage_data_temp");
        this.tableNormagePilTemp = FormatSQL.temporaryTableName("normage_pil_Temp");
        
        this.tableNormageOKTemp = FormatSQL.temporaryTableName("ok_Temp");
        this.tableNormageKOTemp = FormatSQL.temporaryTableName("ko_Temp");       
        
        this.tableNormageOK = TableNaming.phaseDataTableName(theApi.getEnvExecution(), theApi.getCurrentPhase(), TraitementEtat.OK);
        this.tableNormageKO = TableNaming.phaseDataTableName(theApi.getEnvExecution(), theApi.getCurrentPhase(), TraitementEtat.KO);

        // tables héritées
        this.tablePil = theApi.getTablePil();
        this.tablePilTemp = theApi.getTablePilTemp();
        this.currentPhase = theApi.getCurrentPhase();
        this.tablePrevious = theApi.getTablePrevious();
        this.tabIdSource=theApi.getTabIdSource();
        this.envExecution=theApi.getEnvExecution();
        this.paramBatch=theApi.getParamBatch();
        
		// arc thread dao
		arcThreadGenericDao=new ThreadOperations(connexion, tablePil, tablePilTemp, tableNormagePilTemp, tablePrevious, paramBatch, idSource);

    }

    public void start() {
		StaticLoggerDispatcher.debug(LOGGER, "Starting ThreadNormageService");
        t = new Thread(this);
        t.start();
    }

    @Override
    public void run() {
        try {

            // créer la table des données à traiter dans le module
            creerTableTravail();

            // transformation de table de donnée; mise à plat du fichier; suppression et relation
            jointureBlocXML();
            
            // sortir les données du module vers l'application
            insertionFinale();

        } catch (ArcException e) {
            StaticLoggerDispatcher.error(LOGGER, e);
	    try {
			PilotageOperations.traitementSurErreur(this.connexion.getCoordinatorConnection(), this.getCurrentPhase(), this.tablePil, this.idSource, e);
		    } catch (ArcException e2) {
	            StaticLoggerDispatcher.error(LOGGER, e2);
		    }
            Sleep.sleep(PREVENT_ERROR_SPAM_DELAY);
        }
    }


    /**
     * Créer la table de travail du normage Contient les donnée d'un seul id source. Cela est du au fait que le type composite varie d'un id
     * source à l'autre,
     * 
     * @throws ArcException
     */
    private void creerTableTravail() throws ArcException {
        StaticLoggerDispatcher.info(LOGGER, "Créer les tables images");
    	ArcPreparedStatementBuilder query= arcThreadGenericDao.preparationDefaultDao();

        // Créer la table image de la phase précédente (ajouter les colonnes qu'il faut)
    	// création des tables temporaires de données
        query.append(TableOperations.createTableTravailIdSource(this.getTablePrevious(),this.tableNormageDataTemp, this.idSource));
       
        //On indique que le normage s'est bien passé par défaut
        query.append("\n UPDATE "+this.tableNormagePilTemp);
        query.append("\n SET etat_traitement = '{"+TraitementEtat.OK+"}'");
        query.append("\n , phase_traitement = '"+this.currentPhase+"'");
        query.append("\n WHERE "+ColumnEnum.ID_SOURCE.getColumnName()+"='"+this.idSource+"';");
        
        query.append(TableOperations.creationTableResultat(this.tableNormageDataTemp, this.tableNormageKOTemp));
                
        UtilitaireDao.get(0).executeBlock(this.getConnexion().getExecutorConnection(), query.getQueryWithParameters());

    }

    /**
     * Réaliser la jointure entre les blocs XML pour mettre les fichier à plat
     * Pour chaque fichier, on retravaille la requete de jointure obtenue en phase de chargement :
     * 1- en supprimant les blocs définis "à supprimer" par les regles utilisateurs du normage
     * 2- en ajoutant des conditions de jointures relationnelles entre 2 rubriques défini par les règle utilisateur de type "relation" dans les regles de normage
     *
     * Fait sur une maintenance urgente après réception des fichiers lot2 en moins de 2j ...
     * La méthode devrait etre refactor (pour séparer "deletion" et "relation")
     * La réécriture de la requete selon les règles utilisateurs devrait être moins adhérente à la structure de la requete issu du chargement
     * (trop dépendant des mot clés ou saut de ligne pour l'instant)
     * @throws ArcException
     *
     */
    private void jointureBlocXML() throws ArcException {

        StaticLoggerDispatcher.info(LOGGER, "jointureBlocXML()");

        // on parcours les fichiers pour effectuer la requete de jointure avec les regles de normage
        // c'est une des requete la plus couteuse de l'application (reconstitution de la structure plate à partir du modele xml)
        // pour chaque fichier, on va executer sa requete


        		// récupérer les caractéristiques du fichier
			    Map<String, List<String>> pil = RulesOperations.getBean(this.connexion.getExecutorConnection(),RulesOperations.getNormeAttributes(this.idSource, tableNormagePilTemp));

			    // récupéreration des règles relative au fichier pour la phase courante
			    Map<String,List<String>> regle = RulesOperations.getBean(this.connexion.getExecutorConnection(),RulesOperations.getRegles(ViewEnum.NORMAGE_REGLE.getFullName(envExecution), this.tableNormagePilTemp));
			    
			    
		        // récupéreration des rubriques utilisées dans règles relative au fichier pour l'ensemble des phases
		        
			    Map<String,List<String>> rubriqueUtiliseeDansRegles=null;
			    
			    if (paramBatch!=null)
			    {
				    String tableTmpRubriqueDansregles="TMP_RUBRIQUE_DANS_REGLES";
				    
				    StringBuilder query=new StringBuilder();
				    query.append("\n DROP TABLE IF EXISTS "+tableTmpRubriqueDansregles+";");
				    query.append("\n CREATE TEMPORARY TABLE "+tableTmpRubriqueDansregles+" AS ");
				    query.append(RulesOperations.getAllRubriquesInRegles(this.tableNormagePilTemp, ViewEnum.NORMAGE_REGLE.getFullName(envExecution), ViewEnum.CONTROLE_REGLE.getFullName(envExecution), ViewEnum.MAPPING_REGLE.getFullName(envExecution)));
			        UtilitaireDao.get(0).executeImmediate(
			        		this.connexion.getExecutorConnection(),
			        		query
			        		);
				    
				    rubriqueUtiliseeDansRegles = RulesOperations.getBean(this.connexion.getExecutorConnection(),RulesOperations.getRegles(tableTmpRubriqueDansregles, this.tableNormagePilTemp));
			    }
		    
			    NormageEngine n=new NormageEngine(this.connexion.getExecutorConnection() , pil, regle, rubriqueUtiliseeDansRegles, this.tableNormageDataTemp, this.tableNormageOKTemp, this.paramBatch);
			    n.execute();
    }
    
    
 

	/**
	 * Remplace les UNION ALL par des inserts
	 * @param jointure
	 * @return
	 */

    /**
     * On sort les données des tables temporaires du module vers : - les tables définitives du normage (normage_ok et normage_ko) de
     * l'application - la vraie table de pilotage - la table buffer
     *
     * IMPORTANT : les ajouts ou mise à jours de données sur les tables de l'application doivent avoir lieu dans un même bloc de transaction
     * (ACID)
     * 
     * @throws ArcException
     *
     */
    private void insertionFinale() throws ArcException {
    	
    	ArcPreparedStatementBuilder query=new ArcPreparedStatementBuilder();
    	
    	// update the number of record ans structure in the pilotage table
    	query.append(PilotageOperations.queryUpdateNbEnr(this.tableNormagePilTemp, this.tableNormageOKTemp, this.structure));
    
    	// promote the application user account to full right
    	query.append(DatabaseConnexionConfiguration.switchToFullRightRole());
    	
    	String tableIdSourceOK=HashFileNameConversion.tableOfIdSource(this.tableNormageOK ,this.idSource);
    	query.append(TableOperations.createTableInherit(this.tableNormageOKTemp, tableIdSourceOK));
        String tableIdSourceKO=HashFileNameConversion.tableOfIdSource(this.tableNormageKO ,this.idSource);
        query.append(TableOperations.createTableInherit(this.tableNormageKOTemp, tableIdSourceKO));
		
        // mark file as done into global pilotage table
        arcThreadGenericDao.marquageFinalDefaultDao(query);
        
    }

    public ScalableConnection getConnexion() {
        return connexion;
    }

    public void setConnexion(ScalableConnection connexion) {
        this.connexion = connexion;
    }

    public Thread getT() {
        return t;
    }
    

}


