// GameQuestFactory.java - 퀘스트 생성을 담당하는 Factory 패턴
package rpg.application.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.domain.item.GameItem;
import rpg.domain.item.ItemRarity;
import rpg.domain.quest.Quest;
import rpg.domain.quest.QuestReward;

/**
 * 퀘스트를 생성하는 Factory 클래스 - 다양한 타입의 퀘스트를 표준화된 방식으로 생성 - 퀘스트 템플릿 기반 생성 지원 - 동적 퀘스트 생성 기능
 */
public class GameQuestFactory {
  private static final Logger logger = LoggerFactory.getLogger(GameQuestFactory.class);
  private static GameQuestFactory instance;

  private final GameItemFactory itemFactory;
  private final GameEffectFactory effectFactory;

  // 퀘스트 템플릿 저장소
  private final Map<String, QuestTemplate> questTemplates;

  private GameQuestFactory() {
    this.itemFactory = GameItemFactory.getInstance();
    this.effectFactory = new GameEffectFactory();
    this.questTemplates = new HashMap<>();
    initializeQuestTemplates();
  }

  public static GameQuestFactory getInstance() {
    if (instance == null) {
      instance = new GameQuestFactory();
    }
    return instance;
  }

  /**
   * 퀘스트 템플릿 정보를 담는 내부 클래스
   */
  public static class QuestTemplate {
    private final String id;
    private final String title;
    private final String description;
    private final Quest.QuestType type;
    private final int requiredLevel;
    private final Map<String, Integer> objectives;
    private final QuestRewardTemplate rewardTemplate;

    public QuestTemplate(String id, String title, String description, Quest.QuestType type, int requiredLevel, Map<String, Integer> objectives,
        QuestRewardTemplate rewardTemplate) {
      this.id = id;
      this.title = title;
      this.description = description;
      this.type = type;
      this.requiredLevel = requiredLevel;
      this.objectives = new HashMap<>(objectives);
      this.rewardTemplate = rewardTemplate;
    }

    // Getters
    public String getId() {
      return id;
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

    public int getRequiredLevel() {
      return requiredLevel;
    }

    public Map<String, Integer> getObjectives() {
      return new HashMap<>(objectives);
    }

    public QuestRewardTemplate getRewardTemplate() {
      return rewardTemplate;
    }
  }

  /**
   * 퀘스트 보상 템플릿 정보를 담는 내부 클래스
   */
  public static class QuestRewardTemplate {
    private final int experience;
    private final int gold;
    private final String rewardItemId;
    private final int rewardItemQuantity;
    private final ItemRarity rewardItemRarity;

    public QuestRewardTemplate(int experience, int gold) {
      this(experience, gold, null, 0, ItemRarity.COMMON);
    }

    public QuestRewardTemplate(int experience, int gold, String rewardItemId, int rewardItemQuantity, ItemRarity rewardItemRarity) {
      this.experience = experience;
      this.gold = gold;
      this.rewardItemId = rewardItemId;
      this.rewardItemQuantity = rewardItemQuantity;
      this.rewardItemRarity = rewardItemRarity;
    }

    // Getters
    public int getExperience() {
      return experience;
    }

    public int getGold() {
      return gold;
    }

    public String getRewardItemId() {
      return rewardItemId;
    }

    public int getRewardItemQuantity() {
      return rewardItemQuantity;
    }

    public ItemRarity getRewardItemRarity() {
      return rewardItemRarity;
    }

    public boolean hasItemReward() {
      return rewardItemId != null;
    }
  }

  /**
   * 기본 퀘스트 템플릿들을 초기화
   */
  private void initializeQuestTemplates() {
    logger.info("퀘스트 템플릿 초기화 중...");

    // 1. 초보자 퀘스트 - 슬라임 사냥
    registerTemplate(createSlimeQuestTemplate());

    // 2. 중급 퀘스트 - 고블린 소탕
    registerTemplate(createGoblinQuestTemplate());

    // 3. 고급 퀘스트 - 오크 토벌
    registerTemplate(createOrcQuestTemplate());

    // 4. 최종 퀘스트 - 드래곤 슬레이어
    registerTemplate(createDragonQuestTemplate());

    // 5. 레벨업 퀘스트
    registerTemplate(createLevelQuestTemplate());

    // 6. 수집 퀘스트
    registerTemplate(createCollectionQuestTemplate());

    // 7. 일일 퀘스트들
    registerTemplate(createDailyKillQuestTemplate());
    registerTemplate(createDailyCollectionQuestTemplate());

    logger.info("퀘스트 템플릿 초기화 완료: {}개 템플릿 등록", questTemplates.size());
  }

