package br.gov.caixa.rtc.advisor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import br.gov.caixa.rtc.dto.EnumerationDto;

import com.ibm.rational.services.rtc.advisor.messages.Messages;
import com.ibm.team.links.common.IReference;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.model.AttributeTypes;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IEnumeration;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.service.IWorkItemServer;

public class ValidadeRelated extends AbstractRTCService implements
		IOperationAdvisor {

	private static final String ERROR_MESSAGE_TYPE = "error";
	
	private Set<String> listaEstadosPermitidos = null;
	private Set<String> listaWIRelacionadoPermitidos = null;

	public void run(AdvisableOperation operation,IProcessConfigurationElement advisorConfiguration,IAdvisorInfoCollector collector, IProgressMonitor monitor)
			throws TeamRepositoryException {

		Object data = operation.getOperationData();
		boolean relatedError = false;
		Integer relatedOutOfScope = 0;
		Integer wiOutOfScope = 0;

		if (data instanceof ISaveParameter) {
			ISaveParameter saveParameter = (ISaveParameter) data;
			IAuditable auditable = saveParameter.getNewState();
			IAuditable oldAuditable = saveParameter.getOldState();
			IWorkItemServer workItemService = getService(IWorkItemServer.class);
			if (auditable instanceof IWorkItem) {
				IWorkItem workItem = (IWorkItem) auditable;
				IWorkItem oldWorkItem = (IWorkItem) oldAuditable;

				// ------se nao alterou o status, para

				String oldStateId = "";
				if (oldWorkItem != null) {
					oldStateId = oldWorkItem.getState2().getStringIdentifier();
				}
				String newStateId = workItem.getState2().getStringIdentifier();

				if (oldStateId.equals(newStateId)) {
					return;
				}

				// -----------------------------------

				try {
					IProcessConfigurationElement[] configElements = advisorConfiguration
							.getChildren();
					if (configElements != null) {

						String wiType = workItem.getWorkItemType();
						String configWiRelatedStateIdAr[] = null;
						String configWiRelatedTypeAr[] = null;
						String configWIType = null;
						String configStateId = null;
						String configWiRelatedType = null;
						String configWiRelatedStateId = null;
						String configWiAttrValue = null;
						String configWiAttrId = null;
						String configWiClassValue = null;
						String configWiClassId = null;
						IWorkItemHandle wiRelatedHandle = null;
						IWorkItemReferences references = null;
						List<IReference> relatedReferences = null;
						IRepositoryItemService repository = getService(IRepositoryItemService.class);

						IWorkItem workItemRelated = null;

						for (IProcessConfigurationElement configElement : configElements) {
							configWIType = configElement.getAttribute("wiType");
							configStateId = configElement.getAttribute("stateId");
							configWiRelatedType = configElement.getAttribute("wiRelatedType");
							configWiRelatedStateId = configElement.getAttribute("wiRelatedStateId");
							// Ambiente apenas Produção
							configWiAttrId = configElement.getAttribute("wiAttrId");
							configWiAttrValue = configElement.getAttribute("wiAttrValue");

							// Clase: Não valida relacionado se Emergencial
							configWiClassId = configElement.getAttribute("wiClassId");
							configWiClassValue = configElement.getAttribute("wiClassValue");

							// checa os parametros
							if (!wiType.equals(configWIType)) {
								continue;
							}

							// TODO LPF
							// valida valor da enumeracao (Ex.: acelerado)
							Object idClassValue = getAttribute(workItem,workItemService, configWiClassId, monitor);
							if (idClassValue.equals(configWiClassValue)) {
								continue;
							}

							// valida valor da enumeracao
							Object idValue = getAttribute(workItem,workItemService, configWiAttrId, monitor);
							if (!idValue.equals(configWiAttrValue)) {
								continue;
							}

							if (!configStateId.equalsIgnoreCase(workItem.getState2().getStringIdentifier())) {
								configStateId = configStateId.replace("s", "");
								if (!configStateId.equalsIgnoreCase(workItem.getState2().getStringIdentifier())) {
									continue;
								}
							}

							// Possibilidade de mais de um status para o item
							// relacionado
							if (configWiRelatedStateId != null) {
								configWiRelatedStateIdAr = configWiRelatedStateId.toLowerCase().split(";");
								listaEstadosPermitidos = new HashSet<String>(Arrays.asList(configWiRelatedStateIdAr));
								if (listaEstadosPermitidos == null) {
									String alert = Messages.getStringWithArgs(Messages.MSG_ERRO_CONFIG_ESTADO,"Valida Item Relacionado");
									IAdvisorInfo info = collector.createProblemInfo(alert,"",ERROR_MESSAGE_TYPE);
									collector.addInfo(info);
									
								}
//								
							}
							// Possibilidade de mais de um tipo de item para o
							// item
							// relacionado
							if (configWiRelatedType != null) {
								configWiRelatedTypeAr = configWiRelatedType.toLowerCase().split(";");
								listaWIRelacionadoPermitidos = new HashSet<String>(Arrays.asList(configWiRelatedTypeAr));
								if (listaWIRelacionadoPermitidos == null) {
									String alert = Messages.getStringWithArgs(Messages.MSG_ERRO_CONFIG_ITEM_RELACIONADO,"Valida Item Relacionado");
									IAdvisorInfo info = collector.createProblemInfo(alert,"",ERROR_MESSAGE_TYPE);
									collector.addInfo(info);
//									
								}
								
							}

							// fim possibilidade

							// Recupera as referencias relacionadas
							references = workItemService.resolveWorkItemReferences(workItem,monitor);
							relatedReferences = references.getReferences(WorkItemEndPoints.RELATED_WORK_ITEM);

							// verifica se existe referencias relacionados
							if (relatedReferences != null && relatedReferences.size() > 0) {
								for (IReference relatedReference : relatedReferences) {
									wiRelatedHandle = (IWorkItemHandle) relatedReference.resolve();
									workItemRelated = (IWorkItem) repository.fetchItem(wiRelatedHandle,IWorkItem.MEDIUM_PROFILE.getPropertiesArray());

									if (workItemRelated != null) {

										// verifica se existe relacionados com o
										// tipo informado e estado relacionado
											if (!listaWIRelacionadoPermitidos.contains(workItemRelated.getWorkItemType().toLowerCase())) {
												relatedOutOfScope++;
												continue;
											}
											if (!listaEstadosPermitidos.contains(workItemRelated.getState2().getStringIdentifier().toLowerCase())) {
													relatedError = true;
												}

										if (relatedOutOfScope.equals(configWiRelatedTypeAr.length)) {
											wiOutOfScope++;
										}

										relatedOutOfScope = 0;
									}
								}
							} else {
								EnumerationDto valueEnum = (EnumerationDto) getFullAttribute(workItem, workItemService,configWiAttrId, monitor);
								String strValueEnum = valueEnum.getValue();
								String alert = Messages.getStringWithArgs(Messages.MSG_ERRO_RELACIONAMENTO_PRD,strValueEnum);

								IAdvisorInfo info = collector.createProblemInfo(alert,Messages.MSG_ALERTA_CONTATO_QUALIDADE,ERROR_MESSAGE_TYPE);
								collector.addInfo(info);
							}
							// fim - verifica se existe referencias relacionados

							// }
							if (relatedError) {
								IAdvisorInfo info = collector.createProblemInfo(Messages.MSG_ERRO_ITENS_NAO_VALIDADOS,Messages.MSG_ALERTA_CONTATO_QUALIDADE,ERROR_MESSAGE_TYPE);
								collector.addInfo(info);
							}
							if (wiOutOfScope.equals(relatedReferences.size()) && relatedReferences.size() > 0) {
								IAdvisorInfo info = collector.createProblemInfo(Messages.MSG_ERRO_RELACIONAMENTO_ERRADO,Messages.MSG_ALERTA_CONTATO_QUALIDADE,ERROR_MESSAGE_TYPE);
								collector.addInfo(info);

							}

						}

					}
				} catch (Exception e) {
					IAdvisorInfo info = collector.createProblemInfo("Erro inesperado", e.getMessage(),ERROR_MESSAGE_TYPE);
					collector.addInfo(info);
					throw new TeamRepositoryException(e);
				}
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object getFullAttribute(IWorkItem workItem,IWorkItemServer workItemService, String attributeName,IProgressMonitor monitor) throws TeamRepositoryException {
		EnumerationDto enumAttr = new EnumerationDto();
		IAttribute attribute = workItemService.findAttribute(workItem.getProjectArea(), attributeName, monitor);
		if (attribute == null || workItem.hasAttribute(attribute) == false) {
			// Attribute not found
			return null;
		}

		if (AttributeTypes.isEnumerationAttributeType(attribute.getAttributeType())) {
			Identifier id = (Identifier) workItem.getValue(attribute);

			IEnumeration enumeration = workItemService.resolveEnumeration(attribute, monitor);
			List<ILiteral> literals = enumeration.getEnumerationLiterals();
			for (ILiteral literal : literals) {
				if (literal.getIdentifier2().getStringIdentifier().equals(id.getStringIdentifier())) {
					enumAttr.setId(literal.getIdentifier2().getStringIdentifier());
					enumAttr.setValue(literal.getName());
					break;
				}
			}
		}
		return enumAttr;
	}
}
