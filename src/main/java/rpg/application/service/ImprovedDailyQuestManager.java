/**
 * 개선된 일일 퀘스트 생성 시스템
 * - 플레이어 레벨에 따른 계층적 퀘스트 생성
 * - 명확한 ID 체계로 저장/로드 최적화
 */
package rpg.application.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.domain.player.Player;
import rpg.domain.quest.Quest;
import rpg.domain.quest.Quest.QuestType;
import rpg.domain.quest.QuestReward;
import rpg.domain.quest.QuestTemplateData;
import rpg.infrastructure.data.loader.QuestTemplateLoader;

public class ImprovedDailyQuestManager {
  private static final Logger logger = LoggerFactory.getLogger(ImprovedDailyQuestManager.class);

  // === 퀘스트 티어 정의 ===
  public enum QuestTier {
    TIER_A(1, 10, "초급", "A"), // 초보자용
    TIER_B(11, 20, "중급", "B"), // 중급자용
    TIER_C(21, 30, "고급", "C"), // 고급자용
    TIER_D(31, 40, "최상급", "D"), // 최상급자용
    TIER_S(41, 50, "전설급", "S"); // 전설급

    private final int minLevel;
    private final int maxLevel;
    private final String description;
    private final String code;

    QuestTier(int minLevel, int maxLevel, String description, String code) {
      this.minLevel = minLevel;
      this.maxLevel = maxLevel;
      this.description = description;
      this.code = code;
    }

    public static QuestTier getTierForLevel(int level) {
      for (QuestTier tier : values()) {
        if (level >= tier.minLevel && level <= tier.maxLevel) {
          return tier;
        }
      }
      return TIER_A; // 기본값
    }

    // Getters...
    public int getMinLevel() {
      return minLevel;
    }

    public int getMaxLevel() {
      return maxLevel;
    }

    public String getDescription() {
      return description;
    }

    public String getCode() {
      return code;
    }
  }

  // === 일일 퀘스트 템플릿 정의 ===
  public static class DailyQuestTemplate {
    private final String baseId;
    private final String title;
    private final String description;
    private final Quest.QuestType type;
    private final QuestTier tier;
    private final Map<String, Object> parameters;

    public DailyQuestTemplate(String baseId, String title, String description, Quest.QuestType type, QuestTier tier, Map<String, Object> parameters) {
      this.baseId = baseId;
      this.title = title;
      this.description = description;
      this.type = type;
      this.tier = tier;
      this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }

    // Getters...
    public String getBaseId() {
      return baseId;
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    public Quest.QuestType getType() {
      return type;
    }

    public QuestTier getTier() {
      return tier;
    }

    public Map<String, Object> getParameters() {
      return parameters;
    }
  }

  // === 퀘스트 생성 메서드들 ===

  /**
   * 플레이어 레벨에 맞는 일일 퀘스트들 생성
   */
  public List<Quest> generateDailyQuestsForPlayer(Player player) {
    List<Quest> dailyQuests = new ArrayList<>();
    QuestTier playerTier = QuestTier.getTierForLevel(player.getLevel());
    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    // 1. 사냥 퀘스트 생성 (티어별 2개씩)
    dailyQuests.addAll(generateKillQuests(today, playerTier, player.getLevel()));

    // 2. 수집 퀘스트 생성 (티어별 1개씩)
    dailyQuests.addAll(generateCollectQuests(today, playerTier, player.getLevel()));

    // 3. 특별 퀘스트 (고레벨만)
    if (playerTier.getMinLevel() >= 20) {
      dailyQuests.addAll(generateSpecialQuests(today, playerTier, player.getLevel()));
    }

    return dailyQuests;
  }

