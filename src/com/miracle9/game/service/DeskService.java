package com.miracle9.game.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.common.entity.Desk;
import com.miracle9.common.entity.DeskResult;
import com.miracle9.common.entity.User;
import com.miracle9.game.bean.ForceAward;
import com.miracle9.game.bean.IntInt;
import com.miracle9.game.bean.Pager;
import com.miracle9.game.bean.RoomDesk;
import com.miracle9.game.bean.UserDesk;
import com.miracle9.game.dao.AdminDao;
import com.miracle9.game.dao.AdminLogDao;
import com.miracle9.game.dao.DeskAwardDao;
import com.miracle9.game.dao.DeskDao;
import com.miracle9.game.dao.DeskResultDao;
import com.miracle9.game.dao.RoomDao;
import com.miracle9.game.dao.SystemConfigDao;
import com.miracle9.game.dao.UserDao;
import com.miracle9.game.entity.Admin;
import com.miracle9.game.entity.AdminLog;
import com.miracle9.game.entity.DeskAward;
import com.miracle9.game.util.LocalMem;
import com.miracle9.game.util.MyUtil;

@Service("deskService")
public class DeskService extends BaseService {
	@Autowired
	private DeskDao deskDao;

	@Autowired
	private RoomDao roomDao;

	@Autowired
	private AdminDao adminDao;

	@Autowired
	private AdminLogDao adminLogDao;

	@Autowired
	private DeskResultDao deskResultDao;

	@Autowired
	private SystemConfigDao systemConfigDao;

	@Autowired
	private UserDao userDao;

	@Autowired
	private DeskAwardDao deskAwardDao;

	public List<Desk> getAllDesk() {
		return deskDao.getAllDesk();
	}

	public Desk getDesk(int id) {
		return deskDao.getDesk(id);
	}

	// 获取所有桌子
	public Map<String, Object> getDeskList(IoSession session) {
		List<Desk> desks = deskDao.getAllDesk();
		// 处理在线人数
		for (Desk d : desks) {
			int i = 0;
			for (Entry<Integer, RoomDesk> entry : LocalMem.userid_desk_map.entrySet()) {
				if (d.getId() == entry.getValue().deskId) {
					i++;
				}
			}
			d.setOnlineNumber(i);
		}
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		result.put("message", "");
		result.put("desk", desks.toArray());
		return result;
	}

	// 添加桌子
	public synchronized Map<String, Object> addDesk(Desk desk, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();

		deskDao.addDesk(desk);

		List<Desk> desks = getAllDesk();
		for (Desk d : desks) {
			if (!LocalMem.desk_user_result.containsKey(d.getId())) {
				UserDesk ud = new UserDesk();
				ud.desk = d;
				ud.resulttime = Long.MAX_VALUE;
				ud.restarttime = System.currentTimeMillis() + 10000;
				ud.colors = MyUtil.getColors();
				LocalMem.desk_user_result.put(d.getId(), ud);
			}
		}

		result.put("success", true);
		result.put("message", "");
		return result;
	}

	// 更新桌子参数
	public Map<String, Object> updateDesk(Desk desk, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		int deskCount = deskDao.queryTotalCount("select count(*) from Desk where name=? and id!=?", desk.getName(),
				desk.getId());
		if (deskCount > 0) {
			result.put("success", false);
			result.put("message", "桌子名称已存在");
			return result;
		}
		Desk old = deskDao.getDesk(desk.getId());
		// 难度”，“场地类型”，“一币分值”修改后，该“桌”的游戏账需要归0
		if (old.getAnimalDiff() != desk.getAnimalDiff() || old.getExchange() != desk.getExchange()
				|| old.getSiteType() != desk.getSiteType()) {
			deskDao.cleanDeskData(old.getId());
		}

		deskDao.updateDesk(desk);

		/*
		 * for (IoSession s : LuckSocketHandler.luckClients) {
		 * AmfMessageSend.queue.put(new Data(s, "updateDesk", new Object[] {
		 * desk.getId(), isExitSeat, isLimit, isAutoKick })); }
		 */

		for (Entry<Integer, UserDesk> entry : LocalMem.desk_user_result.entrySet()) {
			if (entry.getKey() == desk.getId()) {
				entry.getValue().desk = desk;
			}
		}

		result.put("success", true);
		result.put("message", "");

		return result;
	}

