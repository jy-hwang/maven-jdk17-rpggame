package model.item;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import config.BaseConstant;

public class GameItemRequirements {
  private final int minLevel;
  private final String jobType;

  @JsonCreator
  public GameItemRequirements(@JsonProperty("minLevel") Integer minLevel, @JsonProperty("jobType") String jobType) {
    this.minLevel = minLevel != null ? minLevel : BaseConstant.NUMBER_ONE;
    this.jobType = jobType != null ? jobType : "ALL";
  }

  public GameItemRequirements() {
    this(BaseConstant.NUMBER_ONE, "ALL");
  }

  // Getters
  public int getMinLevel() {
    return minLevel;
  }

  public String getJobType() {
    return jobType;
  }
}