  /**
   * 사냥 퀘스트 생성
   */
  private List<Quest> generateKillQuests(String date, QuestTier tier, int playerLevel) {
    List<Quest> killQuests = new ArrayList<>();

    //@formatter:off
    Map<QuestTier, List<MonsterTarget>> tierMonsters = Map.of(
        QuestTier.TIER_A, Arrays.asList(
            new MonsterTarget("FOREST_SLIME", 5, 50, 30), 
            new MonsterTarget("FOREST_GOBLIN", 3, 80, 50)
        ),
        QuestTier.TIER_B, Arrays.asList(
            new MonsterTarget("WILD_BOAR", 4, 120, 80), 
            new MonsterTarget("CAVE_TROLL", 3, 150, 100)
        ),
        QuestTier.TIER_C, Arrays.asList(
            new MonsterTarget("FOREST_WOLF", 4, 200, 150), 
            new MonsterTarget("SKELETON_WARRIOR", 3, 250, 180)
        ),
        QuestTier.TIER_D, Arrays.asList(
            new MonsterTarget("FIRE_DRAGON", 2, 500, 350), 
            new MonsterTarget("ICE_GIANT", 3, 400, 300)
        ),
        QuestTier.TIER_S, Arrays.asList(
            new MonsterTarget("MAGMA_DRAGON", 1, 1000, 800), 
            new MonsterTarget("VOID_REAPER", 1, 1200, 1000)
        )
    );
    //@formatter:on
    
    List<MonsterTarget> monsters = tierMonsters.get(tier);
    if (monsters != null) {
      for (int i = 0; i < Math.min(2, monsters.size()); i++) {
        MonsterTarget monster = monsters.get(i);
        String questId = String.format("daily_kill_%s_%s%02d", date, tier.getCode(), i + 1);

        Map<String, Integer> objectives = new HashMap<>();
        objectives.put("kill_" + monster.name, monster.count);

        QuestReward reward = new QuestReward(monster.exp + (playerLevel * 10), // 레벨별 경험치 보정
            monster.gold + (playerLevel * 5) // 레벨별 골드 보정
        );

        Quest quest = new Quest(questId, String.format("[%s] %s 사냥", tier.getDescription(), monster.name),
            String.format("%s을(를) %d마리 처치하세요.", monster.name, monster.count), Quest.QuestType.KILL, tier.getMinLevel(), objectives, reward);

        killQuests.add(quest);
      }
    }

    return killQuests;
  }

  /**
   * 수집 퀘스트 생성
   */
  private List<Quest> generateCollectQuests(String date, QuestTier tier, int playerLevel) {
    List<Quest> collectQuests = new ArrayList<>();

    // 티어별 수집 아이템 정의
    Map<QuestTier, List<CollectTarget>> tierItems = Map.of(QuestTier.TIER_A, Arrays.asList(new CollectTarget("체력 물약", 3, 60, 40)), QuestTier.TIER_B,
        Arrays.asList(new CollectTarget("마나 물약", 5, 100, 70)), QuestTier.TIER_C, Arrays.asList(new CollectTarget("희귀 광석", 3, 200, 150)),
        QuestTier.TIER_D, Arrays.asList(new CollectTarget("전설 재료", 2, 400, 300)), QuestTier.TIER_S,
        Arrays.asList(new CollectTarget("신화 파편", 1, 800, 600)));

    List<CollectTarget> items = tierItems.get(tier);
    if (items != null && !items.isEmpty()) {
      CollectTarget item = items.get(0);
      String questId = String.format("daily_collect_%s_%s01", date, tier.getCode());

      Map<String, Integer> objectives = new HashMap<>();
      objectives.put("collect_" + item.name, item.count);

      QuestReward reward = new QuestReward(item.exp + (playerLevel * 8), item.gold + (playerLevel * 4));

      Quest quest = new Quest(questId, String.format("[%s] %s 수집", tier.getDescription(), item.name),
          String.format("%s을(를) %d개 수집하세요.", item.name, item.count), Quest.QuestType.COLLECT, tier.getMinLevel(), objectives, reward);

      collectQuests.add(quest);
    }

    return collectQuests;
  }

  /**
   * 특별 퀘스트 생성 (고레벨 전용)
   */
  private List<Quest> generateSpecialQuests(String date, QuestTier tier, int playerLevel) {
    List<Quest> specialQuests = new ArrayList<>();

    if (tier.getMinLevel() >= 20) {
      String questId = String.format("daily_special_%s_%s01", date, tier.getCode());

      Map<String, Integer> objectives = new HashMap<>();
      objectives.put("complete_dungeon", 1);

      QuestReward reward = new QuestReward(500 + (playerLevel * 20), 300 + (playerLevel * 15));

      Quest quest = new Quest(questId, String.format("[%s] 던전 클리어", tier.getDescription()), "던전을 1회 클리어하세요.", QuestType.EXPLORE, tier.getMinLevel(),
          objectives, reward);

      specialQuests.add(quest);
    }

    return specialQuests;
  }

  /**
   * 퀘스트 ID 파싱 유틸리티
   */
  public static class QuestIdParser {
    public static boolean isDailyQuest(String questId) {
      return questId != null && questId.startsWith("daily_");
    }

    public static String extractDate(String questId) {
      if (!isDailyQuest(questId))
        return null;
      String[] parts = questId.split("_");
      return parts.length >= 3 ? parts[2] : null;
    }

    public static QuestTier extractTier(String questId) {
      if (!isDailyQuest(questId))
        return null;
      String[] parts = questId.split("_");
      if (parts.length >= 4) {
        String tierCode = parts[3].substring(0, 1);
        for (QuestTier tier : QuestTier.values()) {
          if (tier.getCode().equals(tierCode)) {
            return tier;
          }
        }
      }
      return null;
    }

