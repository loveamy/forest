package com.miracle9.game.test;

import java.util.ArrayList;
import java.util.List;

import com.miracle9.common.entity.User;

public class JavaTest {
	
	public static void main(String[] args){
		/*List<int[]> allUserBetsList = new ArrayList<int[]>();
		allUserBetsList.add(new int[]{1,1,1,1,1,1,1,1,1,1,1,1});
		allUserBetsList.add(new int[]{1,1,1,1,1,1,1,1,1,2});
		int[][] a = convertListArrayToArrayArray(allUserBetsList);*/
		/*String s = "a|b|c";
		String[] ss = s.split("\\|");*/
		List<User> u = new ArrayList<User>();
		User u1 = new User();
		User u2 = new User();
		u1.setUserName("haha");
		u2.setUserName("haha");
		u.add(u1);
		System.out.println(u.contains(u1));
		System.out.println(u.contains(u2));
		
	}
	
	public void testRandom(){
		List<Integer> randomIndex = new ArrayList<Integer>();
		randomIndex.add(1);
		int i = 1;
		System.out.println(randomIndex.contains(i));
	}
	
	public static int[][] convertListArrayToArrayArray(List<int[]> listArray){
		int[][] a = new int[listArray.size()][];
		for(int i = 0; i < listArray.size(); i++){
			a[i] = new int[listArray.get(i).length];
			for(int j = 0; j < listArray.get(i).length; j++){
				a[i][j] = listArray.get(i)[j];
			}
		}
		return a;
	}

}
