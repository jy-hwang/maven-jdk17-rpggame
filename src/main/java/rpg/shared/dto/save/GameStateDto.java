package rpg.shared.dto.save;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GameStateDto {
  private int totalPlayTime;
  private int monstersKilled;
  private int questsCompleted;
  private String currentLocation;

  public GameStateDto() {}

  @JsonCreator
  public GameStateDto(
//@formatter:off
@JsonProperty("totalPlayTime") int totalPlayTime
, @JsonProperty("monstersKilled") int monstersKilled
, @JsonProperty("questsCompleted") int questsCompleted
, @JsonProperty("currentLocation") String currentLocation
//@formatter:on
  ) {
    this.totalPlayTime = totalPlayTime;
    this.monstersKilled = monstersKilled;
    this.questsCompleted = questsCompleted;
    this.currentLocation = currentLocation;
  }

  public int getTotalPlayTime() {
    return totalPlayTime;
  }

  public void setTotalPlayTime(int totalPlayTime) {
    this.totalPlayTime = totalPlayTime;
  }

  public int getMonstersKilled() {
    return monstersKilled;
  }

  public void setMonstersKilled(int monstersKilled) {
    this.monstersKilled = monstersKilled;
  }

  public int getQuestsCompleted() {
    return questsCompleted;
  }

  public void setQuestsCompleted(int questsCompleted) {
    this.questsCompleted = questsCompleted;
  }

  public String getCurrentLocation() {
    return currentLocation;
  }

  public void setCurrentLocation(String currentLocation) {
    this.currentLocation = currentLocation;
  }

}
