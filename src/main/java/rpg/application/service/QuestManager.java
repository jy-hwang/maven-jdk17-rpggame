package rpg.application.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import rpg.application.factory.GameEffectFactory;
import rpg.application.factory.GameItemFactory;
import rpg.application.factory.JsonBasedQuestFactory;
import rpg.application.service.ImprovedDailyQuestManager.QuestTier;
import rpg.domain.item.GameConsumable;
import rpg.domain.item.GameEquipment;
import rpg.domain.item.GameItem;
import rpg.domain.item.ItemRarity;
import rpg.domain.item.effect.GameEffect;
import rpg.domain.player.Player;
import rpg.domain.quest.Quest;
import rpg.domain.quest.Quest.QuestStatus;
import rpg.domain.quest.QuestReward;

/**@formatter:off
 * 퀘스트를 관리하는 서비스 클래스 (QuestFactory 패턴 적용)
 * - QuestFactory를 사용하여 퀘스트 생성
 * - 템플릿 기반 퀘스트 관리
 * - 동적 퀘스트 생성 시스템
 * @formatter:on
 */
public class QuestManager {
  private static final Logger logger = LoggerFactory.getLogger(QuestManager.class);

  private List<Quest> availableQuests;
  private List<Quest> activeQuests;
  private List<Quest> completedQuests;

  private List<String> claimedRewardIds; // 보상 수령한 퀘스트 ID 목록

  // 팩토리 인스턴스 - JsonBasedQuestFactory 사용
  private final GameItemFactory itemFactory;
  private final JsonBasedQuestFactory jsonQuestFactory; // 변경

  // 🆕 추가된 필드들
  private final ImprovedDailyQuestManager dailyQuestManager;
  private final QuestHistoryManager questHistoryManager;
  private boolean useImprovedDailyQuestSystem = true;

  @JsonCreator
  public QuestManager() {
    this.itemFactory = GameItemFactory.getInstance();
    this.jsonQuestFactory = JsonBasedQuestFactory.getInstance(); // 추가

    this.availableQuests = new ArrayList<>();
    this.activeQuests = new ArrayList<>();
    this.completedQuests = new ArrayList<>();
    this.claimedRewardIds = new ArrayList<>();

    this.dailyQuestManager = new ImprovedDailyQuestManager();
    this.questHistoryManager = new QuestHistoryManager();
    initializeQuests();
    logger.info("QuestManager 초기화 완료 (JsonBasedQuestFactory 사용)");
  }

  // 로드 전용 생성자 (정적 팩토리 메서드)
  public static QuestManager createForLoading() {
    QuestManager questManager = new QuestManager();
    // 기본 퀘스트들을 제거 (로드된 데이터로 교체될 예정)
    questManager.availableQuests.clear();
    questManager.activeQuests.clear();
    questManager.completedQuests.clear();
    questManager.claimedRewardIds.clear();

    logger.info("로드용 QuestManager 생성 완료 (기본 퀘스트 제거됨)");
    return questManager;
  }

  // ==== 1. initializeQuests() 메서드 수정 ====
  private void initializeQuests() {
    logger.info("퀘스트 초기화 중... (JSON 템플릿 기반)");

    try {
      // JSON 파일의 모든 퀘스트 ID 로드
      List<String> allQuestIds = List.of(
          // 메인 퀘스트
          "quest_001", // 슬라임 사냥꾼 (JSON: 슬라임 3마리)
          "quest_002", // 고블린 소탕 (JSON: 고블린 2마리)
          "quest_003", // 오크 토벌 (JSON: 오크 2마리)
          "quest_004", // 드래곤 슬레이어 (JSON: 드래곤 1마리)

          // 사이드 퀘스트
          "quest_005", // 물약 수집가 (JSON: 체력 물약 2개)
          "quest_006", // 보물 사냥꾼 (JSON: 보물 상자 1개)
          "quest_007", // 던전 마스터 (JSON: 다양한 몬스터)
          "quest_008", // 성장하는 모험가1 (JSON: 레벨 3)
          "quest_009", // 성장하는 모험가2 (JSON: 레벨 5)
          "quest_010", // 성장하는 모험가3 (JSON: 레벨 8)
          "quest_011", // 성장하는 모험가4 (JSON: 레벨 11)
          "quest_012" // 성장하는 모험가5 (JSON: 레벨 15)
      );

      for (String questId : allQuestIds) {
        Quest quest = jsonQuestFactory.createQuest(questId);
        if (quest != null) {
          availableQuests.add(quest);
          logger.debug("퀘스트 생성 완료: {} - {}", questId, quest.getTitle());
        } else {
          logger.warn("퀘스트 생성 실패: {}", questId);
        }
      }

      logger.info("퀘스트 초기화 완료: {}개 퀘스트 생성", availableQuests.size());

    } catch (Exception e) {
      logger.error("퀘스트 초기화 실패", e);
      createFallbackQuests();
    }
  }


  /**
   * 플레이어 레벨에 맞는 동적 퀘스트 생성
   */
  public void generateLevelAppropriateQuests(Player player) {
    logger.info("플레이어 레벨 {}에 맞는 동적 퀘스트 생성 중...", player.getLevel());

    try {
      // 현재 레벨에 맞는 퀘스트가 부족한 경우에만 생성
      List<Quest> availableForPlayer = getAvailableQuests(player);

      if (availableForPlayer.size() < 3) { // 최소 3개의 퀘스트 유지
        Quest dynamicQuest = jsonQuestFactory.createLevelAppropriateQuest(player.getLevel());
        if (dynamicQuest != null) {
          availableQuests.add(dynamicQuest);
          logger.info("동적 퀘스트 생성: {} (레벨 {})", dynamicQuest.getTitle(), player.getLevel());
        }
      }

    } catch (Exception e) {
      logger.error("동적 퀘스트 생성 실패", e);
    }
  }

