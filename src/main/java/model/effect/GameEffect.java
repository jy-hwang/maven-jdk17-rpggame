package model.effect;

import model.GameCharacter;

public interface GameEffect {
  /**
   * 효과를 적용합니다
   * @param target 효과를 받을 대상
   * @return 효과 적용 성공 여부
   */
  boolean apply(GameCharacter target);
  
  /**
   * 효과 설명을 반환합니다
   */
  String getDescription();
  
  /**
   * 효과 타입을 반환합니다
   */
  GameEffectType getType();
  
  /**
   * 효과 값을 반환합니다
   */
  int getValue();
}
