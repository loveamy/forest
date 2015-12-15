package com.miracle9.game.test;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.google.gson.Gson;
import com.miracle9.game.socket.TcpServerProtocolCodecFactory;
import com.miracle9.game.util.MyUtil;
import com.miracle9.game.util.MyUtilForGame;

public class Test {
	public static String key = null;
	private static RSAPrivateKey privateKey;

	public static void main(String[] args) {
		IoConnector conn = new NioSocketConnector();
		conn.setHandler(new IoHandler() {

			@Override
			public void sessionOpened(final IoSession session) throws Exception {
				System.out.println("sessionOpened...");
				// 生成公钥私钥 发送公钥指数和系数给服务器
				KeyPairGenerator keyPairGen = KeyPairGenerator
						.getInstance("RSA");
				keyPairGen.initialize(512);
				KeyPair keyPair = keyPairGen.generateKeyPair();
				RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
				privateKey = (RSAPrivateKey) keyPair.getPrivate();

				String m = com.sun.org.apache.xml.internal.security.utils.Base64
						.encode(publicKey.getModulus());
				String indexStr = m.substring(0, 1);
				String lastStr = m.substring(m.length() - 1, m.length());
				StringBuilder sb = new StringBuilder(m.substring(1,
						m.length() - 1));
				m = indexStr + sb.reverse() + lastStr;
				String ex = com.sun.org.apache.xml.internal.security.utils.Base64
						.encode(publicKey.getPublicExponent());

				sendMessage(session, "userService/publicKeyForJSON",
						new Object[]{m, ex});
				try {
				} catch (Exception e) {
					e.printStackTrace();
				}
				new Thread() {
					public void run() {
						while (session.isConnected()) {
							try {
								sleep(10000);
								sendMessage(session, "userService/heart",
										new Object[]{});
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}.start();
			}

			@Override
			public void sessionIdle(IoSession arg0, IdleStatus arg1)
					throws Exception {

			}

			@Override
			public void sessionCreated(IoSession arg0) throws Exception {

			}

			@Override
			public void sessionClosed(IoSession arg0) throws Exception {
				System.out.println("sessionClosed..");
			}

			@Override
			public void messageSent(IoSession arg0, Object arg1)
					throws Exception {

			}

			@SuppressWarnings("unchecked")
			@Override
			public void messageReceived(IoSession arg0, Object arg1)
					throws Exception {
				try {
					IoBuffer buffer = (IoBuffer) arg1;
					int dataLength = buffer.getInt();
					// System.out.println(dataLength);
					byte[] contentBytes = new byte[dataLength];
					buffer.get(contentBytes);
					System.out.println(new String(contentBytes, "UTF-8"));
					// 数据解密
					byte[] bb = null;
					if (key != null) {
						// 解密
						bb = MyUtilForGame.decrypt(contentBytes, key);
					}
					String jsonStr = null;
					try {
						jsonStr = new String(bb, "UTF-8");
					} catch (Exception e) {
						e.printStackTrace();
						jsonStr = new String(contentBytes);
					}
					System.out.println(jsonStr);
					// 解析数据
					JSONObject jo = JSONObject.fromObject(jsonStr);
					String method = jo.getString("method");
					Object[] args = jo.getJSONArray("args").toArray();
					if (method.equals("sendServerTime")) {
						Map<String, Object> arg = (Map<String, Object>) JSONObject
								.toBean(jo.getJSONArray("args")
										.getJSONObject(0), Map.class);
						String key = (String) arg.get("key");
						key = MyUtil.decryptByPrivateKey(key.getBytes(),
								privateKey);
						// String indexStr = key.substring(0, 1);
						// String lastStr = key.substring(key.length() - 1,
						// key.length());
						// StringBuilder sb = new StringBuilder(key.substring(1,
						// key.length() - 1));
						Test.key = key;
						System.out.println(Test.key);
						sendMessage(arg0, "userService/userLogin",
								new Object[]{"123456", "123456"});
					} else if (method.equals("userLogin")) {
						System.out.println("userLogin...");
						// sendMessage(arg0, "userService/heart", new
						// Object[]{});
					} else if (method.equals("heart")) {
						System.out.println("heart...");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void exceptionCaught(IoSession arg0, Throwable arg1)
					throws Exception {

			}

			public void sendMessage(IoSession session, String method,
					Object... arg) {
				try {
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("method", method);
					map.put("args", arg);
					map.put("time", System.currentTimeMillis());

					Gson gson = new Gson();
					byte[] content_out = gson.toJson(map).getBytes();
					if (key != null) {
						content_out = MyUtilForGame.encrypt(content_out, key);
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
		conn.getFilterChain().addLast("codeFilter",
				new ProtocolCodecFilter(new TcpServerProtocolCodecFactory()));
		conn.connect(new InetSocketAddress("121.41.12.146", 10010));
	}

}
