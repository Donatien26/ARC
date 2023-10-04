package fr.insee.arc.core.dataobjects;

import java.util.LinkedHashMap;
import java.util.Map;

import fr.insee.arc.utils.dao.SQL;
import fr.insee.arc.utils.dataobjects.PgColumnEnum;
import fr.insee.arc.utils.dataobjects.PgViewEnum;

public enum ViewEnum {

	// tables de modalités
	  EXT_ETAT("ext_etat", SchemaEnum.ARC_METADATA, ColumnEnum.ID, ColumnEnum.VAL) //
	, EXT_ETAT_JEUDEREGLE("ext_etat_jeuderegle", SchemaEnum.ARC_METADATA, ColumnEnum.ID, ColumnEnum.VAL,
			ColumnEnum.ISENV, ColumnEnum.MISE_A_JOUR_IMMEDIATE, ColumnEnum.ENV_DESCRIPTION) //
	, EXT_EXPORT_FORMAT("ext_export_format", SchemaEnum.ARC_METADATA, ColumnEnum.ID, ColumnEnum.VAL) //
	, EXT_MOD_PERIODICITE("ext_mod_periodicite", SchemaEnum.ARC_METADATA, ColumnEnum.ID, ColumnEnum.VAL) //
	, EXT_MOD_TYPE_AUTORISE("ext_mod_type_autorise", SchemaEnum.ARC_METADATA, ColumnEnum.NOM_TYPE,
			ColumnEnum.DESCRIPTION_TYPE) //
	, EXT_TYPE_CONTROLE("ext_type_controle", SchemaEnum.ARC_METADATA, ColumnEnum.ID, ColumnEnum.ORDRE) //
	, EXT_TYPE_FICHIER_CHARGEMENT("ext_type_fichier_chargement", SchemaEnum.ARC_METADATA, ColumnEnum.ID, ColumnEnum.ORDRE) //
	, EXT_TYPE_NORMAGE("ext_type_normage", SchemaEnum.ARC_METADATA, ColumnEnum.ID, ColumnEnum.ORDRE) //
	, EXT_WEBSERVICE_QUERYVIEW("ext_webservice_queryview", SchemaEnum.ARC_METADATA, ColumnEnum.ID, ColumnEnum.VAL) //
	, EXT_WEBSERVICE_TYPE("ext_webservice_type", SchemaEnum.ARC_METADATA, ColumnEnum.ID, ColumnEnum.VAL) //

	// tables de règles
	, IHM_CALENDRIER("ihm_calendrier", SchemaEnum.ARC_METADATA, ColumnEnum.ID_NORME, ColumnEnum.PERIODICITE,
			ColumnEnum.VALIDITE_INF, ColumnEnum.VALIDITE_SUP, ColumnEnum.ETAT) //
	, IHM_CHARGEMENT_REGLE("ihm_chargement_regle", SchemaEnum.ARC_METADATA, ColumnEnum.ID_REGLE, ColumnEnum.ID_NORME,
			ColumnEnum.VALIDITE_INF, ColumnEnum.VALIDITE_SUP, ColumnEnum.VERSION, ColumnEnum.PERIODICITE,
			ColumnEnum.TYPE_FICHIER, ColumnEnum.DELIMITER, ColumnEnum.FORMAT, ColumnEnum.COMMENTAIRE) //
	, IHM_CLIENT("ihm_client", SchemaEnum.ARC_METADATA, ColumnEnum.ID_FAMILLE, ColumnEnum.ID_APPLICATION) //
	, IHM_CONTROLE_REGLE("ihm_controle_regle", SchemaEnum.ARC_METADATA, ColumnEnum.ID_REGLE_INT, ColumnEnum.ID_NORME,
			ColumnEnum.PERIODICITE, ColumnEnum.VALIDITE_INF, ColumnEnum.VALIDITE_SUP, ColumnEnum.VERSION,
			ColumnEnum.ID_CLASSE, ColumnEnum.RUBRIQUE_PERE, ColumnEnum.RUBRIQUE_FILS, ColumnEnum.BORNE_INF,
			ColumnEnum.BORNE_SUP, ColumnEnum.CONDITION, ColumnEnum.PRE_ACTION, ColumnEnum.TODO, ColumnEnum.COMMENTAIRE,
			ColumnEnum.XSD_ORDRE, ColumnEnum.XSD_LABEL_FILS, ColumnEnum.XSD_ROLE, ColumnEnum.BLOCKING_THRESHOLD,
			ColumnEnum.ERROR_ROW_PROCESSING) //
	, IHM_ENTREPOT("ihm_entrepot", SchemaEnum.ARC_METADATA, ColumnEnum.ID_ENTREPOT, ColumnEnum.ID_LOADER) //
	, IHM_EXPRESSION("ihm_expression", SchemaEnum.ARC_METADATA, ColumnEnum.ID_REGLE, ColumnEnum.ID_NORME,
			ColumnEnum.VALIDITE_INF, ColumnEnum.VALIDITE_SUP, ColumnEnum.VERSION, ColumnEnum.PERIODICITE,
			ColumnEnum.EXPR_NOM, ColumnEnum.EXPR_VALEUR, ColumnEnum.COMMENTAIRE) //
	, IHM_FAMILLE("ihm_famille", SchemaEnum.ARC_METADATA, ColumnEnum.ID_FAMILLE) //
	, IHM_JEUDEREGLE("ihm_jeuderegle", SchemaEnum.ARC_METADATA, ColumnEnum.ID_NORME, ColumnEnum.PERIODICITE,
			ColumnEnum.VALIDITE_INF, ColumnEnum.VALIDITE_SUP, ColumnEnum.VERSION, ColumnEnum.ETAT,
			ColumnEnum.DATE_PRODUCTION, ColumnEnum.DATE_INACTIF) //
	, IHM_MAPPING_REGLE("ihm_mapping_regle", SchemaEnum.ARC_METADATA, ColumnEnum.ID_REGLE, ColumnEnum.ID_NORME,
			ColumnEnum.VALIDITE_INF, ColumnEnum.VALIDITE_SUP, ColumnEnum.VERSION, ColumnEnum.PERIODICITE,
			ColumnEnum.VARIABLE_SORTIE, ColumnEnum.EXPR_REGLE_COL, ColumnEnum.COMMENTAIRE) //
	, IHM_MOD_TABLE_METIER("ihm_mod_table_metier", SchemaEnum.ARC_METADATA, ColumnEnum.ID_FAMILLE,
			ColumnEnum.NOM_TABLE_METIER, ColumnEnum.DESCRIPTION_TABLE_METIER) //
	, IHM_MOD_VARIABLE_METIER("ihm_mod_variable_metier", SchemaEnum.ARC_METADATA, ColumnEnum.ID_FAMILLE,
			ColumnEnum.NOM_TABLE_METIER, ColumnEnum.NOM_VARIABLE_METIER, ColumnEnum.TYPE_VARIABLE_METIER,
			ColumnEnum.DESCRIPTION_VARIABLE_METIER, ColumnEnum.TYPE_CONSOLIDATION) //
	
