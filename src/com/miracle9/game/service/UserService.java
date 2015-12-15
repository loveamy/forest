package com.miracle9.game.service;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.hibernate.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import wox.serial.SimpleWriter;
import wox.serial.XMLUtil;

import com.miracle9.common.dao.LoginBonusRuleDao;
import com.miracle9.common.entity.Desk;
import com.miracle9.common.entity.User;
import com.miracle9.common.util.TimeUtil;
import com.miracle9.game.bean.DeskSeat;
import com.miracle9.game.bean.GoldChangeType;
import com.miracle9.game.bean.RoomDesk;
import com.miracle9.game.bean.Seat;
import com.miracle9.game.bean.UserDesk;
import com.miracle9.game.dao.AdminDao;
import com.miracle9.game.dao.AdminLogDao;
import com.miracle9.game.dao.DeskDao;
import com.miracle9.game.dao.GameGoldChangeLogDao;
import com.miracle9.game.dao.GoldClaimLogDao;
import com.miracle9.game.dao.LevelInfoDao;
import com.miracle9.game.dao.RechargeFeeTypeDao;
import com.miracle9.game.dao.RechargeRecordDao;
import com.miracle9.game.dao.SystemConfigDao;
import com.miracle9.game.dao.TransferRuleDao;
import com.miracle9.game.dao.UserAwardDao;
import com.miracle9.game.dao.UserDao;
import com.miracle9.game.dao.UserMessageDao;
import com.miracle9.game.entity.GoldClaimLog;
import com.miracle9.game.entity.RechargeFeeType;
import com.miracle9.game.entity.RechargeRecord;
import com.miracle9.game.entity.SystemConfig;
import com.miracle9.game.entity.TransferRule;
import com.miracle9.game.entity.UserMessage;
import com.miracle9.game.socket.AmfMessageSend;
import com.miracle9.game.socket.Data;
import com.miracle9.game.socket.JSONMessageSend;
import com.miracle9.game.thread.ThreadPool;
import com.miracle9.game.util.DeskOrder;
import com.miracle9.game.util.LocalMem;
import com.miracle9.game.util.MyUtil;

@Service("userService")
public class UserService extends BaseService {
	private static Logger logger = Logger.getLogger(UserService.class);
	private static String DEFAULT_USER_PASSWORD = "888888";

	/**
	 * 可用的游客帐号
	 */
	public static ConcurrentHashMap<Integer, User> visitorAccounts = new ConcurrentHashMap<Integer, User>();

	@Autowired
	private UserDao userDao;

	@Autowired
	GameGoldChangeLogDao gameGoldChangeLogDao;

	@Autowired
	private SystemConfigDao systemConfigDao;

	@Autowired
	private DeskDao deskDao;

	@Autowired
	private AdminDao adminDao;

	@Autowired
	private LoginBonusRuleDao loginBonusRuleDao;

	@Autowired
	private UserAwardDao userAwardDao;

	@Autowired
	private AdminLogDao adminLogDao;

	@Autowired
	private LevelInfoDao levelInfoDao;

	@Autowired
	private GoldClaimLogDao goldClaimLogDao;

	@Autowired
	private GameService gameService;

	@Autowired
	private TransferRuleDao transferRuleDao;

	@Autowired
	private UserMessageDao userMessageDao;

	@Autowired
	private UserMessageService userMessageService;

	@Autowired
	private RechargeFeeTypeDao rechargeFeeTypeDao;

	@Autowired
	private RechargeRecordDao rechargeRecordDao;

	public User getUser(int id) {
		return userDao.queryById(id);
	}

