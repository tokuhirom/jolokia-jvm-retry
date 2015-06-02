package me.geso.jolokia_jvm_agent_retry;

import org.jolokia.jvmagent.*;
import java.io.IOException;

// see https://github.com/rhuss/jolokia/blob/master/agent/jvm/src/main/java/org/jolokia/jvmagent/JvmAgent.java
//
// This module retries every 1 sec when got BindException.
public class JvmAgent {
	public static void premain(String agentArgs) {
		long sleep = Long.valueOf(System.getProperty("jolokia-retry.sleep", "1000"));
		Thread starterThread = new Thread(() -> {
			try {
				start(agentArgs, sleep);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
		starterThread.setDaemon(true);
		starterThread.start();
	}

	public static void start(String agentArgs, long sleep) throws InterruptedException {
		JvmAgentConfig pConfig = new JvmAgentConfig(agentArgs);
		while (true) {
			try {
				JolokiaServer server = new JolokiaServer(pConfig,true);

				server.start();
				System.setProperty("jolokia.agent", server.getUrl());

				System.out.println("Jolokia: Agent started with URL " + server.getUrl());
			} catch (RuntimeException exp) {
				System.err.println("Could not start Jolokia agent: " + exp);
			} catch (java.net.BindException exp) {
				System.err.println("Could not start Jolokia agent: " + exp + "... retrying");
				Thread.sleep(sleep);
				continue;
			} catch (IOException exp) {
				System.err.println("Could not start Jolokia agent: " + exp);
			}
			break;
		}
	}
}
