package rpg.domain.skill;

public enum SkillType {
  ATTACK("공격"), HEAL("치유"), BUFF("강화"), DEBUFF("약화");

  private final String displayName;

  SkillType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
