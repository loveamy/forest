package com.miracle9.game.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import com.miracle9.common.entity.Desk;
import com.miracle9.game.bean.UserDesk;
import com.miracle9.game.service.DeskService;
import com.miracle9.game.service.UserService;

@Service("myApplicationContext")
public class MyApplicationContextUtil implements ApplicationContextAware {
	private static ApplicationContext springContext;
	
	@Autowired
	private DeskService deskService;
	
	@Autowired
	private UserService userService;

	// spring初始化完后会调用此方法
	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		springContext = context;
		initData();
		startThread();
	}

	public static ApplicationContext getContext() {
		return springContext;
	}
	
	private void initData() {
		for (Desk d : deskService.getAllDesk()) {
			UserDesk ud = new UserDesk();
			ud.desk = d;
			ud.resulttime = Long.MAX_VALUE;
			ud.restarttime = System.currentTimeMillis() + 10000;
			ud.colors = MyUtil.getColors();
			LocalMem.desk_user_result.put(d.getId(), ud);
		}
		// 加载游客帐号
		userService.loadVisitorAccount();
	}

	/**
	 * 启动线程
	 */
	private void startThread() {
//		MyTimer timer = (MyTimer) springContext.getBean("myTimer");
//		timer.start();
//
//		MyThread thread = (MyThread) springContext.getBean("myThread");
//		thread.start();
//
//		SessionThread st = (SessionThread) springContext.getBean("sessionThread");
//		st.start();
//
//		OnlineCountThread onlineCountThread = (OnlineCountThread) springContext.getBean("onlineCountThread");
//		onlineCountThread.start();
		
//		ThreadGameControler game = (ThreadGameControler) springContext.getBean("threadGameControler");
//		game.start();
	}
}
