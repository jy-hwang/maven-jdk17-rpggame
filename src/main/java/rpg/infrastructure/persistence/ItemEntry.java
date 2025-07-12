package rpg.infrastructure.persistence;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

// === 내부 클래스들 ===
public class ItemEntry {
  private final String itemId;
  private final int quantity;

  @JsonCreator
  public ItemEntry(
//@formatter:off
  @JsonProperty("itemId") String itemId 
, @JsonProperty("quantity") int quantity
//@formatter:on            
  ) {
    this.itemId = itemId;
    this.quantity = quantity;
  }

  public String getItemId() {
    return itemId;
  }

  public int getQuantity() {
    return quantity;
  }
}
