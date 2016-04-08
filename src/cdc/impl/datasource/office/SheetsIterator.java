package cdc.impl.datasource.office;

import java.util.Iterator;

import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class SheetsIterator implements Iterator {
	
	private HSSFWorkbook workbook;
	private String[] sheetNames;
	
	private Iterator rowIterator;
	private HSSFSheet sheet;
	private HSSFFormulaEvaluator evaluator;
	
	private int sheetNumber = 0;
	
	public SheetsIterator(HSSFWorkbook workbook) {
		this(workbook, null);
	}
	
	public SheetsIterator(HSSFWorkbook workbook, String[] sheetNames) {
		this.workbook = workbook;
		this.sheetNames = sheetNames;
		if (sheetNames == null) {
			this.sheetNames = new String[workbook.getNumberOfSheets()];
			for (int i = 0; i < this.sheetNames.length; i++) {
				this.sheetNames[i] = workbook.getSheetName(i);
			}
		}
	}
	
	public boolean hasNext() {
		while (sheet == null || !rowIterator.hasNext()) {
			if (sheetNumber == sheetNames.length) {
				return false;
			}
			//time to move to next sheet
			while ((sheet = workbook.getSheet(sheetNames[sheetNumber++])) == null) {
				if (sheetNumber == sheetNames.length) {
					return false;
				}
			}
			evaluator = new HSSFFormulaEvaluator(sheet, workbook);
			rowIterator = sheet.rowIterator();
			//skipping header
			rowIterator.next();
		}
		return rowIterator.hasNext();
	}

	public Object next() {
		return rowIterator.next();
	}
	
	public HSSFFormulaEvaluator getEvaluator() {
		return evaluator;
	}

	public void remove() {
		evaluator = null;
		sheet = null;
		if (rowIterator != null) {
			rowIterator.remove();
			rowIterator = null;
		}
	}

	public int countRecords() {
		int total = 0;
		for (int i = 0; i < sheetNames.length; i++) {
			HSSFSheet sheet = workbook.getSheet(sheetNames[i]);
			if (sheet != null) {
				total += sheet.getPhysicalNumberOfRows() - 1;
			}
		}
		return total;
	}
}