  /**
   * 퀘스트 템플릿 등록
   */
  private void registerTemplate(QuestTemplate template) {
    questTemplates.put(template.getId(), template);
    logger.debug("퀘스트 템플릿 등록: {} - {}", template.getId(), template.getTitle());
  }

  /**
   * ID로 퀘스트 생성
   */
  public Quest createQuest(String questId) {
    QuestTemplate template = questTemplates.get(questId);
    if (template == null) {
      logger.warn("존재하지 않는 퀘스트 템플릿: {}", questId);
      return null;
    }

    return createQuestFromTemplate(template);
  }

  /**
   * 퀘스트 타입으로 랜덤 퀘스트 생성
   */
  public Quest createRandomQuest(Quest.QuestType type, int playerLevel) {
    List<QuestTemplate> availableTemplates = questTemplates.values().stream().filter(template -> template.getType() == type)
        .filter(template -> template.getRequiredLevel() <= playerLevel).toList();

    if (availableTemplates.isEmpty()) {
      logger.warn("타입 {}에 대해 레벨 {}에서 사용 가능한 퀘스트 템플릿이 없음", type, playerLevel);
      return null;
    }

    QuestTemplate selectedTemplate = availableTemplates.get((int) (Math.random() * availableTemplates.size()));

    return createQuestFromTemplate(selectedTemplate);
  }

  /**
   * 플레이어 레벨에 맞는 동적 퀘스트 생성
   */
  public Quest createLevelAppropriateQuest(int playerLevel) {
    // 레벨에 따른 퀘스트 타입 결정
    Quest.QuestType[] types = Quest.QuestType.values();
    Quest.QuestType selectedType = types[(int) (Math.random() * types.length)];

    return createDynamicQuest(selectedType, playerLevel);
  }

  /**
   * 일일 퀘스트 생성
   */
  public Quest createDailyQuest(Quest.QuestType type) {
    String dailyId = "daily_" + type.name().toLowerCase() + "_" + System.currentTimeMillis();

    switch (type) {
      case KILL:
        return createDailyKillQuest(dailyId);
      case COLLECT:
        return createDailyCollectionQuest(dailyId);
      default:
        logger.warn("지원하지 않는 일일 퀘스트 타입: {}", type);
        return null;
    }
  }

  /**
   * 템플릿으로부터 퀘스트 생성
   */
  private Quest createQuestFromTemplate(QuestTemplate template) {
    try {
      // 보상 생성
      QuestReward reward = createRewardFromTemplate(template.getRewardTemplate());

      // 퀘스트 생성
      Quest quest = new Quest(template.getId(), template.getTitle(), template.getDescription(), template.getType(), template.getRequiredLevel(),
          template.getObjectives(), reward);

      logger.debug("퀘스트 생성 완료: {} - {}", template.getId(), template.getTitle());
      return quest;

    } catch (Exception e) {
      logger.error("퀘스트 생성 실패: {}", template.getId(), e);
      return null;
    }
  }

  /**
   * 보상 템플릿으로부터 실제 보상 생성
   */
  private QuestReward createRewardFromTemplate(QuestRewardTemplate template) {
    QuestReward reward = new QuestReward(template.getExperience(), template.getGold());

    // 아이템 보상이 있는 경우
    if (template.hasItemReward()) {
      GameItem rewardItem = itemFactory.createItem(template.getRewardItemId());
      if (rewardItem != null) {
        reward.addItemReward(rewardItem, template.getRewardItemQuantity());
        logger.debug("퀘스트 보상 아이템 추가: {} x{}", rewardItem.getName(), template.getRewardItemQuantity());
      } else {
        logger.warn("퀘스트 보상 아이템 생성 실패: {}", template.getRewardItemId());
        // 폴백으로 골드 보상 증가
        reward = new QuestReward(template.getExperience(), template.getGold() + 50);
      }
    }

    return reward;
  }

