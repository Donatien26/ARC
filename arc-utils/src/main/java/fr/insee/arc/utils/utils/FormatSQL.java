package fr.insee.arc.utils.utils;

import java.sql.SQLException;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.core.Utils;

import fr.insee.arc.utils.dao.ModeRequete;
import fr.insee.arc.utils.dao.PreparedStatementBuilder;
import fr.insee.arc.utils.exception.ArcException;
import fr.insee.arc.utils.textUtils.IConstanteCaractere;
import fr.insee.arc.utils.textUtils.IConstanteNumerique;

public class FormatSQL implements IConstanteCaractere, IConstanteNumerique
{
    public static final String NULL = "null";
    public static final String WITH_AUTOVACUUM_FALSE = "" + FormatSQL.WITH_NO_VACUUM + "";
    public static final String COLLATE_C = "COLLATE pg_catalog.\"C\"";
    private static final String TEXT = "text";
    public static final String TEXT_COLLATE_C = TEXT + SPACE + COLLATE_C;
    public static final String IS_NOT_DISTINCT_FROM = "IS NOT DISTINCT FROM";
    public static final String NO_VACUUM = " (autovacuum_enabled = false, toast.autovacuum_enabled = false) ";
    public static final String WITH_NO_VACUUM = " WITH" + NO_VACUUM;
    public static final String defaultSeparator = ";\n";
    public static final String _TMP = "$tmp$";
    public static final String _REGEX_TMP = "\\$tmp\\$";

    public static final String PARALLEL_WORK_MEM = "24MB";
    public static final String SEQUENTIAL_WORK_MEM = "32MB";
    
    public static final boolean DROP_FIRST_FALSE = false;
    public static final boolean DROP_FIRST_TRUE = true;
    
    public static final int TAILLE_MAXIMAL_BLOC_SQL = 300000;
    public static final int MAX_LOCK_PER_TRANSACTION = 50;
    public static final int TIME_OUT_SQL_EN_HEURE = 100;
    public static final int TIMEOUT_MAINTENANCE = 600000;
    
    public static final String VACUUM_OPTION_NONE="";
    public static final String VACUUM_OPTION_FULL="full";
    
    private static final Logger LOGGER = LogManager.getLogger(FormatSQL.class);

    
    public enum ObjectType
    {
        TABLE("TABLE"), VIEW("VIEW"), TEMPORARY_TABLE ("TEMPORARY TABLE");
        private String name;

        private ObjectType(String aName)
        {
            this.name = aName;
        }

        @Override
        public String toString()
        {
            return this.name;
        }
    }

    public static String end(String[] separator)
    {
        String end;
        if (separator == null || separator.length == 0)
        {
            end = defaultSeparator;
        }
        else
        {
            end = separator[0];
        }
        return end;
    }

    public static String dropObjectCascade(ObjectType tableOrView, String anObjectName, String... separator)
    {
        StringBuilder sql = new StringBuilder("\n DROP " + tableOrView + " IF EXISTS " + anObjectName + " CASCADE "+end(separator));
        return sql.toString();
    }
    
    public static String dropTable(String tableName, String... separator) {
	return dropObjectCascade(ObjectType.TABLE,tableName,separator);
    }
    
    public static PreparedStatementBuilder tableExists(String table, String... separator) {
	String tableSchema = ManipString.substringBeforeFirst(table, DOT);
	String tableName = ManipString.substringAfterLast(table, DOT);
	PreparedStatementBuilder requete = new PreparedStatementBuilder();
	requete.append("SELECT schemaname||'.'||tablename AS table_name FROM pg_tables ");
	requete.append("\n WHERE tablename like " + requete.quoteText(tableName.toLowerCase()) + " ");
	if (table.contains(DOT)) {
		requete.append("\n AND schemaname = " + requete.quoteText(tableSchema.toLowerCase()) + " ");
	}
	requete.append(end(separator));

	return requete;
    }

   
    /**
     * Pour récupérer la liste des colonnes d'une table rapidement
     *
     * @param table
     * @return
     */
    public static PreparedStatementBuilder listeColonneByHeaders(String table)
    {
        return new PreparedStatementBuilder("select * from " + table + " where false; ");
    }