	// 删除桌子
	public Map<String, Object> deleteDesk(int id, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		deskDao.deleteDesk(id);
		deskAwardDao.executeUpdate("delete from DeskAward where deskId=?", id);
		result.put("success", true);
		result.put("message", "");
		return result;
	}

	// 强制出奖
	public Map<String, Object> forcePrize(int deskId, String forcePrize, boolean isLuck, int luckNum, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		String[] prizeInfo = forcePrize.split("\\|");
		ForceAward fa = new ForceAward();
		fa.setAwardType(Integer.parseInt(prizeInfo[0]));
		fa.setColor(Integer.parseInt(prizeInfo[1]));
		fa.setAnimal(Integer.parseInt(prizeInfo[2]));
		fa.setLightningBeiLv(Integer.parseInt(prizeInfo[3]));
		fa.setSongDengCount(Integer.parseInt(prizeInfo[4]));
		fa.setLuck(isLuck);
		fa.setLuckNum(luckNum);

		if (isLuck && fa.getAwardType() != 6 && fa.getAwardType() != 7) {
			result.put("success", false);
			result.put("message", "幸运奖类型选择错误");
			return result;
		}

		LocalMem.forceAward.put(deskId, fa);
		result.put("success", true);
		result.put("message", "");
		return result;
	}

	// 获取桌子上用户
	public Map<String, Object> getDeskUser(int id, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		result.put("message", "");
		List<User> users = new ArrayList<User>();
		for (Entry<Integer, RoomDesk> entry : LocalMem.userid_desk_map.entrySet()) {
			if (entry.getValue().deskId == id) {
				User user = userDao.queryById(entry.getKey());
				users.add(user);
			}
		}
		result.put("user", users.toArray());
		return result;
	}

	// 获取桌子压分 得分
	public Map<String, Object> getDeskData(int id, IoSession session) {
		Desk desk = deskDao.queryById(id);
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		result.put("message", "");
		result.put("sumYa", desk.getSumYaFen());
		result.put("sumDe", desk.getSumDeFen());
		result.put("sumYin", desk.getSumYaFen() - desk.getSumDeFen());
		return result;
	}

	// 清零桌子压分得分
	public Map<String, Object> cleanDeskData(int id, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		deskDao.cleanDeskData(id);

		// String content = "幸运六狮：清零了ID为 " + id + " 桌子的总押分和总得分";
		// Integer adminId =
		// Integer.valueOf(session.getAttribute("id").toString());
		// Admin admin = adminDao.queryById(adminId);
		// AdminLog adminLog = new AdminLog(admin.getUsername(), content,
		// AdminLog.HALL);
		// adminLogDao.add(adminLog);

		result.put("success", true);
		result.put("message", "");
		return result;
	}

	// 获取开奖结果
	public Pager getDeskResult(int id, String startDate, String endDate, Pager pager, IoSession session) {
		endDate = MyUtil.addDay(endDate, 1);
		pager.setOrderBy("datetime desc");
		pager = deskResultDao.getDeskResult(id, startDate, endDate, pager);
		Object[] os = pager.getList();
		for (Object o : os) {
			DeskResult deskResult = (DeskResult) o;
			setResultStr(deskResult, session);
		}
		return pager;
	}

