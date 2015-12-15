package com.miracle9.game.service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.game.dao.UserTopDao;
import com.miracle9.game.entity.UserTop;
import com.miracle9.game.util.MyUtil;

@Service("userTopService")
public class UserTopService {
	@Autowired
	private UserTopDao userTopDao;

	// 大奖排行 gameType 1-六狮 2-捕鱼 3-单挑 4-万炮捕鱼
	// topListType 1-日排行 2-周排行 3-总排行
	public Map<String, Object> topList(int gameType, int topListType, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("gameType", gameType);
		result.put("topListType", topListType);
		List<UserTop> tops = userTopDao.topList(gameType - 1, topListType);
		result.put("userTop", tops.toArray(new UserTop[] {}));
		return result;
	}
	
	public void addUserTop(UserTop userTop) {
		userTopDao.addUserTop(userTop);
		// 通知管理后台刷新排行榜
		//ManageSocketConnect.sendData("gameService/refreshUserTop", new Object[] { 0 });
	}

	/**
	 * 统计大于传入游戏币的排行榜数量
	 * 
	 * @param gold
	 * @return
	 */
	public int countUserTop(int gold) {
		String datetime = MyUtil.dateToString2(new Date());
		// 进了日排行榜才保持数据
		int topCount = userTopDao.queryTotalCount("select count(*) from UserTop where type=0 and gold>? and datetime>=?", gold, datetime);
		return topCount;
	}

}
