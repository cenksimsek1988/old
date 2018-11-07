package com.softactive.taxreturn.manager;

import java.util.Map;

import org.joda.time.LocalDate;

import com.softactive.core.manager.AbstractXmlHandler;
import com.softactive.core.manager.MyException;
import com.softactive.taxreturn.object.TaxReturnConstants;

public abstract class AbstractTaxHandler<P>  extends AbstractXmlHandler<P> implements TaxReturnConstants{
	public AbstractTaxHandler(Map<String, Object> sharedParams) {
		super(sharedParams);
	}

	private static final long serialVersionUID = 456533834758113624L;
	public static final String VERSION = "versiyon";
	public static final String CODE = "kod";
	public static final String START = "yayimTarihi";
	public static final String END = "kaldirilisTarihi";
	public static final String NAME = "adi";
	public static final String TAX_RETURN = "beyanname";
	@Override
	protected LocalDate resolveDateFromCustomFormat(String dateString) throws MyException {
		String[] dateArray = dateString.split("\\.");
		if(dateArray.length != 3) {
			throw new MyException("Cannot adjusted the date string: '" + dateString +
					"with splitter: '.'"); 
		}
		String adjustedString = dateArray[2] + "-" + dateArray[1] + "-" + dateArray[0];
		return resolveValidDate(adjustedString);
	}
	
	@Override
	protected boolean hasNext(Map<String, Object> metaMap) {
		return false;
	}
}
