package com.miracle9.game.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.miracle9.game.bean.Pager;
import com.miracle9.game.socket.Data;
import com.miracle9.game.socket.XMLMessageSend;
import com.miracle9.game.socket.TcpServerProtocolCodecFactory;
import com.miracle9.game.util.MyUtil;

import flex.messaging.io.SerializationContext;
import flex.messaging.io.amf.Amf3Input;
import flex.messaging.io.amf.Amf3Output;

public class MinaManageTest {
	public static String key = null;
	public static RSAPrivateKey privateKey;

	public static void main(String[] args) {
		IoConnector conn = new NioSocketConnector();
		conn.setHandler(new IoHandler() {

			@Override
			public void sessionOpened(IoSession session) throws Exception {
				try {
					System.out.println("session opened...");
					// 生成公钥私钥 发送公钥指数和系数给服务器
					KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
					keyPairGen.initialize(512);
					KeyPair keyPair = keyPairGen.generateKeyPair();
					RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
					privateKey = (RSAPrivateKey) keyPair.getPrivate();

					String m = com.sun.org.apache.xml.internal.security.utils.Base64.encode(publicKey.getModulus());
					String indexStr = m.substring(0, 1);
					String lastStr = m.substring(m.length() - 1, m.length());
					StringBuilder sb = new StringBuilder(m.substring(1, m.length() - 1));
					m = indexStr + sb.reverse() + lastStr;
//					String e = com.sun.org.apache.xml.internal.security.utils.Base64.encode(publicKey
//							.getPublicExponent());

					XMLMessageSend.queue.put(new Data(session, "adminService/login_getEncrytKey", new Object[] { m }));

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void sessionIdle(IoSession arg0, IdleStatus arg1) throws Exception {

			}

			@Override
			public void sessionCreated(IoSession arg0) throws Exception {

			}

			@Override
			public void sessionClosed(IoSession arg0) throws Exception {
				System.out.println("close..");
			}

			@Override
			public void messageSent(IoSession arg0, Object arg1) throws Exception {

			}

			@SuppressWarnings({ "unchecked" })
			@Override
			public void messageReceived(IoSession arg0, Object arg1) throws Exception {
				try {
					IoBuffer buffer = (IoBuffer) arg1;
					int dataLength = buffer.getInt();
					byte[] contentBytes = new byte[dataLength];
					buffer.get(contentBytes);
					// 数据解密
					if (key != null) {
						// 解密
						contentBytes = MyUtil.decrypt(contentBytes, key);
					}
					// 解析数据
					Amf3Input ai = new Amf3Input(SerializationContext.getSerializationContext());
					ai.setInputStream(new ByteArrayInputStream(contentBytes));
					HashMap<?, ?> map = (HashMap<?, ?>) (ai.readObject());

					String method = (String) map.get("method");
					Object[] args = (Object[]) map.get("args");

					System.out.println(method + ":" + contentBytes.length);
					if (method.equals("login_getEncrytKey")) {
						Map<String, Object> arg = (Map<String, Object>) args[0];
						String key = arg.get("key").toString();
						key = new String(new Base64().decode(key));
						String indexStr = key.substring(0, 1);
						String lastStr = key.substring(key.length() - 1, key.length());
						StringBuilder sb = new StringBuilder(key.substring(1, key.length() - 1));
						MinaManageTest.key = indexStr + sb.reverse() + lastStr;

						// for (int i = 0; i < 20; i++)
						sendMessage(arg0, "adminService/adminLogin", new Object[] { "admin", "888888", true, true, 1 });
					} else if (method.equals("adminLogin")) {
						// Map<String, Object> loginMap = (Map) args[0];
						// SystemConfig config = (SystemConfig)
						// loginMap.get("config");
						// System.out.println(config.getBaodanStatus());
						// Pager pager = new Pager();
						sendMessage(arg0, "deskService/getDeskList", new Object[] {});
					} else if (method.equals("promoterPayLogs")) {
						Pager pager = (Pager) args[0];
						System.out.println(pager);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void exceptionCaught(IoSession arg0, Throwable arg1) throws Exception {

			}

			public void sendMessage(IoSession session, String method, Object... arg) {
				try {
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("method", method);
					map.put("args", arg);
					map.put("time", System.currentTimeMillis());

					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					Amf3Output amf3Output = new Amf3Output(SerializationContext.getSerializationContext());
					amf3Output.setOutputStream(bout);
					amf3Output.writeObject(map);
					amf3Output.flush();
					amf3Output.close();

					byte[] content_out = bout.toByteArray();
					if (key != null) {
						content_out = MyUtil.encrypt(content_out, key);
					}
					IoBuffer buffer = IoBuffer.allocate(content_out.length + 4);
					buffer.putInt(content_out.length);
					buffer.put(content_out);
					buffer.flip();
					session.write(buffer);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		conn.getFilterChain().addLast("codeFilter", new ProtocolCodecFilter(new TcpServerProtocolCodecFactory()));
		// conn.connect(new InetSocketAddress(8888));
		conn.connect(new InetSocketAddress("120.24.211.168", 8888));
	}

}
