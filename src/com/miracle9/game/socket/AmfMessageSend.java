package com.miracle9.game.socket;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.springframework.stereotype.Service;

import com.miracle9.game.util.MyUtil;

import flex.messaging.io.SerializationContext;
import flex.messaging.io.amf.Amf3Output;

/**
 * 发送数据
 */
@Service("amfMessageSend")
public class AmfMessageSend extends Thread {
	private static Logger logger = Logger.getLogger(AmfMessageSend.class);
	public static Queue queue = new Queue();
	private ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 20, 3, TimeUnit.SECONDS,
			new SynchronousQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());

	// 启动线程
	public AmfMessageSend() {
		start();
	}

	public static void send(Data data) {
		try {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("method", data.method);// 指令 比如：addUser、deleteUser等
			map.put("args", data.result);// 参数Object[]类型

			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			Amf3Output amf3Output = new Amf3Output(SerializationContext.getSerializationContext());
			amf3Output.setOutputStream(bout);
			amf3Output.writeObject(map);
			amf3Output.flush();
			amf3Output.close();

			byte[] content_out = bout.toByteArray();
			content_out = MyUtil.encrypt(content_out, data.session);
			IoBuffer bb = IoBuffer.allocate(content_out.length + 4);
			bb.putInt(content_out.length);
			bb.put(content_out);
			bb.flip();

			data.session.write(bb);

			//logger.info(data.method + "：send...");
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	public void run() {
		for (Data data = queue.get(); data != null; data = queue.get()) {
			pool.execute(new DoSend(data));
		}
	}

	private class DoSend extends Thread {
		private Data data;

		public DoSend(Data data) {
			this.data = data;
		}

		public void run() {
			send(data);
		}
	}

}
