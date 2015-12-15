package com.miracle9.game.bean;

public enum GoldChangeType {
	PAY(0),CLAIM(1),GAME(2),GIVE(3),GIVEN(4),SYSTEM(5);
	private int type;
	
	public int getType(){
		return this.type;
	}
	
	public String getTypeString(){
		if(this.type == GoldChangeType.PAY.getType()){
			return "充值";
		}else if(this.type == GoldChangeType.CLAIM.getType()){
			return "领取";
		}else if(this.type == GoldChangeType.GAME.getType()){
			return "开奖";
		}else if(this.type == GoldChangeType.GIVE.getType()){
			return "赠送";
		}else if(this.type == GoldChangeType.GIVEN.getType()){
			return "获得";
		}else if(this.type == GoldChangeType.SYSTEM.getType()){
			return "系统";
		}else{
			return "未知类型";
		}
	}
	
	public static String getTypeString(int type){
		if(type == GoldChangeType.PAY.getType()){
			return "充值";
		}else if(type == GoldChangeType.CLAIM.getType()){
			return "领取";
		}else if(type == GoldChangeType.GAME.getType()){
			return "开奖";
		}else if(type == GoldChangeType.GIVE.getType()){
			return "赠送";
		}else if(type == GoldChangeType.GIVEN.getType()){
			return "获得";
		}else if(type == GoldChangeType.SYSTEM.getType()){
			return "系统";
		}else{
			return "未知类型";
		}
	}
	
	private GoldChangeType(int type){
		this.type = type;
	}
	

}
