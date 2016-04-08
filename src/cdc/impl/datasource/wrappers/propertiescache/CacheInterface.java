package cdc.impl.datasource.wrappers.propertiescache;

import java.io.IOException;

import cdc.datamodel.DataRow;
import cdc.utils.RJException;

public interface CacheInterface {
	
	public CachedObjectInterface cacheObject(DataRow data) throws IOException;
	public DataRow getObject(CachedObjectInterface co) throws IOException, RJException;
	
	public void trash() throws IOException;
	
}
