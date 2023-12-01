package fr.insee.arc.ws.services.importServlet.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import fr.insee.arc.core.dataobjects.ArcDatabase;
import fr.insee.arc.core.dataobjects.ArcPreparedStatementBuilder;
import fr.insee.arc.core.dataobjects.ColumnEnum;
import fr.insee.arc.core.dataobjects.ViewEnum;
import fr.insee.arc.core.model.Delimiters;
import fr.insee.arc.core.model.TraitementEtat;
import fr.insee.arc.core.model.TraitementPhase;
import fr.insee.arc.core.service.global.dao.TableNaming;
import fr.insee.arc.utils.dao.CopyObjectsToDatabase;
import fr.insee.arc.utils.dao.SQL;
import fr.insee.arc.utils.dao.UtilitaireDao;
import fr.insee.arc.utils.exception.ArcException;
import fr.insee.arc.utils.exception.ArcExceptionMessage;
import fr.insee.arc.utils.structure.GenericBean;
import fr.insee.arc.utils.utils.FormatSQL;
import fr.insee.arc.utils.utils.LoggerHelper;
import fr.insee.arc.utils.utils.ManipString;
import fr.insee.arc.ws.services.importServlet.bo.ArcClientIdentifier;
import fr.insee.arc.ws.services.importServlet.bo.ExportTrackingType;
import fr.insee.arc.ws.services.importServlet.bo.JsonKeys;
import fr.insee.arc.ws.services.importServlet.bo.TableToRetrieve;

public class ClientDao {

	protected static final Logger LOGGER = LogManager.getLogger(ClientDao.class);

	private long timestamp;
	private String environnement;
	private String client;
	private String famille;

	// the tablename of the table that contains document data to retrieve identified
	// by id_source
	private String tableOfIdSource;

	// the tablename of the table that shows webservice is still creating table to
	// be consumed by the client
	// it is dropped when client had built all the data table
	private String tableWsPending;

	// the tablename of the table that tracks tables left to retrieved
	private String tableWsTracking;

	private Connection connection;

	public ClientDao(ArcClientIdentifier arcClientIdentifier) {
		this.timestamp = arcClientIdentifier.getTimestamp();
		this.environnement = arcClientIdentifier.getEnvironnement();
		this.client = arcClientIdentifier.getClientIdentifier();
		this.famille = arcClientIdentifier.getFamille();

		this.tableOfIdSource = TableNaming.buildTableNameWithTokens(environnement, ViewEnum.ID_SOURCE, client,
				timestamp);
		this.tableWsPending = TableNaming.buildTableNameWithTokens(environnement, ViewEnum.WS_PENDING, client,
				timestamp);
		this.tableWsTracking = TableNaming.buildTableNameWithTokens(environnement, ViewEnum.WS_TRACKING, client,
				timestamp);

	}

	/**
	 * Vérifie que le client peut consulter les tables métiers de la famille de
	 * normes
	 */
	public boolean verificationClientFamille() throws ArcException {
		LoggerHelper.debugAsComment(LOGGER, timestamp, "ClientDaoImpl#verificationClientFamille()");

		ArcPreparedStatementBuilder request = new ArcPreparedStatementBuilder();
		request.append("SELECT EXISTS (SELECT 1 FROM arc.ihm_client")
				.append(" WHERE id_application=" + request.quoteText(client))
				.append(" AND id_famille=" + request.quoteText(famille))
				.append(" LIMIT 1);");

		String bool = UtilitaireDao.get(0).executeRequestWithoutMetadata(connection, request).get(0).get(0);

		return bool.equals("t");

	}

	/**
	 * return the list of business data table related to the famille provided
	 * 
	 * @return
	 * @throws ArcException
	 */
	public List<String> selectBusinessDataTables() throws ArcException {

		ArcPreparedStatementBuilder request = new ArcPreparedStatementBuilder();
		request.append("SELECT " + ColumnEnum.NOM_TABLE_METIER + " ");
		request.append("FROM " + ViewEnum.MOD_TABLE_METIER.getFullName(environnement) + " T1 ");
		request.append("WHERE T1.id_famille='" + this.famille + "' ");
		request.append(";");

		return new GenericBean(UtilitaireDao.get(0).executeRequest(connection, request))
				.getColumnValues(ColumnEnum.NOM_TABLE_METIER.getColumnName());
	}

