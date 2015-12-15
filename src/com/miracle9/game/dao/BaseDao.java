package com.miracle9.game.dao;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import com.miracle9.game.bean.Pager;

/**
 * 
 * @author lxm
 * 
 * @param <E>
 *            实体对象名
 * @param <PK>
 *            主键类型
 */
public class BaseDao<E, PK extends Serializable> {
	@Autowired
	private SessionFactory sessionFactory;

	private Class<E> entityClass;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public BaseDao() {
		this.entityClass = null;
		Class c = getClass();
		Type type = c.getGenericSuperclass();
		if (type instanceof ParameterizedType) {
			Type[] parameterizedType = ((ParameterizedType) type)
					.getActualTypeArguments();
			this.entityClass = (Class<E>) parameterizedType[0];
		}
	}

	protected Session getSession() {
		return sessionFactory.getCurrentSession();
	}

	@SuppressWarnings("unchecked")
	public E queryById(PK id) {
		Assert.notNull(id, "id is required");
		return (E) getSession().get(entityClass, id);
	}

	@SuppressWarnings("unchecked")
	public PK add(E entity) {
		Assert.notNull(entity, "entity is required");
		return (PK) getSession().save(entity);
	}

	public void addOrUpdate(E entity) {
		Assert.notNull(entity, "entity is required");
		getSession().saveOrUpdate(entity);
	}

	public void update(E entity) {
		getSession().flush();
		getSession().clear();
		Assert.notNull(entity, "entity is required");
		getSession().update(entity);
		
	}

	public void delete(E entity) {
		Assert.notNull(entity, "entity is required");
		getSession().delete(entity);
	}

	public void delete(PK id) {
		Assert.notNull(id, "id is required");
		E entity = queryById(id);
		getSession().delete(entity);
	}

	public void delete(PK[] ids) {
		Assert.notEmpty(ids, "ids must not be empty");
		for (PK id : ids) {
			E entity = queryById(id);
			getSession().delete(entity);
		}
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

	/**
	 * 动态执行HQL语句
	 * 
	 * @param hql
	 * @param params
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<E> queryListByHql(final String hql, final Object... params) {
		Assert.hasText(hql, "hql不能为空");
		Query query = getSession().createQuery(hql);
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				query.setParameter(i, params[i]);
			}
		}
		return query.list();
	}

	/**
	 * 动态执行SQL语句
	 * 
	 * @param hql
	 * @param params
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<E> queryListBySql(final String sql, final Object... params) {
		Assert.hasText(sql, "hql不能为空");
		SQLQuery query = getSession().createSQLQuery(sql);
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				query.setParameter(i, params[i]);
			}
		}
		query.addEntity(entityClass);
		return query.list();
	}

	/**
	 * 动态执行HQL语句查询分页
	 * 
	 * @param hql
	 * @param pager
	 * @param params
	 * @return
	 */
	public Pager queryPagerByHql(String hql, Pager pager,
			final Object... params) {
		Assert.hasText(hql, "hql不能为空");
		Query countQuery = getSession().createQuery("select count(*) " + hql);
		// 处理排序
		if (StringUtils.isNotBlank(pager.getOrderBy())) {
			hql += " order by " + pager.getOrderBy();
		}
		if (StringUtils.isNotBlank(pager.getOrder())) {
			hql += " " + pager.getOrder();
		}
		Query query = getSession().createQuery(hql);
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
		query.addEntity(entityClass);
		pager.setTotalCount(Integer.parseInt(countQuery.uniqueResult() + ""));
		pager.setList(query.list().toArray());
		return pager;
	}

	/**
	 * 动态执行HQL查询单条数据
	 * 
	 * @param hql
	 * @param params
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public E queryByHql(String hql, Object... params) {
		Assert.hasText(hql, "hql不能为空");
		Query query = getSession().createQuery(hql);
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				query.setParameter(i, params[i]);
			}
		}
		List<E> es = query.list();
		return es.isEmpty() ? null : es.get(0);
	}

	/**
	 * 动态执行HQL统计数据条数
	 * 
	 * @param hql
	 * @param params
	 * @return
	 */
	public int queryTotalCount(String hql, Object... params) {
		Assert.hasText(hql, "hql不能为空");
		Query query = getSession().createQuery(hql);
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				query.setParameter(i, params[i]);
			}
		}
		return Integer.parseInt(query.uniqueResult() + "");
	}

	/**
	 * 执行更新操作
	 * 
	 * @param hql
	 * @param params
	 * @return
	 */
	public int executeUpdate(String hql, Object... params) {
		Query query = createQuery(hql, params);
		return query.executeUpdate();
	}
}
