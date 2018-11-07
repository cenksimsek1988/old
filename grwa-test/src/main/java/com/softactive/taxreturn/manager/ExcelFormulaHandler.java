package com.softactive.taxreturn.manager;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.joda.time.LocalDate;
import org.joda.time.Years;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.softactive.core.manager.AbstractHandler;
import com.softactive.core.manager.MyException;
import com.softactive.core.object.ExcelCellPointer;
import com.softactive.core.object.MyError;
import com.softactive.service.CriteriumService;
import com.softactive.taxreturn.object.Criterium;
import com.softactive.taxreturn.object.TaxReturnConstants;

@Component @Lazy
public class ExcelFormulaHandler extends AbstractHandler<String, String, List<String>, String, String, List<String>> implements TaxReturnConstants {
	public ExcelFormulaHandler(Map<String, Object> sharedParams) {
		super(sharedParams);
	}

	private static final long serialVersionUID = 4534755617845373062L;

	private static final char[] EXCEL_OPERATORS = new char[] {
			'*',
			'/',
			'-',
			'+',
			'(',
			')',
			',',
			';',
			'='
	};

	@Autowired
	private CriteriumService cs;

	private AbstractMap.SimpleEntry<String, Integer> getAddress(String cellAddress) throws MyException{
		int rowStart = getRowNumberStartIndex(cellAddress);
		String columnAddress = getColumnAddressName(cellAddress, rowStart);
		int rowNumber = getRowNumber(cellAddress, rowStart);
		return new AbstractMap.SimpleEntry<String, Integer>(columnAddress, rowNumber);
	}

	private int getRowNumber(String cellAddress, int start) {
		String numberString = cellAddress.substring(start);
		return Integer.valueOf(numberString) - 1;
	}

	private String getColumnAddressName(String cellAddress, int end) {
		return cellAddress.substring(0, end);
	}

	private int getRowNumberStartIndex(String cellAddress) throws MyException {
		for(int i = 1; i < cellAddress.length(); i++) {
			char c = cellAddress.charAt(i);
			try {
				Integer.valueOf(c);
				return i;
			} catch (NumberFormatException e) {
				continue;
			}
		}
		throw new MyException("no integer found in cell address (starting with char index:1): " + cellAddress);
	}

	private String getStandardizedAddress(String parentName, AbstractMap.SimpleEntry<String, Integer> address, Map<String, Object> sharedParams) {
		Workbook wb = (Workbook) sharedParams.get(PARAM_WORKBOOK);
		Sheet sheet = wb.getSheet(parentName);

		ExcelCellPointer ex = new ExcelCellPointer(sheet, address.getValue(), address.getKey());
		String sub = null;
		try {
			LocalDate period = ex.getPeriod();
			LocalDate lastPeriod = (LocalDate) sharedParams.get(PARAM_DATE);
			Years periodDiff = Years.yearsBetween(lastPeriod, period);
			int diff = periodDiff.getYears();
			sub = "[" + diff + "]";
		} catch (MyException e) {
			sub = "'" + e + "'";
		}
		String indicatorName = ex.getCode();
		return parentName + "!" + indicatorName + sub;
	}

	private String getParentName(String formula, int index) {
		return formula.substring(0, index);
	}

	private String getCellAdress(String formula, int index) {
		return formula.substring(index+1);
	}

	@Override
	protected String onFormatResponse(String rowInput) throws MyException {
		return rowInput;
	}

	@Override
	protected void mapMetaData(String r) throws MyException {
		sharedParams.put(PARAM_LAST_INDEX, 0);
		sharedParams.put("0", "");
	}

	@Override
	protected boolean hasNext(Map<String, Object> metaMap) {
		return false;
	}

