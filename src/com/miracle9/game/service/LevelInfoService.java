package com.miracle9.game.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.game.dao.LevelInfoDao;
import com.miracle9.game.entity.LevelInfo;

@Service("levelInfoService")
public class LevelInfoService {

	@Autowired
	private LevelInfoDao levelInfoDao;
	
	public LevelInfo getInfoByLevel(int level){
		return levelInfoDao.getInfoByLevel(level);
	}
}
