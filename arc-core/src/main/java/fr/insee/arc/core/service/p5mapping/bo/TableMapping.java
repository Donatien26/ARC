package fr.insee.arc.core.service.p5mapping.bo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import fr.insee.arc.core.dataobjects.ArcPreparedStatementBuilder;
import fr.insee.arc.core.dataobjects.ColumnEnum;
import fr.insee.arc.core.dataobjects.ViewEnum;
import fr.insee.arc.core.service.p5mapping.bo.rules.RegleMappingClePrimaire;
import fr.insee.arc.core.service.p5mapping.dao.MappingQueries;
import fr.insee.arc.utils.database.Delimiters;
import fr.insee.arc.utils.exception.ArcException;
import fr.insee.arc.utils.format.Format;
import fr.insee.arc.utils.textUtils.IConstanteCaractere;
import fr.insee.arc.utils.textUtils.IConstanteNumerique;
import fr.insee.arc.utils.utils.FormatSQL;
import fr.insee.arc.utils.utils.ManipString;

/**
 *
 * Représente une table mapping telle que décrite dans les tables
 * {@code <environnement>_mod_table_metier} et
 * {@code <environnement>_mod_variable_metier}.<br/>
 * Est composé de {@link VariableMapping}, séparées en variables clef et
 * variables non clefs. Rien de très intéressant par là.
 *
 *
 */
public class TableMapping implements IConstanteCaractere, IConstanteNumerique {

	private static final String ARC_PROCESSING_TABLE = "ArcProcessingTable";

	public static final Integer GROUPE_UN = 1;

	private String nomTable;

	private String nomTableCourt;

	private SortedSet<VariableMapping> ensembleVariableMapping;

	private String environnement;

	private Set<String> ensembleIdentifiantsRubriques;

	private SortedSet<VariableMapping> ensembleVariableNonClef;
	private SortedSet<VariableMapping> ensembleVariableClef;
	private SortedSet<String> ensembleVariableClefString;

	private boolean isGroupe;

	private SortedMap<Integer, Set<String>> mapGroupeToEnsembleIdentifiantsRubriques;
	private SortedMap<Integer, Set<String>> mapGroupeToEnsembleNomsRubriques;

	private Set<String> ensembleNomsRubriques;

	private String nomTableTemporaire;

	private Set<RegleMappingClePrimaire> ensembleRegleMappingClefPrimaire;

	public TableMapping(String anEnvironnement, String aNomTableCourt) {
		this.nomTableCourt = aNomTableCourt;
		this.nomTable = ViewEnum.getFullName(anEnvironnement,aNomTableCourt);
		this.ensembleVariableMapping = new TreeSet<>();
		this.environnement = anEnvironnement;
		this.ensembleRegleMappingClefPrimaire = new HashSet<>();
		this.nomTableTemporaire = "tableMappingTemp_" + this.nomTableCourt;
		this.ensembleIdentifiantsRubriques = new HashSet<>();
		this.mapGroupeToEnsembleIdentifiantsRubriques = new TreeMap<>();
		this.mapGroupeToEnsembleNomsRubriques = new TreeMap<>();
		this.ensembleNomsRubriques = new HashSet<>();
		this.ensembleVariableClef = new TreeSet<>();
		this.ensembleVariableClefString = new TreeSet<>();
		this.ensembleVariableNonClef = new TreeSet<>();
		this.isGroupe = false;
	}

	public void ajouterVariable(VariableMapping variable) {
		this.ensembleVariableMapping.add(variable);
	}

	/**
	 * @return the nomTable
	 */
	public String getNomTable() {
		return this.nomTable;
	}

	/**
	 * @return the nomTableCourt
	 */
	public String getNomTableCourt() {
		return this.nomTableCourt;
	}

	/**
	 * @return the ensembleVariableMapping
	 */
	public SortedSet<VariableMapping> getEnsembleVariableMapping() {
		return this.ensembleVariableMapping;
	}

	public Set<String> getEnsembleVarMapping() {
		Set<String> s = new HashSet<>();

		for (VariableMapping v : this.ensembleVariableMapping) {
			s.add(v.toString());
		}

		return s;
	}

