package fr.insee.arc.web.gui.query.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import fr.insee.arc.web.gui.query.service.ServiceViewQuery;

@Controller
public class ControllerViewQuery extends ServiceViewQuery {


	@RequestMapping("/selectQuery")
	public String selectQueryAction(Model model) {
		return selectQuery(model);
	}

	@RequestMapping("/sortQuery")
	public String sortQueryAction(Model model) {
		return sortQuery(model);
	}

	
}
