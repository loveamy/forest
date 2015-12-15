package com.miracle9.game.entity;

import javax.persistence.Column;
import javax.persistence.Entity;

import com.miracle9.common.entity.BaseEntity;

/**
 * 系统设置
 */
@Entity
public class SystemConfig extends BaseEntity {
	
	private int userCheck = 1;// 是否开启用户检测 0-否 1-是
	@Column(length=10240)
	private String helpContent; //帮助内容
	private int notActive = 180;// 未活跃天数 -1不限
	private int gameStatus = 0;// 游戏状态 0-正常 1-维护
	private int openLuckGame = 1;// 是否开启六狮游戏 0-否 1-是
	private int operationStatus = 0;// 运营状态 0-关闭 1-开启
	private String operationDate;// 开启运营的时间
	private int moneyOverrun = -1;// 总额超限 -1不限
	private long operationStopDate;// 结束运营的时间
	private int jiujiIntervalSecond=0; //领取救济金介个时间秒数
	private int jiujiLessThanGold=0; //游戏币少于多少可以领取
	private int jiujiGameGold=0; //每次可以领取多少
	
	private String tips;

	public int getUserCheck() {
		return userCheck;
	}

	public void setUserCheck(int userCheck) {
		this.userCheck = userCheck;
	}

	public int getNotActive() {
		return notActive;
	}

	public void setNotActive(int notActive) {
		this.notActive = notActive;
	}

	public int getGameStatus() {
		return gameStatus;
	}

	public void setGameStatus(int gameStatus) {
		this.gameStatus = gameStatus;
	}

	public int getOpenLuckGame() {
		return openLuckGame;
	}

	public void setOpenLuckGame(int openLuckGame) {
		this.openLuckGame = openLuckGame;
	}

	public int getOperationStatus() {
		return operationStatus;
	}

	public void setOperationStatus(int operationStatus) {
		this.operationStatus = operationStatus;
	}

	public String getOperationDate() {
		return operationDate;
	}

	public void setOperationDate(String operationDate) {
		this.operationDate = operationDate;
	}

	public long getOperationStopDate() {
		return operationStopDate;
	}

	public void setOperationStopDate(long operationStopDate) {
		this.operationStopDate = operationStopDate;
	}

	public String getHelpContent() {
		return helpContent;
	}

	public void setHelpContent(String helpContent) {
		this.helpContent = helpContent;
	}

	public int getJiujiIntervalSecond() {
		return jiujiIntervalSecond;
	}

	public void setJiujiIntervalSecond(int jiujiIntervalSecond) {
		this.jiujiIntervalSecond = jiujiIntervalSecond;
	}
	

	public int getMoneyOverrun() {
		return moneyOverrun;
	}

	public void setMoneyOverrun(int moneyOverrun) {
		this.moneyOverrun = moneyOverrun;
	}

	public int getJiujiLessThanGold() {
		return jiujiLessThanGold;
	}

	public void setJiujiLessThanGold(int jiujiLessThanGold) {
		this.jiujiLessThanGold = jiujiLessThanGold;
	}

	public int getJiujiGameGold() {
		return jiujiGameGold;
	}

	public void setJiujiGameGold(int jiujiGameGold) {
		this.jiujiGameGold = jiujiGameGold;
	}

	public String getTips() {
		return tips;
	}

	public void setTips(String tips) {
		this.tips = tips;
	}
	
}
