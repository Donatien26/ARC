package fr.insee.arc.core.service.engine.normage;

import java.util.ArrayList;

import fr.insee.arc.utils.utils.ManipString;

public class NormageEngineGlobal {

	
	/**
	 * A partir du blocCreate, determine récursivement les enfant d'une rubrique
	 * "m_<***>"
	 * 
	 * @param r
	 * @param blocCreate
	 * @param mRubrique
	 */

	private static void getChildrenTree(ArrayList<String> r, String blocCreate, String mRubrique) {
		ArrayList<String> s = getChildren(blocCreate, mRubrique);

		if (s.isEmpty()) {
			return;
		} else {
			r.addAll(s);

			for (String rub : s) {
				getChildrenTree(r, blocCreate, rub);
			}

		}

	}

	/**
	 * A partir du blocCreate, determine les enfants d'une rubrique "m_<***>"
	 * 
	 * @param blocCreate
	 * @param mRubrique
	 * @return
	 */
	protected static ArrayList<String> getChildren(String blocCreate, String mRubrique) {
		ArrayList<String> r = new ArrayList<>();

		if (mRubrique == null) {
			return r;
		}

		String lines[] = blocCreate.split("\n");

		for (int i = 0; i < lines.length; i++) {

			if (!testRubriqueInCreate(lines[i], mRubrique)
					&& testRubriqueInCreate(lines[i], "i_" + getCoreVariableName(mRubrique))) {
				r.add("m_" + ManipString.substringBeforeFirst(ManipString.substringAfterFirst(lines[i], " as m_"), " ")
						.trim());
			}

		}
		return r;

	}

	/**
	 * variable in arc are in format i_CoreVariableName or v_CoreVariableName this
	 * function exract the core variable name
	 * 
	 * @param fullVariableName
	 * @return
	 */
	protected static String getCoreVariableName(String fullVariableName) {
		return fullVariableName.substring(2);
	}

	/**
	 * la rubrique identifiante <m_...> trouvée dans le bloc
	 * 
	 * @param blocCreate
	 * @return
	 */
	protected static String getM(String bloc) {
		return "m_" + ManipString.substringBeforeFirst(ManipString.substringAfterFirst(bloc, " as m_"), " ");
	}

	/**
	 * le pere du bloc
	 * 
	 * @param bloc
	 * @return
	 */
	protected static String getFather(String bloc) {
		return getFather(bloc, getM(bloc));
	}

	/**
	 * la table du bloc
	 * 
	 * @param bloc
	 * @return
	 */
	protected static String getTable(String bloc) {

		return ManipString.substringBeforeFirst(ManipString.substringAfterFirst(bloc, "create temporary table "),
				" as ");

	}

	/**
	 * les autres colonnes du blocs
	 * 
	 * @param bloc
	 * @return
	 */
	protected static String getOthers(String bloc) {
		String r = ManipString
				.substringBeforeFirst(ManipString.substringAfterFirst(bloc, " as " + getFather(bloc) + " "), " from (");
		if (r.startsWith(",")) {
			r = r.substring(1);
		}
		return r;
	}

	/**
	 * renvoie la ligne a laquelle la rubrique appartient
	 * 
	 * @param blocCreate
	 * @param rubrique
	 * @return
	 */
	protected static String getLine(String blocCreate, String rubrique) {
		String lines[] = blocCreate.split("\n");

		for (int i = 0; i < lines.length; i++) {

			if (testRubriqueInCreate(lines[i], rubrique)) {
				return lines[i];
			}
		}
		return null;
	}

	/**
	 * l'identifiant de la ligne a laquelle la rubrique appartient
	 * 
	 * @param blocCreate
	 * @param rubrique
	 * @return
	 */
	protected static String getM(String blocCreate, String rubrique) {
		String lines[] = blocCreate.split("\n");

		for (int i = 0; i < lines.length; i++) {

			if (testRubriqueInCreate(lines[i], rubrique)) {
				return getM(lines[i]);
			}
		}
		return null;
	}

	protected static String mToI(String rubrique) {
		if (rubrique.startsWith("m_")) {
			return "i_" + getCoreVariableName(rubrique);
		}
		return rubrique;
	}