	// family model variables view in gui
	, VIEW_VARIABLE_METIER("mod_variable_metier", SchemaEnum.SANDBOX_GENERATED, ColumnEnum.NOM_TABLE_METIER, ColumnEnum.NOM_VARIABLE_METIER //
			, ColumnEnum.TYPE_VARIABLE_METIER, ColumnEnum.TYPE_CONSOLIDATION, ColumnEnum.DESCRIPTION_VARIABLE_METIER) //
	
	
	, IHM_NMCL("ihm_nmcl", SchemaEnum.ARC_METADATA, ColumnEnum.NOM_TABLE, ColumnEnum.DESCRIPTION) //
	, IHM_NORMAGE_REGLE("ihm_normage_regle", SchemaEnum.ARC_METADATA, ColumnEnum.ID_REGLE_INT, ColumnEnum.ID_NORME,
			ColumnEnum.PERIODICITE, ColumnEnum.VALIDITE_INF, ColumnEnum.VALIDITE_SUP, ColumnEnum.VERSION,
			ColumnEnum.ID_CLASSE, ColumnEnum.RUBRIQUE, ColumnEnum.RUBRIQUE_NMCL, ColumnEnum.TODO,
			ColumnEnum.COMMENTAIRE) //
	, IHM_NORME("ihm_norme", SchemaEnum.ARC_METADATA, ColumnEnum.ID_FAMILLE, ColumnEnum.ID_NORME,
			ColumnEnum.PERIODICITE, ColumnEnum.DEF_NORME, ColumnEnum.DEF_VALIDITE, ColumnEnum.ETAT) //
	, IHM_SCHEMA_NMCL("ihm_schema_nmcl", SchemaEnum.ARC_METADATA, ColumnEnum.TYPE_NMCL, ColumnEnum.NOM_COLONNE,
			ColumnEnum.TYPE_COLONNE) //
	, IHM_USER("ihm_user", SchemaEnum.ARC_METADATA, ColumnEnum.IDEP, ColumnEnum.PROFIL) //
	, IHM_WEBSERVICE_LOG("ihm_webservice_log", SchemaEnum.ARC_METADATA, ColumnEnum.ID_WEBSERVICE_LOGGING,
			ColumnEnum.ID_FAMILLE, ColumnEnum.ID_APPLICATION, ColumnEnum.HOST_ALLOWED, ColumnEnum.EVENT_TIMESTAMP) //
	, IHM_WEBSERVICE_WHITELIST("ihm_webservice_whitelist", SchemaEnum.ARC_METADATA, ColumnEnum.HOST_ALLOWED,
			ColumnEnum.ID_FAMILLE, ColumnEnum.ID_APPLICATION, ColumnEnum.IS_SECURED) //
	, IHM_WS_CONTEXT("ihm_ws_context", SchemaEnum.ARC_METADATA, ColumnEnum.SERVICE_NAME, ColumnEnum.SERVICE_TYPE,
			ColumnEnum.CALL_ID, ColumnEnum.ENVIRONMENT, ColumnEnum.TARGET_PHASE, ColumnEnum.NORME, ColumnEnum.VALIDITE,
			ColumnEnum.PERIODICITE) //
	, IHM_WS_QUERY("ihm_ws_query", SchemaEnum.ARC_METADATA, ColumnEnum.QUERY_ID, ColumnEnum.QUERY_NAME,
			ColumnEnum.EXPRESSION, ColumnEnum.QUERY_VIEW, ColumnEnum.SERVICE_NAME, ColumnEnum.CALL_ID) //

