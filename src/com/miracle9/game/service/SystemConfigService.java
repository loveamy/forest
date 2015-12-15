package com.miracle9.game.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.common.dao.LoginBonusRuleDao;
import com.miracle9.common.entity.LoginBonusRule;
import com.miracle9.game.dao.AdminDao;
import com.miracle9.game.dao.AdminLogDao;
import com.miracle9.game.dao.SystemConfigDao;
import com.miracle9.game.dao.TransferRuleDao;
import com.miracle9.game.dao.UserAwardDao;
import com.miracle9.game.dao.UserDao;
import com.miracle9.game.entity.Admin;
import com.miracle9.game.entity.AdminLog;
import com.miracle9.game.entity.SystemConfig;
import com.miracle9.game.entity.TransferRule;
import com.miracle9.game.socket.AmfMessageSend;
import com.miracle9.game.socket.Data;
import com.miracle9.game.util.LocalMem;
import com.miracle9.game.util.MyUtil;

@Service("systemConfigService")
public class SystemConfigService extends BaseService {
	@Autowired
	private SystemConfigDao systemConfigDao;

	@Autowired
	private AdminDao adminDao;

	@Autowired
	private AdminLogDao adminLogDao;

	@Autowired
	private UserDao userDao;

	@Autowired
	private DeskService deskService;

	@Autowired
	private UserAwardDao userAwardDao;

	@Autowired
	private TransferRuleDao transferRuleDao;

	@Autowired
	private LoginBonusRuleDao loginBonusRuleDao;

	// 获取系统配置
	public SystemConfig getSystemConfig() {
		return systemConfigDao.getSystemConfig();
	}

	// 设置系统帮助内容
	public Map<String, Object> editHelpContent(String helpContent, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		result.put("message", "");
		SystemConfig sc = systemConfigDao.getSystemConfig();
		if (sc == null) {
			initSystemConfig();
		}
		systemConfigDao.editHelpContent(helpContent);
		return result;
	}

	// 查询系统帮助内容
	public Map<String, Object> getHelpContent(IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		result.put("message", "");
		SystemConfig sc = systemConfigDao.getSystemConfig();
		if (sc == null) {
			initSystemConfig();
			result.put("content", "empty");
		} else {
			result.put("content", sc.getHelpContent());
		}
		return result;
	}

	// 更新系统配置
	public Map<String, Object> updateConfig(SystemConfig config, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		StringBuilder content = new StringBuilder("修改系统设置");
		SystemConfig old = systemConfigDao.getSystemConfig();
		if (old.getUserCheck() != config.getUserCheck()) {
			if (config.getUserCheck() == 0) {
				content.append("，关闭用户检查");
			} else {
				content.append("，开启用户检查");
			}
		}

		if (old.getNotActive() != config.getNotActive()) {
			if (config.getNotActive() == -1) {
				content.append("，修改自动冻结为不设上限");
			} else {
				content.append("，修改自动冻结为" + config.getNotActive());
			}
		}

		systemConfigDao.updateConfig(config);
		result.put("success", true);
		result.put("message", "");
		for (Entry<Integer, IoSession> entry : LocalMem.onlineAdmin.entrySet()) {
			if (!session.equals(entry.getValue()))
				AmfMessageSend.queue.put(new Data(entry.getValue(), "syncConfig", new Object[] { config }));
		}
		Integer adminId = Integer.valueOf(session.getAttribute("id").toString());
		Admin admin = adminDao.queryById(adminId);
		AdminLog adminLog = new AdminLog();
		adminLog.setAdmin(admin.getUsername());
		adminLog.setDatetime(MyUtil.dateToString1(new Date()));
		adminLog.setType(2);
		adminLog.setContent(content.toString());
		adminLogDao.add(adminLog);
		return result;
	}

	// 编辑游戏币赠送规则
	public Map<String, Object> editTransferRule(int minRemainGold, int minTransferGold, int feePercent,
			IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		// 查询是否有游戏币规则
		TransferRule tr = transferRuleDao.getTransferRule();
		if (tr == null) {
			// 新增
			tr = new TransferRule();
			tr.setMinRemainGold(minRemainGold);
			tr.setMinTransferGold(minTransferGold);
			tr.setFeePercent(feePercent);
			transferRuleDao.add(tr);
		} else {
			// 更新
			tr.setFeePercent(feePercent);
			tr.setMinRemainGold(minRemainGold);
			tr.setMinTransferGold(minTransferGold);
			transferRuleDao.update(tr);
		}
		result.put("success", true);
		result.put("message", "");
		return result;
	}

	// 查询游戏币赠送规则
	public Map<String, Object> getTransferRule(IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		result.put("message", "");
		TransferRule tr = transferRuleDao.getTransferRule();
		if (tr == null) {
			result.put("minRemainGold", 0);
			result.put("minTransferGold", 0);
			result.put("feePercent", 0);
		} else {
			result.put("minRemainGold", tr.getMinRemainGold());
			result.put("minTransferGold", tr.getMinTransferGold());
			result.put("feePercent", tr.getFeePercent());
		}
		return result;
	}

