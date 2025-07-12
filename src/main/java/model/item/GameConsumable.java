package model.item;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import config.BaseConstant;
import model.GameCharacter;
import model.effect.GameEffect;
import model.effect.GameEffectType;

/**
 * GameEffect 시스템을 사용하는 소비 아이템 클래스 (확장된 생성자 지원)
 */
public class GameConsumable extends GameItem {
  private static final Logger logger = LoggerFactory.getLogger(GameConsumable.class);

  private final List<GameEffect> effects;
  private final int cooldown; // 쿨다운 시간 (턴)

  /**
   * GameEffect 시스템을 사용하는 생성자 (권장)
   */
  public GameConsumable(String name, String description, int value, ItemRarity rarity, List<GameEffect> effects, int cooldown) {
    super(name, description, value, rarity);
    this.effects = new ArrayList<>(effects);
    this.cooldown = cooldown;

    logger.debug("GameConsumable 생성: {} (효과 {}개)", name, effects.size());
  }

  /**
   * 레거시 생성자 1 - 기존 시그니처 (hpRestore, expGain, stackable)
   * 
   * @deprecated GameEffect 시스템을 사용하는 생성자를 권장
   */
  @Deprecated
  public GameConsumable(String name, String description, int value, ItemRarity rarity, int hpRestore, int expGain, boolean stackable) {
    super(name, description, value, rarity);
    this.cooldown = BaseConstant.NUMBER_ZERO;

    // 레거시 파라미터를 효과로 변환
    this.effects = new ArrayList<>();
    if (hpRestore > BaseConstant.NUMBER_ZERO) {
      this.effects.add(new SimpleHealEffect("HP", hpRestore));
    }
    if (expGain > BaseConstant.NUMBER_ZERO) {
      this.effects.add(new SimpleExpEffect(expGain));
    }

    logger.debug("GameConsumable 생성 (레거시 1): {} (HP: {}, EXP: {})", name, hpRestore, expGain);
  }

  /**
   * 레거시 생성자 2 - QuestManager 호환 시그니처 (hpRestore, mpRestore, expGain, stackable)
   * 
   * @deprecated GameEffect 시스템을 사용하는 생성자를 권장
   */
  @Deprecated
  public GameConsumable(String name, String description, int value, ItemRarity rarity, int hpRestore, int mpRestore, int expGain, boolean stackable) {
    super(name, description, value, rarity);
    this.cooldown = BaseConstant.NUMBER_ZERO;

    // 레거시 파라미터를 효과로 변환
    this.effects = new ArrayList<>();
    if (hpRestore > BaseConstant.NUMBER_ZERO) {
      this.effects.add(new SimpleHealEffect("HP", hpRestore));
    }
    if (mpRestore > BaseConstant.NUMBER_ZERO) {
      this.effects.add(new SimpleHealEffect("MP", mpRestore));
    }
    if (expGain > BaseConstant.NUMBER_ZERO) {
      this.effects.add(new SimpleExpEffect(expGain));
    }

    logger.debug("GameConsumable 생성 (레거시 2): {} (HP: {}, MP: {}, EXP: {})", name, hpRestore, mpRestore, expGain);
  }

  /**
   * 간단한 단일 효과 생성자 (편의용)
   */
  public GameConsumable(String name, String description, int value, ItemRarity rarity, String effectType, int effectValue) {
    super(name, description, value, rarity);
    this.cooldown = BaseConstant.NUMBER_ZERO;
    this.effects = new ArrayList<>();

    // 효과 타입에 따라 적절한 효과 생성
    switch (effectType.toUpperCase()) {
      case "HP", "HEAL_HP" -> this.effects.add(new SimpleHealEffect("HP", effectValue));
      case "MP", "HEAL_MP" -> this.effects.add(new SimpleHealEffect("MP", effectValue));
      case "EXP", "GAIN_EXP" -> this.effects.add(new SimpleExpEffect(effectValue));
      default -> {
        logger.warn("알 수 없는 효과 타입: {}", effectType);
        this.effects.add(new SimpleHealEffect("HP", effectValue)); // 기본값
      }
    }

    logger.debug("GameConsumable 생성 (단일 효과): {} ({}:{})", name, effectType, effectValue);
  }

  /**
   * 아이템 사용
   */
  public boolean use(GameCharacter character) {
    if (character == null) {
      logger.warn("GameConsumable.use() - character가 null입니다");
      return false;
    }

    // 쿨다운 체크 (나중에 구현)
    if (isOnCooldown(character)) {
      System.out.println("⏰ " + getName() + "은(는) 아직 사용할 수 없습니다.");
      return false;
    }

    boolean anyEffectApplied = false;

    System.out.println("🧪 " + getName() + "을(를) 사용합니다.");
    logger.info("아이템 사용: {} -> {}", character.getName(), getName());

    // 모든 효과 적용
    for (GameEffect effect : effects) {
      try {
        if (effect.apply(character)) {
          anyEffectApplied = true;
          logger.debug("효과 적용 성공: {}", effect.getClass().getSimpleName());
        } else {
          logger.debug("효과 적용 실패 또는 무효: {}", effect.getClass().getSimpleName());
        }
      } catch (Exception e) {
        logger.error("효과 적용 중 오류: {}", effect.getClass().getSimpleName(), e);
      }
    }

    if (anyEffectApplied) {
      // 쿨다운 적용 (나중에 구현)
      applyCooldown(character);
      logger.info("아이템 사용 완료: {}", getName());
      return true;
    } else {
      System.out.println("💫 효과가 없었습니다.");
      logger.debug("아이템 사용했지만 효과 없음: {}", getName());
      return false;
    }
  }

