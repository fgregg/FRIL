package cdc.datamodel;

public class PropertyBasedColumn extends DataColumnDefinition {

	private String userLabel;
	
	public PropertyBasedColumn(String columnName, String sourceName) {
		super(columnName, DataColumnDefinition.TYPE_STRING, sourceName);
	}

	public PropertyBasedColumn(String columnName, String sourceName, String userLabel) {
		super(columnName, DataColumnDefinition.TYPE_STRING, sourceName);
		this.userLabel = userLabel;
	}
	
	public String toString() {
		if (userLabel != null) {
			return userLabel;
		} else {
			return super.toString();
		}
	}

}
