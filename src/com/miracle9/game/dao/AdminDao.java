package com.miracle9.game.dao;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import com.miracle9.game.entity.Admin;

@Repository
public class AdminDao extends BaseDao<Admin, Integer> {

	/**
	 * 管理员列表
	 * 
	 * @return
	 */
	@Cacheable(value = "springCache", key = "'adminList'")
	public List<Admin> adminList() {
		return queryListByHql("from Admin");
	}

	/**
	 * 添加管理员
	 * 
	 * @param admin
	 */
	@CacheEvict(value = "springCache", key = "'adminList'")
	public void addAdmin(Admin admin) {
		add(admin);
	}

	/**
	 * 删除管理员
	 * 
	 * @param id
	 */
	@CacheEvict(value = "springCache", key = "'adminList'")
	public void deleteAdmin(int id) {
		delete(id);
	}

	/**
	 * 更新管理员权限
	 * 
	 * @param id
	 * @param type
	 */
	@CacheEvict(value = "springCache", key = "'adminList'")
	public void updateAuth(Admin admin) {
		update(admin);
	}

}
