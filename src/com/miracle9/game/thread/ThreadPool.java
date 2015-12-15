package com.miracle9.game.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.mina.core.session.IoSession;

import com.miracle9.game.service.UserService;
import com.miracle9.game.util.MyApplicationContextUtil;

/**
 * 线程池执行登录后操作
 * 
 * @author yaow
 *
 */
public class ThreadPool {
	private static ExecutorService pool = Executors.newFixedThreadPool(5);

	public static void execute(final IoSession session) {
		pool.execute(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				UserService userService = (UserService) MyApplicationContextUtil.getContext().getBean("userService");
				userService.afterLogin(session);
			}
		});
	}
	
	/**
	 * 创建游客帐号
	 * @param num
	 */
	public static void createVisitorAccount(final int num){
		pool.execute(new Runnable() {

			@Override
			public void run() {
				UserService userService = (UserService) MyApplicationContextUtil.getContext().getBean("userService");
				userService.createVisitorAccount(num);
			}
		});
	}
}
