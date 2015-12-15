package com.miracle9.game.service;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.common.entity.User;
import com.miracle9.game.bean.GoldChangeType;
import com.miracle9.game.bean.Pager;
import com.miracle9.game.dao.AdminDao;
import com.miracle9.game.dao.AdminLogDao;
import com.miracle9.game.dao.DeskDao;
import com.miracle9.game.dao.GameGoldChangeLogDao;
import com.miracle9.game.dao.RoomDao;
import com.miracle9.game.dao.SystemConfigDao;
import com.miracle9.game.dao.UserDao;
import com.miracle9.game.entity.Admin;
import com.miracle9.game.entity.AdminLog;
import com.miracle9.game.entity.GameGoldChangeLog;
import com.miracle9.game.entity.UserMessage;
import com.miracle9.game.socket.AmfMessageSend;
import com.miracle9.game.socket.Data;
import com.miracle9.game.util.LocalMem;
import com.miracle9.game.util.MyUtil;

import flex.messaging.io.SerializationContext;
import flex.messaging.io.amf.Amf3Output;

@Service("adminService")
public class AdminService extends BaseService {
	private Logger logger = Logger.getLogger(AdminService.class);
	private static String DEFAULT_USER_PASSWORD = "888888";

	@Autowired
	private AdminDao adminDao;
	@Autowired
	private RoomDao roomDao;
	@Autowired
	private SystemConfigDao systemConfigDao;
	@Autowired
	private AdminLogDao adminLogDao;
	@Autowired
	private DeskDao deskDao;

	@Autowired
	private UserDao userDao;

	@Autowired
	GameGoldChangeLogDao gameGoldChangeLogDao;

	@Autowired
	private UserService userService;

