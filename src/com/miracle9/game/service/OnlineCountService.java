package com.miracle9.game.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.game.dao.OnlineCountDao;
import com.miracle9.game.entity.OnlineCount;

@Service("onlineCountService")
public class OnlineCountService {
	@Autowired
	private OnlineCountDao onlineCountDao;

	public void addOnlineCount(OnlineCount onlineCount) {
		onlineCountDao.addonlineCount(onlineCount);
	}
}
