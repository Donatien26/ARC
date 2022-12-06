package fr.insee.arc.core.dataobjects;

import java.util.Arrays;
import java.util.Collection;

import fr.insee.arc.utils.dao.GenericPreparedStatementBuilder;

public class ArcPreparedStatementBuilder extends GenericPreparedStatementBuilder {

	public ArcPreparedStatementBuilder() {
		super();
	}

	public ArcPreparedStatementBuilder(String query) {
		super(query);
	}

	public ArcPreparedStatementBuilder(StringBuilder query) {
		super(query);
	}

	/**
	 * build a sql list of column based on a collection of arc column enumeration elements
	 * @param listOfColumns
	 * @return
	 */
	public StringBuilder sqlListeOfColumnsFromModel(Collection<ColumnEnum> listOfColumns) {
		return sqlListeOfColumns(ColumnEnum.listColumnEnumByName(listOfColumns));
	}

	/**
	 * return a liste of column based on a variable array of arc column enumeration elements
	 * @param columns
	 * @return
	 */
	public StringBuilder sqlListeOfColumnsArc(ColumnEnum... columns) {
		return sqlListeOfColumnsFromModel(Arrays.asList(columns));
	}

	
	/**
	 * return sql expression of table columns
	 * @param columns
	 * @return
	 */
	public StringBuilder sqlListeOfColumnsFromModel(ViewEnum tableEnum) {
		return sqlListeOfColumnsFromModel(tableEnum.getColumns());
	}
	
	
	public ArcPreparedStatementBuilder append(ColumnEnum column)
	{
		return (ArcPreparedStatementBuilder) this.append(column.getColumnName());
	}
	
}
