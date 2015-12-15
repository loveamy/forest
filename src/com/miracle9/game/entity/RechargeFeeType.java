package com.miracle9.game.entity;

import javax.persistence.Column;
import javax.persistence.Entity;

import com.miracle9.common.entity.BaseEntity;

@Entity
public class RechargeFeeType extends BaseEntity {
	
	@Column(nullable = false)
	private int feeType;
	@Column(nullable = false)
	private int money;
	@Column(nullable = false)
	private int gold;
	public int getFeeType() {
		return feeType;
	}
	public void setFeeType(int feeType) {
		this.feeType = feeType;
	}
	public int getMoney() {
		return money;
	}
	public void setMoney(int money) {
		this.money = money;
	}
	public int getGold() {
		return gold;
	}
	public void setGold(int gold) {
		this.gold = gold;
	}

	
}
