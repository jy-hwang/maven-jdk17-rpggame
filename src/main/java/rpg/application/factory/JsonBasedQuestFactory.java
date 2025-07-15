package rpg.application.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.domain.quest.Quest;
import rpg.domain.quest.QuestReward;
import rpg.domain.quest.QuestRewardData;
import rpg.domain.quest.QuestTemplateData;
import rpg.infrastructure.data.loader.QuestTemplateLoader;

/**
 * @formatter:off
 * JSON 템플릿 기반 퀘스트 팩토리
 * - 외부 JSON 파일에서 퀘스트 템플릿 로드
 * - 템플릿 기반 퀘스트 생성
 * - 동적 퀘스트 생성 지원
 * - 카테고리별 퀘스트 관리
 * @formatter:on
 */
public class JsonBasedQuestFactory {
  private static final Logger logger = LoggerFactory.getLogger(JsonBasedQuestFactory.class);
  private static JsonBasedQuestFactory instance;

  // 템플릿 저장소
  private final Map<String, QuestTemplateData> allTemplates;
  private final Map<String, QuestTemplateData> mainQuestTemplates;
  private final Map<String, QuestTemplateData> sideQuestTemplates;
  private final Map<String, QuestTemplateData> dailyQuestTemplates;
  private final Map<String, QuestTemplateData> weeklyQuestTemplates;

  // 변환기
  private final QuestTemplateConverter converter;

  private JsonBasedQuestFactory() {
    logger.info("JsonBasedQuestFactory 초기화 시작...");

    // 팩토리 의존성
    GameItemFactory itemFactory = GameItemFactory.getInstance();
    this.converter = new QuestTemplateConverter(itemFactory);

    // 모든 템플릿 로드
    this.allTemplates = QuestTemplateLoader.loadAllQuestTemplates();

    // 카테고리별 분류
    this.mainQuestTemplates = filterByCategory("MAIN");
    this.sideQuestTemplates = filterByCategory("SIDE");
    this.dailyQuestTemplates = filterByCategory("DAILY");
    this.weeklyQuestTemplates = filterByCategory("WEEKLY");

    logger.info("JsonBasedQuestFactory 초기화 완료: 총 {}개 템플릿 로드", allTemplates.size());
    logTemplateStatistics();
  }

  public static JsonBasedQuestFactory getInstance() {
    if (instance == null) {
      instance = new JsonBasedQuestFactory();
    }
    return instance;
  }

  /**
   * 템플릿 통계 로깅
   */
  private void logTemplateStatistics() {
    logger.info("퀘스트 템플릿 통계:");
    logger.info("  메인 퀘스트: {}개", mainQuestTemplates.size());
    logger.info("  사이드 퀘스트: {}개", sideQuestTemplates.size());
    logger.info("  일일 퀘스트: {}개", dailyQuestTemplates.size());
    logger.info("  주간 퀘스트: {}개", weeklyQuestTemplates.size());
  }

