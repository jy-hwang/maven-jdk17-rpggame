package model.item;

import java.util.ArrayList;
import java.util.List;
import model.effect.GameEffect;

/**
 * 소모품 아이템 클래스
 */
public class GameConsumable extends GameItem {
  private final List<GameEffect> effects;
  private final boolean stackable;
  private final int cooldown; // 쿨다운 시간 (턴)
  
  public GameConsumable(String name, String description, int value, ItemRarity rarity,
                       List<GameEffect> effects, boolean stackable, int cooldown) {
      super(name, description, value, rarity);
      this.effects = new ArrayList<>(effects);
      this.stackable = stackable;
      this.cooldown = cooldown;
  }
  
  /**
   * 아이템 사용
   */
  public boolean use(GameCharacter character) {
      if (character == null) {
          return false;
      }
      
      // 쿨다운 체크 (구현 시)
      if (isOnCooldown(character)) {
          System.out.println("⏰ " + getName() + "은(는) 아직 사용할 수 없습니다.");
          return false;
      }
      
      boolean anyEffectApplied = false;
      
      System.out.println("🧪 " + getName() + "을(를) 사용합니다.");
      
      // 모든 효과 적용
      for (GameEffect effect : effects) {
          if (effect.apply(character)) {
              anyEffectApplied = true;
          }
      }
      
      if (anyEffectApplied) {
          // 쿨다운 적용 (구현 시)
          applyCooldown(character);
          return true;
      } else {
          System.out.println("💫 효과가 없었습니다.");
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
      
      return effects.stream()
              .map(GameEffect::getDescription)
              .reduce((a, b) -> a + ", " + b)
              .orElse("효과 없음");
  }
  
  // Getters
  public List<GameEffect> getEffects() { return new ArrayList<>(effects); }
  public boolean isStackable() { return stackable; }
  public int getCooldown() { return cooldown; }
  
  // 쿨다운 관련 메서드들 (구현 시)
  private boolean isOnCooldown(GameCharacter character) {
      // TODO: 캐릭터의 아이템 쿨다운 상태 확인
      return false;
  }
  
  private void applyCooldown(GameCharacter character) {
      // TODO: 캐릭터에게 아이템 쿨다운 적용
  }
}
