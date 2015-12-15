package com.miracle9.game.bean;

/**
 * 桌内详情
 */
public class Seat {
	public int id;// 座位号 0-5
	public String userNickname = "";
	public String headUrl = "1.png";// 头像地址
	public int gameGold;// 游戏币
	public int userId;// 用户Id
	public int userType;
	public int income;// 收益 得分-压分

	public Seat() {

	}

	public Seat(int id, String userNickname, String headUrl, int gameGold, int userId, int userType, int income) {
		super();
		this.id = id;
		this.userNickname = userNickname;
		this.headUrl = headUrl;
		this.gameGold = gameGold;
		this.userId = userId;
		this.userType = userType;
		this.income = income;
	}

}
