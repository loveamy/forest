package com.miracle9.game.bean;

/**
 * 强制开奖
 */
public class ForceAward {

	private int awardType; // 1普通，6全局送灯，7全局闪电，8全局大三元，9全局大四喜
	private int color; // 0红色，1绿色，2黄色
	private int animal;// 普通0-11 全局大三元时0-3 0狮子，1熊猫，2猴子,3兔子
	private int lightningBeiLv; // 闪电倍率
	private int songDengCount; // 送灯 2-12
	private boolean isLuck;
	private int luckNum;

	public int getAwardType() {
		return awardType;
	}

	public void setAwardType(int awardType) {
		this.awardType = awardType;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public int getAnimal() {
		return animal;
	}

	public void setAnimal(int animal) {
		this.animal = animal;
	}

	public int getLightningBeiLv() {
		return lightningBeiLv;
	}

	public void setLightningBeiLv(int lightningBeiLv) {
		this.lightningBeiLv = lightningBeiLv;
	}

	public int getSongDengCount() {
		return songDengCount;
	}

	public void setSongDengCount(int songDengCount) {
		this.songDengCount = songDengCount;
	}

	public boolean isLuck() {
		return isLuck;
	}

	public void setLuck(boolean isLuck) {
		this.isLuck = isLuck;
	}

	public int getLuckNum() {
		return luckNum;
	}

	public void setLuckNum(int luckNum) {
		this.luckNum = luckNum;
	}

}
