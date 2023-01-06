package fr.insee.arc.utils.dao;

public enum SQL {
	SELECT("SELECT"),
	FROM("FROM"),
	WHERE("WHERE"),
	ORDER_BY("ORDER BY"),
	
	COMMA(","),
	AS("AS"),
	
	INSERT_INTO("INSERT INTO"),
	
	ON_CONFLICT_DO_NOTHING ("ON CONFLICT DO NOTHING"),
	
	BEGIN("BEGIN;"),
	END("END;"),
	
	AND("AND"),
	UNION_ALL("UNION ALL"),
	
	END_QUERY(";");
	
	private String sqlCode;
	
	private final static String SPACE=" ";

	private SQL(String sqlCode) {
		this.sqlCode = sqlCode;
	}

	public String getSqlCode() {
		return sqlCode;
	}

	@Override
	public String toString()
	{
		return SPACE+this.sqlCode+SPACE;
	}
	
	
}
