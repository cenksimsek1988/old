package com.softactive.taxreturn.object;

import java.util.Set;
import java.util.TreeMap;

import com.softactive.core.object.ExcelEntry;

import lombok.Getter;
import lombok.Setter;


public class TaxReturnEntry extends ExcelEntry {
	private static final long serialVersionUID = -1269560887272706034L;
	public static final String SIGN = "sign";


	@Getter @Setter
	private String parentName;
	@Getter @Setter
	private TreeMap<String, String> extras = new TreeMap<>();



	//	public TreeMap<String, String> getAllValues(TreeMap<String, String> values){
	//		TreeMap<String, String> answer = new TreeMap<String, String>(values);
	//		answer.put(CODE, code);
	//		answer.put(NAME, name);
	//		return answer;
	//	}
	public TreeMap<String, String> getValuesAsMap(){
		TreeMap<String, String> answer = new TreeMap<String, String>();
		answer.put(CODE, code);
		answer.put(NAME, name);
		for(String date:prices.keySet()) {
			answer.put(date, String.valueOf(prices.get(date)));
		}
		for(String columnName:extras.keySet()) {
			answer.put(columnName, extras.get(columnName));
		}
		return answer;
	}
	public Set<String> getPriceKeys(){
		return prices.keySet();
	}
	public String getPrice(String pseudoDate) {
		return prices.get(pseudoDate);
	}
//	public void addExtras(String columnName, String value) {
//		extras.put(columnName, value);
//	}
}
