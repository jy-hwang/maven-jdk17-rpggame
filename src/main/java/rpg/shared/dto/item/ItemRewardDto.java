package rpg.shared.dto.item;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 아이템 보상 정보를 담는 DTO (Map Key 문제 해결용)
 */
public class ItemRewardDto {
    @JsonProperty("item")
    private GameItemDto item;
    
    @JsonProperty("quantity")
    private int quantity;

    // 기본 생성자
    public ItemRewardDto() {}

    // 생성자
    public ItemRewardDto(GameItemDto item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    // Getters and Setters
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
