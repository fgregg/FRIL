package cdc.impl.datasource.wrappers.propertiescache;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.utils.Log;
import cdc.utils.Props;
import cdc.utils.RJException;
import cdc.utils.RowUtils;

/**
 * This class is messed up and should not be used at the moment.
 * Requires more thoughts and implementation of dirty data trashing.
 * @author pjurczy
 *
 */
public class DiskCache implements CacheInterface {

	public static final int MEMORY_CACHE_SIZE = Props.getInteger("disk-cache-for-properties-mem-capacity", 50000);
	public static final int SWAP_BATCH_SIZE = (int) (0.1 * MEMORY_CACHE_SIZE);
	
	private List buffer = new LinkedList();
	private Map mappings = new HashMap();
	
	private RandomAccessFile raf;
	private File rafFile = new File(System.currentTimeMillis() + "_" + hashCode() + ".bin");
	
	public DiskCache() {
		Log.log(getClass(), "Disk cache created: " + rafFile);
	}
	
	public CachedObjectInterface cacheObject(DataRow data) throws IOException {
		CachedObjectInterface c = new CachedObject(data.getRecordId());
		DiskCacheItem item = (DiskCacheItem) mappings.get(c);
		if (item == null) {
			item = new DiskCacheItem(data);
			mappings.put(c, item);
			buffer.add(item);
			flushIfNeeded();
		} else {
			Log.log(getClass(), "rowId=" + data.getRecordId() + " already in cache.", 2);
		}
		return c;
	}

	public DataRow getObject(CachedObjectInterface co) throws IOException, RJException {
		DiskCacheItem item = (DiskCacheItem) mappings.get(co);
		DataRow row = item.getRow();
		flushIfNeeded();
		if (item == null) {
			System.out.println("WARNING: Cached data item not found. Possible Error?");
			return null;
		}
		return row;
	}

	public void trash() throws IOException {
		mappings.clear();
		buffer.clear();
		if (raf != null) {
			raf.close();
			rafFile.delete();
		}
	}
	
	private void flushIfNeeded() throws IOException {
		if (buffer.size() > MEMORY_CACHE_SIZE) {
			if (raf == null) {
				raf = new RandomAccessFile(rafFile, "rw");
			}
			Log.log(getClass(), "Caching data to disk.", 2);
			for (int i = 0; i < SWAP_BATCH_SIZE; i++) {
				DiskCacheItem item = (DiskCacheItem) buffer.remove(0);
				item.writeToDisk();
			}
		}
	}
	
	private class DiskCacheItem {

		private DataRow row;
		private DataColumnDefinition[] columns;
		private String srcName;
		private int length;
		private long position = -1;
		
		public DiskCacheItem(DataRow data) {
			this.row = data;
		}

		public void writeToDisk() throws IOException {
			if (position == -1) {
				Log.log(getClass(), "recId=" + row.getRecordId() + " writing to disk.", 3);
				//need to write to disk
				columns = row.getRowModel();
				srcName = row.getSourceName();
				byte[] bytes = RowUtils.rowToByteArray(null, row, columns);
				row = null;
				position = raf.length();
				raf.seek(position);
				length = bytes.length;
				raf.write(bytes);
			} else {
				Log.log(getClass(), "recId=" + row.getRecordId() + " already written, skipping.", 3);
				//TODO need to check if record is dirty!!!!
				row = null;
			}
		}
		
		public DataRow getRow() throws IOException, RJException {
			if (row == null) {
				bringFromDisk(raf);
				buffer.add(this);
			}
			return row;
		}
		
		private void bringFromDisk(RandomAccessFile raf) throws IOException, RJException {
			raf.seek(position);
			byte[] data = new byte[length];
			ensureRead(raf, data);
			row = RowUtils.byteArrayToDataRow(null, data, columns, srcName);
			Log.log(getClass(), "recId=" + row.getRecordId() + " read back from disk.", 3);
		}

		private void ensureRead(RandomAccessFile raf, byte[] data) throws IOException {
			int full = 0;
			while ((full += raf.read(data, full, data.length - full)) != data.length);
		}

		
	}

}
