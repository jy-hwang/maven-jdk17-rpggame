package rpg.domain.quest;

import java.util.List;

/**
 * 퀘스트 보상 데이터
 */
public class QuestRewardData {
  private int experience;
  private int gold;
  private List<QuestItemReward> items;

  public QuestRewardData() {}

  public int getExperience() {
    return experience;
  }

  public void setExperience(int experience) {
    this.experience = experience;
  }

  public int getGold() {
    return gold;
  }

  public void setGold(int gold) {
    this.gold = gold;
  }

  public List<QuestItemReward> getItems() {
    return items;
  }

  public void setItems(List<QuestItemReward> items) {
    this.items = items;
  }
}
