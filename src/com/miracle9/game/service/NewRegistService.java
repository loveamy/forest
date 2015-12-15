package com.miracle9.game.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.game.dao.NewRegistDao;
import com.miracle9.game.entity.NewRegist;

@Service("newRegistService")
public class NewRegistService {

	@Autowired
	private NewRegistDao newRegistDao;

	public void addNewRegist(NewRegist newRegist) {
		newRegistDao.addNewRegist(newRegist);
	}
}
