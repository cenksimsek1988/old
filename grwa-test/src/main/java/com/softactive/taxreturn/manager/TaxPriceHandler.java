package com.softactive.taxreturn.manager;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.softactive.core.manager.MyException;
import com.softactive.taxreturn.object.Price;

@Component  @Lazy
public class TaxPriceHandler extends AbstractTaxHandler<Price>{
	public TaxPriceHandler(Map<String, Object> sharedParams) {
		super(sharedParams);
	}

	private static final long serialVersionUID = 5666487400628222840L;

	@Override
	protected Price getObject(Element o) throws MyException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String[] getArrayTags() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void mapMetaData(Document arg0) throws MyException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected boolean isOutputInvalid(List<Price> output) {
		// TODO Auto-generated method stub
		return false;
	}

}
