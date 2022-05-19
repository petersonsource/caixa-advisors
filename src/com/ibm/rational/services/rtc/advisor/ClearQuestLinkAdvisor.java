package com.ibm.rational.services.rtc.advisor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.links.common.IReference;
import com.ibm.team.links.common.factory.IReferenceFactory;
import com.ibm.team.links.internal.links.Reference;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.service.AbstractService;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.service.IWorkItemServer;

/*
 * <precondition id="rtc.advisor.example.ClearQuestLinkAdvisor" name="ClearQuest Link">
 * 		<config wiType="sisol" urlCQ="http://cxextrnt233.desenvolvimento.extracaixa/cqweb/restapi/sclq001/SISOL/RECORD/[ID]?format=HTML&amp;noframes=false&amp;recordType=Demandas"/>
 * </precondition>
*/
public class ClearQuestLinkAdvisor extends AbstractService implements IOperationAdvisor {
	
	public void run(AdvisableOperation operation, IProcessConfigurationElement advisorConfiguration, IAdvisorInfoCollector collector, IProgressMonitor monitor) throws TeamRepositoryException {
		Object data = operation.getOperationData();
		if (data instanceof ISaveParameter) {
			ISaveParameter saveParameter = (ISaveParameter) data;			
			IAuditable auditable = saveParameter.getNewState();
			IAuditable oldAuditable = saveParameter.getOldState();
			if (auditable instanceof IWorkItem) { //  && oldAuditable == null -> only exec in creation
				IWorkItem workItem = (IWorkItem) auditable;
				String a = "abcde";
				
				String wiType = workItem.getWorkItemType();
				
				IProcessConfigurationElement[] configElements = advisorConfiguration.getChildren();
				if (configElements != null) {
					for (IProcessConfigurationElement configElement : configElements) {
						String configWIType = configElement.getAttribute("wiType");
						String configURLCQ = configElement.getAttribute("urlCQ");
						
						if (wiType.equals(configWIType)) {
							IWorkItemServer workItemService = getService(IWorkItemServer.class);
							IWorkItemReferences references = saveParameter.getNewReferences();
							
					        //get id clearquest
					        String idClearQuest = "";
					        IAttribute idCQAttribute = workItemService.findAttribute(workItem.getProjectArea(), "sisol.idClearQuest", monitor);
					        if (idCQAttribute != null && workItem.hasAttribute(idCQAttribute)) {
					        	Object objIdCQ = workItem.getValue(idCQAttribute);
					        	idClearQuest = (String)objIdCQ;
					        } else {
								IAdvisorInfo info = collector.createProblemInfo("Atributo não encontrado", "Não foi possível encontrar o atributo sisol.idClearQuest", "error");
								collector.addInfo(info);
								return;
					        }
					        
					        if (idClearQuest == null || idClearQuest.equals("")) {
								IAdvisorInfo info = collector.createProblemInfo("Atributo em branco", "ID do ClearQuest em branco no work item", "error");
								collector.addInfo(info);
								return;			        	
					        }					
							
							URI uri = null;
							try {
								configURLCQ = configURLCQ.replaceAll("\\[ID\\]", idClearQuest);
								uri = new URI(configURLCQ);
							} catch (URISyntaxException e) {
								IAdvisorInfo info = collector.createProblemInfo("Invalid URI", e.getMessage(), "error");
								collector.addInfo(info);
								return;
							}			        
				            Reference reference = (Reference)IReferenceFactory.INSTANCE.createReferenceFromURI(uri, "Link para SISOL WEB", "cqlink");
				            		            
				            //--replace / insert clearquest link
				            List<IReference> relatedReferences = references.getReferences(WorkItemEndPoints.RELATED_ARTIFACT);
				            for (IReference ref : relatedReferences) {
				            	if (ref.getExtraInfo().equals("cqlink")) {
				            		references.remove(ref);
				            	}
				            }	            
				            references.add(WorkItemEndPoints.RELATED_ARTIFACT, reference);
						}
					}
				}
			}
		}
	}

}
