package com.softactive.service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.softactive.grwa.object.GrwaConstants;
import com.softactive.grwa.object.Indicator;
import com.softactive.grwa.object.Region;
import com.softactive.grwa.object.RiskFactor;

@Repository
public class RiskFactorRepository extends BaseDataRepository<RiskFactor> implements GrwaConstants{
	private final String BASE_SQL = initQuery() + " where (source_code='";

	@Autowired
	private PriceRepository ps;

	@Override
	public String tableName() {
		return "risk_factor";
	}

	@Override
	public RiskFactor findUnique(RiskFactor rf) {
		return findRiskFactorByIndicatorRegionAndFrequency(rf.getIndicatorCode(), rf.getRegionId(), rf.getFrequencyCode());
	}

	public RiskFactor findRiskFactorByIndicatorRegionAndFrequency(Integer ind, Integer rgnId, String frq) {
		String sql = initQuery() + " where (region_id=" + rgnId + " and indicator_id=" + ind + " and frequency_code='" + frq + "') limit 1" ;
		List<RiskFactor> answer = query(sql);
		if (answer!=null && answer.size() == 1) {
			return answer.get(0);
		} else if(answer.size()!=0) {
			for(RiskFactor toDel:answer) {
				delete(toDel.getId());
			}
		}
		return null;
	}

	public void save(List<RiskFactor> riskFactors) {
		for(RiskFactor rf:riskFactors) {
			save(rf);
		}
	}

	public List<RiskFactor> findBySourceAndFrequency(String src, String frq) {
		String sql = BASE_SQL + src + "' and frequency_code='" + frq + "')";
		return query(sql);
	}

	public RiskFactor pickRiskFactorBySourceAndFrequency(String src, String frq) {
		List<RiskFactor> answer = query(pickSql(src, frq));
		if(answer!=null && answer.size()==1) {
			return answer.get(0);
		} else {
			return null;
		}
	}

	public String pickSql(String src, String frq) {
		return BASE_SQL + src + "' and frequency_code='" + frq + 
				"' and (update_date <= '" + thresholdDate(frq) + "' or update_date is null ) ) LIMIT 1";
	}

	public List<RiskFactor> findBySourceForUpdate(String src){
		List<RiskFactor> answer =new ArrayList<RiskFactor>();
		for(String frq:FREQUENCIES) {
			answer.addAll(findBySourceAndFrequency(src, frq));
		}
		return answer;
	}

	private Date thresholdDate(String frq) {
		Calendar c = Calendar.getInstance();
		switch(frq) {
		case FREQUENCY_5_YEAR:
			c.add(Calendar.YEAR, -5);
			break;
		case FREQUENCY_ANNUAL:
			c.add(Calendar.YEAR, -1);
			break;
		case FREQUENCY_QUARTERLY:
			c.add(Calendar.MONTH, -3);
			break;
		case FREQUENCY_MONTHLY:
			c.add(Calendar.MONTH, -1);
			break;
		case FREQUENCY_WEEKLY:
			c.add(Calendar.WEEK_OF_YEAR, -1);
			break;
		case FREQUENCY_DAILY:
			c.add(Calendar.DAY_OF_YEAR, -1);
			break;
		}
		return date(c);
	}

