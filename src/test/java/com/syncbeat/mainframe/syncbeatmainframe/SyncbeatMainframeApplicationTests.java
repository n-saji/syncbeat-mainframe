package com.syncbeat.mainframe.syncbeatmainframe;

import com.syncbeat.mainframe.syncbeatmainframe.repository.RoomRepository;
import com.syncbeat.mainframe.syncbeatmainframe.repository.UserRepository;
import org.mockito.Mockito;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootTest
class SyncbeatMainframeApplicationTests {

	@Test
	void contextLoads() {
	}

	@TestConfiguration
	static class MockRepositoriesConfig {
		@Bean
		UserRepository userRepository() {
			return Mockito.mock(UserRepository.class);
		}

		@Bean
		RoomRepository roomRepository() {
			return Mockito.mock(RoomRepository.class);
		}
	}

}
