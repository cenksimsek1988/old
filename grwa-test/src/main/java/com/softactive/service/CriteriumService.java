package com.softactive.service;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.softactive.taxreturn.object.Criterium;

@Repository
public class CriteriumService extends BaseDataRepository<Criterium>{

	@Override
	public String tableName() {
		return "cmn_criteria";
	}
	
	@Override
	public Criterium findUnique(Criterium c) {
		if(c.getId()==null) {
			return null;
		}
		return find(c.getId());
	}

	public List<Criterium> findByTaxReturn(String taxReturn){
		return query(initQuery() + " where tax_return='" + taxReturn + "'"); 
	}
}
