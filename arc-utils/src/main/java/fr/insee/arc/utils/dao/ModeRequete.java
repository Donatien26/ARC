package fr.insee.arc.utils.dao;

public enum ModeRequete {
    NESTLOOP_ON("set enable_nestloop = on;"), //
    NESTLOOP_OFF("set enable_nestloop = off;"), //
    HASH_JOIN_ON("set enable_hashjoin = on;"), //
    HASH_JOIN_OFF("set enable_hashjoin = off;"), //
    MERGE_JOIN_ON("set enable_mergejoin = on;"), //
    MERGE_JOIN_OFF("set enable_mergejoin = off;"), //
    RESET_WORK_MEM("reset work_mem;"), //
    WORK_MEM_32("set work_mem = \"{}MB\";", 32), //
    RESET_TEMP_BUFFER("reset temp_buffers;"), //
    TEMP_BUFFER_32("set temp_buffers = \"{}MB\";", 32),
    SEQ_SCAN_ON("set enable_seqscan = on;"), //
    SEQ_SCAN_OFF("set enable_seqscan = off;"), //
    HASHAGG_ON("set enable_hashagg = on;"), //
    HASHAGG_OFF("set enable_hashagg = off;"), //
    EXTRA_FLOAT_DIGIT("set extra_float_digits=0;"),
    IHM_INDEXED("set enable_hashjoin=on; set enable_mergejoin=off; set enable_hashagg=on; set enable_seqscan=off; set enable_material=off; ");
   
    private String expression;

    private ModeRequete(String anExpression) {
        this.expression = anExpression;
    }

    private ModeRequete(String anExpression, int value) {
        this.expression = anExpression.replace("{}", String.valueOf(value));
    }

    @Override
    public String toString() {
        return this.expression;
    }

    public String expr() {
        return this.expression;
    }
    

    /**
     * untokenize the configuration modes to be used in the query
     * @param modes
     * @return
     */
	public static String untokenize(ModeRequete... modes) {
		StringBuilder returned = new StringBuilder();
		for (int i = 0; i < modes.length; i++) {
			returned.append(modes[i].expr()).append("\n");
		}
		return returned.toString();
	}


    
    /**
     * Configure query with modes
     * Extra float digit modes in activated by default
     * @param requete
     * @param modes
     * @return
     */
    public static String configureQuery(String requete, ModeRequete... modes)
    {
    	
    	StringBuilder query=new StringBuilder();
    	
		// user defined configuration modes
		query.append(ModeRequete.untokenize(modes));
		
		// decimal approximation configuration for float
		query.append(ModeRequete.EXTRA_FLOAT_DIGIT.expr());

		// query
		if (requete!=null)
		{
			query.append(requete);
		}
		
		return query.toString();
		
    }
	
}
