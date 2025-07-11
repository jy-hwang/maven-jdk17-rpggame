package model.item;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GameEffectData {
  private final String type;
  private final Integer value;
  private final Boolean isPercentage;
  private final String statusType;
  
  @JsonCreator
  public GameEffectData(
          @JsonProperty("type") String type,
          @JsonProperty("value") Integer value,
          @JsonProperty("isPercentage") Boolean isPercentage,
          @JsonProperty("statusType") String statusType) {
      this.type = type;
      this.value = value;
      this.isPercentage = isPercentage;
      this.statusType = statusType;
  }
  
  // Getters
  public String getType() { return type; }
  public Integer getValue() { return value; }
  public Boolean getIsPercentage() { return isPercentage; }
  public String getStatusType() { return statusType; }
}