  /**
   * 레벨업 퀘스트 생성
   */
  private void createBasicLevelQuest() {
    Map<String, Integer> levelObjectives = new HashMap<>();
    levelObjectives.put("reach_level", 5);

    QuestReward levelReward = new QuestReward(100, 150);

    // GameItemFactory에서 물약들 가져오기
    GameItem healthPotion = itemFactory.createItem("HEALTH_POTION");
    GameItem manaPotion = itemFactory.createItem("MANA_POTION");

    if (healthPotion != null) {
      levelReward.addItemReward(healthPotion, 3);
    } else {
      // 폴백: GameEffectFactory로 생성
      GameItem fallbackHealth = createFallbackConsumableItem("HEALTH_POTION", "체력 물약", "HP를 50 회복", 50);
      levelReward.addItemReward(fallbackHealth, 3);
    }

    if (manaPotion != null) {
      levelReward.addItemReward(manaPotion, 2);
    } else {
      // 폴백: GameEffectFactory로 생성
      GameItem fallbackMana = createFallbackConsumableItem("MANA_POTION", "마나 물약", "MP를 40 회복", 40);
      levelReward.addItemReward(fallbackMana, 2);
    }

    Quest levelQuest = new Quest("quest_005", "성장하는 모험가", "레벨 5에 도달하세요.", Quest.QuestType.LEVEL, 1, levelObjectives, levelReward);

    availableQuests.add(levelQuest);
  }

  /**
   * 특별한 장비 생성
   */
  private GameEquipment createSpecialEquipment(String id, String name, String description, int value, ItemRarity rarity, GameEquipment.EquipmentType type, int attackBonus, int defenseBonus,
      int hpBonus) {
    try {
      return new GameEquipment(id, name, description, value, rarity, type, attackBonus, defenseBonus, hpBonus);
    } catch (Exception e) {
      logger.error("특별 장비 생성 실패: {}", name, e);
      // 기본 장비 반환
      return new GameEquipment(id, "기본 " + name, "기본 장비", value / 2, ItemRarity.COMMON, type, Math.max(1, attackBonus / 2), Math.max(1, defenseBonus / 2), Math.max(1, hpBonus / 2));
    }
  }

  /**
   * 특별한 물약 생성 (GameEffectFactory 사용)
   */
  private GameConsumable createSpecialPotion(String id, String name, String description, int value, ItemRarity rarity, List<GameEffect> effects) {
    try {
      return new GameConsumable(id, name, description, value, rarity, effects, 1); // 1턴 쿨다운
    } catch (Exception e) {
      logger.error("특별 물약 생성 실패: {}", name, e);
      // 기본 체력 물약으로 폴백
      return createFallbackConsumableItem("HEALTH_POTION", "체력 물약", "HP를 50 회복", 50);
    }
  }

  /**
   * 폴백용 소비 아이템 생성
   */
  private GameConsumable createFallbackConsumableItem(String id, String name, String description, int healAmount) {
    try {
      List<GameEffect> effects = List.of(GameEffectFactory.createHealHpEffect(healAmount));
      return new GameConsumable(id, name, description, healAmount, ItemRarity.COMMON, effects, 0);
    } catch (Exception e) {
      logger.error("폴백 아이템 생성 실패: {}", name, e);
      // 최후의 수단: 레거시 생성자 (올바른 시그니처)
      try {
        @SuppressWarnings("deprecation")
        GameConsumable fallback = new GameConsumable(id, name, description, healAmount, ItemRarity.COMMON, healAmount, 0, true);
        logger.warn("레거시 생성자로 폴백 아이템 생성: {}", name);
        return fallback;
      } catch (Exception fallbackException) {
        logger.error("레거시 생성자도 실패: {}", name, fallbackException);
        // 절대 null을 반환하지 않도록 최소한의 아이템 반환
        @SuppressWarnings("deprecation")
        GameConsumable emergency = new GameConsumable("EMERGENCY_POTION", "응급 물약", "최소한의 회복 효과", 1, ItemRarity.COMMON, 10, 0, true);
        return emergency;
      }
    }
  }


  // ==== 2. 기존 하드코딩 메서드들 제거 또는 폴백용으로 이동 ====
  private void createFallbackQuests() {
    logger.warn("폴백 퀘스트 생성 중...");

    // 최소한의 기본 퀘스트만 생성
    createBasicSlimeQuest();
    createBasicLevelQuest();

    logger.info("폴백 퀘스트 생성 완료: {}개", availableQuests.size());
  }

  private void createBasicSlimeQuest() {
    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("kill_slime", 3); // JSON과 일치하도록 수정

    QuestReward reward = new QuestReward(50, 100);

    // 체력 물약 2개 보상 (JSON과 일치)
    GameItem healthPotion = itemFactory.createItem("HEALTH_POTION");
    if (healthPotion != null) {
      reward.addItemReward(healthPotion, 2);
    }

    Quest quest = new Quest("quest_001", "슬라임 사냥꾼", "마을 근처의 슬라임 3마리를 처치하세요.", Quest.QuestType.KILL, 1, objectives, reward);

    availableQuests.add(quest);
  }

  // ==================== 기존 퀘스트 관리 메서드들 ====================

  /**
   * 레벨에 맞는 퀘스트만 반환
   */
  public List<Quest> getAvailableQuests(Player player) {
    List<Quest> levelAppropriate = new ArrayList<>();

    for (Quest quest : availableQuests) {
      // 이미 완료했거나 진행중인 퀘스트는 제외
      if (isQuestCompleted(quest.getId()) || isQuestActive(quest.getId())) {
        continue;
      }

      // 레벨 조건 확인
      if (quest.getRequiredLevel() <= player.getLevel()) {
        levelAppropriate.add(quest);
      }
    }

    return levelAppropriate;
  }


  // 헬퍼 메서드들
  private boolean isQuestCompleted(String questId) {
    return completedQuests.stream().anyMatch(q -> q.getId().equals(questId));
  }

  private boolean isQuestActive(String questId) {
    return activeQuests.stream().anyMatch(q -> q.getId().equals(questId));
  }

