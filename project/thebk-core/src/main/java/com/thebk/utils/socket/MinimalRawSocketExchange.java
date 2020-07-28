package com.thebk.utils.socket;

import com.thebk.utils.DefaultSystems;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.io.*;
import java.net.Socket;

public class MinimalRawSocketExchange {

	public static String submit(String host, int port, String outData) throws IOException {
		ByteBuf inData = null;
		OutputStream out = null;
		InputStream in = null;
		String response = null;
		try {
			Socket socket = new Socket(host, port);
			socket.setSoTimeout(500);
			out = socket.getOutputStream();
			out.write(outData.getBytes());
			in = socket.getInputStream();
			inData = DefaultSystems.allocator().buffer();
			byte[] scratch = new byte[1024];
			while (true) {
				int numRead = in.read(scratch);
				if (numRead <= 0) {
					break;
				}
				inData.writeBytes(scratch, 0, numRead);
			}
			response = new String(ByteBufUtil.getBytes(inData));
		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
			if (inData != null) {
				inData.release();
			}
		}
		return response;

	}
}
