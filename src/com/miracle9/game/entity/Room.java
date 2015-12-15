package com.miracle9.game.entity;

import javax.persistence.Entity;

import com.miracle9.common.entity.BaseEntity;
/**
 * 游戏房间
 * @author lxm
 *
 */
@Entity
public class Room extends BaseEntity {
	private String name;
	private int deskNum = 4;// 桌数量
	private int type;// 0-体验 1-竞技

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getDeskNum() {
		return deskNum;
	}

	public void setDeskNum(int deskNum) {
		this.deskNum = deskNum;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

}
