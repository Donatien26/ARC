package fr.insee.arc.utils.dao;

public class ModeRequeteImpl {
	
	public static final String PARALLEL_WORK_MEM = "24MB";

	public static final String SORT_WORK_MEM="128MB";
    
    public static final int TIME_OUT_SQL_EN_HEURE = 100;
	
    public static final String  COLLAPSE_JOIN_LIMIT = "10000";
    

	private ModeRequeteImpl() {
		throw new IllegalStateException("Utility class");
	}

	public static ModeRequete[] arcModeRequeteIHM() {
		return new ModeRequete[] { ModeRequete.HASH_JOIN_ON, ModeRequete.MERGE_JOIN_OFF, ModeRequete.HASHAGG_ON,
				ModeRequete.SEQSCAN_OFF, ModeRequete.MATERIAL_OFF };
	}
	
	
    /**
     * Configuration de la base de données pour des petites requetes
     *
     * @param defaultSchema
     * @return requete
     */
    public static String arcModeRequeteEngine(String defaultSchema)
    {
    	StringBuilder query=new StringBuilder();
    	query
    	.append(ModeRequete.NESTLOOP_ON)
    	.append(ModeRequete.MERGE_JOIN_OFF)
    	.append(ModeRequete.HASH_JOIN_ON)
    	.append(ModeRequete.MATERIAL_OFF)
    	.append(ModeRequete.SEQSCAN_OFF)
    	.append(ModeRequete.HASHAGG_ON)
    	.append("set work_mem='" + PARALLEL_WORK_MEM + "';")
    	.append("set maintenance_work_mem='" + PARALLEL_WORK_MEM+"';")
    	.append("set temp_buffers='" + PARALLEL_WORK_MEM + "';")
    	.append("set statement_timeout="+ (3600000 * TIME_OUT_SQL_EN_HEURE) + ";")
    	.append("set from_collapse_limit="+COLLAPSE_JOIN_LIMIT+";")
    	.append("set join_collapse_limit="+COLLAPSE_JOIN_LIMIT+";")
    	.append("set search_path=" + defaultSchema.toLowerCase() + ", public;")
    	.append(ModeRequete.EXTRA_FLOAT_DIGIT.expr())
    	.append("COMMIT;")
    	;
    	return query.toString();
    }

}
