package com.ibm.rational.services.rtc.advisor.domain;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("validator")
public class Validator {
   	@XStreamAsAttribute
	private String wiType;
   	@XStreamAsAttribute
   	private String targetState;
   	private Condition condition;  	
	@XStreamImplicit(itemFieldName="attributeRequired")
	private List<String> attributeRequired;
	private String reason;
	
	public String getWiType() {
		return wiType;
	}
	public void setWiType(String wiType) {
		this.wiType = wiType;
	}
	public String getTargetState() {
		return targetState;
	}
	public void setTargetState(String targetState) {
		this.targetState = targetState;
	}
	public Condition getCondition() {
		return condition;
	}
	public void setCondition(Condition condition) {
		this.condition = condition;
	}
	public List<String> getAttributeRequired() {
		return attributeRequired;
	}
	public void setAttributeRequired(List<String> attributeRequired) {
		this.attributeRequired = attributeRequired;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
}