	/**
	 * register the table to be retrieved in tracking table
	 * @param wsTrackingType
	 * @param targetNod
	 * @param nomTable
	 * @throws ArcException
	 */
	private void registerTableToBeRetrieved(ExportTrackingType wsTrackingType, ArcDatabase targetNod, String nomTable)
			throws ArcException {
		ArcPreparedStatementBuilder query = new ArcPreparedStatementBuilder();
		query.build(SQL.INSERT_INTO, this.tableWsTracking, "(tracking_type, nod, table_to_retrieve)");
		query.build(SQL.SELECT, query.quoteText(wsTrackingType.toString()), ",", query.quoteText(targetNod.toString()),
				",", query.quoteText(nomTable));
		UtilitaireDao.get(0).executeRequest(connection, query);
	}

	/**
	 * Créer une image des tables métiers.
	 * 
	 * @param tablesMetierNames La liste des noms des tables métiers.
	 *
	 * @return liste des noms de tables images crées
	 * @throws ArcException
	 */
	private String addImage(String tableMetier, int executorConnectionId) throws ArcException {
		StringBuilder request = new StringBuilder();

		String nomTableImage = TableNaming.buildTableNameWithTokens(environnement, tableMetier, client, timestamp);

		request.append("DROP TABLE IF EXISTS " + nomTableImage + "; ");

		request.append("CREATE TABLE " + nomTableImage + FormatSQL.WITH_NO_VACUUM + " AS ");
		request.append("SELECT * ");
		request.append("FROM " + ViewEnum.getFullName(environnement, tableMetier) + " T1 WHERE true ");
		request.append("AND exists (SELECT 1 FROM " + tableOfIdSource + " T2 where T2."
				+ ColumnEnum.ID_SOURCE.getColumnName() + "=T1." + ColumnEnum.ID_SOURCE.getColumnName() + "); ");

		UtilitaireDao.get(executorConnectionId).executeBlock(connection, request);

		registerTableToBeRetrieved(ExportTrackingType.DATA, ArcDatabase.EXECUTOR, nomTableImage);
		
		return nomTableImage;

	}

	/**
	 * Met à jours les colonnes client et date_client de la table
	 * environnement_pilotage_fichier.
	 * 
	 * @param tableSource
	 * @throws ArcException
	 */
	public void updatePilotage(String tableSource) throws ArcException {
		LoggerHelper.debugAsComment(LOGGER, timestamp, ": ClientDaoImpl.updatePilotage()");

		String clientOfTableSource = extractClientFromToken();

		StringBuilder query = new StringBuilder();
		query.append("UPDATE " + ViewEnum.PILOTAGE_FICHIER.getFullName(environnement) + " T1 ");
		query.append("SET client = array_append(client, '" + clientOfTableSource + "') ");
		query.append(", date_client = array_append( date_client, localtimestamp ) ");
		query.append("WHERE true ");
		query.append("AND EXISTS (SELECT 1 FROM " + tableSource + " T2 where T1." + ColumnEnum.ID_SOURCE.getColumnName()
				+ "=T2." + ColumnEnum.ID_SOURCE.getColumnName() + ") ");
		query.append("AND T1.phase_traitement='" + TraitementPhase.MAPPING + "';");

		UtilitaireDao.get(0).executeBlock(connection, query.toString());
	}

	/**
	 * extract the client token name from the client
	 * 
	 * @param client2
	 * @return
	 */
	private String extractClientFromToken() {
		return ManipString.substringBeforeFirst(
				ManipString.substringAfterFirst(this.client, Delimiters.SQL_SCHEMA_DELIMITER),
				Delimiters.SQL_TOKEN_DELIMITER);
	}

	public void createTableTrackRetrievedTables() throws ArcException {
		ArcPreparedStatementBuilder query = new ArcPreparedStatementBuilder();
		query.build(SQL.DROP, SQL.TABLE, SQL.IF_EXISTS, this.tableWsTracking, SQL.END_QUERY);
		query.build(SQL.CREATE, SQL.TABLE, this.tableWsTracking,
				" (tracking_type text, nod text, table_to_retrieve text) ", SQL.END_QUERY);
		UtilitaireDao.get(0).executeRequest(connection, query);

		registerTableToBeRetrieved(ExportTrackingType.TRACK, ArcDatabase.COORDINATOR, this.tableWsTracking);
	}