  /**
   * 퀘스트를 수락합니다.
   */
  public boolean acceptQuest(String questId, Player character) {
    Quest quest = findQuestById(questId, availableQuests);
    if (quest != null && quest.canAccept(character)) {
      if (quest.accept(character)) {
        availableQuests.remove(quest);
        activeQuests.add(quest);

        // 레벨 퀘스트의 경우 추가적으로 진행도 업데이트
        if (quest.getType() == Quest.QuestType.LEVEL) {
          updateLevelProgress(character);
          logger.debug("레벨 퀘스트 수락 후 진행도 업데이트: {} (현재 레벨: {})", quest.getTitle(), character.getLevel());
        }

        logger.info("퀘스트 수락: {} (캐릭터: {})", quest.getTitle(), character.getName());
        return true;
      }
    }
    return false;
  }

  /**
   * 모든 활성 레벨 퀘스트의 진행도를 현재 플레이어 레벨로 동기화 (새로운 메서드)
   */
  public void synchronizeLevelQuestProgress(Player player) {
    for (Quest quest : activeQuests) {
      if (quest.getType() == Quest.QuestType.LEVEL) {
        quest.initializeLevelProgress(player);
        logger.debug("레벨 퀘스트 진행도 동기화: {} -> 레벨 {}", quest.getTitle(), player.getLevel());
      }
    }
  }

  /**
   * 몬스터 처치 시 퀘스트 진행도 업데이트 - 수정된 버전
   */
  public void updateKillProgress(String monsterId) {
    // 정확한 몬스터 ID 사용
    String objectiveKey = "kill_" + monsterId; // 예: "kill_FOREST_SLIME"

    logger.debug("몬스터 처치 진행도 업데이트 시도: {} -> {}", monsterId, objectiveKey);

    boolean progressUpdated = false;
    for (Quest quest : new ArrayList<>(activeQuests)) {
      if (quest.getType() == Quest.QuestType.KILL) {
        logger.debug("퀘스트 {} 목표 확인: {}", quest.getId(), quest.getObjectives().keySet());

        if (quest.updateProgress(objectiveKey, 1)) {
          completeQuest(quest);
          progressUpdated = true;
          break;
        }
      }
    }

    if (!progressUpdated) {
      logger.warn("몬스터 {} 처치에 대한 퀘스트를 찾을 수 없음. 목표 키: {}", monsterId, objectiveKey);
      // 디버깅을 위해 현재 활성 퀘스트 목표들 출력
      debugActiveQuestObjectives();
    }
  }

  /**
   * 레벨업 시 퀘스트 진행도 업데이트 - 개선된 버전
   */
  public void updateLevelProgress(Player player) {
    String objectiveKey = "reach_level";
    int currentLevel = player.getLevel();

    logger.debug("레벨업 퀘스트 진행도 업데이트: 현재 레벨 {}", currentLevel);

    boolean anyQuestCompleted = false;

    // 활성 퀘스트 중 레벨 퀘스트 확인
    for (Quest quest : new ArrayList<>(activeQuests)) {
      if (quest.getType() == Quest.QuestType.LEVEL) {
        logger.debug("레벨 퀘스트 {} 확인: 목표 {}", quest.getId(), quest.getObjectives());

        // 현재 레벨로 진행도 업데이트 (퀘스트 내부에서 목표 레벨과 비교)
        if (quest.updateProgress(objectiveKey, currentLevel)) {
          completeQuest(quest);
          anyQuestCompleted = true;
          System.out.println("🎉 레벨업 퀘스트 완료: " + quest.getTitle());
          logger.info("레벨 퀘스트 완료: {} (레벨 {} 달성)", quest.getTitle(), currentLevel);
        }
      }
    }

    if (!anyQuestCompleted) {
      logger.debug("현재 레벨 {}에 해당하는 활성 레벨 퀘스트가 없음", currentLevel);
    }
  }

  /**
   * 아이템 수집 시 퀘스트 진행도 업데이트 - 수정된 버전
   */
  public void updateCollectionProgress(Player player, String itemId, int quantity) {
    // 정확한 아이템 ID 사용
    String objectiveKey = "collect_" + itemId; // 예: "collect_HEALTH_POTION"

    logger.debug("아이템 수집 진행도 업데이트 시도: {} x{} -> {}", itemId, quantity, objectiveKey);

    boolean progressUpdated = false;
    for (Quest quest : new ArrayList<>(activeQuests)) {
      if (quest.getType() == Quest.QuestType.COLLECT) {
        logger.debug("퀘스트 {} 목표 확인: {}", quest.getId(), quest.getObjectives().keySet());

        if (quest.updateProgress(objectiveKey, quantity)) {
          completeQuest(quest);
          progressUpdated = true;
          break;
        }
      }
    }

    if (!progressUpdated) {
      logger.warn("아이템 {} 수집에 대한 퀘스트를 찾을 수 없음. 목표 키: {}", itemId, objectiveKey);
      // 디버깅을 위해 현재 활성 퀘스트 목표들 출력
      debugActiveQuestObjectives();
    }
  }


  /**
   * 디버깅용 - 현재 활성 퀘스트의 목표들 출력
   */
  private void debugActiveQuestObjectives() {
    if (activeQuests.isEmpty()) {
      logger.debug("현재 활성 퀘스트가 없음");
      return;
    }

    logger.debug("=== 현재 활성 퀘스트 목표들 ===");
    for (Quest quest : activeQuests) {
      logger.debug("퀘스트 {}: {}", quest.getId(), quest.getObjectives().keySet());
    }
    logger.debug("===============================");
  }

  /**
   * 퀘스트 완료 처리
   */
  private void completeQuest(Quest quest) {
    activeQuests.remove(quest);
    completedQuests.add(quest);
    quest.setStatus(QuestStatus.COMPLETED);

    System.out.println("🎉 퀘스트 '" + quest.getTitle() + "'을(를) 완료했습니다!");
    System.out.println("🎁 보상: " + quest.getReward().getRewardDescription());

    logger.info("퀘스트 완료: {} ({})", quest.getTitle(), quest.getId());
  }