    public static int extractQuestNumber(String questId) {
      if (!isDailyQuest(questId))
        return -1;
      String[] parts = questId.split("_");
      if (parts.length >= 4) {
        String tierAndNumber = parts[3];
        try {
          return Integer.parseInt(tierAndNumber.substring(1));
        } catch (NumberFormatException e) {
          return -1;
        }
      }
      return -1;
    }
  }

  // === 헬퍼 클래스들 ===

  private static class MonsterTarget {
    final String name;
    final int count;
    final int exp;
    final int gold;

    MonsterTarget(String name, int count, int exp, int gold) {
      this.name = name;
      this.count = count;
      this.exp = exp;
      this.gold = gold;
    }
  }

  private static class CollectTarget {
    final String name;
    final int count;
    final int exp;
    final int gold;

    CollectTarget(String name, int count, int exp, int gold) {
      this.name = name;
      this.count = count;
      this.exp = exp;
      this.gold = gold;
    }
  }

  /**
   * 일일 퀘스트 만료 검증 및 정리
   */
  public void cleanupExpiredDailyQuests(QuestManager questManager) {
    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    // 활성 퀘스트에서 만료된 일일 퀘스트 제거
    questManager.getActiveQuests().removeIf(quest -> {
      if (QuestIdParser.isDailyQuest(quest.getId())) {
        String questDate = QuestIdParser.extractDate(quest.getId());
        return !today.equals(questDate);
      }
      return false;
    });

    // 사용 가능한 퀘스트에서도 만료된 일일 퀘스트 제거
    questManager.getAvailableQuests().removeIf(quest -> {
      if (QuestIdParser.isDailyQuest(quest.getId())) {
        String questDate = QuestIdParser.extractDate(quest.getId());
        return !today.equals(questDate);
      }
      return false;
    });

    System.out.println("✅ 만료된 일일 퀘스트 정리 완료");
  }

  /**
   * 퀘스트가 만료되었는지 확인
   */
  public boolean isQuestExpired(Quest quest) {
    if (quest == null || !quest.getId().startsWith("daily_")) {
      return false; // 일일 퀘스트가 아니면 만료되지 않음
    }

    try {
      String questDate = QuestIdParser.extractDate(quest.getId());
      if (questDate == null) {
        return false;
      }

      String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
      return !today.equals(questDate);

    } catch (Exception e) {
      logger.warn("퀘스트 만료 확인 중 오류: {}", quest.getId(), e);
      return false;
    }
  }

  /**
   * 퀘스트 ID로 만료 확인 (오버로드)
   */
  public boolean isQuestExpired(String questId) {
    if (questId == null || !questId.startsWith("daily_")) {
      return false;
    }

    try {
      String questDate = QuestIdParser.extractDate(questId);
      if (questDate == null) {
        return false;
      }

      String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
      return !today.equals(questDate);

    } catch (Exception e) {
      logger.warn("퀘스트 ID 만료 확인 중 오류: {}", questId, e);
      return false;
    }
  }

  /**
   * 누락된 퀘스트 관련 메서드들
   * - ImprovedDailyQuestManager와 QuestManager에 추가할 메서드들
   */

  /**
   * 일일 퀘스트 생성 통계 출력
   */
  public void printGenerationStats() {
    System.out.println("\n=== 📊 일일 퀘스트 생성 통계 ===");

    try {
      // 일일 퀘스트 템플릿 정보
      Map<String, QuestTemplateData> dailyTemplates = QuestTemplateLoader.loadDailyQuests();
      System.out.printf("로드된 일일 퀘스트 템플릿: %d개\n", dailyTemplates.size());

      System.out.println("\n📋 템플릿별 상세 정보:");
      for (QuestTemplateData template : dailyTemplates.values()) {
        System.out.printf("\n🎯 %s:\n", template.getId());
        System.out.printf("   제목: %s\n", template.getTitle());
        System.out.printf("   타입: %s\n", template.getType());
        System.out.printf("   최소 레벨: %d\n", template.getRequiredLevel());

        if (template.getVariableTargets() != null && !template.getVariableTargets().isEmpty()) {
          System.out.printf("   가변 타겟: %s\n", template.getVariableTargets());
        }

        if (template.getVariableQuantity() != null) {
          System.out.printf("   수량 범위: %d - %d\n", template.getVariableQuantity().getMin(), template.getVariableQuantity().getMax());
        }

        System.out.printf("   기본 보상: 경험치 %d, 골드 %d\n", template.getReward().getExperience(), template.getReward().getGold());
      }

      // 티어별 정보
      System.out.println("\n🏆 티어별 정보:");
      for (QuestTier tier : QuestTier.values()) {
        System.out.printf("   %s (%s): 레벨 %d-%d\n", tier.getCode(), tier.getDescription(), tier.getMinLevel(), tier.getMaxLevel());
      }

      // 현재 날짜 정보
      String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      String todayId = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
      System.out.printf("\n📅 오늘 날짜: %s (ID: %s)\n", today, todayId);

    } catch (Exception e) {
      System.out.println("❌ 통계 출력 중 오류: " + e.getMessage());
      logger.error("일일 퀘스트 통계 출력 실패", e);
    }

    System.out.println("=".repeat(50));
  }

