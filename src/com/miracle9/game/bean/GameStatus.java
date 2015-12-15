package com.miracle9.game.bean;


/**
 * 游戏状态
 */
public class GameStatus {
	public int statusIndex;// 0 运营状态；1 维护状态：立刻进入；2 维护状态：预定时间进
	public int time;// 进入维护的时间0-23
	public String content;// 维护内容
	public int cooperateMode;// 是否开启合作运营模式
	public String cooperateStartDate;// 开启时间
	public String cooperateEndDate;// 结束时间
}
