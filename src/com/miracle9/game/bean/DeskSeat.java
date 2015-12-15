package com.miracle9.game.bean;

/**
 * 桌子和座位
 */
public class DeskSeat {
	private int deskId;// 桌子ID
	private int seat;// 座位号

	public DeskSeat() {

	}

	public DeskSeat(int deskId, int seat) {
		this.deskId = deskId;
		this.seat = seat;
	}

	public int getDeskId() {
		return deskId;
	}

	public void setDeskId(int deskId) {
		this.deskId = deskId;
	}

	public int getSeat() {
		return seat;
	}

	public void setSeat(int seat) {
		this.seat = seat;
	}

	
}
