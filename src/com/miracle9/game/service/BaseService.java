package com.miracle9.game.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.session.IoSession;

public class BaseService {

	protected Map<String, Object> createResult(boolean success, String message) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", success);
		result.put("message", message);
		return result;
	}

	protected int getLanguage(IoSession session) {
		return (Integer) session.getAttribute("language");
	}

}