	/**
	 * Create the container with all the files name (idSource) that will be retrieve
	 * This query is built around the parameters given in the json request
	 * PERIODICITE : A for annual file, M for monthly file VALINF : the minimum
	 * validity date required for the file VALSUP : the maximum validity date
	 * required for the file NBFICHIERS : number of file to retrieve. most recent
	 * file first REPRISE : tell if arc wont mark file as retrieved
	 * 
	 * @param query
	 * @param requeteJSON
	 * @throws ArcException
	 */
	public void createTableOfIdSource(JSONObject requeteJSON) throws ArcException {

		String periodicite = requeteJSON.getString(JsonKeys.PERIODICITE.getKey());
		String validiteInf = requeteJSON.keySet().contains(JsonKeys.VALINF.getKey())
				? requeteJSON.getString(JsonKeys.VALINF.getKey())
				: null;
		String validiteSup = requeteJSON.getString(JsonKeys.VALSUP.getKey());

		boolean reprise = requeteJSON.getBoolean(JsonKeys.REPRISE.getKey());

		StringBuilder query = new StringBuilder();
		query.append("DROP TABLE IF EXISTS " + tableOfIdSource + "; ");

		query.append("CREATE TABLE " + tableOfIdSource + " ");
		query.append("AS SELECT " + ColumnEnum.ID_SOURCE.getColumnName() + " FROM ");

		query.append("(");
		query.append("SELECT " + ColumnEnum.ID_SOURCE.getColumnName()
				+ " FROM " + ViewEnum.PILOTAGE_FICHIER.getFullName(this.environnement) + " T1 ");
		query.append(
				"WHERE '" + TraitementEtat.OK + "'=ANY(T1.etat_traitement) AND T1.periodicite='" + periodicite + "' ");

		if (validiteInf != null) {
			query.append("AND validite>='" + validiteInf + "' ");
		}

		query.append("AND validite<='" + validiteSup + "' AND T1.phase_traitement='" + TraitementPhase.MAPPING + "' ");
		query.append("AND EXISTS (SELECT 1 FROM " + ViewEnum.NORME.getFullName(environnement)
				+ " T2 WHERE T2.id_famille='" + famille + "' AND T1.id_norme=T2.id_norme) ");

		// if reprise is true, we want to retrieve all files, even the one which had
		// been already retrieved
		if (!reprise) {
			LoggerHelper.debugAsComment(LOGGER, "ClientDaoImpl.getIdSrcTableMetier() : Reprise = false");
			query.append("AND NOT '" + client + "' = ANY(coalesce(T1.client, ARRAY[]::text[])) ");
		} else {
			LoggerHelper.debugAsComment(LOGGER, "ClientDaoImpl.getIdSrcTableMetier() : Reprise = true");
		}

		query.append("GROUP BY " + ColumnEnum.ID_SOURCE.getColumnName()); // )
		query.append(") as foo; ");

		UtilitaireDao.get(0).executeBlock(connection, query);

		registerTableToBeRetrieved(ExportTrackingType.ID_SOURCE, ArcDatabase.EXECUTOR, tableOfIdSource);

	}

