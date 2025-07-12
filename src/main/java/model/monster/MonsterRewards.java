package model.monster;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MonsterRewards {
  private int exp;
  private int gold;
  private List<DropItem> dropItems;

  @JsonCreator
  public MonsterRewards(
//@formatter:off
  @JsonProperty("exp") int exp
, @JsonProperty("gold") int gold
, @JsonProperty("dropItems") List<DropItem> dropItems
//@formatter:on
  ) {
    this.exp = exp;
    this.gold = gold;
    this.dropItems = dropItems != null ? dropItems : List.of();
  }

  // Getters
  public int getExp() {
    return exp;
  }

  public int getGold() {
    return gold;
  }

  public List<DropItem> getDropItems() {
    return dropItems;
  }
}


