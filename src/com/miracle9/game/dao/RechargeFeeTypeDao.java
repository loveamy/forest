package com.miracle9.game.dao;

import org.springframework.stereotype.Repository;

import com.miracle9.game.entity.RechargeFeeType;

@Repository
public class RechargeFeeTypeDao extends BaseDao<RechargeFeeType, Integer> {

	public RechargeFeeType queryByFeeType(int feeType){
		return queryByHql("from RechargeFeeType where feeType=?", feeType);
	}

}
