package rpg.domain.quest;

/**
 * 퀘스트 아이템 보상 데이터
 */
public class QuestItemReward {
  private String itemId;
  private int quantity;
  private String rarity;

  public QuestItemReward() {}

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public String getRarity() {
    return rarity;
  }

  public void setRarity(String rarity) {
    this.rarity = rarity;
  }
}
