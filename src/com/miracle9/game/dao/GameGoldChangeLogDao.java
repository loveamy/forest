package com.miracle9.game.dao;

import org.springframework.stereotype.Repository;

import com.miracle9.game.bean.GoldChangeType;
import com.miracle9.game.entity.GameGoldChangeLog;
import com.miracle9.game.util.MyUtil;


@Repository
public class GameGoldChangeLogDao extends BaseDao<GameGoldChangeLog, Integer> {
	
	public void addLog(int userId, String userName, int beforeGold, int changeGold, int afterGold, GoldChangeType changeType, String remark){
		GameGoldChangeLog log = new GameGoldChangeLog();
		log.setUserId(userId);
		log.setUserName(userName);
		log.setBeforeGold(beforeGold);
		log.setChangeGold(changeGold);
		log.setAfterGold(afterGold);
		log.setChangeTime(MyUtil.getCurrentTimestamp());
		log.setChangeType(changeType.getType());
		log.setRemark(remark);
		this.add(log);
	}
	
}
