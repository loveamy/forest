package com.miracle9.game.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.miracle9.common.entity.Desk;
import com.miracle9.common.entity.DeskResult;
import com.miracle9.common.entity.User;
import com.miracle9.game.service.UserService;
import com.miracle9.game.util.LocalMem;

public class UserDesk {
	// /**
	// * 押注时间
	// */
	// public int betTime;
	/**
	 * 每局的24个灯彩灯颜色
	 */
	public int[] colors;

	/**
	 * 转盘每次停止的24个灯的索引
	 */

	public int[] colorsIndex;
	/**
	 * 倒计时的结束时间
	 */
	public long resulttime;
	/**
	 * 上一次开奖最后动物类型
	 */
	// 全局动物类型列表:
	// 列表的长度12,其中元素有效取值范围为0-11。
	// 红色狮子、绿色狮子、黄色狮子、
	// 红色熊猫、绿色熊猫、黄色熊猫、
	// 红色猴子、绿色猴子、黄色猴子、
	// 红色兔子、绿色兔子、黄色兔子
	public int lastAnimalIndex = 0;
	/**
	 * 上一次开奖最后彩灯颜色索引
	 */
	public int lastColorIndex = 0;
	/**
	 * 倍率
	 */
	public int[] magnification = new int[15];
	/**
	 * 重新开始的时间
	 */
	public long restarttime;
	/**
	 * 用户-个人下注总金额列表
	 */
	public Map<Integer, int[]> userBets = new ConcurrentHashMap<Integer, int[]>();
	/**
	 * 开奖结果
	 */
	public DeskResult deskResult = new DeskResult();

	public long sumYaFen = 0;// 统计本局总压分

	public long sumDeFen = 0;// 统计本局总得分

	public long sumZhxYaFen = 0;// 统计本局庄和闲总压分

	public long sumZhxDeFen = 0;// 统计本局庄和闲总得分

	// public int exchange;// 即一个游戏币可以换算成的游戏分值

	public Desk desk;

	public int virtualBetCount = 0;

	public volatile int[] virtualDeskBet = new int[15];

	/**
	 * 设置下局开始时间
	 */
	public void setRestarttime() {
		// 根据开奖结果设置
		if (deskResult.getType() == 0) {
			restarttime = resulttime + 34000;
		} else if (deskResult.getType() == 1) {
			if (deskResult.getGlobalType() == 0 || deskResult.getGlobalType() == 1) {// 全局彩金、闪电
				restarttime = resulttime + 34000;
			} else if (deskResult.getGlobalType() == 3) {// 大三元
				restarttime = resulttime + 34000;
			} else if (deskResult.getGlobalType() == 4) {// 大四喜
				int colorCount = 0;
				for (int i : colors) {
					if (i == deskResult.getColor()) {// 彩灯颜色=大四喜颜色
						colorCount++;
					}
				}
				restarttime = resulttime + 29000 + colorCount * 1000;
			} else if (deskResult.getGlobalType() == 2) {// 全局送灯
				restarttime = resulttime + 20000 * deskResult.getSongDengCount() + 11000;
			}
		}
		if (deskResult.getLuckType() == 1) {// 幸运闪电
			restarttime = resulttime + 60000;
		} else if (deskResult.getLuckType() == 2) {// 幸运送灯
			// restarttime = resulttime + 38000 + 15000 *
			// deskResult.getSongDengCount();
			restarttime = resulttime + 20000 * deskResult.getSongDengCount() + 42000;
		}
	}

	/**
	 * 开始下局清空上局数据
	 */
	public void restart() {
		restarttime = Long.MAX_VALUE;
		resulttime = System.currentTimeMillis() + desk.getBetTime() * 1000;
		// deskResult = null;
		// 清空用户押注信息
		userBets = new ConcurrentHashMap<Integer, int[]>();
		sumDeFen = 0;
		sumYaFen = 0;
		sumZhxYaFen = 0;
		sumZhxDeFen = 0;
	}

