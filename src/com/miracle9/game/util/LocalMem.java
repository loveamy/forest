package com.miracle9.game.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.core.session.IoSession;

import com.miracle9.game.bean.DeskSeat;
import com.miracle9.game.bean.ForceAward;
import com.miracle9.game.bean.RoomDesk;
import com.miracle9.game.bean.RoomStatus;
import com.miracle9.game.bean.UserDesk;
import com.miracle9.game.service.AdminService;

public class LocalMem {

	/**
	 * 在线用户
	 */
	public static Map<Integer, IoSession> onlineUsers = new ConcurrentHashMap<Integer, IoSession>();

	public static Map<Integer, String> animal_map = new HashMap<Integer, String>();

	public static Map<Integer, String> animal_map_en = new HashMap<Integer, String>();

	/**
	 * 在线用户和session的映射
	 */
	public static Map<IoSession, Integer> online_session_userId_map = new ConcurrentHashMap<IoSession, Integer>();

	public static Map<Integer, ForceAward> forceAward = new ConcurrentHashMap<Integer, ForceAward>();

	/**
	 * 加密密钥
	 */
	public static Map<IoSession, String> encrypt_key_map = new HashMap<IoSession, String>();

	/**
	 * 在线管理员列表
	 */
	public static Map<Integer, IoSession> onlineAdmin = new ConcurrentHashMap<Integer, IoSession>();

	/**
	 * FLASH策略文件
	 */
	public static String crossdomain = "";

	/**
	 * 在幸运六狮的玩家
	 */
	public static Map<Integer, Integer> luckUser = new ConcurrentHashMap<Integer, Integer>();

	/**
	 * 用户ID-桌子
	 */
	public static Map<Integer, RoomDesk> userid_desk_map = new ConcurrentHashMap<Integer, RoomDesk>();

	/**
	 * 用户ID-座位
	 */
	public static Map<Integer, DeskSeat> userid_seat_map = new ConcurrentHashMap<Integer, DeskSeat>();

	/**
	 * 桌子-用户押注-开奖结果
	 */
	public static Map<Integer, UserDesk> desk_user_result = new ConcurrentHashMap<Integer, UserDesk>();

	/**
	 * 等待用户输入密码列表
	 */
	public static Map<IoSession, Map<Integer, Long>> waitAdminUser = new ConcurrentHashMap<IoSession, Map<Integer, Long>>();

	/**
	 * 等待直属推广员输入密码兑奖的游戏币数量 key:childPromoterId value:游戏币数量
	 */

	public static Map<Integer, Integer> waitExpiryNum = new ConcurrentHashMap<Integer, Integer>();

	/**
	 * 等待推广员直属会员输入密码兑奖的游戏币数量 key:userId value:游戏币数量
	 */

	public static Map<Integer, Integer> waitUserExpiryNum = new ConcurrentHashMap<Integer, Integer>();

	/**
	 * 桌子参数发生变化
	 */
	public static Map<Integer, Integer> deskStatus = new ConcurrentHashMap<Integer, Integer>();

	/**
	 * 房间状态
	 */
	public static RoomStatus roomStatus = null;

	/**
	 * 桌子限制条件发生变化
	 */
	public static Map<Integer, Integer> deskLimit = new ConcurrentHashMap<Integer, Integer>();

	/**
	 * session的活动时间
	 */
	public static Map<IoSession, Long> session_time = new ConcurrentHashMap<IoSession, Long>();

	/**
	 * session的押注时间
	 */
	public static Map<IoSession, Long> sessionBetTime = new ConcurrentHashMap<IoSession, Long>();

	/**
	 * 中文
	 */
	public static Properties languageZh = new Properties();

	/**
	 * 英文
	 */
	public static Properties languageEn = new Properties();

	/**
	 * 掉线用户和掉线时间
	 */
	public static Map<Integer, Long> offlineUsers = new ConcurrentHashMap<Integer, Long>();

	/**
	 * 头像http路径
	 */
	public static String headUrl = null;

	/**
	 * 头像存储路径
	 */
	public static String headPath = null;

	// 初始化配置信息
	static {
		animal_map.put(0, "红狮子");
		animal_map.put(1, "绿狮子");
		animal_map.put(2, "黄狮子");
		animal_map.put(3, "红熊猫");
		animal_map.put(4, "绿熊猫");
		animal_map.put(5, "黄熊猫");
		animal_map.put(6, "红猴子");
		animal_map.put(7, "绿猴子");
		animal_map.put(8, "黄猴子");
		animal_map.put(9, "红兔子");
		animal_map.put(10, "绿兔子");
		animal_map.put(11, "黄兔子");

		animal_map_en.put(0, "red lion");
		animal_map_en.put(1, "green lion");
		animal_map_en.put(2, "yellow lion");
		animal_map_en.put(3, "red panda");
		animal_map_en.put(4, "green panda");
		animal_map_en.put(5, "yellow panda");
		animal_map_en.put(6, "red monkey");
		animal_map_en.put(7, "green monkey");
		animal_map_en.put(8, "yellow monkey");
		animal_map_en.put(9, "red rabbit");
		animal_map_en.put(10, "green rabbit");
		animal_map_en.put(11, "yellow rabbit");

		// 加载FLASH策略文件
		InputStream in = LocalMem.class.getClassLoader().getResourceAsStream("crossdomain.xml");
		BufferedReader buff = new BufferedReader(new InputStreamReader(in));
		StringBuilder sb = new StringBuilder();
		String row = "";
		try {
			while ((row = buff.readLine()) != null) {
				sb.append(row);
			}
			crossdomain = sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Properties pro = new Properties();
		try {
			pro.load(AdminService.class.getClassLoader().getResourceAsStream("url.properties"));
			headUrl = pro.getProperty("headUrl");
			headPath = pro.getProperty("headPath");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
