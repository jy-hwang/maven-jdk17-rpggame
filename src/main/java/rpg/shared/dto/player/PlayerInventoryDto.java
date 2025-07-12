package rpg.shared.dto.player;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rpg.shared.dto.item.GameItemDto;
import rpg.shared.dto.item.ItemStackDto;

public class PlayerInventoryDto {
  private List<ItemStackDto> items;
  private int maxSlots;
  private GameItemDto equippedWeapon;
  private GameItemDto equippedArmor;
  private GameItemDto equippedAccessory;

  public PlayerInventoryDto() {
    this.items = new ArrayList<>();
  }

  @JsonCreator
  public PlayerInventoryDto(
//@formatter:off
  @JsonProperty("items") List<ItemStackDto> items
, @JsonProperty("maxSlots") int maxSlots
, @JsonProperty("equippedWeapon") GameItemDto equippedWeapon
, @JsonProperty("equippedArmor") GameItemDto equippedArmor
, @JsonProperty("equippedAccessory") GameItemDto equippedAccessory
//@formatter:on      
  ) {
    this.items = items != null ? items : new ArrayList<>();
    this.maxSlots = maxSlots;
    this.equippedWeapon = equippedWeapon;
    this.equippedArmor = equippedArmor;
    this.equippedAccessory = equippedAccessory;
  }

  // Getters and Setters
  public List<ItemStackDto> getItems() {
    return items;
  }

  public void setItems(List<ItemStackDto> items) {
    this.items = items;
  }

  public int getMaxSlots() {
    return maxSlots;
  }

  public void setMaxSlots(int maxSlots) {
    this.maxSlots = maxSlots;
  }

  public GameItemDto getEquippedWeapon() {
    return equippedWeapon;
  }

  public void setEquippedWeapon(GameItemDto equippedWeapon) {
    this.equippedWeapon = equippedWeapon;
  }

  public GameItemDto getEquippedArmor() {
    return equippedArmor;
  }

  public void setEquippedArmor(GameItemDto equippedArmor) {
    this.equippedArmor = equippedArmor;
  }

  public GameItemDto getEquippedAccessory() {
    return equippedAccessory;
  }

  public void setEquippedAccessory(GameItemDto equippedAccessory) {
    this.equippedAccessory = equippedAccessory;
  }
}