	@Override
	protected List<String> getArray(String r) {
		List<String> components = new ArrayList<String>();
		String preText = "";
		String indicator = "";
		int index = 0;
		for(int i = 0; i < r.length(); i++) {
			try {
				char op = returnIfOperator(r.charAt(i));
				preText += String.valueOf(op);
				if(indicator.length()>0) {
					components.add(indicator);
					indicator = "";
				}
			} catch (MyException e) {
				indicator += e.getMsg();
				if(preText.length()>0) {
					sharedParams.put(String.valueOf(index), preText);
					index++;
					preText = "";
				}
				if(index==0) {
					index = 1;
				}
			}
			if(i == r.length()-1) {
				if(indicator.length() > 0) {
					components.add(indicator);
					indicator = "";
				} else if(preText.length() > 0) {
					sharedParams.put(String.valueOf(index), preText);
					preText = "";
				}
			}
		}
		return components;
	}

	private char returnIfOperator(char toCheck) throws MyException {
		for(char op:EXCEL_OPERATORS) {
			if(op==toCheck) {
				return op;
			}
		}
		throw new MyException(String.valueOf(toCheck));
	}

	@Override
	protected List<String> getList(List<String> array) {
		Criterium criterium = new Criterium();
		String formula = (String) sharedParams.get("0");
		for(int i = 0; i < array.size(); i++) {
			String address = null;
			try {
				address = getObject(array.get(i));
			} catch (MyException e) {
				System.out.println(e);
			}
			formula += address + sharedParams.get(String.valueOf(i+1));
		}
		criterium.setFormula(formula);
		criterium.setCeil((double)sharedParams.get(PARAM_CEIL));
		criterium.setFloor((double)sharedParams.get(PARAM_FLOOR));
		Object cPoint = sharedParams.get(PARAM_CEIL_POINT);
		if(cPoint != null) {
			criterium.setCeilPoint((double)cPoint);
		}
		Object fPoint = sharedParams.get(PARAM_FLOOR_POINT);
		if(fPoint != null) {
			criterium.setFloorPoint((double)fPoint);
		}
		Object code = sharedParams.get(PARAM_CODE);
		if(code != null) {
			criterium.setCode((String)code);
		}
		Object coef = sharedParams.get(PARAM_IMPORTANCE_COEF);
		if(coef != null) {
			criterium.setCoef((double)coef);
		}
		setByProducts(criterium);
		criterium.setTaxReturn((String)sharedParams.get(PARAM_TAX_RETURN_NAME_FOR_CRITERIUM));
		cs.save(criterium);
		return null;
	}

	private void setByProducts(Criterium cr){
		double a = 
				(
					cr.getCeil() - 
					cr.getFloor()
				)
					/
				(
					(
						2-cr.getCeilPoint()
					)
						/
					(
						1-cr.getCeilPoint()
					)
						- 
					(
						2-cr.getFloorPoint()
					)
						/
					(
						1-cr.getFloorPoint()
					)				
				);
		double b = cr.getFloor() - ( a / (1 - cr.getFloorPoint()) ) - a;
		cr.setA(a);
		cr.setB(b);
	}

	@Override
	protected String getObject(String o) throws MyException {
		int index = o.indexOf('!');
		String parentName = getParentName(o, index);
		String cellAddress = getCellAdress(o, index);
		AbstractMap.SimpleEntry<String, Integer> address = getAddress(cellAddress);
		String answer = getStandardizedAddress(parentName, address, sharedParams);
		return answer;
	}

	@Override
	protected boolean isOutputInvalid(List<String> output) {
		if(output.size()==0) {
			MyError er = new MyError(1,"couldnt create any String component from criteria");
			sharedParams.put(PARAM_ERROR, er);
			return true;
		}
		for(String component:output) {
			if(component==null) {
				MyError er = new MyError(1,"there is a null component in output formula");
				sharedParams.put(PARAM_ERROR, er);
				return true;
			} else if(component.length()==0) {
				MyError er = new MyError(1,"there is an empty component in output formula");
				sharedParams.put(PARAM_ERROR, er);
				return true;
			}
		}
		return false;
	}

}
