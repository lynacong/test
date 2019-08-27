package wsTest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class Arithmetic {

	/**
	 * base64����
	 * 
	 * @param str
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String encode(String str) throws UnsupportedEncodingException {
		String target = null;
		if (str != null) {
			target = new String(Base64.encode(str.getBytes("GBK")));

		}
		return target;
	}

	/**
	 * base64����
	 * 
	 * @param str
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String encode(byte[] str) throws UnsupportedEncodingException {
		String target = null;
		if (str != null) {
			target = new String(Base64.encode(str));
		}
		return target;
	}

	/**
	 * base64����
	 * 
	 * @param str
	 * @return
	 * @throws IOException
	 */
	public static String decode(String str) throws IOException {
		byte[] dec = Base64.decode(str.getBytes());
		return new String(dec, "GBK");
	}

	/**
	 * base64����
	 * 
	 * @param str
	 * @return
	 * @throws IOException
	 */
	public static byte[] decodeForByte(String str) {
		try {
			return Base64.decode(str.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * ������ת��Ϊ�ַ��ʽ���磺{"1","2","3"} ---> "1,2,3"
	 * 
	 * @param array
	 * @return
	 */
	public static String array2String(String[] array) {
		StringBuffer voucherNos = new StringBuffer();
		if (array.length > 0) {
			for (int i = 0; i < array.length; i++) {
				voucherNos.append("'");
				voucherNos.append(array[i]).append("',");
			}
		}
		return voucherNos.substring(0, voucherNos.length() - 1);
	}

}
