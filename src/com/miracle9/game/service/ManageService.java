package com.miracle9.game.service;

import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.common.entity.Desk;
import com.miracle9.common.entity.User;
import com.miracle9.game.bean.UserDesk;
import com.miracle9.game.dao.DeskDao;
import com.miracle9.game.dao.UserDao;
import com.miracle9.game.socket.Data;
import com.miracle9.game.socket.JSONMessageSend;
import com.miracle9.game.util.LocalMem;
import com.miracle9.game.util.MyUtil;

/**
 * 管理后台通信业务处理
 */
@Service("manageService")
public class ManageService {

	@Autowired
	private UserService userService;

	@Autowired
	private DeskDao deskDao;

	@Autowired
	private UserDao userDao;

	/*// 连接到服务端后主动推送过来key
	public void sendServerTime(Map<String, Object> map, IoSession session) {
		String key = map.get("key").toString();
		key = new String(new Base64().decode(key));
		String indexStr = key.substring(0, 1);
		String lastStr = key.substring(key.length() - 1, key.length());
		StringBuilder sb = new StringBuilder(key.substring(1, key.length() - 1));
		ManageSocketConnect.key = indexStr + sb.reverse() + lastStr;
		// 同步在线人数
		for (Integer uid : LocalMem.online_userId_session_map.keySet()) {
			ManageSocketConnect.sendData("gameService/login", new Object[] {
					uid, 0 });
		}
		List<Desk> desks = deskDao.getAllDesk();
		for (Desk d : desks) {
			List<User> users = new ArrayList<User>();
			for (Entry<Integer, RoomDesk> entry : LocalMem.userid_desk_map
					.entrySet()) {
				if (entry.getValue().deskId == d.getId()) {
					User user = userDao.queryById(entry.getKey());
					users.add(user);
				}
			}
			ManageSocketConnect.sendData("gameService/deskInfo", new Object[] {
					d.getId(), users, 0 });
		}
	}*/

	// 后台添加桌子
	public void addDesk(Desk desk, IoSession session) {
		// 添加到开奖结果
		UserDesk ud = new UserDesk();
		ud.desk = desk;
		ud.resulttime = Long.MAX_VALUE;
		ud.restarttime = System.currentTimeMillis() + 10000;
		ud.colors = MyUtil.getColors();
		LocalMem.desk_user_result.put(desk.getId(), ud);

		/*// 在添加桌子所属房间里面的人
		List<Desk> desks = new ArrayList<Desk>();
		for (Entry<Integer, UserDesk> dr : LocalMem.desk_user_result.entrySet()) {
			Desk dk = dr.getValue().desk;
			if (dk.getRoomId() != desk.getRoomId())
				continue;
			dk.setOnlineNumber(0);
			for (Entry<Integer, DeskSeat> us : LocalMem.userid_seat_map
					.entrySet()) {
				if (dk.getId() == us.getValue().deskId) {
					dk.setOnlineNumber(dk.getOnlineNumber() + 1);
				}
			}
			desks.add(dk);
		}
		Collections.sort(desks, new DeskOrder());
		for (Entry<Integer, Integer> entry : LocalMem.userid_roomid_map
				.entrySet()) {
			if (entry.getValue() == desk.getRoomId()) {
				IoSession s = LocalMem.online_userId_session_map.get(entry
						.getKey());
				MessageSend.queue.put(new Data(s, "updateRoomInfo",
						new Object[] { desks.toArray(new Desk[] {}) }));
			}
		}*/
	}

	

	public void deleteDesk(int id, IoSession session) {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		deskDao.refreshDeskCache(id);
	}

	// 后台发送游戏公告
	public void sendNotice(int userId, String content, IoSession session) {
		IoSession s = LocalMem.onlineUsers.get(userId);
		JSONMessageSend.queue.put(new Data(s, "sendNotice",
				new Object[] { content }));
	}

	// 后台禁言或者取消禁言
	public void userShutup(int userId, boolean isShutup, IoSession session) {
		IoSession s = LocalMem.onlineUsers.get(userId);
		JSONMessageSend.queue.put(new Data(s, "userShutup",
				new Object[] { isShutup }));
	}

