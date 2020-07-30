package com.thebk.app;

public class ApplicationBootstrap {
	public static void bootstrap(String[] args) {
		ApplicationRun.bootstrapLog("ApplicationBootstrap Java " + System.getProperty("java.version"));
		ApplicationRun.loadLoggingImplementation();
		if (ApplicationRun.registeredLoggingImpl() != null) {
			ApplicationRun.registeredLoggingImpl().bootstrap(args);
		}
		processCommandLine(args);
		ApplicationRun.bootstrapLog("bootstrap done");
		if (ApplicationRun.isTerminating()) {
			ApplicationRun.bootstrapLog("ERROR: Attempt to re-initialize a terminated application");
			ApplicationRun.fatalExit();
			// Won't get here unless certain options are set to prevent JVM halt
			throw new RuntimeException("Attempt to re-initialize a terminated application");
		}
	}

	private static void processCommandLine(String[] args) {
		System.setProperty("javax.net.ssl.trustStore", "app-ca-certs");
		if (args != null && args.length != 0 && args[0].equals("--install-cert")) {
			if (args.length != 3) {
				System.out.println("Usage: --install-cert <host> <store password>");
				System.exit(1);
			}
			try {
				//InstallCert.install(args[1], args[2]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.exit(1);
		}
	}
}
