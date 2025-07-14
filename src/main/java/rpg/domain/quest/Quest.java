package rpg.domain.quest;


import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rpg.domain.item.GameItem;
import rpg.domain.player.Player;
import rpg.shared.constant.GameConstants;

/**
 * 퀘스트를 나타내는 클래스 (QuestReward와 연동)
 */
public class Quest {
  private static final Logger logger = LoggerFactory.getLogger(Quest.class);

  private String id;
  private String title;
  private String description;
  private QuestType type;
  private int requiredLevel;
  private Map<String, Integer> objectives;
  private Map<String, Integer> currentProgress;
  private QuestReward reward;
  private QuestStatus status;

  // 기본 생성자
  public Quest() {
    this.objectives = new HashMap<>();
    this.currentProgress = new HashMap<>();
    this.status = QuestStatus.AVAILABLE;
  }

  @JsonCreator
  public Quest(
      // @fotmatter:off
      @JsonProperty("id") String id, @JsonProperty("title") String title, @JsonProperty("description") String description,
      @JsonProperty("type") QuestType type, @JsonProperty("requiredLevel") int requiredLevel,
      @JsonProperty("objectives") Map<String, Integer> objectives, @JsonProperty("reward") QuestReward reward
  // @fotmatter:on
  ) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.type = type;
    this.requiredLevel = requiredLevel;
    this.objectives = objectives != null ? new HashMap<>(objectives) : new HashMap<>();
    this.currentProgress = new HashMap<>();
    this.reward = reward;
    this.status = QuestStatus.AVAILABLE;

