package com.thebk.utils.metrics;

import com.thebk.utils.TestBase;
import com.thebk.utils.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Metrics_FileWriter_Test extends TestBase {
	@Test
	public void test() throws InterruptedException {
		File path = new File(MetricsEngine.METRICS_PATH);
		if (path.exists()) {
			FileUtils.deleteDir(path);
		}
		MetricsEngine.startTimedSnapshotFileWriter(100);
		Thread.sleep(350);
		CountDownLatch latch = new CountDownLatch(1);
		MetricsEngine.stopTimedSnapshot().addListener((f) -> {
			latch.countDown();
		}).release();
		Assertions.assertTrue(latch.await(500, TimeUnit.MILLISECONDS));

	}

}
