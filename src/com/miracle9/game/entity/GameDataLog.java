package com.miracle9.game.entity;

import javax.persistence.Entity;

import com.miracle9.common.entity.BaseEntity;

/**
 * 游戏运行统计
 */
@Entity
public class GameDataLog extends BaseEntity {
	private int type;// 游戏类型0-六狮 1-捕鱼
	private String datetime;// 日期
	private double sumYaGold;// 当天用户总玩游戏币
	private double sumDeGold;// 当天用户总得游戏币

	public GameDataLog() {

	}

	public GameDataLog(int type, String datetime, double sumYaGold, double sumDeGold) {
		super();
		this.type = type;
		this.datetime = datetime;
		this.sumYaGold = sumYaGold;
		this.sumDeGold = sumDeGold;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getDatetime() {
		return datetime;
	}

	public void setDatetime(String datetime) {
		this.datetime = datetime;
	}

	public double getSumYaGold() {
		return sumYaGold;
	}

	public void setSumYaGold(double sumYaGold) {
		this.sumYaGold = sumYaGold;
	}

	public double getSumDeGold() {
		return sumDeGold;
	}

	public void setSumDeGold(double sumDeGold) {
		this.sumDeGold = sumDeGold;
	}

}