	// 客户端发送公钥
	public void publicKey(String modulus, String exponent, IoSession session) {
		Map<String, Object> arg = new HashMap<String, Object>();
		String key = MyUtil.generatePassword(16);
		LocalMem.encrypt_key_map.put(session, key);

		key = MyUtil.encodeKeyCsharp(key, modulus, exponent);
		arg.put("key", key);
		arg.put("time", System.currentTimeMillis());
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("method", "sendServerTime");
		map.put("args", new Object[] { arg });
		SimpleWriter w = new SimpleWriter();
		try {
			String str = XMLUtil.element2String(w.write(map));
			byte[] content = str.getBytes("UTF-8");
			IoBuffer bb = IoBuffer.allocate(content.length + 4);
			bb.putInt(content.length);
			bb.put(content);
			bb.flip();
			session.write(bb);
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	// 客户端发送公钥
	public void publicKeyForJSON(String modulus, String exponent, IoSession session) {
		Map<String, Object> arg = new HashMap<String, Object>();
		String key = MyUtil.generatePassword(16);
		LocalMem.encrypt_key_map.put(session, key);

		key = MyUtil.encodeKeyCsharp(key, modulus, exponent);
		arg.put("key", key);
		arg.put("time", System.currentTimeMillis());
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("method", "sendServerTime");
		map.put("args", new Object[] { arg });
		byte[] content = null;
		try {
			content = MyUtil.GSON.toJson(map).getBytes("UTF-8");
		} catch (Exception ex) {
			content = MyUtil.GSON.toJson(map).getBytes();
		}
		// 加密
		IoBuffer bb = IoBuffer.allocate(content.length + 4);
		bb.putInt(content.length);
		bb.put(content);
		bb.flip();
		session.write(bb);
	}

	// 用户登录进入游戏
	public Map<String, Object> userLoginGame(int type, final IoSession session) {

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", false);
		result.put("message", "");
		result.put("user", null);
		int userId = (Integer) session.getAttribute("id");

		User user = userDao.queryById(userId);

		if (user.getStatus() == 1) {// 封号
			result.put("message", "该账号已被封号，如有疑问请联系管理员");
			return result;
		}

		result.put("success", true);
		result.put("user", user);
		List<Desk> allkList = deskDao.getAllDesk();
		List<Desk> deskList = new ArrayList<Desk>();
		for (Desk d : allkList) {
			if (d.getType() == type) {
				deskList.add(d);
			}
			if (type == 1) {
				d.setOnlineNumber(0);
				d.setHeadUrls(new ArrayList<Object>());
				for (Entry<Integer, RoomDesk> entry : LocalMem.userid_desk_map.entrySet()) {
					if (entry.getValue().deskId == d.getId()) {
						d.setOnlineNumber(d.getOnlineNumber() + 1);
						User u = userDao.queryById(entry.getKey());
						Map<String, Object> map = new HashMap<String, Object>();
						map.put("userType", u.getUserType());
						map.put("headUrl", u.getHeadUrl());
						d.getHeadUrls().add(map);
					}
				}
			}
		}
		if (type == 1) {// 人数多的在最前面
			Collections.sort(deskList, new DeskOrder());
		}
		result.put("desk", deskList.toArray());
		user.setOnlineStatus("森林舞会");
		userDao.update(user);
		return result;
	}

	// 用户登录
	public Map<String, Object> userLogin(String username, String password, final IoSession session) {

		String timeStamp = TimeUtil.getCurrentTimestamp();
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", false);
		result.put("message", "");
		result.put("user", null);
		result.put("hasClaimedLoginBonus", false);
		result.put("loginBonus", 0);
		User user = userDao.queryByHql("from User where userName=?", username);
		if (user == null || !user.getPassword().equals(DigestUtils.md5Hex(password))) {
			result.put("message", "用户名或密码错误");
			return result;
		}
		if (user.getStatus() == 1) {// 封号
			result.put("message", "对不起,您的账号存在异常行为,无法登入,有疑问请联系客服");
			return result;
		}
		if (user.getStatus() == 2) { // 删号
			result.put("message", "对不起,您的账号存在异常行为,无法登入,有疑问请联系客服");
			return result;
		}

		// 判断是否是重复登录
		if (LocalMem.onlineUsers.containsKey(user.getId())) {
			// 重复登录，通知前一次登录的session退出登录
			quitToLoginNotice(user.getId(), 4);
		}

		// LoginBonusRule lbr = loginBonusRuleDao.getLoginBonusRule();
		//
		// // 判断用户连续登录天数
		// if (StringUtils.isBlank(user.getLoginDate())) {
		// user.setContinueLoginDays(1);
		// user.setHasClaimedLoginBonus(0);
		// user.setLoginBonus(lbr.getOneDayBonus());
		// result.put("loginBonus", lbr.getOneDayBonus());
		// result.put("hasClaimedLoginBonus", false);
		// result.put("continueClamingDays", 1);
		//
		// } else {
		// // 判断上次登录是不是昨天
		// if (TimeUtil.isYesterday(user.getLoginDate(), timeStamp)) {
		// // 如果是昨天，则连续登录天数加1
		// user.setContinueLoginDays(user.getContinueLoginDays() + 1);
		// // 计算登录总天数带来的奖励
		// int continuedDays = user.getContinueLoginDays();
		// if (continuedDays > 7) {
		// user.setLoginBonus(lbr.getGt7DayBonus());
		// } else if (continuedDays == 7) {
		// user.setLoginBonus(lbr.getSevenDayBonus());
		// } else if (continuedDays == 6) {
		// user.setLoginBonus(lbr.getSixDayBonus());
		// } else if (continuedDays == 5) {
		// user.setLoginBonus(lbr.getFiveDayBonus());
		// } else if (continuedDays == 4) {
		// user.setLoginBonus(lbr.getFourDayBonus());
		// } else if (continuedDays == 3) {
		// user.setLoginBonus(lbr.getThreeDayBonus());
		// } else if (continuedDays == 2) {
		// user.setLoginBonus(lbr.getTwoDayBonus());
		// } else if (continuedDays <= 1) {
		// user.setLoginBonus(lbr.getOneDayBonus());
		// }
		// user.setHasClaimedLoginBonus(0);
		// result.put("loginBonus", user.getLoginBonus());
		// result.put("hasClaimedLoginBonus", false);
		// result.put("continueClamingDays", user.getContinueLoginDays());
		// } else if (TimeUtil.isSameToday(user.getLoginDate(), timeStamp)) {
		// // 判断上次登录是不是今天，如果是的话，不设置重复登录天数和奖金
		// result.put("loginBonus", user.getLoginBonus());
		// result.put("hasClaimedLoginBonus", user.getHasClaimedLoginBonus() ==
		// 0 ? false : true);
		// result.put("continueClamingDays", user.getContinueLoginDays());
		// } else {
		// // 如果不是昨天，则连续登录天数重置为1
		// user.setContinueLoginDays(1);
		// user.setLoginBonus(lbr.getOneDayBonus());
		// user.setHasClaimedLoginBonus(0);
		// result.put("loginBonus", lbr.getOneDayBonus());
		// result.put("hasClaimedLoginBonus", false);
		// result.put("continueClamingDays", 1);
		//
		// }
		// }

		session.setAttribute("userName", user.getUserName());
		session.setAttribute("id", user.getId());
		LocalMem.online_session_userId_map.put(session, user.getId());
		LocalMem.onlineUsers.put(user.getId(), session);

		result.put("success", true);
		result.put("user", user);
		user.setOnlineStatus("大厅");
		if (user.getLuckNum() == 0) {
			user.setLuckNum(new Random().nextInt(8) + 1);
		}
		user.setLoginDate(timeStamp);
		userDao.update(user);

		result.put("userInfoModified", user.getUserInfoModified());
		SystemConfig conf = systemConfigDao.getSystemConfig();
		result.put("tip", conf == null ? "" : conf.getTips());

		readMessageNotice(user.getId(), user.getHasMessageToRead(), session);

		return result;
	}

	// 游客登录
	public Map<String, Object> visitorLogin(String nickName, IoSession session) {
		String timeStamp = TimeUtil.getCurrentTimestamp();
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", false);
		result.put("message", "");
		result.put("user", null);
		result.put("hasClaimedLoginBonus", false);
		result.put("loginBonus", 0);
		if (visitorAccounts.size() < 100) {
			ThreadPool.createVisitorAccount(100);
		}
		int userId = 0;
		for (User u : visitorAccounts.values()) {
			if (u != null) {
				userId = u.getId();
				visitorAccounts.remove(userId);
				break;
			}
		}
		User user = userDao.queryById(userId);
		if (user == null) {
			result.put("message", "帐号不存在");
			return result;
		}
		if (user.getStatus() == 1) {// 封号
			result.put("message", "对不起,您的账号存在异常行为,无法登入,有疑问请联系客服");
			return result;
		}
		if (user.getStatus() == 2) { // 删号
			result.put("message", "对不起,您的账号存在异常行为,无法登入,有疑问请联系客服");
			return result;
		}
		user.setNickName(nickName);

		// 判断是否是重复登录
		if (LocalMem.onlineUsers.containsKey(user.getId())) {
			// 重复登录，通知前一次登录的session退出登录
			quitToLoginNotice(user.getId(), 4);
		}

		// LoginBonusRule lbr = loginBonusRuleDao.getLoginBonusRule();
		//
		// // 判断用户连续登录天数
		// if (StringUtils.isBlank(user.getLoginDate())) {
		// user.setContinueLoginDays(1);
		// user.setHasClaimedLoginBonus(0);
		// user.setLoginBonus(lbr.getOneDayBonus());
		// result.put("loginBonus", lbr.getOneDayBonus());
		// result.put("hasClaimedLoginBonus", false);
		// result.put("continueClamingDays", 1);
		//
		// } else {
		// // 判断上次登录是不是昨天
		// if (TimeUtil.isYesterday(user.getLoginDate(), timeStamp)) {
		// // 如果是昨天，则连续登录天数加1
		// user.setContinueLoginDays(user.getContinueLoginDays() + 1);
		// // 计算登录总天数带来的奖励
		// int continuedDays = user.getContinueLoginDays();
		// if (continuedDays > 7) {
		// user.setLoginBonus(lbr.getGt7DayBonus());
		// } else if (continuedDays == 7) {
		// user.setLoginBonus(lbr.getSevenDayBonus());
		// } else if (continuedDays == 6) {
		// user.setLoginBonus(lbr.getSixDayBonus());
		// } else if (continuedDays == 5) {
		// user.setLoginBonus(lbr.getFiveDayBonus());
		// } else if (continuedDays == 4) {
		// user.setLoginBonus(lbr.getFourDayBonus());
		// } else if (continuedDays == 3) {
		// user.setLoginBonus(lbr.getThreeDayBonus());
		// } else if (continuedDays == 2) {
		// user.setLoginBonus(lbr.getTwoDayBonus());
		// } else if (continuedDays <= 1) {
		// user.setLoginBonus(lbr.getOneDayBonus());
		// }
		// user.setHasClaimedLoginBonus(0);
		// result.put("loginBonus", user.getLoginBonus());
		// result.put("hasClaimedLoginBonus", false);
		// result.put("continueClamingDays", user.getContinueLoginDays());
		// } else if (TimeUtil.isSameToday(user.getLoginDate(), timeStamp)) {
		// // 判断上次登录是不是今天，如果是的话，不设置重复登录天数和奖金
		// result.put("loginBonus", user.getLoginBonus());
		// result.put("hasClaimedLoginBonus", user.getHasClaimedLoginBonus() ==
		// 0 ? false : true);
		// result.put("continueClamingDays", user.getContinueLoginDays());
		// } else {
		// // 如果不是昨天，则连续登录天数重置为1
		// user.setContinueLoginDays(1);
		// user.setLoginBonus(lbr.getOneDayBonus());
		// user.setHasClaimedLoginBonus(0);
		// result.put("loginBonus", lbr.getOneDayBonus());
		// result.put("hasClaimedLoginBonus", false);
		// result.put("continueClamingDays", 1);
		//
		// }
		// }

		session.setAttribute("userName", user.getUserName());
		session.setAttribute("id", user.getId());
		LocalMem.online_session_userId_map.put(session, user.getId());
		LocalMem.onlineUsers.put(user.getId(), session);

		result.put("success", true);
		result.put("user", user);
		user.setOnlineStatus("大厅");
		user.setLoginDate(timeStamp);
		userDao.update(user);

		result.put("userInfoModified", user.getUserInfoModified());
		SystemConfig conf = systemConfigDao.getSystemConfig();
		result.put("tip", conf == null ? "" : conf.getTips());

		readMessageNotice(user.getId(), user.getHasMessageToRead(), session);

		return result;
	}

	/**
	 * 登录后执行
	 * 
	 * @param session
	 */
	public void afterLogin(IoSession session) {
		int userId = (Integer) session.getAttribute("id");
		User user = userDao.queryById(userId);
		readMessageNotice(userId, user.getHasMessageToRead(), session);
	}

	/**
	 * 是否有未读信息，推送给用户
	 * 
	 * @param userId
	 * @param has
	 */
	public static void readMessageNotice(int userId, int hasMessageToRead, IoSession session) {
		JSONMessageSend.queue.put(new Data(session, "hasMessageToRead", new Object[] { hasMessageToRead }));
	}

	public Map<String, Object> addRechargeRecord(int feeType, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		int userId = (Integer) session.getAttribute("id");
		RechargeFeeType rft = rechargeFeeTypeDao.queryByFeeType(feeType);
		User beforeUser = userDao.queryById(userId);
		// 设置用户的游戏币
		userDao.addGameGold(userId, rft.getGold());

		User afterUser = userDao.queryById(userId);

		// 记录用户的游戏币变化
		gameGoldChangeLogDao.addLog(userId, beforeUser.getUserName(), beforeUser.getGameGold(), rft.getGold(),
				afterUser.getGameGold(), GoldChangeType.PAY, "");

		// 插入一条用户的充值记录
		RechargeRecord rr = new RechargeRecord();
		rr.setCreateTime(MyUtil.getCurrentTimestamp());
		rr.setFeeType(feeType);
		rr.setGameGold(rft.getGold());
		rr.setMoney(rft.getMoney());
		rr.setUserId(userId);
		rr.setUserName(beforeUser.getUserName());
		rechargeRecordDao.add(rr);

		result.put("success", true);
		result.put("message", "");
		result.put("gameGold", afterUser.getGameGold());
		return result;
	}

	// 检查用户名是否存在
	public boolean checkUsername(String username, IoSession session) {
		int count = userDao.queryTotalCount("select count(*) from User where user_name=?", username);
		return count > 0 ? true : false;
	}

	// 检查用户名是否存在
	public Map<String, Object> checkInformation(String username, String promoterName, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		int count = userDao.queryTotalCount("select count(*) from User where user_name=?", username);
		if (count > 0) {
			result.put("success", false);
			result.put("messageStatus", 1);
			return result;
		}
		if (promoterName.equals("admin")) {
			result.put("success", true);
			result.put("messageStatus", 0);
			return result;
		}
		if (count <= 0) {
			result.put("success", false);
			result.put("messageStatus", 2);
			return result;
		}
		result.put("success", true);
		result.put("messageStatus", 0);
		return result;
	}

	// 用户注册
	public Map<String, Object> register(String userName, String password, IoSession session) {

		Map<String, Object> result = new HashMap<String, Object>();
		if (StringUtils.isBlank(userName) || StringUtils.isBlank(password)) {
			result.put("success", false);
			result.put("message", "用户名和密码不能为空");
			return result;
		}
		int count = userDao.queryTotalCount("select count(*) from User where user_name=?", userName);
		if (count > 0) {
			result.put("success", false);
			result.put("message", "用户名已经存在");
			return result;
		}
		User user = new User();
		user.setUserName(userName);
		user.setAnswer("");
		user.setRealName("");
		user.setNickName("");
		user.setMobilePhone("");
		user.setQq("");
		user.setQuestion("");
		user.setSignature("");
		user.setHeadUrl("1.png");
		user.setStatus(0);
		user.setPassword(DigestUtils.md5Hex(password));// 加密
		user.setRegistDate(MyUtil.dateToString1(new Date()));
		user.setLoginDate("");
		user.setContinueLoginDays(0);
		user.setLoginBonus(0);
		user.setHasClaimedLoginBonus(0);
		user.setOnlineStatus("离线");
		user.setGameGold(0);
		user.setUserType(0);
		user.setUserInfoModified(0);
		userDao.add(user);

		result.put("success", true);
		result.put("message", "注册成功");
		return result;
	}

	/**
	 * 领取游戏币
	 * 
	 * @param session
	 * @return
	 */
	public Map<String, Object> claimGameGold(String deviceId, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();

		if (StringUtils.isEmpty(deviceId)) {
			result.put("success", false);
			result.put("message", "传入参数错误!");
		}

		Integer userId = (Integer) session.getAttribute("id");
		User user = userDao.queryById(userId);
		GoldClaimLog gcl = goldClaimLogDao.queryLastClaimByUserId(userId, deviceId);
		SystemConfig sc = systemConfigDao.getSystemConfig();
		if (gcl != null) {
			// 检查领取时间间隔
			long currentMillis = System.currentTimeMillis();
			if ((currentMillis - gcl.getCreateTime()) / 1000 <= sc.getJiujiIntervalSecond()) {
				// 判断上次和这次领取的时间间隔是否在设置的范围之内
				result.put("success", false);

				// 计算再过多长时间可以领取
				long minutes = (sc.getJiujiIntervalSecond() - (currentMillis - gcl.getCreateTime()) / 1000) / 60;
				long seconds = (sc.getJiujiIntervalSecond() - (currentMillis - gcl.getCreateTime()) / 1000) % 60;
				result.put("message", minutes == 0 ? "" : (minutes + "分") + seconds + "秒后才能领取");
				return result;
			}
			// 检查用户游戏币是否小于可以领取的游戏币
			if (user.getGameGold() > sc.getJiujiLessThanGold()) {
				result.put("success", false);
				result.put("message", "游戏币超过" + sc.getJiujiLessThanGold() + "无法领取");
				return result;
			}

		}
		// 可以进行领取了
		// step1,给用户的游戏币加上领取的值
		int beforeGameGold = user.getGameGold();
		userDao.addGameGold(userId, sc.getJiujiGameGold());
		user = userDao.queryById(userId);

		// step2,给用户的游戏币变化记录一条数据
		gameGoldChangeLogDao.addLog(userId, user.getUserName(), beforeGameGold, sc.getJiujiGameGold(),
				user.getGameGold(), GoldChangeType.CLAIM, "领取游戏币");

		// step3,插入一条用户领取记录
		GoldClaimLog newLog = new GoldClaimLog();
		newLog.setCreateTime(System.currentTimeMillis());
		newLog.setDeviceId(deviceId);
		newLog.setGold(sc.getJiujiGameGold());
		newLog.setUserId(userId);
		goldClaimLogDao.add(newLog);

		result.put("success", true);
		result.put("message", "");
		result.put("gameGold", user.getGameGold());
		return result;
	}

	public void insertGoldChangeLog(User user, int changeGold, GoldChangeType changeType) {
		gameGoldChangeLogDao.addLog(user.getId(), user.getUserName(), user.getGameGold(), changeGold,
				user.getGameGold() + changeGold, changeType, "");

	}

	// /**
	// * 领取连续登录奖励
	// *
	// * @param session
	// * @return
	// */
	// public Map<String, Object> claimLoginBonus(IoSession session) {
	// Map<String, Object> result = new HashMap<String, Object>();
	// Integer userId = (Integer) session.getAttribute("id");
	// User user = userDao.queryById(userId);
	//
	// // 已经领取成功
	// if (user.getHasClaimedLoginBonus() == 1) {
	// result.put("success", false);
	// result.put("message", "不能重复领取");
	// } else {
	// int bonus = user.getLoginBonus();
	// int oldGameGold = user.getGameGold();
	// if (userDao.addGameGold(userId, bonus) > 0) {
	// // 设置用户已经领取过
	// user = userDao.queryById(userId);
	// user.setHasClaimedLoginBonus(1);
	// userDao.update(user);
	// gameGoldChangeLogDao.addLog(userId, user.getUserName(), oldGameGold,
	// bonus, user.getGameGold(),
	// GoldChangeType.CLAIM, "");
	// // 设置用户游戏币变化记录
	// result.put("success", true);
	// result.put("message", "领取成功");
	// result.put("gameGold", user.getGameGold());
	// } else {
	// result.put("success", false);
	// result.put("message", "领取失败，请联系管理员");
	// }
	// }
	//
	// return result;
	// }

	// 修改密码
	public Map<String, Object> updatePassword(String oldPassword, String newPassword, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();

		Integer userId = (Integer) session.getAttribute("id");
		User user = userDao.queryById(userId);
		if (user.getPassword().equals(DigestUtils.md5Hex(oldPassword))) {
			userDao.updatePassword(userId, DigestUtils.md5Hex(newPassword));
			result.put("success", true);
			result.put("message", "修改成功！");
		} else {
			result.put("success", false);
			result.put("message", "原密码错误！");
		}

		return result;
	}

	// 进入房间
	public Map<String, Object> enterDesk(int id, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();

		Desk desk = deskDao.queryById(id);

		if (desk == null) {
			result.put("success", false);
			result.put("message", "房间已经被删除");
			return result;
		}

		int userId = (Integer) session.getAttribute("id");
		User user = userDao.queryById(userId);
		if (user.getGameGold() < desk.getMinGold()) {
			result.put("success", false);
			result.put("message", "不满足进入条件");
			return result;
		}
		if (user.getGameGold() > desk.getMaxGold() && desk.getMaxGold() != -1) {
			result.put("success", false);
			result.put("message", "请选择游戏币匹配的房间");
			return result;
		}
		// int seatNum = 0;
		// if (desk.getType() == 0) {
		// seatNum = Integer.MAX_VALUE;
		// } else {// 6人房
		// seatNum = 6;
		// }
		// 自动选择一张座位坐下，直到成功
		// for (int i = 0; i < seatNum; i++) {
		result = selectSeat(id, user.getLuckNum(), session);
		if (result != null && (Boolean) result.get("success") == true) {
			RoomDesk rd = new RoomDesk();
			rd.deskId = id;
			rd.roomId = 0;
			LocalMem.userid_desk_map.put(userId, rd);
			user.setOnlineStatus(desk.getName());
			userDao.update(user);
			// break;
		}
		// }
		result.put("hideBet", desk.getHideBet());
		JSONMessageSend.execute(new Data(session, "enterDesk", new Object[] { result }));
		if (result != null && (Boolean) result.get("success") == true) {
			refreshUser(desk.getId());
		}
		return null;
	}

	/*
	 * 退出游戏 1.从当前桌子上退出 2.通知管理后台用户已经退出到大厅
	 */
	public void leaveGame(IoSession session) {
		Integer userId = LocalMem.online_session_userId_map.get(session);
		// 移除用户进入的房间
		LocalMem.userid_desk_map.remove(userId);
		User user = userDao.queryById(userId);
		user.setOnlineStatus("大厅");
		userDao.update(user);
	}

	// 获取桌内详细信息(桌子上)
	// public Seat[] deskInfo(int deskId, IoSession session) {
	// Integer userId = LocalMem.online_session_userId_map.get(session);
	// if (LocalMem.userid_desk_map.containsKey(userId)) {
	// return null;
	// }
	// Desk desk = LocalMem.desk_user_result.get(deskId).desk;
	// // 保存用户进入的桌子
	// // LocalMem.userid_desk_map.put(userId, new RoomDesk(desk.getRoomId(),
	// // deskId));
	// List<Seat> seats = new ArrayList<Seat>();
	// // 用来保存已经添加的桌位
	// List<Integer> existSeat = new ArrayList<Integer>();
	//
	// for (int i = 1; i <= 8; i++) {
	// if (!existSeat.contains(i)) {
	// Seat seat = new Seat();
	// seat.id = i;
	// seat.isFree = true;
	// seat.userNickname = "";
	// seats.add(seat);
	// }
	// }
	// return seats.toArray(new Seat[] {});
	// }

	// 离开桌子(离开桌子上)
	public Map<String, Object> leaveDesk(IoSession session) {

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", "true");
		result.put("message", "");
		Integer userId = LocalMem.online_session_userId_map.get(session);
		int deskId = LocalMem.userid_desk_map.get(userId).deskId;
		// 删除用户占用的桌位
		LocalMem.userid_desk_map.remove(userId);
		LocalMem.userid_seat_map.remove(userId);
		// 删除用户的下注时间
		LocalMem.sessionBetTime.remove(session);

		// 如果到了开奖时间,押注不取消,没有到开奖时间，押注才取消
		UserDesk ud = LocalMem.desk_user_result.get(deskId);
		if (System.currentTimeMillis() < ud.resulttime) {
			// 删除用户的下注
			LocalMem.desk_user_result.get(deskId).userBets.remove(userId);
			// 取消下注
			this.cancelBet(deskId, session);
		}

		// 更新用户的当前状态是在游戏
		User user = userDao.queryById(userId);
		user.setOnlineStatus("森林舞会");
		userDao.update(user);

		refreshUser(deskId);
		result.put("gameGold", user.getGameGold());

		readMessageNotice(user.getId(), user.getHasMessageToRead(), session);

		return result;
	}

	// 选择座位坐下
	public Map<String, Object> selectSeat(int deskId, int seat, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		Integer userId = LocalMem.online_session_userId_map.get(session);
		if (LocalMem.userid_seat_map.containsKey(userId)) {
			return null;
		}
		// Integer synId = deskId;
		// synchronized (synId) {
		// // 判断桌位是否有人
		// for (Entry<Integer, DeskSeat> entry :
		// LocalMem.userid_seat_map.entrySet()) {
		// // 桌子有人了
		// if (entry.getValue().getDeskId() == deskId &&
		// entry.getValue().getSeat() == seat) {
		// result.put("success", false);
		// result.put("betTime", 0);
		// result.put("message", "房间人数已满");
		// return result;
		// }
		// }
		// LocalMem.userid_seat_map.put(userId, new DeskSeat(deskId, seat));
		// }
		LocalMem.userid_seat_map.put(userId, new DeskSeat(deskId, seat));
		LocalMem.sessionBetTime.put(session, System.currentTimeMillis());

		UserDesk userDesk = LocalMem.desk_user_result.get(deskId);
		if (userDesk.resulttime - System.currentTimeMillis() > userDesk.desk.getBetTime() * 1000) {
			result.put("betTime", 0);
			result.put("needWaiting", true);
		} else {
			int betTime = (int) ((userDesk.resulttime - System.currentTimeMillis()) / 1000);
			result.put("betTime", betTime);
			result.put("needWaiting", false);
		}
		result.put("success", true);
		result.put("zxh", userDesk.deskResult.getZxh());
		result.put("pointerLocation", userDesk.lastColorIndex);
		result.put("awardType", userDesk.lastAnimalIndex);
		result.put("colors", userDesk.colors);
		result.put("beilv", userDesk.magnification);
		result.put("choumas", MyUtil.convertChips(userDesk.desk.getChip()));
		return result;
	}

	/**
	 * 刷新座位用户列表和桌子在线人数
	 * 
	 * @param deskId
	 */
	public void refreshUser(int deskId) {
		Desk desk = deskDao.getDesk(deskId);
		if (desk.getType() == 0) {
			return;
		}
		UserDesk userDesk = LocalMem.desk_user_result.get(deskId);
		List<Seat> seats = new ArrayList<Seat>();// 座位上的用户
		for (Entry<Integer, DeskSeat> entry : LocalMem.userid_seat_map.entrySet()) {
			if (entry.getValue().getDeskId() == deskId) {
				User user = userDao.queryById(entry.getKey());
				int[] bet = userDesk.userBets.get(user.getId());
				Seat s = new Seat();
				s.id = entry.getValue().getSeat();
				s.userNickname = user.getNickName();
				s.headUrl = user.getHeadUrl();
				s.userId = user.getId();
				if (bet == null) {
					s.gameGold = user.getGameGold();
				} else {
					s.gameGold = user.getGameGold() - userDesk.sumArry(bet);
				}
				s.userType = user.getUserType();
				seats.add(s);
			}
		}
		// 通知座位和桌子上的人更新用户列表
		for (Entry<Integer, RoomDesk> entry : LocalMem.userid_desk_map.entrySet()) {
			if (deskId == entry.getValue().deskId) {
				IoSession sess = LocalMem.onlineUsers.get(entry.getKey());
				JSONMessageSend.queue.put(new Data(sess, "updateDeskInfo",
						new Object[] { seats.toArray(new Seat[] {}) }));
			}
		}
	}

	// 用户押注
	public void userBet(int index, int score, int deskIdOut, IoSession session) {
		int userId = LocalMem.online_session_userId_map.get(session);
		RoomDesk roomDesk = LocalMem.userid_desk_map.get(userId);
		if (roomDesk == null) {
			return;
		}
		int deskId = roomDesk.deskId;
		UserDesk userDesk = LocalMem.desk_user_result.get(deskId);
		int[] bet = userDesk.userBets.get(userId);
		if (bet == null) {
			bet = new int[15];
		}
		if (score <= 0) {
			JSONMessageSend.queue.put(new Data(session, "currentBet", new Object[] { bet }));
			return;
		}
		// 超过了押注时间
		if (userDesk.resulttime - System.currentTimeMillis() > userDesk.desk.getBetTime() * 1000) {
			JSONMessageSend.queue.put(new Data(session, "currentBet", new Object[] { bet }));
			return;
		}
		// 验证总押注是否超过桌子最大押注
		Desk desk = userDesk.desk;
		int allBet = bet[index] + score;
		if (index < 12) {
			if (allBet > desk.getMaxBet()) {// 单个押注大于桌子最大押注或小于桌子最小押注
				JSONMessageSend.queue.put(new Data(session, "currentBet", new Object[] { bet }));
				return;
			}
		} else {// 庄闲和
//			if (index == 13) {// 和
//				if (allBet > desk.getMax_h()) {
//					JSONMessageSend.queue.put(new Data(session, "currentBet", new Object[] { bet }));
//					return;
//				}
//			} else {
//				if (allBet > desk.getMax_zx()) {
//					JSONMessageSend.queue.put(new Data(session, "currentBet", new Object[] { bet }));
//					return;
//				}
//			}
		}
		// 验证总押注是否超过用户分值
		User user = userDao.queryById(userId);
		allBet = userDesk.sumArry(bet) + score;

		if (allBet > user.getGameGold()) {
			JSONMessageSend.queue.put(new Data(session, "currentBet", new Object[] { bet }));
			return;
		} else {
			bet[index] = bet[index] + score;
			userDesk.userBets.put(userId, bet);
			JSONMessageSend.queue.put(new Data(session, "currentBet", new Object[] { bet }));
			// 统计本桌所有押注信息
			// int[] deskBet = MyUtil.getDeskVirtualTotalBet(userDesk);
			// for (Entry<Integer, DeskSeat> entry :
			// LocalMem.userid_seat_map.entrySet()) {
			// if (entry.getValue().getDeskId() == deskId) {
			// IoSession s = LocalMem.onlineUsers.get(entry.getKey());
			// JSONMessageSend.queue.put(new Data(s, "deskTotalBet", new
			// Object[] { deskBet }));
			// }
			// }
		}
		// refreshUser(deskId);
	}

	// 取消所有押注
	public void cancelBet(int deskIdOut, IoSession session) {
		int userId = LocalMem.online_session_userId_map.get(session);
		RoomDesk roomDesk = LocalMem.userid_desk_map.get(userId);
		if (roomDesk == null) {
			return;
		}
		int deskId = roomDesk.deskId;
		UserDesk userDesk = LocalMem.desk_user_result.get(deskId);
		// 超过了押注时间
		if (userDesk.resulttime - System.currentTimeMillis() > userDesk.desk.getBetTime() * 1000) {
			int bet[] = userDesk.userBets.get(userId);
			if (bet != null) {
				JSONMessageSend.queue.put(new Data(session, "currentBet", new Object[] { bet }));
			}
			return;
		}
		userDesk.userBets.remove(userId);// 取消押注
		JSONMessageSend.queue.put(new Data(session, "currentBet", new Object[] { new int[15] }));
		// 统计本桌所有押注信息
		int[] deskBet = MyUtil.getDeskVirtualTotalBet(userDesk);
		for (Entry<Integer, DeskSeat> entry : LocalMem.userid_seat_map.entrySet()) {
			if (entry.getValue().getDeskId() == deskId) {
				IoSession s = LocalMem.onlineUsers.get(entry.getKey());
				JSONMessageSend.queue.put(new Data(s, "deskTotalBet", new Object[] { deskBet }));
			}
		}
		/*
		 * Desk desk = deskDao.queryById(deskId); User user =
		 * userDao.queryById(userId);
		 * 
		 * JSONMessageSend.queue.put(new Data(session, "newGameScore", new
		 * Object[] { user.getGameGold() }));
		 */
	}

	/**
	 * @param toUserName
	 * @param session
	 * @return
	 */
	public Map<String, Object> giveAwayGameGold(String toUserName, int gameGold, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();

		// step1检查被赠送的账号是否存在
		User toUser = userDao.queryByUserName(toUserName);
		int fromUserId = (Integer) session.getAttribute("id");
		User fromUser = userDao.queryById(fromUserId);
		if (toUser == null) {
			result.put("success", false);
			result.put("message", "对方账号不存在!");
			return result;
		}
		if (toUser.getStatus() != 0) {
			result.put("success", false);
			result.put("message", "对方账号状态异常!");
			return result;
		}
		if (LocalMem.userid_desk_map.containsKey(toUser.getId())) {
			result.put("success", false);
			result.put("message", "对方正在游戏中不能赠送");
			return result;
		}
		// 金币只能赠送给超级玩家，超级玩家可以赠送给所有人
		if (toUser.getUserType() != 1 && fromUser.getUserType() != 1) {
			result.put("success", false);
			result.put("message", "只能赠送给超级玩家");
			return result;
		}

		// step2判断赠送的金额加上费率是否超过了用户的总额
		TransferRule tr = transferRuleDao.getTransferRule();

		int feePercent = 0;
		if (fromUser.getUserType() != 1) {
			if (gameGold < tr.getMinTransferGold()) {
				result.put("success", false);
				result.put("message", "赠送失败，赠送的游戏币不能少于" + tr.getMinTransferGold());
				return result;
			}
			feePercent = tr.getFeePercent();
		}
		int feeCostGold = (int) Math.ceil(gameGold * feePercent / 100);
		int totalCostGold = gameGold + feeCostGold;
		if (fromUser.getGameGold() < totalCostGold) {
			result.put("success", false);
			result.put("message", "您的游戏币不足!");
			return result;
		}
		if (fromUser.getUserType() == 0) {
			if ((fromUser.getGameGold() - totalCostGold) < tr.getMinRemainGold()) {
				result.put("success", false);
				result.put("message", "赠送失败，赠送后保留的游戏币不能少于" + tr.getMinRemainGold());
				return result;
			}
		}

		int fromBeforeGold = fromUser.getGameGold();
		int toBeforeGold = toUser.getGameGold();

		// step3到这里可以发起了赠送了
		if (userDao.addGameGold(fromUserId, -totalCostGold) > 0) {

			userDao.addGameGold(toUser.getId(), gameGold);

			fromUser = userDao.queryById(fromUserId);

			toUser = userDao.queryById(toUser.getId());

			givenGameGoldNotice(toUser.getId(), fromUser.getUserName(), gameGold, toUser.getGameGold());

			UserMessage um = new UserMessage();
			um.setContent("赠送给" + toUserName + " " + gameGold + "游戏币" + ",费用" + totalCostGold + "游戏币");
			um.setDatetime(MyUtil.getCurrentTimestamp());
			um.setSender("admin");
			um.setStatus(0);
			um.setTitle("游戏币赠送");
			um.setType(1);
			um.setUserId(fromUserId);
			userMessageService.sendMessage(toUser.getId(), um);

			UserMessage um2 = new UserMessage();
			um2.setContent("获得 " + fromUser.getUserName() + "赠送" + gameGold + "游戏币");
			um2.setDatetime(MyUtil.getCurrentTimestamp());
			um2.setSender("admin");
			um2.setStatus(0);
			um2.setTitle("获得游戏币赠送");
			um2.setType(1);
			um2.setUserId(toUser.getId());
			userMessageService.sendMessage(fromUser.getId(), um2);

			gameGoldChangeLogDao.addLog(fromUserId, fromUser.getUserName(), fromBeforeGold, -totalCostGold,
					fromUser.getGameGold(), GoldChangeType.GIVE, "赠送给" + toUserName + " " + gameGold + "游戏币" + ",费用"
							+ totalCostGold + "游戏币");

			gameGoldChangeLogDao
					.addLog(toUser.getId(), toUser.getUserName(), toBeforeGold, gameGold, toUser.getGameGold(),
							GoldChangeType.GIVEN, "获得" + fromUser.getUserName() + "赠送" + gameGold + "游戏币");

			result.put("gameGold", fromUser.getGameGold());
			result.put("feeCostGold", (int) feeCostGold);

		} else {
			result.put("success", false);
			result.put("message", "赠送失败!");
			return result;
		}

		result.put("success", true);
		result.put("message", "");
		return result;
	}

	/*
	 * // 上分 public void userCoinIn(int num, IoSession session) { int userId =
	 * LocalMem.online_session_userId_map.get(session); Desk desk = null;
	 * DeskSeat ds = LocalMem.userid_seat_map.get(userId); if (ds == null)
	 * return; desk = deskDao.getDesk(ds.getDeskId()); UserDesk userDesk =
	 * LocalMem.desk_user_result.get(desk.getId()); int shouyi = 0; int bet =
	 * userDesk.sumArry(userDesk.userBets.get(userId));// 总押注 if
	 * (userDesk.resulttime == Long.MAX_VALUE) { shouyi =
	 * userDesk.getScore(userId) - bet;// 收益 } User user =
	 * userDao.queryById(userId);
	 * 
	 * if (num > user.getGameGold()) { return; } userDao.addGameGold(userId,
	 * -num, num * userDesk.desk.getExchange()); user =
	 * userDao.queryById(userId); JSONMessageSend.queue.put(new Data(session,
	 * "newGameGold", new Object[] { user.getGameGold() }));
	 * JSONMessageSend.queue .put(new Data(session, "newGameScore", new Object[]
	 * { user.getGameGold() - bet - shouyi }));
	 * 
	 * }
	 */

	/*
	 * // 下分 public void userCoinOut(int score, IoSession session) { int userId
	 * = LocalMem.online_session_userId_map.get(session); Desk desk = null;
	 * DeskSeat ds = LocalMem.userid_seat_map.get(userId); if (ds == null)
	 * return; desk = deskDao.getDesk(ds.getDeskId()); UserDesk userDesk =
	 * LocalMem.desk_user_result.get(desk.getId()); int shouyi = 0; int bet =
	 * userDesk.sumArry(userDesk.userBets.get(userId));// 总押注 if
	 * (userDesk.resulttime == Long.MAX_VALUE) { shouyi =
	 * userDesk.getScore(userId) - bet;// 收益 } User user =
	 * userDao.queryById(userId);
	 * 
	 * if (score > user.getGameGold() - bet - shouyi) return;
	 * userDao.addGameGold(userId, score / userDesk.desk.getExchange(), -score);
	 * user = userDao.queryById(userId); JSONMessageSend.queue.put(new
	 * Data(session, "newGameGold", new Object[] { user.getGameGold() }));
	 * JSONMessageSend.queue .put(new Data(session, "newGameScore", new Object[]
	 * { user.getGameGold() - bet - shouyi }));
	 * 
	 * }
	 */

//	// 玩家的具体信息
//	public Map<String, Object> playerInfo(int id, IoSession session) {
//		Map<String, Object> result = new HashMap<String, Object>();
//		DeskSeat ds = LocalMem.userid_seat_map.get(id);
//		if (ds == null)
//			return null;
//		UserDesk userDesk = LocalMem.desk_user_result.get(ds.getDeskId());
//		int shouyi = 0;
//		int bet = userDesk.sumArry(userDesk.userBets.get(id));// 总押注
//		if (userDesk.resulttime == Long.MAX_VALUE) {
//			shouyi = userDesk.getScore(id) - bet;// 收益
//		}
//		User user = userDao.queryById(id);
//		result.put("nickname", user.getNickName());
//		result.put("seatId", ds.getSeat());
//		// Desk desk = deskDao.getDesk(ds.getDeskId());
//
//		result.put("userGameScore", user.getGameGold() - bet - shouyi);
//
//		return result;
//	}

	// 退出大厅 到登录界面
	public Map<String, Object> exitHall(IoSession session) {
		Integer userId = Integer.parseInt(session.getAttribute("id").toString());
		if (userId != null) {
			session.removeAttribute("username");
			session.removeAttribute("id");
			LocalMem.onlineUsers.remove(userId);
			LocalMem.online_session_userId_map.remove(session);
			LocalMem.userid_desk_map.remove(userId);
			LocalMem.userid_seat_map.remove(userId);
			LocalMem.sessionBetTime.remove(session);
			User user = userDao.queryById(userId);
			user.setOnlineStatus("离线");
			userDao.update(user);
		}
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		result.put("message", "");
		return result;
	}

	/**
	 * 公共游戏币通知
	 * 
	 * @param user
	 */
	/*
	 * public static void pubGoldNotice(User user) { if
	 * (LocalMem.onlineUsers.containsKey(user.getId())) {
	 * XMLMessageSend.queue.put(new Data(LocalMem.onlineUsers.get(user.getId()),
	 * "newGameGold", new Object[] { user .getGameGold() })); } if
	 * (LocalMem.luckUser.containsKey(user.getId())) { for (IoSession ls :
	 * GameSocketHandler.gameClients) { AmfMessageSend.queue.put(new Data(ls,
	 * "manageService/newGameGold", new Object[] { user.getId(),
	 * user.getGameGold() })); } } }
	 */

	/**
	 * 用户登录后，更新自己的个人信息
	 * 
	 * @param realName
	 * @param mobilePhone
	 * @param qq
	 * @param question
	 * @param answer
	 * @param session
	 * @return
	 */
	public Map<String, Object> updateUserInfo(String realName, String mobilePhone, String qq, String question,
			String answer, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		try {
			int userId = (Integer) session.getAttribute("id");
			User user = userDao.queryById(userId);
			// user.setNickName(nickName);
			user.setRealName(realName);
			user.setMobilePhone(mobilePhone);
			user.setQq(qq);
			user.setQuestion(question);
			user.setUserInfoModified(1);
			user.setAnswer(answer);
			userDao.update(user);
			result.put("success", true);
			result.put("message", "更新成功");
		} catch (Exception ex) {
			result.put("success", false);
			result.put("message", "更新失败");
			result.put("debug", ex.toString());
		}
		return result;
	}

	/**
	 * 公共通知方法
	 * 
	 * @param id
	 *            会员id
	 * @param type
	 *            1表示服务器升级维护 2表示玩家账号被冻结；3表示玩家账号被删除；4表示重复登录 5从后台修改了玩家密码
	 */
	public static void quitToLoginNotice(int id, int type) {
		if (LocalMem.onlineUsers.containsKey(id)) {
			IoSession s = LocalMem.onlineUsers.get(id);
			JSONMessageSend.queue.put(new Data(s, "quitToLogin", new Object[] { type }));
			IoSession se = LocalMem.onlineUsers.remove(id);
			se.removeAttribute("username");
			se.removeAttribute("id");
			LocalMem.online_session_userId_map.remove(se);
			LocalMem.userid_seat_map.remove(id);
			LocalMem.userid_desk_map.remove(id);
			LocalMem.sessionBetTime.remove(se);
			LocalMem.session_time.remove(se);
		}
	}

	public void setUserOffLine(int userId) {
		userDao.setUserOffline(userId);
	}

	/**
	 * 游戏会员在游戏客户端发起充值密码请求
	 * 
	 * @param userName
	 * @param question
	 * @param answer
	 * @param session
	 * @return
	 */
	public Map<String, Object> resetPassword(String userName, String question, String answer, IoSession session) {

		Map<String, Object> result = new HashMap<String, Object>();

		if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(question) || StringUtils.isEmpty(answer)) {
			result.put("success", false);
			result.put("message", "找回密码失败");
		} else {

			User user = userDao.queryByUserName(userName);
			if (user == null) {
				result.put("success", false);
				result.put("message", "用户名不存在");
			} else if (user.getQuestion() != null && user.getAnswer() != null && user.getQuestion().equals(question)
					&& user.getAnswer().equals(answer)) {
				// 重置用户密码为888888
				userDao.updatePassword(user.getId(), DigestUtils.md5Hex(DEFAULT_USER_PASSWORD));
				result.put("success", true);
				result.put("message", "");
				result.put("newPassword", DEFAULT_USER_PASSWORD);
			} else {
				result.put("success", false);
				result.put("message", "找回密码失败");
			}
		}
		return result;

	}

	/**
	 * 像游戏客户端推送最新的用户游戏币数值
	 * 
	 * @param id
	 * @param gameGold
	 */
	public static void userGameGoldNotice(int id, int gameGold) {
		if (LocalMem.onlineUsers.containsKey(id)) {
			IoSession s = LocalMem.onlineUsers.get(id);
			JSONMessageSend.queue.put(new Data(s, "gameGold", new Object[] { gameGold }));
		}
	}

	/**
	 * @param id
	 * @param gameGold
	 */
	public static void givenGameGoldNotice(int id, String fromUserName, int givenGameGold, int newGameGold) {
		if (LocalMem.onlineUsers.containsKey(id)) {
			IoSession s = LocalMem.onlineUsers.get(id);
			JSONMessageSend.queue.put(new Data(s, "givenGameGoldNotice", new Object[] { fromUserName, givenGameGold,
					newGameGold }));
		}
	}

	/**
	 * 通用通知
	 * 
	 * @param id
	 * @param method
	 */
	public static void commonNotice(int id, String method) {
		if (LocalMem.onlineUsers.containsKey(id)) {
			IoSession s = LocalMem.onlineUsers.get(id);
			JSONMessageSend.queue.put(new Data(s, method, new Object[] { id }));
		}
	}

	// 验证用户输入的密码
	public Map<String, Object> tellServerUserPassword(int type, String password, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("nType", type);
		int userId = Integer.parseInt(session.getAttribute("id").toString());
		User user = userDao.queryById(userId);
		Map<String, Object> manage = new HashMap<String, Object>();
		IoSession waitSession = null;
		for (Entry<IoSession, Map<Integer, Long>> entry : LocalMem.waitAdminUser.entrySet()) {
			if (entry.getValue().containsKey(userId)) {
				waitSession = entry.getKey();
			}
		}
		if (user.getPassword().equals(DigestUtils.md5Hex(password))) {
			manage.put("success", true);
			manage.put("type", type);
			manage.put("message", "");
			result.put("bFlag", true);

		} else {// 密码错误
			if (waitSession != null) {
				manage.put("success", false);
				manage.put("message", MyUtil.getText(getLanguage(waitSession), "userInputPwdError"));
				manage.put("type", type);
			}
			result.put("bFlag", false);
		}
		if (waitSession != null) {
			AmfMessageSend.queue.put(new Data(waitSession, "getGameInput", new Object[] { manage }));
			LocalMem.waitAdminUser.remove(waitSession);
			LocalMem.waitUserExpiryNum.remove(userId);
		}
		return result;
	}

	// 心跳请求
	public void heart(IoSession session) {
		LocalMem.session_time.put(session, System.currentTimeMillis());
		JSONMessageSend.queue.put(new Data(session, "heart", new Object[] {}));
	}

	public void addGameGold(int userId, int gameGold) {
		userDao.addGameGold(userId, gameGold);
	}

	// 金币排行榜
	@SuppressWarnings("unchecked")
	public Object[] getGoldRank(IoSession session) {
		Query query = userDao
				.createQuery("from User where userName not in('45681791','123288681','小猫咪') and status=0 order by gameGold desc");
		query.setMaxResults(5);
		List<User> users = query.list();
		List<Object> data = new ArrayList<Object>();
		for (User u : users) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("id", u.getId());
			map.put("nickName", u.getNickName());
			map.put("gameGold", u.getGameGold());
			map.put("headUrl", u.getHeadUrl());
			map.put("userType", u.getUserType());
			data.add(map);
		}
		return data.toArray();
	}

