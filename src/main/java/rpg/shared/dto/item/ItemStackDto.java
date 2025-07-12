package rpg.shared.dto.item;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemStackDto {
  private GameItemDto item;
  private int quantity;

  public ItemStackDto() {}

  @JsonCreator
  public ItemStackDto(
//@formatter:off
  @JsonProperty("item") GameItemDto item
, @JsonProperty("quantity") int quantity
//@formatter:on
  ) {
    this.item = item;
    this.quantity = quantity;
  }

  public GameItemDto getItem() {
    return item;
  }

  public void setItem(GameItemDto item) {
    this.item = item;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

}
