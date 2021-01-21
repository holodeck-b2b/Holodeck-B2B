package org.holodeckb2b.core.workerpool;

import java.util.HashMap;
import java.util.Map;

public class TaskReporter {
	Map<String, Integer>	workerRuns = new HashMap<>();
	Map<String, Map<String, ?>> workerParams = new HashMap<>();
	
	public void reportRun(String wName) {
		Integer runs = workerRuns.getOrDefault(wName, 0);
		workerRuns.put(wName, runs + 1);
	}

	public void reportParams(String wName, Map<String, ?> params) {
		workerParams.put(wName, params);
	}
}