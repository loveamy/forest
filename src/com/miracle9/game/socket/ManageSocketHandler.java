package com.miracle9.game.socket;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.springframework.stereotype.Service;

import com.miracle9.game.entity.Admin;
import com.miracle9.game.entity.AdminLog;
import com.miracle9.game.service.AdminLogService;
import com.miracle9.game.service.AdminService;
import com.miracle9.game.util.LocalMem;
import com.miracle9.game.util.MyApplicationContextUtil;
import com.miracle9.game.util.MyUtil;

import flex.messaging.io.SerializationContext;
import flex.messaging.io.amf.Amf3Input;

@Service("manageSocketHandler")
public class ManageSocketHandler implements IoHandler {
	private Logger logger = Logger.getLogger(ManageSocketHandler.class);

	@Override	
	public void exceptionCaught(IoSession session, Throwable arg1) throws Exception {
		logger.error("", arg1);
		session.close(true);
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		try {
			logger.info("管理后台messageReceived...");
			IoBuffer in = (IoBuffer) message;

			int length = in.getInt();
			byte[] contentBytes = new byte[length];
			in.get(contentBytes);
			// 数据解密
			contentBytes = MyUtil.decrypt(contentBytes, session);
			// 解析数据
			Amf3Input ai = new Amf3Input(SerializationContext.getSerializationContext());
			ai.setInputStream(new ByteArrayInputStream(contentBytes));
			HashMap<?, ?> map = (HashMap<?, ?>) (ai.readObject());
			ai.close();
			
			String url = (String) map.get("method");
			Object[] args = (Object[]) map.get("args");
			double time = (Double) map.get("time");// 时间戳
			// 请求时间大于5秒不处理
			if (System.currentTimeMillis() - time >= 5000) {
				logger.info("请求时间大于5秒");
				return;
			}
			String[] urls = url.split("/");
			String beanId = urls[0];
			String method = urls[1];
			String argsStr = method + "请求参数：";
			for (Object o : args) {
				argsStr += o + ";";
			}
			logger.info(argsStr);
			// 验证是否登录
			if (!LocalMem.onlineAdmin.containsValue(session) && !"login_getEncrytKey".equals(method)
					&& !"adminLogin".equals(method)) {
				logger.info("未登录操作");
				return;
			}

			// 在最后增加IoSession参数
			Object[] args_login = new Object[args.length + 1];
			for (int i = 0; i < args.length; i++) {
				args_login[i] = args[i];
			}
			args_login[args_login.length - 1] = session;
			args = args_login;

			Object service = MyApplicationContextUtil.getContext().getBean(beanId);
			Method m = service.getClass().getDeclaredMethod(method, getClasses(args));
			Object result = m.invoke(service, args);
			if (result != null) {
				AmfMessageSend.queue.put(new Data(session, method, new Object[] { result }));
			}
			
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	private Class<?>[] getClasses(Object[] args) {
		try {
			Class<?>[] classArray = new Class<?>[args.length];

			for (int i = 0; i < args.length; i++) {
				classArray[i] = args[i].getClass();

				if (args[i] instanceof Integer) {
					classArray[i] = Integer.TYPE;
					continue;
				} else if (args[i] instanceof Boolean) {
					classArray[i] = Boolean.TYPE;
					continue;
				} else if (args[i] instanceof Map) {
					classArray[i] = Map.class;
					continue;
				} else if (args[i] instanceof Long) {
					classArray[i] = Long.TYPE;
					continue;
				} else if (args[i] instanceof Double) {
					classArray[i] = Double.TYPE;
					continue;
				} else if (args[i] instanceof IoSession) {
					classArray[i] = IoSession.class;
					continue;
				} else if (args[i] instanceof Float) {
					classArray[i] = Float.TYPE;
				}
			}
			return classArray;
		} catch (Exception e) {
			logger.error("", e);
		}
		return null;
	}

	@Override
	public void messageSent(IoSession arg0, Object arg1) throws Exception {

	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		Object id = session.removeAttribute("id");
		if (id != null) {
			LocalMem.onlineAdmin.remove(Integer.valueOf(id.toString()));
			session.removeAttribute("username");
			AdminService adminService = (AdminService) MyApplicationContextUtil.getContext().getBean("adminService");
			Admin admin = adminService.getAdmin(Integer.valueOf(id.toString()));
			AdminLog adminLog = new AdminLog();
			adminLog.setAdmin(admin.getUsername());
			adminLog.setDatetime(MyUtil.dateToString1(new Date()));
			adminLog.setType(2);
			adminLog.setContent("退出管理后台");
			AdminLogService adminLogService = (AdminLogService) MyApplicationContextUtil.getContext().getBean(
					"adminLogService");
			adminLogService.addAdminLog(adminLog);
					}
		if (LocalMem.encrypt_key_map.containsKey(session))
			LocalMem.encrypt_key_map.remove(session);
	}

	@Override
	public void sessionCreated(IoSession arg0) throws Exception {

	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus arg1) throws Exception {
		logger.info("sessionIdle...");
		session.close(true);
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		logger.info("管理后台sessionOpened...");
	}
}
