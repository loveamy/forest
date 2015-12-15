package com.miracle9.game.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.common.entity.Desk;
import com.miracle9.game.bean.DeskSeat;
import com.miracle9.game.bean.UserDesk;
import com.miracle9.game.service.DeskResultService;
import com.miracle9.game.service.DeskService;
import com.miracle9.game.service.GameDataLogService;
import com.miracle9.game.service.LevelInfoService;
import com.miracle9.game.service.MagnificationService;
import com.miracle9.game.service.SystemConfigService;
import com.miracle9.game.service.UserService;
import com.miracle9.game.service.UserTopService;
import com.miracle9.game.socket.Data;
import com.miracle9.game.socket.JSONMessageSend;
import com.miracle9.game.util.LocalMem;
import com.miracle9.game.util.MyUtil;

/**
 * 虚拟押注线程,每秒钟运行一次，进行虚拟押注， 把虚拟押注后的桌子总额推送给桌子上的玩家
 */
@Service("virtualBetThreadControler")
public class VirtualBetThreadControler extends Thread {

	private Logger logger = Logger.getLogger(VirtualBetThreadControler.class);

	@Autowired
	private UserService userService;

	@Autowired
	private DeskService deskService;

	@Autowired
	private MagnificationService magnificationService;

	@Autowired
	private LevelInfoService levelInfoService;

	@Autowired
	private DeskResultService deskResultService;

	@Autowired
	private UserTopService userTopService;

	@Autowired
	private SystemConfigService systemConfigService;

	@Autowired
	private GameDataLogService gameDataLogService;

	@PostConstruct
	public void startThis() {
		start();
	}

