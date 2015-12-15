package com.miracle9.game.socket;

import java.util.HashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.springframework.stereotype.Service;

import wox.serial.SimpleWriter;
import wox.serial.XMLUtil;

import com.miracle9.game.util.MyUtil;

/**
 * 发送数据
 */
@Service("messageSend")
public class XMLMessageSend extends Thread {
	private static Logger logger = Logger.getLogger(XMLMessageSend.class);
	public static Queue queue = new Queue();
	private ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 20, 3, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());

	// 启动线程
	public XMLMessageSend() {
		start();
	}

	public void run() {
		for (Data data = queue.get(); data != null; data = queue.get()) {
			pool.execute(new DoSend(data));
		}
	}

	/**
	 * 执行发送
	 * 
	 * @param data
	 */
	public static void execute(Data data) {
		try {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("method", data.method);
			map.put("args", data.result);

			SimpleWriter w = new SimpleWriter();
			String str = XMLUtil.element2String(w.write(map));
			byte[] content = str.getBytes("UTF-8");
			// 加密
			content = MyUtil.encrypt(content, data.session);
			IoBuffer bb = IoBuffer.allocate(content.length + 4);
			bb.putInt(content.length);
			bb.put(content);
			bb.flip();
			if (data.session != null) {
				data.session.write(bb);
			}
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