    /**
     * Switch the database user
     * @param roleName
     * @return
     * @throws ArcException
     */
	public static String changeRole(String roleName)
	{
		return "SET role='"+roleName+"';COMMIT;";
	}

    /**
     * Configuration de la base de données pour des petites requetes
     *
     * @param defaultSchema
     * @return requete
     */
    public static String modeParallel(String defaultSchema)
    {
    	StringBuilder query=new StringBuilder();
    	query
    	.append("set enable_nestloop=on;")
    	.append("set enable_mergejoin=off;")
    	.append("set enable_hashjoin=on;")
    	.append("set enable_material=off;")
    	.append("set enable_seqscan=off;")
    	.append("set work_mem='" + PARALLEL_WORK_MEM + "';")
    	.append("set maintenance_work_mem='" + PARALLEL_WORK_MEM+"';")
    	.append("set temp_buffers='" + PARALLEL_WORK_MEM + "';")
    	.append("set statement_timeout="+ (3600000 * TIME_OUT_SQL_EN_HEURE) + ";")
    	.append("set from_collapse_limit=10000;")
    	.append("set join_collapse_limit=10000;")
    	.append("set enable_hashagg=on;")
    	.append("set search_path=" + defaultSchema.toLowerCase() + ", public;")
    	.append(ModeRequete.EXTRA_FLOAT_DIGIT.expr())
    	.append("COMMIT;")
    	;
    	return query.toString();
    }
    
    /**
     * timeOut
     */
    public static String setTimeOutMaintenance()
    {
        return "BEGIN;SET statement_timeout="+TIMEOUT_MAINTENANCE+";COMMIT;";
    }
    
    public static String resetTimeOutMaintenance()
    {
        return "BEGIN;RESET statement_timeout;COMMIT;";
    }
    
    
    /**
     * essaie d'exectuer une requete et si elle n'échoue ne fait rien
     */
    public static String tryQuery(String query)
    {
        return "do $$ begin " + query + " exception when others then end; $$; ";
    }

    /**
     * Met entre cote ou renvoie null (comme pour un champ de base de donnée)
     *
     * @param t
     * @return
     */
    public static String cast(String t)
    {
        if (t == null)
        {
            return "null";
        }
        else
        {
            return "'" + t + "'";
        }
    }

    /**
     * Lance un vacuum d'un certain type sur une table
     * @param table
     * @param type
     * @return
     */
    public static String vacuumSecured(String table, String type)
    {    		
    	return "VACUUM "+ type +" " + table + "; COMMIT; \n"; 
    }

    /**
     * Lance un vacuum d'un certain type sur une table
     * @param table
     * @param type
     * @return
     */
    public static String analyzeSecured(String table)
    {    		
    	return "ANALYZE " + table + "; COMMIT; \n"; 
    }
    
    /**
     * Recopie une table à l'identique
     *
     * @param table
     * @param where
     * @param triggersAndIndexes
     * @return
     */
    public static StringBuilder rebuildTableAsSelectWhere(String table, String where, String... triggersAndIndexes)
    {
        String tableRebuild = temporaryTableName(table, "RB");
        StringBuilder requete = new StringBuilder();
        requete.append("set enable_nestloop=off; ");
        requete.append("\n DROP TABLE IF EXISTS " + tableRebuild + " CASCADE; ");
        requete.append("\n CREATE ");
        if (!table.contains("."))
        {
            requete.append("TEMPORARY ");
        }
        else
        {
            requete.append(" ");
        }
        requete.append("TABLE " + tableRebuild + " " + FormatSQL.WITH_NO_VACUUM + " as select * FROM " + table
                + " a WHERE " + where + "; ");
        requete.append("\n DROP TABLE IF EXISTS " + table + " CASCADE;");
        requete.append(
                "\n ALTER TABLE " + tableRebuild + " RENAME TO " + ManipString.substringAfterFirst(table, ".") + " ;");
        requete.append("set enable_nestloop=on; ");
        for (int i = 0; i < triggersAndIndexes.length; i++)
        {
            requete.append(triggersAndIndexes[i]);
        }
        return requete;
    }

