package br.gov.caixa.rtc.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.log4j.Logger;

/**
 * Classe utilizada para facilitar a execucao de recursos Http.
 * 
 * @author David da Silva Teles
 *
 */
@SuppressWarnings("deprecation")
public class HttpUtils {

	static String AUTHREQUIRED = "X-com-ibm-team-repository-web-auth-msg";
	private static Logger log = Logger.getLogger("log");

	public static void setupLazySSLSupport(HttpClient httpClient) {
		ClientConnectionManager connManager = httpClient.getConnectionManager();
		SchemeRegistry schemeRegistry = connManager.getSchemeRegistry();
		schemeRegistry.unregister("https");
		/** Create a trust manager that does not validate certificate chains */
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				/** Ignore Method Call */
			}

			public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				/** Ignore Method Call */
			}

			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		} };

		SSLContext sc = null;
		try {
			sc = SSLContext.getInstance("SSL"); //$NON-NLS-1$
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (NoSuchAlgorithmException e) {
			/* Fail Silently */
		} catch (KeyManagementException e) {
			/* Fail Silently */
		}

		SSLSocketFactory sf = new SSLSocketFactory(sc);
		sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		Scheme https = new Scheme("https", sf, 443);

		schemeRegistry.register(https);
	}

	public static void printResponseHeaders(HttpResponse response) {
		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			log.debug("\t- " + headers[i].getName() + ": " + headers[i].getValue());
		}
	}

	public static void printResponseBody(HttpResponse response) {
		HttpEntity entity = response.getEntity();
		if (entity == null)
			return;
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(entity.getContent()));
			String line = reader.readLine();
			while (line != null) {
				System.out.println(line);
				line = reader.readLine();
			}
			reader.close();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static String getResultBody(HttpResponse response) {
		HttpEntity entity = response.getEntity();
		if (entity == null)
			return null;

		StringBuilder builder = new StringBuilder();

		try {
			BufferedHttpEntity bufferedEntity = new BufferedHttpEntity(entity);
			BufferedReader reader = new BufferedReader(new InputStreamReader(bufferedEntity.getContent()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
//			reader.close();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return builder.toString();
	}

	public static void printRequest(HttpGet request) {
		log.debug("\t- Method: " + request.getMethod());
		log.debug("\t- URL: " + request.getURI());
		log.debug("\t- Headers: ");
		Header[] headers = request.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			log.debug("\t\t- " + headers[i].getName() + ": " + headers[i].getValue());
		}
	}

	public static DocumentBuilder getDocumentParser() {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			dbf.setValidating(false);
			return dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new Error(e);
		}
	}

	/**
	 * Utilizado para codificar a url pois pode conter par�metros invalidos como
	 * espa�os e aspas
	 * 
	 * @param url a ser encodada
	 * @return {@link String} encodada no formato http
	 * @throws EncoderException
	 */
	public static String encodeURL(String url) throws EncoderException {
		URLCodec urlCodec = new URLCodec();
		String encoded = urlCodec.encode(url);
		return encoded;
	}

	/**
	 * Decodifica a URL informada.
	 * 
	 * @param url
	 * @return {@link String} com a url decodificada
	 * @throws DecoderException
	 */
	public static String decodeURL(String url) throws DecoderException {
		URLCodec urlCodec = new URLCodec();
		String decoded = urlCodec.decode(url);
		return decoded;
	}
}