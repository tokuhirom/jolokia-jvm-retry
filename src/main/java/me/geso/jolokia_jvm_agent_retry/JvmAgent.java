package me.geso.jolokia_jvm_agent_retry;

import org.jolokia.jvmagent.*;
import java.io.IOException;

// see https://github.com/rhuss/jolokia/blob/master/agent/jvm/src/main/java/org/jolokia/jvmagent/JvmAgent.java
//
// This module retries every 1 sec when got BindException.
public class JvmAgent {
	public static void premain(String agentArgs) {
		long interval = Long.parseLong(System.getProperty("jolokia-retry.interval", "1000"));
		long maxRetries = Long.parseLong(System.getProperty("jolokia-retry.maxRetries", "100"));
		Thread starterThread = new Thread(() -> {
			try {
				start(agentArgs, interval, maxRetries);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
		starterThread.setDaemon(true);
		starterThread.start();
	}

	public static void start(String agentArgs, long interval, long maxRetries) throws InterruptedException {
		JvmAgentConfig pConfig = new JvmAgentConfig(agentArgs);
		for (int i=0; i<maxRetries; ++i) {
			try {
				JolokiaServer server = new JolokiaServer(pConfig,true);

				server.start();
				System.setProperty("jolokia.agent", server.getUrl());

				System.out.println("Jolokia: Agent started with URL " + server.getUrl());
			} catch (RuntimeException exp) {
				System.err.println("Could not start Jolokia agent: " + exp);
			} catch (java.net.BindException exp) {
				System.err.println("Could not start Jolokia agent: " + exp + "... retrying");
				Thread.sleep(interval);
				continue;
			} catch (IOException exp) {
				System.err.println("Could not start Jolokia agent: " + exp);
			}
			break;
		}
	}
}
