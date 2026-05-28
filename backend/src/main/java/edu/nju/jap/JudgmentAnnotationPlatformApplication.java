package edu.nju.jap;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("edu.nju.jap.mapper")
public class JudgmentAnnotationPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(JudgmentAnnotationPlatformApplication.class, args);
    }
}