	public Map<String, Object> setLoginBonusRule(int oneDayBonus, int twoDayBonus, int threeDayBonus, int fourDayBonus,
			int fiveDayBonus, int sixDayBonus, int sevenDayBonus, int gt7DayBonus, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();

		result.put("success", true);
		result.put("message", "");
		LoginBonusRule lbr = loginBonusRuleDao.getLoginBonusRule();
		if (lbr != null) {
			lbr.setOneDayBonus(oneDayBonus);
			lbr.setTwoDayBonus(twoDayBonus);
			lbr.setThreeDayBonus(threeDayBonus);
			lbr.setFourDayBonus(fourDayBonus);
			lbr.setFiveDayBonus(fiveDayBonus);
			lbr.setSixDayBonus(sixDayBonus);
			lbr.setSevenDayBonus(sevenDayBonus);
			lbr.setGt7DayBonus(gt7DayBonus);
			loginBonusRuleDao.updateLoginBonusRule(lbr);
		} else {
			lbr = new LoginBonusRule();
			lbr.setOneDayBonus(oneDayBonus);
			lbr.setTwoDayBonus(twoDayBonus);
			lbr.setThreeDayBonus(threeDayBonus);
			lbr.setFourDayBonus(fourDayBonus);
			lbr.setFiveDayBonus(fiveDayBonus);
			lbr.setSixDayBonus(sixDayBonus);
			lbr.setSevenDayBonus(sevenDayBonus);
			lbr.setGt7DayBonus(gt7DayBonus);
			loginBonusRuleDao.add(lbr);
		}
		return result;
	}

	public Map<String, Object> getLoginBonusRule(IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("sucess", true);
		result.put("message", "");
		LoginBonusRule lbr = loginBonusRuleDao.getLoginBonusRule();
		int[] dayBonus;
		if (lbr != null) {
			dayBonus = new int[] { lbr.getOneDayBonus(), lbr.getTwoDayBonus(), lbr.getThreeDayBonus(),
					lbr.getFourDayBonus(), lbr.getFiveDayBonus(), lbr.getSixDayBonus(), lbr.getSevenDayBonus(),
					lbr.getGt7DayBonus() };

		} else {
			dayBonus = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
		}

		result.put("dayBonus", dayBonus);
		return result;
	}

	public Map<String, Object> getFreeGoldRule(IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		result.put("message", "");
		SystemConfig sc = systemConfigDao.getSystemConfig();
		if (sc != null) {
			result.put("intervalSecond", sc.getJiujiIntervalSecond());
			result.put("lessThanGold", sc.getJiujiLessThanGold());
			result.put("gameGold", sc.getJiujiGameGold());
		} else {
			result.put("intervalSecond", 0);
			result.put("lessThanGold", 0);
			result.put("gameGold", 0);
		}
		return result;
	}

	public Map<String, Object> setFreeGoldRule(int intervalSecond, int lessThanGold, int gameGold, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		result.put("message", "");
		SystemConfig sc = systemConfigDao.getSystemConfig();
		if (sc != null) {
			sc.setJiujiIntervalSecond(intervalSecond);
			sc.setJiujiLessThanGold(lessThanGold);
			sc.setJiujiGameGold(gameGold);
			systemConfigDao.updateConfig(sc);
		} else {
			initSystemConfig();
		}
		return result;
	}

	// 获取公告
	public String getTip(IoSession session) {
		SystemConfig sc = systemConfigDao.getSystemConfig();
		if (sc == null) {
			initSystemConfig();
		}
		return sc.getTips();
	}

	// 更新公告
	public Map<String, Object> updateTip(String tip, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		result.put("message", "");
		SystemConfig sc = systemConfigDao.getSystemConfig();
		if (sc != null) {
			sc.setTips(tip);
			systemConfigDao.updateTip(tip);
		}
		return result;
	}

	/*
	 * public void pubNotice() { // 所有在线人员退出到登录界面 for (Integer uid :
	 * LocalMem.onlineUsers.keySet()) { XMLMessageSend.queue.put(new
	 * Data(LocalMem.onlineUsers.get(uid), "quitToLogin", new Object[] { 1 }));
	 * } LocalMem.onlineUsers = new ConcurrentHashMap<Integer, IoSession>(); for
	 * (IoSession s : GameSocketHandler.gameClients)
	 * AmfMessageSend.queue.put(new Data(s, "manageService/quitToLogin", new
	 * Object[] { -1, 1 })); LocalMem.luckUser = new ConcurrentHashMap<Integer,
	 * Integer>(); LocalMem.luckDeskInfo = new ConcurrentHashMap<Integer,
	 * List<User>>(); }
	 */

	/*
	 * // 立刻停止运营 public Map<String, Object> stopCooperate(IoSession session) {
	 * Map<String, Object> result = new HashMap<String, Object>(); SystemConfig
	 * config = systemConfigDao.getSystemConfig(); if (config.getGameStatus() ==
	 * 0) { pubNotice(); } config.setGameStatus(1);
	 * config.setOperationStatus(0); result.put("success", true);
	 * result.put("message", ""); GameStatus gs = new GameStatus();
	 * gs.statusIndex = config.getGameStatus(); gs.cooperateMode =
	 * config.getOperationStatus(); gs.cooperateStartDate = "——————";
	 * gs.cooperateEndDate = "——————"; for (Entry<Integer, IoSession> entry :
	 * LocalMem.onlineAdmin.entrySet()) { AmfMessageSend.queue.put(new
	 * Data(entry.getValue(), "syncGameStatus", new Object[] { gs })); } return
	 * result; }
	 */

	private void initSystemConfig() {
		/**
		 * 初始化SystemConfig
		 */
		SystemConfig sc = systemConfigDao.getSystemConfig();
		if (sc == null) {
			sc = new SystemConfig();
			sc.setGameStatus(0);
			sc.setHelpContent("empty");
			sc.setJiujiGameGold(0);
			sc.setJiujiIntervalSecond(0);
			sc.setJiujiLessThanGold(0);
			sc.setOperationDate(MyUtil.getCurrentTimestamp());
			systemConfigDao.add(sc);
		}

	}

}
