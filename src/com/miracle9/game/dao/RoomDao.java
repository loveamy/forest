package com.miracle9.game.dao;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import com.miracle9.game.entity.Room;

@Repository
public class RoomDao extends BaseDao<Room, Integer> {

	/**
	 * 添加房间桌子数量
	 * 
	 * @param deskNum
	 */
	@CacheEvict(value = "springCache", key = "'getRoomById'+#id")
	public void addRoomDesk(int deskNum, int id) {
		String hql = "update Room set deskNum = deskNum + ? where id = ?";
		createQuery(hql, deskNum, id).executeUpdate();
	}

	@Cacheable(value = "springCache", key = "'getRoomById'+#id")
	public Room getRoomById(int id) {
		return queryById(id);
	}
}
