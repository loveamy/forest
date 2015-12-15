package com.miracle9.game.dao;

import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import com.miracle9.game.entity.GoldClaimLog;


@Repository
public class GoldClaimLogDao extends BaseDao<GoldClaimLog, Integer> {
	
	public GoldClaimLog queryLastClaimByUserId(int userId, String deviceId){
		Query query = createQuery("from GoldClaimLog where userId=? or deviceId=?  order by id desc", userId, deviceId);
		query.setMaxResults(1);
		return (GoldClaimLog) query.uniqueResult();
	}

}
