package com.miracle9.game.socket;

import java.util.HashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.springframework.stereotype.Service;

import com.miracle9.game.util.MyUtil;
import com.miracle9.game.util.MyUtilForGame;

/**
 * 发送数据
 */
@Service("jsonMessageSend")
public class JSONMessageSend extends Thread {
	private static Logger logger = Logger.getLogger(JSONMessageSend.class);
	public static Queue queue = new Queue();
	private ThreadPoolExecutor pool = new ThreadPoolExecutor(20, 40, 3, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());

	// 启动线程
	public JSONMessageSend() {
		start();
	}

	public void run() {
		for (Data data = queue.get(); data != null; data = queue.get()) {
			pool.execute(new DoSend(data));
		}
	}

	/**
	 * 同步执行发送
	 * @param data
	 */
	public static void execute(Data data) {
		try {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("method", data.method);
			map.put("args", data.result);

			byte[] content = MyUtil.GSON.toJson(map).getBytes("UTF-8");
			// 加密
			content = MyUtilForGame.encrypt(content, data.session);
			IoBuffer bb = IoBuffer.allocate(content.length + 4);
			bb.putInt(content.length);
			bb.put(content);
			bb.flip();
			if (data.session != null){
				data.session.write(bb);
			}
			//logger.info(data.method + "：send... " + content.length);
		} catch (Exception e) {
			logger.error(data.method, e);
		}
	}

	private class DoSend extends Thread {
		private Data data;

		public DoSend(Data data) {
			this.data = data;
		}

		public void run() {
			execute(data);
		}
	}

}
