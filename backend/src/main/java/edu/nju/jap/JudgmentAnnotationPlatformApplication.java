package edu.nju.jap;

import edu.nju.jap.config.JwtProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("edu.nju.jap.mapper")
@EnableConfigurationProperties(JwtProperties.class)
@EnableScheduling
public class JudgmentAnnotationPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(JudgmentAnnotationPlatformApplication.class, args);
    }
}