  // ==================== 동적 퀘스트 생성 시스템 ====================

  private void generateQuestForLevel(int level) {
    // JSON 템플릿에 없는 동적 퀘스트만 생성
    Quest dynamicQuest = jsonQuestFactory.createLevelAppropriateQuest(level);
    if (dynamicQuest != null) {
      availableQuests.add(dynamicQuest);
      logger.info("동적 퀘스트 생성: {} (레벨: {})", dynamicQuest.getTitle(), level);
    }
  }


  // ==================== 퀘스트 표시 메서드들 ====================

  /**
   * 활성 퀘스트 목록을 표시합니다.
   */
  public void displayActiveQuests() {
    System.out.println("\n=== 진행 중인 퀘스트 ===");
    if (activeQuests.isEmpty()) {
      System.out.println("진행 중인 퀘스트가 없습니다.");
    } else {
      for (int i = 0; i < activeQuests.size(); i++) {
        Quest quest = activeQuests.get(i);
        System.out.printf("%d. %s%n", i + 1, quest.getTitle());
        System.out.printf("   진행도: %s%n", quest.getProgressDescription());
        System.out.printf("   보상: %s%n", quest.getReward().getRewardDescription());
      }
    }
    System.out.println("===================");
  }

  /**
   * 완료된 퀘스트 목록을 표시합니다.
   */
  public void displayCompletedQuests() {
    System.out.println("\n=== 완료된 퀘스트 ===");
    if (completedQuests.isEmpty()) {
      System.out.println("완료된 퀘스트가 없습니다.");
    } else {
      for (int i = 0; i < completedQuests.size(); i++) {
        Quest quest = completedQuests.get(i);
        String status = quest.getStatus() == Quest.QuestStatus.COMPLETED ? " (보상 수령 대기)" : " (보상 수령 완료)";
        System.out.printf("%d. %s%s%n", i + 1, quest.getTitle(), status);
      }
    }
    System.out.println("==================");
  }

  /**
   * 수락 가능한 퀘스트 목록을 표시합니다.
   */
  public void displayAvailableQuests(Player character) {
    List<Quest> available = getAvailableQuests(character);
    System.out.println("\n=== 수락 가능한 퀘스트 ===");
    if (available.isEmpty()) {
      System.out.println("현재 수락할 수 있는 퀘스트가 없습니다.");
      if (character.getLevel() < 25) {
        System.out.println("💡 레벨을 올리면 새로운 퀘스트가 해금됩니다!");

        // 동적 퀘스트 생성 제안
        if (character.getLevel() >= 10 && character.getLevel() % 5 == 0) {
          generateQuestForLevel(character.getLevel());
          System.out.println("🎉 새로운 퀘스트가 생성되었습니다!");
        }
      }
    } else {
      for (int i = 0; i < available.size(); i++) {
        Quest quest = available.get(i);
        System.out.printf("%d. %s (필요 레벨: %d)%n", i + 1, quest.getTitle(), quest.getRequiredLevel());
        System.out.printf("   설명: %s%n", quest.getDescription());
        System.out.printf("   보상: %s%n", quest.getReward().getRewardDescription());

        // 팩토리 기반 아이템인지 표시
        if (quest.getReward().getItemRewards() != null && !quest.getReward().getItemRewards().isEmpty()) {
          System.out.println("   ✨ 특별 아이템 보상 포함!");
        }
      }
    }
    System.out.println("========================");
  }

  // ==================== 유틸리티 메서드들 ====================

  /**
   * ID로 퀘스트 찾기 (private 메서드가 이미 있는지 확인)
   */
  private Quest findQuestById(String questId, List<Quest> questList) {
    return questList.stream().filter(quest -> quest.getId().equals(questId)).findFirst().orElse(null);
  }

  /**
   * 인덱스로 수락 가능한 퀘스트를 반환합니다.
   */
  public Quest getAvailableQuestByIndex(int index, Player character) {
    List<Quest> available = getAvailableQuests(character);
    if (index >= 0 && index < available.size()) {
      return available.get(index);
    }
    return null;
  }

  /**
   * 인덱스로 완료된 퀘스트를 반환합니다 (보상 수령 가능한 것만).
   */
  public Quest getCompletedQuestByIndex(int index) {
    List<Quest> completed = completedQuests.stream().filter(quest -> quest.getStatus() == Quest.QuestStatus.COMPLETED).toList();
    if (index >= 0 && index < completed.size()) {
      return completed.get(index);
    }
    return null;
  }

  /**
   * 인덱스로 활성 퀘스트를 반환합니다.
   */
  public Quest getActiveQuestByIndex(int index) {
    if (index >= 0 && index < activeQuests.size()) {
      return activeQuests.get(index);
    }
    return null;
  }

  /**
   * 보상 수령 가능한 퀘스트 목록을 반환합니다.
   */
  public List<Quest> getClaimableQuests() {
    return completedQuests.stream().filter(quest -> quest.getStatus() == Quest.QuestStatus.COMPLETED).toList();
  }

  /**
   * 퀘스트 통계를 반환합니다.
   */
  public QuestStatistics getStatistics(Player character) {
    var available = getAvailableQuests(character);
    var claimable = getClaimableQuests();
    var claimed = completedQuests.stream().filter(quest -> quest.getStatus() == Quest.QuestStatus.CLAIMED).toList();

    return new QuestStatistics(available.size(), activeQuests.size(), claimable.size(), claimed.size());
  }

  // ==================== 고급 퀘스트 관리 기능 ====================

  /**
   * 특정 타입의 퀘스트 개수 반환
   */
  public int getQuestCountByType(Quest.QuestType type) {
    return availableQuests.stream().mapToInt(quest -> quest.getType() == type ? 1 : 0).sum();
  }

