package dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SkillCooldownDto {
  private String skillName;
  private int remainingTurns;
  
  public SkillCooldownDto() {}
  
  @JsonCreator
  public SkillCooldownDto(
//@formatter:off
  @JsonProperty("skillName") String skillName
, @JsonProperty("remainingTurns") int remainingTurns
//@formatter:on
  ) {
      this.skillName = skillName;
      this.remainingTurns = remainingTurns;
  }

  public String getSkillName() {
    return skillName;
  }

  public void setSkillName(String skillName) {
    this.skillName = skillName;
  }

  public int getRemainingTurns() {
    return remainingTurns;
  }

  public void setRemainingTurns(int remainingTurns) {
    this.remainingTurns = remainingTurns;
  }
  
}
