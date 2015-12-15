package com.miracle9.game.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.game.bean.Pager;
import com.miracle9.game.dao.AdminDao;
import com.miracle9.game.dao.AdminLogDao;
import com.miracle9.game.dao.DeskDao;
import com.miracle9.game.dao.UserDao;
import com.miracle9.game.dao.UserMessageDao;
import com.miracle9.game.entity.UserMessage;
import com.miracle9.game.util.LocalMem;

@Service("userMessageService")
public class UserMessageService extends BaseService {


	@Autowired
	private UserDao userDao;

	@Autowired
	private UserMessageDao userMessageDao;

	@Autowired
	private AdminDao adminDao;

	@Autowired
	private AdminLogDao adminLogDao;

	@Autowired
	private DeskDao deskDao;

/*
	// 用户获取自己的邮件列表
	public UserMessage[] mailList(IoSession session) {
		Object obj = session.getAttribute("id");
		Integer userId = Integer.parseInt(obj.toString());
		List<UserMessage> mails = userMailDao.getUserMailList(userId);
		for (UserMessage m : mails) {
			m.setContent("");
		}
		return mails.toArray(new UserMessage[] {});
	}

	// 用户删除邮件 ids是,号分割的id
	public boolean deleteMail(String ids, IoSession session) {
		Object obj = session.getAttribute("id");
		Integer userId = Integer.parseInt(obj.toString());
		try {
			userMailDao.deleteUserMail(ids, userId);
		} catch (Exception e) {
			return false;
		}
		return true;
	}*/

	//游戏获取邮件发送记录
	public Map<String, Object> getMessageList(int type,int pageNumber, int pageSize, IoSession session) {
		
		Pager pager = new Pager();
		pager.setPageSize(pageSize);
		pager.setPageNumber(pageNumber);
		pager.setOrderBy("datetime desc");
		int userId = (Integer)session.getAttribute("id");

		if(type == 0){
			pager = userMessageDao.getMessageListType0(pager);
		}else if(type == 1){
			pager = userMessageDao.getMessageListType1(pager, userId);
		}
		Object[] os = pager.getList();
		for (Object o : os) {
			UserMessage m = (UserMessage) o;
			m.setContent(m.getContent().replaceAll("(\r\n|\r|\n|\n\r)", ""));
		}
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("pageNumber", pageNumber);
		result.put("pageCount", pager.getPageCount());
		result.put("messages", os);
		
		//只要请求了这个借口表示用户读取了所有信息
		userDao.setUserMessageToReadStatus(userId, 0);
		UserService.readMessageNotice(userId, 0, session);
		return result;
	}
	
	public void sendMessage(int userId, UserMessage um){
	   
	   //记录一条用户消息
	   userMessageDao.add(um);

	   userDao.setUserMessageToReadStatus(userId, 1);
	   if(LocalMem.onlineUsers.containsKey(userId)){
		   UserService.readMessageNotice(userId, 1, LocalMem.onlineUsers.get(userId));
	   }
	}

