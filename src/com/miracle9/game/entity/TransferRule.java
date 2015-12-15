package com.miracle9.game.entity;

import javax.persistence.Entity;

import com.miracle9.common.entity.BaseEntity;

@Entity
public class TransferRule extends BaseEntity {
	
	private int minRemainGold; //最少保留游戏币
	private int minTransferGold; //最少起送游戏币
	private int feePercent; //每次赠送收取手续费比例
	
	
	public int getMinRemainGold() {
		return minRemainGold;
	}
	public void setMinRemainGold(int minRemainGold) {
		this.minRemainGold = minRemainGold;
	}
	public int getMinTransferGold() {
		return minTransferGold;
	}
	public void setMinTransferGold(int minTransferGold) {
		this.minTransferGold = minTransferGold;
	}
	public int getFeePercent() {
		return feePercent;
	}
	public void setFeePercent(int feePercent) {
		this.feePercent = feePercent;
	}
	
	
	
}
