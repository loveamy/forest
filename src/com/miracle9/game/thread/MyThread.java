package com.miracle9.game.thread;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.game.dao.SystemConfigDao;
import com.miracle9.game.service.SystemConfigService;
import com.miracle9.game.service.UserService;

@Service("myThread")
public class MyThread extends Thread {
	private Logger logger = Logger.getLogger(MyThread.class);

	@Autowired
	private UserService userService;

	@Autowired
	private SystemConfigService systemConfigService;
	
	@Autowired
	private SystemConfigDao systemConfigDao;



	@PostConstruct
	public void startThis() {
		start();
	}

	public void run() {
		while (true) {
			this.weihu();
			try {
				sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}



	/**
	 * 维护和运营时间结束
	 */
	private void weihu() {
		try {
			/*SystemConfig config = systemConfigService.getSystemConfig();
			if (config.getOperationStatus() == 1) {
				// 运营结束
				if (System.currentTimeMillis() > config.getOperationStopDate()) {
					config.setOperationStatus(0);
					config.setGameStatus(1);
					systemConfigService.pubNotice();
					GameStatus gs = new GameStatus();
					gs.statusIndex = 1;
					gs.content = "";
					gs.cooperateMode = config.getOperationStatus();
					gs.cooperateStartDate = "——————";
					gs.cooperateEndDate = "——————";
					for (Entry<Integer, IoSession> entry : LocalMem.onlineAdmin.entrySet()) {
						AmfMessageSend.queue.put(new Data(entry.getValue(), "syncGameStatus", new Object[] { gs }));
					}
					return;
				}
			}*/
			
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	
}
