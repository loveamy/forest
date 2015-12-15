package com.miracle9.game.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.game.bean.Pager;
import com.miracle9.game.dao.AdminDao;
import com.miracle9.game.dao.AdminLogDao;
import com.miracle9.game.dao.DeskDao;
import com.miracle9.game.dao.MarqueeDao;
import com.miracle9.game.dao.UserDao;
import com.miracle9.game.dao.UserMessageDao;
import com.miracle9.game.entity.Marquee;
import com.miracle9.game.entity.UserMessage;
import com.miracle9.game.socket.Data;
import com.miracle9.game.socket.JSONMessageSend;
import com.miracle9.game.util.LocalMem;
import com.miracle9.game.util.MyUtil;

@Service("noticeService")
public class NoticeService extends BaseService {

	@Autowired
	private UserDao userDao;

	@Autowired
	private AdminDao adminDao;

	@Autowired
	private AdminLogDao adminLogDao;

	@Autowired
	private DeskDao deskDao;

	@Autowired
	private MarqueeDao marqueeDao;

	@Autowired
	private UserMessageDao userMessageDao;

	public Map<String, Object> querySentNotice(Pager pager, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		result.put("message", "");
		if (StringUtils.isBlank(pager.getOrderBy())) {// 初始状态
			pager.setOrderBy("datetime desc");
		}
		pager = userMessageDao.queryPagerByHql("from UserMessage where type=0", pager);
		result.put("pager", pager);
		return result;
	}

	public Map<String, Object> sendNotice(String title, String content, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		result.put("message", "");
		UserMessage notice = new UserMessage();
		notice.setContent(content);
		notice.setDatetime(MyUtil.getCurrentTimestamp());
		notice.setTitle(title);
		notice.setSender("admin");
		notice.setStatus(0);
		notice.setType(0);
		notice.setUserId(0);
		userMessageDao.add(notice);
		// 设置所有的用户有没有读取的信息
		userDao.setAllUserHasMessageToRead();
		// 所有在线用户推送消息
		for (Entry<Integer, IoSession> entry : LocalMem.onlineUsers.entrySet()) {
			UserService.readMessageNotice(entry.getKey(), 1, entry.getValue());
		}
		return result;
	}

	public Map<String, Object> createMarquee(String content, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();

		if (content == null || "".equals(content)) {
			result.put("success", false);
			result.put("message", "消息体不能为空");
		} else {
			result.put("success", true);
			result.put("message", "");
			Marquee marquee = new Marquee();
			marquee.setContent(content);
			marquee.setDatetime(MyUtil.getCurrentTimestamp());
			publishMarquee(content);
			marqueeDao.add(marquee);
		}
		return result;
	}

	public Map<String, Object> queryMarquee(Pager pager, IoSession session) {
		Map<String, Object> map = new HashMap<String, Object>();
		String orgOrderBy = pager.getOrderBy();
		String orgOrder = pager.getOrder();
		pager.setOrderBy("datetime desc");
		String hql = "from Marquee";
		pager = marqueeDao.queryPagerByHql(hql, pager);
		pager.setOrderBy(orgOrderBy);
		pager.setOrder(orgOrder);
		map.put("success", true);
		map.put("message", "");
		map.put("pager", pager);
		return map;
	}

	// 给客户端推送滚动消息
	public void publishMarquee(String content) {
		for (Entry<Integer, IoSession> e : LocalMem.onlineUsers.entrySet()) {
			JSONMessageSend.queue.put(new Data(e.getValue(), "sendMarquee", new Object[] { content }));
		}
	}

	// 删除系统公告
	public Map<String, Object> deleteNotice(int id, IoSession session) {
		userMessageDao.delete(id);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("success", true);
		map.put("message", "");
		return map;
	}
}
