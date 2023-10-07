package fr.insee.arc.ws.services.importServlet.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import fr.insee.arc.core.dataobjects.ArcPreparedStatementBuilder;
import fr.insee.arc.core.dataobjects.ColumnEnum;
import fr.insee.arc.core.dataobjects.ViewEnum;
import fr.insee.arc.core.model.TraitementEtat;
import fr.insee.arc.core.model.TraitementPhase;
import fr.insee.arc.core.service.global.util.Patch;
import fr.insee.arc.utils.dao.UtilitaireDao;
import fr.insee.arc.utils.exception.ArcException;
import fr.insee.arc.utils.exception.ArcExceptionMessage;
import fr.insee.arc.utils.format.Format;
import fr.insee.arc.utils.utils.FormatSQL;
import fr.insee.arc.utils.utils.LoggerHelper;
import fr.insee.arc.utils.utils.ManipString;
import fr.insee.arc.ws.services.importServlet.actions.SendResponse;
import fr.insee.arc.ws.services.importServlet.bo.JsonKeys;

public class ClientDaoImpl implements ClientDao {

	protected static final Logger LOGGER = LogManager.getLogger(ClientDaoImpl.class);

	private static final char CHAR_SEPARATOR = (char) 1;
	private static final String FIELD_SEPARATOR = Character.toString(CHAR_SEPARATOR);

