/*******************************************************************************
 * Licensed Materials - Property of IBM (c) Copyright IBM Corporation 2005-2006.
 * All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights: Use, duplication or
 * disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 ******************************************************************************/
package com.ibm.rational.services.rtc.advisor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.rational.services.rtc.advisor.domain.Validator;
import com.ibm.rational.services.rtc.advisor.messages.Messages;
import com.ibm.rational.services.rtc.advisor.util.XmlManager;
import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.IAuditableCommon;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.AttributeTypes;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;

public class WorkItemCustomValidatorAdvisor implements IOperationAdvisor {

	/** FIX THAT
	 * E.g., add in <milestone-phase>:
		<operation id="com.ibm.team.workitem.operation.workItemSave">
			<preconditions>
				<precondition id="com.ibm.rational.services.rtc.advisor.WorkItemCustomValidatorAdvisor" name="Custom Validator">
					<validator wiType="mensuracao">
					    <condition>
					      <attribute>com.ibm.team.workitem.workItemType.mensuracao.tipoServico</attribute>
					      <value>com.ibm.team.workitem.common.model.ILiteral:tipoServico.literal.l19</value>
					    </condition>
					    <attributeRequired>com.ibm.team.workitem.workItemType.mensuracao.percentualRealizado</attributeRequired>	
						<reason>Campo obrigatorio em funcao do Tipo de Servico "Alteracao de Escopo"</reason>			
					</validator>
				</precondition>
			</preconditions>
		</operation>
	 */	
	
	public void run(AdvisableOperation operation, IProcessConfigurationElement advisorConfiguration, IAdvisorInfoCollector collector, IProgressMonitor monitor)	throws TeamRepositoryException {
		Object data = operation.getOperationData();
		if (data instanceof ISaveParameter) {
			ISaveParameter saveParameter = ((ISaveParameter) data);
			IAuditable auditable = saveParameter.getNewState();
			if (auditable instanceof IWorkItem) {
				IWorkItem workItem = (IWorkItem) auditable;
				
				IAuditableCommon auditableCommon = saveParameter.getSaveOperationParameter().getAuditableCommon();
				IWorkItemCommon workItemCommon = auditableCommon.getPeer(IWorkItemCommon.class);
				
				//get attributes required
				List<Validator> validatorsToApply = new ArrayList<Validator>();
				List<Validator> validators = (List<Validator>)XmlManager.loadXml(this.getClass().getResourceAsStream("/validators.xml"));
				for (Validator validator : validators) {
					if (workItem.getWorkItemType().equals(validator.getWiType()) && 
							(validator.getTargetState() == null || validator.getTargetState().equals("") || validator.getTargetState().equals(workItem.getState2().getStringIdentifier()))) { //check WI Type
						String attrId = validator.getCondition().getAttribute();
						IAttribute attribute = workItemCommon.findAttribute(workItem.getProjectArea(), attrId, monitor);
						String value = workItem.getValue(attribute).toString();
						if (value != null && !value.equals("")) {
							for (String conditionValue : validator.getCondition().getValue()) {
								if (conditionValue.equals(value)) {
									validatorsToApply.add(validator);
									break;
								}
							}
						}
					}
				}
				
				//check if required attributes are filled
				if (validatorsToApply.size() > 0) {
					for (Validator validator : validatorsToApply) {
						for (String attrId : validator.getAttributeRequired()){
							IAttribute attribute = workItemCommon.findAttribute(workItem.getProjectArea(), attrId, monitor);
							Object value = getValue(workItem, attribute);
							if (value == null || value.toString().equals("")) {
								IAdvisorInfo info = collector.createProblemInfo(attribute.getDisplayName()+" "+Messages.getString("workItemCustomValidator.isRequired"), validator.getReason(), "error");
								collector.addInfo(info);
							}
						}
					}
				}
			}
		}
	}
	
	private Object getValue(IWorkItem workItem, IAttribute attribute) {
		Object value= workItem.getValue(attribute);
		if (AttributeTypes.STRING_TYPES.contains(attribute.getAttributeType()) && value instanceof String)
			value= ((String) value).trim();
		else if (AttributeTypes.HTML_TYPES.contains(attribute.getAttributeType()) && value instanceof String)
			value= XMLString.createFromXMLText(((String) value)).getPlainText().trim();
		return value;
	}	
}
