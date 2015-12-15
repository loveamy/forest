package com.miracle9.game.entity;

import javax.persistence.Column;
import javax.persistence.Entity;

import com.miracle9.common.entity.BaseEntity;

/**
 * 消息系统
 */
@Entity
public class UserMessage extends BaseEntity {
	private String title;
	@Column(updatable = false)
	private String content;
	private String datetime;
	private String sender;// 发件人
	private int status;// 0-未查看 1-已查看
	private int userId;// 收件人
	private int type; //消息类型 0系统公告，1玩家消息
	public UserMessage() {

	}

	public UserMessage( String title, String content, String datetime, String sender, int status,
			int userId, int type) {
		this.title = title;
		this.content = content;
		this.datetime = datetime;
		this.sender = sender;
		this.status = status;
		this.userId = userId;
		this.type = type;
	}
	
	public UserMessage(String title, String content, String datetime, String sender, int status,
			int userId) {
		this.title = title;
		this.content = content;
		this.datetime = datetime;
		this.sender = sender;
		this.status = status;
		this.userId = userId;
		this.type = 0;
	}



	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getDatetime() {
		return datetime;
	}

	public void setDatetime(String datetime) {
		this.datetime = datetime;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}


	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}
	

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}	

}
