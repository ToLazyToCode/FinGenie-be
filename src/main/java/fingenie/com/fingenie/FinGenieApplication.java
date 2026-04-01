package fingenie.com.fingenie;

import fingenie.com.fingenie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "fingenie.com.fingenie")
@EntityScan(basePackages = "fingenie.com.fingenie")
@EnableCaching
@EnableAsync
@EnableScheduling
@Slf4j
public class FinGenieApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinGenieApplication.class, args);
    }

    @Bean
    CommandLineRunner seedData(CategoryService categoryService) {
        return args -> {
            log.info("Seeding default categories...");
            categoryService.seedDefaultCategories();
            log.info("Seeding complete.");
        };
    }
}
