package com.thebk.utils;

import com.denaliai.fw.utility.test.AbstractTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class TestBase extends AbstractTestBase {
	@BeforeAll
	public static void baseInit() {
		System.setProperty("io.netty.leakDetectionLevel", "PARANOID");
		System.setProperty("io.netty.leakDetection.samplingInterval", "1");
		bootstrap();
	}

	@AfterAll
	public static void baseDeinit() {
		deinit();
	}
}

