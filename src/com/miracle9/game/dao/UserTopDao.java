package com.miracle9.game.dao;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import com.miracle9.game.entity.UserTop;
import com.miracle9.game.util.MyUtil;

@Repository
public class UserTopDao extends BaseDao<UserTop, Integer> {

	/**
	 * 大奖排行榜
	 * 
	 * @return
	 */
	@Cacheable(value = "springCache", key = "'topList'+#type+#topListType")
	@SuppressWarnings("unchecked")
	public List<UserTop> topList(int type, int topListType) {
		if (topListType == 2) {// 周排行
			Calendar c = Calendar.getInstance();
			// 把日期设置为本周星期一
			c.set(Calendar.DAY_OF_WEEK, 2);
			String datetime = MyUtil.dateToString2(c.getTime());
			Query query = createQuery("from UserTop where type=? and datetime >=? order by gold desc", type, datetime);
			query.setMaxResults(10);
			return query.list();
		} else if (topListType == 3) {// 总排行
			Query query = createQuery("from UserTop where type=? order by gold desc", type);
			query.setMaxResults(10);
			return query.list();
		} else {// 日排行
			String datetime = MyUtil.dateToString2(new Date());
			Query query = createQuery("from UserTop where type=? and datetime >=? order by gold desc", type, datetime);
			query.setMaxResults(10);
			return query.list();
		}
	}
	
	public void addUserTop(UserTop userTop) {
		add(userTop);
	}


}