  /**
   * 카테고리별 템플릿 필터링
   */
  private Map<String, QuestTemplateData> filterByCategory(String category) {
    return allTemplates.entrySet().stream().filter(entry -> category.equals(entry.getValue().getCategory())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  // ==================== 기본 퀘스트 생성 메서드들 ====================

  /**
   * ID로 퀘스트 생성
   */
  public Quest createQuest(String questId) {
    QuestTemplateData template = allTemplates.get(questId);
    if (template == null) {
      logger.warn("존재하지 않는 퀘스트 템플릿: {}", questId);
      return null;
    }

    Quest quest = converter.convertToQuest(template);
    if (quest != null) {
      logger.debug("퀘스트 생성 완료: {} - {}", questId, quest.getTitle());
    }
    return quest;
  }

  /**
   * 메인 퀘스트 생성
   */
  public Quest createMainQuest(String questId) {
    QuestTemplateData template = mainQuestTemplates.get(questId);
    if (template == null) {
      logger.warn("존재하지 않는 메인 퀘스트 템플릿: {}", questId);
      return null;
    }

    return converter.convertToQuest(template);
  }

  /**
   * 사이드 퀘스트 생성
   */
  public Quest createSideQuest(String questId) {
    QuestTemplateData template = sideQuestTemplates.get(questId);
    if (template == null) {
      logger.warn("존재하지 않는 사이드 퀘스트 템플릿: {}", questId);
      return null;
    }

    return converter.convertToQuest(template);
  }

  // ==================== 동적 퀘스트 생성 메서드들 ====================

  /**
   * 일일 퀘스트 생성
   */
  public Quest createDailyQuest(Quest.QuestType type) {
    // 타입에 맞는 일일 퀘스트 템플릿 찾기
    QuestTemplateData template = findDailyTemplate(type);
    if (template == null) {
      logger.warn("타입 {}에 대한 일일 퀘스트 템플릿을 찾을 수 없음", type);
      return null;
    }

    // 동적 퀘스트로 변환
    Quest dailyQuest = converter.convertToDynamicQuest(template);
    if (dailyQuest != null) {
      logger.info("일일 퀘스트 생성: {} - {}", dailyQuest.getId(), dailyQuest.getTitle());
    }
    return dailyQuest;
  }

  /**
   * 주간 퀘스트 생성
   */
  public Quest createWeeklyQuest(Quest.QuestType type) {
    QuestTemplateData template = findWeeklyTemplate(type);
    if (template == null) {
      logger.warn("타입 {}에 대한 주간 퀘스트 템플릿을 찾을 수 없음", type);
      return null;
    }

    Quest weeklyQuest = converter.convertToDynamicQuest(template);
    if (weeklyQuest != null) {
      logger.info("주간 퀘스트 생성: {} - {}", weeklyQuest.getId(), weeklyQuest.getTitle());
    }
    return weeklyQuest;
  }

  /**
   * 플레이어 레벨에 맞는 퀘스트 생성
   */
  public Quest createLevelAppropriateQuest(int playerLevel) {
    // 플레이어 레벨에 맞는 템플릿 찾기
    List<QuestTemplateData> suitableTemplates =
        allTemplates.values().stream().filter(template -> template.getRequiredLevel() <= playerLevel).filter(template -> template.getRequiredLevel() >= Math.max(1, playerLevel - 3)) // 너무 낮은 레벨 제외
            .filter(template -> !"DAILY".equals(template.getCategory()) && !"WEEKLY".equals(template.getCategory())) // 일일/주간 제외
            .collect(Collectors.toList());

    if (suitableTemplates.isEmpty()) {
      logger.warn("레벨 {}에 적합한 퀘스트 템플릿이 없음", playerLevel);
      // return createDynamicLevelQuest(playerLevel);
    }

    // 랜덤 선택
    QuestTemplateData selectedTemplate = suitableTemplates.get((int) (Math.random() * suitableTemplates.size()));

    Quest quest = converter.convertToQuest(selectedTemplate);
    if (quest != null) {
      logger.info("레벨 적합 퀘스트 생성: {} (레벨 {})", quest.getTitle(), playerLevel);
    }
    return quest;
  }

  /**
   * 랜덤 퀘스트 생성
   */
  public Quest createRandomQuest(Quest.QuestType type, int playerLevel) {
    List<QuestTemplateData> typeTemplates =
        allTemplates.values().stream().filter(template -> type.name().equals(template.getType())).filter(template -> template.getRequiredLevel() <= playerLevel).collect(Collectors.toList());

    if (typeTemplates.isEmpty()) {
      logger.warn("타입 {} 레벨 {}에 적합한 템플릿이 없음", type, playerLevel);
      return null;
    }

    QuestTemplateData selectedTemplate = typeTemplates.get((int) (Math.random() * typeTemplates.size()));

    Quest quest = converter.convertToQuest(selectedTemplate);
    if (quest != null) {
      logger.info("랜덤 {} 퀘스트 생성: {}", type.name(), quest.getTitle());
    }
    return quest;
  }

  private Quest createDynamicQuest(String category, int playerLevel) {
    String questId = category + "_dynamic_" + System.currentTimeMillis();
    String title = switch (category) {
      case "MAIN" -> "긴급 처치 명령";
      case "SIDE" -> "사냥꾼의 의뢰";
      case "DAILY" -> "일일 사냥 임무";
      default -> "특별 임무";
    };

    String description = switch (category) {
      case "MAIN" -> "마을을 위협하는 몬스터를 처치하세요.";
      case "SIDE" -> "사냥꾼 길드에서 몬스터 처치를 의뢰했습니다.";
      case "DAILY" -> "오늘의 일일 사냥 임무를 완료하세요.";
      default -> "특별한 임무를 완료하세요.";
    };

    // 🔧 수정: 한국어 키 → 영어 몬스터 ID 사용
    Map<String, Integer> objectives = new HashMap<>();
    if (playerLevel <= 3) {
      objectives.put("kill_FOREST_SLIME", Math.max(3, playerLevel * 2));
    } else if (playerLevel <= 6) {
      objectives.put("kill_FOREST_GOBLIN", Math.max(2, playerLevel));
    } else if (playerLevel <= 10) {
      objectives.put("kill_WILD_BOAR", Math.max(2, playerLevel / 2));
    } else {
      objectives.put("kill_CAVE_TROLL", Math.max(1, playerLevel / 3));
    }

    // 레벨 기반 보상
    int baseExp = 50 * playerLevel;
    int baseGold = 30 * playerLevel;
    QuestReward reward = new QuestReward(baseExp, baseGold);

    Quest dynamicQuest = new Quest(questId, title, description, Quest.QuestType.KILL, playerLevel, objectives, reward);

    logger.info("동적 퀘스트 생성 완료: {}", title);
    return dynamicQuest;
  }


  // ==================== 헬퍼 메서드들 ====================

  /**
   * 일일 퀘스트 템플릿 찾기
   */
  private QuestTemplateData findDailyTemplate(Quest.QuestType type) {
    return dailyQuestTemplates.values().stream().filter(template -> type.name().equals(template.getType())).findFirst().orElse(null);
  }

  /**
   * 주간 퀘스트 템플릿 찾기
   */
  private QuestTemplateData findWeeklyTemplate(Quest.QuestType type) {
    return weeklyQuestTemplates.values().stream().filter(template -> type.name().equals(template.getType())).findFirst().orElse(null);
  }

  /**
   * 동적 레벨 퀘스트 생성 (템플릿이 없을 때의 폴백)
   */
  private Quest createDynamicLevelQuest(int playerLevel) {
    logger.info("동적 레벨 퀘스트 생성 중... (레벨: {})", playerLevel);

    String questId = "dynamic_level_" + playerLevel + "_" + System.currentTimeMillis();
    String title = "레벨 " + playerLevel + " 모험가의 시험";
    String description = "당신의 실력을 증명할 시간입니다.";

    // 레벨에 맞는 목표 설정
    Map<String, Integer> objectives = new HashMap<>();
    if (playerLevel <= 3) {
      objectives.put("kill_슬라임", Math.max(3, playerLevel * 2));
    } else if (playerLevel <= 6) {
      objectives.put("kill_고블린", Math.max(2, playerLevel));
    } else {
      objectives.put("kill_오크", Math.max(1, playerLevel / 2));
    }

    // 레벨 기반 보상
    int baseExp = 50 * playerLevel;
    int baseGold = 30 * playerLevel;
    QuestReward reward = new QuestReward(baseExp, baseGold);

    Quest dynamicQuest = new Quest(questId, title, description, Quest.QuestType.KILL, playerLevel, objectives, reward);

    logger.info("동적 퀘스트 생성 완료: {}", title);
    return dynamicQuest;
  }

  // ==================== 조회 메서드들 ====================

  /**
   * 모든 메인 퀘스트 ID 목록
   */
  public List<String> getAllMainQuestIds() {
    return mainQuestTemplates.keySet().stream().sorted().collect(Collectors.toList());
  }

  /**
   * 모든 사이드 퀘스트 ID 목록
   */
  public List<String> getAllSideQuestIds() {
    return sideQuestTemplates.keySet().stream().sorted().collect(Collectors.toList());
  }

  /**
   * 레벨별 사용 가능한 퀘스트 ID 목록
   */
  public List<String> getAvailableQuestIds(int playerLevel) {
    return allTemplates.entrySet().stream().filter(entry -> entry.getValue().getRequiredLevel() <= playerLevel).map(Map.Entry::getKey).sorted().collect(Collectors.toList());
  }

  /**
   * 태그별 퀘스트 ID 목록
   */
  public List<String> getQuestIdsByTag(String tag) {
    return allTemplates.entrySet().stream().filter(entry -> {
      List<String> tags = entry.getValue().getTags();
      return tags != null && tags.contains(tag);
    }).map(Map.Entry::getKey).sorted().collect(Collectors.toList());
  }

  /**
   * 퀘스트 템플릿 정보 조회
   */
  public QuestTemplateData getQuestTemplate(String questId) {
    return allTemplates.get(questId);
  }

  /**
   * 퀘스트 존재 여부 확인
   */
  public boolean hasQuest(String questId) {
    return allTemplates.containsKey(questId);
  }

  /**
   * 카테고리별 퀘스트 개수
   */
  public int getQuestCount(String category) {
    switch (category.toUpperCase()) {
      case "MAIN":
        return mainQuestTemplates.size();
      case "SIDE":
        return sideQuestTemplates.size();
      case "DAILY":
        return dailyQuestTemplates.size();
      case "WEEKLY":
        return weeklyQuestTemplates.size();
      default:
        return 0;
    }
  }

  /**
   * 특정 타입의 퀘스트 개수
   */
  public int getQuestCountByType(Quest.QuestType type) {
    return (int) allTemplates.values().stream().filter(template -> type.name().equals(template.getType())).count();
  }

  // ==================== 템플릿 관리 메서드들 ====================

  /**
   * 템플릿 다시 로드
   */
  public void reloadTemplates() {
    logger.info("퀘스트 템플릿 다시 로드 중...");

    try {
      Map<String, QuestTemplateData> newTemplates = QuestTemplateLoader.loadAllQuestTemplates();

      // 기존 템플릿 교체
      allTemplates.clear();
      allTemplates.putAll(newTemplates);

      // 카테고리별 재분류
      mainQuestTemplates.clear();
      mainQuestTemplates.putAll(filterByCategory("MAIN"));

      sideQuestTemplates.clear();
      sideQuestTemplates.putAll(filterByCategory("SIDE"));

      dailyQuestTemplates.clear();
      dailyQuestTemplates.putAll(filterByCategory("DAILY"));

      weeklyQuestTemplates.clear();
      weeklyQuestTemplates.putAll(filterByCategory("WEEKLY"));

      logger.info("퀘스트 템플릿 다시 로드 완료: {}개", allTemplates.size());
      logTemplateStatistics();

    } catch (Exception e) {
      logger.error("퀘스트 템플릿 다시 로드 실패", e);
    }
  }

  /**
   * 템플릿 검증
   */
  public boolean validateTemplates() {
    logger.info("퀘스트 템플릿 검증 시작...");

    boolean allValid = true;
    int validCount = 0;
    int errorCount = 0;

    for (Map.Entry<String, QuestTemplateData> entry : allTemplates.entrySet()) {
      String templateId = entry.getKey();
      QuestTemplateData template = entry.getValue();

      try {
        // 기본 검증
        if (template.getId() == null || template.getTitle() == null || template.getType() == null || template.getObjectives() == null) {
          throw new IllegalArgumentException("필수 필드가 누락됨");
        }

        // 퀘스트 변환 테스트
        Quest testQuest = converter.convertToQuest(template);
        if (testQuest == null) {
          throw new IllegalStateException("퀘스트 변환 실패");
        }

        validCount++;

      } catch (Exception e) {
        allValid = false;
        errorCount++;
        logger.warn("템플릿 검증 실패: {} - {}", templateId, e.getMessage());
      }
    }

    logger.info("퀘스트 템플릿 검증 완료: 성공 {}개, 실패 {}개", validCount, errorCount);
    return allValid;
  }

  // ==================== 상태 출력 메서드들 ====================

  /**
   * 팩토리 상태 정보 출력
   */
  public void printFactoryStatus() {
    System.out.println("\n=== 🏭 JsonBasedQuestFactory 상태 ===");

    // 기본 통계
    System.out.printf("📊 로드된 템플릿: %d개%n", allTemplates.size());
    System.out.printf("   메인 퀘스트: %d개%n", mainQuestTemplates.size());
    System.out.printf("   사이드 퀘스트: %d개%n", sideQuestTemplates.size());
    System.out.printf("   일일 퀘스트: %d개%n", dailyQuestTemplates.size());
    System.out.printf("   주간 퀘스트: %d개%n", weeklyQuestTemplates.size());

    // 타입별 분포
    System.out.println("\n🎭 타입별 분포:");
    for (Quest.QuestType type : Quest.QuestType.values()) {
      int count = getQuestCountByType(type);
      if (count > 0) {
        System.out.printf("   %s: %d개%n", type.name(), count);
      }
    }

    // 레벨별 분포
    System.out.println("\n📈 레벨별 분포:");
    Map<Integer, Long> levelDistribution = allTemplates.values().stream().collect(Collectors.groupingBy(QuestTemplateData::getRequiredLevel, Collectors.counting()));

    levelDistribution.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> System.out.printf("   레벨 %d: %d개%n", entry.getKey(), entry.getValue()));

    // 템플릿 검증 상태
    System.out.println("\n🔍 검증 상태:");
    boolean isValid = validateTemplates();
    System.out.printf("   전체 검증: %s%n", isValid ? "✅ 통과" : "❌ 실패");

    // 연동 상태
    System.out.println("\n🔗 연동 상태:");
    System.out.printf("   GameItemFactory: %s%n", GameItemFactory.getInstance() != null ? "✅ 연결됨" : "❌ 연결안됨");
    System.out.printf("   QuestTemplateConverter: %s%n", converter != null ? "✅ 연결됨" : "❌ 연결안됨");

    System.out.println("=========================================");
  }

  /**
   * 퀘스트 템플릿 상세 정보 출력
   */
  public void printQuestTemplateDetails(String questId) {
    QuestTemplateData template = allTemplates.get(questId);
    if (template == null) {
      System.out.printf("퀘스트 템플릿을 찾을 수 없습니다: %s%n", questId);
      return;
    }

    System.out.printf("\n=== 📋 퀘스트 템플릿 상세: %s ===\n", questId);
    System.out.printf("제목: %s%n", template.getTitle());
    System.out.printf("설명: %s%n", template.getDescription());
    System.out.printf("타입: %s%n", template.getType());
    System.out.printf("카테고리: %s%n", template.getCategory());
    System.out.printf("필요 레벨: %d%n", template.getRequiredLevel());
    System.out.printf("반복 가능: %s%n", template.isRepeatable() ? "예" : "아니오");

    if (template.getTimeLimit() > 0) {
      System.out.printf("시간 제한: %d초%n", template.getTimeLimit());
    }

    System.out.println("\n목표:");
    template.getObjectives().forEach((key, value) -> System.out.printf("   %s: %d%n", key, value));

    System.out.println("\n보상:");
    QuestRewardData reward = template.getReward();
    System.out.printf("   경험치: %d%n", reward.getExperience());
    System.out.printf("   골드: %d%n", reward.getGold());

    if (reward.getItems() != null && !reward.getItems().isEmpty()) {
      System.out.println("   아이템:");
      reward.getItems().forEach(item -> System.out.printf("     %s x%d (%s)%n", item.getItemId(), item.getQuantity(), item.getRarity()));
    }

    if (template.getTags() != null && !template.getTags().isEmpty()) {
      System.out.printf("태그: %s%n", String.join(", ", template.getTags()));
    }

    if (template.getPrerequisites() != null && !template.getPrerequisites().isEmpty()) {
      System.out.printf("선행 퀘스트: %s%n", String.join(", ", template.getPrerequisites()));
    }

    if (template.getUnlocks() != null && !template.getUnlocks().isEmpty()) {
      System.out.printf("해금 퀘스트: %s%n", String.join(", ", template.getUnlocks()));
    }

    System.out.println("=======================================");
  }

  /**
   * 모든 퀘스트 목록 출력
   */
  public void printAllQuests() {
    System.out.println("\n=== 📚 전체 퀘스트 목록 ===");

    System.out.println("\n📖 메인 퀘스트:");
    mainQuestTemplates.forEach((id, template) -> System.out.printf("   %s - %s (레벨 %d)%n", id, template.getTitle(), template.getRequiredLevel()));

    System.out.println("\n📋 사이드 퀘스트:");
    sideQuestTemplates.forEach((id, template) -> System.out.printf("   %s - %s (레벨 %d)%n", id, template.getTitle(), template.getRequiredLevel()));

    System.out.println("\n⏰ 일일 퀘스트:");
    dailyQuestTemplates.forEach((id, template) -> System.out.printf("   %s - %s (%s)%n", id, template.getTitle(), template.getType()));

    System.out.println("\n📅 주간 퀘스트:");
    weeklyQuestTemplates.forEach((id, template) -> System.out.printf("   %s - %s (%s)%n", id, template.getTitle(), template.getType()));

    System.out.println("============================");
  }
}
