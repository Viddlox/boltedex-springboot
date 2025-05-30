package com.example.boltedex;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
	"spring.data.redis.host=localhost",
	"spring.data.redis.port=6379",
	"spring.data.redis.password=",
	"spring.data.redis.username=",
	"cache.preload.pokemon-details=false",
	"cache.preload.on-startup=false"
})
class BoltedexApplicationTests {

	@Test
	void contextLoads() {
	}

}