	// tables de paramètre
	, PARAMETER("parameter", SchemaEnum.ARC_METADATA, ColumnEnum.KEY, ColumnEnum.VAL, ColumnEnum.DESCRIPTION) //
	, PILOTAGE_BATCH("pilotage_batch", SchemaEnum.ARC_METADATA, ColumnEnum.LAST_INIT, ColumnEnum.OPERATION) //

	// table d'export
	, EXPORT("export", SchemaEnum.SANDBOX, ColumnEnum.FILE_NAME, ColumnEnum.ZIP, ColumnEnum.TABLE_TO_EXPORT,
			ColumnEnum.HEADERS, ColumnEnum.NULLS, ColumnEnum.FILTER_TABLE, ColumnEnum.ORDER_TABLE,
			ColumnEnum.NOMENCLATURE_EXPORT, ColumnEnum.COLUMNS_ARRAY_HEADER, ColumnEnum.COLUMNS_ARRAY_VALUE,
			ColumnEnum.ETAT) //

	// tables représentant le contenu des vobject (utilisées pour les tests)
	, VIEW_PILOTAGE_FICHIER("pilotage_fichier", SchemaEnum.SANDBOX, ColumnEnum.DATE_ENTREE) //
	, VIEW_RAPPORT_FICHIER("pilotage_fichier", SchemaEnum.SANDBOX, ColumnEnum.DATE_ENTREE,
			ColumnEnum.PHASE_TRAITEMENT, ColumnEnum.ETAT_TRAITEMENT, ColumnEnum.RAPPORT, ColumnEnum.NB) //
	// tables de pilotage
	, PILOTAGE_FICHIER("pilotage_fichier", SchemaEnum.SANDBOX, ColumnEnum.ID_SOURCE, ColumnEnum.ID_NORME,
			ColumnEnum.VALIDITE, ColumnEnum.PERIODICITE, ColumnEnum.PHASE_TRAITEMENT, ColumnEnum.ETAT_TRAITEMENT,
			ColumnEnum.DATE_TRAITEMENT, ColumnEnum.RAPPORT, ColumnEnum.TAUX_KO, ColumnEnum.NB_ENR, ColumnEnum.NB_ESSAIS,
			ColumnEnum.ETAPE, ColumnEnum.VALIDITE_INF, ColumnEnum.VALIDITE_SUP, ColumnEnum.VERSION,
			ColumnEnum.DATE_ENTREE, ColumnEnum.CONTAINER, ColumnEnum.V_CONTAINER, ColumnEnum.O_CONTAINER,
			ColumnEnum.TO_DELETE, ColumnEnum.CLIENT, ColumnEnum.DATE_CLIENT, ColumnEnum.JOINTURE,
			ColumnEnum.GENERATION_COMPOSITE) //
	, PILOTAGE_ARCHIVE("pilotage_archive", SchemaEnum.SANDBOX, ColumnEnum.ENTREPOT , ColumnEnum.NOM_ARCHIVE)

	// family model table in sandbox
	, MOD_TABLE_METIER("mod_table_metier", SchemaEnum.SANDBOX_GENERATED, ColumnEnum.ID_FAMILLE,
			ColumnEnum.NOM_TABLE_METIER, ColumnEnum.DESCRIPTION_TABLE_METIER) //
	, MOD_VARIABLE_METIER("mod_variable_metier", SchemaEnum.SANDBOX_GENERATED, ColumnEnum.ID_FAMILLE,
			ColumnEnum.NOM_TABLE_METIER, ColumnEnum.NOM_VARIABLE_METIER, ColumnEnum.TYPE_VARIABLE_METIER,
			ColumnEnum.DESCRIPTION_VARIABLE_METIER, ColumnEnum.TYPE_CONSOLIDATION) //