	/*// 发送邮件
	public Map<String, Object> sendMail(Mail mail, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		List<User> sendToUsers = null;
		if (mail.getRangeType() == 3) {
			try {
				int deskId = Integer.valueOf(mail.getIds());
				int deskCount = deskDao.queryTotalCount("select count(*) from Desk where id=?", deskId);
				int fishDeskCount = deskDao.queryTotalCount("select count(*) from FishDesk where id=?", deskId);
				int cardDeskCount = deskDao.queryTotalCount("select count(*) from CardDesk where id=?", deskId);
				int bulletDeskCount = deskDao.queryTotalCount("select count(*) from BulletFishDesk where id=?", deskId);
				if (deskCount <= 0 && fishDeskCount <= 0 && cardDeskCount <= 0 && bulletDeskCount <= 0) {
					result.put("success", false);
					result.put("message", MyUtil.getText(getLanguage(session), "deskIdNotExist"));
					return result;
				}
			} catch (Exception e) {
				result.put("success", false);
				result.put("message", MyUtil.getText(getLanguage(session), "deskIdNotExist"));
				return result;
			}
		} else if (mail.getRangeType() == 4) {
			sendToUsers = new ArrayList<User>();
			String[] usernames = mail.getIds().split(";");
			for (String uname : usernames) {
				User u = userDao.queryByHql("from User where username=?", uname);
				if (u == null) {
					result.put("success", false);
					result.put("message", MyUtil.getText(getLanguage(session), "usernameNotExist"));
					return result;
				}
				sendToUsers.add(u);
			}
		}
		// 保存邮件记录
		Integer adminId = Integer.valueOf(session.getAttribute("id").toString());
		Admin admin = adminDao.queryById(adminId);
		String datetime = MyUtil.dateToString1(new Date());// 发送时间
		mail.setDatetime(datetime);
		mail.setTitle(mail.getTitle().replaceAll("(\r\n|\r|\n|\n\r)", ""));
		mail.setAdmin(admin.getUserName());
		mailDao.addMail(mail);

		String content = null;
		if (mail.getRangeType() == 0) {
			content = "发送了邮件，目标全服";
			List<User> users = userDao.queryListByHql("from User");
			for (User u : users) {
				UserMessage um = new UserMessage(mail.getId(), mail.getTitle(), mail.getContent(), datetime,
						admin.getUserName(), 0, u.getId(), u.getUserName());
				userMailDao.addUserMail(um);
				IoSession s = LocalMem.onlineUsers.get(u.getId());
				if (s != null)
					MessageSend.queue.put(new Data(s, "newUserMail", new Object[] {}));
			}
		} else if (mail.getRangeType() == 1) {
			content = "发送了邮件，目标新手练习厅所有桌";
			List<Desk> desks = deskDao.getAllDesk();
			for (Desk d : desks) {
				if (d.getRoomId() == 2)
					continue;
				List<User> users = LocalMem.luckDeskInfo.get(d.getId());
				if (users != null) {
					for (User u : users) {
						UserMessage um = new UserMessage(mail.getId(), mail.getTitle(), mail.getContent(), datetime,
								admin.getUserName(), 0, u.getId(), u.getUserName());
						userMailDao.addUserMail(um);
					}
				}
			}

		
		
		
		} else if (mail.getRangeType() == 2) {
			content = "发送了邮件，目标欢乐竞技厅所有桌";
			List<Desk> desks = deskDao.getAllDesk();
			for (Desk d : desks) {
				if (d.getRoomId() == 1)
					continue;
				List<User> users = LocalMem.luckDeskInfo.get(d.getId());
				if (users != null) {
					for (User u : users) {
						UserMessage um = new UserMessage(mail.getId(), mail.getTitle(), mail.getContent(), datetime,
								admin.getUserName(), 0, u.getId(), u.getUserName());
						userMailDao.addUserMail(um);
					}
				}
			}

			
		} else if (mail.getRangeType() == 3) {
			content = "发送了邮件，目标桌ID" + mail.getIds();
			Integer deskId = Integer.valueOf(mail.getIds());
			List<User> users = LocalMem.luckDeskInfo.get(deskId);
			if (users != null) {
				for (User u : users) {
					UserMessage um = new UserMessage(mail.getId(), mail.getTitle(), mail.getContent(), datetime,
							admin.getUserName(), 0, u.getId(), u.getUserName());
					userMailDao.addUserMail(um);
				}
			}

		
		} else if (mail.getRangeType() == 4) {
			content = "发送了邮件，目标会员账号" + mail.getIds();
			for (User u : sendToUsers) {
				UserMessage um = new UserMessage(mail.getId(), mail.getTitle(), mail.getContent(), datetime,
						admin.getUserName(), 0, u.getId(), u.getUserName());
				userMailDao.addUserMail(um);
				if (LocalMem.onlineUsers.containsKey(u.getId()))
					MessageSend.queue.put(new Data(LocalMem.onlineUsers.get(u.getId()), "newUserMail", new Object[] {}));
			}
		}
		AdminLog adminLog = new AdminLog(admin.getUserName(), content, AdminLog.OTHER);
		adminLogDao.add(adminLog);
		return result;
	}*/

	/*// 删除邮件发送记录
	public Map<String, Object> deleteMail(int id, IoSession session) {
		Map<String, Object> result = new HashMap<String, Object>();
		try {
			mailDao.delete(id);
			result.put("success", true);
			result.put("message", "");
			// 删除玩家邮件
			List<UserMessage> umails = userMailDao.getMailByMailId(id);
			for (UserMessage m : umails) {
				userMailDao.deleteUserMail(m.getId() + "", m.getUserId());
			}
		} catch (Exception e) {
			result.put("success", true);
			result.put("message", "");
		}
		int adminId = (Integer) session.getAttribute("id");
		Admin admin = adminDao.queryById(adminId);
		AdminLog adminLog = new AdminLog(admin.getUserName(), "删除了邮件", AdminLog.OTHER);
		adminLogDao.add(adminLog);
		return result;
	}*/
}
