package com.star.share;

import org.springframework.ai.vectorstore.elasticsearch.autoconfigure.ElasticsearchVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
		ElasticsearchVectorStoreAutoConfiguration.class
})
public class ShareApplication {
	public static void main(String[] args) {
		SpringApplication.run(ShareApplication.class, args);
	}

}
