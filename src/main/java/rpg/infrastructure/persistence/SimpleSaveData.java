package rpg.infrastructure.persistence;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rpg.application.factory.GameItemFactory;
import rpg.application.factory.JsonBasedQuestFactory;
import rpg.application.service.QuestManager;
import rpg.application.service.SkillService;
import rpg.core.engine.GameState;
import rpg.domain.inventory.ItemStack;
import rpg.domain.inventory.PlayerInventory;
import rpg.domain.item.GameEquipment;
import rpg.domain.item.GameItem;
import rpg.domain.player.Player;
import rpg.domain.player.PlayerStatusCondition;
import rpg.domain.quest.Quest;
import rpg.shared.constant.SystemConstants;

/**
 * 최적화된 저장 데이터 클래스 - Factory 패턴 완전 활용 - ID 기반 저장으로 파일 크기 최소화 - 템플릿과 상태 데이터 분리 - 안전한 복원 메커니즘
 */
public class SimpleSaveData {
  private static final Logger logger = LoggerFactory.getLogger(SimpleSaveData.class);

  // === 메타데이터 ===
  private final String version;
  private final String saveTime;
  private final int slotNumber;

  // === 플레이어 핵심 데이터 ===
  private final String playerName;
  private final int level;
  private final int experience;
  private final int hp;
  private final int maxHp;
  private final int mana;
  private final int maxMana;
  private final int attack;
  private final int defense;
  private final int gold;
  private final double restoreHp;
  private final double restoreMp;
  private final String statusCondition;

  // === 인벤토리 (ID + 수량만) ===
  private final List<ItemEntry> items;
  private final EquipmentSlots equipped;
  private final int maxSlots;

  // === 퀘스트 (ID + 진행도만) ===
  private final List<QuestProgress> activeQuests;
  private final List<String> completedQuestIds;
  private final List<String> claimedRewardIds;

  // === 스킬 (ID만) ===
  private final List<String> learnedSkillIds;
  private final Map<String, Integer> skillCooldowns;

  // === 게임 상태 ===
  private final int totalPlayTime;
  private final int monstersKilled;
  private final int questsCompleted;
  private final String currentLocation;

  @JsonCreator
  public SimpleSaveData(
//@formatter:off
  @JsonProperty("version") String version
, @JsonProperty("saveTime") String saveTime
, @JsonProperty("slotNumber") int slotNumber
, @JsonProperty("playerName") String playerName
, @JsonProperty("level") int level
, @JsonProperty("experience") int experience
, @JsonProperty("hp") int hp
, @JsonProperty("maxHp") int maxHp
, @JsonProperty("mana") int mana
, @JsonProperty("maxMana") int maxMana
, @JsonProperty("attack") int attack
, @JsonProperty("defense") int defense
, @JsonProperty("gold") int gold
, @JsonProperty("restoreHp") double restoreHp
, @JsonProperty("restoreMp") double restoreMp
, @JsonProperty("statusCondition") String statusCondition
, @JsonProperty("items") List<ItemEntry> items
, @JsonProperty("equipped") EquipmentSlots equipped
, @JsonProperty("maxSlots") int maxSlots
, @JsonProperty("activeQuests") List<QuestProgress> activeQuests
, @JsonProperty("completedQuestIds") List<String> completedQuestIds
, @JsonProperty("claimedRewardIds") List<String> claimedRewardIds
, @JsonProperty("learnedSkillIds") List<String> learnedSkillIds
, @JsonProperty("skillCooldowns") Map<String, Integer> skillCooldowns
, @JsonProperty("totalPlayTime") int totalPlayTime
, @JsonProperty("monstersKilled") int monstersKilled
, @JsonProperty("questsCompleted") int questsCompleted
, @JsonProperty("currentLocation") String currentLocation
//@formatter:on
  ) {
    this.version = version;
    this.saveTime = saveTime;
    this.slotNumber = slotNumber;
    this.playerName = playerName;
    this.level = level;
    this.experience = experience;
    this.hp = hp;
    this.maxHp = maxHp;
    this.mana = mana;
    this.maxMana = maxMana;
    this.attack = attack;
    this.defense = defense;
    this.gold = gold;
    this.restoreHp = restoreHp;
    this.restoreMp = restoreMp;
    this.statusCondition = statusCondition;
    this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    this.equipped = equipped;
    this.maxSlots = maxSlots;
    this.activeQuests = activeQuests != null ? new ArrayList<>(activeQuests) : new ArrayList<>();
    this.completedQuestIds = completedQuestIds != null ? new ArrayList<>(completedQuestIds) : new ArrayList<>();
    this.claimedRewardIds = claimedRewardIds != null ? new ArrayList<>(claimedRewardIds) : new ArrayList<>();
    this.learnedSkillIds = learnedSkillIds != null ? new ArrayList<>(learnedSkillIds) : new ArrayList<>();
    this.skillCooldowns = skillCooldowns != null ? new HashMap<>(skillCooldowns) : new HashMap<>();
    this.totalPlayTime = totalPlayTime;
    this.monstersKilled = monstersKilled;
    this.questsCompleted = questsCompleted;
    this.currentLocation = currentLocation;
  }