  /**
   * 플레이어의 퀘스트 완료률 계산
   */
  public double getCompletionRate(Player character) {
    int totalAccessible = getAvailableQuests(character).size() + activeQuests.size() + completedQuests.size();
    return totalAccessible > 0 ? ((double) completedQuests.size() / totalAccessible) * 100 : 0;
  }

  /**
   * 기존 generateDailyQuests 메서드 완전 교체
   */
  public void generateDailyQuests(Player character) {
    logger.info("개선된 일일 퀘스트 생성 중... (레벨: {})", character.getLevel());

    try {
      if (useImprovedDailyQuestSystem && dailyQuestManager != null) {
        // 🆕 개선된 시스템 사용
        List<Quest> newDailyQuests = dailyQuestManager.generateDailyQuestsForPlayer(character);

        // 기존 일일 퀘스트 정리
        cleanupOldDailyQuests();

        // 새로운 일일 퀘스트 추가
        for (Quest quest : newDailyQuests) {
          availableQuests.add(quest);
          logger.info("일일 퀘스트 추가: {}", quest.getTitle());
        }

        logger.info("개선된 일일 퀘스트 생성 완료: {}개", newDailyQuests.size());

      } else {
        // 기존 하드코딩 방식 사용 (폴백)
        logger.warn("개선된 시스템을 사용할 수 없어 기존 방식 사용");
        generateLegacyDailyQuests(character);
      }

    } catch (Exception e) {
      logger.error("일일 퀘스트 생성 실패", e);
      // 폴백: 기존 방식으로 생성
      generateLegacyDailyQuests(character);
    }
  }


  /**
   * 만료된 퀘스트 정리 (일일 퀘스트 등)
   */
  public void cleanupExpiredQuests() {
    logger.info("만료된 퀘스트 정리 시작...");

    // 활성 퀘스트에서 만료된 일일 퀘스트 찾기
    List<Quest> expiredQuests = new ArrayList<>();
    activeQuests.removeIf(quest -> {
      if (dailyQuestManager.isQuestExpired(quest)) {
        expiredQuests.add(quest);
        return true;
      }
      return false;
    });

    // 사용 가능한 퀘스트에서도 만료된 퀘스트 제거
    availableQuests.removeIf(quest -> dailyQuestManager.isQuestExpired(quest));

    // 만료된 퀘스트를 히스토리에 기록
    for (Quest expiredQuest : expiredQuests) {
      questHistoryManager.recordQuestExpiry(expiredQuest.getId(), "일일 리셋으로 인한 만료");
    }

    logger.info("만료된 퀘스트 정리 완료: {}개 만료됨", expiredQuests.size());
  }

  /**
   * 퀘스트 시스템 상태 출력
   */
  public void printQuestSystemStatus() {
    System.out.println("\n=== 🎯 퀘스트 시스템 상태 ===");
    System.out.println("📊 팩토리 상태:");
    System.out.printf("   GameItemFactory: %s (%d개 아이템)%n", itemFactory.isInitialized() ? "활성화" : "비활성화", itemFactory.getItemCount());

    System.out.println("\n📋 퀘스트 통계:");
    System.out.printf("   사용 가능: %d개%n", availableQuests.size());
    System.out.printf("   진행 중: %d개%n", activeQuests.size());
    System.out.printf("   완료됨: %d개%n", completedQuests.size());

    // 타입별 퀘스트 분포
    System.out.println("\n🎭 퀘스트 타입별 분포:");
    for (Quest.QuestType type : Quest.QuestType.values()) {
      long count = availableQuests.stream().filter(quest -> quest.getType() == type).count();
      if (count > 0) {
        System.out.printf("   %s: %d개%n", type.name(), count);
      }
    }

    System.out.println("========================");
  }

  /**
   * 퀘스트 보상 미리보기
   */
  public void previewQuestRewards(Player character) {
    List<Quest> available = getAvailableQuests(character);

    System.out.println("\n=== 🎁 퀘스트 보상 미리보기 ===");

    if (available.isEmpty()) {
      System.out.println("현재 수락 가능한 퀘스트가 없습니다.");
      return;
    }

    for (Quest quest : available) {
      System.out.printf("\n🎯 %s:%n", quest.getTitle());
      QuestReward reward = quest.getReward();

      if (reward.getGoldReward() > 0) {
        System.out.printf("   💰 골드: %d%n", reward.getGoldReward());
      }

      if (reward.getExpReward() > 0) {
        System.out.printf("   📈 경험치: %d%n", reward.getExpReward());
      }

      if (reward.getItemRewards() != null && !reward.getItemRewards().isEmpty()) {
        System.out.println("   🎁 아이템 보상:");
        reward.getItemRewards().forEach((item, quantity) -> {
          System.out.printf("     - %s x%d%n", item.getName(), quantity);
          if (item instanceof GameConsumable consumable) {
            System.out.printf("       ✨ %s%n", consumable.getEffectsDescription());
          }
        });
      }
    }

    System.out.println("==========================");
  }

  /**
   * 로드용 전용: 모든 퀘스트를 제거하고 로드된 데이터로 교체
   */
  public void replaceAllQuestsForLoad(List<Quest> newAvailable, List<Quest> newActive, List<Quest> newCompleted, List<String> newClaimedIds) {

    logger.debug("퀘스트 데이터 교체 시작");

    // 기존 데이터 완전 제거
    if (availableQuests != null) {
      availableQuests.clear();
    } else {
      availableQuests = new ArrayList<>();
    }

    if (activeQuests != null) {
      activeQuests.clear();
    } else {
      activeQuests = new ArrayList<>();
    }

    if (completedQuests != null) {
      completedQuests.clear();
    } else {
      completedQuests = new ArrayList<>();
    }

    if (claimedRewardIds != null) {
      claimedRewardIds.clear();
    } else {
      claimedRewardIds = new ArrayList<>();
    }

    // 새 데이터로 교체
    if (newAvailable != null) {
      availableQuests.addAll(newAvailable);
    }
    if (newActive != null) {
      activeQuests.addAll(newActive);
    }
    if (newCompleted != null) {
      completedQuests.addAll(newCompleted);
    }
    if (newClaimedIds != null) {
      claimedRewardIds.addAll(newClaimedIds);
    }


    logger.debug("퀘스트 데이터 교체 완료: 사용가능 {}개, 활성 {}개, 완료 {}개, 보상수령 {}개", availableQuests.size(), activeQuests.size(), completedQuests.size(), claimedRewardIds.size());

  }

