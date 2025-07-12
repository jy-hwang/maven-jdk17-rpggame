package rpg.shared.dto.item;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GameEffectDto {
  private String type; // "HEAL_HP", "HEAL_MP", "GAIN_EXP" 등
  private int value;
  private String description;

  public GameEffectDto() {}

  @JsonCreator
  public GameEffectDto(
//@formatter:off
  @JsonProperty("type") String type
, @JsonProperty("value") int value
, @JsonProperty("description") String description
//@formatter:on
  ) {
    this.type = type;
    this.value = value;
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

}