	/*
	 * (non-Javadoc)
	 *
	 * @see fr.insee.arc.ws.dao.ClientDao#verificationClientFamille(long,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public void verificationClientFamille(long timestamp, String client, String idFamille, String environnement)
			throws ArcException {
		LoggerHelper.debugAsComment(LOGGER, timestamp, "ClientDaoImpl#verificationClientFamille()");
		Connection connection = null;

		ArcPreparedStatementBuilder request = new ArcPreparedStatementBuilder();
		request.append("SELECT EXISTS (SELECT 1 FROM arc.ihm_client")
				.append(" WHERE id_application=" + request.quoteText(client))
				.append(" AND id_famille=" + request.quoteText(idFamille)).append(" LIMIT 1);");

		try {
			long beginning1 = System.currentTimeMillis();
			connection = UtilitaireDao.get(0).getDriverConnexion();
			long time1 = System.currentTimeMillis() - beginning1;
			LoggerHelper.debugAsComment(LOGGER, timestamp,
					"ClientDaoImpl#verificationClientFamille() : Connection Done -", time1, "ms");
			long beginning2 = System.currentTimeMillis();
			String bool = UtilitaireDao.get(0).executeRequestWithoutMetadata(connection, request).get(0).get(0);
			long time2 = System.currentTimeMillis() - beginning2;
			LoggerHelper.debugAsComment(LOGGER, timestamp,
					": ClientDaoImpl.verificationClientFamille() : ExecuteQuery Done -", time2, "ms");

			if (!bool.equals("t")) {
				throw new ArcException(ArcExceptionMessage.WS_RETRIEVE_DATA_FAMILY_FORBIDDEN);
			}
		} finally {
			close(connection);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see fr.insee.ararcs.dao.ClientDao#getIdSrcTableMetier(long,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public List<List<String>> getIdSrcTableMetier(long timestamp, String client, boolean reprise, String environnement,
			String idFamille, String validiteInf, String validiteSup, String periodicite) throws ArcException {
		//
		LoggerHelper.debugAsComment(LOGGER, timestamp, "ClientDaoImpl#getIdSrcTableMetier()");
		Connection connection = null;
		List<List<String>> tablesMetierNames = new ArrayList<>();

		String tableOfIdSource = ViewEnum.getFullName(environnement,
				client + "_" + timestamp + "_" + ColumnEnum.ID_SOURCE.getColumnName());

		StringBuilder request = new StringBuilder("DROP TABLE IF EXISTS " + tableOfIdSource + "; ");
		request.append("CREATE TABLE " + tableOfIdSource + " ");
		request.append("AS SELECT " + ColumnEnum.ID_SOURCE.getColumnName() + " FROM "
				+ ViewEnum.PILOTAGE_FICHIER.getFullName(environnement) + " T1 ");
		request.append(
				"WHERE '" + TraitementEtat.OK + "'=ANY(T1.etat_traitement) AND T1.periodicite='" + periodicite + "' ");

		if (!StringUtils.isEmpty(validiteInf)) {
			request.append("AND validite>='" + validiteInf + "' ");
		}
		request.append(
				"AND validite<='" + validiteSup + "' AND T1.phase_traitement='" + TraitementPhase.MAPPING + "' ");
		request.append("AND EXISTS (SELECT 1 FROM " + ViewEnum.NORME.getFullName(environnement)
				+ " T2 WHERE T2.id_famille='" + idFamille + "' AND T1.id_norme=T2.id_norme) ");
		if (!reprise) {
			LoggerHelper.debugAsComment(LOGGER, "ClientDaoImpl.getIdSrcTableMetier() : Reprise = false");
			request.append("AND NOT '" + client + "' = ANY(coalesce(T1.client, ARRAY[]::text[])) GROUP BY "
					+ ColumnEnum.ID_SOURCE.getColumnName() + "");
		} else {
			LoggerHelper.debugAsComment(LOGGER, "ClientDaoImpl.getIdSrcTableMetier() : Reprise = true");
			request.append("GROUP BY " + ColumnEnum.ID_SOURCE.getColumnName() + "");
		}
		request.append("; ");

		String tableOfModTableMetier = ViewEnum.getFullName(environnement,
				client + "_" + timestamp + "_" + ViewEnum.MOD_TABLE_METIER.getTableName());

		request.append("DROP TABLE IF EXISTS " + tableOfModTableMetier + "; ");

		request.append("CREATE TABLE " + tableOfModTableMetier + " ");
		request.append(
				"AS SELECT nom_table_metier FROM " + ViewEnum.MOD_TABLE_METIER.getFullName(environnement) + " T1 ");
		request.append("WHERE T1.id_famille='" + idFamille + "' ");
		request.append("AND exists (select 1 from pg_tables T2 where ");
		request.append("T2.schemaname='" + environnement + "' ");
		request.append("AND T1.nom_table_metier=T2.tablename);");

		try {
			connection = UtilitaireDao.get(0).getDriverConnexion();
			UtilitaireDao.get(0).executeBlock(connection, request);
			tablesMetierNames = UtilitaireDao.get(0).executeRequestWithoutMetadata(connection,
					new ArcPreparedStatementBuilder("SELECT nom_table_metier FROM " + tableOfModTableMetier + ";"));
			UtilitaireDao.get(0).executeImmediate(connection, "DROP TABLE " + tableOfModTableMetier + ";");
		} finally {
			close(connection);
		}
		return tablesMetierNames;
	}

	@Override
	public List<List<String>> getIdSrcTableMetier(long timestamp, JSONObject requeteJSON) throws ArcException {

		LoggerHelper.debugAsComment(LOGGER, timestamp, "ClientDaoImpl#getIdSrcTableMetier()");

		// Initialisation des variables
		Connection connection = null;
		List<List<String>> tablesMetierNames = new ArrayList<>();
		final String env = Patch.normalizeSchemaName(requeteJSON.getString(JsonKeys.ENVIRONNEMENT.getKey()));
		final String client = requeteJSON.getString(JsonKeys.CLIENT.getKey());
		final String periodicite = requeteJSON.getString(JsonKeys.PERIODICITE.getKey());
		final String validiteSup = requeteJSON.getString(JsonKeys.VALSUP.getKey());
		final boolean reprise = requeteJSON.getBoolean(JsonKeys.REPRISE.getKey());
		final String idFamille = requeteJSON.getString(JsonKeys.FAMILLE.getKey());

		// Nombre de fichiers à recupérer (optionnel pour l'instant)
		int nbFichiers;

		try {
			nbFichiers = requeteJSON.getInt(JsonKeys.NBFICHIERS.getKey());
		} catch (Exception e) {
			LoggerHelper.debugAsComment(LOGGER, "Le nombre de fichiers à récupérer n'a pas été renseigné " + e);
			// par défaut, on considère que tous les fichiers sont à charger.
			nbFichiers = 0;
		}

		/**************************************************************************************************************************/
		/************************
		 * CREATION DE LA TABLE TEMPORAIRE CONTENANT LES idsources A CHARGER
		 ********************************/
		/**************************************************************************************************************************/

