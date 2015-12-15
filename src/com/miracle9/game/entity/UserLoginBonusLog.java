package com.miracle9.game.entity;

import javax.persistence.Entity;

import com.miracle9.common.entity.BaseEntity;

@Entity
public class UserLoginBonusLog extends BaseEntity {
	private int userId;
	private int continueLoginDays;
	private int bonus;
	private int status; //连续登录奖励状态0 未领取，1 领取，2，因为后面的连续登录，此条记录作废，,3，因为没有连续登录，此条记录作废
	private String createTime;
	private String updateTime;
	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}
	public int getContinueLoginDays() {
		return continueLoginDays;
	}
	public void setContinueLoginDays(int continueLoginDays) {
		this.continueLoginDays = continueLoginDays;
	}
	public int getBonus() {
		return bonus;
	}
	public void setBonus(int bonus) {
		this.bonus = bonus;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String getCreateTime() {
		return createTime;
	}
	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}
	public String getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}
	
	

}
