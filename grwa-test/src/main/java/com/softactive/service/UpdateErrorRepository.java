package com.softactive.service;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.softactive.grwa.object.UpdateError;

@Repository
public class UpdateErrorRepository extends BaseDataRepository<UpdateError> {

	@Override
	public String tableName() {
		return "update_error";
	}

	@Override
	public UpdateError findUnique(UpdateError ue) {
		return findByRiskFactorId(ue.getRiskFactorId());
	}


	public UpdateError findByRiskFactorId(Integer rfId) {
		String sql = initQuery() + " where risk_factor_id=" + rfId + " LIMIT 1";
		List<UpdateError> list = query(sql);
		if(list==null || list.isEmpty()) {
			return null;
		} else {
			return list.get(0);
		}
	}
}
