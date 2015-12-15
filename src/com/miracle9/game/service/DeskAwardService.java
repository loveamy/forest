package com.miracle9.game.service;

import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.game.dao.DeskAwardDao;
import com.miracle9.game.entity.DeskAward;

@Service
public class DeskAwardService {
	@Autowired
	private DeskAwardDao deskAwardDao;

	private Random random = new Random();

	/**
	 * 随机获取大奖
	 * 
	 * @return
	 */
	public DeskAward randomDeskAward(int deskId) {
		List<DeskAward> awards = deskAwardDao.queryListByHql("from DeskAward where deskId=?", deskId);
		if (!awards.isEmpty()) {
			DeskAward award = awards.get(random.nextInt(awards.size()));
			award.setCount(award.getCount() + 1);
			return award;
		}
		return null;
	}
}
