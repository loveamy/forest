package com.miracle9.game.socket;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

public class Data {
	public IoSession session;
	public IoBuffer buffer;
	public Object[] result;
	public String method;
	
	public Data() {

	}

	public Data(IoSession session, IoBuffer buffer) {
		this.session = session;
		this.buffer = buffer;
	}

	public Data(IoSession session, String method, Object[] result) {
		this.session = session;
		this.method = method;
		this.result = result;
	}

}
