package com.softactive.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.softactive.grwa.object.Region;

@Repository
public class RegionRepository extends BaseDataRepository<Region> {
	
	@Autowired
	RiskFactorRepository rfs;
	
	@Override
	public String tableName() {
		return "cmn_region";
	}
	
	@Override
	public Region findUnique(Region r) {
		return findRegionByIsoCode(r.getIsoCode());
	}
	public List<Region> getListFrom(String src, String last) {
		String sql = initQuery() + " where (source_code='" + src + "' and id > '" + last + "')";
		return query(sql);
	}

	public List<Region> getRegionsBySource(String src, boolean in) {
		String sql = initQuery() + " where " + src.toLowerCase() + "=" + in;
		return query(sql);
	}

	public Region findRegionByIsoCode(String isoCode) {
		String sql = initQuery() + " where iso_code='" + isoCode + "'";
		List<Region> answer = query(sql);
		if (answer.size() == 1) {
			return answer.get(0);
		}
		return null;
	}
	
	public Region findRegionById(Integer id) {
		String sql = initQuery() + " where id=" + id;
		List<Region> answer = query(sql);
		if (answer.size() == 1) {
			return answer.get(0);
		}
		return null;
	}
	
	public void delete(List<Region> list) {
		for(Region r:list) {
			delete(r.getId());
		}
		rfs.deleteForRegions(list);
	}
	
	protected String getWhereStatement(List<Region> list) {
		String sql = " where id in (";
		for (Region r : list) {
			sql += "'" + r.getId() + "', ";
		}
		sql = sql.substring(0, sql.length() - 2) + ")";
		return sql;
	}
	
	public List<Region> getRegionsByIds(List<String> ids) {
		String sql = initQuery() + getWhereStatementFromIds(ids);
		return query(sql);
	}
	
	protected String getWhereStatementFromIds(List<String> list) {
		String sql = " where id in (";
		for (String s : list) {
			sql += "'" + s + "', ";
		}
		sql = sql.substring(0, sql.length() - 2) + ")";
		return sql;
	}

	public boolean exists(Region r) {
		return findRegionByIsoCode(r.getIsoCode()) != null;
	}

	public void updateDelicate(List<Region> list) {
		for (Region r : list) {
			updateDelicate(r);
		}
	}

	public void updateDelicate(Region r) {
		List<Region> ins = query(initQuery() + " where id=" + r.getId() + " limit 1");
		Region in = null;
		if(ins.size()==1) {
			in = ins.get(0);
		} else {
			save(r);
			return;
		}
		if (r.getLandlocked() != null) {
			in.setLandlocked(r.getLandlocked());
		}
		if (r.getCurrencyCode() != null) {
			in.setCurrencyCode(r.getCurrencyCode());
		}
		if (r.getCurrency() != null) {
			in.setCurrency(r.getCurrency());
		}
		if (r.getName() != null) {
			in.setName(r.getName());
		}
		if (r.getDistance() != null) {
			in.setDistance(r.getDistance());
		}
		if (r.getAdminRegionCode() != null) {
			in.setAdminRegionCode(r.getAdminRegionCode());
		}
		if (r.getCapital() != null) {
			in.setCapital(r.getCapital());
		}
		if (r.getGroupCode() != null) {
			in.setGroupCode(r.getGroupCode());
		}
		if (r.getIncomeCode() != null) {
			in.setIncomeCode(r.getIncomeCode());
		}
		if (r.getLegalOriginCode() != null) {
			in.setLegalOriginCode(r.getLegalOriginCode());
		}
		if (r.getLendingCode() != null) {
			in.setLendingCode(r.getLendingCode());
		}
		if (r.getSubContinentId() != null) {
			in.setSubContinentId(r.getSubContinentId());
		}
		if (r.getRe() != null) {
			in.setRe(r.getRe());
		}
		if (r.getEf() != null) {
			in.setEf(r.getEf());
		}
		if (r.getPr() != null) {
			in.setPr(r.getPr());
		}
		if (r.getAggregate() != null) {
			in.setAggregate(r.getAggregate());
		}
		if (r.getWb() != null) {
			in.setWb(r.getWb());
		}
		if (r.getFr() != null) {
			in.setFr(r.getFr());
		}
		update(in);
	}

	public List<Region> getOnlyCountries() {
		String sql = initQuery() + " where aggregate=0";
		return query(sql);
	}
}