  /**
   * 동적 퀘스트 생성 (레벨에 맞춰 스케일링)
   */
  private Quest createDynamicQuest(Quest.QuestType type, int playerLevel) {
    String questId = "dynamic_" + type.name().toLowerCase() + "_" + playerLevel;
    String title;
    String description;
    Map<String, Integer> objectives = new HashMap<>();
    QuestReward reward;

    // 레벨 기반 난이도 조정
    int difficulty = Math.max(1, playerLevel / 3);
    int baseExp = 50 * playerLevel;
    int baseGold = 30 * playerLevel;

    switch (type) {
      case KILL:
        title = "레벨 " + playerLevel + " 사냥 임무";
        description = "강해진 당신에게 어울리는 사냥 임무입니다.";
        objectives.put("kill_random_monster", difficulty + 2);
        reward = new QuestReward(baseExp, baseGold);
        break;

      case COLLECT:
        title = "레벨 " + playerLevel + " 수집 임무";
        description = "귀중한 아이템들을 수집해주세요.";
        objectives.put("collect_random_item", difficulty + 1);

        // 수집 퀘스트는 더 좋은 보상
        GameItem rewardItem = itemFactory.createRandomItemByRarity(playerLevel > 10 ? ItemRarity.RARE : ItemRarity.UNCOMMON);
        reward = new QuestReward(baseExp, baseGold, rewardItem, 1);
        break;

      case LEVEL:
        title = "성장의 길";
        description = "더 높은 레벨에 도달하세요.";
        objectives.put("reach_level", playerLevel + 1);
        reward = new QuestReward(baseExp * 2, baseGold);
        break;

      default:
        return null;
    }

    return new Quest(questId, title, description, type, playerLevel, objectives, reward);
  }

  // === 퀘스트 템플릿 생성 메서드들 ===

  private QuestTemplate createSlimeQuestTemplate() {
    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("kill_슬라임", 5);

    QuestRewardTemplate reward = new QuestRewardTemplate(50, 100, "HEALTH_POTION", 2, ItemRarity.COMMON);

    return new QuestTemplate("quest_001", "슬라임 사냥꾼", "마을 근처의 슬라임 5마리를 처치하세요.", Quest.QuestType.KILL, 1, objectives, reward);
  }

  private QuestTemplate createGoblinQuestTemplate() {
    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("kill_고블린", 3);

    QuestRewardTemplate reward = new QuestRewardTemplate(100, 200, "IRON_SWORD", 1, ItemRarity.UNCOMMON);

    return new QuestTemplate("quest_002", "고블린 소탕", "위험한 고블린 3마리를 처치하세요.", Quest.QuestType.KILL, 3, objectives, reward);
  }

  private QuestTemplate createOrcQuestTemplate() {
    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("kill_오크", 2);

    QuestRewardTemplate reward = new QuestRewardTemplate(200, 500, "PLATE_ARMOR", 1, ItemRarity.RARE);

    return new QuestTemplate("quest_003", "오크 토벌", "강력한 오크 2마리를 처치하세요.", Quest.QuestType.KILL, 5, objectives, reward);
  }

  private QuestTemplate createDragonQuestTemplate() {
    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("kill_드래곤", 1);

    QuestRewardTemplate reward = new QuestRewardTemplate(1000, 2000, "DRAGON_RING", 1, ItemRarity.LEGENDARY);

    return new QuestTemplate("quest_004", "드래곤 슬레이어", "전설의 드래곤을 처치하고 영웅이 되세요!", Quest.QuestType.KILL, 8, objectives, reward);
  }

  private QuestTemplate createLevelQuestTemplate() {
    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("reach_level", 5);

    QuestRewardTemplate reward = new QuestRewardTemplate(100, 150, "HEALTH_POTION", 3, ItemRarity.COMMON);

    return new QuestTemplate("quest_005", "성장하는 모험가", "레벨 5에 도달하세요.", Quest.QuestType.LEVEL, 1, objectives, reward);
  }

