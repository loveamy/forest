package com.miracle9.game.entity;

import javax.persistence.Column;
import javax.persistence.Entity;

import com.miracle9.common.entity.BaseEntity;

/**
 * 管理用户
 */
@Entity
public class Admin extends BaseEntity {
	@Column(length = 50, nullable = false, unique = true)
	private String username;
	private String password;
	private String datetime;
	private int type;// 0-超级 1-二级

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDatetime() {
		return datetime;
	}

	public void setDatetime(String datetime) {
		this.datetime = datetime;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

}
