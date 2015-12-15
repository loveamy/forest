package com.miracle9.game.util;

import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.apache.mina.core.session.IoSession;

import com.google.gson.Gson;

public class MyUtilForGame {
	private static Logger logger = Logger.getLogger(MyUtilForGame.class);
	public static int RAND_MAX = 0x7fff;
	public static Random mRand = new Random();
	public static int FISH_ID = 1000;
	public static Gson GSON = new Gson();


	/**
	 * 通信加密for游戏端通信，不带base64加密
	 * 
	 * @param sSrc
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public static byte[] encrypt(byte[] bytes, IoSession session) throws Exception {
		if (LocalMem.encrypt_key_map.containsKey(session)) {
			String key = LocalMem.encrypt_key_map.get(session);
			byte[] raw = key.getBytes("utf-8");
			SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");// "算法/模式/补码方式"
			IvParameterSpec iv = new IvParameterSpec("abcdefghijklmnop".getBytes("utf-8"));
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
			byte[] encrypted = cipher.doFinal(bytes);
			return encrypted;
			// return Base64.encode(encrypted);// 此处使用BASE64做转码功能，同时能起到2次加密的作用。
		}
		return bytes;
	}

	/**
	 * 通信解密for游戏端通信，不带base64解密
	 * 
	 * @param sSrc
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public static byte[] decrypt(byte[] bytes, IoSession session) throws Exception {
		try {
			if (LocalMem.encrypt_key_map.containsKey(session)) {
				String key = LocalMem.encrypt_key_map.get(session);
				byte[] raw = key.getBytes("utf-8");
				SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
				Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
				IvParameterSpec iv = new IvParameterSpec("abcdefghijklmnop".getBytes("utf-8"));
				cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
				// byte[] encrypted1 = Base64.decode(bytes);// 先用base64解密
				try {
					byte[] original = cipher.doFinal(bytes);
					return original;
				} catch (Exception e) {
					System.out.println(e.toString());
					return null;
				}
			} else {
				return bytes;
			}
		} catch (Exception ex) {
			logger.error("", ex);
			return null;
		}
	}

	/**
	 * 通信加密for游戏端通信，不带base64加密
	 * 
	 * @param sSrc
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public static byte[] encrypt(byte[] bytes, String key) throws Exception {
		byte[] raw = key.getBytes("utf-8");
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");// "算法/模式/补码方式"
		IvParameterSpec iv = new IvParameterSpec("abcdefghijklmnop".getBytes("utf-8"));
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
		byte[] encrypted = cipher.doFinal(bytes);
		return encrypted;
		// return Base64.encode(encrypted);// 此处使用BASE64做转码功能，同时能起到2次加密的作用。
	}

	/**
	 * 通信解密for游戏端通信，不带base64解密
	 * 
	 * @param sSrc
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public static byte[] decrypt(byte[] bytes, String key) throws Exception {
		try {
			byte[] raw = key.getBytes("utf-8");
			SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			IvParameterSpec iv = new IvParameterSpec("abcdefghijklmnop".getBytes("utf-8"));
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
			// byte[] encrypted1 = Base64.decode(bytes);// 先用base64解密
			try {
				byte[] original = cipher.doFinal(bytes);
				return original;
			} catch (Exception e) {
				System.out.println(e.toString());
				return null;
			}
		} catch (Exception ex) {
			System.out.println(ex.toString());
			return null;
		}
	}

}
