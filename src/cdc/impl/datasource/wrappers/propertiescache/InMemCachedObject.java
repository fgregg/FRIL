package cdc.impl.datasource.wrappers.propertiescache;

import cdc.datamodel.DataRow;

public class InMemCachedObject implements CachedObjectInterface {

	private CachedObject cacheKey;
	private DataRow record;
	
	public InMemCachedObject(DataRow data) {
		this.cacheKey = new CachedObject(data.getRecordId());
		this.record = data;
	}
	
	public DataRow getRecord() {
		return record;
	}
	
	public CachedObject getCacheKey() {
		return cacheKey;
	}
	
	public boolean equals(Object obj) {
		InMemCachedObject that = (InMemCachedObject)obj;
		return cacheKey.equals(that.cacheKey);
	}
	
	public int hashCode() {
		return cacheKey.hashCode();
	}
	
}