	/**
	 * Créer une image des tables métiers.
	 * 
	 * @param tablesMetierNames La liste des noms des tables métiers.
	 *
	 * @return liste des noms de tables images crées
	 * @throws ArcException
	 */
	public List<String> createImages(List<String> tablesMetierNames, int executorConnectionId) throws ArcException {
		LoggerHelper.debugAsComment(LOGGER, timestamp, "ClientDaoImpl.createImage()");

		List<String> dataTableImages = new ArrayList<>(); 
		
		for (String tableMetier : tablesMetierNames) {
			dataTableImages.add(addImage(tableMetier, executorConnectionId));
		}
		return dataTableImages;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * fr.insee.arc_essnet.ws.dao.ClientDarcl(fr.insee.arc_essnet.ws.actions.Senarc
	 */
	public void createTableNmcl() throws ArcException {
		LoggerHelper.debugAsComment(LOGGER, "ClientDaoImpl.createNmcl()");

		ArcPreparedStatementBuilder requete = new ArcPreparedStatementBuilder();
		requete.append("SELECT tablename FROM pg_tables ")
				.append(" WHERE schemaname = " + requete.quoteText(environnement))
				.append(" AND tablename LIKE " + requete.quoteText("nmcl_%"));

		List<List<String>> nmclNames = UtilitaireDao.get(0).executeRequestWithoutMetadata(connection, requete);

		for (List<String> nmcl : nmclNames) {
			String nomTableImage = ViewEnum.getFullNameNotNormalized(environnement, client + "_" + timestamp + "_" + nmcl.get(0));
			UtilitaireDao.get(0).executeImmediate(connection, "CREATE TABLE " + nomTableImage + FormatSQL.WITH_NO_VACUUM
					+ " AS SELECT * FROM " + ViewEnum.getFullName(environnement, nmcl.get(0)) + ";");
			registerTableToBeRetrieved(ExportTrackingType.DATA, ArcDatabase.COORDINATOR, nomTableImage);
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see fr.insee.arc_essnet.ws.dao.ClientDarcMetier(java.lang.String,
	 * fr.insee.arc_essnet.ws.actions.Senarc
	 */
	public void createTableVarMetier() throws ArcException {
		LoggerHelper.debugAsComment(LOGGER, "ClientDaoImpl.createVarMetier()");

		String nomTableImage = TableNaming.buildTableNameWithTokens(environnement, ViewEnum.MOD_VARIABLE_METIER, client,
				timestamp);

		ArcPreparedStatementBuilder requete = new ArcPreparedStatementBuilder();
		requete.append("CREATE TABLE " + nomTableImage + FormatSQL.WITH_NO_VACUUM + " AS");
		requete.append("\n SELECT * FROM " + ViewEnum.MOD_VARIABLE_METIER.getFullName(environnement));
		requete.append("\n WHERE id_famille = " + requete.quoteText(famille));
		requete.append(";");
		UtilitaireDao.get(0).executeRequest(connection, requete);

		registerTableToBeRetrieved(ExportTrackingType.DATA, ArcDatabase.COORDINATOR, nomTableImage);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see fr.insee.arc_essnet.ws.dao.ClientDarcleMetier(java.lang.String,
	 * fr.insee.arc_essnet.ws.actions.Senarc
	 */
	public void createTableMetier() throws ArcException {
		LoggerHelper.debugAsComment(LOGGER, "ClientDaoImpl.sendTableMetier()");
	
		String nomTableImage = TableNaming.buildTableNameWithTokens(environnement, ViewEnum.MOD_TABLE_METIER, client,
				timestamp);
	
		ArcPreparedStatementBuilder requete = new ArcPreparedStatementBuilder(
				"\n CREATE TABLE " + nomTableImage + FormatSQL.WITH_NO_VACUUM + " AS");
		requete.append("\n SELECT * FROM " + ViewEnum.MOD_TABLE_METIER.getFullName(environnement) + " ");
		requete.append("\n WHERE id_famille = " + requete.quoteText(famille));
		requete.append(";");
		UtilitaireDao.get(0).executeRequest(connection, requete);
	
		registerTableToBeRetrieved(ExportTrackingType.DATA, ArcDatabase.COORDINATOR, nomTableImage);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see fr.insee.arc_essnet.ws.dao.ClientDarcablesFamilles(long,
	 * java.lang.String)
	 */
	public void createTableFamille() throws ArcException {
		LoggerHelper.debugAsComment(LOGGER, "ClientDaoImpl.createTableFamille()");

		String nomTableImage = TableNaming.buildTableNameWithTokens(environnement, ViewEnum.EXT_MOD_FAMILLE, client,
				timestamp);

		ArcPreparedStatementBuilder requete = new ArcPreparedStatementBuilder();
		requete.append("CREATE TABLE " + nomTableImage + FormatSQL.WITH_NO_VACUUM
				+ " AS SELECT DISTINCT f.id_famille FROM arc.ihm_famille f INNER JOIN  "
				+ "arc.ihm_client c ON f.id_famille = c.id_famille WHERE lower(c.id_application) = lower("
				+ requete.quoteText(client) + ");");
		UtilitaireDao.get(0).executeRequest(connection, requete);

		registerTableToBeRetrieved(ExportTrackingType.DATA, ArcDatabase.COORDINATOR, nomTableImage);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see fr.insee.arc_essnet.ws.dao.ClientDarcablesFamilles(long,
	 * java.lang.String)
	 */
	public void createTablePeriodicite() throws ArcException {
		LoggerHelper.debugAsComment(LOGGER, "ClientDaoImpl.createTablePeriodicite()");

		String nomTableImage = ViewEnum.getFullNameNotNormalized(environnement,
				client + "_" + timestamp + "_" + ViewEnum.EXT_MOD_PERIODICITE.getTableName());

		UtilitaireDao.get(0).executeImmediate(connection, "CREATE TABLE " + nomTableImage + FormatSQL.WITH_NO_VACUUM
				+ " AS SELECT DISTINCT id, val FROM " + ViewEnum.EXT_MOD_PERIODICITE.getFullName() + ";");

		registerTableToBeRetrieved(ExportTrackingType.DATA, ArcDatabase.COORDINATOR, nomTableImage);

	}

	/**
	 * Get the table object of the table to retrieve by its type
	 * 
	 * @param tableName
	 * @return
	 * @throws ArcException
	 */
	public TableToRetrieve getAClientTableByType(ExportTrackingType type) throws ArcException {

		// return data table found in track table for the given type

		ArcPreparedStatementBuilder query = new ArcPreparedStatementBuilder();
		query.build(SQL.SELECT, "nod, table_to_retrieve", SQL.FROM, this.tableWsTracking);
		query.build(SQL.WHERE, "tracking_type=", query.quoteText(type.toString()));
		query.build(SQL.LIMIT, "1");

		Map<String, List<String>> content = new GenericBean(UtilitaireDao.get(0).executeRequest(connection, query))
				.mapContent();

		return content.isEmpty() ? new TableToRetrieve()
				: new TableToRetrieve(content.get("nod").get(0), content.get("table_to_retrieve").get(0));

	}

	/**
	 * Get the table object of the table to retrieve by its name
	 * 
	 * @param tableName
	 * @return
	 * @throws ArcException
	 */
	public TableToRetrieve getAClientTableByName(String tableName) throws ArcException {

		ArcPreparedStatementBuilder query = new ArcPreparedStatementBuilder();
		query.build(SQL.SELECT, "nod, table_to_retrieve", SQL.FROM, this.tableWsTracking);
		query.build(SQL.WHERE, "table_to_retrieve=", query.quoteText(tableName));
		query.build(SQL.LIMIT, "1");

		Map<String, List<String>> content = new GenericBean(UtilitaireDao.get(0).executeRequest(connection, query))
				.mapContent();

		return content.isEmpty() ? new TableToRetrieve()
				: new TableToRetrieve(content.get("nod").get(0), content.get("table_to_retrieve").get(0));
	}


	private void dropTable(int connectionIndex, String clientTable) {
		UtilitaireDao.get(connectionIndex).dropTable(connection, clientTable);
	}

	public void dropTable(TableToRetrieve table) {

		dropTable(ArcDatabase.COORDINATOR.getIndex(), table.getTableName());
		
		int numberOfExecutorNods = ArcDatabase.numberOfExecutorNods();

		if (table.getNod().equals(ArcDatabase.EXECUTOR)) {
			for (int executorConnectionId = ArcDatabase.EXECUTOR.getIndex(); executorConnectionId < ArcDatabase.EXECUTOR
					.getIndex() + numberOfExecutorNods; executorConnectionId++) {
				dropTable(executorConnectionId, table.getTableName());
			}
		}

	}

	/**
	 * drop table from the client if some already exists
	 * 
	 * @throws ArcException
	 */
	public void dropPendingClientTables(int connectionId) throws ArcException {

		String findClientTable = ViewEnum.normalizeTableName(client + "\\_%");

		ArcPreparedStatementBuilder requete = new ArcPreparedStatementBuilder();
		requete.append("SELECT schemaname||'.'||tablename as " + ColumnEnum.TABLE_NAME + " FROM pg_tables");
		requete.append(" WHERE tablename like " + requete.quoteText(findClientTable));
		requete.append(" AND schemaname = " + requete.quoteText(this.environnement));

		List<String> tablesToDrop = new GenericBean(UtilitaireDao.get(connectionId).executeRequest(connection, requete))
				.getColumnValues(ColumnEnum.TABLE_NAME.getColumnName());

		UtilitaireDao.get(connectionId).executeImmediate(null,
				FormatSQL.dropTable(tablesToDrop.toArray(new String[0])));
	}

	/**
	 * create reporting table
	 * 
	 * @throws ArcException
	 */
	public void createTableWsInfo() throws ArcException {

		String tableWsInfo = TableNaming.buildTableNameWithTokens(environnement, ViewEnum.WS_INFO, client, timestamp);

		ArcPreparedStatementBuilder requete = new ArcPreparedStatementBuilder();
		requete.append("\n DROP TABLE IF EXISTS " + tableWsInfo + ";");

		requete.append("\n CREATE TABLE " + tableWsInfo + FormatSQL.WITH_NO_VACUUM + " AS");
		requete.append("\n SELECT " + requete.quoteText(client) + " as client ");
		requete.append(", " + requete.quoteText(Long.toString(timestamp)) + " as timestamp ");
		requete.append(";");

		requete.append("\n DROP TABLE IF EXISTS " + tableWsPending + ";");
		requete.append("\n CREATE TABLE " + tableWsPending + "();");

		UtilitaireDao.get(0).executeImmediate(connection, requete);
		registerTableToBeRetrieved(ExportTrackingType.DATA, ArcDatabase.COORDINATOR, tableWsInfo);

	}

	public void createTableWsKO() throws ArcException {
		registerTableToBeRetrieved(ExportTrackingType.KO, ArcDatabase.COORDINATOR, ViewEnum.WS_KO.toString());
	}

	public void dropTableWsPending() throws ArcException {
		ArcPreparedStatementBuilder requete = new ArcPreparedStatementBuilder();
		requete.append("DROP TABLE IF EXISTS " + tableWsPending + ";");
		UtilitaireDao.get(0).executeImmediate(connection, requete);
	}

	/**
	 * web service data creation is not pending if tableWsPending doesn't exists
	 * anymore
	 * 
	 * @return
	 * @throws ArcException
	 */
	public boolean isWebServiceNotPending() throws ArcException {

		ArcPreparedStatementBuilder requete = new ArcPreparedStatementBuilder();

		requete.append("SELECT 1 FROM pg_tables WHERE schemaname||'.'||tablename = " + requete.quoteText(tableWsPending)
				+ " ");

		return !UtilitaireDao.get(0).hasResults(connection, requete);
	}

	public void copyTableOfIdSourceToExecutorNod(int connectionId) throws ArcException {
		GenericBean gb = new GenericBean(UtilitaireDao.get(0).executeRequest(connection,
				new ArcPreparedStatementBuilder("SELECT * FROM " + tableOfIdSource)));

		try (Connection executorConnection = UtilitaireDao.get(connectionId).getDriverConnexion()) {
			CopyObjectsToDatabase.execCopyFromGenericBean(executorConnection, tableOfIdSource, gb);
		} catch (SQLException e) {
			ArcException customException = new ArcException(e, ArcExceptionMessage.DATABASE_CONNECTION_EXECUTOR_FAILED);
			customException.logFullException();
			throw customException;
		}
	}

	public void deleteFromTrackTable(String tableName) throws ArcException {
		ArcPreparedStatementBuilder query = new ArcPreparedStatementBuilder();
		query.build(SQL.DELETE, this.tableWsTracking);
		query.build(SQL.WHERE, "table_to_retrieve=", query.quoteText(tableName));
		UtilitaireDao.get(0).executeImmediate(connection, query);
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getEnvironnement() {
		return environnement;
	}

	public String getClient() {
		return client;
	}

	public String getFamille() {
		return famille;
	}

	public String getTableOfIdSource() {
		return tableOfIdSource;
	}

	public String getTableWsPending() {
		return tableWsPending;
	}

	public String getTableWsTracking() {
		return tableWsTracking;
	}

	public Connection getConnection() {
		return connection;
	}
	
	

}
