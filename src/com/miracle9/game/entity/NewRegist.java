package com.miracle9.game.entity;

import javax.persistence.Entity;

import com.miracle9.common.entity.BaseEntity;

/**
 * 新注册记录
 */
@Entity
public class NewRegist extends BaseEntity {
	private int count;
	private String datetime;
	private int type;// 0-直属 1-非直属 2-所有

	public NewRegist() {

	}

	public NewRegist(int count, String datetime, int type) {
		super();
		this.count = count;
		this.datetime = datetime;
		this.type = type;
	}

	public String getDatetime() {
		return datetime;
	}

	public void setDatetime(String datetime) {
		this.datetime = datetime;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

}
