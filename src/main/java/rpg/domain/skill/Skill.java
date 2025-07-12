package rpg.domain.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rpg.domain.monster.Monster;
import rpg.domain.player.Player;
import rpg.shared.constant.GameConstants;

/**
 * 캐릭터 스킬을 나타내는 클래스
 */
public class Skill {
  private static final Logger logger = LoggerFactory.getLogger(Skill.class);

  private String name;
  private String description;
  private SkillType type;
  private int requiredLevel;
  private int manaCost;
  private int cooldown; // 쿨다운 턴 수
  private double damageMultiplier; // 공격 스킬의 데미지 배율
  private int healAmount; // 힐 스킬의 회복량
  private int buffDuration; // 버프 지속 턴 수


  @JsonCreator
  public Skill(
//@formatter:off
  @JsonProperty("name") String name
, @JsonProperty("description") String description
, @JsonProperty("type") SkillType type
, @JsonProperty("requiredLevel") int requiredLevel
, @JsonProperty("manaCost") int manaCost
, @JsonProperty("cooldown") int cooldown
, @JsonProperty("damageMultiplier") double damageMultiplier
, @JsonProperty("healAmount") int healAmount
, @JsonProperty("buffDuration") int buffDuration
//@formatter:off
      ) {
    this.name = name;
    this.description = description;
    this.type = type;
    this.requiredLevel = requiredLevel;
    this.manaCost = manaCost;
    this.cooldown = cooldown;
    this.damageMultiplier = damageMultiplier;
    this.healAmount = healAmount;
    this.buffDuration = buffDuration;
  }

  /**
   * 스킬을 사용합니다.
   * 
   * @param caster 스킬 시전자
   * @param target 대상 (몬스터)
   * @return 스킬 사용 결과
   */
  public SkillResult useSkill(Player caster, Monster target) {
    if (caster.getMana() < manaCost) {
      return new SkillResult(false, "마나가 부족합니다!", 0);
    }

    caster.useMana(manaCost);

    switch (type) {
      case ATTACK:
        return useAttackSkill(caster, target);
      case HEAL:
        return useHealSkill(caster);
      case BUFF:
        return useBuffSkill(caster);
      case DEBUFF:
        return useDebuffSkill(target);
      default:
        return new SkillResult(false, "알 수 없는 스킬 타입입니다.", GameConstants.NUMBER_ZERO);
    }
  }

  private SkillResult useAttackSkill(Player caster, Monster target) {
    int damage = (int) (caster.getAttack() * damageMultiplier);
    target.takeDamage(damage);

    String message = String.format("%s이(가) %s을(를) 사용하여 %s에게 %d의 데미지를 입혔습니다!", caster.getName(), name, target.getName(), damage);

    logger.debug("공격 스킬 사용: {} -> {} (데미지: {})", name, target.getName(), damage);
    return new SkillResult(true, message, damage);
  }

  private SkillResult useHealSkill(Player caster) {
    int oldHp = caster.getHp();
    caster.heal(healAmount);
    int actualHeal = caster.getHp() - oldHp;

    String message = String.format("%s이(가) %s을(를) 사용하여 %d HP를 회복했습니다!", caster.getName(), name, actualHeal);

    logger.debug("힐 스킬 사용: {} (회복량: {})", name, actualHeal);
    return new SkillResult(true, message, actualHeal);
  }

  private SkillResult useBuffSkill(Player caster) {
    // 버프 효과는 캐릭터의 버프 시스템에서 처리
    String message = String.format("%s이(가) %s을(를) 사용했습니다!", caster.getName(), name);

    logger.debug("버프 스킬 사용: {}", name);
    return new SkillResult(true, message, GameConstants.NUMBER_ZERO);
  }

  private SkillResult useDebuffSkill(Monster target) {
    String message = String.format("%s이(가) %s의 영향을 받았습니다!", target.getName(), name);

    logger.debug("디버프 스킬 사용: {} -> {}", name, target.getName());
    return new SkillResult(true, message, GameConstants.NUMBER_ZERO);
  }

  public String getSkillInfo() {
    StringBuilder info = new StringBuilder();
    info.append(String.format("[%s] %s", type.getDisplayName(), name)).append("\n");
    info.append("설명: ").append(description).append("\n");
    info.append("필요 레벨: ").append(requiredLevel).append("\n");
    info.append("마나 소모: ").append(manaCost).append("\n");
    info.append("쿨다운: ").append(cooldown).append("턴\n");

    switch (type) {
      case ATTACK:
        info.append("데미지 배율: ").append((int) (damageMultiplier * 100)).append("%");
        break;
      case HEAL:
        info.append("회복량: ").append(healAmount).append(" HP");
        break;
      case BUFF:
      case DEBUFF:
        info.append("지속 시간: ").append(buffDuration).append("턴");
        break;
    }

    return info.toString();
  }

  // Getters
  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public SkillType getType() {
    return type;
  }

  public int getRequiredLevel() {
    return requiredLevel;
  }

  public int getManaCost() {
    return manaCost;
  }

  public int getCooldown() {
    return cooldown;
  }

  public double getDamageMultiplier() {
    return damageMultiplier;
  }

  public int getHealAmount() {
    return healAmount;
  }

  public int getBuffDuration() {
    return buffDuration;
  }

  /**
   * 스킬 사용 결과를 나타내는 클래스
   */
  public static class SkillResult {
    private final boolean success;
    private final String message;
    private final int value; // 데미지 또는 회복량

    public SkillResult(boolean success, String message, int value) {
      this.success = success;
      this.message = message;
      this.value = value;
    }

    public boolean isSuccess() {
      return success;
    }

    public String getMessage() {
      return message;
    }

    public int getValue() {
      return value;
    }
  }
}