	@Override
	public void run() {
		while (true) {
			virtualBet();
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
			}
		}
	}

	/**
	 * @param times
	 *            第几次运行
	 */
	private void virtualBet() {
		try {
			for (Entry<Integer, UserDesk> entry : LocalMem.desk_user_result.entrySet()) {
				int deskId = entry.getKey();
				Desk desk = deskService.getDesk(deskId);
				if (desk == null) {// 桌子已经删除
					continue;
				}
				UserDesk userDesk = entry.getValue();
//				Map<String, Object> sendData = new HashMap<String, Object>();
				// 如果到了下局开始和结束时间之间，开始进行虚拟押注
				if (userDesk.resulttime - System.currentTimeMillis() < userDesk.desk.getBetTime() * 1000) {

					String virtualBetCountStr = desk.getVirtualBet();
					String[] virtualBetCountArray = virtualBetCountStr.split("\\|");
					String chipStr = desk.getChip();
					String[] chipArray = chipStr.split("\\|");
					// 一共运行多少次
					int[] virtualBetCount = MyUtil.stringArrayToIntArray(virtualBetCountArray);

					/*
					 * 0-红色狮子 1-绿色狮子 2-黄色狮子 3-红色熊猫 4-绿色熊猫 5-黄色熊猫 6-红色猴子 7-绿色猴子
					 * 8-黄色猴子 9-红色兔子 10-绿色兔子 11-黄色兔子 12-庄 13-和 14-闲
					 */

					// step1.随机选择该房间一个筹码进行12门满的下注 （不带庄闲和）
					if (userDesk.virtualBetCount < virtualBetCount[0]) {
						int randomChip = getRandomChip(chipArray);
						for (int i = 0; i < userDesk.virtualDeskBet.length; i++) {
							// 不带庄和闲
							if (i <= 11) {
								userDesk.virtualDeskBet[i] = userDesk.virtualDeskBet[i] + randomChip;
							}
						}
					}

					// step2.随机选择该房间一个筹码进行9门(放弃兔子）下注 （不带庄闲和）
					if (userDesk.virtualBetCount < virtualBetCount[1]) {
						int randomChip = getRandomChip(chipArray);
						for (int i = 0; i < userDesk.virtualDeskBet.length; i++) {
							// 不带庄和闲，放弃兔子
							if (i < 9) {
								userDesk.virtualDeskBet[i] = userDesk.virtualDeskBet[i] + randomChip;
							} else {
								break;
							}
						}
					}

					// step3.随机选择该房间一个筹码进行9门（放弃狮子） （不带庄闲和）
					if (userDesk.virtualBetCount < virtualBetCount[2]) {
						int randomChip = getRandomChip(chipArray);
						for (int i = 3; i < userDesk.virtualDeskBet.length; i++) {
							// 不带庄和闲，放弃狮子
							if (i <= 11) {
								userDesk.virtualDeskBet[i] = userDesk.virtualDeskBet[i] + randomChip;
							} else {
								break;
							}
						}
					}

					// step4.随机选择该房间一个筹码进行6门（放弃狮子熊猫） （不带庄闲和）
					if (userDesk.virtualBetCount < virtualBetCount[3]) {
						int randomChip = getRandomChip(chipArray);
						for (int i = 6; i < userDesk.virtualDeskBet.length; i++) {
							// 不带庄和闲，放弃狮子熊猫
							if (i <= 11) {
								userDesk.virtualDeskBet[i] = userDesk.virtualDeskBet[i] + randomChip;
							} else {
								break;
							}
						}
					}
					// step5.随机选择该房间一个筹码进行6门（放弃兔子猴子） （不带庄闲和）
					if (userDesk.virtualBetCount < virtualBetCount[4]) {
						int randomChip = getRandomChip(chipArray);
						for (int i = 0; i < userDesk.virtualDeskBet.length; i++) {
							// 不带庄和闲，放弃兔子猴子
							if (i <= 5) {
								userDesk.virtualDeskBet[i] = userDesk.virtualDeskBet[i] + randomChip;
							} else {
								break;
							}
						}
					}

					// step6.随机选择该房间一个筹码随机挑选2-6门进行13下注 （不带庄闲和）
					if (userDesk.virtualBetCount < virtualBetCount[5]) {
						int randomChip = getRandomChip(chipArray);
						int betCount = getRandomInt(2, 6);
						List<Integer> randomIndexArray = new ArrayList<Integer>();

						while (true) {
							int randomIndex = getRandomInt(0, 11);
//							int randomIndex = getRandomInt(0, 12);
//							if (randomIndex == 12) {
//								randomIndex = 13;
//							}
							if (!randomIndexArray.contains(randomIndex)) {
								randomIndexArray.add(randomIndex);
							}
							if (randomIndexArray.size() == betCount) {
								break;
							}
						}

						for (Integer betIndex : randomIndexArray) {
							userDesk.virtualDeskBet[betIndex] = userDesk.virtualDeskBet[betIndex] + randomChip;
						}
					}
					userDesk.virtualBetCount++;

					// 虚拟下注后，结合用户真实下注数据，返回给每一个用户
					// 统计本桌所有押注信息
					int[] deskBet = MyUtil.getDeskVirtualTotalBet(userDesk);

					for (Entry<Integer, DeskSeat> userSeatEntry : LocalMem.userid_seat_map.entrySet()) {
						if (userSeatEntry.getValue().getDeskId() == deskId) {
							IoSession s = LocalMem.onlineUsers.get(userSeatEntry.getKey());
							JSONMessageSend.queue.put(new Data(s, "deskTotalBet", new Object[] { deskBet }));
						}
					}
					// 每秒同步一次玩家游戏币
					userService.refreshUser(deskId);
				} else {
					userDesk.virtualBetCount = 0;
					userDesk.virtualDeskBet = new int[15];
				}
			}
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	/**
	 * 从传入的筹码数组中随机选择一个筹码
	 * 
	 * @param chipArray
	 * @return
	 */
	private int getRandomChip(String[] chipArray) {
		Random random = new Random();
		int index = Math.abs(random.nextInt()) % chipArray.length;
		return Integer.parseInt(chipArray[index]);
	}

	/**
	 * 选择最小的筹码
	 * 
	 * @param chipArray
	 * @return
	 */
//	private int getMinChip(String[] chipArray) {
//		int min = Integer.MAX_VALUE;
//		for (String chip : chipArray) {
//			int c = Integer.parseInt(chip);
//			if (c < min) {
//				min = c;
//			}
//		}
//		return min;
//	}

	/**
	 * 随机获取start到end的整型，包括start和end
	 * 
	 * @return
	 */
	private int getRandomInt(int start, int end) {
		Random random = new Random();
		while (true) {
			int index = Math.abs(random.nextInt()) % (end + 1);
			if (index >= start && index <= end) {
				return index;
			}
		}
	}

}
