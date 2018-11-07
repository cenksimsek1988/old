package com.softactive.taxreturn.manager;

import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.softactive.core.manager.AbstractExcelHandler;
import com.softactive.core.manager.MyException;
import com.softactive.taxreturn.object.Criterium;
import com.softactive.taxreturn.object.TaxReturnConstants;

@Component @Lazy
public class TaxReturnFormulaHandler extends AbstractExcelHandler<Criterium> implements TaxReturnConstants{
	public TaxReturnFormulaHandler(Map<String, Object> sharedParams) {
		super(sharedParams);
	}

	private static final long serialVersionUID = 9107145398620520329L;
	
	@Autowired
	private ExcelFormulaHandler fHandler;
	
	private static final String SHEET_NAME = "Kriterler";

	@Override
	protected int getArrayStartIndex() {
		// Skip the header row
		return 1;
	}

	@Override
	protected void mapMetaData(Workbook r) throws MyException {
		sharedParams.put(PARAM_WORKBOOK, r);
		sharedParams.put(PARAM_DATE, getLastDate(r));
		sharedParams.put(PARAM_TAX_RETURN_NAME_FOR_CRITERIUM, getTaxReturnType(r));
	}
	
	private LocalDate getLastDate(Workbook wb) {
		String dateString = wb.getSheet("idari").getRow(2).getCell(2).getStringCellValue();
		return LocalDate.parse(dateString);
	}
	
	private String getTaxReturnType(Workbook wb) {
		return wb.getSheet("Beyanname").getRow(1).getCell(0).getStringCellValue();
	}

	@Override
	protected boolean hasNext(Map<String, Object> metaMap) {
		return false;
	}

	@Override
	protected String getSheetName() {
		return SHEET_NAME;
	}
	
	private void addParamFromRow(Row r, int cellIndex, String key, Map<String, Object> sharedParams) {
		Cell cell = r.getCell(cellIndex);
		System.out.println(cell.getNumericCellValue());
		if(cell != null) {
			double d = cell.getNumericCellValue();
			sharedParams.put(key, d);
		}
	}

	@Override
	protected Criterium getObject(Row o) throws MyException {
		addParamFromRow(o, 0, PARAM_CODE, sharedParams);
		addParamFromRow(o, 1, PARAM_NAME, sharedParams);
		addParamFromRow(o, 2, PARAM_IMPORTANCE_COEF, sharedParams);
		String formula = o.getCell(3).getCellFormula();
		sharedParams.put(PARAM_FLOOR, o.getCell(4).getNumericCellValue());
		sharedParams.put(PARAM_CEIL, o.getCell(5).getNumericCellValue());
		addParamFromRow(o, 6, PARAM_FLOOR_POINT, sharedParams);
		addParamFromRow(o, 7, PARAM_CEIL_POINT, sharedParams);
		fHandler.handle(formula);
		return null;
	}

	@Override
	protected boolean isOutputInvalid(List<Criterium> output) {
		// TODO Auto-generated method stub
		return false;
	}

}
