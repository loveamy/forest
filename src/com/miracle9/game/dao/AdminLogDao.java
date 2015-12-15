package com.miracle9.game.dao;

import org.apache.mina.core.session.IoSession;
import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import com.miracle9.game.entity.Admin;
import com.miracle9.game.entity.AdminLog;
import com.miracle9.game.util.LocalMem;

@Repository
public class AdminLogDao extends BaseDao<AdminLog, Integer> {

	/**
	 * 获取管理员最后操作记录
	 * 
	 * @param adminId
	 * @return
	 */
	public AdminLog getLastLog(Admin admin) {
		IoSession session = LocalMem.onlineAdmin.get(admin.getId());
		Query query = createQuery("from AdminLog where admin=? order by id desc", admin.getUsername());
		if (session == null)
			query.setFirstResult(1);
		query.setMaxResults(1);
		return (AdminLog) query.uniqueResult();
	}

}
