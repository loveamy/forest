package com.miracle9.game.dao;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import com.miracle9.game.entity.Magnification;

@Repository
public class MagnificationDao extends BaseDao<Magnification, Integer> {

	@Cacheable(value = "springCache", key = "'getBeilvByType'+#type")
	public List<Magnification> getBeilvByType(int type) {
		return queryListByHql("from Magnification where type=?", type);
	}

}
