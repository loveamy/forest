package com.miracle9.game.thread;

import java.util.Calendar;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.miracle9.game.service.DeskService;
import com.miracle9.game.service.OnlineCountService;
import com.miracle9.game.service.UserService;

@Service("onlineCountThread")
public class OnlineCountThread extends Thread {
	private Logger logger = Logger.getLogger(OnlineCountThread.class);



	@Autowired
	private OnlineCountService onlineCountService;

	@Autowired
	private DeskService deskService;



	@Autowired
	private UserService userService;


	@PostConstruct
	public void startThis() {
		//start();
	}

	public void run() {
		while (true) {
			countOnline();
			try {
				sleep(50000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 统计在线人数
	 */
	public void countOnline() {
		try {
			Calendar calendar = Calendar.getInstance();
			int minute = calendar.get(Calendar.MINUTE);
			if (minute == 0 || minute == 30) {// 半小时统计一次
				procAll();
				Thread.sleep(25 * 60000);
			}
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	/**
	 * 处理所有数据
	 */
	private void procAll() {
		/*int hallCount = LocalMem.onlineUsers.size();
		int luckCount = LocalMem.luckUser.size();
		OnlineCount oc = new OnlineCount();
		oc.setDatetime(MyUtil.dateToString1(new Date()));
		oc.setOnlineCount(hallCount + luckCount);
		String detailStr = "大厅:" + hallCount;
		int expeRoomPeople = 0;// 练习房
		int athleticsRoomPeople = 0;// 竞技房
		for (Entry<Integer, List<User>> entry : LocalMem.luckDeskInfo.entrySet()) {
			Desk desk = deskService.getDesk(entry.getKey());
			if (desk == null)
				continue;
			if (desk.getRoomId() == 1) {
				expeRoomPeople += entry.getValue().size();
			} else {
				athleticsRoomPeople += entry.getValue().size();
			}
		}
		detailStr += ";幸运六狮:" + luckCount + ",练习厅:" + expeRoomPeople + ",竞技厅:" + athleticsRoomPeople + ";";

		expeRoomPeople = 0;
		athleticsRoomPeople = 0;
		

		expeRoomPeople = 0;
		athleticsRoomPeople = 0;
		

		expeRoomPeople = 0;
		athleticsRoomPeople = 0;
		
		oc.setDetail(detailStr);
		oc.setType(2);
		onlineCountService.addOnlineCount(oc);*/
	}

	

	
}
