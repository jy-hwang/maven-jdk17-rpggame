package rpg.shared.dto.player;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SkillManagerDto {
  private List<SkillDto> learnedSkills = new ArrayList<>();
  private List<SkillCooldownDto> skillCooldowns = new ArrayList<>();

  public SkillManagerDto() {}

  @JsonCreator
  public SkillManagerDto(
//@formatter:off
  @JsonProperty("learnedSkills") List<SkillDto> learnedSkills
, @JsonProperty("skillCooldowns") List<SkillCooldownDto> skillCooldowns
//@formatter:off
  ) {
      this.learnedSkills = learnedSkills != null ? learnedSkills : new ArrayList<>();
      this.skillCooldowns = skillCooldowns != null ? skillCooldowns : new ArrayList<>();
  }

  public List<SkillDto> getLearnedSkills() {
    return learnedSkills;
  }

  public void setLearnedSkills(List<SkillDto> learnedSkills) {
    this.learnedSkills = learnedSkills;
  }

  public List<SkillCooldownDto> getSkillCooldowns() {
    return skillCooldowns;
  }

  public void setSkillCooldowns(List<SkillCooldownDto> skillCooldowns) {
    this.skillCooldowns = skillCooldowns;
  }
  
}
