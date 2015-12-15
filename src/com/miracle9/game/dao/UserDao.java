package com.miracle9.game.dao;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import com.miracle9.common.entity.User;

@Repository
public class UserDao extends BaseDao<User, Integer> {

	/**
	 * 修改用户昵称和性别
	 * 
	 * @param integer
	 * @param nickname
	 * @param sex
	 */
	public void updateNicknamAndSex(Integer id, String nickname, char sex, int photoId) {
		String hql = "update User set nickName=?,sex=?,photoId=? where id=?";
		Query query = createQuery(hql, nickname, sex, photoId, id);
		query.executeUpdate();
	}

	/**
	 * 修改用户密码
	 * 
	 * @param userId
	 * @param newPassword
	 */
	public void updatePassword(Integer id, String newPassword) {
		String hql = "update User set password=? where id=?";
		Query query = createQuery(hql, newPassword, id);
		query.executeUpdate();
	}
	
	public void setUserOffline(int userId){
		String hql = "update User set onlineStatus='离线' where id=?";
		Query query = createQuery(hql, userId);
		query.executeUpdate();
	}
	
	public User queryByUserName(String userName){
		return queryByHql("from User where userName=?", userName);
	}
	
	public void setAllUserHasMessageToRead(){
		String hql = "update User set hasMessageToRead=1";
		Query query = createQuery(hql);
		query.executeUpdate();
	}
	
	public void setUserMessageToReadStatus(int userId, int hasMessageToRead){
		String hql = "update User set hasMessageToRead= ? where id=?";
		Query query = createQuery(hql, hasMessageToRead, userId);
		query.executeUpdate();
	}

	/**
	 * 添加游戏币
	 * 
	 * @param gold
	 */
	public int addGameGold(Integer id, int gameGold) {
		getSession().flush();
		getSession().clear();
		List<Object> params = new ArrayList<Object>();
		String hql = "update User set gameGold = gameGold+? where id = ?";
		params.add(gameGold);
		params.add(id);
		if (gameGold < 0) {
			hql += " and gameGold>=?";
			params.add(-gameGold);
		}
		return createQuery(hql, params.toArray()).executeUpdate();
	}
	
		/**
	 * 添加游戏币和游戏分值
	 * 
	 * @param gold
	 */
	public int addGameGold(Integer id, int gameGold, int gameScore) {
		getSession().clear();
		List<Object> params = new ArrayList<Object>();
		String hql = "update User set gameGold = gameGold+?,gameScore=gameScore+? where id = ?";
		params.add(gameGold);
		params.add(gameScore);
		params.add(id);
		if (gameGold < 0) {
			hql += " and gameGold>=?";
			params.add(-gameGold);
		}
		
		return createQuery(hql, params.toArray()).executeUpdate();
	}



	/**
	 * 更新用户最后进入的桌子
	 * 
	 * @param userId
	 * @param deskId
	 */
	/*public void updateLastDeskId(Integer userId, int deskId) {
		createQuery("update User set lastDeskId=? where id = ?", deskId, userId).executeUpdate();
	}*/

	/**
	 * 更新用户状态
	 * 
	 * @param id
	 * @param i
	 */
	public void updateStatus(int id, int status) {
		createQuery("update User set status=? where id = ?", status, id).executeUpdate();
	}

	
	/**
	 * 设置游戏币
	 * 
	 * @param newGold
	 */
	public boolean setGameGold(int id, int gold) {
		getSession().clear();
		int cnt = createQuery("update User set gameGold=? where id = ?", gold, id).executeUpdate();
		if (cnt > 0) {
			return true;
		} else {
			return false;
		}
	}
	/**
	 * 设置会员为超级会员
	 * @param id 会员id
	 * @return
	 */
	public boolean enableSuperUser(int id){
		getSession().clear();
		int cnt = createQuery("update User set userType=1 where id = ?", id).executeUpdate();
		if (cnt > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean disableSuperUser(int id){
		getSession().clear();
		int cnt = createQuery("update User set userType=0 where id = ?", id).executeUpdate();
		if (cnt > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	

	/**
	 * 修改身份信息
	 * 
	 * @param id
	 * @param question
	 * @param answer
	 */
	public void updateIndentityInfo(int id, String question, String answer, String phone, String qq) {
		createQuery("update User set question=?,answer=?,phone=?,qq=? where id = ?", question, answer, phone, qq,
				id).executeUpdate();
	}
	
	
	/**
	 * 	清空玩家信息
	 * 
	 * @param id
	 * @param question
	 * @param answer
	 */
	public void resetUserInfo(int id) {
		createQuery("update User set question='',answer='',mobilePhone='',qq='',realName='',userInfoModified=0 where id = ?",id).executeUpdate();
	}



	/**
	 * 获取指定日期注册用户
	 * 
	 * @param date
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Integer> queryRegistUsers(String date, int type) {
		Query query = null;
		if (type == 0) {
			query = createQuery("select id from User where promoterId=0 and registDate like ?", date + "%");
		} else if (type == 1) {
			query = createQuery("select id from User where promoterId!=0 and registDate like ?", date + "%");
		} else {
			query = createQuery("select id from User where registDate like ?", date + "%");
		}
		return query.list();
	}

	/**
	 * 统计指定日期用户留存
	 * 
	 * @param registUsers
	 * @param date
	 * @return
	 */
	public int getStayCount(List<Integer> registUsers, String date) {
		Query query = getSession().createQuery("select count(*) from User where loginDate like ? and id in(:ids)");
		query.setParameter(0, date + "%");
		query.setParameterList("ids", registUsers);
		return Integer.valueOf(query.uniqueResult().toString());
	}




	/**
	 * 更新未活跃用户状态
	 * 
	 * @param date
	 */
	public void updateNoActive(String date) {
		String hql = "update User set status=1 where (loginDate !='' and loginDate<? and status=0) or (loginDate='' and registDate<? and status=0)";
		createQuery(hql, date, date).executeUpdate();
	}

	/**
	 * 统计总公司所有直属玩家游戏币
	 * 
	 * @return
	 */
	public long sumAdminUserGold() {
		Object sum = createQuery("select sum(gameGold) from User where promoterId=0").uniqueResult();
		if (sum == null) {
			return 0;
		}
		return (Long) sum;
	}

	/**
	 * 统计某个推广员的所有直属会员的游戏币
	 * 
	 * @return
	 */
	public long sumAllChildUserGold(int promoterId) {
		Object sum = createQuery("select sum(gameGold) from User where promoterId=? and status in (0,1)", promoterId).uniqueResult();
		if (sum == null) {
			return 0;
		}
		return (Long) sum;
	}



	/**
	 * 清理缓存
	 * 
	 * @param user
	 */
	public void evict(User user) {
		getSession().evict(user);
	}

	/**
	 * 统计所有玩家
	 * 
	 * @return
	 */
	public int userCount() {
		return queryTotalCount("select count(*) from User");
	}

	/**
	 * 更新登陆时间
	 */
	public void updateLoginDate(int id, String loginDate) {
		executeUpdate("update User set loginDate=? where id=?", loginDate, id);
	}
}
