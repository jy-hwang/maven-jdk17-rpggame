package service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import model.GameCharacter;
import model.GameConsumable;
import model.GameEquipment;
import model.GameItem;
import model.Quest;
import model.QuestReward;

/**
 * 퀘스트를 관리하는 서비스 클래스
 */
public class QuestManager {
  private static final Logger logger = LoggerFactory.getLogger(QuestManager.class);

  private List<Quest> availableQuests;
  private List<Quest> activeQuests;
  private List<Quest> completedQuests;

  @JsonCreator
  public QuestManager() {
    this.availableQuests = new ArrayList<>();
    this.activeQuests = new ArrayList<>();
    this.completedQuests = new ArrayList<>();
    initializeQuests();
  }

  /**
   * 기본 퀘스트들을 초기화합니다.
   */
  private void initializeQuests() {
    // 초보자 퀘스트
    Map<String, Integer> slimeObjectives = new HashMap<>();
    slimeObjectives.put("kill_슬라임", 5);

    QuestReward slimeReward = new QuestReward(50, 100, new GameConsumable("체력 물약", "HP를 50 회복합니다", 20, GameItem.ItemRarity.COMMON, 50, 0, 0, true), 2);

    Quest slimeQuest = new Quest("quest_001", "슬라임 사냥꾼", "마을 근처의 슬라임 5마리를 처치하세요.", Quest.QuestType.KILL, 1, slimeObjectives, slimeReward);

    // 고블린 퀘스트
    Map<String, Integer> goblinObjectives = new HashMap<>();
    goblinObjectives.put("kill_고블린", 3);

    GameEquipment ironSword = new GameEquipment("철검", "날카로운 철로 만든 검", 100, GameItem.ItemRarity.UNCOMMON, GameEquipment.EquipmentType.WEAPON, 15, 0, 0);

    QuestReward goblinReward = new QuestReward(100, 200, ironSword, 1);

    Quest goblinQuest = new Quest("quest_002", "고블린 소탕", "위험한 고블린 3마리를 처치하세요.", Quest.QuestType.KILL, 3, goblinObjectives, goblinReward);

    // 오크 퀘스트
    Map<String, Integer> orcObjectives = new HashMap<>();
    orcObjectives.put("kill_오크", 2);

    GameEquipment plateArmor = new GameEquipment("판금 갑옷", "튼튼한 판금으로 만든 갑옷", 200, GameItem.ItemRarity.RARE, GameEquipment.EquipmentType.ARMOR, 0, 20, 50);

    QuestReward orcReward = new QuestReward(200, 500, plateArmor, 1);

    Quest orcQuest = new Quest("quest_003", "오크 토벌", "강력한 오크 2마리를 처치하세요.", Quest.QuestType.KILL, 5, orcObjectives, orcReward);

    // 드래곤 퀘스트 (최종 보스)
    Map<String, Integer> dragonObjectives = new HashMap<>();
    dragonObjectives.put("kill_드래곤", 1);

    GameEquipment legendaryRing = new GameEquipment("드래곤 반지", "전설적인 드래곤의 힘이 깃든 반지", 1000, GameItem.ItemRarity.LEGENDARY, GameEquipment.EquipmentType.ACCESSORY, 30, 15, 100);

    QuestReward dragonReward = new QuestReward(1000, 2000, legendaryRing, 1);

    Quest dragonQuest = new Quest("quest_004", "드래곤 슬레이어", "전설의 드래곤을 처치하고 영웅이 되세요!", Quest.QuestType.KILL, 8, dragonObjectives, dragonReward);

    // 레벨업 퀘스트
    Map<String, Integer> levelObjectives = new HashMap<>();
    levelObjectives.put("reach_level", 5);

    QuestReward levelReward = new QuestReward(100, 150);
    levelReward.addItemReward(new GameConsumable("체력 물약", "HP를 50 회복합니다", 20, GameItem.ItemRarity.COMMON, 50, 0, 0, true), 3);
    levelReward.addItemReward(new GameConsumable("마나 물약", "MP를 30 회복합니다", 25, GameItem.ItemRarity.COMMON, 0, 30, 0, true), 2);

    Quest levelQuest = new Quest("quest_005", "성장하는 모험가", "레벨 5에 도달하세요.", Quest.QuestType.LEVEL, 1, levelObjectives, levelReward);

    availableQuests.addAll(Arrays.asList(slimeQuest, goblinQuest, orcQuest, dragonQuest, levelQuest));
    logger.info("기본 퀘스트 {} 개 초기화 완료", availableQuests.size());
  }