    /**
     *
     * Méthode de création d'une requête
     * {@code CREATE TABLE aNomTableCible AS SELECT columns FROM aNomTableSource WHERE clauseWhere;}
     * , éventuellement précédée d'un {@code DROP}
     *
     * @param aNomTableCible
     * @param aNomTableSource
     * @param columns
     * @param clauseWhere
     * @param dropFirst
     * @return
     */
    public static String createAsSelectFrom(String aNomTableCible, String aNomTableSource, String columns,
            String clauseWhere, boolean dropFirst)
    {
        // Si la table contient un . on est dans un schema, sinon c'est du temps
        if (aNomTableCible.contains(".")) {
            return createObjectAsSelectFrom(ObjectType.TABLE, aNomTableCible, aNomTableSource, columns, clauseWhere,
                    dropFirst);
        } else {
            return createObjectAsSelectFrom(ObjectType.TEMPORARY_TABLE, aNomTableCible, aNomTableSource, columns, clauseWhere,
                    dropFirst);
        }
        
       
    }

    
    
    /**
     * this sql block test is the query to test is true to execute the other query
     * @param queryToTest
     * @param queryToExecute
     * @return
     */
    public static String executeIf(String queryToTest, String queryToExecute)
    {
    	StringBuilder query=new StringBuilder();
    	query
    	.append("do $$ declare b boolean; begin execute ")
    	.append(quoteText(queryToTest))
    	.append(" into b; ")
    	.append("if (b) then execute ")
    	.append(quoteText(queryToExecute))
    	.append("; end if; end; $$;");
    	return query.toString();
    }

    public static String executeIf(StringBuilder queryToTest, StringBuilder queryToExecute)
    {
    	return executeIf(queryToTest.toString(), queryToExecute.toString());
    }

    /**
     * 
     * @param tableOrView
     * @param aNomTableCible
     * @param aNomTableSource
     * @param columns
     * @param clauseWhere
     * @param dropFirst
     * @return
     */
    private static String createObjectAsSelectFrom(ObjectType tableOrView, String aNomTableCible,
            String aNomTableSource, String columns, String clauseWhere, boolean dropFirst)
    {
        StringBuilder requete = new StringBuilder();
        if (dropFirst)
        {
            requete.append(dropObjectCascade(tableOrView, aNomTableCible));
        }
        String where = ((StringUtils.isBlank(clauseWhere)) ? EMPTY : " WHERE " + clauseWhere);
        /*
         * Attention ! Les vues ne peuvent être créées avec un
         * autovacuum_enabled
         */
        String vacuumIfNeeded = tableOrView.equals(ObjectType.TABLE) ? " " + FormatSQL.WITH_NO_VACUUM : "";
        String orReplaceForViewsOnly = tableOrView.equals(ObjectType.VIEW) ? "OR REPLACE " : "";
        requete.append(
                "\n CREATE " + orReplaceForViewsOnly + tableOrView + " " + aNomTableCible + vacuumIfNeeded + " AS ");
        requete.append("\n SELECT " + columns + " FROM " + aNomTableSource);
        requete.append(where + ";");
        return requete.toString();
    }

    /**
    *
    * @param aNomTableCible
    * @param aNomTableSource
    * @param clauseWhere
    *            le WHERE n'y est pas, je le rajouterai tout seul merci.
    * @return
    */
   public static String createAsSelectFrom(String aNomTableCible, String aNomTableSource, String clauseWhere)
   {
       return createAsSelectFrom(aNomTableCible, aNomTableSource, "*", clauseWhere, DROP_FIRST_FALSE);
   }
    
    
    
    /**
     * @param table
     * @return
     */
    public static PreparedStatementBuilder isTableExists(String table)
    {
        String tokenJoin = table.contains(".") ?
        /*
         * Le nom de la table contient "." ? Il est précédé du nom du schéma.
         */
                " INNER JOIN pg_namespace ON pg_class.relnamespace = pg_namespace.oid" :
                /*
                 * Sinon, aucune jointure sur le nom de schéma.
                 */
                "";
        String tokenCond = table.contains(".") ?
        /*
         * Le nom de la table contient "." ? Il est précédé du nom de schéma.
         */
                "lower(pg_namespace.nspname||'.'||pg_class.relname)" :
                /*
                 * Sinon, la condition d'égalité porte sur le nom de la table
                 */
                "pg_class.relname";
        PreparedStatementBuilder requete = new PreparedStatementBuilder(
                "SELECT CASE WHEN count(1)>0 THEN TRUE ELSE FALSE END table_existe\n");
        requete.append("  FROM pg_class" + tokenJoin);
        requete.append("  WHERE " + tokenCond + " = lower(" + requete.quoteText(table) + ")");
        return requete;
    }

