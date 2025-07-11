package model.item;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GameItemRequirements {
  private final int minLevel;
  private final String jobType;
  
  @JsonCreator
  public GameItemRequirements(
          @JsonProperty("minLevel") Integer minLevel,
          @JsonProperty("jobType") String jobType) {
      this.minLevel = minLevel != null ? minLevel : 1;
      this.jobType = jobType != null ? jobType : "ALL";
  }
  
  public GameItemRequirements() {
      this(1, "ALL");
  }
  
  // Getters
  public int getMinLevel() { return minLevel; }
  public String getJobType() { return jobType; }
}
