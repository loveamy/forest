package com.miracle9.game.dao;

import org.hibernate.Query;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import com.miracle9.game.entity.SystemConfig;

@Repository
public class SystemConfigDao extends BaseDao<SystemConfig, Integer> {

	/**
	 * 更新系统配置
	 * 
	 * @param config
	 */
	@CacheEvict(value = "springCache", key = "'getSystemConfig'")
	public void updateConfig(SystemConfig config) {
		String hql = "update SystemConfig set userCheck=:userCheck,helpContent=:helpContent,notActive=:notActive,gameStatus=:gameStatus,openLuckGame=:openLuckGame,operationStatus=:operationStatus,operationDate=:operationDate,operationStopDate=:operationStopDate,jiujiIntervalSecond=:jiujiIntervalSecond,jiujiLessThanGold=:jiujiLessThanGold,jiujiGameGold=:jiujiGameGold";
		Query query = getSession().createQuery(hql);
		query.setProperties(config);
		query.executeUpdate();
	}

	/**
	 * 获取系统配置
	 * 
	 * @return
	 */
	@Cacheable(value = "springCache", key = "'getSystemConfig'")
	public SystemConfig getSystemConfig() {
		return queryByHql("from SystemConfig");
	}

	/**
	 * 更新运营信息
	 * 
	 * @param status
	 * @param startDate
	 * @param endTime
	 */
	@CacheEvict(value = "springCache", key = "'getSystemConfig'")
	public void updateOperateInfo(int status, String startDate, long endTime) {
		String hql = "update SystemConfig set operationStatus=?,operationDate=?,operationStopDate=?";
		createQuery(hql, status, startDate, endTime).executeUpdate();
	}

	/**
	 * 更新
	 * 
	 * @param status
	 * @param startDate
	 * @param endTime
	 */
	@CacheEvict(value = "springCache", key = "'getSystemConfig'")
	public void editHelpContent(String helpContent) {
		String hql = "update SystemConfig set helpContent=?";
		createQuery(hql, helpContent).executeUpdate();
	}

	/**
	 * 更新游戏的开放状态
	 * 
	 * @param type
	 * @param state
	 */
	@CacheEvict(value = "springCache", key = "'getSystemConfig'")
	public void updateGameOpenStatus(int type, int status) {
		String hql = null;
		if (type == 0) {
			hql = "update SystemConfig set openLuckGame=?";
		} else if (type == 1) {
			hql = "update SystemConfig set openFishGame=?";
		} else if (type == 2) {
			hql = "update SystemConfig set openCardGame=?";
		} else if (type == 3) {
			hql = "update SystemConfig set openBulletGame=?";
		}
		createQuery(hql, status).executeUpdate();
	}

	/**
	 * 更新保单箱密码
	 * 
	 * @param newPwd
	 */
	public void updateBaodanPwd(String newPwd) {
		createQuery("update SystemConfig set baodanPwd = ?", newPwd).executeUpdate();
	}

	/**
	 * 更新保单箱状态
	 * 
	 * @param status
	 */
	public void updateBaodanStatus(int status) {
		createQuery("update SystemConfig set baodanStatus = ?", status).executeUpdate();
	}

	/**
	 * 更新
	 * 
	 * @param status
	 * @param startDate
	 * @param endTime
	 */
	@CacheEvict(value = "springCache", key = "'getSystemConfig'")
	public void updateTip(String tip) {
		String hql = "update SystemConfig set tips=?";
		createQuery(hql, tip).executeUpdate();
	}

}
