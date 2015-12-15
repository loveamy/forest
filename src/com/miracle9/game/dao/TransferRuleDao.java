package com.miracle9.game.dao;

import org.hibernate.Query;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import com.miracle9.game.entity.TransferRule;

@Repository
public class TransferRuleDao extends BaseDao<TransferRule, Integer> {
	/**
	 * 更新系统配置
	 * 
	 * @param config
	 */
	@CacheEvict(value = "springCache", key = "'getTransferRule'")
	public void updateTransferRule(TransferRule transferRule) {
		String hql = "update TransferRule set minRemainGold=:minRemainGold,minTransferGold=:minTransferGold,feePercent=:feePercent";
		Query query = getSession().createQuery(hql);
		query.setProperties(transferRule);
		query.executeUpdate();
	}

	/**
	 * 获取系统配置
	 * 
	 * @return
	 */
	@Cacheable(value = "springCache", key = "'getTransferRule'")
	public TransferRule getTransferRule() {
		return queryByHql("from TransferRule");
	}
}