		String tableOfIdSource = ViewEnum.getFullName(env,
				client + "_" + timestamp + "_" + ColumnEnum.ID_SOURCE.getColumnName());

		// Préparation du block de requêtes à executer
		StringBuilder request = new StringBuilder("DROP TABLE IF EXISTS " + tableOfIdSource + "; ");

		// Création de la requête de création de la table temporaire contenant la liste
		// des id_sources
		StringBuilder query = new StringBuilder();

		// Cas 1 : Avec limitation du nombre de fichiers à récupérer
		if (nbFichiers > 0) {

			query.append("CREATE TABLE " + tableOfIdSource + " ");
			query.append("AS SELECT " + ColumnEnum.ID_SOURCE.getColumnName() + " FROM (");
			query.append("SELECT " + ColumnEnum.ID_SOURCE.getColumnName()
					+ ", substr(date_entree,1,10)::date as date_entree FROM "
					+ ViewEnum.PILOTAGE_FICHIER.getFullName(env) + " T1 ");
			query.append("WHERE '" + TraitementEtat.OK + "'=ANY(T1.etat_traitement) AND T1.periodicite='" + periodicite
					+ "' ");

			if (requeteJSON.keySet().contains(JsonKeys.VALINF.getKey())) {
				query.append("AND validite>='" + requeteJSON.getString(JsonKeys.VALINF.getKey()) + "' ");
			}

			query.append(
					"AND validite<='" + validiteSup + "' AND T1.phase_traitement='" + TraitementPhase.MAPPING + "' ");
			query.append("AND EXISTS (SELECT 1 FROM " + ViewEnum.NORME.getFullName(env) + " T2 WHERE T2.id_famille='"
					+ idFamille + "' AND T1.id_norme=T2.id_norme) ");

			if (!reprise) {
				LoggerHelper.debugAsComment(LOGGER, "ClientDaoImpl.getIdSrcTableMetier() : Reprise = false");
				query.append("AND NOT '" + client + "' = ANY(coalesce(T1.client, ARRAY[]::text[])) GROUP BY "
						+ ColumnEnum.ID_SOURCE.getColumnName() + ", date_entree");
			} else {
				LoggerHelper.debugAsComment(LOGGER, "ClientDaoImpl.getIdSrcTableMetier() : Reprise = true");
				query.append("GROUP BY " + ColumnEnum.ID_SOURCE.getColumnName() + ", date_entree ");
			}

			// on trie par ordre decroissant de date d'entree
			query.append("ORDER BY date_entree DESC LIMIT ");
			query.append(nbFichiers);
			query.append(") as foo; ");
		}

		// Cas 2 : Sans limitation du nombre de fichiers à récupérer
		else {
			query.append("CREATE TABLE " + tableOfIdSource + " ");
			query.append("AS SELECT " + ColumnEnum.ID_SOURCE.getColumnName() + " FROM "
					+ ViewEnum.PILOTAGE_FICHIER.getFullName(env) + " T1 ");
			query.append("WHERE '" + TraitementEtat.OK + "'=ANY(T1.etat_traitement) AND T1.periodicite='" + periodicite
					+ "' ");

			if (requeteJSON.keySet().contains(JsonKeys.VALINF.getKey())) {
				query.append("AND validite>='" + requeteJSON.getString(JsonKeys.VALINF.getKey()) + "' ");
			}

			query.append(
					"AND validite<='" + validiteSup + "' AND T1.phase_traitement='" + TraitementPhase.MAPPING + "' ");
			query.append("AND EXISTS (SELECT 1 FROM " + ViewEnum.NORME.getFullName(env) + " T2 WHERE T2.id_famille='"
					+ idFamille + "' AND T1.id_norme=T2.id_norme) ");

			if (!reprise) {
				LoggerHelper.debugAsComment(LOGGER, "ClientDaoImpl.getIdSrcTableMetier() : Reprise = false");
				query.append("AND NOT '" + client + "' = ANY(coalesce(T1.client, ARRAY[]::text[])) GROUP BY "
						+ ColumnEnum.ID_SOURCE.getColumnName() + "");
			} else {
				LoggerHelper.debugAsComment(LOGGER, "ClientDaoImpl.getIdSrcTableMetier() : Reprise = true");
				query.append("GROUP BY " + ColumnEnum.ID_SOURCE.getColumnName() + " ");
			}

			query.append("; ");
		}

