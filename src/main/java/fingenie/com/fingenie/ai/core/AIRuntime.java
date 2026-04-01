package fingenie.com.fingenie.ai.core;

public interface AIRuntime {

    AIResponse generate(AIRequest request);

    double[] embed(String text);
}