  /**
   * 로드용 퀘스트 추가 메서드들
   */
  public void addToActiveQuests(Quest quest) {
    if (quest != null && !activeQuests.contains(quest)) {
      activeQuests.add(quest);
    }
  }

  public void addToCompletedQuests(Quest quest) {
    if (quest != null && !completedQuests.contains(quest)) {
      completedQuests.add(quest);
    }
  }

  public void clearAllQuests() {
    availableQuests.clear();
    activeQuests.clear();
    completedQuests.clear();
  }

  /**
   * 퀘스트 상태 설정 (로드용)
   */
  public void setQuestProgress(String questId, Map<String, Integer> progress) {
    Quest quest = findQuestById(questId, activeQuests);
    if (quest != null) {
      quest.setCurrentProgress(progress);
    }
  }


  /**
   * ⭐ 누락된 메서드 1: 보상 수령 상태 마킹
   */
  public void markRewardAsClaimed(String questId) {
    if (questId != null && !claimedRewardIds.contains(questId)) {
      claimedRewardIds.add(questId);
      logger.debug("퀘스트 보상 수령 상태 마킹: {}", questId);

      // 해당 퀘스트의 상태도 CLAIMED로 변경
      Quest quest = findQuestById(questId, completedQuests);
      if (quest != null) {
        quest.setStatus(Quest.QuestStatus.CLAIMED);
        logger.debug("퀘스트 상태 CLAIMED로 변경: {}", questId);
      }
    }
  }

  /**
   * ⭐ 누락된 메서드 2: 보상 수령한 퀘스트 ID 목록 반환
   */
  public List<String> getClaimedRewardIds() {
    return new ArrayList<>(claimedRewardIds);
  }

  /**
   * 보상 수령 여부 확인
   */
  public boolean isRewardClaimed(String questId) {
    return claimedRewardIds.contains(questId);
  }

  /**
   * SimpleSaveData 로드 시 보상 수령 상태 복원
   */
  public void setClaimedRewardIds(List<String> claimedIds) {
    this.claimedRewardIds = claimedIds != null ? new ArrayList<>(claimedIds) : new ArrayList<>();
    logger.debug("보상 수령 상태 복원: {}개", this.claimedRewardIds.size());
  }

  /**
   * 🔥 퀘스트 완료 처리 개선
   */
  public boolean completeQuest(String questId, Player character) {
    Quest quest = findQuestById(questId, activeQuests);
    if (quest != null && quest.isCompleted()) {
      // 기존 완료 처리
      activeQuests.remove(quest);
      completedQuests.add(quest);
      quest.setStatus(Quest.QuestStatus.COMPLETED);

      // 🆕 히스토리에 기록
      questHistoryManager.recordQuestCompletion(quest, false);

      logger.info("퀘스트 완료: {} (캐릭터: {})", quest.getTitle(), character.getName());
      return true;
    }
    return false;
  }

  /**
   * 🔥 퀘스트 보상 수령 처리 개선
   */
  public boolean claimQuestReward(String questId, Player character) {
    Quest quest = findQuestById(questId, completedQuests);
    if (quest != null && quest.getStatus() == Quest.QuestStatus.COMPLETED) {
      if (quest.claimReward(character)) {
        markRewardAsClaimed(questId);

        // 🆕 히스토리 업데이트
        questHistoryManager.recordQuestCompletion(quest, true);

        logger.info("퀘스트 보상 수령: {} (캐릭터: {})", quest.getTitle(), character.getName());
        return true;
      }
    }
    return false;
  }

  /**
   * 일일 퀘스트 강제 새로고침
   */
  public void refreshDailyQuests(Player character) {
    logger.info("일일 퀘스트 강제 새로고침...");
    cleanupExpiredQuests();
    generateDailyQuests(character);
    System.out.println("✅ 일일 퀘스트가 새로고침되었습니다!");
  }

  /**
   * 퀘스트 히스토리 표시
   */
  public void showQuestHistory(Player character) {
    questHistoryManager.displayQuestHistory(character);
  }

  /**
   * 일일 퀘스트 생성 통계 표시
   */
  public void showDailyQuestStats() {
    dailyQuestManager.printGenerationStats();
  }

  /**
   * 특정 플레이어를 위한 일일 퀘스트 시뮬레이션
   */
  public void simulateDailyQuests(Player character) {
    dailyQuestManager.simulateGenerationForPlayer(character);
  }

  // ==== 6. 유효성 검증 추가 ====
  public void validateQuestData() {
    logger.debug("퀘스트 데이터 검증 시작...");

    // 중복 퀘스트 제거
    removeDuplicateQuests();

    // 무효한 퀘스트 제거
    removeInvalidQuests();

    logger.debug("퀘스트 데이터 검증 완료");
  }

  private void removeDuplicateQuests() {
    // 각 리스트에서 중복 제거
    availableQuests = availableQuests.stream().collect(Collectors.toMap(Quest::getId, quest -> quest, (existing, replacement) -> existing)).values().stream().collect(Collectors.toList());

    // 다른 리스트들도 동일하게 처리
  }

  private void removeInvalidQuests() {
    // null 또는 무효한 퀘스트 제거
    availableQuests.removeIf(quest -> quest == null || quest.getId() == null);
    activeQuests.removeIf(quest -> quest == null || quest.getId() == null);
    completedQuests.removeIf(quest -> quest == null || quest.getId() == null);
  }
  // ==================== Getters ====================

