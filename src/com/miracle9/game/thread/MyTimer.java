package com.miracle9.game.thread;

import java.util.Calendar;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.game.service.AdminLogService;
import com.miracle9.game.service.DeskResultService;
import com.miracle9.game.service.GameService;
import com.miracle9.game.service.NewRegistService;
import com.miracle9.game.service.SystemConfigService;
import com.miracle9.game.service.UserAwardService;
import com.miracle9.game.service.UserService;
import com.miracle9.game.util.MyUtil;

/**
 * 定时执行任务
 */
@Service("myTimer")
public class MyTimer extends Thread {
	private Logger logger = Logger.getLogger(MyTimer.class);
	@Autowired
	private SystemConfigService systemConfigService;
	@Autowired
	private UserService userService;
	@Autowired
	private NewRegistService newRegistService;

	@Autowired
	private DeskResultService deskResultService;
	@Autowired
	private AdminLogService adminLogService;

	@Autowired
	private UserAwardService userAwardService;

	@Autowired
	private GameService gameService;

	@PostConstruct
	public void startThis() {
		start();
	}

	public void run() {
		while (true) {
			try {
				Calendar calendar = Calendar.getInstance();
				int hour = calendar.get(Calendar.HOUR_OF_DAY);
				int minute = calendar.get(Calendar.MINUTE);
				if (hour == 0 && minute == 0) {
					logger.info("开始执行凌晨任务");
					// 刷新排行榜
					for (int i = 0; i < 4; i++) {
						gameService.refreshUserTop(i, null);
					}
					// 重置临时授权数据
					calendar.add(Calendar.DAY_OF_MONTH, -1);
					//String date = MyUtil.dateToString2(calendar.getTime());
					deletePastData();
					sleep(60 * 60000 * 23);
				} else {
					sleep(50000);
				}
			} catch (Exception e) {
				logger.error("", e);
			}
		}
	}

	/**
	 * 删除过期数据
	 */
	private void deletePastData() {
		try {
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -61);
			// 开奖记录 保留2个月内数据。
			deskResultService.deletePastResult(MyUtil.dateToString2(c.getTime()));
			c.add(Calendar.DATE, -120);
			// 系统日志 保留6个月内数据。
			String date = MyUtil.dateToString2(c.getTime());
			adminLogService.deletePastAdminLog(date);

			// 赠送记录 保留6个月内数据。
			userAwardService.deletePastAward(date);
		} catch (Exception e) {
			logger.error("", e);
		}
	}





}
