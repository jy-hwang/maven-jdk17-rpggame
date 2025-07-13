package rpg.shared.dto.quest;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import rpg.shared.dto.item.ItemRewardDto;

public class QuestRewardDto {
  @JsonProperty("expReward")
  private int expReward;

  @JsonProperty("goldReward")
  private int goldReward;

  @JsonProperty("itemRewards")
  private List<ItemRewardDto> itemRewards;

  // 기본 생성자
  public QuestRewardDto() {}

  // 생성자
  public QuestRewardDto(int expReward, int goldReward, List<ItemRewardDto> itemRewards) {
    this.expReward = expReward;
    this.goldReward = goldReward;
    this.itemRewards = itemRewards;
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

  public List<ItemRewardDto> getItemRewards() {
    return itemRewards;
  }

  public void setItemRewards(List<ItemRewardDto> itemRewards) {
    this.itemRewards = itemRewards;
  }

}
