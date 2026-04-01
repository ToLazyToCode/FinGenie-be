//package fingenie.com.fingenie.seed;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.Ordered;
//import org.springframework.core.annotation.Order;
//import org.springframework.core.env.Environment;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StringUtils;
//
//import java.net.URI;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Locale;
//import java.util.Optional;
//import java.util.Set;
//
//@Component
//@Order(Ordered.LOWEST_PRECEDENCE)
//@RequiredArgsConstructor
//@Slf4j
//public class MockDataSeederRunner implements CommandLineRunner {
//
//    private static final Set<String> ALLOWED_PROFILES = Set.of("local", "dev", "test");
//    private static final Set<String> ALLOWED_HOSTS = Set.of("localhost", "127.0.0.1");
//
//    private final Environment environment;
//    private final MockDataSeederService mockDataSeederService;
//
//    @Override
//    public void run(String... args) {
//        List<String> skipReasons = validateEnvironment();
//        if (!skipReasons.isEmpty()) {
//            log.info(
//                    "Skipping FinGenie mock-data seeding: {}",
//                    String.join("; ", skipReasons)
//            );
//            return;
//        }
//
//        MockDataSeederService.SeedSummary summary = mockDataSeederService.seedManagedMockData();
//        log.info(
//                "FinGenie mock-data seeding completed: managedAccounts={} transactions={} goals={} piggies={} aiMessages={}",
//                summary.managedAccounts(),
//                summary.transactions(),
//                summary.goals(),
//                summary.piggies(),
//                summary.aiMessages()
//        );
//    }
//
//    private List<String> validateEnvironment() {
//        List<String> reasons = new ArrayList<>();
//
//        boolean enabled = environment.getProperty(
//                "fingenie.mock-data.seed.enabled",
//                Boolean.class,
//                false
//        );
//        if (!enabled) {
//            reasons.add("property fingenie.mock-data.seed.enabled is false");
//        }
//
//        Set<String> activeProfiles = resolveActiveProfiles();
//        boolean hasAllowedProfile = activeProfiles.stream().anyMatch(ALLOWED_PROFILES::contains);
//        if (!hasAllowedProfile) {
//            reasons.add("active profiles " + activeProfiles + " do not include local/dev/test");
//        }
//
//        String datasourceUrl = environment.getProperty("spring.datasource.url");
//        Optional<String> host = extractHost(datasourceUrl);
//        if (host.isEmpty()) {
//            reasons.add("spring.datasource.url host could not be resolved");
//        } else if (!ALLOWED_HOSTS.contains(host.get())) {
//            reasons.add("datasource host " + host.get() + " is not localhost or 127.0.0.1");
//        }
//
//        return reasons;
//    }
//
//    private Set<String> resolveActiveProfiles() {
//        Set<String> profiles = new LinkedHashSet<>();
//        profiles.addAll(Arrays.asList(environment.getActiveProfiles()));
//        if (profiles.isEmpty()) {
//            String property = environment.getProperty("spring.profiles.active");
//            if (StringUtils.hasText(property)) {
//                Arrays.stream(property.split(","))
//                        .map(String::trim)
//                        .filter(StringUtils::hasText)
//                        .map(value -> value.toLowerCase(Locale.ROOT))
//                        .forEach(profiles::add);
//            }
//        }
//        return profiles;
//    }
//
//    private Optional<String> extractHost(String datasourceUrl) {
//        if (!StringUtils.hasText(datasourceUrl)) {
//            return Optional.empty();
//        }
//
//        String normalized = datasourceUrl.trim();
//        if (normalized.startsWith("jdbc:")) {
//            normalized = normalized.substring(5);
//        }
//
//        try {
//            return Optional.ofNullable(URI.create(normalized).getHost())
//                    .map(host -> host.toLowerCase(Locale.ROOT));
//        } catch (Exception ignored) {
//            int start = normalized.indexOf("//");
//            if (start < 0) {
//                return Optional.empty();
//            }
//            int hostStart = start + 2;
//            int hostEnd = normalized.indexOf('/', hostStart);
//            String hostPort = hostEnd < 0 ? normalized.substring(hostStart) : normalized.substring(hostStart, hostEnd);
//            int colon = hostPort.indexOf(':');
//            String host = colon < 0 ? hostPort : hostPort.substring(0, colon);
//            if (!StringUtils.hasText(host)) {
//                return Optional.empty();
//            }
//            return Optional.of(host.toLowerCase(Locale.ROOT));
//        }
//    }
//}
