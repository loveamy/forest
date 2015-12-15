package com.miracle9.game.dao;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import com.miracle9.game.entity.LevelInfo;

@Repository
public class LevelInfoDao extends BaseDao<LevelInfo, Integer> {

	@Cacheable(value = "springCache", key = "'getInfoByLevel'+#level")
	public LevelInfo getInfoByLevel(int level) {
		return queryByHql("from LevelInfo where level=?", level);
	}
}