  // === ⭐ Factory 패턴을 활용한 Player 생성 메서드 ===

  /**
   * Player와 GameState를 SimpleSaveData로 변환 (저장용)
   */
  public static SimpleSaveData from(Player player, GameState gameState, int slotNumber) {
    try {
      logger.debug("Player를 SimpleSaveData로 변환 시작: {}", player.getName());

      return new SimpleSaveData(String.valueOf(SystemConstants.GAME_VERSION), LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), slotNumber, player.getName(),
          player.getLevel(), player.getExp(), player.getHp(), player.getMaxHp(), player.getMana(), player.getMaxMana(), player.getBaseAttack(), player.getBaseDefense(), player.getGold(),
          player.getRestoreHp(), player.getRestoreMana(), player.getPlayerStatusCondition().name(), extractItemEntries(player.getInventory()), extractEquipmentSlots(player.getInventory()),
          player.getInventory().getMaxSize(), extractQuestProgress(player.getQuestManager()), extractCompletedQuestIds(player.getQuestManager()), extractClaimedRewardIds(player.getQuestManager()),
          extractLearnedSkillIds(player.getSkillManager()), extractSkillCooldowns(player.getSkillManager()), gameState.getTotalPlayTime(), gameState.getMonstersKilled(),
          gameState.getQuestsCompleted(), gameState.getCurrentLocation());

    } catch (Exception e) {
      logger.error("Player를 SimpleSaveData로 변환 중 오류", e);
      throw new RuntimeException("저장 데이터 변환 실패: " + e.getMessage(), e);
    }
  }

  /**
   * SimpleSaveData를 Player로 변환 (로드용)
   */
  public Player toPlayer() {
    try {
      logger.debug("SimpleSaveData를 Player로 변환 시작: {}", playerName);

      // === 1. 인벤토리 복원 ===
      PlayerInventory inventory = restoreInventoryWithFactory();

      // === 2. 스킬 매니저 복원 ===
      SkillService skillManager = restoreSkillManagerWithFactory();

      // === 3. 퀘스트 매니저 복원 ===
      QuestManager questManager = restoreQuestManagerWithFactory();

      // === 4. Player 생성 ===
      Player player = new Player(playerName, level, hp, maxHp, mana, maxMana, restoreHp, restoreMp, experience, attack, defense, gold, inventory, skillManager,
          PlayerStatusCondition.valueOf(statusCondition), questManager);

      // 상태 조건 설정
      try {
        PlayerStatusCondition condition = PlayerStatusCondition.valueOf(statusCondition);
        player.setPlayerStatusCondition(condition);
      } catch (Exception e) {
        logger.warn("잘못된 상태 조건, 기본값으로 설정: {}", statusCondition);
        player.setPlayerStatusCondition(PlayerStatusCondition.NORMAL);
      }

      // 인벤토리, 스킬, 퀘스트 매니저 설정
      player.setInventory(inventory);
      player.setSkillManager(skillManager);

      try {
        // 모든 활성 레벨 퀘스트의 진행도를 현재 플레이어 레벨과 동기화
        questManager.synchronizeLevelQuestProgress(player);
        logger.info("게임 로드 후 퀘스트 진행도 동기화 완료 (레벨: {})", level);
        // 추가: 활성 레벨 퀘스트들의 진행도를 현재 레벨로 즉시 업데이트
        questManager.updateLevelProgress(player);
        logger.debug("레벨 퀘스트 진행도 즉시 업데이트 완료");
      } catch (Exception e) {
        logger.error("퀘스트 진행도 동기화 중 오류", e);
      }

      player.setQuestManager(questManager);

      logger.info("Player 복원 완료: {} (레벨: {}, 스킬: {}개)", playerName, level, learnedSkillIds.size());
      return player;

    } catch (Exception e) {
      logger.error("Player 복원 중 오류", e);
      throw new RuntimeException("플레이어 데이터 복원 실패: " + e.getMessage(), e);
    }
  }

  // === ⭐ 인벤토리 복원 (Factory 패턴 활용) ===

  /**
   * Factory 패턴을 활용한 인벤토리 복원
   */
  private PlayerInventory restoreInventoryWithFactory() {
    try {
      logger.debug("Factory 패턴으로 인벤토리 복원 시작: {}개 아이템, {}개 슬롯", items.size(), maxSlots);

      PlayerInventory inventory = new PlayerInventory(maxSlots);
      GameItemFactory itemFactory = GameItemFactory.getInstance();

      // === 일반 아이템들 복원 ===
      int successCount = 0;
      int failCount = 0;

      for (ItemEntry entry : items) {
        try {
          GameItem item = itemFactory.createItem(entry.getItemId());

          if (item != null) {
            boolean added = inventory.addItem(item, entry.getQuantity());
            if (added) {
              successCount++;
              logger.debug("아이템 복원 성공: {} (ID: {}) x{}", item.getName(), entry.getItemId(), entry.getQuantity());
            } else {
              logger.warn("인벤토리 추가 실패: {} (인벤토리 가득참?)", item.getName());
              failCount++;
            }
          } else {
            logger.warn("아이템 생성 실패: {}", entry.getItemId());
            failCount++;
          }

        } catch (Exception e) {
          logger.error("아이템 복원 중 오류: {}", entry.getItemId(), e);
          failCount++;
        }
      }

      // === 장착 장비 복원 ===
      restoreEquippedItems(inventory, itemFactory);

      logger.info("인벤토리 복원 완료: 성공 {}개, 실패 {}개", successCount, failCount);
      return inventory;

    } catch (Exception e) {
      logger.error("인벤토리 복원 중 치명적 오류", e);
      return new PlayerInventory(maxSlots > 0 ? maxSlots : 20);
    }
  }

  /**
   * 장착된 장비 복원
   */
  private void restoreEquippedItems(PlayerInventory inventory, GameItemFactory itemFactory) {
    if (equipped == null) {
      logger.debug("장착된 장비 없음");
      return;
    }

    try {
      // === 무기 복원 ===
      if (equipped.getWeapon() != null) {
        restoreEquippedItem(inventory, itemFactory, equipped.getWeapon(), "무기", (inv, equipment) -> inv.setEquippedWeapon(equipment));
      }

      // === 방어구 복원 ===
      if (equipped.getArmor() != null) {
        restoreEquippedItem(inventory, itemFactory, equipped.getArmor(), "방어구", (inv, equipment) -> inv.setEquippedArmor(equipment));
      }

      // === 액세서리 복원 ===
      if (equipped.getAccessory() != null) {
        restoreEquippedItem(inventory, itemFactory, equipped.getAccessory(), "액세서리", (inv, equipment) -> inv.setEquippedAccessory(equipment));
      }

    } catch (Exception e) {
      logger.error("장착 장비 복원 중 오류", e);
    }
  }

  /**
   * 개별 장비 아이템 복원
   */
  private void restoreEquippedItem(PlayerInventory inventory, GameItemFactory itemFactory, String itemId, String equipmentType, EquipmentSetter setter) {
    try {
      GameItem item = itemFactory.createItem(itemId);

      if (item instanceof GameEquipment equipment) {
        setter.setEquipment(inventory, equipment);
        logger.debug("{} 복원 성공: {} (ID: {})", equipmentType, equipment.getName(), itemId);
      } else if (item != null) {
        logger.warn("{} 복원 실패: {}는 장비가 아님", equipmentType, item.getName());
      } else {
        logger.warn("{} 복원 실패: {} 아이템을 찾을 수 없음", equipmentType, itemId);
      }

    } catch (Exception e) {
      logger.error("{} 복원 중 오류: {}", equipmentType, itemId, e);
    }
  }

  // === ⭐ 퀘스트 복원 (Factory 패턴 활용) ===

  /**
   * Factory 패턴을 활용한 퀘스트 매니저 복원
   */
  private QuestManager restoreQuestManagerWithFactory() {
    try {
      logger.debug("Factory 패턴으로 퀘스트 복원 시작");

      QuestManager questManager = QuestManager.createForLoading();
      JsonBasedQuestFactory questFactory = JsonBasedQuestFactory.getInstance();

      // === 활성 퀘스트 복원 ===
      logger.debug("활성 퀘스트 {}개 복원 시작", activeQuests.size());
      for (QuestProgress questProgress : activeQuests) {
        try {
          Quest quest = questFactory.createQuest(questProgress.getQuestId());

          if (quest != null) {
            quest.setCurrentProgress(questProgress.getProgress());
            Quest.QuestStatus status = Quest.QuestStatus.valueOf(questProgress.getStatus());
            quest.setStatus(status);
            questManager.addToActiveQuests(quest);

            logger.debug("활성 퀘스트 복원 성공: {} (상태: {})", quest.getId(), quest.getStatus());
          } else {
            logger.warn("퀘스트 생성 실패: {}", questProgress.getQuestId());
          }

        } catch (Exception e) {
          logger.error("활성 퀘스트 복원 실패: {}", questProgress.getQuestId(), e);
        }
      }

      // === 완료된 퀘스트 복원 ===
      logger.debug("완료 퀘스트 {}개 복원 시작", completedQuestIds.size());
      for (String questId : completedQuestIds) {
        try {
          Quest quest = questFactory.createQuest(questId);

          if (quest != null) {
            quest.setStatus(Quest.QuestStatus.COMPLETED);
            questManager.addToCompletedQuests(quest);
            logger.debug("완료 퀘스트 복원 성공: {}", questId);
          } else {
            logger.warn("완료 퀘스트 생성 실패: {}", questId);
          }

        } catch (Exception e) {
          logger.error("완료 퀘스트 복원 실패: {}", questId, e);
        }
      }

      // === 보상 수령 상태 복원 ===
      for (String rewardId : claimedRewardIds) {
        questManager.markRewardAsClaimed(rewardId);
      }

      logger.info("퀘스트 복원 완료: 활성 {}개, 완료 {}개, 보상수령 {}개", activeQuests.size(), completedQuestIds.size(), claimedRewardIds.size());

      return questManager;

    } catch (Exception e) {
      logger.error("퀘스트 매니저 복원 중 오류", e);
      return new QuestManager();
    }
  }

  // === ⭐ 스킬 복원 ===

  /**
   * 스킬 매니저 복원
   */
  private SkillService restoreSkillManagerWithFactory() {
    try {
      // ⭐ 저장된 데이터로 SkillService 생성 (기본 스킬 초기화 안함)
      SkillService skillManager = new SkillService(learnedSkillIds, skillCooldowns);

      logger.info("스킬 매니저 복원 완료: {}개 스킬", learnedSkillIds.size());
      return skillManager;
    } catch (Exception e) {
      logger.error("스킬 매니저 복원 중 오류", e);
      return new SkillService();
    }
  }

  // === ⭐ 저장용 추출 메서드들 ===

  /**
   * 아이템 엔트리 추출 (GameItem.getId() 직접 사용)
   */
  private static List<ItemEntry> extractItemEntries(PlayerInventory inventory) {
    try {
      List<ItemEntry> entries = new ArrayList<>();

      for (ItemStack itemStack : inventory.getItems()) {
        try {
          String itemId = itemStack.getItem().getId();

          if (itemId != null && !itemId.isEmpty()) {
            entries.add(new ItemEntry(itemId, itemStack.getQuantity()));
            logger.debug("아이템 저장: {} (ID: {}) x{}", itemStack.getItem().getName(), itemId, itemStack.getQuantity());
          } else {
            logger.warn("아이템 ID가 비어있음: {}", itemStack.getItem().getName());
          }

        } catch (Exception e) {
          logger.error("아이템 엔트리 변환 실패: {}", itemStack.getItem().getName(), e);
        }
      }

      logger.debug("아이템 엔트리 추출 완료: {}개", entries.size());
      return entries;

    } catch (Exception e) {
      logger.error("아이템 엔트리 추출 중 오류", e);
      return new ArrayList<>();
    }
  }

  /**
   * 장착 장비 슬롯 추출 (GameItem.getId() 직접 사용)
   */
  private static EquipmentSlots extractEquipmentSlots(PlayerInventory inventory) {
    try {
      String weaponId = inventory.getEquippedWeapon() != null ? inventory.getEquippedWeapon().getId() : null;
      String armorId = inventory.getEquippedArmor() != null ? inventory.getEquippedArmor().getId() : null;
      String accessoryId = inventory.getEquippedAccessory() != null ? inventory.getEquippedAccessory().getId() : null;

      logger.debug("장착 장비 추출: 무기={}, 방어구={}, 액세서리={}", weaponId, armorId, accessoryId);

      return new EquipmentSlots(weaponId, armorId, accessoryId);

    } catch (Exception e) {
      logger.error("장착 장비 슬롯 추출 중 오류", e);
      return new EquipmentSlots(null, null, null);
    }
  }

  /**
   * 퀘스트 진행도 추출
   */
  private static List<QuestProgress> extractQuestProgress(QuestManager questManager) {
    try {
      List<QuestProgress> progress = questManager.getActiveQuests().stream().map(quest -> {
        String status = quest.getStatus() != null ? quest.getStatus().name() : "ACTIVE";
        logger.debug("퀘스트 저장: {} (진행도: {}, 상태: {})", quest.getId(), quest.getCurrentProgress(), status);

        return new QuestProgress(quest.getId(), quest.getCurrentProgress(), status);
      }).collect(Collectors.toList());

      logger.debug("퀘스트 진행도 추출 완료: {}개", progress.size());
      return progress;

    } catch (Exception e) {
      logger.error("퀘스트 진행도 추출 실패", e);
      return new ArrayList<>();
    }
  }

  /**
   * 완료된 퀘스트 ID 추출
   */
  private static List<String> extractCompletedQuestIds(QuestManager questManager) {
    try {
      List<String> completedIds = questManager.getCompletedQuests().stream().map(Quest::getId).collect(Collectors.toList());

      logger.debug("완료 퀘스트 ID 추출 완료: {}개", completedIds.size());
      return completedIds;

    } catch (Exception e) {
      logger.error("완료 퀘스트 ID 추출 실패", e);
      return new ArrayList<>();
    }
  }

  /**
   * 보상 수령 상태 추출
   */
  private static List<String> extractClaimedRewardIds(QuestManager questManager) {
    try {
      List<String> claimedIds = questManager.getClaimedRewardIds();
      logger.debug("보상 수령 상태 추출 완료: {}개", claimedIds.size());
      return claimedIds;

    } catch (Exception e) {
      logger.error("보상 수령 상태 추출 실패", e);
      return new ArrayList<>();
    }
  }

  /**
   * 학습한 스킬 ID 추출
   */
  private static List<String> extractLearnedSkillIds(SkillService skillManager) {
    try {
      List<String> skillIds = skillManager.getLearnedSkillIds();

      logger.debug("학습 스킬 ID 추출 완료: {}개", skillIds.size());
      return skillIds;

    } catch (Exception e) {
      logger.error("학습 스킬 ID 추출 실패", e);
      return new ArrayList<>();
    }
  }

  /**
   * 스킬 쿨다운 추출
   */
  private static Map<String, Integer> extractSkillCooldowns(SkillService skillManager) {
    try {
      Map<String, Integer> cooldowns = skillManager.getSkillCooldowns();

      logger.debug("스킬 쿨다운 추출 완료: {}개", cooldowns.size());
      return cooldowns;

    } catch (Exception e) {
      logger.error("스킬 쿨다운 추출 실패", e);
      return new HashMap<>();
    }
  }

  /**
   * 장비 설정용 함수형 인터페이스
   */
  @FunctionalInterface
  private interface EquipmentSetter {
    void setEquipment(PlayerInventory inventory, GameEquipment equipment);
  }

  // === Getters (모든 필드) ===

  public String getVersion() {
    return version;
  }

  public String getSaveTime() {
    return saveTime;
  }

  public int getSlotNumber() {
    return slotNumber;
  }

  public String getPlayerName() {
    return playerName;
  }

  public int getLevel() {
    return level;
  }

  public int getExperience() {
    return experience;
  }

  public int getHp() {
    return hp;
  }

  public int getMaxHp() {
    return maxHp;
  }

  public int getMana() {
    return mana;
  }

  public int getMaxMana() {
    return maxMana;
  }

  public int getAttack() {
    return attack;
  }

  public int getDefense() {
    return defense;
  }

  public int getGold() {
    return gold;
  }

  public double getRestoreHp() {
    return restoreHp;
  }

  public double getRestoreMp() {
    return restoreMp;
  }

  public String getStatusCondition() {
    return statusCondition;
  }

  public List<ItemEntry> getItems() {
    return new ArrayList<>(items);
  }

  public EquipmentSlots getEquipped() {
    return equipped;
  }

  public int getMaxSlots() {
    return maxSlots;
  }

  public List<QuestProgress> getActiveQuests() {
    return new ArrayList<>(activeQuests);
  }

  public List<String> getCompletedQuestIds() {
    return new ArrayList<>(completedQuestIds);
  }

  public List<String> getClaimedRewardIds() {
    return new ArrayList<>(claimedRewardIds);
  }

  public List<String> getLearnedSkillIds() {
    return new ArrayList<>(learnedSkillIds);
  }

  public Map<String, Integer> getSkillCooldowns() {
    return new HashMap<>(skillCooldowns);
  }

  public int getTotalPlayTime() {
    return totalPlayTime;
  }

  public int getMonstersKilled() {
    return monstersKilled;
  }

  public int getQuestsCompleted() {
    return questsCompleted;
  }

  public String getCurrentLocation() {
    return currentLocation;
  }
}
