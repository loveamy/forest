package com.miracle9.game.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;

import com.miracle9.common.entity.BaseEntity;
import com.miracle9.game.util.MyUtil;

/**
 * 管理员操作日志
 */
@Entity
public class AdminLog extends BaseEntity {
	private String admin;
	private String datetime;
	@Column(length = 1000)
	private String content;
	private int type;// 0-会员高级操作 1-大厅操作2-其他操作
	/**
	 * 会员高级操作
	 */
	public static final int USER = 0;
	/**
	 * 大厅操作
	 */
	public static final int HALL = 1;
	/**
	 * 其他操作
	 */
	public static final int OTHER = 2;

	public AdminLog() {

	}

	public AdminLog(String admin, String content, int type) {
		super();
		this.admin = admin;
		this.datetime = MyUtil.dateToString1(new Date());
		this.content = content;
		this.type = type;
	}

	public String getAdmin() {
		return admin;
	}

	public void setAdmin(String admin) {
		this.admin = admin;
	}

	public String getDatetime() {
		return datetime;
	}

	public void setDatetime(String datetime) {
		this.datetime = datetime;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

}
