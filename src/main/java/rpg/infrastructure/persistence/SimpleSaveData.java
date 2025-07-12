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
import rpg.application.service.QuestManager;
import rpg.application.service.SkillService;
import rpg.core.engine.GameState;
import rpg.domain.inventory.ItemStack;
import rpg.domain.inventory.PlayerInventory;
import rpg.domain.item.GameEquipment;
import rpg.domain.item.GameItem;
import rpg.domain.player.Player;
import rpg.domain.quest.Quest;
import rpg.domain.quest.QuestReward;
import rpg.shared.constant.SystemConstants;

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
    this.items = items != null ? items : new ArrayList<>();
    this.equipped = equipped;
    this.maxSlots = maxSlots;
    this.activeQuests = activeQuests != null ? activeQuests : new ArrayList<>();
    this.completedQuestIds = completedQuestIds != null ? completedQuestIds : new ArrayList<>();
    this.claimedRewardIds = claimedRewardIds != null ? claimedRewardIds : new ArrayList<>();
    this.learnedSkillIds = learnedSkillIds != null ? learnedSkillIds : new ArrayList<>();
    this.skillCooldowns = skillCooldowns != null ? skillCooldowns : new HashMap<>();
    this.totalPlayTime = totalPlayTime;
    this.monstersKilled = monstersKilled;
    this.questsCompleted = questsCompleted;
    this.currentLocation = currentLocation;
  }

  // === Getters ===
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
    return items;
  }

  public EquipmentSlots getEquipped() {
    return equipped;
  }

  public int getMaxSlots() {
    return maxSlots;
  }

  public List<QuestProgress> getActiveQuests() {
    return activeQuests;
  }

  public List<String> getCompletedQuestIds() {
    return completedQuestIds;
  }

  public List<String> getClaimedRewardIds() {
    return claimedRewardIds;
  }

  public List<String> getLearnedSkillIds() {
    return learnedSkillIds;
  }

  public Map<String, Integer> getSkillCooldowns() {
    return skillCooldowns;
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


  public static SimpleSaveData from(Player player, GameState gameState, int slotNumber) {
    try {
      logger.debug("Player를 SimpleSaveData로 변환 시작: {}", player.getName());

      // 인벤토리 아이템 확인
      PlayerInventory inventory = player.getInventory();
      logger.debug("인벤토리 아이템 수: {}", inventory.getItems().size());
      for (ItemStack stack : inventory.getItems()) {
        logger.debug("아이템: {} x{}", stack.getItem().getName(), stack.getQuantity());
      }

      // 퀘스트 확인
      QuestManager questManager = player.getQuestManager();
      logger.debug("활성 퀘스트 수: {}", questManager.getActiveQuests().size());
      logger.debug("완료 퀘스트 수: {}", questManager.getCompletedQuests().size());

      // 스킬 확인
      SkillService skillManager = player.getSkillManager();
      logger.debug("학습 스킬 수: {}", skillManager.getLearnedSkills().size());

      return new SimpleSaveData(String.valueOf(SystemConstants.GAME_VERSION),
          LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), slotNumber, player.getName(), player.getLevel(),
          player.getExp(), player.getHp(), player.getMaxHp(), player.getMana(), player.getMaxMana(), player.getBaseAttack(), player.getBaseDefense(),
          player.getGold(), player.getRestoreHp(), player.getRestoreMana(), player.getPlayerStatusCondition().name(),
          extractItemEntries(player.getInventory()), extractEquipmentSlots(player.getInventory()), player.getInventory().getMaxSize(),
          extractQuestProgress(player.getQuestManager()), extractCompletedQuestIds(player.getQuestManager()),
          extractClaimedRewardIds(player.getQuestManager()), extractLearnedSkillIds(player.getSkillManager()),
          extractSkillCooldowns(player.getSkillManager()), gameState.getTotalPlayTime(), gameState.getMonstersKilled(),
          gameState.getQuestsCompleted(), gameState.getCurrentLocation());
    } catch (Exception e) {
      logger.error("Player를 SimpleSaveData로 변환 중 오류", e);
      throw new RuntimeException("저장 데이터 변환 실패: " + e.getMessage(), e);
    }
  }

  // === SimpleSaveData에서 Player로 변환 ===
  public Player toPlayer() {
    try {
      logger.debug("SimpleSaveData를 Player로 변환 시작: {}", playerName);

      // PlayerInventory 복원
      PlayerInventory inventory = new PlayerInventory(maxSlots);

      // 아이템들 복원
      GameItemFactory itemFactory = GameItemFactory.getInstance();
      for (ItemEntry entry : items) {
        GameItem item = itemFactory.createItem(entry.getItemId());
        if (item != null) {
          inventory.addItem(item, entry.getQuantity());
        } else {
          logger.warn("아이템 복원 실패: {}", entry.getItemId());
        }
      }

      // 장착 장비 복원
      if (equipped != null) {
        if (equipped.getWeapon() != null) {
          GameItem weapon = itemFactory.createItem(equipped.getWeapon());
          if (weapon instanceof GameEquipment) {
            inventory.setEquippedWeapon((GameEquipment) weapon);
          }
        }
        if (equipped.getArmor() != null) {
          GameItem armor = itemFactory.createItem(equipped.getArmor());
          if (armor instanceof GameEquipment) {
            inventory.setEquippedArmor((GameEquipment) armor);
          }
        }
        if (equipped.getAccessory() != null) {
          GameItem accessory = itemFactory.createItem(equipped.getAccessory());
          if (accessory instanceof GameEquipment) {
            inventory.setEquippedAccessory((GameEquipment) accessory);
          }
        }
      }

      // SkillService 복원
      SkillService skillManager = new SkillService();
      // TODO: 스킬 복원 로직 구현 필요

      QuestManager questManager = QuestManager.createForLoading(); // 빈 QuestManager 생성

      try {
        // 활성 퀘스트 복원
        for (QuestProgress questProgress : activeQuests) {
          Quest restoredQuest = restoreQuestFromProgress(questProgress);
          if (restoredQuest != null) {
            questManager.addToActiveQuests(restoredQuest);
            logger.debug("활성 퀘스트 복원: {} (상태: {})", restoredQuest.getId(), restoredQuest.getStatus());
          }
        }

        // 완료된 퀘스트 복원
        for (String questId : completedQuestIds) {
          Quest completedQuest = createQuestById(questId);
          if (completedQuest != null) {
            completedQuest.setStatus(Quest.QuestStatus.COMPLETED);
            questManager.addToCompletedQuests(completedQuest);
            logger.debug("완료 퀘스트 복원: {}", questId);
          }
        }

        logger.debug("퀘스트 복원 완료: 활성 {}개, 완료 {}개", activeQuests.size(), completedQuestIds.size());

      } catch (Exception e) {
        logger.error("퀘스트 복원 중 오류", e);
        // 오류 시 기본 퀘스트 매니저 사용
        questManager = new QuestManager();
      }


      // Player 생성 (저장된 데이터로)
      Player player = new Player(playerName, level, hp, maxHp, mana, maxMana, restoreHp, restoreMp, experience, attack, defense, gold, inventory,
          skillManager, rpg.domain.player.PlayerStatusCondition.valueOf(statusCondition), questManager);

      logger.debug("Player 복원 완료: {}", playerName);
      return player;

    } catch (Exception e) {
      logger.error("SimpleSaveData를 Player로 변환 중 오류", e);
      throw new RuntimeException("플레이어 데이터 복원 실패: " + e.getMessage(), e);
    }
  }

  // 개선된 추출 메서드들
  private static List<ItemEntry> extractItemEntries(PlayerInventory inventory) {
    try {
      List<ItemEntry> entries = inventory.getItems().stream().map(itemStack -> {
        String itemId = getItemId(itemStack.getItem());
        logger.debug("아이템 변환: {} -> {}", itemStack.getItem().getName(), itemId);
        return new ItemEntry(itemId, itemStack.getQuantity());
      }).collect(Collectors.toList());

      logger.debug("변환된 아이템 엔트리 수: {}", entries.size());
      return entries;
    } catch (Exception e) {
      logger.error("아이템 엔트리 추출 실패", e);
      return new ArrayList<>();
    }
  }

  private static EquipmentSlots extractEquipmentSlots(PlayerInventory inventory) {
    return new EquipmentSlots(inventory.getEquippedWeapon() != null ? getItemId(inventory.getEquippedWeapon()) : null,
        inventory.getEquippedArmor() != null ? getItemId(inventory.getEquippedArmor()) : null,
        inventory.getEquippedAccessory() != null ? getItemId(inventory.getEquippedAccessory()) : null);
  }

  // extractQuestProgress 메서드도 수정 필요
  private static List<QuestProgress> extractQuestProgress(QuestManager questManager) {
    try {
      List<QuestProgress> progress = questManager.getActiveQuests().stream().map(quest -> {
        String status = quest.getStatus() != null ? quest.getStatus().name() : "ACTIVE";
        logger.debug("퀘스트 변환: {} (진행도: {}, 상태: {})", quest.getId(), quest.getCurrentProgress(), status);
        return new QuestProgress(quest.getId(), quest.getCurrentProgress(), status // ✅ 상태 추가!
        );
      }).collect(Collectors.toList());

      logger.debug("변환된 퀘스트 진행도 수: {}", progress.size());
      return progress;
    } catch (Exception e) {
      logger.error("퀘스트 진행도 추출 실패", e);
      return new ArrayList<>();
    }
  }

  private static List<String> extractCompletedQuestIds(QuestManager questManager) {
    return questManager.getCompletedQuests().stream().map(Quest::getId).collect(Collectors.toList());
  }

  private static List<String> extractClaimedRewardIds(QuestManager questManager) {
    // TODO: QuestManager에 getClaimedRewardIds() 메서드 필요
    return new ArrayList<>(); // 임시
  }

  private static List<String> extractLearnedSkillIds(SkillService skillManager) {
    return skillManager.getLearnedSkills().stream().map(skill -> skill.getName()) // 스킬 ID 대신 이름 사용 (임시)
        .collect(Collectors.toList());
  }

  private static Map<String, Integer> extractSkillCooldowns(SkillService skillManager) {
    return new HashMap<>(skillManager.getSkillCooldowns());
  }


  // SimpleSaveData에서 수정된 아이템 ID 매핑 메서드

  private static String getItemId(GameItem item) {
    if (item == null) {
      return "UNKNOWN_ITEM";
    }

    String name = item.getName();
    if (name == null) {
      return "UNKNOWN_ITEM";
    }
    
      return item.getId();
  }


  // 퀘스트 진행도에서 퀘스트 객체 복원
  private static Quest restoreQuestFromProgress(QuestProgress questProgress) {
    try {
      // questId로 기본 퀘스트 생성
      Quest quest = createQuestById(questProgress.getQuestId());
      if (quest == null) {
        logger.warn("퀘스트 생성 실패: {}", questProgress.getQuestId());
        return null;
      }

      // 진행도 복원
      quest.setCurrentProgress(questProgress.getProgress());

      // 상태 복원
      try {
        Quest.QuestStatus status = Quest.QuestStatus.valueOf(questProgress.getStatus());
        quest.setStatus(status);
      } catch (IllegalArgumentException e) {
        logger.warn("잘못된 퀘스트 상태: {}, 기본값 사용", questProgress.getStatus());
        quest.setStatus(Quest.QuestStatus.ACTIVE);
      }

      return quest;

    } catch (Exception e) {
      logger.error("퀘스트 복원 실패: {}", questProgress.getQuestId(), e);
      return null;
    }
  }

  // questId로 퀘스트 생성 (기본 퀘스트들 중에서)
  private static Quest createQuestById(String questId) {
    // 기본 퀘스트 생성 로직
    switch (questId) {
      case "quest_001":
        return createSlimeQuest();
      // case "quest_002":
      // return createGoblinQuest();
      // case "quest_003":
      // return createOrcQuest();
      // case "quest_004":
      // return createDragonQuest();
      // case "quest_005":
      // return createLevelQuest();
      // case "quest_006":
      // return createCollectionQuest();
      default:
        logger.warn("알 수 없는 퀘스트 ID: {}", questId);
        return null;
    }
  }

  // 기본 퀘스트 생성 메서드들 (QuestManager에서 복사)
  private static Quest createSlimeQuest() {
    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("kill_슬라임", 5);

    // 간단한 보상 (ItemFactory 없이)
    QuestReward reward = new QuestReward(50, 100, GameItemFactory.getInstance().createItem("HEALTH_POTION"),2);
    logger.debug("reward : {} ", reward);
    return new Quest("quest_001", "슬라임 사냥꾼", "마을 근처의 슬라임 5마리를 처치하세요.", Quest.QuestType.KILL, 1, objectives, reward);
  }

}
