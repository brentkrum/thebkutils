package com.thebk.utils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class TestBase {
	@BeforeAll
	public static void baseInit() {
		System.setProperty("io.netty.leakDetectionLevel", "PARANOID");
		System.setProperty("io.netty.leakDetection.samplingInterval", "1");
	}

	@AfterAll
	public static void baseDeinit() {
	}
}

