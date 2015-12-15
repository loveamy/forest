package com.miracle9.game.entity;

import javax.persistence.Entity;

import com.miracle9.common.entity.BaseEntity;

@Entity
public class Marquee extends BaseEntity {
	
	private String content;
	private String datetime;
	
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
	
}