  private QuestTemplate createCollectionQuestTemplate() {
    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("collect_체력 물약", 5);

    QuestRewardTemplate reward = new QuestRewardTemplate(150, 100, "SPECIAL_POTION", 1, ItemRarity.RARE);

    return new QuestTemplate("quest_006", "물약 수집가", "체력 물약 5개를 수집하세요.", Quest.QuestType.COLLECT, 3, objectives, reward);
  }

  private QuestTemplate createDailyKillQuestTemplate() {
    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("kill_daily_target", 10);

    QuestRewardTemplate reward = new QuestRewardTemplate(80, 120);

    return new QuestTemplate("daily_kill_template", "일일 사냥 임무", "오늘의 목표 몬스터를 처치하세요.", Quest.QuestType.KILL, 1, objectives, reward);
  }

  private QuestTemplate createDailyCollectionQuestTemplate() {
    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("collect_daily_item", 5);

    QuestRewardTemplate reward = new QuestRewardTemplate(60, 80, "DAILY_REWARD_BOX", 1, ItemRarity.UNCOMMON);

    return new QuestTemplate("daily_collect_template", "일일 수집 임무", "오늘의 목표 아이템을 수집하세요.", Quest.QuestType.COLLECT, 1, objectives, reward);
  }

  // 일일 퀘스트 구체 생성 메서드들
  private Quest createDailyKillQuest(String questId) {
    String[] monsters = {"슬라임", "고블린", "오크", "스켈레톤"};
    String targetMonster = monsters[(int) (Math.random() * monsters.length)];

    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("kill_" + targetMonster, 8 + (int) (Math.random() * 5));

    QuestReward reward = new QuestReward(80, 120);

    return new Quest(questId, "일일 " + targetMonster + " 사냥", "오늘의 목표인 " + targetMonster + "을(를) 처치하세요.", Quest.QuestType.KILL, 1, objectives, reward);
  }

  private Quest createDailyCollectionQuest(String questId) {
    String[] items = {"체력 물약", "마나 물약", "철광석", "허브"};
    String targetItem = items[(int) (Math.random() * items.length)];

    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("collect_" + targetItem, 3 + (int) (Math.random() * 3));

    QuestReward reward = new QuestReward(60, 80);

    return new Quest(questId, "일일 " + targetItem + " 수집", "오늘의 목표인 " + targetItem + "을(를) 수집하세요.", Quest.QuestType.COLLECT, 1, objectives, reward);
  }

  // === 유틸리티 메서드들 ===

  /**
   * 등록된 모든 퀘스트 템플릿 ID 반환
   */
  public List<String> getAllQuestTemplateIds() {
    return questTemplates.keySet().stream().sorted().toList();
  }

  /**
   * 특정 타입의 퀘스트 템플릿 개수 반환
   */
  public int getQuestTemplateCount(Quest.QuestType type) {
    return (int) questTemplates.values().stream().filter(template -> template.getType() == type).count();
  }

  /**
   * 레벨 범위에 맞는 퀘스트 템플릿 개수 반환
   */
  public int getAvailableQuestCount(int playerLevel) {
    return (int) questTemplates.values().stream().filter(template -> template.getRequiredLevel() <= playerLevel).count();
  }

  /**
   * 팩토리 상태 정보 출력
   */
  public void printFactoryStatus() {
    System.out.println("\n=== 🏭 GameQuestFactory 상태 ===");
    System.out.printf("등록된 퀘스트 템플릿: %d개%n", questTemplates.size());

    System.out.println("\n📊 타입별 분포:");
    for (Quest.QuestType type : Quest.QuestType.values()) {
      int count = getQuestTemplateCount(type);
      if (count > 0) {
        System.out.printf("  %s: %d개%n", type.name(), count);
      }
    }

    System.out.println("\n🔧 연동된 팩토리:");
    System.out.printf("  GameItemFactory: %s%n", itemFactory != null ? "연결됨" : "연결안됨");
    System.out.printf("  GameEffectFactory: %s%n", effectFactory != null ? "연결됨" : "연결안됨");
    System.out.println("==========================");
  }
}