  public List<Quest> getAvailableQuests() {
    return new ArrayList<>(availableQuests);
  }

  public List<Quest> getActiveQuests() {
    return new ArrayList<>(activeQuests);
  }

  public List<Quest> getCompletedQuests() {
    return new ArrayList<>(completedQuests);
  }

  public GameItemFactory getItemFactory() {
    return itemFactory;
  }

  /**
   * 퀘스트 통계 클래스
   */
  public static class QuestStatistics {
    private final int availableCount;
    private final int activeCount;
    private final int claimableCount;
    private final int claimedCount;

    public QuestStatistics(int availableCount, int activeCount, int claimableCount, int claimedCount) {
      this.availableCount = availableCount;
      this.activeCount = activeCount;
      this.claimableCount = claimableCount;
      this.claimedCount = claimedCount;
    }

    public int getAvailableCount() {
      return availableCount;
    }

    public int getActiveCount() {
      return activeCount;
    }

    public int getClaimableCount() {
      return claimableCount;
    }

    public int getClaimedCount() {
      return claimedCount;
    }

    public int getTotalCount() {
      return availableCount + activeCount + claimableCount + claimedCount;
    }

    public double getCompletionRate() {
      int total = getTotalCount();
      return total > 0 ? ((double) claimedCount / total) * 100 : 0;
    }

    @Override
    public String toString() {
      return String.format("QuestStatistics{available=%d, active=%d, claimable=%d, claimed=%d, completion=%.1f%%}", availableCount, activeCount, claimableCount, claimedCount, getCompletionRate());
    }
  }

  /**
   * 기존 QuestManager의 createDailyKillQuest 메서드 (하드코딩 버전 - 호환성용)
   */
  private void createDailyKillQuest(int playerLevel) {
    Map<String, Integer> objectives = new HashMap<>();

    String targetMonsterId = switch (playerLevel) {
      case 5, 6, 7 -> "FOREST_GOBLIN";
      case 8, 9, 10, 11, 12 -> "WILD_BOAR";           // 오크 → 멧돼지
      case 13, 14, 15, 16, 17 -> "CAVE_TROLL";        // 트롤
      case 18, 19, 20, 21, 22 -> "SKELETON_WARRIOR";  // 스켈레톤
      case 23, 24, 25, 26, 27 -> "FOREST_WOLF";       // 늑대
      default -> playerLevel <= 4 ? "FOREST_SLIME" : "FIRE_DRAGON";
  };

    int killCount = Math.max(3, playerLevel / 3);
    objectives.put("kill_" + targetMonsterId, killCount); // ✅ "kill_FOREST_SLIME"


    // 일일 퀘스트 보상 (레벨에 맞게 스케일링)
    GameItem dailyReward = itemFactory.createItem("HEALTH_POTION");
    if (dailyReward == null) {
      dailyReward = createFallbackConsumableItem("DAILY_POTION", "일일 보상 물약", "HP를 50 회복", 50);
    }

    QuestReward reward = new QuestReward(playerLevel * 10 + 50, // 경험치 (기본 50 + 레벨당 10)
        playerLevel * 5 + 30, // 골드 (기본 30 + 레벨당 5)
        dailyReward, Math.max(1, playerLevel / 5) // 아이템 수량
    );

    // 오늘 날짜 기반 ID 생성
    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    QuestTier tier = QuestTier.getTierForLevel(playerLevel);
    String questId = String.format("daily_kill_%s_%s01", today, tier.getCode());
    String displayName = getMonsterDisplayName(targetMonsterId);

    //@formatter:off
    Quest dailyQuest = new Quest(
        questId, 
        String.format("[%s] 일일 사냥 - %s", tier.getDescription(), displayName), 
        String.format("%s을(를) %d마리 처치하세요.", displayName, killCount),
        Quest.QuestType.KILL,
        Math.max(1, playerLevel - 2), // 최소 레벨 요구사항
        objectives, 
        reward
    );
    //@formatter:on
    availableQuests.add(dailyQuest);
    logger.info("일일 처치 퀘스트 생성: {} (레벨: {})", dailyQuest.getTitle(), playerLevel);
  }
  /**
   * 몬스터 ID -> 표시명 매핑
   */
  private String getMonsterDisplayName(String monsterId) {
      return switch (monsterId) {
          case "FOREST_SLIME" -> "숲 슬라임";
          case "FOREST_GOBLIN" -> "숲 고블린";
          case "FOREST_WOLF" -> "숲늑대";
          case "CAVE_BAT" -> "동굴 박쥐";
          case "WILD_BOAR" -> "멧돼지";
          case "FOREST_SPIDER" -> "숲 거미";
          case "CAVE_TROLL" -> "동굴 트롤";
          case "SKELETON_WARRIOR" -> "스켈레톤 전사";
          case "FIRE_DRAGON" -> "화염 드래곤";
          default -> monsterId; // 폴백: ID 그대로 반환
      };
  }
  /**
   * 기존 QuestManager의 createDailyCollectionQuest 메서드 (하드코딩 버전 - 호환성용)
   */
  private void createDailyCollectionQuest() {
    Map<String, Integer> objectives = new HashMap<>();

    // 기본 수집 아이템 (하드코딩)
    String[] collectableItemIds = {
        "HEALTH_POTION", "MANA_POTION", "IRON_ORE", 
        "HEALING_HERB", "LEATHER", "BONE"
    };
    String targetItemId = collectableItemIds[(int) (Math.random() * collectableItemIds.length)];
    int collectCount = 3 + (int) (Math.random() * 3); // 3-5개

    objectives.put("collect_" + targetItemId, collectCount); 

    // 특별 일일 보상
    List<GameEffect> dailyEffects = List.of(
        GameEffectFactory.createHealHpEffect(50), 
        GameEffectFactory.createGainExpEffect(150)
    );

    GameConsumable dailyPotion = createSpecialPotion(
        "DAILY_SPECIAL_POTION", "일일 특제 물약", 
        "하루 한 번 받을 수 있는 특별한 물약", 
        100, ItemRarity.UNCOMMON, dailyEffects
    );

    QuestReward reward = new QuestReward(100, 150, dailyPotion, 1);

    // 오늘 날짜 기반 ID 생성
    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String questId = String.format("daily_collect_%s_A01", today);
    String displayName = getItemDisplayName(targetItemId);
    
    //@formatter:off
    Quest dailyCollectionQuest = new Quest(
        questId, 
        String.format("[초급] 일일 수집 - %s", displayName), 
        String.format("%s을(를) %d개 수집하세요.", displayName, collectCount),
        Quest.QuestType.COLLECT, 
        5, // 최소 레벨 5
        objectives, 
        reward
    );
    //@formatter:on


    availableQuests.add(dailyCollectionQuest);
    logger.info("일일 수집 퀘스트 생성: {}", dailyCollectionQuest.getTitle());
  }

