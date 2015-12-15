package com.miracle9.game.thread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Algorithm.AlgorithmInput;
import Algorithm.AlgorithmOutput;
import Algorithm.LuckyLionAlgor;

import com.miracle9.common.entity.Desk;
import com.miracle9.common.entity.DeskResult;
import com.miracle9.common.entity.User;
import com.miracle9.game.bean.DeskSeat;
import com.miracle9.game.bean.ForceAward;
import com.miracle9.game.bean.GoldChangeType;
import com.miracle9.game.bean.RoomDesk;
import com.miracle9.game.bean.UserDesk;
import com.miracle9.game.dao.GameGoldChangeLogDao;
import com.miracle9.game.entity.DeskAward;
import com.miracle9.game.entity.Magnification;
import com.miracle9.game.service.DeskAwardService;
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

@Service("threadGameControler")
public class ThreadGameControler extends Thread {

	private Logger logger = Logger.getLogger(ThreadGameControler.class);

	private Random random = new Random();

	/**
	 * 46倍率索引
	 */
	private Map<Integer, Integer> beilvIndex = new HashMap<Integer, Integer>();// deskId--index

	/**
	 * 68 78已使用的倍率(控制每三局无重复)
	 */
	private Map<Integer, LinkedList<Integer>> usedBeilv = new HashMap<Integer, LinkedList<Integer>>();// deskId--index

	// /**
	// * 要保存的排行榜 当前时间大于value才保存
	// */
	// private Map<UserTop, Long> addUserTops = new HashMap<UserTop, Long>();

	/**
	 * 桌子算法
	 */
	private Map<Integer, LuckyLionAlgor> algors = new HashMap<Integer, LuckyLionAlgor>();

	/**
	 * 大奖连续出现的次数
	 */
	private static int bigCount = 0;

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

	@Autowired
	GameGoldChangeLogDao gameGoldChangeLogDao;

	@Autowired
	private DeskAwardService deskAwardService;

	@PostConstruct
	public void startThis() {
		start();
	}