	// rule model tables in sandbox
	, NORME("norme", SchemaEnum.SANDBOX_GENERATED, ColumnEnum.ID_FAMILLE, ColumnEnum.ID_NORME, ColumnEnum.PERIODICITE,
			ColumnEnum.DEF_NORME, ColumnEnum.DEF_VALIDITE, ColumnEnum.ETAT) //
	, CALENDRIER("calendrier", SchemaEnum.SANDBOX_GENERATED, ColumnEnum.ID_NORME, ColumnEnum.PERIODICITE,
			ColumnEnum.VALIDITE_INF, ColumnEnum.VALIDITE_SUP, ColumnEnum.ETAT) //
	, JEUDEREGLE("jeuderegle", SchemaEnum.SANDBOX_GENERATED, ColumnEnum.ID_NORME, ColumnEnum.PERIODICITE,
			ColumnEnum.VALIDITE_INF, ColumnEnum.VALIDITE_SUP, ColumnEnum.VERSION, ColumnEnum.ETAT,
			ColumnEnum.DATE_PRODUCTION, ColumnEnum.DATE_INACTIF) //
	, CHARGEMENT_REGLE("chargement_regle", SchemaEnum.SANDBOX_GENERATED) //
	, NORMAGE_REGLE("normage_regle", SchemaEnum.SANDBOX_GENERATED) //
	, CONTROLE_REGLE("controle_regle", SchemaEnum.SANDBOX_GENERATED) //
	, MAPPING_REGLE("mapping_regle", SchemaEnum.SANDBOX_GENERATED) //
	, EXPRESSION("expression", SchemaEnum.SANDBOX_GENERATED) //
			
	// tables utilisés pour les tests
	, TABLE_TEST_IN_PUBLIC(PgViewEnum.TABLE_TEST_IN_PUBLIC)
	, TABLE_TEST_OUT_PUBLIC(PgViewEnum.TABLE_TEST_OUT_PUBLIC)

	, TABLE_TEST_IN_TEMPORARY(PgViewEnum.TABLE_TEST_IN_TEMPORARY)
	, TABLE_TEST_OUT_TEMPORARY(PgViewEnum.TABLE_TEST_OUT_TEMPORARY)

	// view for table aliases or temporary table in query
	, T1(PgViewEnum.T1), T2(PgViewEnum.T2), T3(PgViewEnum.T3)
	
	, TMP_CHARGEMENT_ARC(PgViewEnum.ALIAS_A), TMP_CHARGEMENT_BRUT(PgViewEnum.ALIAS_B)

	
	, ALIAS_A(PgViewEnum.ALIAS_A), ALIAS_B(PgViewEnum.ALIAS_B), ALIAS_C(PgViewEnum.ALIAS_C)

	, TMP_FILES("tmp_files", SchemaEnum.TEMPORARY, ColumnEnum.FILE_NAME)

	, PG_TABLES(PgViewEnum.PG_TABLES)

	;

	private ViewEnum(String tableName, SchemaEnum location, ColumnEnum... columns) {
		this.tableName = tableName;
		this.tableLocation = location;

		this.columns = new LinkedHashMap<>();
		for (ColumnEnum col : columns) {
			this.columns.put(col, col);

		}
	}

	private ViewEnum(PgViewEnum view) {
		this.tableName = view.getTableName();
		this.tableLocation = SchemaEnum.valueOf(view.getTableLocation().name());
		this.columns = new LinkedHashMap<>();
		for (PgColumnEnum col : view.getColumns().keySet()) {
			this.columns.put(ColumnEnum.valueOf(col.name()), ColumnEnum.valueOf(col.name()));
		}
	}

	/**
	 * database real name
	 */
	private String tableName;

	/**
	 * indicate if the table belongs to a sandbox
	 */
	private SchemaEnum tableLocation;

	private Map<ColumnEnum, ColumnEnum> columns;

	public String getTableName() {
		return tableName;
	}

	public SchemaEnum getTableLocation() {
		return tableLocation;
	}

	public Map<ColumnEnum, ColumnEnum> getColumns() {
		return columns;
	}

	public String getFullName() {
		return DataObjectService.getFullTableNameInSchema(this.tableLocation, this.tableName);
	}

	public String getFullName(String schema) {
		return schema + SQL.DOT.getSqlCode() + this.tableName;
	}

	public static String getFullName(String schema, String providedTableName) {
		return providedTableName.contains(SQL.DOT.getSqlCode())? providedTableName : schema + SQL.DOT.getSqlCode() + providedTableName;
	}
	
	public ColumnEnum col(ColumnEnum e) {
		return this.getColumns().get(e);
	}
	
	@Override
	public String toString()
	{
		return this.getTableName();
	}
	
}
