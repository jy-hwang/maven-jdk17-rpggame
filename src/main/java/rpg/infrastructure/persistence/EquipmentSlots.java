package rpg.infrastructure.persistence;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EquipmentSlots {
  private final String weapon;
  private final String armor;
  private final String accessory;

  @JsonCreator
  public EquipmentSlots(
//@formatter:off
  @JsonProperty("weapon") String weapon
, @JsonProperty("armor") String armor
, @JsonProperty("accessory") String accessory
 //@formatter:on                 
  ) {
    this.weapon = weapon;
    this.armor = armor;
    this.accessory = accessory;
  }

  public String getWeapon() {
    return weapon;
  }

  public String getArmor() {
    return armor;
  }

  public String getAccessory() {
    return accessory;
  }
}
