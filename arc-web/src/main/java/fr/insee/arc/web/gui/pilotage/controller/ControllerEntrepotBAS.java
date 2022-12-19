package fr.insee.arc.web.gui.pilotage.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import fr.insee.arc.web.gui.pilotage.service.ServiceEntrepotBAS;

@Controller
public class ControllerEntrepotBAS extends ServiceEntrepotBAS {


	/**
	 * Fabrication d'une table temporaire avec comme contenu le nom des archives
	 * d'un entrepot donné puis Ouverture d'un VObject sur cette table
	 *
	 * @return
	 */
	@RequestMapping("/visualiserEntrepotBAS")
	public String visualiserEntrepotBASAction(Model model) {		
		return visualiserEntrepotBAS(model);
	}
	
	
	
}