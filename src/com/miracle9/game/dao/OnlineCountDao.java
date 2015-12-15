package com.miracle9.game.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.miracle9.game.entity.OnlineCount;

@Repository
public class OnlineCountDao extends BaseDao<OnlineCount, Integer> {

	public void addonlineCount(OnlineCount onlineCount) {
		add(onlineCount);
	}

	public List<OnlineCount> getOnlineCount(String startDate, String endDate, int type) {
		return queryListByHql("from OnlineCount where type=? and datetime>=? and datetime<? order by id desc", type,
				startDate, endDate);
	}

}
