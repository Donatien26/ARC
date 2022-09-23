package fr.insee.arc.web.action;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;

import fr.insee.arc.web.model.NoModel;

@Controller
@Scope(scopeName = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class HomeAction extends ArcAction<NoModel> {

    static final String ACTION_NAME = "home";
    private static final String RESULT_SUCCESS = "jsp/home.jsp";
    
    @RequestMapping("/")
    public String index(Model model, HttpServletRequest request) {
    	return generateDisplay(model, RESULT_SUCCESS);
    }



    @Override
    public void putAllVObjects(NoModel arcModel) {
    	// no vObject in this controller
    }

    @Override
    public String getActionName() {
    	return ACTION_NAME;
    }
    
}