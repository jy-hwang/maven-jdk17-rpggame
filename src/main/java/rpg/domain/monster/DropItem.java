package rpg.domain.monster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 드롭 아이템 클래스
 */
public class DropItem {
  private String itemId;
  private double dropRate; // 0.0 ~ 1.0
  private int minQuantity;
  private int maxQuantity;

  @JsonCreator
  public DropItem(
//@formatter:off
  @JsonProperty("itemId") String itemId
, @JsonProperty("dropRate") double dropRate
, @JsonProperty("minQuantity") int minQuantity
, @JsonProperty("maxQuantity") int maxQuantity
//@formatter:on
  ) {
    this.itemId = itemId;
    this.dropRate = dropRate;
    this.minQuantity = minQuantity;
    this.maxQuantity = maxQuantity;
  }

  // Getters
  public String getItemId() {
    return itemId;
  }

  public double getDropRate() {
    return dropRate;
  }

  public int getMinQuantity() {
    return minQuantity;
  }

  public int getMaxQuantity() {
    return maxQuantity;
  }
}
