package rpg.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import rpg.application.factory.GameEffectFactory;
import rpg.application.factory.GameItemFactory;
import rpg.application.factory.GameQuestFactory;
import rpg.domain.item.GameConsumable;
import rpg.domain.item.GameEquipment;
import rpg.domain.item.GameItem;
import rpg.domain.item.ItemRarity;
import rpg.domain.item.effect.GameEffect;
import rpg.domain.player.Player;
import rpg.domain.quest.Quest;
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

  // 팩토리 인스턴스
  private final GameItemFactory itemFactory;

  private final GameQuestFactory gameQuestFactory;
  @JsonCreator
  public QuestManager() {
    this.gameQuestFactory = GameQuestFactory.getInstance();
    this.itemFactory = GameItemFactory.getInstance();
    this.availableQuests = new ArrayList<>();
    this.activeQuests = new ArrayList<>();
    this.completedQuests = new ArrayList<>();
    initializeQuests();
    logger.info("QuestManager 초기화 완료 (GameItemFactory 통합)");
  }

  // 로드 전용 생성자 (정적 팩토리 메서드)
  public static QuestManager createForLoading() {
    QuestManager questManager = new QuestManager();
    // 기본 퀘스트들을 제거 (로드된 데이터로 교체될 예정)
    questManager.availableQuests.clear();
    questManager.activeQuests.clear();
    questManager.completedQuests.clear();

    logger.info("로드용 QuestManager 생성 완료 (기본 퀘스트 제거됨)");
    return questManager;
  }

  /**
   * 기본 퀘스트들을 초기화합니다.
   */
  private void initializeQuests() {
    logger.info("퀘스트 초기화 중... (팩토리 기반 보상 시스템)");

    try {
      // 기본 퀘스트들을 팩토리에서 생성
      List<String> basicQuestIds = List.of(
          "quest_001", // 슬라임 사냥꾼
          "quest_002", // 고블린 소탕
          "quest_003", // 오크 토벌
          "quest_004", // 드래곤 슬레이어
          "quest_005", // 성장하는 모험가
          "quest_006"  // 물약 수집가
      );

      for (String questId : basicQuestIds) {
          Quest quest = gameQuestFactory.createQuest(questId);
          if (quest != null) {
              availableQuests.add(quest);
              logger.debug("퀘스트 생성 완료: {} - {}", questId, quest.getTitle());
          } else {
              logger.warn("퀘스트 생성 실패: {}", questId);
          }
      }

      // 일일 퀘스트 몇 개 추가
      addDailyQuests();

      logger.info("퀘스트 초기화 완료: {}개 퀘스트 생성", availableQuests.size());

    } catch (Exception e) {
      logger.error("퀘스트 초기화 실패", e);
      createFallbackQuests();
    }
  }
  /**
   * 일일 퀘스트 추가
   */
  private void addDailyQuests() {
      try {
          // 일일 사냥 퀘스트
          Quest dailyKillQuest = gameQuestFactory.createDailyQuest(Quest.QuestType.KILL);
          if (dailyKillQuest != null) {
              availableQuests.add(dailyKillQuest);
              logger.debug("일일 사냥 퀘스트 생성: {}", dailyKillQuest.getTitle());
          }

          // 일일 수집 퀘스트
          Quest dailyCollectQuest = gameQuestFactory.createDailyQuest(Quest.QuestType.COLLECT);
          if (dailyCollectQuest != null) {
              availableQuests.add(dailyCollectQuest);
              logger.debug("일일 수집 퀘스트 생성: {}", dailyCollectQuest.getTitle());
          }

      } catch (Exception e) {
          logger.warn("일일 퀘스트 생성 실패", e);
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
              Quest dynamicQuest = gameQuestFactory.createLevelAppropriateQuest(player.getLevel());
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
   * 기본 퀘스트만 초기화 (중복 방지용)
   */
  private void initializeDefaultQuestsOnly() {
    logger.info("기본 퀘스트만 초기화 중...");

    try {
      // 최소한의 기본 퀘스트만 생성
      createSlimeQuest();
      createLevelQuest();

      logger.info("기본 퀘스트 초기화 완료: {}개 퀘스트", availableQuests.size());

    } catch (Exception e) {
      logger.error("기본 퀘스트 초기화 실패", e);
      createFallbackQuests();
    }
  }


  /**
   * 슬라임 사냥 퀘스트 생성
   */
  private void createSlimeQuest() {
    Map<String, Integer> slimeObjectives = new HashMap<>();
    slimeObjectives.put("kill_슬라임", 5);

    // GameItemFactory에서 체력 물약 가져오기
    GameItem healthPotion = itemFactory.createItem("HEALTH_POTION");

    QuestReward slimeReward;
    if (healthPotion != null) {
      slimeReward = new QuestReward(50, 100, healthPotion, 2);
      logger.debug("슬라임 퀘스트 보상: 팩토리에서 체력 물약 생성");
    } else {
      // 팩토리에서 실패 시 GameEffectFactory로 생성
      slimeReward = createFallbackConsumableReward("HEALTH_POTION", "체력 물약", "HP를 50 회복", 50, 2);
      logger.debug("슬라임 퀘스트 보상: GameEffectFactory로 폴백 생성");
    }

    Quest slimeQuest = new Quest("quest_001", "슬라임 사냥꾼", "마을 근처의 슬라임 5마리를 처치하세요.", Quest.QuestType.KILL, 1, slimeObjectives, slimeReward);

    availableQuests.add(slimeQuest);
  }

  /**
   * 고블린 소탕 퀘스트 생성
   */
  private void createGoblinQuest() {
    Map<String, Integer> goblinObjectives = new HashMap<>();
    goblinObjectives.put("kill_고블린", 3);

    // 철검 보상 (GameEffectFactory 기반 또는 직접 생성)
    GameEquipment ironSword = createSpecialEquipment("MAGIC_IRON_SWORD", "마법 철검", "슬라임을 처치하며 단련된 마법의 철검", 100, ItemRarity.UNCOMMON,
        GameEquipment.EquipmentType.WEAPON, 15, 0, 0);

    QuestReward goblinReward = new QuestReward(100, 200, ironSword, 1);

    Quest goblinQuest = new Quest("quest_002", "고블린 소탕", "위험한 고블린 3마리를 처치하세요.", Quest.QuestType.KILL, 3, goblinObjectives, goblinReward);

    availableQuests.add(goblinQuest);
  }

  /**
   * 오크 토벌 퀘스트 생성
   */
  private void createOrcQuest() {
    Map<String, Integer> orcObjectives = new HashMap<>();
    orcObjectives.put("kill_오크", 2);

    // 판금 갑옷 보상
    GameEquipment plateArmor = createSpecialEquipment("RARE_PLATE_ARMOR", "용사의 판금 갑옷", "오크와 싸우기 위해 특별히 제작된 갑옷", 200, ItemRarity.RARE,
        GameEquipment.EquipmentType.ARMOR, 0, 20, 50);

    QuestReward orcReward = new QuestReward(200, 500, plateArmor, 1);

    Quest orcQuest = new Quest("quest_003", "오크 토벌", "강력한 오크 2마리를 처치하세요.", Quest.QuestType.KILL, 5, orcObjectives, orcReward);

    availableQuests.add(orcQuest);
  }

  /**
   * 드래곤 슬레이어 퀘스트 생성
   */
  private void createDragonQuest() {
    Map<String, Integer> dragonObjectives = new HashMap<>();
    dragonObjectives.put("kill_드래곤", 1);

    // 전설의 드래곤 반지
    GameEquipment legendaryRing = createSpecialEquipment("DRAGON_HEART_RING", "드래곤 하트 링", "드래곤의 심장으로 만든 전설적인 반지", 1000, ItemRarity.LEGENDARY,
        GameEquipment.EquipmentType.ACCESSORY, 30, 15, 100);

    QuestReward dragonReward = new QuestReward(1000, 2000, legendaryRing, 1);

    Quest dragonQuest = new Quest("quest_004", "드래곤 슬레이어", "전설의 드래곤을 처치하고 영웅이 되세요!", Quest.QuestType.KILL, 8, dragonObjectives, dragonReward);

    availableQuests.add(dragonQuest);
  }

  /**
   * 레벨업 퀘스트 생성
   */
  private void createLevelQuest() {
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
   * 수집 퀘스트 생성 (새로운 퀘스트 타입)
   */
  private void createCollectionQuest() {
    Map<String, Integer> collectionObjectives = new HashMap<>();
    collectionObjectives.put("collect_체력 물약", 5);

    // 특별 보상: 복합 효과 물약
    GameConsumable specialPotion = createSpecialPotion("ADVENTURER_POTION", "모험가의 물약", "HP와 MP를 동시에 회복하고 약간의 경험치를 얻는 특별한 물약", 150, ItemRarity.RARE,
        List.of(GameEffectFactory.createHealHpEffect(100), GameEffectFactory.createHealMpEffect(100), GameEffectFactory.createGainExpEffect(200)));

    QuestReward collectionReward = new QuestReward(150, 100, specialPotion, 1);

    Quest collectionQuest = new Quest("quest_006", "물약 수집가", "체력 물약 5개를 수집하세요.", Quest.QuestType.COLLECT, 3, collectionObjectives, collectionReward);

    availableQuests.add(collectionQuest);
  }

  /**
   * 특별한 장비 생성
   */
  private GameEquipment createSpecialEquipment(String id, String name, String description, int value, ItemRarity rarity,
      GameEquipment.EquipmentType type, int attackBonus, int defenseBonus, int hpBonus) {
    try {
      return new GameEquipment(id, name, description, value, rarity, type, attackBonus, defenseBonus, hpBonus);
    } catch (Exception e) {
      logger.error("특별 장비 생성 실패: {}", name, e);
      // 기본 장비 반환
      return new GameEquipment(id, "기본 " + name, "기본 장비", value / 2, ItemRarity.COMMON, type, Math.max(1, attackBonus / 2),
          Math.max(1, defenseBonus / 2), Math.max(1, hpBonus / 2));
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

  /**
   * 폴백용 보상 생성
   */
  private QuestReward createFallbackConsumableReward(String id, String name, String description, int healAmount, int quantity) {
    GameConsumable item = createFallbackConsumableItem(id, name, description, healAmount);
    return new QuestReward(50, 100, item, quantity);
  }

  /**
   * 폴백 퀘스트들 생성 (초기화 실패 시)
   */
  private void createFallbackQuests() {
    logger.warn("폴백 퀘스트 생성 중...");

    try {
      // 간단한 기본 퀘스트
      Map<String, Integer> basicObjectives = new HashMap<>();
      basicObjectives.put("kill_슬라임", 3);

      GameConsumable basicPotion = createFallbackConsumableItem("SMALL_HEALTH_POTION", "작은 체력 물약", "HP를 30 회복", 30);
      QuestReward basicReward = new QuestReward(25, 50, basicPotion, 1);

      Quest basicQuest = new Quest("quest_basic", "기본 퀘스트", "슬라임 3마리를 처치하세요.", Quest.QuestType.KILL, 1, basicObjectives, basicReward);

      availableQuests.add(basicQuest);
      logger.info("폴백 퀘스트 1개 생성");

    } catch (Exception e) {
      logger.error("폴백 퀘스트 생성도 실패", e);
      logger.warn("퀘스트 없이 시작합니다.");
    }
  }

  // ==================== 기존 퀘스트 관리 메서드들 ====================

  /**
   * 캐릭터가 수락할 수 있는 퀘스트 목록을 반환합니다.
   */
  public List<Quest> getAvailableQuests(Player character) {
    return availableQuests.stream().filter(quest -> quest.getRequiredLevel() <= character.getLevel())
        .filter(quest -> quest.getStatus() == Quest.QuestStatus.AVAILABLE).toList();
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
        logger.info("퀘스트 수락: {} (캐릭터: {})", quest.getTitle(), character.getName());
        return true;
      }
    }
    return false;
  }

  /**
   * 몬스터 처치 시 관련 퀘스트 진행도를 업데이트합니다.
   */
  public void updateKillProgress(String monsterName) {
    String objectiveKey = "kill_" + monsterName;

    for (Quest quest : new ArrayList<>(activeQuests)) {
      if (quest.getType() == Quest.QuestType.KILL && quest.updateProgress(objectiveKey, 1)) {
        completeQuest(quest);
        break;
      }
    }
  }

  /**
   * 레벨업 퀘스트 진행도를 업데이트합니다.
   */
  public void updateLevelProgress(Player character) {
    for (Quest quest : new ArrayList<>(activeQuests)) {
      if (quest.getType() == Quest.QuestType.LEVEL) {
        if (quest.updateProgress("reach_level", character.getLevel())) {
          completeQuest(quest);
          break;
        }
      }
    }
  }

  /**
   * 아이템 수집 퀘스트 진행도를 업데이트합니다.
   */
  public void updateCollectionProgress(Player character, String itemName, int quantity) {
    String objectiveKey = "collect_" + itemName;

    for (Quest quest : new ArrayList<>(activeQuests)) {
      if (quest.getType() == Quest.QuestType.COLLECT && quest.updateProgress(objectiveKey, quantity)) {
        completeQuest(quest);
        break;
      }
    }
  }

  /**
   * 퀘스트 완료 처리
   */
  private void completeQuest(Quest quest) {
    activeQuests.remove(quest);
    completedQuests.add(quest);
    System.out.println("🎉 퀘스트 '" + quest.getTitle() + "'을(를) 완료했습니다!");
    logger.info("퀘스트 완료: {}", quest.getTitle());
  }

  /**
   * 완료된 퀘스트의 보상을 수령합니다.
   */
  public boolean claimQuestReward(String questId, Player character) {
    Quest quest = findQuestById(questId, completedQuests);
    if (quest != null && quest.getStatus() == Quest.QuestStatus.COMPLETED) {
      if (quest.claimReward(character)) {
        logger.info("퀘스트 보상 수령: {} (캐릭터: {})", quest.getTitle(), character.getName());
        return true;
      }
    }
    return false;
  }

  // ==================== 동적 퀘스트 생성 시스템 ====================

  /**
   * 플레이어 레벨에 맞는 새로운 퀘스트 생성
   */
  public void generateQuestForLevel(int level) {
    logger.info("레벨 {}에 맞는 퀘스트 생성 중...", level);

    try {
      if (level == 10) {
        createAdvancedCollectionQuest();
      } else if (level == 15) {
        createEliteMonsterQuest();
      } else if (level >= 20) {
        createEndgameQuest(level);
      }
    } catch (Exception e) {
      logger.error("동적 퀘스트 생성 실패 (레벨: {})", level, e);
    }
  }

  /**
   * 고급 수집 퀘스트 생성
   */
  private void createAdvancedCollectionQuest() {
    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("collect_큰 체력 물약", 3);
    objectives.put("collect_마나 물약", 5);

    // 특별 보상: 경험치 증가 물약
    List<GameEffect> expPotionEffects = List.of(GameEffectFactory.createGainExpEffect(100), GameEffectFactory.createHealHpEffect(50));

    GameConsumable expPotion = createSpecialPotion("EXPERIENCE_POTION", "경험의 영약", "경험치를 대량으로 획득하는 특별한 물약", 200, ItemRarity.EPIC, expPotionEffects);

    QuestReward reward = new QuestReward(300, 200, expPotion, 2);

    Quest advancedQuest =
        new Quest("quest_advanced_collection", "고급 연금술사", "다양한 물약을 수집하여 연금술 실력을 증명하세요.", Quest.QuestType.COLLECT, 10, objectives, reward);

    availableQuests.add(advancedQuest);
    logger.info("고급 수집 퀘스트 생성: {}", advancedQuest.getTitle());
  }

  /**
   * 엘리트 몬스터 퀘스트 생성
   */
  private void createEliteMonsterQuest() {
    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("kill_트롤", 1);
    objectives.put("kill_스켈레톤", 3);

    GameEquipment eliteWeapon = createSpecialEquipment("ELITE_KILLER", "엘리트 킬러", "엘리트 몬스터를 사냥하기 위한 특수 무기", 400, ItemRarity.EPIC,
        GameEquipment.EquipmentType.WEAPON, 25, 5, 20);

    QuestReward reward = new QuestReward(500, 800, eliteWeapon, 1);

    Quest eliteQuest = new Quest("quest_elite_hunter", "엘리트 헌터", "강력한 엘리트 몬스터들을 처치하세요.", Quest.QuestType.KILL, 15, objectives, reward);

    availableQuests.add(eliteQuest);
    logger.info("엘리트 몬스터 퀘스트 생성: {}", eliteQuest.getTitle());
  }

  /**
   * 엔드게임 퀘스트 생성
   */
  private void createEndgameQuest(int level) {
    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("kill_드래곤", 1);
    objectives.put("reach_level", level + 5);

    // 최고급 보상들
    QuestReward reward = new QuestReward(1000, 2000);

    // 여러 아이템 보상
    GameEquipment ultimateWeapon = createSpecialEquipment("DRAGON_SLAYER", "드래곤 슬레이어", "궁극의 드래곤 처치용 무기", 2000, ItemRarity.LEGENDARY,
        GameEquipment.EquipmentType.WEAPON, 50, 10, 50);

    reward.addItemReward(ultimateWeapon, 1);

    Quest endgameQuest = new Quest("quest_endgame_" + level, "궁극의 도전", "드래곤을 처치하고 더 높은 레벨에 도달하세요.", Quest.QuestType.KILL, level, objectives, reward);

    availableQuests.add(endgameQuest);
    logger.info("엔드게임 퀘스트 생성: {} (레벨: {})", endgameQuest.getTitle(), level);
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
   * ID로 퀘스트를 찾습니다.
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
   * 일일 퀘스트 생성 (확장 기능)
   */
  public void generateDailyQuests(Player character) {
    logger.info("일일 퀘스트 생성 중... (레벨: {})", character.getLevel());

    try {
      // 플레이어 레벨에 맞는 일일 퀘스트 생성
      if (character.getLevel() >= 5) {
        createDailyKillQuest(character.getLevel());
      }

      if (character.getLevel() >= 10) {
        createDailyCollectionQuest();
      }

    } catch (Exception e) {
      logger.error("일일 퀘스트 생성 실패", e);
    }
  }

  /**
   * 일일 처치 퀘스트 생성
   */
  private void createDailyKillQuest(int playerLevel) {
    Map<String, Integer> objectives = new HashMap<>();

    // 레벨에 따른 적절한 몬스터 선택
    String targetMonster = switch (playerLevel) {
      case 5, 6, 7 -> "고블린";
      case 8, 9, 10, 11, 12 -> "오크";
      case 13, 14, 15, 16, 17 -> "트롤";
      default -> "슬라임";
    };

    int killCount = Math.max(3, playerLevel / 3);
    objectives.put("kill_" + targetMonster, killCount);

    // 일일 퀘스트 보상 (적당한 수준)
    GameItem dailyReward = itemFactory.createItem("HEALTH_POTION");
    if (dailyReward == null) {
      dailyReward = createFallbackConsumableItem("DAILY_POTION", "일일 보상 물약", "HP를 40 회복", 40);
    }

    QuestReward reward = new QuestReward(playerLevel * 10, // 골드
        playerLevel * 15, // 경험치
        dailyReward, Math.max(1, playerLevel / 5) // 수량
    );

    Quest dailyQuest = new Quest("daily_kill_" + System.currentTimeMillis(), // 고유 ID
        "일일 사냥 - " + targetMonster, targetMonster + " " + killCount + "마리를 처치하세요.", Quest.QuestType.KILL, Math.max(1, playerLevel - 2), // 최소 레벨 요구사항
        objectives, reward);

    availableQuests.add(dailyQuest);
    logger.info("일일 처치 퀘스트 생성: {} (레벨: {})", dailyQuest.getTitle(), playerLevel);
  }

  /**
   * 일일 수집 퀘스트 생성
   */
  private void createDailyCollectionQuest() {
    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("collect_체력 물약", 3);

    // 특별 일일 보상
    List<GameEffect> dailyEffects = List.of(GameEffectFactory.createHealHpEffect(60), GameEffectFactory.createGainExpEffect(30));

    GameConsumable dailyPotion =
        createSpecialPotion("DAILY_SPECIAL_POTION", "일일 특제 물약", "하루 한 번 받을 수 있는 특별한 물약", 100, ItemRarity.UNCOMMON, dailyEffects);

    QuestReward reward = new QuestReward(100, 150, dailyPotion, 1);

    Quest dailyCollectionQuest = new Quest("daily_collection_" + System.currentTimeMillis(), "일일 수집 - 물약", "체력 물약 3개를 수집하세요.",
        Quest.QuestType.COLLECT, 10, objectives, reward);

    availableQuests.add(dailyCollectionQuest);
    logger.info("일일 수집 퀘스트 생성: {}", dailyCollectionQuest.getTitle());
  }

  /**
   * 만료된 퀘스트 정리 (일일 퀘스트 등)
   */
  public void cleanupExpiredQuests() {
    // 일일 퀘스트 ID 패턴으로 식별하여 제거
    availableQuests.removeIf(quest -> quest.getId().startsWith("daily_") && isQuestExpired(quest));

    logger.debug("만료된 퀘스트 정리 완료");
  }

  /**
   * 퀘스트 만료 여부 확인 (단순 구현)
   */
  private boolean isQuestExpired(Quest quest) {
    // 실제 구현에서는 생성 시간을 추적해야 함
    // 현재는 단순히 일일 퀘스트만 체크
    return quest.getId().startsWith("daily_");
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
   * 퀘스트 데이터 검증 (로드 후 호출)
   */
  public void validateQuestData() {
    logger.info("퀘스트 데이터 검증 시작");

    // null 체크 및 초기화
    if (availableQuests == null) {
      availableQuests = new ArrayList<>();
    }
    if (activeQuests == null) {
      activeQuests = new ArrayList<>();
    }
    if (completedQuests == null) {
      completedQuests = new ArrayList<>();
    }

    // 잘못된 상태의 퀘스트 정리
    activeQuests.removeIf(quest -> quest == null || quest.getStatus() != Quest.QuestStatus.ACTIVE);
    completedQuests.removeIf(quest -> quest == null);

    // 🔥 로드된 데이터가 비어있으면 기본 퀘스트만 추가 (중복 방지)
    if (availableQuests.isEmpty() && activeQuests.isEmpty() && completedQuests.isEmpty()) {
      logger.info("빈 퀘스트 데이터 감지 - 기본 퀘스트 추가");
      initializeDefaultQuestsOnly();
    }

    logger.info("퀘스트 데이터 검증 완료: 사용가능 {}개, 활성 {}개, 완료 {}개", availableQuests.size(), activeQuests.size(), completedQuests.size());
  }

  /**
   * 🔥 로드용 전용: 모든 퀘스트를 제거하고 로드된 데이터로 교체
   */
  public void replaceAllQuestsForLoad(List<Quest> newAvailable, List<Quest> newActive, List<Quest> newCompleted) {
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

    // 로드된 데이터로 교체
    if (newAvailable != null) {
      availableQuests.addAll(newAvailable);
    }
    if (newActive != null) {
      activeQuests.addAll(newActive);
    }
    if (newCompleted != null) {
      completedQuests.addAll(newCompleted);
    }

    logger.debug("퀘스트 데이터 교체 완료: 사용가능 {}개, 활성 {}개, 완료 {}개", availableQuests.size(), activeQuests.size(), completedQuests.size());
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
      return String.format("QuestStatistics{available=%d, active=%d, claimable=%d, claimed=%d, completion=%.1f%%}", availableCount, activeCount,
          claimableCount, claimedCount, getCompletionRate());
    }


  }
}