		request.append(query);

		/**************************************************************************************************************************/
		/*************
		 * CREATION DE LA TABLE TEMPORAIRE CONTENANT LES NOMS DE TABLES METIERS
		 * CONCERNES POUR LE DECHARGEMENT
		 *********/
		/**************************************************************************************************************************/

		String tableModTableMetier = ViewEnum.getFullName(env,
				client + "_" + timestamp + "_" + ViewEnum.MOD_TABLE_METIER.getTableName());

		request.append("DROP TABLE IF EXISTS " + tableModTableMetier + "; ");

		request.append("CREATE TABLE " + tableModTableMetier + " ");
		request.append("AS SELECT nom_table_metier FROM " + ViewEnum.MOD_TABLE_METIER.getFullName(env) + " T1 ");
		request.append("WHERE T1.id_famille='" + idFamille + "' ");
		request.append("AND exists (select 1 from pg_tables T2 where ");
		request.append("T2.schemaname='" + ManipString.substringBeforeFirst(env, ".") + "' ");
		request.append("AND '" + ManipString.substringAfterFirst(env, ".") + "'||T1.nom_table_metier=T2.tablename);");

		try {
			connection = UtilitaireDao.get(0).getDriverConnexion();
			UtilitaireDao.get(0).executeBlock(connection, request);
			tablesMetierNames = UtilitaireDao.get(0).executeRequestWithoutMetadata(connection,
					new ArcPreparedStatementBuilder("SELECT nom_table_metier FROM " + tableModTableMetier + ";"));
			UtilitaireDao.get(0).executeImmediate(connection, "DROP TABLE IF EXISTS " + tableModTableMetier + ";");
		} finally {
			close(connection);
		}

		return tablesMetierNames;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see fr.insee.arc_essnet.ws.dao.ClientDarcmage(long, java.lang.String,
	 * java.lang.String, java.util.ArrayList)
	 */
	@Override
	public List<String> createImages(long timestamp, String client, String environnement,
			List<List<String>> tablesMetierNames) throws ArcException {
		LoggerHelper.debugAsComment(LOGGER, timestamp, "ClientDaoImpl.createImage()");

		List<String> mesTablesImagesCrees = new ArrayList<>();

		for (List<String> tableMetier : tablesMetierNames) {

			addImage(timestamp, client, environnement, tableMetier, mesTablesImagesCrees);

		}

		return mesTablesImagesCrees;
	}

	@Override
	public void addImage(long timestamp, String client, String environnement, List<String> tableMetier,
			List<String> mesTablesImagesCrees) throws ArcException {
		Connection connection = null;
		StringBuilder request = new StringBuilder();

		String nomTableImage = ViewEnum.getFullName(environnement, client + "_" + timestamp + "_" + tableMetier.get(0));
		String tableOfIdSource = ViewEnum.getFullName(environnement,
				client + "_" + timestamp + "_" + ColumnEnum.ID_SOURCE.getColumnName());

		request.append("DROP TABLE IF EXISTS " + nomTableImage + "; ");

		request.append("CREATE TABLE " + nomTableImage + FormatSQL.WITH_NO_VACUUM + " AS ");
		request.append("SELECT * ");
		request.append("FROM " + ViewEnum.getFullName(environnement, tableMetier.get(0)) + " T1 WHERE true ");
		request.append("AND exists (SELECT 1 FROM " + tableOfIdSource + " T2 where T2."
				+ ColumnEnum.ID_SOURCE.getColumnName() + "=T1." + ColumnEnum.ID_SOURCE.getColumnName() + "); ");

		mesTablesImagesCrees.add(nomTableImage);

		try {
			connection = UtilitaireDao.get(0).getDriverConnexion();
			UtilitaireDao.get(0).executeBlock(connection, request);
		} finally {
			close(connection);
		}

	}

