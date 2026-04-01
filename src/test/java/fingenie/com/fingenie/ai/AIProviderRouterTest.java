package fingenie.com.fingenie.ai;

import fingenie.com.fingenie.ai.provider.AIProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class AIProviderRouterTest {

    @Test
    void routerSelectsProviders() throws Exception {
        AIProvider gemini = Mockito.mock(AIProvider.class);
        AIProvider ollama = Mockito.mock(AIProvider.class);
        AIProvider stub = Mockito.mock(AIProvider.class);

        // Priority #1: Gemini when healthy
        Mockito.when(gemini.healthCheck()).thenReturn(true);
        Mockito.when(ollama.healthCheck()).thenReturn(true);
        Map<String, AIProvider> providers = Map.of(
                "geminiProvider", gemini,
                "ollamaProvider", ollama,
                "stubProvider", stub
        );

        Class<?> cls = Class.forName("fingenie.com.fingenie.ai.router.AIProviderRouter");
        Constructor<?> ctor = cls.getDeclaredConstructor(Map.class);
        Object router = ctor.newInstance(providers);

        Method select = cls.getMethod("selectProvider", String.class, String.class);
        Object p1 = select.invoke(router, "AUTO", "hello");
        assertSame(gemini, p1);

        // Priority #2: Ollama when Gemini unhealthy
        Mockito.when(gemini.healthCheck()).thenReturn(false);
        Object p2 = select.invoke(router, "AUTO", "hello");
        assertSame(ollama, p2);

        // Priority #3: Stub when both primary providers unhealthy
        Mockito.when(ollama.healthCheck()).thenReturn(false);
        Object p3 = select.invoke(router, "AUTO", "hello");
        assertSame(stub, p3);

        assertNotNull(p1);
        assertNotNull(p2);
        assertNotNull(p3);
    }
}
