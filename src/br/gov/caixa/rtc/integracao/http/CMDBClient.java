package br.gov.caixa.rtc.integracao.http;



import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import br.gov.caixa.rtc.exception.APIException;
import br.gov.caixa.rtc.exception.ApplicationException;
import br.gov.caixa.rtc.util.HttpUtils;
import br.gov.caixa.rtc.util.PropertiesReader;

import com.ibm.rational.services.rtc.advisor.messages.MessagesIntegracao;


public class CMDBClient {

	private static CloseableHttpClient httpClient;
	private HttpResponse loginFormResponse;
	private HttpResponse logoutFormResponse;
	private Logger log = Logger.getLogger("log");
	private String token;
	
	public static final ContentType APPLICATION_JSON = ContentType.create("application/json", Consts.UTF_8);
	public static final ContentType MULTIPART_DATA_FORM = ContentType.create("multipart/form-data", Consts.UTF_8);

	public CMDBClient() {
		configHttpClient();
	}
	
	/**
	 * Configuracao basica do httpClient
	 */
	private void configHttpClient() {
	   httpClient = HttpClientBuilder.create().build();
		
	}

	public String login() throws ApplicationException  {
		HttpPost formPost = new HttpPost(PropertiesReader.getConfig("CMDB_SERVER") + "jwt/login");
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("username", PropertiesReader.getConfig("CMDB_USER")));
		nvps.add(new BasicNameValuePair("password", PropertiesReader.getConfig("CMDB_PWD")));
		formPost.setEntity(new UrlEncodedFormEntity(nvps, StandardCharsets.UTF_8));

		
		try {
		    loginFormResponse = httpClient.execute(formPost);
		} catch (ClientProtocolException e) {
		    e.printStackTrace();
		    throw new ApplicationException("Erro ao tentar logar no ITSM - Client Protocol Exception.", e);
		} catch (IOException e1) {
		    e1.printStackTrace();
		    throw new ApplicationException("Erro ao tentar logar no ITSM - IOException.", e1);
		}

		int code = loginFormResponse.getStatusLine().getStatusCode();