	/**
	 * 获取用户押注得分
	 * 
	 * @param userId
	 * @return addArr 索引0是总得分 索引1是幸运得分
	 */
	public int[] getScore(int userId) {
		int[] addScore = new int[2];
		int score = 0;
		int[] bet = userBets.get(userId);
		if (bet == null) {
			return addScore;
		}
		if (deskResult.getType() == 0) {// 普通奖
			int index = deskResult.getAnimal();
			score = bet[index] * magnification[index];
		} else if (deskResult.getType() == 1) {// 全局奖
			if (deskResult.getGlobalType() == 0) {// 彩金
				int index = deskResult.getAnimal();
				score = bet[index] * magnification[index];
				if (bet[index] > 0) {// 获得额外的彩金 彩金总额 * 中彩金动物玩家押注/动物最大押注
					score += deskResult.getAwardGold() * (double) bet[index] / (double) desk.getMaxBet();
				}
			} else if (deskResult.getGlobalType() == 1) {// 闪电
				int index = deskResult.getAnimal();
				score = bet[index] * magnification[index] * deskResult.getLightningBeilv();
				if (deskResult.getLightningBeilv() > 3) {// 小猫变身
					score = bet[index] * deskResult.getLightningBeilv();
				}
			} else if (deskResult.getGlobalType() == 2) {// 送灯
				String[] infos = deskResult.getMoreInfo().split(",");
				for (String s : infos) {
					int index = Integer.parseInt(s);
					score += bet[index] * magnification[index];
				}
				score = score * deskResult.getLightningBeilv();
			} else if (deskResult.getGlobalType() == 3) {// 大三元
				if (deskResult.getAnimal() == 0) {// 狮子
					score = bet[0] * magnification[0] + bet[1] * magnification[1] + bet[2] * magnification[2];
				} else if (deskResult.getAnimal() == 1) {// 熊猫
					score = bet[3] * magnification[3] + bet[4] * magnification[4] + bet[5] * magnification[5];
				} else if (deskResult.getAnimal() == 2) {// 猴子
					score = bet[6] * magnification[6] + bet[7] * magnification[7] + bet[8] * magnification[8];
				} else if (deskResult.getAnimal() == 3) {// 兔子
					score = bet[9] * magnification[9] + bet[10] * magnification[10] + bet[11] * magnification[11];
				}
				score = score * deskResult.getLightningBeilv();
			} else if (deskResult.getGlobalType() == 4) {// 大四喜
				if (deskResult.getColor() == 0) {// 红色
					score = bet[0] * magnification[0] + bet[3] * magnification[3] + bet[6] * magnification[6] + bet[9]
							* magnification[9];
				} else if (deskResult.getColor() == 1) {// 绿色
					score = bet[1] * magnification[1] + bet[4] * magnification[4] + bet[7] * magnification[7] + bet[10]
							* magnification[10];
				} else if (deskResult.getColor() == 2) {// 黄色
					score = bet[2] * magnification[2] + bet[5] * magnification[5] + bet[8] * magnification[8] + bet[11]
							* magnification[11];
				}
				score = score * deskResult.getLightningBeilv();
			}
		}
		int luckScore = 0;
		if (deskResult.getType() == 2 || deskResult.getLuckType() != 0) {
			int index = 0;
			DeskSeat ds = LocalMem.userid_seat_map.get(userId);
			if (ds != null && ds.getSeat() == deskResult.getLuckNum()) {
				if (deskResult.getLuckType() == 1) {// 闪电
					index = deskResult.getLuckAnimal();
					if (deskResult.getLightningBeilv() > 3) {// 小猫变身
						luckScore = bet[index] * deskResult.getLightningBeilv();
					} else {
						luckScore = bet[index] * magnification[index] * deskResult.getLightningBeilv();
					}
				} else if (deskResult.getLuckType() == 2) {// 送灯
					String[] infos = deskResult.getMoreInfo().split(",");
					for (String s : infos) {
						index = Integer.parseInt(s);
						luckScore += bet[index] * magnification[index];
					}
					luckScore = luckScore * deskResult.getLightningBeilv();
				}
			}
		}
		addScore[0] = score + luckScore;
		addScore[1] = luckScore;
		return addScore;
	}

	/**
	 * 获取桌上玩家收益
	 * 
	 * @return
	 */
	/**
	 * public Income[] getDeskIncome() { List<Income> incomes = new
	 * ArrayList<Income>(); // 用来保存已经添加的桌位 List<Integer> existSeat = new
	 * ArrayList<Integer>(); for (Integer uid : userBets.keySet()) { DeskSeat ds
	 * = LocalMem.userid_seat_map.get(uid); if (ds == null) { continue; } int[]
	 * betArr = userBets.get(uid); int addScore = getScore(uid); int bet =
	 * sumArry(betArr);// 总押注 sumYaFen += bet; sumDeFen += addScore; sumZhxYaFen
	 * += betArr[12] + betArr[13] + betArr[14]; // 处理庄闲和 int index =
	 * deskResult.getZxh() + 12; sumZhxDeFen += betArr[index] *
	 * magnification[index]; // 如果是“和”需要返回“庄”、“闲”押注 if (index == 13) {
	 * sumZhxDeFen += betArr[12] + betArr[14]; } Income i = new Income();
	 * //i.score = addScore - bet;// 收益 得分-押注 i.score = addScore; i.seatId =
	 * ds.getSeat(); incomes.add(i); existSeat.add(i.seatId); } for (int i = 1;
	 * i <= 8; i++) { if (!existSeat.contains(i)) { Income in = new Income();
	 * in.score = 0; in.seatId = i; incomes.add(in); } } return
	 * incomes.toArray(new Income[] {}); }
	 */

