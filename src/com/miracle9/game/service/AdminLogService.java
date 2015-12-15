package com.miracle9.game.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.game.dao.AdminLogDao;
import com.miracle9.game.entity.AdminLog;

@Service("adminLogService")
public class AdminLogService {
	@Autowired
	private AdminLogDao adminLogDao;

	public void addAdminLog(AdminLog adminLog) {
		adminLogDao.add(adminLog);
	}

	/**
	 * 删除过期数据(小于date)
	 * 
	 * @param date
	 */
	public void deletePastAdminLog(String date) {
		adminLogDao.createQuery("delete from AdminLog where datetime<?", date).executeUpdate();
	}
}
