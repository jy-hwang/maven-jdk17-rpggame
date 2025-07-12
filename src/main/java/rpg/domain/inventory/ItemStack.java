package rpg.domain.inventory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rpg.domain.item.GameItem;
import rpg.shared.constant.GameConstants;

/**
 * 아이템 스택 클래스
 */
public class ItemStack {
  private final GameItem item;
  private int quantity;

  // Jackson 역직렬화용 생성자 추가
  @JsonCreator
  public ItemStack(
//@formatter:off
@JsonProperty("item") GameItem item
, @JsonProperty("quantity") int quantity
//@formatter:on
  ) {
    this.item = item;
    this.quantity = quantity;
  }

  public void addQuantity(int amount) {
    this.quantity += amount;
  }

  public void removeQuantity(int amount) {
    this.quantity = Math.max(GameConstants.NUMBER_ZERO, this.quantity - amount);
  }

  public GameItem getItem() {
    return item;
  }

  public int getQuantity() {
    return quantity;
  }
}
