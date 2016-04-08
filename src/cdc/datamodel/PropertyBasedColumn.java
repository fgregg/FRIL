package cdc.datamodel;

public class PropertyBasedColumn extends DataColumnDefinition {

	public PropertyBasedColumn(String columnName, String sourceName) {
		super(columnName, DataColumnDefinition.TYPE_STRING, sourceName);
	}

}