	// 获取用户信息
	public Map<String, Object> getUserBasicInfo(int id, IoSession session) {
		Map<String, Object> map = new HashMap<String, Object>();
		int userId = (Integer) session.getAttribute("id");
		User user = userDao.queryById(id);
		map.put("nickName", user.getNickName());
		map.put("signature", user.getSignature() == null ? "" : user.getSignature());
		map.put("headUrl", user.getHeadUrl() == null ? "" : user.getHeadUrl());
		map.put("gameGold", user.getGameGold());
		map.put("userType", user.getUserType());
		map.put("own", userId == id ? 0 : 1);
		return map;
	}

	// 修改用户信息
	public Map<String, Object> updateUserBasicInfo(int id, String nickname, String signature, String headUrl,
			IoSession session) {
		Map<String, Object> map = new HashMap<String, Object>();
		userDao.executeUpdate("update User set nickName=?,signature=?,headUrl=? where id=?", nickname, signature,
				headUrl, id);
		map.put("success", true);
		map.put("message", "");
		return map;
	}

	// 文本聊天
	public void normalChat(String content, IoSession session) {
		int userId = (Integer) session.getAttribute("id");
		RoomDesk roomDesk = LocalMem.userid_desk_map.get(userId);
		if (roomDesk == null) {
			return;
		}
		DeskSeat selfSeat = LocalMem.userid_seat_map.get(userId);
		for (Entry<Integer, RoomDesk> entry : LocalMem.userid_desk_map.entrySet()) {
			if (entry.getValue().deskId == roomDesk.deskId) {
				IoSession s = LocalMem.onlineUsers.get(entry.getKey());
				JSONMessageSend.queue.put(new Data(s, "normalChat", new Object[] { selfSeat.getSeat(), content }));
			}
		}
	}

