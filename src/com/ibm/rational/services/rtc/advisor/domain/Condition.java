package com.ibm.rational.services.rtc.advisor.domain;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamImplicit;

public class Condition {
	private String attribute;
	@XStreamImplicit(itemFieldName="value")	
	private List<String> value;
	
	public String getAttribute() {
		return attribute;
	}
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
	public List<String> getValue() {
		return value;
	}
	public void setValue(List<String> value) {
		this.value = value;
	}
}
