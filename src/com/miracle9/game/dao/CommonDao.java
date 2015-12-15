package com.miracle9.game.dao;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import com.miracle9.game.bean.Pager;

@Repository
public class CommonDao{
	@Autowired
	private SessionFactory sessionFactory;

	protected Session getSession() {
		return sessionFactory.getCurrentSession();
	}
	/**
	 * 动态执行SQL语句查询分页
	 * 
	 * @param sql
	 * @param pager
	 * @param params
	 * @return
	 */
	public Pager queryPagerBySql(String sql, Pager pager,
			final Object... params) {
		Assert.hasText(sql, "hql不能为空");
		Query countQuery = getSession().createSQLQuery(
				"select count(*) from (" + sql + ") t");
		// 处理排序
		if (StringUtils.isNotBlank(pager.getOrderBy())) {
			sql += " order by " + pager.getOrderBy();
		}
		if (StringUtils.isNotBlank(pager.getOrder())) {
			sql += " " + pager.getOrder();
		}
		SQLQuery query = getSession().createSQLQuery(sql);
		query.setFirstResult((pager.getPageNumber() - 1) * pager.getPageSize());
		query.setMaxResults(pager.getPageSize());
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				query.setParameter(i, params[i]);
				countQuery.setParameter(i, params[i]);
			}
		}
		pager.setTotalCount(Integer.parseInt(countQuery.uniqueResult() + ""));
		pager.setList(query.list().toArray());
		return pager;
	}
	
	/**
	 * 动态创建一个Query
	 * 
	 * @param hql
	 * @param params
	 * @return
	 */
	public Query createQuery(final String hql, final Object... params) {
		Assert.hasText(hql, "queryString不能为空");
		Query query = getSession().createQuery(hql);
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				query.setParameter(i, params[i]);
			}
		}
		return query;
	}
	
	/**
	 * 动态创建一个SQLQuery
	 * 
	 * @param sql
	 * @param params
	 * @return
	 */
	public SQLQuery createSQLQuery(final String sql, final Object... params) {
		Assert.hasText(sql, "queryString不能为空");
		SQLQuery query = getSession().createSQLQuery(sql);
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				query.setParameter(i, params[i]);
			}
		}
		return query;
	}

}