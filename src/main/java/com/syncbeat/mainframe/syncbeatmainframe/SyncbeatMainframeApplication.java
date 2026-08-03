package com.syncbeat.mainframe.syncbeatmainframe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SyncbeatMainframeApplication {

	public static void main(String[] args) {
		SpringApplication.run(SyncbeatMainframeApplication.class, args);
	}

}
