package com.miracle9.game.socket;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.springframework.stereotype.Service;

import com.miracle9.game.util.LocalMem;
import com.miracle9.game.util.MyApplicationContextUtil;
import com.miracle9.game.util.MyUtilForGame;

/**
 * 游戏socket
 */
@Service("gameSocketHandler")
public class GameSocketHandler implements IoHandler {
	private static Logger logger = Logger.getLogger(GameSocketHandler.class);
	public static Set<IoSession> gameClients = new HashSet<IoSession>();

	@Override
	public void exceptionCaught(IoSession session, Throwable arg1) throws Exception {
		logger.error("", arg1);
		session.close(true);
	}

	@Override
	public void messageReceived(IoSession session, Object message)
			throws Exception {
		String method = "";
		try {
			IoBuffer buffer = (IoBuffer) message;
			int dataLength = buffer.getInt();
			byte[] contentBytes = new byte[dataLength];
			buffer.get(contentBytes);
			contentBytes = MyUtilForGame.decrypt(contentBytes, session);
			// 解析数据
			String jsonStr = new String(contentBytes, "UTF-8");
			Object[] args = null;
			String beanId = "userService";
			// long time = System.currentTimeMillis();
			if (!jsonStr.startsWith("{")) {
				method = jsonStr.split(",")[0];
				args = new String[]{jsonStr};
			} else {
				JSONObject jo = JSONObject.fromObject(jsonStr);
				String url = jo.getString("method");
				String[] urls = url.split("/");
				beanId = urls[0];
				method = urls[1];
				args = jo.getJSONArray("args").toArray();
				Object obj = jo.get("time");// 时间戳
				if (obj != null) {
					// time = (Long) obj;
				}
			}

			// String argsStr = method + "请求参数：";
			// for (Object o : args) {
			// argsStr += o + ";";
			// }
			// logger.info(argsStr);

			// 验证是否登录
			if (!LocalMem.onlineUsers.containsValue(session)
					&& !"publicKeyForJSON".equals(method)
					&& !"register".equals(method)
					&& !"resetPassword".equals(method)
					&& !"userLogin".equals(method) && !"heart".equals(method)
					&& !"visitorLogin".equals(method)) {
				logger.error("未登录操作");
				return;
			}

			/*
			 * // 请求时间大于5秒不处理 if (!"publicKey".equals(method)) { if
			 * (System.currentTimeMillis() - time >= 5000) {
			 * logger.error("请求时间大于5秒," + method + (System.currentTimeMillis() -
			 * time)); session.close(true); return; } }
			 */
			// 在最后增加IoSession参数
			Object[] args_login = new Object[args.length + 1];
			for (int i = 0; i < args.length; i++) {
				args_login[i] = args[i];
			}
			args_login[args_login.length - 1] = session;
			args = args_login;

			Object service = MyApplicationContextUtil.getContext().getBean(
					beanId);
			Method m = service.getClass().getDeclaredMethod(method,
					getClasses(args));
			Object result = m.invoke(service, args);
			if (result != null) {
				JSONMessageSend.execute(new Data(session, method,
						new Object[]{result}));
				// execute(new Data(session, method, new Object[] { result }));
			}
		} catch (Exception e) {
			logger.error(method, e);
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
		LocalMem.encrypt_key_map.remove(session);
		gameClients.remove(session);
		logger.info("sessionClosed...");
	}

	@Override
	public void sessionCreated(IoSession arg0) throws Exception {

	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus arg1)
			throws Exception {
		logger.error("IdleStatus...");
		session.close(true);
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		logger.info("sessionOpened...");
		LocalMem.session_time.put(session, System.currentTimeMillis());
		gameClients.add(session);
	}
}
