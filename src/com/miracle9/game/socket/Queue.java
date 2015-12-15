package com.miracle9.game.socket;

import java.util.LinkedList;

/**
 * 数据队列
 */
public class Queue {

	private LinkedList<Data> queue = new LinkedList<Data>();

	public synchronized void put(Data data) {
		queue.addFirst(data);
		notifyAll();
	}

	public synchronized Data get() {
		try {
			while (queue.isEmpty())
				wait();
		} catch (Exception e) {
			return null;
		}
		return queue.pollLast();
	}
}
