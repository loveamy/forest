package com.miracle9.game.service;

import org.apache.log4j.Logger;
import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import com.miracle9.game.dao.SystemConfigDao;
import com.miracle9.game.socket.Data;
import com.miracle9.game.socket.JSONMessageSend;
import com.miracle9.game.util.LocalMem;

/**
 * 游戏服务端通信处理
 */
@Service("gameService")
public class GameService {
	private Logger logger = Logger.getLogger(GameService.class);
	public final static int LUCK = 0;
	public final static int FISH = 1;
	public final static int CARD = 2;
	public final static int BULLET = 3;
	@Autowired
	private SystemConfigDao systemConfigDao;


	// 玩家登录游戏
	public void login(int userId, int type, IoSession session) {
		if (type == LUCK) {
			LocalMem.luckUser.put(userId, 0);
		}
		logger.info("login:" + userId + "," + type);
	}

	// 玩家退出游戏
	public void logout(int userId, int type) {
		if (type == LUCK) {
			LocalMem.luckUser.remove(userId);
		}
		logger.info("logout:" + userId + "," + type);
	}

	/*@SuppressWarnings("unchecked")
	public void deskInfo(int deskId, List<User> users, int type) {
		if (type == LUCK) {
			LocalMem.luckDeskInfo.put(deskId, users);
		}
		logger.info("deskInfo:" + deskId + "," + type + "," + users.size());
	}*/

	// 更新排行榜
	@Caching(evict = { @CacheEvict(value = "springCache", key = "'topList'+#type+'1'"),
			@CacheEvict(value = "springCache", key = "'topList'+#type+'2'"),
			@CacheEvict(value = "springCache", key = "'topList'+#type+'3'") })
	public void refreshUserTop(int type, IoSession session) {

	}

	// 心跳请求
	public void heart(IoSession session) {
		LocalMem.session_time.put(session, System.currentTimeMillis());
		JSONMessageSend.queue.put(new Data(session, "heart", new Object[] {}));
		logger.info("heart beat...");
	}
	
	
}
