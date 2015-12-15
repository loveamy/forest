package com.miracle9.game.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.miracle9.game.entity.NewRegist;

@Repository
public class NewRegistDao extends BaseDao<NewRegist, Integer> {

	public List<NewRegist> getNewRegist(String startDate, String endDate, int userType) {
		return queryListByHql("from NewRegist where type = ? and datetime>=? and datetime<? order by id desc",
				userType, startDate, endDate);
	}

	public void addNewRegist(NewRegist newRegist) {
		add(newRegist);
	}

}
