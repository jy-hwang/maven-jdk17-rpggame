package model;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import model.item.GameItem;

/**
 * 퀘스트 보상을 나타내는 클래스
 */
public class QuestReward {
  private int expReward;
  private int goldReward;
  private Map<GameItem, Integer> itemRewards;

  // 기본 생성자
  public QuestReward() {
    this.expReward = 0;
    this.goldReward = 0;
    this.itemRewards = new HashMap<>();
  }

  @JsonCreator
  public QuestReward(@JsonProperty("expReward") int expReward, @JsonProperty("goldReward") int goldReward, @JsonProperty("itemRewards") Map<GameItem, Integer> itemRewards) {
    this.expReward = expReward;
    this.goldReward = goldReward;
    this.itemRewards = itemRewards != null ? new HashMap<>(itemRewards) : new HashMap<>();
  }

  // 편의 생성자들
  public QuestReward(int expReward, int goldReward) {
    this(expReward, goldReward, new HashMap<>());
  }

  public QuestReward(int expReward, int goldReward, GameItem item, int itemQuantity) {
    this.expReward = expReward;
    this.goldReward = goldReward;
    this.itemRewards = new HashMap<>();
    if (item != null && itemQuantity > 0) {
      this.itemRewards.put(item, itemQuantity);
    }
  }

  /**
   * 아이템 보상을 추가합니다.
   */
  public void addItemReward(GameItem item, int quantity) {
    if (item != null && quantity > 0) {
      itemRewards.put(item, itemRewards.getOrDefault(item, 0) + quantity);
    }
  }

  /**
   * 보상이 비어있는지 확인합니다.
   */
  public boolean isEmpty() {
    return expReward <= 0 && goldReward <= 0 && itemRewards.isEmpty();
  }

  /**
   * 보상 내용을 문자열로 반환합니다.
   */
  public String getRewardDescription() {
    StringBuilder desc = new StringBuilder();

    if (expReward > 0) {
      desc.append("경험치 ").append(expReward);
    }

    if (goldReward > 0) {
      if (desc.length() > 0)
        desc.append(", ");
      desc.append("골드 ").append(goldReward);
    }

    if (!itemRewards.isEmpty()) {
      for (Map.Entry<GameItem, Integer> entry : itemRewards.entrySet()) {
        if (desc.length() > 0)
          desc.append(", ");
        desc.append(entry.getKey().getName()).append(" x").append(entry.getValue());
      }
    }

    return desc.length() > 0 ? desc.toString() : "보상 없음";
  }

  // Getters and Setters
  public int getExpReward() {
    return expReward;
  }

  public void setExpReward(int expReward) {
    this.expReward = expReward;
  }

  public int getGoldReward() {
    return goldReward;
  }

  public void setGoldReward(int goldReward) {
    this.goldReward = goldReward;
  }

  public Map<GameItem, Integer> getItemRewards() {
    return new HashMap<>(itemRewards);
  }

  public void setItemRewards(Map<GameItem, Integer> itemRewards) {
    this.itemRewards = itemRewards != null ? new HashMap<>(itemRewards) : new HashMap<>();
  }

  /**
   * 특정 아이템의 보상 수량을 반환합니다.
   */
  public int getItemQuantity(GameItem item) {
    return itemRewards.getOrDefault(item, 0);
  }

  /**
   * 첫 번째 아이템 보상을 반환합니다 (호환성을 위해)
   */
  public GameItem getFirstItem() {
    return itemRewards.keySet().stream().findFirst().orElse(null);
  }

  /**
   * 첫 번째 아이템의 수량을 반환합니다 (호환성을 위해)
   */
  public int getFirstItemQuantity() {
    GameItem firstItem = getFirstItem();
    return firstItem != null ? itemRewards.get(firstItem) : 0;
  }
}
