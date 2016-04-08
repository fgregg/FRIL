package cdc.impl.datasource.wrappers.propertiescache;

public class CachedObject implements CachedObjectInterface {

	private int recId;
	
	public CachedObject(int recordId) {
		this.recId = recordId;
	}
	
	public boolean equals(Object obj) {
		CachedObject that = (CachedObject)obj;
		return recId == that.recId;
	}
	
	public int hashCode() {
		return recId;
	}
	
}