  /**
   * 캐릭터가 수락할 수 있는 퀘스트 목록을 반환합니다.
   */
  public List<Quest> getAvailableQuests(GameCharacter character) {
    return availableQuests.stream().filter(quest -> quest.getRequiredLevel() <= character.getLevel()).filter(quest -> quest.getStatus() == Quest.QuestStatus.AVAILABLE).toList();
  }

  /**
   * 퀘스트를 수락합니다.
   */
  public boolean acceptQuest(String questId, GameCharacter character) {
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

    for (Quest quest : new ArrayList<>(activeQuests)) { // ConcurrentModificationException 방지
      if (quest.getType() == Quest.QuestType.KILL && quest.updateProgress(objectiveKey, 1)) {
        // 퀘스트 완료
        activeQuests.remove(quest);
        completedQuests.add(quest);
        System.out.println("🎉 퀘스트 '" + quest.getTitle() + "'을(를) 완료했습니다!");
        break; // 하나씩 처리
      }
    }
  }

  /**
   * 레벨업 퀘스트 진행도를 업데이트합니다.
   */
  public void updateLevelProgress(GameCharacter character) {
    for (Quest quest : new ArrayList<>(activeQuests)) {
      if (quest.getType() == Quest.QuestType.LEVEL) {
        if (quest.updateProgress("reach_level", character.getLevel())) {
          activeQuests.remove(quest);
          completedQuests.add(quest);
          System.out.println("🎉 퀘스트 '" + quest.getTitle() + "'을(를) 완료했습니다!");
          break;
        }
      }
    }
  }

  /**
   * 아이템 수집 퀘스트 진행도를 업데이트합니다.
   */
  public void updateCollectionProgress(GameCharacter character, String itemName, int quantity) {
    String objectiveKey = "collect_" + itemName;

    for (Quest quest : new ArrayList<>(activeQuests)) {
      if (quest.getType() == Quest.QuestType.COLLECT && quest.updateProgress(objectiveKey, quantity)) {
        activeQuests.remove(quest);
        completedQuests.add(quest);
        System.out.println("🎉 퀘스트 '" + quest.getTitle() + "'을(를) 완료했습니다!");
        break;
      }
    }
  }

  /**
   * 완료된 퀘스트의 보상을 수령합니다.
   */
  public boolean claimQuestReward(String questId, GameCharacter character) {
    Quest quest = findQuestById(questId, completedQuests);
    if (quest != null && quest.getStatus() == Quest.QuestStatus.COMPLETED) {
      if (quest.claimReward(character)) {
        logger.info("퀘스트 보상 수령: {} (캐릭터: {})", quest.getTitle(), character.getName());
        return true;
      }
    }
    return false;
  }

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
  public void displayAvailableQuests(GameCharacter character) {
    List<Quest> available = getAvailableQuests(character);
    System.out.println("\n=== 수락 가능한 퀘스트 ===");
    if (available.isEmpty()) {
      System.out.println("현재 수락할 수 있는 퀘스트가 없습니다.");
      if (character.getLevel() < 8) {
        System.out.println("💡 레벨을 올리면 새로운 퀘스트가 해금됩니다!");
      }
    } else {
      for (int i = 0; i < available.size(); i++) {
        Quest quest = available.get(i);
        System.out.printf("%d. %s (필요 레벨: %d)%n", i + 1, quest.getTitle(), quest.getRequiredLevel());
        System.out.printf("   설명: %s%n", quest.getDescription());
        System.out.printf("   보상: %s%n", quest.getReward().getRewardDescription());
      }
    }
    System.out.println("========================");
  }

  /**
   * ID로 퀘스트를 찾습니다.
   */
  private Quest findQuestById(String questId, List<Quest> questList) {
    return questList.stream().filter(quest -> quest.getId().equals(questId)).findFirst().orElse(null);
  }

  /**
   * 인덱스로 수락 가능한 퀘스트를 반환합니다.
   */
  public Quest getAvailableQuestByIndex(int index, GameCharacter character) {
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
  public QuestStatistics getStatistics(GameCharacter character) {
    var available = getAvailableQuests(character);
    var claimable = getClaimableQuests();
    var claimed = completedQuests.stream().filter(quest -> quest.getStatus() == Quest.QuestStatus.CLAIMED).toList();

    return new QuestStatistics(available.size(), activeQuests.size(), claimable.size(), claimed.size());
  }

  // Getters
  public List<Quest> getAvailableQuests() {
    return new ArrayList<>(availableQuests);
  }

  public List<Quest> getActiveQuests() {
    return new ArrayList<>(activeQuests);
  }

  public List<Quest> getCompletedQuests() {
    return new ArrayList<>(completedQuests);
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
  }
}
