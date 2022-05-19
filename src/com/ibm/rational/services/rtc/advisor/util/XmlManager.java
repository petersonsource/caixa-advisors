
package com.ibm.rational.services.rtc.advisor.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.ibm.rational.services.rtc.advisor.domain.Validator;
import com.thoughtworks.xstream.XStream;

public class XmlManager {	
	
	public static void writeXml(Object data, String pathArquivo) throws SAXException, ParserConfigurationException, IOException {
		XStream xstream = getXStream();
		
		String xml = xstream.toXML(data);
		
		File xmlFile = new File(pathArquivo);
		try {
			xmlFile.createNewFile();
			FileOutputStream fileOutputStream = new FileOutputStream(xmlFile);
			fileOutputStream.write(xml.getBytes());
			fileOutputStream.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public static String convertToXml(Object data) throws SAXException, ParserConfigurationException, IOException {
		XStream xstream = getXStream();
		
		String xml = xstream.toXML(data);
		
		return xml;
	}	
	
	public static Object loadXml(String pathFile) {
		XStream xstream = getXStream();
		
		FileReader fileReader = null;
		try {
			fileReader = new FileReader(pathFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}	
		
		return xstream.fromXML(fileReader);
	}
	
	public static Object loadXml(InputStream inputStream) {
		XStream xstream = getXStream();
		
		return xstream.fromXML(inputStream);
	}
	
	/**
	 * Instance and configure XStream
	 * @return
	 */
	private static XStream getXStream() {
		XStream xstream = new XStream();
		//configure rules
		//xstream.addImplicitCollection(ArrayList.class, "list");
		
		//configure alias
		xstream.processAnnotations(new Class[]{Validator.class});
		xstream.setMode(XStream.NO_REFERENCES); //no references for attribute data
		
		return xstream;
	}
	
}
