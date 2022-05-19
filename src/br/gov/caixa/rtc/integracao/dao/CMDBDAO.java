package br.gov.caixa.rtc.integracao.dao;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.log4j.Logger;

import br.gov.caixa.rtc.exception.ApplicationException;
import br.gov.caixa.rtc.integracao.http.CMDBClient;
import br.gov.caixa.rtc.util.HttpUtils;
import br.gov.caixa.rtc.util.PropertiesReader;

import com.ibm.rational.services.rtc.advisor.messages.MessagesIntegracao;


public class CMDBDAO {

	final Logger log = Logger.getLogger(CMDBDAO.class);
	static CMDBClient client;

	private String query = null;
	private String resultadoBody = null;
	private String token = null;
	
	public static final ContentType APPLICATION_JSON = ContentType.create("application/json", Consts.UTF_8);

	public CMDBDAO() throws ApplicationException {
	    client = new CMDBClient();
	    token = client.login();
	}
	

	public String getIncidente(String idInternoIncidente) throws UnsupportedEncodingException, ApplicationException{
	    	log.info(MessagesIntegracao.MSG_START_REQUEST_PROJECT.getMessage());
				
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Authorization", "AR-JWT " + token);
		
		query = PropertiesReader.getConfig("CMDB_QUERY_INCIDENTE") + idInternoIncidente;
		query = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
		String url = PropertiesReader.getConfig("CMDB_SERVER") + query;
		
		HttpResponse response = null;
		response = client.get(url, headers);

		if (response.getStatusLine().getStatusCode()== 404) {
		    return null;
		}
		resultadoBody = HttpUtils.getResultBody(response);
		return resultadoBody;

		
	} 
	
	public void atualizaIncidente( String idInternoIncidente, String body) throws IllegalStateException, IOException, ApplicationException{
	    	log.info(MessagesIntegracao.MSG_START_REQUEST_PROJECT.getMessage());
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Authorization", "AR-JWT " + token);
		headers.put("Content-Type", "application/json");
		
		query = PropertiesReader.getConfig("CMDB_QUERY_INCIDENTE")+ idInternoIncidente;
		query = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
		String url = PropertiesReader.getConfig("CMDB_SERVER") + query;
		
		HttpResponse response = null;
		response = client.put(url, headers, body);
		resultadoBody = HttpUtils.getResultBody(response);
	    
	}

	public void atualizaIncidenteComAnexo(String requestIdIncidente, String body, List<File> arquivos) throws IllegalStateException, IOException, ApplicationException {
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Authorization", "AR-JWT " + token);
		
		query = PropertiesReader.getConfig("CMDB_QUERY_INCIDENTE")+ requestIdIncidente;
		query = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
		String url = PropertiesReader.getConfig("CMDB_SERVER") + query;
				
		HttpResponse response = null;
		response = client.putComAnexo(url, headers, body, arquivos);
		resultadoBody = HttpUtils.getResultBody(response);
	    
	}
	
	public HttpResponse logout() throws ApplicationException {
	    HttpResponse logoutResponse = null;
	    logoutResponse = client.logout(token);
	    return logoutResponse;
	}
	
}
