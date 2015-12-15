package com.miracle9.game.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.common.entity.User;
import com.miracle9.game.bean.Pager;
import com.miracle9.game.dao.UserAwardDao;
import com.miracle9.game.dao.UserDao;
import com.miracle9.game.entity.UserAward;
import com.miracle9.game.util.MyUtil;

@Service("userAwardService")
public class UserAwardService {

	@Autowired
	private UserAwardDao userAwardDao;

	@Autowired
	private UserDao userDao;

	// 获取某个用户的赠送信息
	public Pager getUserAward(int userId, String startDate, String endDate, Pager pager, IoSession session) {
		endDate = MyUtil.addDay(endDate, 1);
		User user = userDao.queryById(userId);
		pager = userAwardDao.queryPagerByHql("from UserAward where userId=? and datetime>=? and datetime<?", pager,
				userId, startDate, endDate);
		List<Object> datas = new ArrayList<Object>();
		for (Object o : pager.getList()) {
			UserAward ua = (UserAward) o;
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("datetime", ua.getDatetime());
			data.put("username", user.getUserName());
			data.put("gold", ua.getGold());
			data.put("admin", ua.getAdmin());
			datas.add(data);
		}
		pager.setList(datas.toArray());
		return pager;
	}

	/**
	 * 删除过期数据(小于date)
	 * 
	 * @param date
	 */
	public void deletePastAward(String date) {
		userAwardDao.deletePastAward(date);
	}
}
