package com.ibm.rational.services.rtc.advisor.messages;

import java.text.MessageFormat;

import br.gov.caixa.rtc.util.PropertiesReader;
/**
 * Classe responsavel por armazenar as mensagens do sistem interagindo com o arquivo de mensagem  
 * @author f624405
 *
 */
public enum MessagesIntegracao {
	MSG_START_PROCESS("br.com.caixa.rtc.ewave.msg.001"),
	MSG_FINISH_PROCESS("br.com.caixa.rtc.ewave.msg.002"),
	MSG_ERROR_PROCESS("br.com.caixa.rtc.ewave.msg.004"),
	MSG_START_AUTHENTICATION("br.com.caixa.rtc.ewave.msg.004"),
	MSG_SUCCESS_AUTHENTICATION("br.com.caixa.rtc.ewave.msg.005"),
	MSG_ERROR_AUTHENTICATION("br.com.caixa.rtc.ewave.msg.006"),
	MGS_ERRO_SERVER_CONNECT("br.com.caixa.rtc.ewave.msg.011"),

	//project
	MSG_START_REQUEST_PROJECT("br.com.caixa.rtc.ewave.msg.019"),
	MSG_FINISH_REQUEST_PROJECT("br.com.caixa.rtc.ewave.msg.020"),
	MSG_ERRO_CONSULTAR_PROJECT("br.com.caixa.rtc.ewave.msg.021"),
	
	//cmdb
	MSG_REALIZANDO_LOGIN("br.com.caixa.rtc.ewave.msg.022"),
	MSG_IMPRIMINDO_HEADER("br.com.caixa.rtc.ewave.msg.023"),
	MSG_SUCESSO_LOGIN("br.com.caixa.rtc.ewave.msg.024"), 
	MSG_FALHA_LOGIN("br.com.caixa.rtc.ewave.msg.025"),
	MSG_GET("br.com.caixa.rtc.ewave.msg.026"),
	MSG_GET_URL("br.com.caixa.rtc.ewave.msg.027"), 
	MSG_GET_ACCEPT("br.com.caixa.rtc.ewave.msg.028"),
	MSG_GET_OK("br.com.caixa.rtc.ewave.msg.029"),
	MSG_CONFIG_HEADER("br.com.caixa.rtc.ewave.msg.030"),
	MSG_500("br.com.caixa.rtc.ewave.msg.031"),
	MSG_PUT("br.com.caixa.rtc.ewave.msg.032"),
	MSG_PUT_URL("br.com.caixa.rtc.ewave.msg.033"),
	MSG_PUT_RESPONSE("br.com.caixa.rtc.ewave.msg.034"),
	MSG_PUT_OK("br.com.caixa.rtc.ewave.msg.035"),
	MSG_PUT_ERROR("br.com.caixa.rtc.ewave.msg.036"),
	MSG_REALIZANDO_LOGOUT("br.com.caixa.rtc.ewave.msg.037"),
	MSG_SUCESSO_LOGOUT("br.com.caixa.rtc.ewave.msg.038"), 
	MSG_FALHA_LOGOUT("br.com.caixa.rtc.ewave.msg.039");
	
	
	private String id;

	private MessagesIntegracao(final String id) {
		this.id = id;
	}

	public String getMessage(final Object... parameters) {
		String msgSt = PropertiesReader.getMessage(id);
		msgSt = setParameters(msgSt, parameters);
		return msgSt;
	}
	/**
	 * obtem as mensagens e caso necessario os parametros de recepcao
	 * @param msgSt
	 * @param parameters
	 * @return
	 */
	private String setParameters(String msgSt, final Object... parameters) {
		if(parameters.length>=0){
			final MessageFormat messageFormat = new MessageFormat(msgSt);
			msgSt = messageFormat.format(parameters);
		}
		return msgSt;
	}

}
