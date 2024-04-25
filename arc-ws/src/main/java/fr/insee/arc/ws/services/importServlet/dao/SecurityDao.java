package fr.insee.arc.ws.services.importServlet.dao;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.insee.arc.core.dataobjects.ArcPreparedStatementBuilder;
import fr.insee.arc.utils.dao.UtilitaireDao;
import fr.insee.arc.utils.exception.ArcException;
import fr.insee.arc.utils.exception.ArcExceptionMessage;
import fr.insee.arc.utils.structure.GenericBean;
import fr.insee.arc.utils.utils.LoggerHelper;
import fr.insee.arc.ws.services.importServlet.bo.RemoteHost;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SecurityDao {

	private static final Logger LOGGER = LogManager.getLogger(SecurityDao.class);

	private SecurityDao() {
		throw new IllegalStateException("SecurityDao class");
	}

	public static String validateEnvironnement(String unsafe) throws ArcException
	{
		ArcPreparedStatementBuilder query = new ArcPreparedStatementBuilder();
		query.append("SELECT "+query.quoteText(unsafe)+" as bas_name FROM arc.ext_etat_jeuderegle where isenv and lower(replace(id,'.','_')) = "+query.quoteText(unsafe.toLowerCase()));
		String result = UtilitaireDao.get(0).getString(null, query);
		
		return validateOfThrow(result, unsafe);
	}
	
	private static String validateOfThrow(String result, String unsafe) throws ArcException {
		if (result==null)
		{
			throw new ArcException(ArcExceptionMessage.WS_INVALID_PARAMETER, unsafe);
		}
		return result;
	}

	public static String validateClientIdentifier(String unsafe) throws ArcException
	{
		ArcPreparedStatementBuilder query = new ArcPreparedStatementBuilder();
		query.append("SELECT "+query.quoteText(unsafe)+" as client FROM arc.ihm_client where lower(id_application) = "+query.quoteText(unsafe.toLowerCase()));
		String result = UtilitaireDao.get(0).getString(null, query);
		return validateOfThrow(result, unsafe);
	}
	
	/**
	 * Manage the security accesses and traces for the data retrieval webservice
	 * returns true if security acess is ok
	 * 
	 * @param request
	 * @param response
	 * @param dsnRequest
	 * @return
	 * @throws ArcException
	 */
	public static void securityAccessAndTracing(String familyName, String clientRealName, RemoteHost remoteHost)
			throws ArcException {

		// get the family name and client name

		ArcPreparedStatementBuilder query;
		// check if security is enable
		query = new ArcPreparedStatementBuilder();
		query.append("SELECT count(*) ");
		query.append("FROM arc.ihm_webservice_whitelist ");
		query.append("WHERE id_famille=" + query.quoteText(familyName) + " ");
		query.append("AND id_application=" + query.quoteText(clientRealName) + " ");

		if (UtilitaireDao.get(0).getInt(null, query) == 0) {
			LoggerHelper.warn(LOGGER, "Security is not enabled for (" + familyName + "," + clientRealName + ")");
			return;
		}

		// check the host
		String hostName = remoteHost.getName();

		query = new ArcPreparedStatementBuilder();
		query.append("SELECT is_secured ");
		query.append("FROM arc.ihm_webservice_whitelist ");
		query.append("WHERE id_famille=" + query.quoteText(familyName) + " ");
		query.append("AND id_application=" + query.quoteText(clientRealName) + " ");
		query.append("AND " + query.quoteText(hostName) + " like host_allowed ");

		Map<String, List<String>> result = new HashMap<>();
		try {
			result = new GenericBean(UtilitaireDao.get(0).executeRequest(null, query)).mapContent();
		} catch (ArcException e1) {
			LoggerHelper.error(LOGGER, "Error in querying host allowed");
			throw new ArcException(ArcExceptionMessage.HOST_NOT_RESOLVED);
		}

		if (result.isEmpty() || result.get("is_secured").isEmpty()) {
			LoggerHelper.error(LOGGER, "The host " + hostName + " has not been allowed to retrieved data of ("
					+ familyName + "," + clientRealName + "). Check the family norm interface to declare it.");

			throw new ArcException(ArcExceptionMessage.HOST_NOT_RESOLVED);
		}

		// check security and log query if security required
		boolean hostDeclaredAsSecured = !StringUtils.isBlank(result.get("is_secured").get(0));
		boolean requestSecured = remoteHost.isSecure();

		// if the request is not secured and the host had been declared as secured,
		// return
		// forbidden
		if (hostDeclaredAsSecured && !requestSecured) {
			LoggerHelper.error(LOGGER, hostName + " connexion is not secured. Abort.");
			throw new ArcException(ArcExceptionMessage.HOST_NOT_RESOLVED);
		}

		// log the access
		query = new ArcPreparedStatementBuilder();
		query.append(
				"DELETE FROM arc.ihm_webservice_log  where event_timestamp < current_timestamp - INTERVAL '1 YEAR';");
		query.append("INSERT INTO arc.ihm_webservice_log (id_famille, id_application, host_allowed, event_timestamp) ");
		query.append("SELECT " + query.quoteText(familyName) + ", " + query.quoteText(clientRealName) + ", "
				+ query.quoteText(hostName) + ", current_timestamp;");

		try {
			UtilitaireDao.get(0).executeRequest(null, query);
		} catch (ArcException e) {
			LoggerHelper.error(LOGGER, "Error in querying to register the connection entry");
			throw new ArcException(ArcExceptionMessage.HOST_NOT_RESOLVED);
		}

	}

	public static void sendForbidden(HttpServletRequest request, HttpServletResponse response) {
		try {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
		} catch (IOException e) {
			LoggerHelper.error(LOGGER, "Error in sending forbidden to host " + request.getRemoteHost());
		}
	}

}
