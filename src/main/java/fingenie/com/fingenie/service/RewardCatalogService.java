package fingenie.com.fingenie.service;

import fingenie.com.fingenie.common.error.exceptions.SystemExceptions;
import fingenie.com.fingenie.entity.PiggyBank;
import fingenie.com.fingenie.entity.RewardCatalogItem;
import fingenie.com.fingenie.repository.RewardCatalogItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RewardCatalogService {

    private final RewardCatalogItemRepository rewardCatalogItemRepository;

    @Transactional
    public void ensureSeedData() {
        for (SeedReward seed : SeedReward.values()) {
            if (rewardCatalogItemRepository.findByCode(seed.code).isPresent()) {
                continue;
            }
            RewardCatalogItem item = RewardCatalogItem.builder()
                    .code(seed.code)
                    .title(seed.title)
                    .description(seed.description)
                    .kind(seed.kind)
                    .category(seed.category)
                    .pointCost(seed.pointCost)
                    .goalThemeTags(seed.goalThemeTags)
                    .partnerName(seed.partnerName)
                    .partnerMetadata(seed.partnerMetadata)
                    .imageUrl(seed.imageUrl)
                    .termsUrl(seed.termsUrl)
                    .isActive(true)
                    .build();
            try {
                rewardCatalogItemRepository.save(item);
            } catch (DataIntegrityViolationException ignored) {
                // Another request seeded the same row concurrently.
            }
        }
    }

    @Transactional(readOnly = true)
    public List<RewardCatalogItem> listActivePersonalCatalog() {
        return rewardCatalogItemRepository.findByKindAndIsActiveTrueOrderByIdAsc(RewardCatalogItem.Kind.PERSONAL_VOUCHER);
    }

    @Transactional(readOnly = true)
    public List<RewardCatalogItem> listActiveSharedCatalog() {
        return rewardCatalogItemRepository.findByKindAndIsActiveTrueOrderByIdAsc(RewardCatalogItem.Kind.SHARED_PIGGY_GROUP_REWARD);
    }

    @Transactional(readOnly = true)
    public Optional<RewardCatalogItem> findActivePersonalById(Long rewardId) {
        return rewardCatalogItemRepository.findById(rewardId)
                .filter(RewardCatalogItem::isActive)
                .filter(item -> item.getKind() == RewardCatalogItem.Kind.PERSONAL_VOUCHER);
    }

    @Transactional(readOnly = true)
    public RewardCatalogItem selectSharedRewardForPiggy(PiggyBank piggyBank) {
        RewardCatalogItem.Category category = resolveCategoryByPiggyTheme(piggyBank);

        List<RewardCatalogItem> byCategory = rewardCatalogItemRepository
                .findByKindAndCategoryAndIsActiveTrueOrderByIdAsc(
                        RewardCatalogItem.Kind.SHARED_PIGGY_GROUP_REWARD,
                        category
                );
        if (!byCategory.isEmpty()) {
            return byCategory.get(0);
        }

        return listActiveSharedCatalog().stream()
                .findFirst()
                .orElseThrow(() -> new SystemExceptions.ValidationException(Map.of(
                        "rewardCatalog", "missingSharedReward"
                )));
    }

    private RewardCatalogItem.Category resolveCategoryByPiggyTheme(PiggyBank piggyBank) {
        String source = "";
        if (piggyBank != null && piggyBank.getWallet() != null && piggyBank.getWallet().getWalletName() != null) {
            source = piggyBank.getWallet().getWalletName();
        }
        String normalized = source.toLowerCase(Locale.ROOT);

        if (containsAny(normalized, Set.of("travel", "trip", "vacation", "du lich", "du lịch"))) {
            return RewardCatalogItem.Category.TRAVEL;
        }
        if (containsAny(normalized, Set.of("gadget", "laptop", "phone", "tech", "dien tu", "điện tử", "accessory"))) {
            return RewardCatalogItem.Category.ELECTRONICS;
        }
        if (containsAny(normalized, Set.of("emergency", "medical", "grocery", "essential", "khan cap", "khẩn cấp", "sinh hoat", "sinh hoạt"))) {
            return RewardCatalogItem.Category.ESSENTIALS;
        }
        if (containsAny(normalized, Set.of("study", "school", "course", "book", "hoc tap", "học tập", "education"))) {
            return RewardCatalogItem.Category.EDUCATION;
        }
        return RewardCatalogItem.Category.LIFESTYLE;
    }

    private boolean containsAny(String source, Set<String> keywords) {
        return keywords.stream().anyMatch(source::contains);
    }

    @RequiredArgsConstructor
    private enum SeedReward {
        PERSONAL_TRAVEL(
                "PERSONAL_TRAVEL_SAVER",
                "Travel Saver Voucher",
                "Personal discount for transport and trip essentials.",
                RewardCatalogItem.Kind.PERSONAL_VOUCHER,
                RewardCatalogItem.Category.TRAVEL,
                120L,
                "travel,trip,transport",
                "FinGenie Partner Travel",
                "Starter travel voucher",
                null,
                null
        ),
        PERSONAL_GADGET(
                "PERSONAL_GADGET_PICK",
                "Gadget Accessory Voucher",
                "Personal discount for gadgets and accessories.",
                RewardCatalogItem.Kind.PERSONAL_VOUCHER,
                RewardCatalogItem.Category.ELECTRONICS,
                180L,
                "gadget,electronics,accessory",
                "FinGenie Tech Partner",
                "Starter gadget voucher",
                null,
                null
        ),
        PERSONAL_ESSENTIAL(
                "PERSONAL_ESSENTIAL_PACK",
                "Essential Grocery Voucher",
                "Personal discount for grocery and daily essentials.",
                RewardCatalogItem.Kind.PERSONAL_VOUCHER,
                RewardCatalogItem.Category.ESSENTIALS,
                140L,
                "grocery,essential,daily",
                "FinGenie Essentials Partner",
                "Starter essentials voucher",
                null,
                null
        ),
        PERSONAL_EDU(
                "PERSONAL_STUDY_BOOST",
                "Study Booster Voucher",
                "Personal discount for books, courses, and stationery.",
                RewardCatalogItem.Kind.PERSONAL_VOUCHER,
                RewardCatalogItem.Category.EDUCATION,
                130L,
                "study,books,course",
                "FinGenie Education Partner",
                "Starter study voucher",
                null,
                null
        ),
        SHARED_TRAVEL(
                "SHARED_TRAVEL_BONUS",
                "Shared Travel Bonus",
                "Group reward unlocked for travel-themed shared piggy goals.",
                RewardCatalogItem.Kind.SHARED_PIGGY_GROUP_REWARD,
                RewardCatalogItem.Category.TRAVEL,
                null,
                "travel,trip,transport",
                "FinGenie Partner Travel",
                "Group reward placeholder",
                null,
                null
        ),
        SHARED_GADGET(
                "SHARED_GADGET_BONUS",
                "Shared Gadget Bonus",
                "Group reward unlocked for gadget-themed shared piggy goals.",
                RewardCatalogItem.Kind.SHARED_PIGGY_GROUP_REWARD,
                RewardCatalogItem.Category.ELECTRONICS,
                null,
                "gadget,electronics,accessory",
                "FinGenie Tech Partner",
                "Group reward placeholder",
                null,
                null
        ),
        SHARED_ESSENTIAL(
                "SHARED_ESSENTIAL_BONUS",
                "Shared Essentials Bonus",
                "Group reward unlocked for emergency/essential shared piggy goals.",
                RewardCatalogItem.Kind.SHARED_PIGGY_GROUP_REWARD,
                RewardCatalogItem.Category.ESSENTIALS,
                null,
                "essential,grocery,emergency",
                "FinGenie Essentials Partner",
                "Group reward placeholder",
                null,
                null
        ),
        SHARED_EDU(
                "SHARED_STUDY_BONUS",
                "Shared Study Bonus",
                "Group reward unlocked for study-themed shared piggy goals.",
                RewardCatalogItem.Kind.SHARED_PIGGY_GROUP_REWARD,
                RewardCatalogItem.Category.EDUCATION,
                null,
                "study,books,course",
                "FinGenie Education Partner",
                "Group reward placeholder",
                null,
                null
        ),
        SHARED_LIFESTYLE(
                "SHARED_LIFESTYLE_BONUS",
                "Shared Lifestyle Bonus",
                "Group reward unlocked for general shared piggy goals.",
                RewardCatalogItem.Kind.SHARED_PIGGY_GROUP_REWARD,
                RewardCatalogItem.Category.LIFESTYLE,
                null,
                "lifestyle,general",
                "FinGenie Lifestyle Partner",
                "Group reward placeholder",
                null,
                null
        );

        private final String code;
        private final String title;
        private final String description;
        private final RewardCatalogItem.Kind kind;
        private final RewardCatalogItem.Category category;
        private final Long pointCost;
        private final String goalThemeTags;
        private final String partnerName;
        private final String partnerMetadata;
        private final String imageUrl;
        private final String termsUrl;
    }
}
