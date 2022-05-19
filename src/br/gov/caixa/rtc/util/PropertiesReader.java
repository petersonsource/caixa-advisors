package br.gov.caixa.rtc.util;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

public class PropertiesReader {

	private static ResourceBundle configBundler = ResourceBundle.getBundle("configuration");
	private static ResourceBundle message = ResourceBundle.getBundle("messages");
	public static final String RTC_QUERY_PROJECT = "RTC_QUERY_PROJECT";
	public static final String CMDB_SERVER = "CMDB_SERVER";
	public static final String CMDB_USER = "CMDB_USER";
	public static final String CMDB_PWD = "CMDB_PWD";
	public static final String CMDB_QUERY_PROJECT = "CMDB_QUERY_PROJECT";

	public static String getConfig(String key) {
		String valor = System.getProperty(key);
		if (valor == null || valor.equals("")) {
			valor = configBundler.getString(key);
		}
		return valor;
	}

	public static Integer getIntConfig(String key) {
		String valor = getConfig(key);
		Integer resp = null;
		if (valor != null) {
			resp = Integer.valueOf(valor);
		}
		return resp;
	}

	public static String getMessage(String idMessage) {
		String valor = message.getString(idMessage);
		return valor;
	}

	public static List<String> getAllConfig(String key) {

		List<String> itensTrabalho = new ArrayList<String>();

		String valor = getConfig(key);
		StringTokenizer iT = null;
		iT = new StringTokenizer(valor, ",");

		while (iT.hasMoreTokens()) {
			itensTrabalho.add(iT.nextToken());
		}

		return itensTrabalho;
	}

}