	public Seat[] getDeskIncome(UserService userService) {
		List<Seat> seats = new ArrayList<Seat>();
		// 用来保存已经添加的桌位
		for (Entry<Integer, DeskSeat> entry : LocalMem.userid_seat_map.entrySet()) {
			if (entry.getValue().getDeskId() == desk.getId()) {
				int uid = entry.getKey();
				DeskSeat ds = LocalMem.userid_seat_map.get(uid);
				if (ds == null) {
					continue;
				}
				int[] betArr = userBets.get(uid);
				if (betArr == null) {
					betArr = new int[15];
				}
				int addScore = getScore(uid)[0];
				int bet = sumArry(betArr);// 总押注
				sumYaFen += bet;
				sumDeFen += addScore;
				sumZhxYaFen += betArr[12] + betArr[13] + betArr[14];
				// 处理庄闲和
				int index = deskResult.getZxh() + 12;
				sumZhxDeFen += betArr[index] * magnification[index];
				// 如果是“和”需要返回“庄”、“闲”押注
				if (index == 13) {
					sumZhxDeFen += betArr[12] + betArr[14];
				}
				User user = userService.getUser(uid);
				Seat s = new Seat(ds.getSeat(), user.getNickName(), user.getHeadUrl(), user.getGameGold() + addScore
						- bet, user.getId(), user.getUserType(), addScore - bet);
				seats.add(s);
			}
		}
		return seats.toArray(new Seat[] {});
	}

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

	public int[] getColors() {
		return colors;
	}

	public void setColors(int[] colors) {
		this.colors = colors;
	}

	public int[] getColorsIndex() {
		return colorsIndex;
	}

	public void setColorsIndex(int[] colorsIndex) {
		this.colorsIndex = colorsIndex;
	}

	public long getResulttime() {
		return resulttime;
	}

	public void setResulttime(long resulttime) {
		this.resulttime = resulttime;
	}

	public int getLastAnimalIndex() {
		return lastAnimalIndex;
	}

	public void setLastAnimalIndex(int lastAnimalIndex) {
		this.lastAnimalIndex = lastAnimalIndex;
	}

	public int getLastColorIndex() {
		return lastColorIndex;
	}

	public void setLastColorIndex(int lastColorIndex) {
		this.lastColorIndex = lastColorIndex;
	}

	public int[] getMagnification() {
		return magnification;
	}

	public void setMagnification(int[] magnification) {
		this.magnification = magnification;
	}

	public long getRestarttime() {
		return restarttime;
	}

	public void setRestarttime(long restarttime) {
		this.restarttime = restarttime;
	}

	public Map<Integer, int[]> getUserBets() {
		return userBets;
	}

	public void setUserBets(Map<Integer, int[]> userBets) {
		this.userBets = userBets;
	}

	public DeskResult getDeskResult() {
		return deskResult;
	}

	public void setDeskResult(DeskResult deskResult) {
		this.deskResult = deskResult;
	}

	public long getSumYaFen() {
		return sumYaFen;
	}

	public void setSumYaFen(long sumYaFen) {
		this.sumYaFen = sumYaFen;
	}

	public long getSumDeFen() {
		return sumDeFen;
	}

	public void setSumDeFen(long sumDeFen) {
		this.sumDeFen = sumDeFen;
	}

	public long getSumZhxYaFen() {
		return sumZhxYaFen;
	}

	public void setSumZhxYaFen(long sumZhxYaFen) {
		this.sumZhxYaFen = sumZhxYaFen;
	}

	public long getSumZhxDeFen() {
		return sumZhxDeFen;
	}

	public void setSumZhxDeFen(long sumZhxDeFen) {
		this.sumZhxDeFen = sumZhxDeFen;
	}

	public Desk getDesk() {
		return desk;
	}

	public void setDesk(Desk desk) {
		this.desk = desk;
	}

}
