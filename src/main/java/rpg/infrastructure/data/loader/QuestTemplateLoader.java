package rpg.infrastructure.data.loader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import rpg.domain.quest.QuestItemReward;
import rpg.domain.quest.QuestRewardData;
import rpg.domain.quest.QuestTemplateData;
import rpg.domain.quest.VariableQuantity;
import rpg.shared.constant.SystemConstants;

public class QuestTemplateLoader {
  private static final Logger logger = LoggerFactory.getLogger(QuestTemplateLoader.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  // 퀘스트 파일 경로들
  private static final String MAIN_QUESTS_CONFIG = "/config/quests/main-quests.json";
  private static final String SIDE_QUESTS_CONFIG = "/config/quests/side-quests.json";
  private static final String DAILY_QUESTS_CONFIG = "/config/quests/daily-quests.json";
  private static final String WEEKLY_QUESTS_CONFIG = "/config/quests/weekly-quests.json";

  /**
   * 모든 퀘스트 템플릿 로드 (통합 메서드)
   */
  public static Map<String, QuestTemplateData> loadAllQuestTemplates() {
    logger.info("전체 퀘스트 템플릿 로드 시작...");

    Map<String, QuestTemplateData> allTemplates = new HashMap<>();

    // 1. 메인 퀘스트 로드
    Map<String, QuestTemplateData> mainQuests = loadQuestTemplates(MAIN_QUESTS_CONFIG, "메인");
    allTemplates.putAll(mainQuests);

    // 2. 사이드 퀘스트 로드
    Map<String, QuestTemplateData> sideQuests = loadQuestTemplates(SIDE_QUESTS_CONFIG, "사이드");
    allTemplates.putAll(sideQuests);

    // 3. 일일 퀘스트 로드
    Map<String, QuestTemplateData> dailyQuests = loadQuestTemplates(DAILY_QUESTS_CONFIG, "일일");
    allTemplates.putAll(dailyQuests);

    // 4. 주간 퀘스트 로드
    Map<String, QuestTemplateData> weeklyQuests = loadQuestTemplates(WEEKLY_QUESTS_CONFIG, "주간");
    allTemplates.putAll(weeklyQuests);

    logger.info("전체 퀘스트 템플릿 로드 완료: {}개", allTemplates.size());
    validateQuestTemplates(allTemplates);

    return allTemplates;
  }

  /**
   * 특정 파일에서 퀘스트 템플릿 로드
   */
  private static Map<String, QuestTemplateData> loadQuestTemplates(String configPath, String category) {
    try {
      logger.info("{} 퀘스트 템플릿 로드 시작: {}", category, configPath);

      // 설정 파일 존재 여부 확인
      if (!isConfigFileExists(configPath)) {
        logger.warn("{} 퀘스트 설정 파일을 찾을 수 없습니다: {}", category, configPath);
        return createDefaultQuestTemplates(category);
      }

      // JSON 파일 로드
      InputStream inputStream = QuestTemplateLoader.class.getResourceAsStream(configPath);
      List<QuestTemplateData> questList = objectMapper.readValue(inputStream, new TypeReference<List<QuestTemplateData>>() {});

      // Map으로 변환
      Map<String, QuestTemplateData> questMap = questList.stream().collect(Collectors.toMap(QuestTemplateData::getId, template -> template));

      logger.info("{} 퀘스트 템플릿 로드 완료: {}개", category, questList.size());
      return questMap;

    } catch (Exception e) {
      logger.error("{} 퀘스트 템플릿 로드 실패", category, e);
      return createDefaultQuestTemplates(category);
    }
  }

  /**
   * 설정 파일 존재 여부 확인
   */
  private static boolean isConfigFileExists(String configPath) {
    try (InputStream inputStream = QuestTemplateLoader.class.getResourceAsStream(configPath)) {
      boolean exists = inputStream != null;
      logger.debug("설정 파일 존재 여부: {} ({})", exists, configPath);
      return exists;
    } catch (Exception e) {
      logger.debug("설정 파일 확인 중 오류: {}", configPath, e);
      return false;
    }
  }

  /**
   * 퀘스트 템플릿 검증
   */
  private static void validateQuestTemplates(Map<String, QuestTemplateData> templates) {
    if (!SystemConstants.ENABLE_JSON_VALIDATION) {
      return;
    }

    logger.info("퀘스트 템플릿 검증 시작...");

    int validCount = 0;
    int errorCount = 0;

    for (Map.Entry<String, QuestTemplateData> entry : templates.entrySet()) {
      String templateId = entry.getKey();
      QuestTemplateData template = entry.getValue();

      try {
        validateSingleTemplate(template);
        validCount++;
      } catch (Exception e) {
        errorCount++;
        if (SystemConstants.LOG_VALIDATION_ERRORS) {
          logger.warn("퀘스트 템플릿 검증 실패: {} - {}", templateId, e.getMessage());
        }
      }
    }

    logger.info("퀘스트 템플릿 검증 완료: 성공 {}개, 실패 {}개", validCount, errorCount);

    if (SystemConstants.STRICT_VALIDATION && errorCount > 0) {
      throw new IllegalStateException("엄격한 검증 모드에서 " + errorCount + "개의 퀘스트 템플릿 검증 실패");
    }
  }

  /**
   * 개별 퀘스트 템플릿 검증
   */
  private static void validateSingleTemplate(QuestTemplateData template) {
    if (template.getId() == null || template.getId().trim().isEmpty()) {
      throw new IllegalArgumentException("퀘스트 ID가 없습니다");
    }

    if (template.getTitle() == null || template.getTitle().trim().isEmpty()) {
      throw new IllegalArgumentException("퀘스트 제목이 없습니다");
    }

    if (template.getType() == null) {
      throw new IllegalArgumentException("퀘스트 타입이 없습니다");
    }

    if (template.getRequiredLevel() < 1) {
      throw new IllegalArgumentException("필요 레벨이 유효하지 않습니다: " + template.getRequiredLevel());
    }

    if (template.getObjectives() == null || template.getObjectives().isEmpty()) {
      throw new IllegalArgumentException("퀘스트 목표가 없습니다");
    }

    if (template.getReward() == null) {
      throw new IllegalArgumentException("퀘스트 보상이 없습니다");
    }

    // 보상 검증
    QuestRewardData reward = template.getReward();
    if (reward.getExperience() < 0 || reward.getGold() < 0) {
      throw new IllegalArgumentException("보상 값이 음수입니다");
    }
  }

  /**
   * 기본 퀘스트 템플릿 생성 (폴백)
   */
  private static Map<String, QuestTemplateData> createDefaultQuestTemplates(String category) {
    logger.info("기본 {} 퀘스트 템플릿을 코드로 생성 중...", category);

    Map<String, QuestTemplateData> defaultTemplates = new HashMap<>();

    switch (category) {
      case "메인":
        defaultTemplates.put("quest_001", createDefaultSlimeQuest());
        break;
      case "사이드":
        defaultTemplates.put("quest_005", createDefaultLevelQuest());
        break;
      case "일일":
        defaultTemplates.put("daily_kill_template", createDefaultDailyKillQuest());
        break;
      default:
        logger.warn("알 수 없는 퀘스트 카테고리: {}", category);
    }

    logger.info("기본 {} 퀘스트 템플릿 생성 완료: {}개", category, defaultTemplates.size());
    return defaultTemplates;
  }

  /**
   * 기본 슬라임 퀘스트 템플릿 생성
   */
  private static QuestTemplateData createDefaultSlimeQuest() {
    QuestTemplateData template = new QuestTemplateData();
    template.setId("quest_001");
    template.setTitle("슬라임 사냥꾼");
    template.setDescription("마을 근처의 슬라임 5마리를 처치하세요.");
    template.setType("KILL");
    template.setRequiredLevel(1);
    template.setCategory("MAIN");

    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("kill_슬라임", 5);
    template.setObjectives(objectives);

    QuestRewardData reward = new QuestRewardData();
    reward.setExperience(50);
    reward.setGold(100);

    List<QuestItemReward> items = new ArrayList<>();
    QuestItemReward healthPotion = new QuestItemReward();
    healthPotion.setItemId("HEALTH_POTION");
    healthPotion.setQuantity(2);
    healthPotion.setRarity("COMMON");
    items.add(healthPotion);
    reward.setItems(items);

    template.setReward(reward);
    template.setPrerequisites(new ArrayList<>());
    template.setUnlocks(List.of("quest_002"));
    template.setRepeatable(false);
    template.setTimeLimit(0);
    template.setTags(List.of("beginner", "combat"));

    return template;
  }

  /**
   * 기본 레벨 퀘스트 템플릿 생성
   */
  private static QuestTemplateData createDefaultLevelQuest() {
    QuestTemplateData template = new QuestTemplateData();
    template.setId("quest_005");
    template.setTitle("성장하는 모험가");
    template.setDescription("레벨 5에 도달하세요.");
    template.setType("LEVEL");
    template.setRequiredLevel(1);
    template.setCategory("SIDE");

    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("reach_level", 5);
    template.setObjectives(objectives);

    QuestRewardData reward = new QuestRewardData();
    reward.setExperience(100);
    reward.setGold(150);

    List<QuestItemReward> items = new ArrayList<>();
    QuestItemReward healthPotion = new QuestItemReward();
    healthPotion.setItemId("HEALTH_POTION");
    healthPotion.setQuantity(3);
    healthPotion.setRarity("COMMON");
    items.add(healthPotion);
    reward.setItems(items);

    template.setReward(reward);
    template.setPrerequisites(new ArrayList<>());
    template.setUnlocks(List.of("quest_006"));
    template.setRepeatable(false);
    template.setTimeLimit(0);
    template.setTags(List.of("progression", "reward"));

    return template;
  }

  /**
   * 기본 일일 사냥 퀘스트 템플릿 생성
   */
  private static QuestTemplateData createDefaultDailyKillQuest() {
    QuestTemplateData template = new QuestTemplateData();
    template.setId("daily_kill_template");
    template.setTitle("일일 사냥 임무");
    template.setDescription("오늘의 목표 몬스터를 처치하세요.");
    template.setType("KILL");
    template.setRequiredLevel(1);
    template.setCategory("DAILY");

    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("kill_daily_target", 8);
    template.setObjectives(objectives);

    QuestRewardData reward = new QuestRewardData();
    reward.setExperience(80);
    reward.setGold(120);
    reward.setItems(new ArrayList<>());

    template.setReward(reward);
    template.setPrerequisites(new ArrayList<>());
    template.setUnlocks(new ArrayList<>());
    template.setRepeatable(true);
    template.setTimeLimit(86400); // 24시간

    template.setTags(List.of("daily", "combat"));
    template.setVariableTargets(List.of("슬라임", "고블린", "오크", "스켈레톤", "거미", "늑대"));

    VariableQuantity variableQuantity = new VariableQuantity();
    variableQuantity.setMin(5);
    variableQuantity.setMax(12);
    template.setVariableQuantity(variableQuantity);

    return template;
  }

  // === 카테고리별 로드 메서드들 ===

  /**
   * 메인 퀘스트만 로드
   */
  public static Map<String, QuestTemplateData> loadMainQuests() {
    return loadQuestTemplates(MAIN_QUESTS_CONFIG, "메인");
  }

  /**
   * 사이드 퀘스트만 로드
   */
  public static Map<String, QuestTemplateData> loadSideQuests() {
    return loadQuestTemplates(SIDE_QUESTS_CONFIG, "사이드");
  }

  /**
   * 일일 퀘스트만 로드
   */
  public static Map<String, QuestTemplateData> loadDailyQuests() {
    return loadQuestTemplates(DAILY_QUESTS_CONFIG, "일일");
  }

  /**
   * 주간 퀘스트만 로드
   */
  public static Map<String, QuestTemplateData> loadWeeklyQuests() {
    return loadQuestTemplates(WEEKLY_QUESTS_CONFIG, "주간");
  }

  /**
   * 특정 카테고리의 퀘스트 필터링
   */
  public static Map<String, QuestTemplateData> filterByCategory(Map<String, QuestTemplateData> templates, String category) {
    return templates.entrySet().stream().filter(entry -> category.equals(entry.getValue().getCategory()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * 특정 레벨 범위의 퀘스트 필터링
   */
  public static Map<String, QuestTemplateData> filterByLevelRange(Map<String, QuestTemplateData> templates, int minLevel, int maxLevel) {
    return templates.entrySet().stream().filter(entry -> {
      int requiredLevel = entry.getValue().getRequiredLevel();
      return requiredLevel >= minLevel && requiredLevel <= maxLevel;
    }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * 태그로 퀘스트 필터링
   */
  public static Map<String, QuestTemplateData> filterByTag(Map<String, QuestTemplateData> templates, String tag) {
    return templates.entrySet().stream().filter(entry -> {
      List<String> tags = entry.getValue().getTags();
      return tags != null && tags.contains(tag);
    }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * 로더 상태 정보 출력
   */
  public static void printLoaderStatus() {
    System.out.println("\n=== 📚 QuestTemplateLoader 상태 ===");

    // 각 카테고리별 로드 테스트
    Map<String, QuestTemplateData> mainQuests = loadMainQuests();
    Map<String, QuestTemplateData> sideQuests = loadSideQuests();
    Map<String, QuestTemplateData> dailyQuests = loadDailyQuests();
    Map<String, QuestTemplateData> weeklyQuests = loadWeeklyQuests();

    System.out.printf("📖 메인 퀘스트: %d개%n", mainQuests.size());
    System.out.printf("📋 사이드 퀘스트: %d개%n", sideQuests.size());
    System.out.printf("⏰ 일일 퀘스트: %d개%n", dailyQuests.size());
    System.out.printf("📅 주간 퀘스트: %d개%n", weeklyQuests.size());

    int totalTemplates = mainQuests.size() + sideQuests.size() + dailyQuests.size() + weeklyQuests.size();
    System.out.printf("📊 총 템플릿: %d개%n", totalTemplates);

    // 설정 파일 존재 여부 확인
    System.out.println("\n🔍 설정 파일 상태:");
    System.out.printf("  main-quests.json: %s%n", isConfigFileExists(MAIN_QUESTS_CONFIG) ? "✅ 존재" : "❌ 없음");
    System.out.printf("  side-quests.json: %s%n", isConfigFileExists(SIDE_QUESTS_CONFIG) ? "✅ 존재" : "❌ 없음");
    System.out.printf("  daily-quests.json: %s%n", isConfigFileExists(DAILY_QUESTS_CONFIG) ? "✅ 존재" : "❌ 없음");
    System.out.printf("  weekly-quests.json: %s%n", isConfigFileExists(WEEKLY_QUESTS_CONFIG) ? "✅ 존재" : "❌ 없음");

    System.out.println("===================================");
  }
}