		if (code != 200) {
			InvalidCredentialsException ex = new InvalidCredentialsException("Authentication failed");
			log.fatal(MessagesIntegracao.MSG_FALHA_LOGIN.getMessage());
			throw new APIException(ex);
		} else {
			log.info(MessagesIntegracao.MSG_SUCESSO_LOGIN.getMessage() + PropertiesReader.getConfig("CMDB_USER"));
			
			try {
			    token = HttpUtils.getResultBody(loginFormResponse);
			} catch (ParseException e) {
			    throw new ApplicationException("Erro ao recuperar token doITSM - Parse Exception.", e);
			} 
		}
		return token;

	}

	public HttpResponse put(String url, Map<String, String> headers, String body) throws ApplicationException  {

	    HttpPut put = new HttpPut(url);
		

		if (headers != null) {
		    for (String headerName : headers.keySet()) {
			put.addHeader(headerName, headers.get(headerName));
			}
		}
		
		if (body != null) {
		   put.setEntity(new StringEntity(body, StandardCharsets.UTF_8.toString()));
		}
		
		
		// Manda executar o metodo put e recebe a resposta
		
		HttpResponse response = null;
		try {
		    response = httpClient.execute(put);
		} catch (ClientProtocolException e) {
		    e.printStackTrace();
		    throw new ApplicationException("Erro ao criar nota de trabalho no ITSM, SEM anexo - Client Protocol Exception.", e);
		} catch (IOException e1) {
		    e1.printStackTrace();
		    throw new ApplicationException("Erro ao criar nota de trabalho no ITSM, SEM anexo - IOException.", e1);
		}
		
		
		// Imprime o status da requisicao GET
		log.info(">> HTTP Status code:" + response.getStatusLine());

		// Se a resposta for OK
		if (response.getStatusLine().getStatusCode() == 204) {
			log.info(MessagesIntegracao.MSG_GET_OK.getMessage());

		} else {
		    APIException ex = new APIException("Erro ao criar nota de trabalho no ITSM, SEM anexo." 
				+ MessagesIntegracao.MSG_PUT_ERROR.getMessage()+response.getStatusLine()+ " - " + HttpUtils.getResultBody(response));
			log.fatal(MessagesIntegracao.MSG_PUT_ERROR.getMessage() +" " + HttpUtils.getResultBody(response)+ " " + response.getStatusLine(), ex);
			
			try {
			    log.fatal(HttpUtils.getResultBody(response));
			} catch (ParseException e) {
			    throw new ApplicationException("Erro ao recuperar mensagem de erro da transação.(CMDBClient.put)", e);
			}
			throw ex;
			
		}
		
		return response;

	}
	
	public HttpResponse putComAnexo(String url, Map<String, String> headers, String entry, List<File> arquivos) throws ApplicationException {
	    HttpEntity entity = MultipartEntityBuilder.create().addTextBody("entry", entry, APPLICATION_JSON).
	    addBinaryBody("attach-z2AF_Act_Attachment_1", arquivos.get(0)).build();
	    
	    HttpPut put = new HttpPut(url);
		if (headers != null) {
		    for (String headerName : headers.keySet()) {
			put.addHeader(headerName, headers.get(headerName));
			}
		}
		
		if (entry != null) {
		   put.setEntity(entity);
		}
		
		// Manda executar o metodo put e recebe a resposta
		HttpResponse response = null;
		try {
		    response = httpClient.execute(put);
		} catch (ClientProtocolException e) {
		    e.printStackTrace();
		    throw new ApplicationException("Erro ao criar nota de trabalho no ITSM, COM anexo - Client Protocol Exception.", e);
		} catch (IOException e1) {
		    e1.printStackTrace();
		    throw new ApplicationException("Erro ao criar nota de trabalho no ITSM, COM anexo - IOException.", e1);
		}
		
		// Imprime o status da requisicao GET
		log.info(">> HTTP Status code:" + response.getStatusLine());

		System.out.println(">> HTTP Status code:" + response.getStatusLine());
		// Se a resposta for OK
		if (response.getStatusLine().getStatusCode() == 204) {
		log.info(MessagesIntegracao.MSG_GET_OK.getMessage());

		} else {
			APIException ex = new APIException("Erro ao criar nota de trabalho no ITSM, SEM anexo." 
								+ MessagesIntegracao.MSG_PUT_ERROR.getMessage()
								+ " " + HttpUtils.getResultBody(response)+ " "
								+ " " + response.getStatusLine());
			log.fatal(MessagesIntegracao.MSG_PUT_ERROR.getMessage() + " " + HttpUtils.getResultBody(response)+ " " + response.getStatusLine(), ex);
			try {
			    log.fatal(HttpUtils.getResultBody(response));
			} catch (ParseException e) {
			    logout(token);
			    throw new ApplicationException("Erro ao recuperar mensagem de erro da transação.(CMDBClient.putComAnexo)", e);
			}
			throw ex;
		}
		return response;
	}
	
	public HttpResponse get(String url, Map<String, String> headers) throws ApplicationException  {
		log.debug(MessagesIntegracao.MSG_GET.getMessage());
		log.info(MessagesIntegracao.MSG_GET_URL.getMessage() + url);

		// Configura o metodo GET para aceitar conteudo do tipo especificado por
		// parametro

		HttpGet get = new HttpGet(url);

		if (headers != null) {

			log.debug(MessagesIntegracao.MSG_CONFIG_HEADER.getMessage());

			for (String headerName : headers.keySet()) {
				get.addHeader(headerName, headers.get(headerName));

				log.debug("\t" + headerName + ":  " + headers.get(headerName));
			}

		}

		// Manda executar o metodo get e recebe a resposta
		HttpResponse response = null;
		try {
		    response = httpClient.execute(get);
		} catch (ClientProtocolException e) {
		    e.printStackTrace();
		    throw new ApplicationException("Erro ao recuperar o incidente no ITSM - Client Protocol Exception", e);
		} catch (IOException e1) {
		    e1.printStackTrace();
		    throw new ApplicationException("Erro ao recuperar o incidente no ITSM - IOException", e1);
		}

		// Imprime o status da requisicao GET
		log.info(">> HTTP Status code:" + response.getStatusLine());

		// Se a resposta for OK
		if (response.getStatusLine().getStatusCode() == 200) {
			log.info(MessagesIntegracao.MSG_GET_OK.getMessage());

		} else {
			APIException ex = new APIException(MessagesIntegracao.MSG_500.getMessage() 
						+" " + HttpUtils.getResultBody(response)
						+ " " + response.getStatusLine());
			log.fatal(MessagesIntegracao.MSG_500.getMessage() + " " + HttpUtils.getResultBody(response)+ " " + response.getStatusLine(), ex);
			try {
			    log.fatal(HttpUtils.getResultBody(response));
			} catch (ParseException e) {
			    throw new ApplicationException("Erro ao recuperar mensagem de erro da transação.(CMDBClient.get)", e);
			} 
			
		}
		return response;

	}

	public static String getBody(String url) throws IllegalStateException, IOException {

		// Configura o metodo GET para aceitar conteudo do tipo especificado por
		// parametro

		HttpGet get = new HttpGet(url);

		// Manda executar o metodo get e recebe a resposta
		HttpResponse response = httpClient.execute(get);
		String resultadoBody = HttpUtils.getResultBody(response);

		return resultadoBody;
	}

	public HttpClient getHttpClient() {
		return httpClient;
	}

	public HttpResponse logout(String token) throws ApplicationException  {
		log.info(MessagesIntegracao.MSG_REALIZANDO_LOGOUT.getMessage());

		HttpPost formPost = new HttpPost(PropertiesReader.getConfig("CMDB_SERVER") + "jwt/logout");
		formPost.addHeader("Authorization", "AR-JWT " + token);

		try {
		    logoutFormResponse = httpClient.execute(formPost);
		} catch (ClientProtocolException e) {
		    e.printStackTrace();
		    throw new ApplicationException("Erro ao fazer o logout no ITSM - Client Protocol Exception", e);
		} catch (IOException e1) {
		    e1.printStackTrace();
		    throw new ApplicationException("Erro ao fazer o logout no ITSM - IOException", e1);
		    
		}

		int code = logoutFormResponse.getStatusLine().getStatusCode();

		if (code != 204) {
			InvalidCredentialsException ex = new InvalidCredentialsException("Logout failed");
			log.fatal(MessagesIntegracao.MSG_FALHA_LOGOUT.getMessage());
			throw new APIException(ex);
		} else {
			log.info(MessagesIntegracao.MSG_SUCESSO_LOGOUT.getMessage() + PropertiesReader.getConfig("CMDB_USER"));
			try {
			    token = HttpUtils.getResultBody(logoutFormResponse);
			} catch (ParseException e) {
			    throw new ApplicationException("Erro ao recuperar mensagem de erro da transação.(CMDBClient.logout)", e);
			} 
		}
		return logoutFormResponse;

	}

}