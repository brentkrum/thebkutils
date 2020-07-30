package com.thebk.app.socket;

import com.thebk.app.test.AbstractTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class TestBase extends AbstractTestBase {
	@BeforeAll
	public static void baseInit() {
		System.setProperty("com.thebk.app.logger-level-root", "DEBUG");
		bootstrap();
	}

	@AfterAll
	public static void baseDeinit() {
		deinit();
	}
}