	// 表情聊天
	public void expressionChat(int content, IoSession session) {
		int userId = (Integer) session.getAttribute("id");
		RoomDesk roomDesk = LocalMem.userid_desk_map.get(userId);
		if (roomDesk == null) {
			return;
		}
		DeskSeat selfSeat = LocalMem.userid_seat_map.get(userId);
		for (Entry<Integer, RoomDesk> entry : LocalMem.userid_desk_map.entrySet()) {
			if (entry.getValue().deskId == roomDesk.deskId) {
				IoSession s = LocalMem.onlineUsers.get(entry.getKey());
				JSONMessageSend.queue.put(new Data(s, "expressionChat", new Object[] { selfSeat.getSeat(), content }));
			}
		}
	}

	// 上传头像
	public Map<String, Object> uploadHead(String headContent, IoSession session) {
		Map<String, Object> map = new HashMap<String, Object>();
		// int userId = (Integer) session.getAttribute("id");
		String headName = UUID.randomUUID().toString().replace("-", "") + ".jpg";
		try {
			File file = new File(LocalMem.headPath);
			if (!file.exists()) {
				file.mkdir();
			}
			byte[] headBytes = Base64.decodeBase64(headContent);
			// if (headBytes.length > 1000000) {
			// map.put("success", false);
			// map.put("message", "头像太大了");
			// return map;
			// }
			resize(headBytes, 158, 158, LocalMem.headPath + headName);
			// FileOutputStream out = new FileOutputStream(LocalMem.headPath +
			// headName);
			// out.write(headBytes);
			// out.close();
			map.put("success", true);
			map.put("message", "");
			map.put("headUrl", headName);
		} catch (IOException e) {
			logger.error("", e);
			map.put("success", false);
			map.put("message", "上传出错");
		}
		return map;
	}

