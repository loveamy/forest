package com.miracle9.game.entity;

import javax.persistence.Entity;

import com.miracle9.common.entity.BaseEntity;

/**
 * 倍率
 */
@Entity
public class Magnification extends BaseEntity {
	private int type;// 46、68、78
	private int lion;// 狮子倍率
	private int panda;// 熊猫倍率
	private int monkey;// 猴子倍率
	private int rabbit;// 兔子倍率
	private int color;// 颜色
	private int zindex;

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getLion() {
		return lion;
	}

	public void setLion(int lion) {
		this.lion = lion;
	}

	public int getMonkey() {
		return monkey;
	}

	public void setMonkey(int monkey) {
		this.monkey = monkey;
	}

	public int getPanda() {
		return panda;
	}

	public void setPanda(int panda) {
		this.panda = panda;
	}

	public int getRabbit() {
		return rabbit;
	}

	public void setRabbit(int rabbit) {
		this.rabbit = rabbit;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public int getZindex() {
		return zindex;
	}

	public void setZindex(int zindex) {
		this.zindex = zindex;
	}

}