	protected static String iToM(String rubrique) {
		if (rubrique.startsWith("i_")) {
			return "m_" + getCoreVariableName(rubrique);
		}
		return rubrique;
	}

	protected static String anyToM(String rubrique) {

		if (rubrique.startsWith("m_")) {
			return rubrique;
		}

		if (rubrique.startsWith("i_") || rubrique.startsWith("v_")) {
			return "m_" + getCoreVariableName(rubrique);
		}

		return "m_" + rubrique;
	}

	/**
	 * le nom de la table de la ligne a laquelle la rubrique appartient
	 * 
	 * @param blocCreate
	 * @param rubrique
	 * @return
	 */
	protected static String getTable(String blocCreate, String rubrique) {
		String lines[] = blocCreate.split("\n");

		for (int i = 0; i < lines.length; i++) {

			if (testRubriqueInCreate(lines[i], rubrique)) {
				return getTable(lines[i]);
			}
		}
		return null;
	}

	/**
	 * test si la rubrique est dans le bloc fourni
	 * 
	 * @param bloc
	 * @param rubrique
	 * @return
	 */
	protected static boolean testRubriqueInCreate(String bloc, String rubrique) {
		return ManipString.substringBeforeFirst(bloc, " from (").contains(" as " + rubrique + " ");
	}

	/**
	 * pppc = plus petit pere en commun renvoir le pppc
	 * 
	 * @param blocCreate
	 * @param rubriqueM
	 * @param rubriqueNmclM
	 * @return
	 */
	protected static String pppc(String blocCreate, String rubriqueM, String rubriqueNmclM) {

		ArrayList<String> rubriqueParents = getParentsTree(blocCreate, rubriqueM, true);
		ArrayList<String> rubriqueNmclParents = getParentsTree(blocCreate, rubriqueNmclM, true);

		for (int k = 0; k < rubriqueNmclParents.size(); k++) {
			for (int l = 0; l < rubriqueParents.size(); l++) {
				if (rubriqueParents.get(l).equals(rubriqueNmclParents.get(k))) {
					return rubriqueNmclParents.get(k);
				}

			}
		}

		return null;

	}

	/**
	 * determine le pere d'une rubrique "m_<***>" dans un bloc
	 * 
	 * @param bloc
	 * @param rubrique
	 * @return
	 */
	private static String getFather(String bloc, String rubrique) {
		return ManipString
				.substringBeforeFirst(
						ManipString.substringAfterFirst(
								ManipString.substringBeforeFirst(
										ManipString.substringAfterFirst(bloc, " as " + rubrique + " "), " from ("),
								" as "),
						" ,")
				.trim();
	}

	/**
	 * determine le pere d'une rubrique "m_<***>" dans un bloc
	 * 
	 * @param bloc
	 * @param rubrique
	 * @return
	 */
	protected static String getFatherM(String bloc, String rubrique) {
		String r = getFather(bloc, rubrique);
		if (!r.equals("")) {
			r = "m_" + getCoreVariableName(r);
		} else {
			r = rubrique;
		}
		return r;

	}

	/**
	 * A partir du blocCreate, determine l'arbre vers une rubrique "m_<***>"
	 * 
	 * @param blocCreate
	 * @param mRubrique
	 * @return
	 */
	protected static ArrayList<String> getParentsTree(String blocCreate, String mRubrique, boolean... keep) {
		ArrayList<String> r = new ArrayList<>();
		r.add(mRubrique);
		String s;

		while (!(s = getFatherM(blocCreate, r.get(r.size() - 1))).equals(r.get(r.size() - 1))) {
			r.add(s);
		}

		if (!(keep.length > 0 && keep[0])) {
			r.remove(0);
		}

		return r;

	}

	/**
	 * A partir du blocCreate, determine récursivement les enfant d'une rubrique
	 * "m_<***>"
	 * 
	 * @param blocCreate
	 * @param mRubrique
	 * @return
	 */
	protected static ArrayList<String> getChildrenTree(String blocCreate, String mRubrique) {
		ArrayList<String> s = new ArrayList<>();
		getChildrenTree(s, blocCreate, mRubrique);
		return s;
	}
	
}