	/**
	 * 强制压缩/放大图片到固定的大小
	 */
	private void resize(byte[] content, int w, int h, String path) throws IOException {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
		Image img = ImageIO.read(inputStream); // 构造Image对象
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		image.getGraphics().drawImage(img, 0, 0, w, h, null); // 绘制缩小后的图
		FileOutputStream out = new FileOutputStream(path); // 输出到文件流
		ImageIO.write(image, "jpg", out);
		// 可以正常实现bmp、png、gif转jpg
		// JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
		// encoder.encode(image); // JPEG编码
		out.close();
	}

	/**
	 * 程序启动时加载游客帐号
	 */
	public void loadVisitorAccount() {
		List<User> users = userDao.queryListByHql("from User where userType=2 and status = 0");
		for (User u : users) {
			visitorAccounts.put(u.getId(), u);
		}
		if (visitorAccounts.size() < 100) {
			ThreadPool.createVisitorAccount(100);
		}
	}

	/**
	 * 创建游客帐号
	 * 
	 * @param num
	 */
	public void createVisitorAccount(final int num) {
		for (int i = 0; i < num; i++) {
			String username = null;
			while (true) {
				username = MyUtil.createAccount(8);
				int count = userDao.queryTotalCount("select count(*) from User where userName=?", username);
				if (count > 0) {
					continue;
				}
				break;
			}
			User user = new User();
			user.setUserName(username);
			user.setAnswer("");
			user.setRealName("");
			user.setNickName("");
			user.setMobilePhone("");
			user.setQq("");
			user.setQuestion("");
			user.setSignature("");
			user.setHeadUrl("1.png");
			user.setStatus(0);
			user.setPassword(DigestUtils.md5Hex(username));// 加密
			user.setRegistDate(MyUtil.dateToString1(new Date()));
			user.setLoginDate("");
			user.setContinueLoginDays(0);
			user.setLoginBonus(0);
			user.setHasClaimedLoginBonus(0);
			user.setOnlineStatus("离线");
			user.setGameGold(0);
			user.setUserType(2);// 游客
			user.setUserInfoModified(0);
			userDao.add(user);
			visitorAccounts.put(user.getId(), user);
		}
	}

