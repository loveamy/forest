package com.miracle9.game.dao;

import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import com.miracle9.game.entity.UserLoginBonusLog;

@Repository
public class UserLoginBonusLogDao extends BaseDao<UserLoginBonusLog, Integer> {
	
	
	private void updateStatus(int id, int status){
		String hql = "update UserLoginBonusLog set status=? where id=?";
		Query query = createQuery(hql, status, id);
		query.executeUpdate();
	}
	
	public void updateStatusAsClaimed(int id){
		updateStatus(id, 1);
	}
	

}
