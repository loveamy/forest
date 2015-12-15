package com.miracle9.game.thread;

import java.util.Iterator;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.common.entity.User;
import com.miracle9.game.bean.DeskSeat;
import com.miracle9.game.service.GameService;
import com.miracle9.game.service.UserService;
import com.miracle9.game.util.LocalMem;

/**
 * 处理未活动session
 */
@Service("sessionThread")
public class SessionThread extends Thread {
	private Logger logger = Logger.getLogger(SessionThread.class);

	@Autowired
	private UserService userService;
	
	@Autowired
	private GameService gameService;

	@PostConstruct
	public void startThis() {
		start();
	}

	public void run() {
		while (true) {
			try {
				// 处理无反应的session
				Iterator<IoSession> it = LocalMem.session_time.keySet().iterator();
				while (it.hasNext()) {
					IoSession key = it.next();
					Long value = LocalMem.session_time.get(key);
					if (System.currentTimeMillis() - value > 32000) {
						key.close(true);
						if (LocalMem.online_session_userId_map.containsKey(key)) {
							logger.error("超时清除状态" + key.getAttribute("userName"));
							Integer userId = LocalMem.online_session_userId_map.remove(key);
							LocalMem.onlineUsers.remove(userId);
							// 发送下线通知个管理后台服务端
							//ManageSocketConnect.sendData("gameService/logout", new Object[] { userId, 0 });
							gameService.logout(userId, 0);
							
							if (LocalMem.userid_desk_map.containsKey(userId)) {
								LocalMem.userid_desk_map.remove(userId);
							}
							if (LocalMem.userid_seat_map.containsKey(userId)) {// 在座位上
								DeskSeat ds = LocalMem.userid_seat_map.remove(userId);
								userService.refreshUser(ds.getDeskId());
							}
							if (LocalMem.sessionBetTime.containsKey(key)) {
								LocalMem.sessionBetTime.remove(key);
							}
							//将用户标记为离线
							userService.setUserOffLine(userId);
							User user = userService.getUser(userId);
							if(user.getUserType()==2){//游客下线后要回收帐号
								UserService.visitorAccounts.put(userId, user);
							}
						}
						it.remove();
					}
				}
				sleep(1000);
			} catch (Exception e) {
				logger.error("", e);
			}
		}
	}
}