	private Date date(Calendar c) {
		return date(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
	}

	private Date date(int year, int month, int day) {
		return Date.valueOf(year + "-" + month + "-" + day);
	}

	public List<RiskFactor> findBySourceAndFrequencyForUpdate(String src, String frq) {
		String sql = BASE_SQL + src + "' and frequency_code='" + frq + 
				"' and update_date > '" + getThresholdForFrequency(frq) + "')";
		return query(sql);
	}

	public List<RiskFactor> findBySourceAndFrequencyForHistorical(String src, String frq) {
		String sql = BASE_SQL + src + "' and frequency_code='" + frq + 
				"' and update_date is null)";
		return query(sql);
	}

	private String getThresholdForFrequency(String frq) {
		Calendar c = Calendar.getInstance();
		switch(frq) {
		case FREQUENCY_5_YEAR:
			c.add(Calendar.YEAR, -5);
			break;
		case FREQUENCY_ANNUAL:
			c.add(Calendar.YEAR, -1);
			break;
		case FREQUENCY_QUARTERLY:
			c.add(Calendar.MONTH, -3);
			break;
		case FREQUENCY_MONTHLY:
			c.add(Calendar.MONTH, -1);
			break;
		case FREQUENCY_WEEKLY:
			c.add(Calendar.WEEK_OF_YEAR, -1);
			break;
		case FREQUENCY_DAILY:
			c.add(Calendar.DAY_OF_YEAR, -1);
			break;
		}
		return getDateString(c);
	}

	private String getDateString(Calendar c) {
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH);
		int day = c.get(Calendar.DAY_OF_MONTH);
		return year + "-" + month + "-" + day;
	}

	public List<RiskFactor> findBySource(String src) {
		String sql = BASE_SQL + src + "')";
		return query(sql);
	}

	public List<RiskFactor> findByFrequency(String frq) {
		String sql = initQuery() + " where frequency_code='" + frq + "'";
		return query(sql);
	}

	public void deleteForRegionsAndSource(List<Region> regions, String src) {
		List<RiskFactor> toDelete = findByRegionsAndSource(regions, src);
		if(toDelete==null || toDelete.size()==0) {
			return;
		}
		ps.deleteByRiskFactor(toDelete);
		String sql = initQuery() + getWhereStatementForRegions(regions) +
				" and source_code='" + src + "'";
		List<RiskFactor> list = query(sql);
		for(RiskFactor rf:list) {
			delete(rf.getId());
		}
	}

	public void deleteForRegionIdAndSource(String regionId, String src) {
		List<RiskFactor> toDelete = findByRegionIdAndSource(regionId, src);
		if(toDelete==null || toDelete.size()==0) {
			return;
		}
		ps.deleteByRiskFactor(toDelete);
		deleteByRiskFactors(toDelete);
	}

	private void deleteByRiskFactors(List<RiskFactor> rfs) {
		for(RiskFactor rf:rfs) {
			delete(rf.getId());
		}
	}

	public void deleteForRegions(List<Region> regions) {
		List<RiskFactor> toDelete = findByRegions(regions);
		ps.deleteByRiskFactor(toDelete);
		for(RiskFactor rf:toDelete) {
			delete(rf.getId());
		}
	}

	public void deleteForIndicators(List<Indicator> indicators) {
		List<RiskFactor> toDelete = findByIndicators(indicators);
		ps.deleteByRiskFactor(toDelete);
		for(RiskFactor rf:toDelete) {
			delete(rf.getId());
		}
	}

	private List<RiskFactor> findByRegionsAndSource(List<Region> regions, String src){
		String sql = initQuery() + getWhereStatementForRegions(regions) +
				" and source_code='" + src + "'";
		return query(sql);
	}

	private List<RiskFactor> findByRegionIdAndSource(String regionId, String src){
		String sql = initQuery() + " where region_id='" + regionId +
				"' and source_code='" + src + "'";
		return query(sql);
	}

	private List<RiskFactor> findByRegions(List<Region> regions){
		String sql = initQuery() + getWhereStatementForRegions(regions);
		return query(sql);
	}

	private List<RiskFactor> findByIndicators(List<Indicator> indicators){
		String sql = initQuery() + getWhereStatementForIndicators(indicators);
		return query(sql);
	}


	private String getWhereStatementForRegions(List<Region> regions) {
		String sql = " where region_id in (";
		for(Region r:regions) {
			sql += r.getId() + ",";
		}
		sql = sql.substring(0,sql.length()-1);
		sql += ")";
		return sql;
	}

	private String getWhereStatementForIndicators(List<Indicator> indicators) {
		String sql = " where indicator_id in (";
		for(Indicator i:indicators) {
			sql += i.getId() + ",";
		}
		sql = sql.substring(0,sql.length()-1);
		sql += ")";
		return sql;
	}
}