	// 修改幸运号
	public Map<String, Object> changeLuckNum(int luckNum, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		int userId = (Integer) session.getAttribute("id");
		User user = userDao.queryById(userId);
		if (StringUtils.isNotBlank(user.getChangeLuckNumDate())) {
			Date changeDate = MyUtil.stringToDate1(user.getChangeLuckNumDate());
			if (System.currentTimeMillis() - changeDate.getTime() < 60 * 1000 * 60) {
				int passMinute = (int) ((System.currentTimeMillis() - changeDate.getTime()) / 60000);
				result.put("success", false);
				result.put("message", 60 - passMinute + "分钟后才可修改");
				return result;
			}
			user.setChangeLuckNumDate(MyUtil.getCurrentTimestamp());
			user.setLuckNum(luckNum);
			LocalMem.userid_seat_map.get(user.getId()).setSeat(luckNum);
			result.put("success", true);
			result.put("luckNum", luckNum);
			result.put("message", "");
		} else {
			user.setChangeLuckNumDate(MyUtil.getCurrentTimestamp());
			user.setLuckNum(luckNum);
			LocalMem.userid_seat_map.get(user.getId()).setSeat(luckNum);
			result.put("success", true);
			result.put("luckNum", luckNum);
			result.put("message", "");
		}
		return result;
	}
}