  /**
   * 효과 설명 생성
   */
  public String getEffectsDescription() {
    if (effects.isEmpty()) {
      return "효과 없음";
    }

    StringBuilder description = new StringBuilder();
    for (int i = 0; i < effects.size(); i++) {
      if (i > 0) {
        description.append(", ");
      }
      description.append(effects.get(i).getDescription());
    }

    return description.toString();
  }

  /**
   * 레거시 지원: HP 회복량 반환
   * 
   * @deprecated 효과 시스템 사용 권장
   */
  @Deprecated
  public int getHpRestore() {
    // 효과 중에서 HP 회복 효과 찾기
    for (GameEffect effect : effects) {
      if (effect instanceof SimpleHealEffect healEffect && "HP".equals(healEffect.getType())) {
        return healEffect.getValue();
      }
    }
    return 0;
  }

  /**
   * 레거시 지원: MP 회복량 반환
   * 
   * @deprecated 효과 시스템 사용 권장
   */
  @Deprecated
  public int getMpRestore() {
    // 효과 중에서 MP 회복 효과 찾기
    for (GameEffect effect : effects) {
      if (effect instanceof SimpleHealEffect healEffect && "MP".equals(healEffect.getType())) {
        return healEffect.getValue();
      }
    }
    return 0;
  }

  /**
   * 레거시 지원: 경험치 획득량 반환
   * 
   * @deprecated 효과 시스템 사용 권장
   */
  @Deprecated
  public int getExpGain() {
    // 효과 중에서 경험치 효과 찾기
    for (GameEffect effect : effects) {
      if (effect instanceof SimpleExpEffect expEffect) {
        return expEffect.getValue();
      }
    }
    return 0;
  }

  // Getters
  public List<GameEffect> getEffects() {
    return new ArrayList<>(effects);
  }

  public int getCooldown() {
    return cooldown;
  }

  // 쿨다운 관련 메서드들 (나중에 구현)
  private boolean isOnCooldown(GameCharacter character) {
    // TODO: 캐릭터의 아이템 쿨다운 상태 확인
    return false;
  }

  private void applyCooldown(GameCharacter character) {
    // TODO: 캐릭터에게 아이템 쿨다운 적용
    if (cooldown > 0) {
      logger.debug("쿨다운 적용: {} ({}턴)", getName(), cooldown);
    }
  }

  @Override
  public String toString() {
    return String.format("GameConsumable{name='%s', effects=%d}", getName(), effects.size());
  }

  @Override
  public String getItemInfo() {
    return String.format("%s\n효과: %s\n가격: %d골드", getDescription(), getEffectsDescription(), getValue());
  }

  // ==================== 임시 효과 클래스들 (레거시 지원용) ====================

  /**
   * 간단한 회복 효과 (레거시 지원용)
   */
  private static class SimpleHealEffect implements GameEffect {
    private final String type;
    private final int value;

    public SimpleHealEffect(String type, int value) {
      this.type = type;
      this.value = value;
    }

    @Override
    public boolean apply(GameCharacter target) {
      switch (type) {
        case "HP" -> {
          int oldHp = target.getHp();
          target.heal(value);
          int healedAmount = target.getHp() - oldHp;
          if (healedAmount > 0) {
            System.out.println("💚 " + healedAmount + " HP 회복!");
            return true;
          }
        }
        case "MP" -> {
          int oldMp = target.getMana();
          target.restoreMana(value);
          int restoredAmount = target.getMana() - oldMp;
          if (restoredAmount > 0) {
            System.out.println("💙 " + restoredAmount + " MP 회복!");
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public String getDescription() {
      return type + " +" + value;
    }

    public int getValue() {
      return value;
    }

    @Override
    public GameEffectType getType() {
      return GameEffectType.GAIN_EXP;
    }
  }

  /**
   * 간단한 경험치 효과 (레거시 지원용)
   */
  private static class SimpleExpEffect implements GameEffect {
    private final int value;

    public SimpleExpEffect(int value) {
      this.value = value;
    }

    @Override
    public boolean apply(GameCharacter target) {
      target.gainExp(value);
      System.out.println("📈 " + value + " 경험치 획득!");
      return true;
    }

    @Override
    public String getDescription() {
      return "경험치 +" + value;
    }

    public int getValue() {
      return value;
    }

    @Override
    public GameEffectType getType() {

      return GameEffectType.GAIN_EXP;
    }
  }
}
