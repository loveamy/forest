package com.miracle9.game.bean;

/**
 * 开奖记录
 */
public class DeskRecord {
	//奖励类型：取值0-8 0-普通奖、1-闪电奖、2-彩金奖、3-大三元、4-大四喜、5-送灯奖、6-幸运奖-闪电、7-幸运奖-彩金、8-幸运奖-送灯
	public int awardType;
	//中奖的动物类型：取值0-11。顺序为：红色狮子、绿色狮子、黄色狮子、红色熊猫、绿色熊猫、黄色熊猫、红色猴子、绿色猴子、黄色猴子、红色兔子、绿色兔子、黄色兔子
	//全局送灯奖时，此参数无效；全局大三元时，取值0狮子,1熊猫,2猴子,3兔子 全局大四喜时，取值0红,1绿,2黄 三种幸运奖时，此参数表示全局转出的动物类型。
	public int animalType;
	public int songDengCount;//送灯次数 全局送灯时有效
	public int zhuangXianHe;//庄闲和开奖0-庄、1-和、2-闲
	public int luckNum;//幸运奖中奖台号：1-8 幸运奖时有效
	public int lightningBeilv;
}
