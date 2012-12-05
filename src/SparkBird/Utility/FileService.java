/*
 * Copyright (c) 2011 安致创想 工作室. 
 * All rights reserved. http://www.dalian1008.com/
 * 
 * The soft is Non-commercial use only
 * Cannot modify source-code for any purpose (cannot create derivative works)
 */
package SparkBird.Utility;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FileService {

	/**
	 * 保存数据
	 * 
	 * @param outputStream
	 * @param content
	 * @throws Exception
	 */
	public static void save(OutputStream outputStream, String content)
			throws Exception {
		outputStream.write(content.getBytes());
		outputStream.close();
	}

	/**
	 * 读取数据
	 * 
	 * @param inputStream
	 * @return
	 * @throws Exception
	 */
	public static String read(InputStream inputStream) throws Exception {
		// 往内存写数据
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		// 缓冲区
		byte[] buffer = new byte[1024];
		int len = -1;
		while ((len = inputStream.read(buffer)) != -1) {
			byteArrayOutputStream.write(buffer, 0, len);
		}
		// 存储数据
		byte[] data = byteArrayOutputStream.toByteArray();
		byteArrayOutputStream.close();
		inputStream.close();
		return new String(data);
	}
}
