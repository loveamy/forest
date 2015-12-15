package com.miracle9.game.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.common.entity.DeskResult;
import com.miracle9.game.bean.DeskRecord;
import com.miracle9.game.dao.DeskResultDao;

@Service("deskResultService")
public class DeskResultService {

	@Autowired
	private DeskResultDao deskResultDao;

	// 获取桌子开奖结果
	public DeskRecord[] resultList(int deskId, IoSession session) {
		List<DeskRecord> deskRecords = new ArrayList<DeskRecord>();
		List<DeskResult> results = deskResultDao.resultList(deskId);
		for (DeskResult r : results) {
			DeskRecord dr = new DeskRecord();
			dr.zhuangXianHe = r.getZxh();
			if (r.getType() == 0) {
				dr.awardType = 0;
				dr.animalType = r.getAnimal();
			} else if (r.getType() == 1) {
				if (r.getGlobalType() == 0) {
					dr.awardType = 2;
					dr.animalType = r.getAnimal();
				} else if (r.getGlobalType() == 1) {
					dr.awardType = 1;
					dr.animalType = r.getAnimal();
				} else if (r.getGlobalType() == 2) {
					dr.awardType = 5;
					dr.songDengCount = r.getSongDengCount();
				} else if (r.getGlobalType() == 3) {
					dr.awardType = 3;
					dr.animalType = r.getAnimal();
				} else if (r.getGlobalType() == 4) {
					dr.awardType = 4;
					dr.animalType = r.getColor();
				}
			}
			if (r.getType() == 2 || r.getLuckType() != 0) {
				dr.animalType = r.getAnimal();
				dr.luckNum = r.getLuckNum();
				
				dr.awardType = 0;
//				if (r.getLuckType() == 0) {
//					dr.awardType = 7;
//				} else if (r.getLuckType() == 1) {
//					dr.awardType = 6;
//				} else if (r.getLuckType() == 2) {
//					dr.awardType = 8;
//				}
			}
			dr.lightningBeilv = r.getLightningBeilv();
			deskRecords.add(dr);
		}
		return deskRecords.toArray(new DeskRecord[] {});
	}

	/**
	 * 删除过期数据(小于date)
	 * 
	 * @param date
	 */
	public void deletePastResult(String date) {
		deskResultDao.deletePastResult(date);
	}

	/**
	 * 保存开奖结果
	 * 
	 * @param deskResult
	 */
	public void addDeskResult(DeskResult deskResult) {
		deskResultDao.add(deskResult);
	}
}
