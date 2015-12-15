package com.miracle9.game.dao;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Repository;

import com.miracle9.common.entity.Desk;

@Repository
public class DeskDao extends BaseDao<Desk, Integer> {

	@Cacheable(value = "springCache", key = "'getAllDesk'")
	public List<Desk> getAllDesk() {
		return queryListByHql("from Desk order by id asc,orderBy");
	}

	@Cacheable(value = "springCache", key = "'getDesk'+#id")
	public Desk getDesk(int id) {
		return queryById(id);
	}

	/**
	 * 添加桌子
	 * 
	 * @param desk
	 */
	@Caching(evict = { @CacheEvict(value = "springCache", key = "'getAllDesk'") })
	public void addDesk(Desk desk) {
		add(desk);
	}

	/**
	 * 更新桌子
	 * 
	 * @param desk
	 */
	@Caching(evict = { @CacheEvict(value = "springCache", key = "'getDesk'+#desk.id"),
			@CacheEvict(value = "springCache", key = "'getAllDesk'") })
	public void updateDesk(Desk desk) {
		Desk update = getDesk(desk.getId());
		update.setName(desk.getName());
		update.setMinGold(desk.getMinGold());
		update.setMinBet(desk.getMinBet());
		update.setMaxBet(desk.getMaxBet());
		// update.setMin_zxh(desk.getMin_zxh());
		// update.setMax_zx(desk.getMax_zx());
		update.setMaxGold(desk.getMaxGold());
		// update.setMax_h(desk.getMax_h());
		update.setBetTime(desk.getBetTime());
		update.setExchange(1);
		update.setAnimalDiff(desk.getAnimalDiff());
		// update.setZxhDiff(desk.getZxhDiff());
		update.setBeilvType(desk.getBeilvType());
		update.setBeilvModel(desk.getBeilvModel());
		update.setSiteType(desk.getSiteType());
		update.setWaterType(desk.getWaterType());
		update.setWaterValue(desk.getWaterValue());
		update.setAutoKick(desk.getAutoKick());
		update.setChip(desk.getChip());
		update.setVirtualBet(desk.getVirtualBet());

		update.setType(desk.getType());
		update.setHideBet(desk.getHideBet());
		update(update);
	}

	/**
	 * 删除桌子
	 * 
	 * @param id
	 */
	@Caching(evict = { @CacheEvict(value = "springCache", key = "'getDesk'+#id"),
			@CacheEvict(value = "springCache", key = "'getAllDesk'") })
	public void deleteDesk(int id) {
		delete(id);
	}

	/**
	 * 清零桌子总押注总得分
	 * 
	 * @param id
	 */
	@CacheEvict(value = "springCache", key = "'getDesk'+#id")
	public void cleanDeskData(int id) {
		// createQuery("update Desk set sumYaFen = 0,sumDeFen=0,sumZhxYaFen=0,sumZhxDeFen=0 where id=?",
		// id)
		// .executeUpdate();
		createQuery("update Desk set sumYaFen = 0,sumDeFen=0 where id=?", id).executeUpdate();
	}

	/**
	 * 更新桌子排序
	 * 
	 * @param id
	 * @param index
	 */
	@Caching(evict = { @CacheEvict(value = "springCache", key = "'getAllDesk'") })
	public void updateOrderBy(int id, int index) {
		createQuery("update Desk set orderBy = ? where id=?", index, id).executeUpdate();
	}

	/**
	 * 增加压分得分
	 * 
	 * @param deskId
	 * @param sumYaFen
	 * @param sumDeFen
	 */
	@CacheEvict(value = "springCache", key = "'getDesk'+#deskId")
	public void addScore(int deskId, long sumYaFen, long sumDeFen, long sumZhxYaFen, long sumZhxDeFen) {
		// createQuery("update Desk set sumYaFen=sumYaFen+?,sumDeFen=sumDeFen+?,sumZhxYaFen=sumZhxYaFen+?,sumZhxDeFen=sumZhxDeFen+? where id=?",
		// sumYaFen, sumDeFen, sumZhxYaFen, sumZhxDeFen, deskId)
		// .executeUpdate();
		createQuery("update Desk set sumYaFen=sumYaFen+?,sumDeFen=sumDeFen+? where id=?", sumYaFen, sumDeFen, deskId)
				.executeUpdate();
	}

	/**
	 * 刷新桌子缓存
	 */
	@CacheEvict(value = "springCache", key = "'getDesk'+#id")
	public void refreshDeskCache(int id) {

	}
}