	/**
	 * 设置开奖描述
	 * 
	 * @param deskResult
	 * @return
	 */
	public void setResultStr(DeskResult deskResult, IoSession session) {
		StringBuilder resultStr = new StringBuilder();
		if (deskResult.getIsForce() == 1) {
			resultStr.append("*");
		}
		// if (deskResult.getZxh() == 0) {
		// resultStr.append("庄 ");
		// } else if (deskResult.getZxh() == 1) {
		// resultStr.append("和 ");
		// } else {
		// resultStr.append("闲 ");
		// }
		if (deskResult.getType() == 0) {
			resultStr.append("普通" + LocalMem.animal_map.get(deskResult.getAnimal()));
		} else if (deskResult.getType() == 1) {
			if (deskResult.getGlobalType() == 0) {
				resultStr.append("全局彩金" + deskResult.getAwardGold() + "分，开奖动物"
						+ LocalMem.animal_map.get(deskResult.getAnimal()));
			} else if (deskResult.getGlobalType() == 1) {
				if (deskResult.getLightningBeilv() <= 3) {
					resultStr.append("全局闪电" + deskResult.getLightningBeilv() + "倍，开奖动物"
							+ LocalMem.animal_map.get(deskResult.getAnimal()));
				} else {
					resultStr.append("小猫变身" + deskResult.getLightningBeilv() + "倍，开奖动物"
							+ LocalMem.animal_map.get(deskResult.getAnimal()));
				}
			} else if (deskResult.getGlobalType() == 2) {
				if (deskResult.getLightningBeilv() == 2) {
					resultStr.append("全局送灯" + deskResult.getSongDengCount() + "盏X2，开奖动物");
				} else {
					resultStr.append("全局送灯" + deskResult.getSongDengCount() + "盏，开奖动物");
				}
				for (String index : deskResult.getMoreInfo().split(",")) {
					resultStr.append(LocalMem.animal_map.get(Integer.valueOf(index)) + ",");
				}
				resultStr = new StringBuilder(resultStr.substring(0, resultStr.length() - 1));
			} else if (deskResult.getGlobalType() == 3) {
				if (deskResult.getAnimal() == 0) {
					resultStr.append("大三元狮子X" + deskResult.getLightningBeilv());
				} else if (deskResult.getAnimal() == 1) {
					resultStr.append("大三元熊猫X" + deskResult.getLightningBeilv());
				} else if (deskResult.getAnimal() == 2) {
					resultStr.append("大三元猴子X" + deskResult.getLightningBeilv());
				} else {
					resultStr.append("大三元兔子X" + deskResult.getLightningBeilv());
				}
			} else if (deskResult.getGlobalType() == 4) {
				if (deskResult.getColor() == 0) {
					resultStr.append("大四喜红色X" + deskResult.getLightningBeilv());
				} else if (deskResult.getColor() == 1) {
					resultStr.append("大四喜绿色X" + deskResult.getLightningBeilv());
				} else {
					resultStr.append("大四喜黄色X" + deskResult.getLightningBeilv());
				}
			}
		}
		if (deskResult.getLuckType() == 1) {
			if (deskResult.getLightningBeilv() <= 3) {
				resultStr.append(",幸运闪电" + deskResult.getLightningBeilv() + "倍,幸运号" + deskResult.getLuckNum() + ",幸运动物"
						+ LocalMem.animal_map.get(deskResult.getLuckAnimal()));
			} else {
				resultStr.append(",幸运小猫变身" + deskResult.getLightningBeilv() + "倍,幸运号" + deskResult.getLuckNum()
						+ ",幸运动物" + LocalMem.animal_map.get(deskResult.getLuckAnimal()));
			}
		} else if (deskResult.getLuckType() == 2) {
			if (deskResult.getLightningBeilv() == 2) {
				resultStr.append(",幸运送灯" + deskResult.getSongDengCount() + "盏X2,幸运号" + deskResult.getLuckNum()
						+ ",幸运动物");
			} else {
				resultStr.append(",幸运送灯" + deskResult.getSongDengCount() + "盏,幸运号" + deskResult.getLuckNum() + ",幸运动物");
			}
			for (String index : deskResult.getMoreInfo().split(",")) {
				resultStr.append(LocalMem.animal_map.get(Integer.valueOf(index)) + ",");
			}
			resultStr = new StringBuilder(resultStr.substring(0, resultStr.length() - 1));
		}
		deskResult.setResultStr(resultStr.toString());
	}