  /**
   * 특정 플레이어를 위한 일일 퀘스트 생성 시뮬레이션
   */
  public void simulateGenerationForPlayer(Player player) {
    System.out.printf("\n🎯 %s (레벨 %d)를 위한 일일 퀘스트 생성 시뮬레이션:\n", player.getName(), player.getLevel());

    try {
      // 플레이어 티어 결정
      QuestTier playerTier = QuestTier.getTierForLevel(player.getLevel());
      System.out.printf("플레이어 티어: %s (%s)\n", playerTier.getCode(), playerTier.getDescription());

      // 일일 퀘스트 생성
      List<Quest> generatedQuests = generateDailyQuestsForPlayer(player);

      if (generatedQuests.isEmpty()) {
        System.out.println("❌ 생성된 일일 퀘스트가 없습니다.");
        System.out.println("💡 가능한 원인:");
        System.out.println("   - 플레이어 레벨이 너무 낮음");
        System.out.println("   - 해당 레벨에 적용 가능한 템플릿이 없음");
        System.out.println("   - 템플릿 로드 실패");
        return;
      }

      System.out.printf("\n✅ 총 %d개의 일일 퀘스트가 생성되었습니다:\n", generatedQuests.size());

      for (int i = 0; i < generatedQuests.size(); i++) {
        Quest quest = generatedQuests.get(i);
        System.out.printf("\n%d. %s\n", i + 1, quest.getTitle());
        System.out.printf("   📋 ID: %s\n", quest.getId());
        System.out.printf("   📝 설명: %s\n", quest.getDescription());
        System.out.printf("   🎯 목표: %s\n", formatObjectives(quest.getObjectives()));
        System.out.printf("   🎁 보상: 경험치 %d, 골드 %d\n", quest.getReward().getExpReward(), quest.getReward().getGoldReward());

        // 아이템 보상이 있는 경우
        if (quest.getReward().getItemRewards() != null && !quest.getReward().getItemRewards().isEmpty()) {
          System.out.print("   🎒 아이템 보상: ");
          quest.getReward().getItemRewards().forEach((item, quantity) -> System.out.printf("%s x%d ", item.getName(), quantity));
          System.out.println();
        }

        // 퀘스트 티어 정보
        QuestTier questTier = QuestIdParser.extractTier(quest.getId());
        if (questTier != null) {
          System.out.printf("   🏆 티어: %s (%s)\n", questTier.getCode(), questTier.getDescription());
        }
      }

      // 생성 통계
      System.out.println("\n📊 생성 통계:");
      Map<Quest.QuestType, Long> typeCount = generatedQuests.stream().collect(Collectors.groupingBy(Quest::getType, Collectors.counting()));

      for (Map.Entry<Quest.QuestType, Long> entry : typeCount.entrySet()) {
        System.out.printf("   %s: %d개\n", entry.getKey(), entry.getValue());
      }

      // 평균 보상 계산
      double avgExp = generatedQuests.stream().mapToInt(quest -> quest.getReward().getExpReward()).average().orElse(0);
      double avgGold = generatedQuests.stream().mapToInt(quest -> quest.getReward().getGoldReward()).average().orElse(0);

      System.out.printf("\n💰 평균 보상: 경험치 %.1f, 골드 %.1f\n", avgExp, avgGold);

    } catch (Exception e) {
      System.out.println("❌ 시뮬레이션 중 오류: " + e.getMessage());
      logger.error("일일 퀘스트 시뮬레이션 실패", e);
    }
  }

  /**
   * 목표를 읽기 쉬운 형태로 포맷
   */
  private String formatObjectives(Map<String, Integer> objectives) {
    if (objectives == null || objectives.isEmpty()) {
      return "없음";
    }

    return objectives.entrySet().stream().map(entry -> {
      String key = entry.getKey();
      Integer value = entry.getValue();

      // 키를 더 읽기 쉽게 변환
      if (key.startsWith("kill_")) {
        String monster = key.substring(5);
        return String.format("%s %d마리 처치", monster, value);
      } else if (key.startsWith("collect_")) {
        String item = key.substring(8);
        return String.format("%s %d개 수집", item, value);
      } else if (key.startsWith("explore_")) {
        String location = key.substring(8);
        return String.format("%s %d회 탐험", location, value);
      } else {
        return String.format("%s: %d", key, value);
      }
    }).collect(Collectors.joining(", "));
  }

}