	/**
	 * Constitue les listes :<br/>
	 * 1. Des identifiants de rubriques non groupes.<br/>
	 * 2. Des noms de rubriques non groupes.<br/>
	 * 3. Des identifiants de rubriques de groupes (ils sont stockés dans une table
	 * associative).<br/>
	 * 4. Des noms de rubriques de groupes (ils sont stockés dans une table
	 * associative).<br/>
	 */
	public void construireEnsembleRubrique() {
		for (VariableMapping variable : this.ensembleVariableMapping) {
			this.ensembleIdentifiantsRubriques.addAll(variable.getEnsembleIdentifiantsRubriques());
			this.ensembleNomsRubriques.addAll(variable.getEnsembleNomsRubriques());
			for (Integer groupe : variable.getEnsembleGroupes()) {
				if (!this.mapGroupeToEnsembleIdentifiantsRubriques.containsKey(groupe)) {
					this.mapGroupeToEnsembleIdentifiantsRubriques.put(groupe, new HashSet<>());
					this.mapGroupeToEnsembleNomsRubriques.put(groupe, new HashSet<>());
				}
				this.mapGroupeToEnsembleIdentifiantsRubriques.get(groupe)
						.addAll(variable.getEnsembleIdentifiantsRubriques(groupe));
				this.mapGroupeToEnsembleNomsRubriques.get(groupe).addAll(variable.getEnsembleNomsRubriques(groupe));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.environnement == null) ? 0 : this.environnement.hashCode());
		result = prime * result + ((this.nomTableCourt == null) ? 0 : this.nomTableCourt.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TableMapping)) {
			return false;
		}
		TableMapping other = (TableMapping) obj;
		if (this.environnement == null) {
			if (other.environnement != null) {
				return false;
			}
		} else if (!this.environnement.equals(other.environnement)) {
			return false;
		}
		if (this.nomTableCourt == null) {
			if (other.nomTableCourt != null) {
				return false;
			}
		} else if (!this.nomTableCourt.equals(other.nomTableCourt)) {
			return false;
		}
		return true;
	}

	/**
	 * @return the ensembleIdentifiantsRubriques
	 */
	public Set<String> getEnsembleIdentifiantsRubriques() {
		return this.ensembleIdentifiantsRubriques;
	}

	/**
	 *
	 * @param aNumeroGroupe
	 * @return l'ensemble des identifiants de rubriques pour le groupe
	 *         {@code aNumeroGroupe} et pour cette table.
	 */
	public Set<String> getEnsembleIdentifiantsRubriques(Integer aNumeroGroupe) {
		Set<String> returned = new HashSet<>();
		for (VariableMapping variable : this.ensembleVariableMapping) {
			returned.addAll(variable.getEnsembleIdentifiantsRubriques(aNumeroGroupe));
		}
		return returned;
	}

	/**
	 * @return the ensembleNomsRubriques
	 */
	public Set<String> getEnsembleNomsRubriques() {
		return this.ensembleNomsRubriques;
	}

	/**
	 *
	 *
	 * @return the ensembleGroupes
	 */
	public Set<Integer> getEnsembleGroupes() {
		return this.mapGroupeToEnsembleIdentifiantsRubriques.keySet();
	}

	public String getNomTableTemporaire() {
		return this.nomTableTemporaire;
	}

	public ArcPreparedStatementBuilder requeteCreation() {
		ArcPreparedStatementBuilder returned = new ArcPreparedStatementBuilder();
		returned.append(FormatSQL.dropTable(this.getNomTableTemporaire()));
		returned.append("CREATE TEMPORARY TABLE "+ this.getNomTableTemporaire() + " (");
		boolean isFirst = true;
		for (VariableMapping variable : this.getEnsembleVariableMapping()) {
			if (isFirst) {
				isFirst = false;
			} else {
				returned.append(", ");
			}
			returned.append(variable.getNomVariable() + " " + variable.getType());
		}
		returned.append(") " + FormatSQL.WITH_NO_VACUUM + ";");
		return returned;
	}

	/**
	 * Compute the sql expression for the temporary table needed to prepare the
	 * mapping calculation More exactly it computes all the identifier of the model
	 * tables and the link between them
	 * 
	 * @param aNumeroGroupe
	 * @param nomsVariablesIdentifiantes
	 * @param reglesIdentifiantes
	 * @return
	 * @throws ArcException
	 */
	public String expressionSQLPrepUnion(Integer aNumeroGroupe, Map<String, String> reglesIdentifiantes)
			throws ArcException {
		StringBuilder returned = new StringBuilder();
		boolean isFirst = true;

		for (VariableMapping variable : this.ensembleVariableClef) {
			if (isFirst) {
				isFirst = false;
			} else {
				returned.append(", ");
			}
			String nomVariable = variable.getNomVariable() + Delimiters.SQL_TOKEN_DELIMITER + aNumeroGroupe;
			String expression = reglesIdentifiantes.get(nomVariable);
			if (expression != null) {
				expression = variable.getNomVariable() + "::text as " + variable.getNomVariable();
			} else {
				nomVariable = variable.getNomVariable();
				expression = reglesIdentifiantes.get(nomVariable);
				if (expression != null) {
					expression = nomVariable + "::text as " + nomVariable;
				} else {
					expression = "null::text as " + nomVariable;
				}
			}
			returned.append(expression);
		}

		isFirst = this.ensembleVariableClef.isEmpty();
		for (VariableMapping variable : this.ensembleVariableNonClef) {
			if (isFirst) {
				isFirst = false;
			} else {
				returned.append(", ");
			}
			returned.append(variable.expressionSQLtoText(aNumeroGroupe) + " as " + variable.getNomVariable());
		}

		return applyModelTableIdentifier(returned.toString());
	}

	/**
	 * 
	 * @param expression
	 * @return
	 */
	private String applyModelTableIdentifier(String expression) {
		return expression.replace(ARC_PROCESSING_TABLE, "'" + this.getNomTableCourt() + "'");
	}

	/**
	 *
	 * @return {@code INSERT INTO <table_definitive> (<champs>) SELECT <champs> FROM <table_temporaire>}
	 */
	public String requeteTransfererVersTableFinale() {
		StringBuilder returned = new StringBuilder("INSERT INTO " + this.getNomTable());
		StringBuilder nomsVariables = new StringBuilder();
		sqlListeVariables(nomsVariables);
		returned.append(" (" + nomsVariables + ")")//
				.append("\nSELECT " + nomsVariables)//
				.append("\nFROM " + this.getNomTableTemporaire() + semicolon + newline);
		return returned.toString();
	}

	/**
	 * @param returned
	 * @return
	 */
	private void sqlListeVariables(StringBuilder returned) {
		boolean isFirst = true;
		for (VariableMapping variable : this.ensembleVariableClef) {
			if (isFirst) {
				isFirst = false;
			} else {
				returned.append(", ");
			}
			returned.append(variable.getNomVariable());
		}
		isFirst = this.ensembleVariableClef.isEmpty();
		for (VariableMapping variable : this.ensembleVariableNonClef) {
			if (isFirst) {
				isFirst = false;
			} else {
				returned.append(", ");
			}
			returned.append(variable.getNomVariable());
		}
	}

	/**
	 * @param returned
	 * @param separateur
	 * @return
	 */
	public void sqlListeVariablesTypes(StringBuilder returned, String separateur, boolean removeArrayTypeForGroupe) {
		boolean isFirst = true;
		for (VariableMapping variable : this.getEnsembleVariableClef()) {
			if (isFirst) {
				isFirst = false;
			} else {
				returned.append(", ");
			}

			String type = variable.getType();
			// si removeArrayTypeForGroupe alors on retire l'aspect tableau du type de la
			// variable de groupe
			if (removeArrayTypeForGroupe && variable.isGroupe()) {
				type = type.replace("[]", "");
			}

			returned.append(variable.getNomVariable() + separateur + type);
		}

		isFirst = this.ensembleVariableClef.isEmpty();

		for (VariableMapping variable : this.ensembleVariableNonClef) {
			if (isFirst) {
				isFirst = false;
			} else {
				returned.append(", ");
			}

			String type = variable.getType();
			// si removeArrayTypeForGroupe alors on retire l'aspect tableau du type de la
			// variable de groupe
			if (removeArrayTypeForGroupe && variable.isGroupe()) {
				type = type.replace("[]", "");
			}

			returned.append(variable.getNomVariable() + separateur + type);
		}
	}

	/**
	 * les variables de type groupe sont mise en tableau avec la fonction array_agg
	 * alors que les autres variables constitue le group by
	 * 
	 * @param select  : la listes des variables de la clause select : var1, var2,
	 *                array_agg(var3 over prep_union.ctid) as var3, var4
	 * @param groupBy : la liste des variable de la clause groupe by : var1, var2,
	 *                var4
	 * @param alias
	 */
	public void sqlListeVariablesArrayAgg(StringBuilder select, StringBuilder groupBy, StringBuilder where,
			StringBuilder whereAllNonClefNull, StringBuilder allVar, String alias) {
		boolean isFirst = true;
		boolean isFirstGroupVar = true;
		boolean isFirstAggVar = true;
		boolean isFirstNonClef = true;

		StringBuilder where2 = new StringBuilder();
		StringBuilder whereAllNonClefNull2 = new StringBuilder();

		for (VariableMapping variable : this.getEnsembleVariableClef()) {
			if (isFirst) {
				isFirst = false;
			} else {
				select.append(", ");
				allVar.append(",");
			}

			if (variable.isGroupe()) {
				if (isFirstAggVar) {
					isFirstAggVar = false;
					where.append(" WHERE ROW(");
					where2.append(")::text != '(");
				} else {
					where.append(",");
					where2.append(",");

				}
				select.append(MappingQueries.FUNCTION_BEFORE + variable.getNomVariable() + MappingQueries.FUNCTION_AFTER
						+ " as " + variable.getNomVariable());
				allVar.append(variable.getNomVariable());
				where.append(variable.getNomVariable());

			} else {
				if (isFirstGroupVar) {
					isFirstGroupVar = false;
				} else {
					groupBy.append(",");
				}

				select.append(variable.getNomVariable());
				allVar.append(variable.getNomVariable());
				groupBy.append(MappingQueries.ALIAS_TABLE + "." + variable.getNomVariable());
			}
		}

		isFirst = this.ensembleVariableClef.isEmpty();

		for (VariableMapping variable : this.ensembleVariableNonClef) {
			if (isFirst) {
				isFirst = false;
			} else {
				select.append(", ");
				allVar.append(",");

			}

			if (!variable.getNomVariable().equals(ColumnEnum.ID_SOURCE.getColumnName())) {
				if (isFirstNonClef) {
					isFirstNonClef = false;
					whereAllNonClefNull.append(" WHERE ROW(");
					whereAllNonClefNull2.append(")::text='(");
				} else {
					whereAllNonClefNull.append(",");
					whereAllNonClefNull2.append(",");
				}
			}

			if (variable.isGroupe()) {
				if (isFirstAggVar) {
					isFirstAggVar = false;
					where.append(" WHERE ROW(");
					where2.append(")::text != '(");
				} else {
					where.append(",");
					where2.append(",");
				}

				select.append(MappingQueries.FUNCTION_BEFORE + variable.getNomVariable() + MappingQueries.FUNCTION_AFTER
						+ " as " + variable.getNomVariable());
				allVar.append(variable.getNomVariable());
				where.append(variable.getNomVariable());

				if (!variable.getNomVariable().equals(ColumnEnum.ID_SOURCE.getColumnName())) {
					whereAllNonClefNull.append(variable.getNomVariable());
					whereAllNonClefNull2.append("{NULL}");
				}
			} else {
				if (isFirstGroupVar) {
					isFirstGroupVar = false;
				} else {
					groupBy.append(",");
				}

				select.append(variable.getNomVariable());
				allVar.append(variable.getNomVariable());
				groupBy.append(MappingQueries.ALIAS_TABLE + "." + variable.getNomVariable());
				if (!variable.getNomVariable().equals(ColumnEnum.ID_SOURCE.getColumnName())) {
					whereAllNonClefNull.append(variable.getNomVariable());
				}
			}

		}

		if (where.length() > 0) {
			where2.append(")'");
			where.append(where2);
		}

		if (whereAllNonClefNull.length() > 0) {
			whereAllNonClefNull2.append(")'");
			whereAllNonClefNull.append(whereAllNonClefNull2);
		}

	}

	/**
	 * Quand on a des variables de groupe, renvoi les variable non groupe de la
	 * table qui vont nous permettre de determiner les identifiants
	 * 
	 * @return
	 */
	public String getGroupIdentifier() {
		Set<String> s = new HashSet<>();

		for (VariableMapping variable : this.getEnsembleVariableClef()) {

			if (!variable.isGroupe()) {
				if (!variable.getNomVariable().equals(ColumnEnum.ID_SOURCE.getColumnName())
						&& !variable.getNomVariable().equals(getPrimaryKey())) {
					if (variable.getNomVariable().startsWith("id_")) {
						s.add(variable.toString());
					} else {
						if (!variable.getEnsembleIdentifiantsRubriques().isEmpty()) {

							s.addAll(variable.getEnsembleIdentifiantsRubriques());
						}
					}

				}
			}
		}

		for (VariableMapping variable : this.ensembleVariableNonClef) {
			if (!variable.isGroupe()) {
				if (!variable.getNomVariable().equals(ColumnEnum.ID_SOURCE.getColumnName())
						&& !variable.getNomVariable().equals(getPrimaryKey())) {
					if (variable.getNomVariable().startsWith("id_")) {
						s.add(variable.toString());
					} else {
						if (!variable.getEnsembleIdentifiantsRubriques().isEmpty()) {
							s.addAll(variable.getEnsembleIdentifiantsRubriques());
						}
					}

				}
			}

		}

		return s.isEmpty() ? "" : "," + Format.untokenize(s, ",");
	}

	public void construireEnsembleVariablesTypes() {
		for (VariableMapping variable : this.ensembleVariableMapping) {
			if (variable.getExpressionRegle() instanceof RegleMappingClePrimaire) {
				this.ensembleVariableClef.add(variable);
				this.ensembleVariableClefString.add(variable.toString());
				this.ensembleRegleMappingClefPrimaire.add((RegleMappingClePrimaire) variable.getExpressionRegle());
			} else {
				this.ensembleVariableNonClef.add(variable);
			}
		}
	}

	/**
	 * @return the ensembleVariableNonClef
	 */
	public SortedSet<VariableMapping> getEnsembleVariableNonClef() {
		return this.ensembleVariableNonClef;
	}

	/**
	 * @return the ensembleVariableClef
	 */
	public SortedSet<VariableMapping> getEnsembleVariableClef() {
		return this.ensembleVariableClef;
	}

	/**
	 * @return the ensembleVariableClef
	 */
	public SortedSet<String> getEnsembleVariableClefString() {
		return this.ensembleVariableClefString;
	}

	/**
	 * @return the ensembleVariableClef
	 */
	public Set<RegleMappingClePrimaire> getEnsembleRegleMappingClefPrimaire() {
		return this.ensembleRegleMappingClefPrimaire;
	}

	/**
	 * @return the isGroupe
	 */
	public boolean isGroupe() {
		return this.isGroupe;
	}

	/**
	 * @param isGroupe the isGroupe to set
	 */
	public void setGroupe(boolean someIsGroupe) {
		this.isGroupe = this.isGroupe || someIsGroupe;
	}

	public String getPrimaryKey() {
		return "id_" + ManipString
				.substringBeforeLast(ManipString
						.substringAfterFirst(ManipString.substringAfterFirst(this.getNomTableCourt(), "_"), "_"), "_")
				.toLowerCase();
	}

}
