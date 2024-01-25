package fr.insee.arc.web.gui.pilotage.controller;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import fr.insee.arc.utils.exception.ArcException;
import fr.insee.arc.web.gui.pilotage.service.ServiceViewFichierBAS;

@Controller
public class ControllerFichierBAS extends ServiceViewFichierBAS {

	@RequestMapping("/secure/selectFichierBAS")
	public String selectFichierBASAction(Model model) {
		return selectFichierBAS(model);
	}

	@RequestMapping("/secure/sortFichierBAS")
	public String sortFichierBASAction(Model model) {
		return sortFichierBAS(model);
	}

	@RequestMapping("/secure/downloadFichierBAS")
	public void downloadFichierBASAction(HttpServletResponse response) {
		downloadFichierBAS(response);
	}

	/**
	 * Marquage de fichier pour le rejouer lors de la prochaine initialisation
	 *
	 * @return
	 */
	@RequestMapping("/secure/toRestoreBAS")
	public String toRestoreBASAction(Model model) {		
		return toRestoreBAS(model);
	}

	/**
	 * Marquage des archives à rejouer lors de la prochaine initialisation
	 *
	 * @return
	 */
	@RequestMapping("/secure/toRestoreArchiveBAS")
	public String toRestoreArchiveBASAction(Model model) {
		return toRestoreArchiveBAS(model);
	}

	@RequestMapping("/secure/downloadBdBAS")
	public void downloadBdBASAction(HttpServletResponse response) throws ArcException {
		downloadBdBAS(response);
	}

	@RequestMapping("/secure/downloadEnveloppeBAS")
	public String downloadEnveloppeBASAction(HttpServletResponse response) {
		return downloadEnveloppeBAS(response);
	}

	/**
	 * Marquage de fichier pour suppression lors de la prochaine initialisation
	 *
	 * @return
	 */
	@RequestMapping("/secure/toDeleteBAS")
	public String toDeleteBASAction(Model model) {
		return toDeleteBAS(model);
	}

	/**
	 * Suppression du marquage de fichier pour suppression lors de la prochaine
	 * initialisation
	 *
	 * @return
	 */
	@RequestMapping("/secure/undoActionBAS")
	public String undoActionBASAction(Model model) {
		return undoActionBAS(model);
	}

}
