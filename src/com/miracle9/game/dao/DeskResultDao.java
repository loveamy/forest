package com.miracle9.game.dao;

import java.util.List;

import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import com.miracle9.common.entity.DeskResult;
import com.miracle9.game.bean.Pager;

@Repository
public class DeskResultDao extends BaseDao<DeskResult, Integer> {

	/**
	 * 获取桌子开奖结果
	 * 
	 * @param deskId
	 * @param startDate
	 * @param endDate
	 * @param pageNumber
	 * @param pageSize
	 * @return
	 */
	public Pager getDeskResult(int deskId, String startDate, String endDate, Pager pager) {
		return queryPagerByHql("from DeskResult where deskId = ? and datetime>=? and datetime<?", pager, deskId,
				startDate, endDate);
	}

	/**
	 * 删除过期数据
	 * 
	 * @param date
	 */
	public void deletePastResult(String date) {
		createQuery("delete from DeskResult where datetime<?", date).executeUpdate();
	}
	
		/**
	 * 获取桌子开奖结果
	 * 
	 * @param deskId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<DeskResult> resultList(int deskId) {
		Query query = createQuery("from DeskResult where deskId=? order by id desc", deskId);
		query.setMaxResults(21);
		return query.list();
	}


}
