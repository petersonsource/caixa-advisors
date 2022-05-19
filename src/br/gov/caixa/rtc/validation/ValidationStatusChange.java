package br.gov.caixa.rtc.validation;

import com.ibm.team.workitem.common.model.IWorkItem;

public class ValidationStatusChange implements IValidation {
	private IWorkItem workItem;
	private IWorkItem oldWorkItem;

	public ValidationStatusChange(IWorkItem workItem, IWorkItem oldWorkItem) {
		super();
		this.workItem = workItem;
		this.oldWorkItem = oldWorkItem;
	}

	public Boolean isValid() {
		String oldStateId = "";
		if (oldWorkItem != null) {
			oldStateId = oldWorkItem.getState2().getStringIdentifier();
		}
		String newStateId = workItem.getState2().getStringIdentifier();
		return !oldStateId.equals(newStateId);
	}

}
