package com.miracle9.game.entity;

import javax.persistence.Column;
import javax.persistence.Entity;

import com.miracle9.common.entity.BaseEntity;

@Entity
public class RechargeRecord extends BaseEntity {
	@Column(nullable = false)
	private int userId;
	@Column(nullable = false)
	private String userName;
	@Column(nullable = false)
	private int feeType;
	@Column(nullable = false)
	private int money;
	@Column(nullable = false)
	private long gameGold;
	@Column(nullable = false)
	private String createTime;
	
	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}
	
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public int getFeeType() {
		return feeType;
	}
	public void setFeeType(int feeType) {
		this.feeType = feeType;
	}
	public String getCreateTime() {
		return createTime;
	}
	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}
	public int getMoney() {
		return money;
	}
	public void setMoney(int money) {
		this.money = money;
	}
	public long getGameGold() {
		return gameGold;
	}
	public void setGameGold(long gameGold) {
		this.gameGold = gameGold;
	}
	
	
	

}
