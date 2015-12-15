package com.miracle9.game.dao;

import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import com.miracle9.game.entity.UserAward;

@Repository
public class UserAwardDao extends BaseDao<UserAward, Integer> {

	/**
	 * 删除指定日期的数据
	 * 
	 * @param date
	 */
	public void deleteByDate(String date) {
		createQuery("delete from UserAward where datetime like ?", date + "%").executeUpdate();
	}

	// /**
	// * 统计月份每天赠送金币
	// *
	// * @param date
	// * @return
	// */
	// @SuppressWarnings("unchecked")
	// public List<Object[]> sumMonthAward(String date) {
	// Query query =
	// createQuery("select substring(datetime,1,10),sum(gold) from UserAward where datetime like ? group by substring(datetime,1,10)",
	// date + "%");
	// return query.list();
	// }
	/**
	 * 统计一天的赠送
	 * @param date
	 * @return
	 */
	public int sumDayAward(String date) {
		Query query = createQuery("select sum(gold) from UserAward where datetime like ?", date + "%");
		Object sum = query.uniqueResult();
		if(sum == null){
			return 0;
		}
		return ((Number) sum).intValue();
	}

	/**
	 * 删除过期数据(小于date)
	 * 
	 * @param date
	 */
	public void deletePastAward(String date) {
		createQuery("delete from UserAward where datetime<?", date).executeUpdate();
	}

	/**
	 * 统计日期内所有赠送游戏币
	 * 
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public int sumByDate(String startDate, String endDate) {
		Object count = createQuery("select sum(gold) from UserAward where datetime>=? and datetime<?", startDate,
				endDate).uniqueResult();
		if (count == null) {
			return 0;
		}
		return Integer.parseInt(count.toString());
	}

}
