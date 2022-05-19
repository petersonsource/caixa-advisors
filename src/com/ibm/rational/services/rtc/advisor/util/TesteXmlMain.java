package com.ibm.rational.services.rtc.advisor.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.ibm.rational.services.rtc.advisor.domain.Condition;
import com.ibm.rational.services.rtc.advisor.domain.Validator;

public class TesteXmlMain {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		List<Validator> validators = new ArrayList<Validator>();
		
		Validator v1 = new Validator();
		v1.setWiType("task");
		Condition c1 = new Condition();
		c1.setAttribute("summary");
		c1.setValue(Arrays.asList("aaa", "bbb"));
		v1.setCondition(c1);
		v1.setAttributeRequired(Arrays.asList("dueDate", "description"));
		
		validators.add(v1);
		
		Validator v2 = new Validator();
		v2.setWiType("task");
		Condition c2 = new Condition();
		c2.setAttribute("summary");
		c2.setValue(Arrays.asList("aaa", "bbb"));
		v2.setCondition(c2);
		v2.setAttributeRequired(Arrays.asList("dueDate", "description"));
		
		validators.add(v2);
		
		XmlManager.writeXml(validators, "/home/bbraga/Desktop/validators.xml");
	}

}
