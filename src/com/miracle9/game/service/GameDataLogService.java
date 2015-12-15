package com.miracle9.game.service;

import java.util.Date;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.game.dao.GameDataLogDao;
import com.miracle9.game.entity.GameDataLog;
import com.miracle9.game.util.MyUtil;

@Service("gameDataLogService")
public class GameDataLogService {
	@Autowired
	private GameDataLogDao gameDataLogDao;

	/**
	 * 添加总玩、总得游戏币
	 * 
	 * @param yaGold
	 * @param deGold
	 */
	public void addDataLog(double yaGold, double deGold) {
		String date = MyUtil.dateToString2(new Date());
		GameDataLog gameLog = gameDataLogDao.queryByHql(
				"from GameDataLog where datetime=? and type=0", date);
		if (gameLog == null) {
			gameLog = new GameDataLog(0, date, yaGold, deGold);
			try {
				gameDataLogDao.add(gameLog);
			} catch (ConstraintViolationException e) {// 违反唯一约束表示数据库有这条记录了
				gameDataLogDao.clear();// 必须清空缓存后才能保存数据
				gameDataLogDao.createQuery(
						"update GameDataLog set sumYaGold=sumYaGold+?,sumDeGold=sumDeGold+?"
								+ " where datetime=? and type=0", yaGold, deGold, date)
						.executeUpdate();
			}
		} else {
			gameDataLogDao.createQuery("update GameDataLog set sumYaGold=sumYaGold+?,sumDeGold=sumDeGold+? where id=?",
					yaGold, deGold, gameLog.getId()).executeUpdate();
		}
	}
}
