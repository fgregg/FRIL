package cdc.gui.validation;

import cdc.gui.components.paramspanel.ParamPanelField;
import cdc.gui.components.paramspanel.ParamsPanel;

public class CompoundValidator implements Validator {

	private Validator[] nestedValidators;
	
	public CompoundValidator(Validator[] nestedValidators) {
		this.nestedValidators = nestedValidators;
	}
	
	public boolean validate(ParamsPanel paramsPanel, ParamPanelField paramPanelField, String parameterValue) {
		for (int i = 0; i < nestedValidators.length; i++) {
			if (!nestedValidators[i].validate(paramsPanel, paramPanelField, parameterValue)) {
				return false;
			}
		}
		return true;
	}

}