	/**
	 *
	 *
	 * @see fr.insee.arc.ws.services.importServlet.dao.ClientDao#getResponse(long,
	 *      java.lang.String, java.lang.String,
	 *      fr.insee.arc.ws.services.importServlet.actions.SendResponse)
	 */
	@Override
	public void getResponse(long timestamp, String client, String tableMetierName, String environnement,
			SendResponse resp) throws ArcException {
		LoggerHelper.debugAsComment(LOGGER, timestamp, ": ClientDaoImpl.getResponse()");
		Connection connection = null;
		List<List<String>> result = new ArrayList<>();

		int nbLines = 0;
		int blockSize = 10000;
		int nbBlock = 0;

		try {
			connection = UtilitaireDao.get(0).getDriverConnexion();

			String tableMetier = ViewEnum.getFullName(environnement, client + "_" + timestamp + "_" + tableMetierName);

			nbLines = UtilitaireDao.get(0).getInt(connection,
					new ArcPreparedStatementBuilder("select max(rowid) FROM " + tableMetier));
			nbBlock = (nbLines - 1) / blockSize + 1;

			for (int i = 0; i < nbBlock; i++) {
				LoggerHelper.debugAsComment(LOGGER, "Traitement du bloc ", i, "/", (nbBlock - 1));
				result = UtilitaireDao.get(0).executeRequest(connection,
						new ArcPreparedStatementBuilder("SELECT * FROM " + tableMetier + " u where rowid>"
								+ i * blockSize + " and rowid<=" + blockSize * (i + 1)));
				resp.send("{\"" + JsonKeys.ID.getKey() + "\":\"" + tableMetierName + "\",\"" + JsonKeys.TABLE.getKey()
						+ "\":");
				mapJsonResponse(result, resp);
				resp.send("}");
			}
		} finally {
			close(connection);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 */
	public void updatePilotage(long timestamp, String environnement, String tableSource) throws ArcException {
		LoggerHelper.debugAsComment(LOGGER, timestamp, ": ClientDaoImpl.updatePilotage()");
		Connection connection = null;

		String client = ManipString.substringBeforeFirst(ManipString.substringAfterFirst(tableSource, "."), "_")
				.toUpperCase();

		StringBuilder columnClient = new StringBuilder();
		columnClient.append("UPDATE " + ViewEnum.PILOTAGE_FICHIER.getFullName(environnement) + " T1 ");
		columnClient.append("SET client = array_append(client, '" + client + "') ");
		columnClient.append(", date_client = array_append( date_client, localtimestamp ) ");
		columnClient.append("WHERE true ");
		columnClient.append("AND EXISTS (SELECT 1 FROM " + tableSource + " T2 where T1."
				+ ColumnEnum.ID_SOURCE.getColumnName() + "=T2." + ColumnEnum.ID_SOURCE.getColumnName() + ") ");
		columnClient.append("AND T1.phase_traitement='" + TraitementPhase.MAPPING + "';");

		try {
			connection = UtilitaireDao.get(0).getDriverConnexion();
			UtilitaireDao.get(0).executeBlock(connection, columnClient.toString());
		} finally {
			close(connection);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * fr.insee.arc_essnet.ws.dao.ClientDarcl(fr.insee.arc_essnet.ws.actions.Senarc
	 */
	public void createNmcl(long timestamp, String client, String environnement) throws ArcException {
		LoggerHelper.debugAsComment(LOGGER, "ClientDaoImpl.createNmcl()");
		Connection connection = null;
		List<List<String>> nmclNames = new ArrayList<>();

		try {
			connection = UtilitaireDao.get(0).getDriverConnexion();

			ArcPreparedStatementBuilder requete = new ArcPreparedStatementBuilder();
			requete.append("SELECT tablename FROM pg_tables ")
					.append(" WHERE schemaname = " + requete.quoteText(environnement))
					.append(" AND tablename LIKE " + requete.quoteText("nmcl_%"));

			nmclNames = UtilitaireDao.get(0).executeRequestWithoutMetadata(connection, requete);

			for (List<String> nmcl : nmclNames) {
				String nomTableImage = ViewEnum.getFullName(environnement,
						client + "_" + timestamp + "_" + nmcl.get(0));
				UtilitaireDao.get(0).executeImmediate(connection,
						"CREATE TABLE " + nomTableImage + FormatSQL.WITH_NO_VACUUM + " AS SELECT * FROM "
								+ ViewEnum.getFullName(environnement, nmcl.get(0)) + ";");
			}
		} finally {
			close(connection);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see fr.insee.arc_essnet.ws.dao.ClientDarcMetier(java.lang.String,
	 * fr.insee.arc_essnet.ws.actions.Senarc
	 */
	public void createVarMetier(long timestamp, String client, String idFamille, String environnement)
			throws ArcException {
		LoggerHelper.debugAsComment(LOGGER, "ClientDaoImpl.createVarMetier()");
		Connection connection = null;
		try {
			connection = UtilitaireDao.get(0).getDriverConnexion();

			String nomTableImage = ViewEnum.getFullName(environnement,
					client + "_" + timestamp + "_" + ViewEnum.MOD_VARIABLE_METIER.getTableName());

			ArcPreparedStatementBuilder requete = new ArcPreparedStatementBuilder();
			requete.append("CREATE TABLE " + nomTableImage + FormatSQL.WITH_NO_VACUUM + " AS");
			requete.append("\n SELECT * FROM " + ViewEnum.MOD_VARIABLE_METIER.getFullName(environnement));
			requete.append("\n WHERE lower(id_famille) = lower(" + requete.quoteText(idFamille) + ")");
			requete.append(";");
			UtilitaireDao.get(0).executeRequest(connection, requete);
		} finally {
			close(connection);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see fr.insee.arc_essnet.ws.dao.ClientDarcablesFamilles(long,
	 * java.lang.String)
	 */
	@Override
	public void createTableFamille(long timestamp, String client, String environnement) throws ArcException {
		LoggerHelper.debugAsComment(LOGGER, "ClientDaoImpl.createTableFamille()");
		Connection connection = null;
		try {
			connection = UtilitaireDao.get(0).getDriverConnexion();

			String nomTableImage = ViewEnum.getFullName(environnement,
					client + "_" + timestamp + "_" + "ext_mod_famille");

			ArcPreparedStatementBuilder requete = new ArcPreparedStatementBuilder();
			requete.append("CREATE TABLE " + nomTableImage + FormatSQL.WITH_NO_VACUUM
					+ " AS SELECT DISTINCT f.id_famille FROM arc.ihm_famille f INNER JOIN  "
					+ "arc.ihm_client c ON f.id_famille = c.id_famille WHERE lower(c.id_application) = lower("
					+ requete.quoteText(client) + ");");
			UtilitaireDao.get(0).executeRequest(connection, requete);
		} finally {
			close(connection);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see fr.insee.arc_essnet.ws.dao.ClientDarcablesFamilles(long,
	 * java.lang.String)
	 */
	@Override
	public void createTablePeriodicite(long timestamp, String client, String environnement) throws ArcException {
		LoggerHelper.debugAsComment(LOGGER, "ClientDaoImpl.createTablePeriodicite()");
		Connection connection = null;
		try {
			connection = UtilitaireDao.get(0).getDriverConnexion();
			String nomTableImage = ViewEnum.getFullName(environnement,
					client + "_" + timestamp + "_" + ViewEnum.EXT_MOD_PERIODICITE.getTableName());

			UtilitaireDao.get(0).executeImmediate(connection, "CREATE TABLE " + nomTableImage + FormatSQL.WITH_NO_VACUUM
					+ " AS SELECT DISTINCT id, val FROM " + ViewEnum.EXT_MOD_PERIODICITE.getFullName() + ";");
		} finally {
			close(connection);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see fr.insee.arc_essnet.ws.dao.ClientDarcleMetier(java.lang.String,
	 * fr.insee.arc_essnet.ws.actions.Senarc
	 */
	public void createTableMetier(long timestamp, String client, String idFamille, String environnement)
			throws ArcException {
		LoggerHelper.debugAsComment(LOGGER, "ClientDaoImpl.sendTableMetier()");
		Connection connection = null;
		try {
			connection = UtilitaireDao.get(0).getDriverConnexion();

			String nomTableImage = ViewEnum.getFullName(environnement,
					client + "_" + timestamp + "_" + ViewEnum.MOD_TABLE_METIER.getTableName());

			ArcPreparedStatementBuilder requete = new ArcPreparedStatementBuilder(
					"\n CREATE TABLE " + nomTableImage + FormatSQL.WITH_NO_VACUUM + " AS");
			requete.append("\n SELECT * FROM " + ViewEnum.MOD_TABLE_METIER.getFullName(environnement) + "mod_table_metier");
			requete.append("\n WHERE lower(id_famille) = lower(" + requete.quoteText(idFamille) + ")");
			requete.append(";");
			UtilitaireDao.get(0).executeRequest(connection, requete);
		} finally {
			close(connection);
		}
	}

	/**
	 * Met en une table reçue par la base données en JSON.
	 *
	 * @param result Resultat reçu de la base de données.
	 * @param resp   Flux où écrire la réponse une fois mise en forme.
	 */
	private void mapJsonResponse(List<List<String>> result, SendResponse resp) {
		List<String> table = new ArrayList<>();
		StringBuilder row = new StringBuilder("\"");
		String cell;

		for (int i = 0; i < result.size(); i++) {
			for (int j = 0; j < result.get(i).size(); j++) {
				cell = result.get(i).get(j);
				row.append(cell + FIELD_SEPARATOR);
			}
			table.add(row.append("\"").toString());
			row.delete(0, row.length());
			row.append("\"");
		}
		resp.send("[" + Format.untokenize(table, ",") + "]");
	}

	/**
	 * Ferme la connexion placée en paramètre
	 *
	 * @param connection Connexion à fermer.
	 */
	private static void close(Connection connection) throws ArcException {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				throw new ArcException(e, ArcExceptionMessage.DATABASE_CONNECTION_FAILED);
			}
		}
	}

	/**
	 * 
	 * @param client
	 * @param isSourceListTable : is it the table containing the list of id_source
	 *                          of the files to be marked ?
	 * @return
	 * @throws ArcException
	 */
	public String getAClientTable(String client, boolean isSourceListTable) throws ArcException {
		String clientLc = client.toLowerCase();
		String schema = ManipString.substringBeforeFirst(clientLc, ".");
		String clientDb = ManipString.substringAfterFirst(clientLc, ".").replace("_", "\\_") + "%";

		String realClient = ManipString.substringBeforeFirst(ManipString.substringAfterFirst(client, "."), "_");

		ArcPreparedStatementBuilder requete = new ArcPreparedStatementBuilder();
		requete.append("SELECT schemaname||'.'||tablename FROM pg_tables")
				.append(" WHERE tablename like " + requete.quoteText(clientDb))
				.append(" AND schemaname=" + requete.quoteText(schema)).append(" AND tablename "
						+ (isSourceListTable ? "" : "NOT") + " like " + requete.quoteText("%id\\_source%"));

		String r = UtilitaireDao.get(0).getString(null, requete);
		if (r != null) {
			r = r.replace(realClient.toLowerCase(), realClient);
		}
		return r;

	}

	public String getAClientTable(String client) throws ArcException {
		return getAClientTable(client, false);
	}

	public String getIdTable(String client) throws ArcException {
		return getAClientTable(client, true);
	}

	public void dropTable(String clientTable) throws ArcException {
		if (StringUtils.isBlank(clientTable)) {
			return;
		}
		UtilitaireDao.get(0).dropTable(null, clientTable);
	}
	
	
	private String buildTableMetier(String environnement, String client, String timestamp, ViewEnum view) 
	{
		return ViewEnum.getFullName(environnement,
				client + "_" + timestamp + "_" + view.getTableName());
	}

}
