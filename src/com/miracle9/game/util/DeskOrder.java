package com.miracle9.game.util;

import java.util.Comparator;

import com.miracle9.common.entity.Desk;

/**
 * 桌子排序
 */
public class DeskOrder implements Comparator<Desk> {
	@Override
	public int compare(Desk d1, Desk d2) {
		return d2.getOnlineNumber() - d1.getOnlineNumber();
	}
}