  /**
   * 아이템 ID -> 표시명 매핑
   */
  private String getItemDisplayName(String itemId) {
      return switch (itemId) {
          case "HEALTH_POTION" -> "체력 물약";
          case "MANA_POTION" -> "마나 물약";
          case "IRON_ORE" -> "철광석";
          case "HEALING_HERB" -> "치유 허브";
          case "LEATHER" -> "가죽";
          case "BONE" -> "뼈";
          case "SLIME_GEL" -> "슬라임 젤";
          case "WOLF_PELT" -> "늑대 가죽";
          case "BAT_WING" -> "박쥐 날개";
          default -> itemId;
      };
  }
  /**
   * 기존 방식의 일일 퀘스트 생성 (폴백용)
   */
  private void generateLegacyDailyQuests(Player character) {
    logger.info("기존 방식으로 일일 퀘스트 생성 중...");

    try {
      // 플레이어 레벨에 맞는 일일 퀘스트 생성
      if (character.getLevel() >= 5) {
        createDailyKillQuest(character.getLevel());
      }

      if (character.getLevel() >= 10) {
        createDailyCollectionQuest();
      }

      logger.info("기존 방식 일일 퀘스트 생성 완료");

    } catch (Exception e) {
      logger.error("기존 방식 일일 퀘스트 생성도 실패", e);
    }
  }

  /**
   * 오래된 일일 퀘스트 정리
   */
  private void cleanupOldDailyQuests() {
    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    int removedCount = 0;
    Iterator<Quest> iterator = availableQuests.iterator();
    while (iterator.hasNext()) {
      Quest quest = iterator.next();
      if (quest.getId().startsWith("daily_")) {
        String questDate = ImprovedDailyQuestManager.QuestIdParser.extractDate(quest.getId());
        if (questDate != null && !today.equals(questDate)) {
          iterator.remove();
          removedCount++;
        }
      }
    }

    if (removedCount > 0) {
      logger.info("오래된 일일 퀘스트 {}개 정리됨", removedCount);
    }
  }

  /**
   * 개선된 시스템 사용 여부 설정
   */
  public void setUseImprovedDailyQuestSystem(boolean useImproved) {
    this.useImprovedDailyQuestSystem = useImproved;
    logger.info("개선된 일일 퀘스트 시스템 사용: {}", useImproved);
  }

  /**
   * 일일 퀘스트 시스템 상태 출력
   */
  public void printDailyQuestSystemStatus() {
    System.out.println("\n=== 🌅 일일 퀘스트 시스템 상태 ===");
    System.out.printf("개선된 시스템 사용: %s\n", useImprovedDailyQuestSystem ? "✅ 활성화" : "❌ 비활성화");

    if (dailyQuestManager != null) {
      System.out.println("📊 개선된 시스템 통계:");
      dailyQuestManager.printGenerationStats();
    } else {
      System.out.println("❌ 개선된 시스템이 초기화되지 않음");
    }

    // 현재 일일 퀘스트 수
    long dailyQuestCount = availableQuests.stream().filter(quest -> quest.getId().startsWith("daily_")).count();
    System.out.printf("현재 사용 가능한 일일 퀘스트: %d개\n", dailyQuestCount);

    System.out.println("=".repeat(50));
  }

  /**
   * 커스텀 퀘스트 진행도 업데이트
   */
  public void updateCustomProgress(String objectiveKey, int amount) {
    for (Quest quest : new ArrayList<>(activeQuests)) {
      if (quest.updateProgress(objectiveKey, amount)) {
        completeQuest(quest);
        break;
      }
    }
  }

  /**
   * 지역 기반 퀘스트 확인
   */
  public void checkLocationBasedQuests(String locationName) {
    // TODO
  }

  /**
   * 상인 기반 퀘스트 확인
   */
  public void checkMerchantBasedQuests(String merchantName) {
    // TODO
  }
  
  /**
   * QuestManager에 추가할 새로운 메서드 - 플레이어 정보를 활용한 진행도 표시
   */
  public void displayActiveQuestsWithPlayer(Player player) {
    System.out.println("\n=== 진행 중인 퀘스트 ===");
    List<Quest> activeQuests = player.getQuestManager().getActiveQuests();
    if (activeQuests.isEmpty()) {
      System.out.println("진행 중인 퀘스트가 없습니다.");
    } else {
      for (int i = 0; i < activeQuests.size(); i++) {
        Quest quest = activeQuests.get(i);
        System.out.printf("%d. %s%n", i + 1, quest.getTitle());
        
        // 플레이어 정보를 활용한 정확한 진행도 표시
        if (player != null && quest.getType() == Quest.QuestType.LEVEL) {
          System.out.printf("   진행도: %s%n", quest.getProgressDescription(player));
        } else {
          System.out.printf("   진행도: %s%n", quest.getProgressDescription());
        }
        
        System.out.printf("   보상: %s%n", quest.getReward().getRewardDescription());
      }
    }
    System.out.println("===================");
  }
}
