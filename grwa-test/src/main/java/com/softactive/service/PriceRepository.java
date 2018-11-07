package com.softactive.service;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

import org.joda.time.LocalDate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import com.softactive.grwa.object.Price;
import com.softactive.grwa.object.RiskFactor;


@Repository
public class PriceRepository extends BaseDataRepository<Price> {

	@Override
	public String tableName() {
		return "prc_price";
	}

	@Override
	public int save(Price p) {
		try {
			return insert(p);
		} catch(DuplicateKeyException e){
			Price in = findUnique(p);
			p.setId(in.getId());
			update(p);
			return p.getId();
		}
	}

	@Override
	public Price findUnique(Price p) {
		return findByRiskFactorIdAndDate(p.getRiskFactorId(), p.getDataDate());
	}

	public Price findByRiskFactorIdAndDate(Integer rfId, Date date) {
		String sql = initQuery() + " where risk_factor_id=" + rfId +
				" and data_date='" + date + "'";
		List<Price> answer = query(sql);
		if (answer.size() == 1) {
			return answer.get(0);
		}
		return null;
	}

	private Date sqlDate(LocalDate d) {
		return new Date(d.toDate().getTime());
	}

	private List<Price> findByRiskFactors(List<RiskFactor> riskFactors){
		String sql = initQuery() + " where risk_factor_id " + getWhereStatementForRiskFactors(riskFactors);
		return query(sql);
	}

	public void deleteByRiskFactor(List<RiskFactor> riskFactors) {
		List<Price> list = findByRiskFactors(riskFactors);
		for(Price rf:list) {
			delete(rf.getId());
		}
	}

	private String getWhereStatementForRiskFactors(List<RiskFactor> riskFactors) {
		String sql = " in ('";
		for(RiskFactor rf:riskFactors) {
			sql += rf.getId() + "','";
		}
		sql = sql.substring(0,sql.length()-2);
		sql += ")";
		return sql;
	}
}
