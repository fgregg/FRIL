package cdc.impl.join.svm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

public class SortedCircularBuffer {
	
	public static final int REMOVE_POLICY_REMOVE_SMALLEST = 1;
	public static final int REMOVE_POLICY_REMOVE_LARGEST = 2;
	
	private int removePolicy = REMOVE_POLICY_REMOVE_SMALLEST;
	private int capacity = Integer.MAX_VALUE;
	private int currentCapacity = 0;
	private TreeMap map = new TreeMap();
	
	public SortedCircularBuffer() {
	}
	
	public SortedCircularBuffer(int maxCapacity, int removePolicy) {
		this.capacity = maxCapacity;
		this.removePolicy = removePolicy;
	}
	
	public void add(Object key, Object value) {
		List list = (List) map.get(key);
		if (list == null) {
			list = new ArrayList();
			map.put(key, list);
		}
		list.add(value);
		currentCapacity++;
		ensureCapacity();
	}
	
	private void ensureCapacity() {
		while (currentCapacity > capacity) {
			Object removeCandidate;
			if (removePolicy == REMOVE_POLICY_REMOVE_LARGEST) {
				removeCandidate = map.lastKey();
			} else {
				removeCandidate = map.firstKey();
			}
			List l = (List) map.get(removeCandidate);
			l.remove(0);
			if (l.isEmpty()) {
				map.remove(removeCandidate);
			}
			currentCapacity--;
		}
	}
	
	public Iterator getOrderedValuesIterator() {
		return new BufferIterator();
	}
	
	public int size() {
		return currentCapacity;
	}
	
	public void remove(Object key) {
		map.remove(key);
	}
	
	protected void finalize() throws Throwable {
		map.clear();
		map = null;
	}
	
	private class BufferIterator implements Iterator {

		private Iterator mainMapIterator = map.navigableKeySet().iterator();
		private Iterator activeIterator = null;
		
		public boolean hasNext() {
			if (activeIterator != null) {
				if (activeIterator.hasNext()) {
					return true;
				}
			}
			if (mainMapIterator.hasNext()) {
				Object key = mainMapIterator.next();
				List l = (List) map.get(key);
				activeIterator = l.iterator();
				return true;
			} else {
				return false;
			}
		}

		public Object next() {
			return activeIterator.next();
		}

		public void remove() {
			throw new RuntimeException("Method not supported.");
		}
		
	}
	
	public static void main(String[] args) {
		SortedCircularBuffer buffer = new SortedCircularBuffer(3, REMOVE_POLICY_REMOVE_SMALLEST);
		testAddition(buffer, new Double(1.0), "1:1.0");
		testAddition(buffer, new Double(1.0), "2:1.0");
		testAddition(buffer, new Double(1.5), "3:1.5");
		testAddition(buffer, new Double(2.0), "4:2.0");
		testAddition(buffer, new Double(2.0), "5:2.0");
	}

	private static void testAddition(SortedCircularBuffer buffer, Double double1, String string) {
		buffer.add(double1, string);
		System.out.println("After add.");
		for (Iterator iterator = buffer.getOrderedValuesIterator(); iterator.hasNext();) {
			String val = (String) iterator.next();
			System.out.print(val + ", ");
		}
		System.out.println();
		System.out.println("Size: " + buffer.size());
	}

	public void clear() {
		map.clear();
		currentCapacity = 0;
	}
}