	// 获取加密秘钥和服务器时间
	public void login_getEncrytKey(String pubKey, IoSession session) {
		try {
			Map<String, Object> arg = new HashMap<String, Object>();

			String key = MyUtil.generatePassword(16);
			LocalMem.encrypt_key_map.put(session, key);
			key = MyUtil.encodeKey(key, pubKey);
			arg.put("key", key);
			arg.put("time", System.currentTimeMillis());
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("method", "login_getEncrytKey");
			map.put("args", new Object[] { arg });

			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			Amf3Output amf3Output = new Amf3Output(SerializationContext.getSerializationContext());
			amf3Output.setOutputStream(bout);
			amf3Output.writeObject(map);
			amf3Output.flush();
			amf3Output.close();

			byte[] content_out = bout.toByteArray();
			IoBuffer bb = IoBuffer.allocate(content_out.length + 4);
			bb.putInt(content_out.length);
			bb.put(content_out);
			bb.flip();

			session.write(bb);
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	public Map<String, Object> addAdmin(String userName, String password, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();

		Admin admin = adminDao.queryByHql("from Admin where username=?", userName);
		if (admin != null) {
			result.put("success", false);
			result.put("message", "");
		} else {
			result.put("success", true);
			result.put("message", "");
			admin = new Admin();
			admin.setDatetime(MyUtil.getCurrentTimestamp());
			admin.setUsername(userName);
			admin.setPassword(DigestUtils.md5Hex(password));
			adminDao.add(admin);
		}
		return result;
	}

	// 后台管理员登陆
	public Map<String, Object> adminLogin(String username, String password, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();

		Admin admin = adminDao.queryByHql("from Admin where username=?", username);
		if (admin == null || !admin.getPassword().equals(DigestUtils.md5Hex(password))) {
			result.put("success", false);
			result.put("message", "用户名或密码错误");
			return result;
		} else {
			result.put("success", true);
			result.put("message", "");
			result.put("admin", admin);

			if (LocalMem.onlineAdmin.containsKey(admin.getId())) {// 已经登录过了
				// 发送下线通知
				AmfMessageSend.queue.put(new Data(LocalMem.onlineAdmin.get(admin.getId()), "logoffNotice",
						new Object[] {}));
				IoSession s = LocalMem.onlineAdmin.remove(admin.getId());
				s.removeAttribute("id");
				s.removeAttribute("username");
			}

			session.setAttribute("username", admin.getUsername());
			session.setAttribute("id", admin.getId());
			LocalMem.onlineAdmin.put(admin.getId(), session);

			AdminLog adminLog = new AdminLog(admin.getUsername(), "登陆管理后台", AdminLog.OTHER);
			adminLogDao.add(adminLog);
		}
		return result;
	}

	// 退出
	public Map<String, Object> logout(IoSession session) {
		Integer adminId = Integer.valueOf(session.getAttribute("id").toString());
		Map<String, Object> result = new HashMap<String, Object>();
		IoSession s = LocalMem.onlineAdmin.remove(adminId);
		s.removeAttribute("id");
		s.removeAttribute("username");
		result.put("success", true);
		result.put("message", "");

		Admin admin = adminDao.queryById(adminId);
		AdminLog adminLog = new AdminLog(admin.getUsername(), "退出管理后台", AdminLog.OTHER);
		adminLogDao.add(adminLog);
		return result;
	}

	// 修改密码
	public Map<String, Object> updatePassword(String oldPwd, String newPwd, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		Integer adminId = Integer.valueOf(session.getAttribute("id").toString());
		Admin admin = adminDao.queryById(adminId);
		if (DigestUtils.md5Hex(oldPwd).equals(admin.getPassword())) {
			adminDao.createQuery("update Admin set password=? where id=?", DigestUtils.md5Hex(newPwd), admin.getId())
					.executeUpdate();
			result.put("success", true);
			result.put("message", "");

			AdminLog adminLog = new AdminLog(admin.getUsername(), "修改密码", AdminLog.OTHER);
			adminLogDao.add(adminLog);
		} else {
			result.put("success", false);
			result.put("message", "原密码错误");
		}
		return result;
	}

	// 会员操作后台获取用户列表
	public Map<String, Object> getUserList(String searchWord, Pager pager, IoSession session) {
		Map<String, Object> map = new HashMap<String, Object>();
		String orgOrderBy = pager.getOrderBy();
		String orgOrder = pager.getOrder();
		pager.setOrderBy("gameGold desc");
		String hql = "";
		if (searchWord != null && !"".equals(searchWord)) {
			hql = "from User where userName like ? and status != 2";
			pager = userDao.queryPagerByHql(hql, pager, "%" + searchWord + "%");
		} else {
			hql = "from User where status != 2";
			pager = userDao.queryPagerByHql(hql, pager);
		}

		pager.setOrderBy(orgOrderBy);
		pager.setOrder(orgOrder);

		map.put("success", true);
		map.put("message", "");
		map.put("pager", pager);
		return map;
	}

	// 获取会员详细信息
	public Map<String, Object> getUserInfo(int id, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		User user = userDao.queryById(id);
		if (user != null) {

			result.put("success", true);
			result.put("message", "");
			result.put("user", user);

			Integer adminId = Integer.valueOf(session.getAttribute("id").toString());
			Admin admin = adminDao.queryById(adminId);
			AdminLog adminLog = new AdminLog(admin.getUsername(), "对会员“" + user.getUserName() + "”进行了“详情”操作",
					AdminLog.USER);
			adminLogDao.add(adminLog);
		} else {
			result.put("success", false);
			result.put("message", "无法查询到会员");
		}
		return result;
	}

	// 封号
	public Map<String, Object> lockUser(int id, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		userDao.updateStatus(id, 1);
		User user = userDao.queryById(id);

		Integer adminId = Integer.valueOf(session.getAttribute("id").toString());
		Admin admin = adminDao.queryById(adminId);
		AdminLog adminLog = new AdminLog(admin.getUsername(), "对会员“" + user.getUserName() + "”进行了“封号”操作", AdminLog.USER);
		adminLogDao.add(adminLog);

		UserService.quitToLoginNotice(user.getId(), 2);
		result.put("success", true);
		result.put("message", "");
		return result;
	}

	// 解封
	public Map<String, Object> unlockUser(int id, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		userDao.updateStatus(id, 0);

		User user = userDao.queryById(id);
		Integer adminId = Integer.valueOf(session.getAttribute("id").toString());
		Admin admin = adminDao.queryById(adminId);
		AdminLog adminLog = new AdminLog(admin.getUsername(), "对会员“" + user.getUserName() + "”进行了“解封”操作", AdminLog.USER);
		adminLogDao.add(adminLog);

		result.put("success", true);
		result.put("message", "");
		return result;
	}

	// 删号
	public Map<String, Object> deleteUser(int id, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		userDao.updateStatus(id, 2);

		User user = userDao.queryById(id);
		Integer adminId = Integer.valueOf(session.getAttribute("id").toString());
		Admin admin = adminDao.queryById(adminId);
		AdminLog adminLog = new AdminLog(admin.getUsername(), "对会员“" + user.getUserName() + "”进行了“删号”操作", AdminLog.USER);
		adminLogDao.add(adminLog);

		result.put("success", true);
		result.put("message", "");
		UserService.quitToLoginNotice(id, 3);
		return result;
	}

	// 重置会员密码
	public Map<String, Object> resetUserPassword(int id, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		userDao.updatePassword(id, DigestUtils.md5Hex(DEFAULT_USER_PASSWORD));

		User user = userDao.queryById(id);
		Integer adminId = Integer.valueOf(session.getAttribute("id").toString());
		Admin admin = adminDao.queryById(adminId);
		AdminLog adminLog = new AdminLog(admin.getUsername(), "对会员“" + user.getUserName() + "”进行了“重置密码”操作",
				AdminLog.USER);
		adminLogDao.add(adminLog);

		userDao.resetUserInfo(id);

		result.put("success", true);
		result.put("message", "");

		UserService.quitToLoginNotice(id, 5);
		return result;
	}

	// 设置玩家游戏币
	public Map<String, Object> setGameGold(int id, int gameGold, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		Admin admin = adminDao.queryById(Integer.parseInt(session.getAttribute("id").toString()));
		if (admin.getType() != 0) {
			result.put("success", false);
			result.put("message", "权限不足");
			return result;
		}

		User user = userDao.queryById(id);
		if (user == null) {
			result.put("success", false);
			result.put("message", "玩家不存在");
		} else if (user.getStatus() != 0) {
			result.put("success", false);
			result.put("message", "玩家账号状态异常");
		} else if (LocalMem.userid_desk_map.containsKey(id)) {
			// 判断玩家是否在游戏中
			result.put("success", false);
			result.put("message", "玩家正在游戏中，不能设置游戏币");
		} else if (gameGold < 0) {
			result.put("success", false);
			result.put("message", "游戏币不能为负数");
		} else if (userDao.setGameGold(id, gameGold)) {
			result.put("success", true);
			result.put("message", "");
			UserService.userGameGoldNotice(id, gameGold);

			// 记录会员游戏币变化
			gameGoldChangeLogDao.addLog(id, user.getUserName(), user.getGameGold(), gameGold - user.getGameGold(),
					gameGold, GoldChangeType.SYSTEM, "");

			// TODO
			UserMessage um = new UserMessage();
			um.setContent("管理员将您的游戏币修改为" + gameGold);
			um.setDatetime(MyUtil.getCurrentTimestamp());
			um.setSender("admin");
			um.setStatus(0);
			um.setTitle("游戏币变更");
			um.setType(1);
			um.setUserId(id);

		} else {
			result.put("success", false);
			result.put("message", "设置游戏币失败");
		}
		return result;
	}

	/**
	 * 设置超级玩家
	 * 
	 * @param id
	 * @param session
	 * @return
	 */
	public Map<String, Object> enableSuperUser(int id, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();

		if (userDao.enableSuperUser(id)) {
			result.put("success", true);
			result.put("message", "");

			// 向客户端推送会员被设置成超级玩家
			UserService.commonNotice(id, "enableSuperUser");
		} else {
			result.put("success", false);
			result.put("message", "设置为超级玩家失败");
		}
		return result;
	}

	/**
	 * 取消会员为超级玩家
	 * 
	 * @param id
	 * @param session
	 * @return
	 */
	public Map<String, Object> disableSuperUser(int id, IoSession session) {

		Map<String, Object> result = new HashMap<String, Object>();

		if (userDao.disableSuperUser(id)) {
			result.put("success", true);
			result.put("message", "");

			// 向客户端推送取消超级玩家
			UserService.commonNotice(id, "disableSuperUser");
		} else {
			result.put("success", false);
			result.put("message", "取消超级玩家失败");
		}

		return result;
	}

	/**
	 * 
	 * @param id
	 *            id传入-1查询所有人
	 * @param startDate
	 * @param endDate
	 * @param pager
	 * @param session
	 * @return
	 */
	public Map<String, Object> queryGameGoldChangeLog(int id, int type, String startDate, String endDate, Pager pager,
			IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		String orgOrderBy = pager.getOrderBy();
		String orgOrder = pager.getOrder();
		pager.setOrderBy("changeTime desc");
		if (id < 0) {
			if (type < 0) {
				pager = gameGoldChangeLogDao.queryPagerByHql(
						"from GameGoldChangeLog where changeTime>=? and changeTime<=?", pager, startDate + " 00:00:00",
						endDate + " 23:59:59");
			} else {
				pager = gameGoldChangeLogDao.queryPagerByHql(
						"from GameGoldChangeLog where changeTime>=? and changeTime<=? and changeType=?", pager,
						startDate + " 00:00:00", endDate + " 23:59:59", type);
			}
		} else {
			if (type < 0) {
				pager = gameGoldChangeLogDao.queryPagerByHql(
						"from GameGoldChangeLog where userId=? and changeTime>=? and changeTime<=?", pager, id,
						startDate + " 00:00:00", endDate + " 23:59:59");
			} else {
				pager = gameGoldChangeLogDao.queryPagerByHql(
						"from GameGoldChangeLog where userId=? and changeTime>=? and changeTime<=? and changeType=?",
						pager, id, startDate + " 00:00:00", endDate + " 23:59:59", type);
			}
		}
		pager.setOrderBy(orgOrderBy);
		pager.setOrder(orgOrder);

		result.put("success", true);
		result.put("message", "");

		// 讲pager中的变化类型由数字转换成中文
		for (Object o : pager.getList()) {
			GameGoldChangeLog log = (GameGoldChangeLog) o;
			log.setChangeTypeDesc(GoldChangeType.getTypeString(log.getChangeType()));
		}

		result.put("pager", pager);

		return result;
	}

	// 心跳
	public void heart(IoSession session) {
		AmfMessageSend.queue.put(new Data(session, "heart", new Object[] {}));
	}

	public Admin getAdmin(int id) {
		return adminDao.queryById(id);
	}
}