	@Override
	public void run() {
		while (true) {
			this.getResult();
			// this.execOffline();
			// this.saveUserTop();
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
			}
		}
	}

	/**
	 * 处理开奖结果
	 */
	private void getResult() {
		try {
			for (Entry<Integer, UserDesk> entry : LocalMem.desk_user_result.entrySet()) {
				int deskId = entry.getKey();
				Desk desk = deskService.getDesk(deskId);
				if (desk == null) {// 桌子已经删除
					for (Entry<Integer, RoomDesk> rd : LocalMem.userid_desk_map.entrySet()) {
						if (rd.getValue().deskId == deskId) {
							IoSession s = LocalMem.onlineUsers.get(rd.getKey());
							JSONMessageSend.queue.put(new Data(s, "quitDesk", new Object[] { 1, "房间已被删除" }));
							if (LocalMem.userid_desk_map.containsKey(rd.getKey())) {
								LocalMem.userid_desk_map.remove(rd.getKey());
								LocalMem.userid_seat_map.remove(rd.getKey());
								LocalMem.sessionBetTime.remove(s);
							}
						}
					}
					LocalMem.desk_user_result.remove(entry.getKey());
					continue;
				}
				UserDesk userDesk = entry.getValue();
				Map<String, Object> sendData = new HashMap<String, Object>();
				// 如果到了开奖时间
				if (System.currentTimeMillis() > userDesk.resulttime) {
					// 从算法接口获取开奖结果
					DeskResult dr = getDeskResult(userDesk, desk);
					if (dr == null)
						continue;
					// 校验颜色 0红,1绿,2黄
					int color = userDesk.colors[userDesk.colorsIndex[0]];
					// 只在普通奖项和全局闪电时校验
					if (dr.getType() == 0 || (dr.getType() == 1 && dr.getGlobalType() == 1)) {
						int animalColor = getColorByAnimal(dr.getAnimal());
						dr.setAnimal(dr.getAnimal() + color - animalColor);
					}

					userDesk.deskResult = dr;
					userDesk.setRestarttime();
					userDesk.resulttime = Long.MAX_VALUE;
					sendData.put("colors", userDesk.colorsIndex);// 转盘每次停止的彩灯索引
					sendData.put("deskResult", userDesk.deskResult);
					sendData.put("incomeInfo", userDesk.getDeskIncome(userService));

					for (Entry<Integer, DeskSeat> us : LocalMem.userid_seat_map.entrySet()) {
						int userId = us.getKey();
						if (us.getValue().getDeskId() == deskId) {
							IoSession session = LocalMem.onlineUsers.get(userId);
							int[] bets = userDesk.userBets.get(userId);// 用户押注数组
							int[] addScore = userDesk.getScore(userId);// 本局得分
							sendData.put("addScore", addScore[0]);
							sendData.put("luckScore", addScore[1]);
							sendData.put("currentBets", bets == null ? new int[15] : bets);
							JSONMessageSend.queue.put(new Data(session, "gameResult", new Object[] { sendData }));
							int sumBet = sumArry(bets);// 总押注
							// 更新押注时间
							if (!LocalMem.sessionBetTime.containsKey(session) || sumBet > 0) {
								if (desk.getAutoKick() == 1) {
									LocalMem.sessionBetTime.put(session, System.currentTimeMillis());
								}
							}
							User user = userService.getUser(userId);
							int score = addScore[0] - sumBet;// 用户收益

							// 同步游戏结束后的玩家游戏币
							JSONMessageSend.queue.put(new Data(session, "newGameGold", new Object[] { user
									.getGameGold() + score }));

							if (bets != null) {
								userService.addGameGold(user.getId(), score);
								// 记录用户的游戏币变化
								userService.insertGoldChangeLog(user, score, GoldChangeType.GAME);
							}

							// 保存大奖排行
							// if (addScore / userDesk.desk.getExchange() > 0) {
							// int gold = addScore /
							// userDesk.desk.getExchange();
							// int topCount = userTopService.countUserTop(gold);
							// if (topCount < 10)
							// addUserTop(userDesk.deskResult, user, addScore /
							// userDesk.desk.getExchange(), us
							// .getValue().getSeat(), userDesk.restarttime);
							// }
							// 保存游戏运行统计
							if (sumBet > 0 || addScore[0] > 0) {
								double yaGold = MyUtil.divide(sumBet, userDesk.desk.getExchange());
								double deGold = MyUtil.divide(addScore[0], userDesk.desk.getExchange());
								gameDataLogService.addDataLog(yaGold, deGold);
							}
						}

					}
					// 保存开奖结果
					userDesk.deskResult.setSumYaFen((int) userDesk.sumYaFen);
					userDesk.deskResult.setSumDeFen((int) userDesk.sumDeFen);
					userDesk.deskResult.setResult((int) (userDesk.sumYaFen - userDesk.sumDeFen));
					userDesk.deskResult.setBetPeople(userDesk.userBets.size());// 押注人数
					deskResultService.addDeskResult(userDesk.deskResult);
					// 更新压分 得分
					deskService.addScore(deskId, userDesk.sumYaFen, userDesk.sumDeFen, userDesk.sumZhxYaFen,
							userDesk.sumZhxDeFen);
				}

				// 如果到了下局开始时间
				if (System.currentTimeMillis() > userDesk.restarttime) {
					// 大奖推送
					sendNotice(userDesk.deskResult, userDesk.desk);
					// SocketConnect.deskOutput.remove(deskId);// 清空上局算法输出结果
					if (LocalMem.deskStatus.containsKey(deskId)) {// 桌子参数发生变化座位上玩家退出到房间
						for (Entry<Integer, DeskSeat> ds : LocalMem.userid_seat_map.entrySet()) {
							if (ds.getValue().getDeskId() == deskId) {
								IoSession s = LocalMem.onlineUsers.get(ds.getKey());
								JSONMessageSend.queue.put(new Data(s, "quitToRoom", new Object[] { 2, "房间参数发生了变化" }));
								LocalMem.userid_seat_map.remove(ds.getKey());
								LocalMem.userid_desk_map.remove(ds.getKey());
								userService.refreshUser(deskId);
								LocalMem.sessionBetTime.remove(s);
							}
						}
						userDesk.desk = desk;
					}
					if (LocalMem.deskLimit.containsKey(deskId) || LocalMem.deskStatus.containsKey(deskId)) {// 桌子限制条件、参数发生变化
						userDesk.desk = desk;
						LocalMem.deskLimit.remove(deskId);
						LocalMem.deskStatus.remove(deskId);
					}
					// 获取倍率
					int[] beilv = getBeilv(desk);
					sendData.put("beilv", beilv);
					sendData.put("zxh", userDesk.deskResult.getZxh());
					int[] colors = MyUtil.getColors();// 24个彩灯颜色
					sendData.put("colors", colors);
					sendData.put("pointerLocation", userDesk.lastColorIndex);
					sendData.put("awardType", userDesk.lastAnimalIndex);
					userDesk.restart();
					userDesk.colors = colors;
					userDesk.magnification = beilv;
					for (Entry<Integer, DeskSeat> us : LocalMem.userid_seat_map.entrySet()) {
						if (us.getValue().getDeskId() == deskId) {
							IoSession session = LocalMem.onlineUsers.get(us.getKey());
							User user = userService.getUser(us.getKey());
							JSONMessageSend.queue.put(new Data(session, "newGameGold", new Object[] { user
									.getGameGold() }));

							// 验证押注时间是否超过10分钟
							Long time = LocalMem.sessionBetTime.get(session);
							if (time != null && System.currentTimeMillis() - time > 600000 && desk.getAutoKick() == 1) {
								// JSONMessageSend.queue.put(new Data(session,
								// "quitToRoom", new Object[] { 3,
								// "长时间没有下注，自动退出房间" }));
								// LocalMem.userid_seat_map.remove(user.getId());
								// LocalMem.userid_desk_map.remove(user.getId());
								// userService.refreshUser(deskId);
								// LocalMem.sessionBetTime.remove(session);
								Map<String, Object> leaveMap = userService.leaveDesk(session);
								JSONMessageSend.execute(new Data(session, "leaveDesk", new Object[] { leaveMap }));
								continue;
							}
							sendData.put("choumas", MyUtil.convertChips(desk.getChip()));
							sendData.put("betTime", (int) ((userDesk.resulttime - System.currentTimeMillis()) / 1000));
							JSONMessageSend.queue.put(new Data(session, "gameRestart", new Object[] { sendData }));

							// 判断玩家的游戏币是否小于最小携带，或者大于最大携带
							if (user.getGameGold() < desk.getMinGold()) {
								quitDeskNotice(user.getId(), session, 4, "游戏币小于房间最小携带");
							}

							if (user.getGameGold() > desk.getMaxGold() && desk.getMaxGold() != -1) {
								quitDeskNotice(user.getId(), session, 5, "游戏币大于房间最大携带");
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	private void quitDeskNotice(int userId, IoSession session, int type, String message) {
		LocalMem.userid_desk_map.remove(userId);
		LocalMem.userid_seat_map.remove(userId);
		LocalMem.sessionBetTime.remove(session);
		JSONMessageSend.queue.put(new Data(session, "quitDesk", new Object[] { type, message }));

	}

	/**
	 * 从算法获取开奖结果
	 * 
	 * @param deskId
	 * @return
	 */
	private DeskResult getDeskResult(UserDesk ud, Desk newDesk) {
		try {
			Desk desk = ud.desk;

			// 检查是否有强制下注
			if (LocalMem.forceAward.containsKey(desk.getId())) {
				ForceAward fa = LocalMem.forceAward.remove(desk.getId());
				DeskResult deskResult = new DeskResult();
				deskResult.setDeskId(desk.getId());
				deskResult.setDatetime(MyUtil.dateToString1(new Date()));
				deskResult.setZxh(getRandomChip());
				deskResult.setAwardGold(0);
				deskResult.setIsForce(1);
				// 转盘停止的24个彩灯位置
				ud.colorsIndex = new int[24];

				List<Integer> colorsIndex = new ArrayList<Integer>();
				for (int k = 0; k < ud.colors.length; k++) {
					if (ud.colors[k] == fa.getColor()) {
						colorsIndex.add(k);
					}
				}

				ud.colorsIndex[0] = colorsIndex.get(MyUtil.getRandom(colorsIndex.size()));

				if (fa.isLuck()) {
					deskResult.setType(0);
					deskResult.setLuckNum(fa.getLuckNum());
					// 随机普通奖的动物 除了狮子
					deskResult.setAnimal(random.nextInt(9) + 3);
					colorsIndex = new ArrayList<Integer>();
					for (int k = 0; k < ud.colors.length; k++) {
						if (ud.colors[k] == getColorByAnimal(deskResult.getAnimal())) {
							colorsIndex.add(k);
						}
					}
					ud.colorsIndex[0] = colorsIndex.get(MyUtil.getRandom(colorsIndex.size()));

					if (fa.getAwardType() == 6) {
						deskResult.setLuckType(2);
						// 幸运送灯
						deskResult.setSongDengCount(fa.getSongDengCount());
						StringBuilder sdInfo = new StringBuilder();
						List<Integer> songDengAnimal = getSongDengAnimal(fa.getSongDengCount());
						int i = 1;
						for (Integer a : songDengAnimal) {
							sdInfo.append(a + ",");

							int color = getColorByAnimal(a);

							colorsIndex = new ArrayList<Integer>();
							for (int k = 0; k < ud.colors.length; k++) {
								if (ud.colors[k] == color) {
									colorsIndex.add(k);
								}
							}

							ud.colorsIndex[i] = colorsIndex.get(MyUtil.getRandom(colorsIndex.size()));

							i++;
						}
						ud.lastColorIndex = ud.colorsIndex[i - 1];
						// 最终停止时主转盘指针指向的动物类型
						ud.lastAnimalIndex = songDengAnimal.get(songDengAnimal.size() - 1);
						deskResult.setMoreInfo(sdInfo.substring(0, sdInfo.length() - 1));

						deskResult.setLightningBeilv(fa.getLightningBeiLv() == 0 ? 1 : fa.getLightningBeiLv());
					} else if (fa.getAwardType() == 7) {
						// 幸运闪电
						deskResult.setLuckType(1);
						deskResult.setLightningBeilv(fa.getLightningBeiLv());
						deskResult.setLuckAnimal(fa.getAnimal());
						// 最终停止时主转盘指针指向的动物类型
						colorsIndex = new ArrayList<Integer>();
						for (int k = 0; k < ud.colors.length; k++) {
							if (ud.colors[k] == getColorByAnimal(deskResult.getLuckAnimal())) {
								colorsIndex.add(k);
							}
						}
						ud.colorsIndex[1] = colorsIndex.get(MyUtil.getRandom(colorsIndex.size()));

						ud.lastAnimalIndex = fa.getAnimal();
						ud.lastColorIndex = ud.colorsIndex[1];
					}
				} else if (fa.getAwardType() == 1) {// 普通奖
					deskResult.setType(0);
					deskResult.setAnimal(fa.getAnimal());
					// 最终停止时主转盘指针指向的动物类型
					ud.lastAnimalIndex = fa.getAnimal();
					ud.lastColorIndex = ud.colorsIndex[0];
				} else {
					deskResult.setType(1);
					if (fa.getAwardType() == 6) {

						// 全局送灯
						deskResult.setGlobalType(2);
						deskResult.setSongDengCount(fa.getSongDengCount());
						StringBuilder sdInfo = new StringBuilder();
						List<Integer> songDengAnimal = getSongDengAnimal(fa.getSongDengCount());
						int i = 0;
						for (Integer a : songDengAnimal) {
							sdInfo.append(a + ",");

							int color = getColorByAnimal(a);

							colorsIndex = new ArrayList<Integer>();
							for (int k = 0; k < ud.colors.length; k++) {
								if (ud.colors[k] == color) {
									colorsIndex.add(k);
								}
							}

							ud.colorsIndex[i] = colorsIndex.get(MyUtil.getRandom(colorsIndex.size()));

							i++;
						}
						ud.lastColorIndex = ud.colorsIndex[i - 1];
						// 最终停止时主转盘指针指向的动物类型
						ud.lastAnimalIndex = songDengAnimal.get(i - 1);
						deskResult.setMoreInfo(sdInfo.substring(0, sdInfo.length() - 1));

						deskResult.setLightningBeilv(fa.getLightningBeiLv());
					} else if (fa.getAwardType() == 7) {
						// 全局闪电

						deskResult.setGlobalType(1);
						deskResult.setAnimal(fa.getAnimal());
						// 最终停止时主转盘指针指向的动物类型
						ud.lastAnimalIndex = fa.getAnimal();
						ud.lastColorIndex = ud.colorsIndex[0];

						deskResult.setLightningBeilv(fa.getLightningBeiLv());
					} else if (fa.getAwardType() == 8) {
						// 全局大三元
						deskResult.setGlobalType(3);
						deskResult.setAnimal(fa.getAnimal());

						int[] xulie = new int[] { 3, 1, 0, 2 };
						int index = xulie[fa.getAnimal() / 3];
						int randomNum = MyUtil.getRandom(7);
						int temp = randomNum * 4 + index;

						int index1 = xulie[ud.lastAnimalIndex / 3];
						int colorIndex = ud.lastColorIndex;

						temp += (colorIndex - index1);

						if (temp > 24) {
							temp = temp - 24;
						}

						ud.colorsIndex[0] = temp;
						ud.lastColorIndex = ud.colorsIndex[0];
						ud.lastAnimalIndex = fa.getAnimal();

						deskResult.setLightningBeilv(fa.getLightningBeiLv());
					} else if (fa.getAwardType() == 9) {
						// 全局大四喜
						deskResult.setGlobalType(4);
						deskResult.setColor(fa.getColor());
						deskResult.setAnimal(fa.getAnimal());
						// 最终停止时主转盘指针指向的动物类型
						ud.lastAnimalIndex = fa.getAnimal();
						ud.lastColorIndex = ud.colorsIndex[0];

						deskResult.setLightningBeilv(fa.getLightningBeiLv());
					}
				}
				deskResult.setLightningBeilv(fa.getLightningBeiLv() == 0 ? 1 : fa.getLightningBeiLv());
				return deskResult;
			}

			AlgorithmInput input = new AlgorithmInput();
			// 用来保存已存在的索引
			List<int[]> allUserBetsList = new ArrayList<int[]>();

			// 初始化八个人，15门下注
			for (int i = 0; i < 8; i++) {
				int[] oneUserBet = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
				allUserBetsList.add(oneUserBet);
			}

			int k = 0;
			// 是否有人押注
			boolean isBet = false;
			for (Integer uid : ud.userBets.keySet()) {
				DeskSeat ds = LocalMem.userid_seat_map.get(uid);
				if (ds == null)
					continue;
				int addIndex = k % 8;
				int[] preUserBets = allUserBetsList.get(addIndex);
				int[] thisUserBets = ud.userBets.get(uid);
				for (int j = 0; j < 15; j++) {
					if (thisUserBets[j] > 0) {
						isBet = true;
					}
					preUserBets[j] = preUserBets[j] + thisUserBets[j];
				}
				allUserBetsList.set(addIndex, preUserBets);
				k++;
			}

			input.nAllUserBets = MyUtil.convertListArrayToArrayArray(allUserBetsList);
			input.nMaxAnimalBet = desk.getMaxBet();
			input.nMinAnimalBet = desk.getMinBet();
			// input.nMaxBankerAndPlayerBet = desk.getMax_zx();
			// input.nMaxTieBet = desk.getMax_h();
			// input.nMinBankerPlayerTieBet = desk.getMin_zxh();
			input.nCoinToScore = desk.getExchange();
			input.nAnimalRate = desk.getAnimalDiff();
			// input.nBankerPlayerTieRate = desk.getZxhDiff();
			input.nWaterType = desk.getWaterType();
			input.nWaterValue = desk.getWaterValue();
			input.nSiteType = desk.getSiteType();
			int[] nRateInFo = new int[12];
			for (int i = 0; i < 12; i++) {
				nRateInFo[i] = ud.magnification[i];
			}
			input.nRateInFo = nRateInFo;
			input.nLightTypeList = ud.colors;
			// input.nTotalZXHYaFen = newDesk.getSumZhxYaFen();
			// input.nTotalZXHDeFen = newDesk.getSumZhxDeFen();
			// input.nTotalAnimalYaFen = newDesk.getSumYaFen() -
			// newDesk.getSumZhxYaFen();
			// input.nTotalAnimalDeFen = newDesk.getSumDeFen() -
			// newDesk.getSumZhxDeFen();
			input.nTotalAnimalYaFen = newDesk.getSumYaFen();
			input.nTotalAnimalDeFen = newDesk.getSumDeFen();

			// SocketConnect.sendAlgIn(desk.getId(), input);
			// AlgorithmOutput output =
			// SocketConnect.deskOutput.get(desk.getId());
			// if (output == null)
			// return null;
			LuckyLionAlgor alg = algors.get(desk.getId());
			if (alg == null) {
				alg = new LuckyLionAlgor(desk.getId());
				algors.put(desk.getId(), alg);
			}
			AlgorithmOutput output = null;

			// 过滤掉幸运奖和全局奖
			while (true) {
				output = alg.RealAlgorithm(input);
				if (output == null) {
					return null;
				} else if (output.nAwardType != 1) {
					if (!isBet) {
						continue;
					}
					DeskAward award = deskAwardService.randomDeskAward(desk.getId());
					if (award == null) {
						continue;
					}
					if (award.getType() == 27) {
						bigCount++;
						if (bigCount < 10) {
							continue;
						}
					}
					bigCount = 0;
					setAlgOutput(output, award.getType(), input.nRateInFo);
					setColors(ud.colors, output);
					break;
				} else {
					break;
				}
			}
			DeskResult deskResult = new DeskResult();
			deskResult.setDeskId(desk.getId());
			deskResult.setDatetime(MyUtil.dateToString1(new Date()));
			deskResult.setZxh(output.nBankerPlayerTieType);
			deskResult.setAwardGold(output.nJackpot);
			// 转盘停止的24个彩灯位置
			ud.colorsIndex = output.nPointerLocation;
			if (output.nAwardType == 1) {// 普通
				deskResult.setType(0);
				deskResult.setAnimal(output.nAnimalType[0]);

				// 指针最后停止的位置
				ud.lastColorIndex = output.nPointerLocation[output.nPointerLocation.length - 1];

				// 最终停止时主转盘指针指向的动物类型
				ud.lastAnimalIndex = output.nAnimalType[output.nAnimalType.length - 1];

			} else if (output.nAwardType <= 4) {// 幸运奖
				deskResult.setType(2);
				deskResult.setLuckNum(output.nLuckyID);
				deskResult.setAnimal(output.nAnimalType[0]);
				// 合并后的新数组
				int[] allPointerLocation = new int[ud.colorsIndex.length + output.nLuckyPointerLocation.length];
				// 把全局彩灯位置和幸运彩灯位置合并
				System.arraycopy(ud.colorsIndex, 0, allPointerLocation, 0, ud.colorsIndex.length);
				System.arraycopy(output.nLuckyPointerLocation, 0, allPointerLocation, ud.colorsIndex.length,
						output.nLuckyPointerLocation.length);
				ud.colorsIndex = allPointerLocation;
				// 指针最后停止的位置
				ud.lastColorIndex = output.nLuckyPointerLocation[output.nLuckyPointerLocation.length - 1];
				// 最终停止时主转盘指针指向的动物类型
				ud.lastAnimalIndex = output.nLuckyAnimalType[output.nLuckyAnimalType.length - 1];
				if (output.nAwardType == 2) {// 幸运闪电
					deskResult.setLuckType(1);
					deskResult.setLightningBeilv(output.nLuckyLightningBet);
					deskResult.setLuckAnimal(output.nLuckyAnimalType[0]);
				} else if (output.nAwardType == 3) {// 幸运彩金
					deskResult.setLuckType(0);
					deskResult.setAwardGold(output.nLuckyJackpot);
					deskResult.setLuckAnimal(output.nLuckyAnimalType[0]);
				} else if (output.nAwardType == 4) {// 幸运送灯
					deskResult.setLuckType(2);
					deskResult.setSongDengCount(output.nLuckySongDengCount);
					StringBuilder sdInfo = new StringBuilder();
					for (int i = 0; i < deskResult.getSongDengCount(); i++) {
						sdInfo.append(output.nLuckyAnimalType[i] + ",");
					}
					deskResult.setMoreInfo(sdInfo.substring(0, sdInfo.length() - 1));
				}
			} else {// 全局奖
				deskResult.setType(1);
				// 指针最后停止的位置
				ud.lastColorIndex = output.nPointerLocation[output.nPointerLocation.length - 1];
				// 最终停止时主转盘指针指向的动物类型
				ud.lastAnimalIndex = output.nAnimalType[output.nAnimalType.length - 1];
				if (output.nAwardType == 5) {// 全局彩金
					deskResult.setGlobalType(0);
					deskResult.setAnimal(output.nAnimalType[0]);
				} else if (output.nAwardType == 6) {// 全局送灯
					deskResult.setGlobalType(2);
					deskResult.setSongDengCount(output.nSongDengCount);
					StringBuilder sdInfo = new StringBuilder();
					for (int i = 0; i < deskResult.getSongDengCount(); i++) {
						sdInfo.append(output.nAnimalType[i] + ",");
					}
					deskResult.setMoreInfo(sdInfo.substring(0, sdInfo.length() - 1));
				} else if (output.nAwardType == 7) {// 全局闪电
					deskResult.setGlobalType(1);
					deskResult.setLightningBeilv(output.nLightningBet);
					deskResult.setAnimal(output.nAnimalType[0]);
				} else if (output.nAwardType == 8) {// 全局大三元
					deskResult.setGlobalType(3);
					deskResult.setAnimal(output.nDaSanYuan);
				} else if (output.nAwardType == 9) {// 全局大四喜
					deskResult.setGlobalType(4);
					deskResult.setColor(output.nDaSiXi);
				}
			}
			deskResult.setIsForce(0);
			if (deskResult.getLightningBeilv() == 0) {
				deskResult.setLightningBeilv(1);
			}
			return deskResult;
		} catch (Exception e) {
			logger.error("", e);
			return new DeskResult();
		}
	}

	/**
	 * 获取倍率
	 * 
	 * @param deskId
	 * @return
	 */
	private int[] getBeilv(Desk desk) {
		int[] returnBeilv = new int[15];
		List<Magnification> beilv = new ArrayList<Magnification>();
		List<Magnification> magn = magnificationService.getBeilvByType(desk.getBeilvType());
		if (desk.getBeilvModel() == 0) {// 固定倍率
			if (desk.getBeilvType() == 0) {
				Integer index = beilvIndex.get(desk.getId());
				if (index == null || index > 2) {
					index = 0;
				}
				for (Magnification m : magn) {
					if (m.getZindex() == index) {
						beilv.add(m);
					}
				}
				beilvIndex.put(desk.getId(), index + 1);
			} else {
				// 处理三局内无重复倍率
				Random random = new Random();
				int index = random.nextInt(4);
				LinkedList<Integer> used = usedBeilv.get(desk.getId());
				if (used == null) {
					used = new LinkedList<Integer>();
					usedBeilv.put(desk.getId(), used);
				}
				while (used.contains(index = random.nextInt(4))) {

				}
				if (used.size() == 3) {
					used.removeLast();
				}
				used.addFirst(index);
				for (Magnification m : magn) {
					if (m.getZindex() == index) {
						beilv.add(m);
					}
				}
			}
		} else {// 打乱
			Random random = new Random();
			int index = 0;
			if (desk.getBeilvType() == 0) {
				index = random.nextInt(3);
			} else {
				index = random.nextInt(4);
			}
			for (Magnification m : magn) {
				if (m.getZindex() == index) {
					beilv.add(m);
				}
			}
			Collections.shuffle(beilv);
		}

		returnBeilv[0] = beilv.get(0).getLion();
		returnBeilv[1] = beilv.get(1).getLion();
		returnBeilv[2] = beilv.get(2).getLion();
		returnBeilv[3] = beilv.get(0).getPanda();
		returnBeilv[4] = beilv.get(1).getPanda();
		returnBeilv[5] = beilv.get(2).getPanda();
		returnBeilv[6] = beilv.get(0).getMonkey();
		returnBeilv[7] = beilv.get(1).getMonkey();
		returnBeilv[8] = beilv.get(2).getMonkey();
		returnBeilv[9] = beilv.get(0).getRabbit();
		returnBeilv[10] = beilv.get(1).getRabbit();
		returnBeilv[11] = beilv.get(2).getRabbit();
		returnBeilv[12] = 2;
		returnBeilv[13] = 8;
		returnBeilv[14] = 2;
		return returnBeilv;
	}

	/**
	 * 大奖播报
	 */
	public void sendNotice(DeskResult deskResult, Desk desk) {
		if (deskResult.getType() == 1 || deskResult.getLuckType() != 0) {
			String message = "";
			String roomName = "";

			// if (deskResult.getGlobalType() == 0) {
			// message = "恭喜：" + roomName + " " + desk.getName() + " 房间开出彩金！";
			// } else
			if (deskResult.getGlobalType() == 1) {
				if (deskResult.getLightningBeilv() <= 3) {
					message = "恭喜：" + roomName + " " + desk.getName() + " 房间开出闪电！";
				} else {
					message = "恭喜：" + roomName + " " + desk.getName() + " 房间开出小猫变身！";
				}
			} else if (deskResult.getGlobalType() == 2) {
				message = "恭喜：" + roomName + " " + desk.getName() + " 房间开出送灯！";
			} else if (deskResult.getGlobalType() == 3) {
				message = "恭喜：" + roomName + " " + desk.getName() + " 房间开出大三元！";
				if (deskResult.getLightningBeilv() == 2) {
					message = "恭喜：" + roomName + " " + desk.getName() + " 房间开出大三元X2！";
				}
			} else if (deskResult.getGlobalType() == 4) {
				message = "恭喜：" + roomName + " " + desk.getName() + " 房间开出大四喜！";
				if (deskResult.getLightningBeilv() == 2) {
					message = "恭喜：" + roomName + " " + desk.getName() + " 房间开出大四喜X2！";
				}
			} else if (deskResult.getLuckType() != 0) {
				message = "恭喜：" + roomName + " " + desk.getName() + " 房间开出幸运号！";
			}
			for (Integer uid : LocalMem.userid_seat_map.keySet()) {
				JSONMessageSend.queue.put(new Data(LocalMem.onlineUsers.get(uid), "sendMarquee",
						new Object[] { message }));
			}
		}
	}

	/**
	 * 大奖排行
	 */
	// public void addUserTop(DeskResult deskResult, User user, int gameGold,
	// int seat, final long saveTime) {
	// if (deskResult.getType() != 0) {
	// String awardName = "";
	// if (deskResult.getType() == 1) {
	// if (deskResult.getGlobalType() == 0) {
	// awardName = "全局彩金";
	// } else if (deskResult.getGlobalType() == 1) {
	// awardName = "全局闪电";
	// } else if (deskResult.getGlobalType() == 2) {
	// awardName = "全局送灯";
	// } else if (deskResult.getGlobalType() == 3) {
	// awardName = "大三元";
	// } else if (deskResult.getGlobalType() == 4) {
	// awardName = "大四喜";
	// }
	// } else if (deskResult.getType() == 2) {
	// if (deskResult.getLuckNum() != seat)
	// return;
	// if (deskResult.getLuckType() == 0) {
	// awardName = "幸运彩金";
	// } else if (deskResult.getLuckType() == 1) {
	// awardName = "幸运闪电";
	// } else if (deskResult.getLuckType() == 2) {
	// awardName = "幸运送灯";
	// }
	// }
	// final UserTop top = new UserTop();
	// top.setUserId(user.getId());
	// top.setNickname(user.getUserName());
	// top.setDatetime(MyUtil.dateToString1(new Date()));
	// top.setAwardName(awardName);
	// top.setGold(gameGold);
	// top.setType(0);
	// addUserTops.put(top, saveTime);
	// }
	// }

	/**
	 * 累加数组元素
	 * 
	 * @param arry
	 * @return
	 */
	public int sumArry(int[] arry) {
		if (arry == null)
			return 0;
		int sum = 0;
		for (int i = 0; i < arry.length; i++) {
			sum += arry[i];
		}
		return sum;
	}

	/**
	 * 保存大家排行到数据库
	 */
	// private void saveUserTop() {
	// try {
	// Iterator<UserTop> it = addUserTops.keySet().iterator();
	// while (it.hasNext()) {
	// UserTop top = it.next();
	// if (System.currentTimeMillis() > addUserTops.get(top)) {
	// userTopService.addUserTop(top);
	// it.remove();
	// }
	// }
	// } catch (Exception e) {
	// logger.error("", e);
	// }
	// }

	/**
	 * 获取随机的庄和闲0-庄1-和2-闲
	 */
	private int getRandomChip() {
		int[] zxh = new int[] { 0, 1, 2 };
		Random random = new Random();
		int index = Math.abs(random.nextInt()) % zxh.length;
		return zxh[index];
	}

	/**
	 * 获取送灯的动物类型
	 * 
	 * @param songDengCount
	 * @return
	 */
	private List<Integer> getSongDengAnimal(int songDengCount) {
		List<Integer> animal = new ArrayList<Integer>();
		Random random = new Random();
		for (int i = 0; i < songDengCount; i++) {
			while (true) {
				int index = Math.abs(random.nextInt()) % 12;
				if (animal.contains(index)) {
					continue;
				} else {
					animal.add(index);
					break;
				}
			}
		}
		return animal;
	}

	/**
	 * 
	 * 0-红色狮子 1-绿色狮子 2-黄色狮子 3-红色熊猫 4-绿色熊猫 5-黄色熊猫 6-红色猴子 7-绿色猴子 8-黄色猴子 9-红色兔子
	 * 10-绿色兔子 11-黄色兔子
	 * 
	 * 根据动物编号，获取到相应的颜色
	 * 
	 * @param animal
	 *            0-11动物
	 * @return color 0红,1绿,2黄
	 */
	private int getColorByAnimal(int animal) {
		if (animal % 3 == 0) {
			return 0;
		} else if ((animal - 1) % 3 == 0) {
			return 1;
		} else {
			return 2;
		}
	}

	/**
	 * 根据大奖类型设置算法输入
	 * 
	 * @param input
	 * @param priceType
	 * @param beilv
	 *            倍率
	 */
	private void setAlgOutput(AlgorithmOutput output, int priceType, int[] beilv) {
		if (priceType == 0) {
			output.nAwardType = 9;
			output.nDaSiXi = 0;
		} else if (priceType == 1) {
			output.nAwardType = 9;
			output.nDaSiXi = 1;
		} else if (priceType == 2) {
			output.nAwardType = 9;
			output.nDaSiXi = 2;
		} else if (priceType == 3) {
			output.nAwardType = 8;
			output.nDaSanYuan = 0;
		} else if (priceType == 4) {
			output.nAwardType = 8;
			output.nDaSanYuan = 1;
		} else if (priceType == 5) {
			output.nAwardType = 8;
			output.nDaSanYuan = 2;
		} else if (priceType == 6) {
			output.nAwardType = 8;
			output.nDaSanYuan = 3;
		} else if (priceType == 7) {
			output.nAwardType = 7;
			output.nAnimalType = new int[] { random.nextInt(3) };
			output.nLightningBet = random.nextInt(2) + 2;
		} else if (priceType == 8) {
			output.nAwardType = 7;
			output.nAnimalType = new int[] { random.nextInt(3) + 3 };
			output.nLightningBet = random.nextInt(2) + 2;
		} else if (priceType == 9) {
			output.nAwardType = 7;
			output.nAnimalType = new int[] { random.nextInt(3) + 6 };
			output.nLightningBet = random.nextInt(2) + 2;
		} else if (priceType == 10) {
			output.nAwardType = 7;
			output.nAnimalType = new int[] { random.nextInt(3) + 9 };
			output.nLightningBet = random.nextInt(2) + 2;
		} else if (priceType == 11) {
			output.nAwardType = 6;
			output.nSongDengCount = 2;
			output.nAnimalType = doSongDeng(2);
		} else if (priceType == 12) {
			output.nAwardType = 6;
			output.nSongDengCount = 3;
			output.nAnimalType = doSongDeng(3);
		} else if (priceType == 13) {
			output.nAwardType = 6;
			output.nSongDengCount = 4;
			output.nAnimalType = doSongDeng(4);
		} else if (priceType == 14) {
			output.nAwardType = 6;
			output.nSongDengCount = 5;
			output.nAnimalType = doSongDeng(5);
		} else if (priceType == 15) {
			output.nAwardType = 6;
			output.nSongDengCount = 6;
			output.nAnimalType = doSongDeng(6);
		} else if (priceType == 16) {
			output.nAwardType = 6;
			output.nSongDengCount = 7;
			output.nAnimalType = doSongDeng(7);
		} else if (priceType == 17) {
			output.nAwardType = 6;
			output.nSongDengCount = 8;
			output.nAnimalType = doSongDeng(8);
		} else if (priceType == 18) {
			output.nAwardType = 6;
			output.nSongDengCount = 9;
			output.nAnimalType = doSongDeng(9);
		} else if (priceType == 19) {
			output.nAwardType = 6;
			output.nSongDengCount = 10;
			output.nAnimalType = doSongDeng(10);
		} else if (priceType == 20) {
			output.nAwardType = 6;
			output.nSongDengCount = 11;
			output.nAnimalType = doSongDeng(11);
		} else if (priceType == 21) {
			output.nAwardType = 6;
			output.nSongDengCount = 12;
			output.nAnimalType = doSongDeng(12);
		} else if (priceType == 22) {
			output.nAwardType = 1;
			output.nAnimalType = new int[] { random.nextInt(3) };
		} else if (priceType == 23) {
			output.nAwardType = 1;
			output.nAnimalType = new int[] { random.nextInt(3) + 3 };
		} else if (priceType == 24) {
			output.nAwardType = 1;
			output.nAnimalType = new int[] { random.nextInt(3) + 6 };
		} else if (priceType == 25) {
			output.nAwardType = 1;
			output.nAnimalType = new int[] { random.nextInt(3) + 9 };
		} else if (priceType == 26) {
			output.nAwardType = 1;
			output.nAnimalType = new int[] { random.nextInt(12) };
		} else if (priceType == 27) {
			output.nAwardType = 1;
			output.nAnimalType = new int[] { random.nextInt(3) + 6 };
		} else if (priceType == 28) {// 28，29，30，31分别代表 最小兔子，最小猴子，最小熊猫，最小狮子
			output.nAwardType = 1;
			int minBeilv = beilv[9];
			output.nAnimalType = new int[] { 9 };
			if (minBeilv > beilv[10]) {
				minBeilv = beilv[10];
				output.nAnimalType = new int[] { 10 };
			}
			if (minBeilv > beilv[11]) {
				output.nAnimalType = new int[] { 11 };
			}
		} else if (priceType == 29) {
			output.nAwardType = 1;
			int minBeilv = beilv[6];
			output.nAnimalType = new int[] { 6 };
			if (minBeilv > beilv[7]) {
				minBeilv = beilv[7];
				output.nAnimalType = new int[] { 7 };
			}
			if (minBeilv > beilv[8]) {
				output.nAnimalType = new int[] { 8 };
			}
		} else if (priceType == 30) {
			output.nAwardType = 1;
			int minBeilv = beilv[3];
			output.nAnimalType = new int[] { 3 };
			if (minBeilv > beilv[4]) {
				minBeilv = beilv[4];
				output.nAnimalType = new int[] { 4 };
			}
			if (minBeilv > beilv[5]) {
				output.nAnimalType = new int[] { 5 };
			}
		} else if (priceType == 31) {
			output.nAwardType = 1;
			int minBeilv = beilv[0];
			output.nAnimalType = new int[] { 0 };
			if (minBeilv > beilv[1]) {
				minBeilv = beilv[1];
				output.nAnimalType = new int[] { 1 };
			}
			if (minBeilv > beilv[2]) {
				output.nAnimalType = new int[] { 2 };
			}
		}
	}

	private int[] doSongDeng(int length) {
		// 送灯动物
		List<Integer> animals = new ArrayList<Integer>();
		for (int i = 0; i < length; i++) {
			int animal = random.nextInt(12);
			while (animals.contains(animal)) {
				animal = random.nextInt(12);
			}
			animals.add(animal);
		}
		int[] songDeng = new int[animals.size()];
		for (int i = 0; i < animals.size(); i++) {
			songDeng[i] = animals.get(i);
		}
		return songDeng;
	}

	private void setColors(int[] colors, AlgorithmOutput out) {
		if (out.nAwardType == 7 || out.nAwardType == 1) {// 闪电
			int animal = out.nAnimalType[0];
			int color = getColorByAnimal(animal);
			int colorIndex = getColorIndex(colors, color);
			out.nPointerLocation = new int[] { colorIndex };
		} else if (out.nAwardType == 6) {
			out.nPointerLocation = new int[out.nAnimalType.length];
			for (int i = 0; i < out.nAnimalType.length; i++) {
				int color = getColorByAnimal(out.nAnimalType[i]);
				int colorIndex = getColorIndex(colors, color);
				out.nPointerLocation[i] = colorIndex;
			}
		} else if (out.nAwardType == 8) {// 大三元
			out.nPointerLocation = new int[] { 0 };
		} else if (out.nAwardType == 9) {// 大四喜
			int colorIndex = getColorIndex(colors, out.nDaSiXi);
			out.nPointerLocation = new int[] { colorIndex };
		}
	}

	/**
	 * 随机获取颜色索引
	 * 
	 * @param colors
	 * @param color
	 * @return
	 */
	private int getColorIndex(int[] colors, int color) {
		List<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < colors.length; i++) {
			if (colors[i] == color) {
				list.add(i);
			}
		}
		return list.get(random.nextInt(list.size()));
	}

}
