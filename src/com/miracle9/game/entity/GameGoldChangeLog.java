package com.miracle9.game.entity;

import javax.persistence.Entity;
import javax.persistence.Transient;

import com.miracle9.common.entity.BaseEntity;


/**
 * 会员游戏币变化日志
 */
@Entity
public class GameGoldChangeLog extends BaseEntity {
	
	private int userId;
	private String userName="";
	private int beforeGold;
	private int changeGold;
	private int afterGold;
	private int changeType; //游戏币变化类型,0充值, 1.领取, 2.开奖, 3.赠送给别人, 4.获得，被别人赠送, 5.系统设置
	
	// 临时字段
	@Transient
	private String changeTypeDesc;//游戏币变化类型描述
	
	private String changeTime;
	private String remark;
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
	public int getBeforeGold() {
		return beforeGold;
	}
	public void setBeforeGold(int beforeGold) {
		this.beforeGold = beforeGold;
	}
	public int getChangeGold() {
		return changeGold;
	}
	public void setChangeGold(int changeGold) {
		this.changeGold = changeGold;
	}
	public int getAfterGold() {
		return afterGold;
	}
	public void setAfterGold(int afterGold) {
		this.afterGold = afterGold;
	}
	
	public int getChangeType() {
		return changeType;
	}
	public void setChangeType(int changeType) {
		this.changeType = changeType;
	}
	public String getChangeTime() {
		return changeTime;
	}
	public void setChangeTime(String changeTime) {
		this.changeTime = changeTime;
	}
	public String getRemark() {
		return remark;
	}
	public void setRemark(String remark) {
		this.remark = remark;
	}
	public String getChangeTypeDesc() {
		return changeTypeDesc;
	}
	public void setChangeTypeDesc(String changeTypeDesc) {
		this.changeTypeDesc = changeTypeDesc;
	}
	
	
	
	
	

}
