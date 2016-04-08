package cdc.gui.validation;

import javax.swing.JOptionPane;

import cdc.datamodel.DataColumnDefinition;
import cdc.gui.components.paramspanel.ParamPanelField;
import cdc.gui.components.paramspanel.ParamsPanel;

public class ColumnNameValidator implements Validator {

	public boolean validate(ParamsPanel paramsPanel, ParamPanelField paramPanelField, String parameterValue) {
		String validatedName = DataColumnDefinition.normalizeColumnName(parameterValue);
		if (!validatedName.equals(parameterValue)) {
			String message = "Parameter " + paramPanelField.getUserLabel() + " can only use letters, numbers or underscore character.";
			paramPanelField.error(message);
			JOptionPane.showMessageDialog(paramsPanel, message);
			return false;
		}
		return true;
	}

}