    // 진행도 초기화
    for (String objective : this.objectives.keySet()) {
      this.currentProgress.put(objective, 0);
    }
  }

  /**
   * 퀘스트를 수락할 수 있는지 확인합니다.
   */
  public boolean canAccept(Player character) {
    return character.getLevel() >= requiredLevel && status == QuestStatus.AVAILABLE;
  }

  /**
   * 퀘스트를 수락합니다.
   */
  public boolean accept(Player character) {
    if (canAccept(character)) {
      this.status = QuestStatus.ACTIVE;
      return true;
    }
    return false;
  }

  /**
   * 퀘스트 진행도를 업데이트합니다.
   * 
   * @param objectiveKey 목표 키
   * @param progress 진행량
   * @return 퀘스트 완료 여부
   */
  public boolean updateProgress(String objectiveKey, int value) {
    if (!objectives.containsKey(objectiveKey)) {
      logger.debug("퀘스트 {}에 목표 키 {}가 없음", this.id, objectiveKey);
      return false;
    }

    int targetValue = objectives.get(objectiveKey);

    if (type == QuestType.LEVEL && objectiveKey.equals("reach_level")) {
      // 레벨 퀘스트: 현재 레벨이 목표 레벨 이상인지 확인
      currentProgress.put(objectiveKey, value);

      logger.debug("레벨 퀘스트 {} 진행도: 현재 레벨 {} vs 목표 레벨 {}", this.id, value, targetValue);

      if (value >= targetValue) {
        logger.info("레벨 퀘스트 {} 완료: 레벨 {} >= {}", this.id, value, targetValue);
        setStatus(QuestStatus.COMPLETED);
        return true;
      }
    } else {
      // 기존 로직 (처치, 수집 등) - 누적
      int currentValue = currentProgress.getOrDefault(objectiveKey, 0);
      int newValue = currentValue + value;
      currentProgress.put(objectiveKey, newValue);

      logger.debug("퀘스트 {} 진행도: {} = {} + {} = {} (목표: {})", this.id, objectiveKey, currentValue, value, newValue, targetValue);

      if (newValue >= targetValue && isCompleted()) {
        setStatus(QuestStatus.COMPLETED);
        return true;
      }
    }

    return false;
  }



  /**
   * 퀘스트가 완료되었는지 확인합니다.
   */
  public boolean isCompleted() {
    return objectives.entrySet().stream().allMatch(entry -> currentProgress.getOrDefault(entry.getKey(), 0) >= entry.getValue());
  }

  /**
   * 퀘스트 보상을 수령합니다.
   */
  public boolean claimReward(Player character) {
    if (status != QuestStatus.COMPLETED) {
      return false;
    }

    // 보상 지급
    if (reward != null) {
      // 경험치 보상
      if (reward.getExpReward() > GameConstants.NUMBER_ZERO) {
        character.gainExp(reward.getExpReward());
      }

      // 골드 보상
      if (reward.getGoldReward() > GameConstants.NUMBER_ZERO) {
        character.setGold(character.getGold() + reward.getGoldReward());
      }

      // 아이템 보상
      var itemRewards = reward.getItemRewards();
      for (Map.Entry<GameItem, Integer> entry : itemRewards.entrySet()) {
        GameItem item = entry.getKey();
        int quantity = entry.getValue();

        // 인벤토리에 공간이 있는지 확인하고 추가
        if (!character.getInventory().addItem(item, quantity)) {
          System.out.println("⚠️ 인벤토리가 가득 차서 " + item.getName() + " x" + quantity + "를 받을 수 없습니다!");
          return false;
        }
      }
    }

    this.status = QuestStatus.CLAIMED;
    return true;
  }

  /**
   * 퀘스트 목표 설명을 반환합니다.
   */
  public String getObjectiveDescription() {
    StringBuilder desc = new StringBuilder();

    for (Map.Entry<String, Integer> entry : objectives.entrySet()) {
      String key = entry.getKey();
      int target = entry.getValue();

      if (desc.length() > GameConstants.NUMBER_ZERO)
        desc.append(", ");

      if (key.startsWith("kill_")) {
        String monsterName = key.substring(5);
        desc.append(monsterName).append(" ").append(target).append("마리 처치");
      } else if (key.startsWith("collect_")) {
        String itemName = key.substring(8);
        desc.append(itemName).append(" ").append(target).append("개 수집");
      } else if (key.equals("reach_level")) {
        desc.append("레벨 ").append(target).append(" 달성");
      } else {
        desc.append(key).append(" ").append(target);
      }
    }

    return desc.toString();
  }

  /**
   * 퀘스트 진행도 설명을 반환합니다.
   */
  public String getProgressDescription() {
    StringBuilder desc = new StringBuilder();

    for (Map.Entry<String, Integer> entry : objectives.entrySet()) {
      String key = entry.getKey();
      int target = entry.getValue();
      int current = currentProgress.getOrDefault(key, GameConstants.NUMBER_ZERO);

      if (desc.length() > GameConstants.NUMBER_ZERO)
        desc.append(", ");
      desc.append(current).append("/").append(target);
    }

    return desc.toString();
  }

  /**
   * 퀘스트 정보를 표시합니다.
   */
  public void displayQuestInfo() {
    System.out.println("\n" + "=".repeat(GameConstants.NUMBER_TWENTY));
    System.out.println("📋 " + title);
    System.out.println("📝 " + description);
    System.out.println("🎯 목표: " + getObjectiveDescription());

    if (status == QuestStatus.ACTIVE) {
      System.out.println("📊 진행도: " + getProgressDescription());
    }

    System.out.println("⭐ 필요 레벨: " + requiredLevel);
    System.out.println("🏷️ 타입: " + getTypeKorean());
    System.out.println("🏆 상태: " + getStatusKorean());

    if (reward != null && !reward.isEmpty()) {
      System.out.println("🎁 보상: " + reward.getRewardDescription());
    }

    System.out.println("=".repeat(GameConstants.NUMBER_TWENTY));
  }

  /**
   * 진행도를 직접 설정합니다 (로드용)
   */
  public void setProgress(String objectiveKey, int progress) {
    if (currentProgress != null) {
      currentProgress.put(objectiveKey, progress);
    }
  }

  /**
   * 퀘스트 타입을 한국어로 반환합니다.
   */
  private String getTypeKorean() {
    return switch (type) {
      case KILL -> "처치";
      case COLLECT -> "수집";
      case LEVEL -> "레벨 달성";
      case EXPLORE -> "탐험";
      case DELIVERY -> "배달";
    };
  }

  /**
   * 퀘스트 상태를 한국어로 반환합니다.
   */
  private String getStatusKorean() {
    return switch (status) {
      case AVAILABLE -> "수락 가능";
      case ACTIVE -> "진행 중";
      case COMPLETED -> "완료";
      case CLAIMED -> "보상 수령 완료";
      case FAILED -> "실패";
    };
  }

  // Getters and Setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public QuestType getType() {
    return type;
  }

  public void setType(QuestType type) {
    this.type = type;
  }

  public int getRequiredLevel() {
    return requiredLevel;
  }

  public void setRequiredLevel(int requiredLevel) {
    this.requiredLevel = requiredLevel;
  }

  public Map<String, Integer> getObjectives() {
    return new HashMap<>(objectives);
  }

  public void setObjectives(Map<String, Integer> objectives) {
    this.objectives = objectives != null ? new HashMap<>(objectives) : new HashMap<>();
  }

  public Map<String, Integer> getCurrentProgress() {
    return new HashMap<>(currentProgress);
  }

  public void setCurrentProgress(Map<String, Integer> currentProgress) {
    this.currentProgress = currentProgress != null ? new HashMap<>(currentProgress) : new HashMap<>();
  }

  public QuestReward getReward() {
    return reward;
  }

  public void setReward(QuestReward reward) {
    this.reward = reward;
  }

  public QuestStatus getStatus() {
    return status;
  }

  public void setStatus(QuestStatus status) {
    this.status = status;
  }

  /**
   * 퀘스트 타입 열거형
   */
  public enum QuestType {
    KILL, // 몬스터 처치
    COLLECT, // 아이템 수집
    LEVEL, // 레벨 달성
    EXPLORE, // 탐험
    DELIVERY // 배달
  }

  /**
   * 퀘스트 상태 열거형
   */
  public enum QuestStatus {
    AVAILABLE, // 수락 가능
    ACTIVE, // 진행 중
    COMPLETED, // 완료 (보상 미수령)
    CLAIMED, // 보상 수령 완료
    FAILED // 실패
  }
}