	// 退出到登录界面 1表示服务器升级维护；2表示玩家账号被冻结；3表示玩家账号被删除；4表示重复登录；5表示后台重置密码
/*	public void quitToLogin(int userId, int type, IoSession session) {
		if (type == 1) {// 维护
			// 所有在线人员退出到登录界面
			for (IoSession s : LocalMem.online_session_userId_map.keySet()) {
				MessageSend.queue.put(new Data(s, "quitToLogin",
						new Object[] { 1 }));
			}
			// 桌位上的人强制结算
			
			LocalMem.online_userId_session_map = new ConcurrentHashMap<Integer, IoSession>();
			LocalMem.online_session_userId_map = new ConcurrentHashMap<IoSession, Integer>();
			LocalMem.userid_roomid_map = new ConcurrentHashMap<Integer, Integer>();
			LocalMem.userid_desk_map = new ConcurrentHashMap<Integer, RoomDesk>();
			LocalMem.userid_seat_map = new ConcurrentHashMap<Integer, DeskSeat>();
			LocalMem.sessionBetTime = new ConcurrentHashMap<IoSession, Long>();
		} else {
			IoSession s = LocalMem.online_userId_session_map.get(userId);
			MessageSend.queue.put(new Data(s, "quitToLogin",
					new Object[] { type }));
			LocalMem.online_userId_session_map.remove(userId);
			LocalMem.online_session_userId_map.remove(s);
			if (LocalMem.userid_roomid_map.containsKey(userId)) {
				LocalMem.userid_roomid_map.remove(userId);
			}
			if (LocalMem.userid_desk_map.containsKey(userId)) {
				LocalMem.userid_desk_map.remove(userId);
			}
			if (LocalMem.userid_seat_map.containsKey(userId)) {// 在座位上
				
				DeskSeat ds = LocalMem.userid_seat_map.remove(userId);
				userService.refreshUser(ds.deskId);
				LocalMem.sessionBetTime.remove(s);
			}
			ManageSocketConnect.sendData("gameService/logout", new Object[] {
					userId, 0 });
		}
	}
*/



	// 新游戏币
	public void newGameGold(int userId, int gameGold, IoSession session) {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		IoSession s = LocalMem.onlineUsers.get(userId);
		User user = userService.getUser(userId);
		JSONMessageSend.queue.put(new Data(s, "newGameGold", new Object[] { user
				.getGameGold() }));
	}

	// 赠送游戏币
	public void userAward(int userId, int gold, IoSession session) {
		IoSession s = LocalMem.onlineUsers.get(userId);
		JSONMessageSend.queue.put(new Data(s, "userAward", new Object[] { gold }));
	}


	/*// 桌子排序 orderString格式：id,index;id,index
	public void orderByDesk(String orderString, boolean isExpeOrder,
			boolean isGameOrder, IoSession session) {
		String[] id_indexs = orderString.split(";");
		for (String s : id_indexs) {
			String[] id_index = s.split(",");
			int id = Integer.parseInt(id_index[0]);
			int index = Integer.parseInt(id_index[1]);
			Desk desk = LocalMem.desk_user_result.get(id).desk;
			if (desk.getId() == id && desk.getOrderBy() != index) {
				// deskDao.refreshDeskCache(id);
				desk.setOrderBy(index);
			}
		}

		if (isExpeOrder) {
			List<Desk> desks = new ArrayList<Desk>();
			for (Entry<Integer, UserDesk> dr : LocalMem.desk_user_result
					.entrySet()) {
				Desk dk = dr.getValue().desk;
				if (dk.getRoomId() != 1)
					continue;
				dk.setOnlineNumber(0);
				for (Entry<Integer, DeskSeat> us : LocalMem.userid_seat_map
						.entrySet()) {
					if (dk.getId() == us.getValue().deskId) {
						dk.setOnlineNumber(dk.getOnlineNumber() + 1);
					}
				}
				desks.add(dk);
			}
			Collections.sort(desks, new DeskOrder());
			// 刷新桌子列表
			for (Entry<Integer, Integer> um : LocalMem.userid_roomid_map
					.entrySet()) {
				if (um.getValue() == 1) {
					IoSession s = LocalMem.online_userId_session_map.get(um
							.getKey());
					MessageSend.queue.put(new Data(s, "updateRoomInfo",
							new Object[] { desks.toArray(new Desk[] {}) }));
				}
			}
		}
		if (isGameOrder) {
			List<Desk> desks = new ArrayList<Desk>();
			for (Entry<Integer, UserDesk> dr : LocalMem.desk_user_result
					.entrySet()) {
				Desk dk = dr.getValue().desk;
				if (dk.getRoomId() != 2)
					continue;
				dk.setOnlineNumber(0);
				for (Entry<Integer, DeskSeat> us : LocalMem.userid_seat_map
						.entrySet()) {
					if (dk.getId() == us.getValue().deskId) {
						dk.setOnlineNumber(dk.getOnlineNumber() + 1);
					}
				}
				desks.add(dk);
			}
			Collections.sort(desks, new DeskOrder());
			// 刷新桌子列表
			for (Entry<Integer, Integer> um : LocalMem.userid_roomid_map
					.entrySet()) {
				if (um.getValue() == 2) {
					IoSession s = LocalMem.online_userId_session_map.get(um
							.getKey());
					MessageSend.queue.put(new Data(s, "updateRoomInfo",
							new Object[] { desks.toArray(new Desk[] {}) }));
				}
			}
		}
	}*/
}