	// 排序
	public Map<String, Object> sortDesk(int gameType, String sortString, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		String content = null;
		boolean isExpeOrder = false;// 修改了练习房排序
		boolean isGameOrder = false;// 修改了竞技房排序
		if (gameType == 0) {
			List<Desk> desks = deskDao.getAllDesk();
			String[] id_indexs = sortString.split(";");
			for (Desk d : desks) {
				for (String s : id_indexs) {
					String[] id_index = s.split(",");
					int id = Integer.parseInt(id_index[0]);
					int index = Integer.parseInt(id_index[1]);
					if (id == d.getId() && d.getOrderBy() != index) {
						isGameOrder = true;
						deskDao.updateOrderBy(id, index);
					}
				}
			}
			if (isExpeOrder && isGameOrder) {
				content = "幸运六狮：修改了竞技厅和练习厅桌排序";
			} else if (isExpeOrder) {
				content = "幸运六狮：修改了练习厅桌排序";
			} else if (isGameOrder) {
				content = "幸运六狮：修改了竞技厅桌排序";
			}
			if (isExpeOrder || isGameOrder) {
				// for (IoSession s : GameSocketHandler.gameClients) {
				// AmfMessageSend.queue.put(new Data(s,
				// "manageService/orderByDesk", new Object[] { sortString,
				// isExpeOrder, isGameOrder }));
				// }
			}
		}
		if (isExpeOrder || isGameOrder) {
			Integer adminId = Integer.valueOf(session.getAttribute("id").toString());
			Admin admin = adminDao.queryById(adminId);
			AdminLog adminLog = new AdminLog(admin.getUsername(), content, AdminLog.HALL);
			adminLogDao.add(adminLog);
		}
		result.put("success", true);
		result.put("message", "");
		return result;
	}

	/**
	 * 增加压分得分
	 * 
	 * @param deskId
	 * @param sumYaFen
	 * @param sumDeFen
	 */
	public void addScore(int deskId, long sumYaFen, long sumDeFen, long sumZhxYaFen, long sumZhxDeFen) {
		deskDao.addScore(deskId, sumYaFen, sumDeFen, sumZhxYaFen, sumZhxDeFen);
	}

	// 获取桌子大奖列表
	public Object[] getBigPrice(int deskId, IoSession session) {
		List<Object> data = new ArrayList<Object>();
		List<DeskAward> awards = deskAwardDao.queryListByHql("from DeskAward where deskId=? order by id", deskId);
		Map<Integer, IntInt> other = new HashMap<Integer, IntInt>();
		Map<String, Object> map = null;
		for (DeskAward d : awards) {
			if (d.getType() < 22) {
				map = new HashMap<String, Object>();
				map.put("type", d.getType());
				map.put("count", d.getCount());
				data.add(map);
			} else {
				IntInt intInt = other.get(d.getType());
				if (intInt == null) {
					intInt = new IntInt();
					other.put(d.getType(), intInt);
				}
				intInt.int1 += 1;
				intInt.int2 += d.getCount();
			}
		}
		for (Entry<Integer, IntInt> entry : other.entrySet()) {
			map = new HashMap<String, Object>();
			map.put("type", entry.getKey() + "|" + entry.getValue().int1);
			map.put("count", entry.getValue().int2);
			data.add(map);
		}
		return data.toArray();
	}

	// 保存桌子大奖
	public Map<String, Object> updateBigPrice(int deskId, String typeStr, IoSession session) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("success", true);
		map.put("message", "");
		deskAwardDao.executeUpdate("delete from DeskAward where deskId=?", deskId);
		if (StringUtils.isBlank(typeStr)) {
			return map;
		}
		// StringBuffer sb = new StringBuffer();
		for (String type : typeStr.split("\\|")) {
			// DeskAward d =
			// deskAwardDao.queryByHql("from DeskAward where type=? and deskId=?",
			// Integer.parseInt(type),
			// deskId);
			// if (d == null) {
			DeskAward d = new DeskAward();
			d.setDeskId(deskId);
			d.setType(Integer.parseInt(type));
			deskAwardDao.add(d);
			// }
			// sb.append(type + ",");
		}
		// if (sb.indexOf(",") != -1) {
		// String types = sb.substring(0, sb.length() - 1);
		// deskAwardDao.executeUpdate("delete from DeskAward where type not in ("
		// + types + ") and deskId=?", deskId);
		// }
		return map;
	}
}
