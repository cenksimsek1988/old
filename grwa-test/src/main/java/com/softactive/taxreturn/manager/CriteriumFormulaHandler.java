package com.softactive.taxreturn.manager;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.joda.time.LocalDate;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.softactive.core.manager.AbstractHandler;
import com.softactive.core.manager.ExcelWriter;
import com.softactive.core.manager.MyException;
import com.softactive.core.object.CoreConstants;
import com.softactive.core.object.ExcelCellPointer;
import com.softactive.core.object.ExcelEntry;
import com.softactive.core.object.MyError;
import com.softactive.taxreturn.object.Criterium;
import com.softactive.taxreturn.object.TaxReturnConstants;

@Component @Lazy
public class CriteriumFormulaHandler extends AbstractHandler<Criterium, Criterium, List<String>, String, String, List<String>> implements TaxReturnConstants, CoreConstants{
	
	public CriteriumFormulaHandler(Map<String, Object> sharedParams) {
		super(sharedParams);
	}

	private static final long serialVersionUID = 4534755617845373062L;
	
	private static final String CEIL = "Alt Limit";
	private static final String FLOOR = "Üst Limit";
	private static final String CEIL_POINT = "Min Puan";
	private static final String FLOOR_POINT = "Max Puan";
	private static final String NAME = "İsim";
	private static final String COEF = "Önem Derecesi";
	private static final String FORMULA = "Formul";
	private static final String POINT = "point";
	private static final String WEIGHTED_POINT = "Ağırlıklı Puan";

	private AbstractMap.SimpleEntry<String, String> getAddress(String pseudoAddress, Map<String, Object> sharedParams) throws MyException{
		int columnStart = getColumnNameStartIndex(pseudoAddress);
		String columnName = getColumnName(pseudoAddress, columnStart, sharedParams);
		String rowName = getRowName(pseudoAddress, columnStart);
		return new AbstractMap.SimpleEntry<String, String>(rowName, columnName);
	}

	private String getRowName(String cellAddress, int end) {
		return cellAddress.substring(0, end);
	}
	
	private String getColumnName(String cellAddress, int start, Map<String, Object> sharedParams) {
		String columnName = cellAddress.substring(start + 1, cellAddress.length() - 1);
		if(cellAddress.charAt(start)=='[') {
			int periodBefore = Integer.valueOf(columnName);
			return getLocalDate(periodBefore, sharedParams).toString();
		}
		return cellAddress.substring(start, cellAddress.length() - 1);
	}
	
	private LocalDate getLocalDate(int periodBefore, Map<String, Object> sharedParams) {
		LocalDate current = (LocalDate)sharedParams.get(PARAM_DATE);
		return current.plusYears(-1*periodBefore);
	}

	private int getColumnNameStartIndex(String cellAddress) throws MyException {
		for(int i = 1; i < cellAddress.length(); i++) {
			char c = cellAddress.charAt(i);
			if(c == '\'' || c =='[') {
				return i;
			}
		}
		throw new MyException("no column definer char found in cell address (starting with char index:1): " + cellAddress);
	}

	private String getParentName(String formula, int index) {
		return formula.substring(0, index);
	}

	private String getCellAdress(String formula, int index) {
		return formula.substring(index+1);
	}

	@Override
	protected Criterium onFormatResponse(Criterium rowInput) throws MyException {
		return rowInput;
	}

	@Override
	protected void mapMetaData(Criterium r) throws MyException {
		sharedParams.put(PARAM_LAST_INDEX, 0);
		sharedParams.put("0", "");
	}

	@Override
	protected boolean hasNext(Map<String, Object> metaMap) {
		return false;
	}

	@Override
	protected List<String> getArray(Criterium cr) {
		String formula = cr.getFormula();
		System.out.println("formula: " + formula);
		List<String> components = new ArrayList<String>();
		String preText = "";
		String indicator = "";
		int index = 0;
		for(int i = 0; i < formula.length(); i++) {
			try {
				char op = returnIfOperator(formula.charAt(i));
				System.out.println("adding to pre text: " + formula.charAt(i));
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
			if(i == formula.length()-1) {
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
	
	private TreeMap<String, String> getMappedCriterium(Criterium cr) {
		TreeMap<String, String> answer = new TreeMap<String, String>();
		answer.put(CEIL, String.valueOf(cr.getCeil()));
		answer.put(CEIL_POINT, String.valueOf(cr.getCeilPoint()));
		answer.put(FLOOR, String.valueOf(cr.getFloor()));
		answer.put(FLOOR_POINT, String.valueOf(cr.getFloorPoint()));
		answer.put(NAME, cr.getName());
		answer.put(COEF, String.valueOf(cr.getCoef()));
		answer.put(ExcelEntry.CODE, cr.getCode());
		double a = cr.getA();
		double b = cr.getB();
		String point= "=(" + FORMULA + "-" + b + "-2*" + a + ")/(" + 
		FORMULA + "-" + b + "-" + a + ")";
		answer.put(POINT, point);
		answer.put(WEIGHTED_POINT, "=" + POINT + "*" + COEF);
		return answer;
	}

	@Override
	protected List<String> getList(List<String> array) {
		Criterium cr = (Criterium)sharedParams.get(PARAM_CRITERIUM);
		ExcelWriter writer = (ExcelWriter) sharedParams.get(PARAM_EXCEL_WRITER);
		String formula = "=" + (String) sharedParams.get("0");
		for(int i = 0; i < array.size(); i++) {
			String address = null;
			try {
				address = getObject(array.get(i));
			} catch (MyException e) {
				System.out.println(e);
			}
			formula += address;
			String end = (String) sharedParams.get(String.valueOf(i+1));
			if(end != null) {
				formula += end;
			}
		}
		TreeMap<String, String> preCalcMap = new TreeMap<>();
		preCalcMap.put(FORMULA, formula);
		try {
			ExcelCellPointer pointer = writer.write(preCalcMap, "Rapor");
		} catch (MyException e1) {
			System.out.println(e1);
		}

		TreeMap<String, String> rowMap = getMappedCriterium(cr);
		try {
			writer.write(rowMap, "Rapor");
		} catch (MyException e) {
			System.out.println(e);
		}
		try {
			writer.flush("beyanname.xlsx");
		} catch (MyException e) {
			System.out.println(e);
		}
		return null;
	}

	@Override
	protected String getObject(String o) throws MyException {
		int index = o.indexOf('!');
		String parentName = getParentName(o, index);
		String pseudeCellAddress = getCellAdress(o, index);
		SimpleEntry<String, String> address = getAddress(pseudeCellAddress, sharedParams);
		Workbook wb = (Workbook) sharedParams.get(PARAM_WORKBOOK);
		Sheet sheet = wb.getSheet(parentName);
		ExcelCellPointer pointer = new ExcelCellPointer(sheet, address.getKey(), address.getValue());
		return pointer.getFullAddress();
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
