package br.gov.caixa.rtc.advisor.validator;

import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;

public class ConfigValidator {
	public static boolean isMissingConfigParams(IProcessConfigurationElement configElement, IAdvisorInfoCollector collector) {
		boolean isMissing = false;
		
		if (configElement.getAttribute("workItemType") == null || configElement.getAttribute("workItemType").equals("")) {
			IAdvisorInfo info = collector.createProblemInfo("Configuração Inválida", "Atributo workItemType não configurado no advisor CustomValidator", "error");
			collector.addInfo(info);
			isMissing = true;
		}
		if (configElement.getAttribute("stateId") == null || configElement.getAttribute("stateId").equals("")) {
			IAdvisorInfo info = collector.createProblemInfo("Configuração Inválida", "Atributo stateId não configurado no advisor CustomValidator", "error");
			collector.addInfo(info);
			isMissing = true;
		}
		
		return isMissing;
	}
}
