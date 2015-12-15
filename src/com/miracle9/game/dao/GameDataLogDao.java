package com.miracle9.game.dao;

import java.util.List;

import org.hibernate.SQLQuery;
import org.springframework.stereotype.Repository;

import com.miracle9.game.entity.GameDataLog;

@Repository
public class GameDataLogDao extends BaseDao<GameDataLog, Integer> {
	
	
	public void clear() {
		getSession().clear();
	}
	
	/**
	 * 删除指定月份的游戏记录
	 * 
	 * @param date
	 */
	public void deleteByDate(String date) {
		createQuery("delete from GameDataLog where datetime like ?", date + "%").executeUpdate();
	}

	// /**
	// * 查询统计所有游戏运行记录
	// *
	// * @param date
	// * @return
	// */
	// @SuppressWarnings("unchecked")
	// public List<GameDataLog> querySumAll(String date, String inStr) {
	// Query query = createQuery(
	// "select datetime,sum(sumYaGold),sum(sumDeGold) from GameDataLog where promoterId in "
	// + inStr
	// + " and datetime like ? group by datetime order by datetime",
	// date + "%");
	// List<GameDataLog> logs = new ArrayList<GameDataLog>();
	// List<Object[]> list = query.list();
	// for (Object[] o : list) {
	// GameDataLog log = new GameDataLog();
	// log.setDatetime(o[0].toString());
	// log.setSumYaGold(Double.parseDouble(o[1].toString()));
	// log.setSumDeGold(Double.parseDouble(o[2].toString()));
	// logs.add(log);
	// }
	// return logs;
	// }

	@SuppressWarnings("unchecked")
	public List<Object[]> queryGameDataLogs(String hql, String date, int type) {
		SQLQuery query = null;
		if (type == -1) {
			query = createSQLQuery(hql, date + "%");
		} else {
			query = createSQLQuery(hql, date + "%", type);
		}
		return query.list();
	}
}
