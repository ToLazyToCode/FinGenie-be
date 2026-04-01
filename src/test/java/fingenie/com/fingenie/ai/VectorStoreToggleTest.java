package fingenie.com.fingenie.ai;

import fingenie.com.fingenie.ai.repository.AIEmbeddingRepository;
import fingenie.com.fingenie.ai.store.InMemoryVectorStoreService;
import fingenie.com.fingenie.ai.store.JpaVectorStoreService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class VectorStoreToggleTest {

    @Test
    void defaultIsInMemory() {
        new ApplicationContextRunner()
                .withUserConfiguration(InMemoryVectorStoreService.class, JpaVectorStoreService.class)
                .run(ctx -> {
                    assertThat(ctx.getBeanNamesForType(InMemoryVectorStoreService.class).length).isGreaterThan(0);
                    // JPA should not be present by default
                    assertThat(ctx.getBeanNamesForType(JpaVectorStoreService.class).length).isEqualTo(0);
                });
    }

    @Test
    void jpaSelectedWhenPropertySet() {
        new ApplicationContextRunner()
                .withUserConfiguration(InMemoryVectorStoreService.class, JpaVectorStoreService.class)
                .withBean(AIEmbeddingRepository.class, () -> Mockito.mock(AIEmbeddingRepository.class))
                .withPropertyValues("ai.vector-store=JPA")
                .run(ctx -> {
                    assertThat(ctx.getBeanNamesForType(JpaVectorStoreService.class).length).isGreaterThan(0);
                    // InMemory should not be present when JPA selected
                    assertThat(ctx.getBeanNamesForType(InMemoryVectorStoreService.class).length).isEqualTo(0);
                });
    }
}
