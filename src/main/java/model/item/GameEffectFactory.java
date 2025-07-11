package model.item;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import model.effect.CureStatusEffect;
import model.effect.GainExpEffect;
import model.effect.GameEffect;
import model.effect.HealHpEffect;
import model.effect.HealMpEffect;

public class GameEffectFactory {
  private static final Logger logger = LoggerFactory.getLogger(GameEffectFactory.class);
  
  /**
   * 효과 데이터로부터 실제 효과 객체들을 생성
   */
  public static List<GameEffect> createEffects(List<GameEffectData> effectDataList) {
      List<GameEffect> effects = new ArrayList<>();
      
      for (GameEffectData effectData : effectDataList) {
          try {
              GameEffect effect = createSingleEffect(effectData);
              if (effect != null) {
                  effects.add(effect);
              }
          } catch (Exception e) {
              logger.error("효과 생성 실패: {}", effectData.getType(), e);
          }
      }
      
      return effects;
  }
  
  /**
   * 단일 효과 데이터로부터 효과 객체 생성
   */
  private static GameEffect createSingleEffect(ItemData.EffectData effectData) {
      String type = effectData.getType();
      Integer value = effectData.getValue();
      Boolean isPercentage = effectData.getIsPercentage();
      String statusType = effectData.getStatusType();
      
      return switch (type.toUpperCase()) {
          case "HEAL_HP" -> {
              if (value == null) {
                  logger.warn("HEAL_HP 효과에 value가 없습니다.");
                  yield null;
              }
              yield new HealHpEffect(value, isPercentage != null ? isPercentage : false);
          }
          
          case "HEAL_MP" -> {
              if (value == null) {
                  logger.warn("HEAL_MP 효과에 value가 없습니다.");
                  yield null;
              }
              yield new HealMpEffect(value, isPercentage != null ? isPercentage : false);
          }
          
          case "CURE_STATUS" -> {
              if (statusType == null) {
                  logger.warn("CURE_STATUS 효과에 statusType이 없습니다.");
                  yield null;
              }
              yield new CureStatusEffect(statusType);
          }
          
          case "CURE_ALL_STATUS" -> new CureAllStatusEffect();
          
          case "GAIN_EXP" -> {
              if (value == null) {
                  logger.warn("GAIN_EXP 효과에 value가 없습니다.");
                  yield null;
              }
              yield new GainExpEffect(value);
          }
          
          case "BUFF_ATTACK" -> {
              if (value == null) {
                  logger.warn("BUFF_ATTACK 효과에 value가 없습니다.");
                  yield null;
              }
              yield new BuffAttackEffect(value, 5); // 기본 5턴
          }
          
          case "BUFF_DEFENSE" -> {
              if (value == null) {
                  logger.warn("BUFF_DEFENSE 효과에 value가 없습니다.");
                  yield null;
              }
              yield new BuffDefenseEffect(value, 5); // 기본 5턴
          }
          
          default -> {
              logger.warn("알 수 없는 효과 타입: {}", type);
              yield null;
          }
      };
  }
  
  /**
   * 간단한 효과 생성 (프로그래밍적 생성용)
   */
  public static GameEffect createSimpleEffect(String type, int value) {
      return switch (type.toUpperCase()) {
          case "HEAL_HP" -> new HealHpEffect(value, false);
          case "HEAL_MP" -> new HealMpEffect(value, false);
          case "GAIN_EXP" -> new GainExpEffect(value);
          case "CURE_POISON" -> new CureStatusEffect("POISON");
          case "CURE_PARALYSIS" -> new CureStatusEffect("PARALYSIS");
          case "CURE_ALL_STATUS" -> new CureAllStatusEffect();
          default -> throw new IllegalArgumentException("Unknown simple effect type: " + type);
      };
  }
}
