package com.miracle9.game.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.game.dao.MagnificationDao;
import com.miracle9.game.entity.Magnification;

@Service("magnificationService")
public class MagnificationService {

	@Autowired
	private MagnificationDao magnificationDao;

	public List<Magnification> getBeilvByType(int type) {
		if (type == 0) {
			type = 46;
		} else if (type == 1) {
			type = 68;
		} else {
			type = 78;
		}
		return magnificationDao.getBeilvByType(type);
	}
}