    /**
     * Ajoute un suffixe de table temporaire au nom de table {@code aName}
     *
     * @param aName
     * @return
     */
    public static final String temporaryTableName(String aName)
    {
        String newName = aName.split(_REGEX_TMP)[0];
        // on met la date du jour dans le nom de la table
        String l = System.currentTimeMillis() + "";
        // on prend que les 10 derniers chiffres (durrée de vie : 6 mois)
        l = l.substring(l.length() - 10);
        // on inverse la chaine de caractere pour avoir les millisecondes en
        // premier en cas de troncature
        l = new StringBuffer(l).reverse().toString();
        return new StringBuilder(newName).append(_TMP).append(l).append(DOLLAR).append(randomNumber(4)).toString();
    }

    /**
     * Ajoute un suffixe de table temporaire au nom de table {@code prefix}
     *
     * @param aName
     *            le nom de la table
     * @param suffix
     *            un suffixe
     * @return
     */
    public final static String temporaryTableName(String aName, String suffix)
    {
        String newName = aName.split(_REGEX_TMP)[0];
        return temporaryTableName(newName + UNDERSCORE + suffix);
    }

    /**
     *
     * @return Un nombre aléatoire d'une certaine précision
     */
    public static final String randomNumber(int precision)
    {
        String rn = ((int) Math.floor((Math.random() * (Math.pow(10, precision))))) + "";
        return ManipString.padLeft(rn, "0", precision);
    }

    /**
     * converti une chaine de caractere pour etre mise en parametre d'un sql si
     * c'est vide, ca devient "null" quote devient quote quote
     *
     * @param val
     * @return
     */
    public static String textToSql(String val)
    {
        if (val == null || val.trim().equals(""))
        {
            return "null";
        }
        else
        {
            return "'" + val.replace("'", "''") + "'";
        }
    }

    /**
     * Ne garde que les séparateurs
     *
     * @param tokens
     * @param separator
     * @return
     */
    public static String toNullRow(Collection<?> tokens)
    {
        return (tokens == null || tokens.isEmpty()) ? "(" + EMPTY + ")"
                : "(" + StringUtils.repeat(",", tokens.size() - 1) + ")";
    }

    /**
     * Renvoie les tables héritant de celle-ci
     * Colonnes de résultat:
     * @child (schema.table)
     */
    public static PreparedStatementBuilder getAllInheritedTables(String tableSchema, String tableName) {
    	PreparedStatementBuilder requete = new PreparedStatementBuilder();
    	requete.append("\n SELECT cn.nspname||'.'||c.relname AS child ");
    	requete.append("\n FROM pg_inherits  ");
    	requete.append("\n JOIN pg_class AS c ON (inhrelid=c.oid) ");
    	requete.append("\n JOIN pg_class as p ON (inhparent=p.oid) ");
    	requete.append("\n JOIN pg_namespace pn ON pn.oid = p.relnamespace ");
    	requete.append("\n JOIN pg_namespace cn ON cn.oid = c.relnamespace ");
    	requete.append("\n WHERE p.relname = "+requete.quoteText(tableName)+" and pn.nspname = "+requete.quoteText(tableSchema)+" ");
    	return requete;
    }
    
    /**
     * escape quote return value through function
     * @param s
     * @return
     * @throws ArcException 
     */
    public static String quoteText(String s)
    {
    	try {
			return "'" + Utils.escapeLiteral(null, s, true) + "'";
		} catch (SQLException e) {
			LoggerHelper.errorAsComment(LOGGER, "This string cannot be escaped to postgres database format");
			return null;
		}
    }
    
    
    public static String toDate(String dateTextIn, String formatIn)
    {
    	return "to_date("+dateTextIn+"::text,"+formatIn+")";
    }
    
}
