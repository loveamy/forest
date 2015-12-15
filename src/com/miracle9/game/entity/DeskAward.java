package com.miracle9.game.entity;

import javax.persistence.Entity;

import com.miracle9.common.entity.BaseEntity;

/**
 * 桌子大奖
 */
@Entity
public class DeskAward extends BaseEntity {
	private int deskId;
	private int type;// 大奖类型
	private int count;// 出现次数

	public int getDeskId() {
		return deskId;
	}

	public void setDeskId(int deskId) {
		this.deskId = deskId;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

}
