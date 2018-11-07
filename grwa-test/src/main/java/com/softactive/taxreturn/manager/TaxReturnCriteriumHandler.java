package com.softactive.taxreturn.manager;

import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.softactive.core.manager.AbstractSqlHandler;
import com.softactive.core.manager.MyException;
import com.softactive.taxreturn.object.Criterium;
import com.softactive.taxreturn.object.TaxReturnConstants;

@Component @Lazy
public class TaxReturnCriteriumHandler extends AbstractSqlHandler<Criterium, Row> implements TaxReturnConstants{
	public TaxReturnCriteriumHandler(Map<String, Object> sharedParams) {
		super(sharedParams);
	}

	private static final long serialVersionUID = -3577106777691057761L;
	
	@Autowired
	private CriteriumFormulaHandler fHandler;

	@Override
	protected void mapMetaData(List<Criterium> r) throws MyException {
	}

	@Override
	protected Row getObject(Criterium o) throws MyException {
		sharedParams.put(PARAM_CRITERIUM, o);
		fHandler.handle(o);
		return null;
	}

	@Override
	protected boolean isOutputInvalid(List<Row> output) {
		// TODO Auto-generated method stub
		return false;
	}
}
