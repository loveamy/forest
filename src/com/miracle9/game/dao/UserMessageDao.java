package com.miracle9.game.dao;

import org.springframework.stereotype.Repository;

import com.miracle9.game.bean.Pager;
import com.miracle9.game.entity.UserMessage;

@Repository
public class UserMessageDao extends BaseDao<UserMessage, Integer> {

	public void addUserMessage(UserMessage m) {
		add(m);
	}

	/**
	 * 获取用户信息发送记录
	 * 
	 * @param pager
	 * @return
	 */
	public Pager getMessageList(Pager pager, int type) {
		return queryPagerByHql("from UserMessage", pager);
	}
	
	
	public Pager getMessageListType0(Pager pager){
		return queryPagerByHql("from UserMessage where type=0", pager);
	}
	
	public Pager getMessageListType1(Pager pager, int userId){
		return queryPagerByHql("from UserMessage where type=1 and userId=?", pager, userId);
	}

}
