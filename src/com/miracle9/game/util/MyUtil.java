package com.miracle9.game.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAPublicKeySpec;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.apache.mina.core.session.IoSession;

import com.google.gson.Gson;
import com.miracle9.game.bean.UserDesk;

public class MyUtil {
	private static Logger logger = Logger.getLogger(MyUtil.class);
	public static int RAND_MAX = 0x7fff;
	public static Random mRand = new Random();
	public static Gson GSON = new Gson();

	public static String numberStr = "123456789";

	/**
	 * 随机生成密码
	 * 
	 * @return
	 */
	public static String generatePassword(int length) {
		try {
			String src = "abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz";
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < length; i++) {
				int index = (int) (Math.random() * src.length());

				if (index < src.length()) {
					sb.append(src.substring(index, index + 1));
				}
			}

			return sb.toString();
		} catch (Exception e) {

		}

		return "";
	}

	public static int[][] convertListArrayToArrayArray(List<int[]> listArray) {
		int[][] a = new int[listArray.size()][];
		for (int i = 0; i < listArray.size(); i++) {
			a[i] = new int[listArray.get(i).length];
			for (int j = 0; j < listArray.get(i).length; j++) {
				a[i][j] = listArray.get(i)[j];
			}
		}
		return a;
	}

	/**
	 * 桌子的筹码在数据库中是100|1000|10000，转换成数组
	 */
	public static int[] convertChips(String chipStr) {
		String[] chipsStr = chipStr.split("\\|");
		return MyUtil.stringArrayToIntArray(chipsStr);
	}

	/**
	 * int转成长度为4的byte数组
	 * 
	 * @param i
	 * @return
	 */
	public static byte[] intToByteArray(int i) {
		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(buf);
			out.writeInt(i);
			byte[] b = buf.toByteArray();
			out.close();
			buf.close();
			return b;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * byte数组转int
	 * 
	 * @param bytes
	 * @return
	 */
	public static int byteArrayToInt(byte[] bytes) {
		try {
			ByteArrayInputStream bytein = new ByteArrayInputStream(bytes);
			DataInputStream in = new DataInputStream(bytein);
			int i = in.readInt();
			bytein.close();
			in.close();
			return i;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	public static int[] stringArrayToIntArray(String[] strArray) {
		int[] intArray = new int[strArray.length];
		for (int i = 0; i < strArray.length; i++) {
			intArray[i] = Integer.parseInt(strArray[i]);
		}
		return intArray;
	}

	/**
	 * Date转String
	 * 
	 * @param date
	 * @return 日期格式：yyyy-MM-dd HH:mm:ss
	 */
	public static String dateToString1(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(date);
	}

	public static String getCurrentTimestamp() {
		return dateToString1(new Date());
	}

	/**
	 * Date转String
	 * 
	 * @param date
	 * @return 日期格式：yyyy-MM-dd
	 */
	public static String dateToString2(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(date);
	}

	/**
	 * String转Date
	 * 
	 * @param dateStr
	 *            字符串格式：yyyy-MM-dd HH:mm:ss
	 * @return
	 */
	public static Date stringToDate1(String dateStr) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			return sdf.parse(dateStr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Date();
	}

	/**
	 * String转Date
	 * 
	 * @param dateStr
	 *            字符串格式：yyyy-MM-dd
	 * @return
	 */
	public static Date stringToDate2(String dateStr) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			return sdf.parse(dateStr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Date();
	}

	/**
	 * 自定义格式String转Date
	 */
	public static Date stringToDate(String dateStr, String format) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			return sdf.parse(dateStr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Date();
	}

	/**
	 * 通信加密
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
			// return encrypted;
			return new Base64().encode(encrypted);// 此处使用BASE64做转码功能，同时能起到2次加密的作用。
		}
		return bytes;
	}

	/**
	 * 通信解密
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
				byte[] encrypted1 = new Base64().decode(bytes);// 先用base64解密
				try {
					byte[] original = cipher.doFinal(encrypted1);
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
	 * 通信加密
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
		return new Base64().encode(encrypted);// 此处使用BASE64做转码功能，同时能起到2次加密的作用。
	}

	/**
	 * 通信解密
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
			byte[] encrypted1 = new Base64().decode(bytes);// 先用base64解密
			byte[] original = cipher.doFinal(encrypted1);
			return original;
		} catch (Exception e) {
			return bytes;
		}
	}

	/**
	 * 提供精确的乘法运算并且保留两位小数
	 */
	public static double multiply(double v1, double v2) {
		BigDecimal b1 = new BigDecimal(Double.toString(v1));
		BigDecimal b2 = new BigDecimal(Double.toString(v2));
		return format(b1.multiply(b2).doubleValue());
	}

	/**
	 * 提供精确的除法运算并且保留两位小数
	 */
	public static double divide(double v1, double v2) {
		BigDecimal b1 = new BigDecimal(Double.toString(v1));
		BigDecimal b2 = new BigDecimal(Double.toString(v2));
		return b1.divide(b2, 2, BigDecimal.ROUND_DOWN).doubleValue();
	}

	/**
	 * 提供精确的减法运算并且保留两位小数
	 */
	public static double subtract(double v1, double v2) {
		BigDecimal b1 = new BigDecimal(Double.toString(v1));
		BigDecimal b2 = new BigDecimal(Double.toString(v2));
		return format(b1.subtract(b2).doubleValue());
	}

	/**
	 * 提供精确的加法运算并且保留两位小数
	 */
	public static double add(double v1, double v2) {
		BigDecimal b1 = new BigDecimal(Double.toString(v1));
		BigDecimal b2 = new BigDecimal(Double.toString(v2));
		return format(b1.add(b2).doubleValue());
	}

	/**
	 * 格式化保留2位小数
	 * 
	 * @param dou
	 * @return
	 */
	public static double format(double dou) {
		BigDecimal bd = new BigDecimal(String.valueOf(dou));
		BigDecimal setScale = bd.setScale(2, BigDecimal.ROUND_DOWN);
		return setScale.doubleValue();
	}

	/**
	 * 判断两个日期是否是同一个月
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public static boolean isSameMonth(Calendar c1, Calendar c2) {
		return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH);
	}

	/**
	 * 根据语言类型和key获取文字提示
	 * 
	 * @param language
	 *            0-中文 1-英文
	 * @param key
	 * @return
	 */
	public static String getText(Object language, String key) {
		if ("0".equals(language.toString())) {
			String str = LocalMem.languageZh.getProperty(key);
			try {
				// 处理乱码
				str = new String(str.getBytes("ISO-8859-1"), "UTF-8");
				return str;
			} catch (UnsupportedEncodingException e) {
				return str;
			}
		} else {
			return LocalMem.languageEn.getProperty(key);
		}
	}

	public static String getText(IoSession session, String key) {
		return getText(session.getAttribute("language"), key);
	}

	/**
	 * 日期增加day天
	 * 
	 * @param date
	 * @param day
	 * @return
	 */
	public static String addDay(String date, int day) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(MyUtil.stringToDate2(date));
		calendar.add(Calendar.DAY_OF_MONTH, day);
		return MyUtil.dateToString2(calendar.getTime());
	}

	/**
	 * 获取灯的颜色
	 * 
	 * @return
	 */
	// public static int[] getColors() {
	// List<Integer> colorList = new ArrayList<Integer>();
	// for (int i = 0; i < 3; i++) {
	// for (int j = 0; j < 3; j++) {
	// colorList.add(j);
	// }
	// }
	//
	// for (int i = 0; i < 24 - 9; i++) {
	// colorList.add(new Random().nextInt(3));
	// }
	//
	// Collections.shuffle(colorList);
	//
	// int[] colorArray = new int[24];
	// for (int i = 0; i < colorArray.length; i++) {
	// colorArray[i] = colorList.get(i);
	// }
	// return colorArray;
	// }

	// 刷颜色灯，lampArray必须是一个24位的数组
	public static int[] getColors() {
		int[] lampArray = new int[24];
		// 狮子
		int tmpRand1 = rand() % 6;
		int tmpRand2 = rand() % 6;
		int tmpRand3 = rand() % 6;
		while (tmpRand2 == tmpRand1) // 取三个不相等的随机数
		{
			tmpRand2 = rand() % 6;
		}
		while (tmpRand3 == tmpRand2 || tmpRand3 == tmpRand1) {
			tmpRand3 = rand() % 6;
		}
		// 保证狮子覆盖3中颜色
		lampArray[4 * tmpRand1] = LIGHT_TYPE.LightType_Red.GetValue();
		lampArray[4 * tmpRand2] = LIGHT_TYPE.LightType_Green.GetValue();
		lampArray[4 * tmpRand3] = LIGHT_TYPE.LightType_Yellow.GetValue();

		// 其他3个狮子的颜色随机
		for (int i = 0; i < 6; i++) {
			if (i != tmpRand1 && i != tmpRand2 && i != tmpRand3) {
				switch (rand() % 3) {
				case 0:
					lampArray[4 * i] = LIGHT_TYPE.LightType_Red.GetValue();
					break;
				case 1:
					lampArray[4 * i] = LIGHT_TYPE.LightType_Green.GetValue();
					break;
				case 2:
					lampArray[4 * i] = LIGHT_TYPE.LightType_Yellow.GetValue();
					break;
				default:
					lampArray[4 * i] = LIGHT_TYPE.LightType_Red.GetValue();
					break;
				}
			}
		}

		// 熊猫
		tmpRand1 = rand() % 6;
		tmpRand2 = rand() % 6;
		tmpRand3 = rand() % 6;
		while (tmpRand2 == tmpRand1) {
			tmpRand2 = rand() % 6;
		}
		while (tmpRand3 == tmpRand2 || tmpRand3 == tmpRand1) {
			tmpRand3 = rand() % 6;
		}
		lampArray[4 * tmpRand1 + 1] = LIGHT_TYPE.LightType_Red.GetValue();
		lampArray[4 * tmpRand2 + 1] = LIGHT_TYPE.LightType_Green.GetValue();
		lampArray[4 * tmpRand3 + 1] = LIGHT_TYPE.LightType_Yellow.GetValue();
		for (int i = 0; i < 6; i++) {
			if (i != tmpRand1 && i != tmpRand2 && i != tmpRand3) {
				switch (rand() % 3) {
				case 0:
					lampArray[4 * i + 1] = LIGHT_TYPE.LightType_Red.GetValue();
					break;
				case 1:
					lampArray[4 * i + 1] = LIGHT_TYPE.LightType_Green.GetValue();
					break;
				case 2:
					lampArray[4 * i + 1] = LIGHT_TYPE.LightType_Yellow.GetValue();
					break;
				default:
					lampArray[4 * i + 1] = LIGHT_TYPE.LightType_Red.GetValue();
					break;
				}
			}
		}

		// 猴子

		tmpRand1 = rand() % 6;
		tmpRand2 = rand() % 6;
		tmpRand3 = rand() % 6;
		while (tmpRand2 == tmpRand1) {
			tmpRand2 = rand() % 6;
		}
		while (tmpRand3 == tmpRand2 || tmpRand3 == tmpRand1) {
			tmpRand3 = rand() % 6;
		}
		lampArray[4 * tmpRand1 + 2] = LIGHT_TYPE.LightType_Red.GetValue();
		lampArray[4 * tmpRand2 + 2] = LIGHT_TYPE.LightType_Green.GetValue();
		lampArray[4 * tmpRand3 + 2] = LIGHT_TYPE.LightType_Yellow.GetValue();
		for (int i = 0; i < 6; i++) {
			if (i != tmpRand1 && i != tmpRand2 && i != tmpRand3) {
				switch (rand() % 3) {
				case 0:
					lampArray[4 * i + 2] = LIGHT_TYPE.LightType_Red.GetValue();
					break;
				case 1:
					lampArray[4 * i + 2] = LIGHT_TYPE.LightType_Green.GetValue();
					break;
				case 2:
					lampArray[4 * i + 2] = LIGHT_TYPE.LightType_Yellow.GetValue();
					break;
				default:
					lampArray[4 * i + 2] = LIGHT_TYPE.LightType_Red.GetValue();
					break;
				}
			}
		}

		// 兔子

		tmpRand1 = rand() % 6;
		tmpRand2 = rand() % 6;
		tmpRand3 = rand() % 6;
		while (tmpRand2 == tmpRand1) {
			tmpRand2 = rand() % 6;
		}
		while (tmpRand3 == tmpRand2 || tmpRand3 == tmpRand1) {
			tmpRand3 = rand() % 6;
		}
		lampArray[4 * tmpRand1 + 3] = LIGHT_TYPE.LightType_Red.GetValue();
		lampArray[4 * tmpRand2 + 3] = LIGHT_TYPE.LightType_Green.GetValue();
		lampArray[4 * tmpRand3 + 3] = LIGHT_TYPE.LightType_Yellow.GetValue();
		for (int i = 0; i < 6; i++) {
			if (i != tmpRand1 && i != tmpRand2 && i != tmpRand3) {
				switch (rand() % 3) {
				case 0:
					lampArray[4 * i + 3] = LIGHT_TYPE.LightType_Red.GetValue();
					break;
				case 1:
					lampArray[4 * i + 3] = LIGHT_TYPE.LightType_Green.GetValue();
					break;
				case 2:
					lampArray[4 * i + 3] = LIGHT_TYPE.LightType_Yellow.GetValue();
					break;
				default:
					lampArray[4 * i + 3] = LIGHT_TYPE.LightType_Red.GetValue();
					break;
				}
			}
		}
		return lampArray;
	}

	public static int rand() {
		return Math.abs(mRand.nextInt()) % RAND_MAX;
	}

	enum LIGHT_TYPE {
		LightType_Red(0), // 红
		LightType_Green(1), // 绿
		LightType_Yellow(2); // 黄

		// 定义私有变量
		private int nCode;

		// 构造函数，枚举类型只能为私有
		private LIGHT_TYPE(int _nCode) {
			this.nCode = _nCode;
		}

		@Override
		public String toString() {
			return String.valueOf(this.nCode);
		}

		public int GetValue() {
			return this.nCode;
		}
	};

	/**
	 * 使用客户端公钥加密通信key
	 * 
	 * @param serverKey
	 * @param modulus
	 * @return
	 */
	public static String encodeKey(String serverKey, String modulus) {
		return encodeKeyCsharp(serverKey, modulus, "AQAB");
	}

	public static String encodeKeyCsharp(String serverKey, String modulus, String exponent) {
		try {
			String indexStr = modulus.substring(0, 1);
			String lastStr = modulus.substring(modulus.length() - 1, modulus.length());
			StringBuilder sb = new StringBuilder(modulus.substring(1, modulus.length() - 1));
			modulus = indexStr + sb.reverse() + lastStr;

			BigInteger b1 = new BigInteger(1, new Base64().decode(modulus));
			BigInteger b2 = new BigInteger(1, new Base64().decode(exponent));
			RSAPublicKeySpec pkSpec = new RSAPublicKeySpec(b1, b2);

			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PublicKey publicK = keyFactory.generatePublic(pkSpec);
			Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, publicK);

			return new Base64().encodeToString(cipher.doFinal(serverKey.getBytes()));
		} catch (Exception e) {
			logger.error("", e);
		}
		return null;
	}

	/**
	 * 格式化字符串
	 * 
	 * @param msg
	 * @param paramer
	 * @return
	 */
	public static String format(String msg, Object... paramers) {
		Object[] strParamers = new Object[paramers.length];
		for (int i = 0; i < paramers.length; i++) {
			Object paramer = paramers[i];
			strParamers[i] = paramer == null ? "NULL" : paramer.toString();
		}
		return MessageFormat.format(msg, strParamers);
	}

	public static String getFormatText(IoSession session, String key, Object... paramers) {
		return format(getText(session, key), paramers);
	}

	public static int money2Gold(double money, int payScale) {
		return (int) divide(multiply(money, 100), payScale);
	}

	public static double gold2Money(int gold, int payScale) {
		return divide(multiply(gold, payScale), 100);
	}

	public static int getRandom(int end) {
		Random random = new Random();
		return Math.abs(random.nextInt()) % end;
	}

	public static int[] getDeskVirtualTotalBet(UserDesk userDesk) {
		int[] deskBet = new int[15];

		if (userDesk.userBets.size() == 0) {
			for (int i = 0; i < deskBet.length; i++) {
				deskBet[i] = userDesk.virtualDeskBet[i];
			}
		} else {
			for (Entry<Integer, int[]> userDeskBet : userDesk.userBets.entrySet()) {
				for (int i = 0; i < deskBet.length; i++) {
					deskBet[i] = deskBet[i] + userDeskBet.getValue()[i];
				}
			}
			for (int i = 0; i < deskBet.length; i++) {
				deskBet[i] = deskBet[i] + userDesk.virtualDeskBet[i];
			}

		}
		return deskBet;
	}

	public static String createAccount(int length) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			int index = (int) (Math.random() * numberStr.length());
			if (index < numberStr.length()) {
				sb.append(numberStr.substring(index, index + 1));
			}
		}
		return sb.toString();
	}

	public static String decryptByPrivateKey(byte[] data, RSAPrivateKey privateKey) {
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			return new String(cipher.doFinal(new Base64().decode(data)));
		} catch (Exception e) {
			logger.error("", e);
		}
		return null;
	}
	
}
