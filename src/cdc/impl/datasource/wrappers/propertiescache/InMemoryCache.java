package cdc.impl.datasource.wrappers.propertiescache;

import java.util.HashMap;
import java.util.Map;

import cdc.datamodel.DataRow;
import cdc.utils.Log;

public class InMemoryCache implements CacheInterface {

	private Map cache = new HashMap();
	
	public CachedObjectInterface cacheObject(DataRow data) {
		CachedObject co = new CachedObject(data.getRecordId());
		cache.put(co, data);
		return co;
	}

	public DataRow getObject(CachedObjectInterface co) {
		return (DataRow)(cache.get(co));
	}

	public void trash() {
		Log.log(getClass(), "Cache is trashing.", 2);
		cache.clear();
	}

}
