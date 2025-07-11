package model.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import config.BaseConstant;
import model.effect.GainExpEffect;
import model.effect.GameEffect;
import model.effect.GameEffectType;
import model.effect.HealHpEffect;
import model.effect.HealMpEffect;
import model.effect.PlaceholderEffect;
import model.item.GameEffectData;

/**
 * 게임 효과 팩토리 (최신 interface 기반 버전) GameEffect interface를 기반으로 모든 효과를 생성
 */
public class GameEffectFactory {
  private static final Logger logger = LoggerFactory.getLogger(GameEffectFactory.class);

  // 효과 생성 함수 맵 (타입, 값) -> GameEffect
  private static final Map<GameEffectType, BiFunction<Integer, Boolean, GameEffect>> EFFECT_CREATORS = new HashMap<>();

  // 정적 초기화
  static {
    initializeEffectCreators();
  }

  /**
   * 효과 생성 함수들 초기화
   */
  private static void initializeEffectCreators() {
    logger.debug("GameEffectFactory 효과 생성 함수 초기화 중...");

    // 구현된 효과들
    EFFECT_CREATORS.put(GameEffectType.HEAL_HP, (value, percentage) -> new HealHpEffect(value, percentage));
    EFFECT_CREATORS.put(GameEffectType.HEAL_MP, (value, percentage) -> new HealMpEffect(value, percentage));
    EFFECT_CREATORS.put(GameEffectType.HEAL_HP_PERCENT, (value, percentage) -> new HealHpEffect(value, true));
    EFFECT_CREATORS.put(GameEffectType.HEAL_MP_PERCENT, (value, percentage) -> new HealMpEffect(value, true));
    EFFECT_CREATORS.put(GameEffectType.GAIN_EXP, (value, percentage) -> new GainExpEffect(value));

    // 미구현 효과들 (플레이스홀더)
    for (GameEffectType type : GameEffectType.values()) {
      if (!EFFECT_CREATORS.containsKey(type)) {
        EFFECT_CREATORS.put(type, (value, percentage) -> new PlaceholderEffect(type, value));
      }
    }

    logger.info("GameEffectFactory 초기화 완료: {}개 효과 타입 지원", EFFECT_CREATORS.size());
  }

  /**
   * 효과 데이터 리스트로부터 실제 효과 객체들 생성 (메인 메서드)
   */
  public static List<GameEffect> createEffects(List<GameEffectData> effectDataList) {
    if (effectDataList == null || effectDataList.isEmpty()) {
      logger.debug("효과 데이터가 없음");
      return new ArrayList<>();
    }

    List<GameEffect> effects = new ArrayList<>();

    for (GameEffectData effectData : effectDataList) {
      try {
        GameEffect effect = createSingleEffect(effectData);
        if (effect != null) {
          effects.add(effect);
          logger.debug("효과 생성 성공: {} (값: {})", effectData.getType(), effectData.getValue());
        } else {
          logger.warn("효과 생성 실패: {} (값: {})", effectData.getType(), effectData.getValue());
        }
      } catch (Exception e) {
        logger.error("효과 생성 중 예외 발생: {} (값: {})", effectData.getType(), effectData.getValue(), e);
      }
    }

    logger.debug("총 {}개의 효과 생성 완료 (요청: {}개)", effects.size(), effectDataList.size());
    return effects;
  }

  /**
   * 단일 효과 생성
   */
  private static GameEffect createSingleEffect(GameEffectData effectData) {
    if (effectData == null) {
      logger.warn("효과 데이터가 null입니다");
      return null;
    }

    String typeStr = effectData.getType();
    int value = effectData.getValue();

    // 입력 검증
    if (typeStr == null || typeStr.trim().isEmpty()) {
      logger.warn("효과 타입이 비어있음");
      return null;
    }

    if (value < BaseConstant.NUMBER_ZERO) {
      logger.warn("효과 값이 음수: {} (값: {})", typeStr, value);
      return null;
    }

    // GameEffectType으로 변환
    GameEffectType effectType = GameEffectType.fromString(typeStr);
    if (effectType == null) {
      logger.warn("지원하지 않는 효과 타입: {}", typeStr);
      return new PlaceholderEffect(GameEffectType.HEAL_HP, value, "알 수 없는 효과: " + typeStr);
    }

    // 효과 생성
    BiFunction<Integer, Boolean, GameEffect> creator = EFFECT_CREATORS.get(effectType);
    if (creator == null) {
      logger.warn("효과 생성자를 찾을 수 없음: {}", effectType);
      return new PlaceholderEffect(effectType, value);
    }

    try {
      boolean isPercentage = effectType == GameEffectType.HEAL_HP_PERCENT || effectType == GameEffectType.HEAL_MP_PERCENT;
      GameEffect effect = creator.apply(value, isPercentage);
      logger.debug("효과 생성: {} -> {}", effectType, effect.getClass().getSimpleName());
      return effect;
    } catch (Exception e) {
      logger.error("효과 생성 함수 실행 중 오류: {}", effectType, e);
      return new PlaceholderEffect(effectType, value);
    }
  }

  /**
   * 프로그래밍적 효과 생성 (간편 메서드)
   */
  public static GameEffect createSimpleEffect(GameEffectType type, int value) {
    return createSimpleEffect(type, value, false);
  }

  /**
   * 프로그래밍적 효과 생성 (퍼센트 옵션 포함)
   */
  public static GameEffect createSimpleEffect(GameEffectType type, int value, boolean isPercentage) {
    if (type == null) {
      throw new IllegalArgumentException("효과 타입이 null입니다");
    }

    if (value < BaseConstant.NUMBER_ZERO) {
      throw new IllegalArgumentException("효과 값이 음수입니다: " + value);
    }

    BiFunction<Integer, Boolean, GameEffect> creator = EFFECT_CREATORS.get(type);
    if (creator == null) {
      throw new IllegalArgumentException("지원하지 않는 효과 타입: " + type);
    }

    try {
      return creator.apply(value, isPercentage);
    } catch (Exception e) {
      throw new RuntimeException("효과 생성 실패: " + type, e);
    }
  }

  /**
   * 문자열 타입으로 효과 생성 (하위 호환성)
   */
  public static GameEffect createSimpleEffect(String typeStr, int value) {
    GameEffectType type = GameEffectType.fromString(typeStr);
    if (type == null) {
      throw new IllegalArgumentException("지원하지 않는 효과 타입: " + typeStr);
    }
    return createSimpleEffect(type, value);
  }

  /**
   * 다중 효과 생성 (프로그래밍적)
   */
  public static List<GameEffect> createMultipleEffects(Map<GameEffectType, Integer> effectMap) {
    if (effectMap == null || effectMap.isEmpty()) {
      return new ArrayList<>();
    }

    List<GameEffect> effects = new ArrayList<>();

    for (Map.Entry<GameEffectType, Integer> entry : effectMap.entrySet()) {
      try {
        GameEffect effect = createSimpleEffect(entry.getKey(), entry.getValue());
        effects.add(effect);
      } catch (Exception e) {
        logger.error("다중 효과 생성 중 오류: {} = {}", entry.getKey(), entry.getValue(), e);
      }
    }

    return effects;
  }

  /**
   * HP 회복 효과 생성 (편의 메서드)
   */
  public static GameEffect createHealHpEffect(int amount) {
    return new HealHpEffect(amount, false);
  }

  /**
   * HP 퍼센트 회복 효과 생성 (편의 메서드)
   */
  public static GameEffect createHealHpPercentEffect(int percentage) {
    return new HealHpEffect(percentage, true);
  }

  /**
   * MP 회복 효과 생성 (편의 메서드)
   */
  public static GameEffect createHealMpEffect(int amount) {
    return new HealMpEffect(amount, false);
  }

  /**
   * MP 퍼센트 회복 효과 생성 (편의 메서드)
   */
  public static GameEffect createHealMpPercentEffect(int percentage) {
    return new HealMpEffect(percentage, true);
  }

  /**
   * 경험치 획득 효과 생성 (편의 메서드)
   */
  public static GameEffect createGainExpEffect(int amount) {
    return new GainExpEffect(amount);
  }

  /**
   * 지원하는 효과 타입 목록 반환
   */
  public static Set<GameEffectType> getSupportedEffectTypes() {
    return Collections.unmodifiableSet(EFFECT_CREATORS.keySet());
  }

  /**
   * 효과 타입 지원 여부 확인
   */
  public static boolean isEffectTypeSupported(GameEffectType type) {
    return type != null && EFFECT_CREATORS.containsKey(type);
  }

  /**
   * 구현된 효과 타입만 반환
   */
  public static Set<GameEffectType> getImplementedEffectTypes() {
    return EFFECT_CREATORS.keySet().stream().filter(GameEffectType::isImplemented).collect(java.util.stream.Collectors.toSet());
  }

  /**
   * 랜덤 효과 생성 (테스트용)
   */
  public static GameEffect createRandomEffect() {
    List<GameEffectType> implementedTypes = new ArrayList<>(getImplementedEffectTypes());
    if (implementedTypes.isEmpty()) {
      return createHealHpEffect(BaseConstant.NUMBER_FIFTY); // 기본값
    }

    Random random = new Random();
    GameEffectType randomType = implementedTypes.get(random.nextInt(implementedTypes.size()));
    int randomValue = BaseConstant.NUMBER_TWENTY + random.nextInt(BaseConstant.NUMBER_EIGHTY_ONE); // 20-100

    return createSimpleEffect(randomType, randomValue);
  }

  /**
   * 레벨에 맞는 랜덤 효과 생성
   */
  public static GameEffect createRandomEffectForLevel(int level) {
    List<GameEffectType> availableTypes =
        getImplementedEffectTypes().stream().filter(type -> level >= type.getMinimumLevel()).collect(java.util.stream.Collectors.toList());

    if (availableTypes.isEmpty()) {
      return createHealHpEffect(level * BaseConstant.NUMBER_TEN); // 기본값
    }

    Random random = new Random();
    GameEffectType randomType = availableTypes.get(random.nextInt(availableTypes.size()));

    // 레벨에 따른 효과 강도 조정
    int baseValue = switch (randomType) {
      case HEAL_HP, HEAL_MP -> BaseConstant.NUMBER_THIRTY + (level * BaseConstant.NUMBER_FIVE);
      case HEAL_HP_PERCENT, HEAL_MP_PERCENT -> Math.min(BaseConstant.NUMBER_FIFTY, BaseConstant.NUMBER_TEN + (level * BaseConstant.NUMBER_TWO));
      case GAIN_EXP -> BaseConstant.NUMBER_FIFTY + (level * BaseConstant.NUMBER_TEN);
      default -> BaseConstant.NUMBER_FIFTY;
    };

    return createSimpleEffect(randomType, baseValue);
  }

  /**
   * 효과 검증
   */
  public static boolean validateEffect(GameEffectData effectData) {
    if (effectData == null)
      return false;

    String typeStr = effectData.getType();
    int value = effectData.getValue();

    // 기본 검증
    if (typeStr == null || typeStr.trim().isEmpty())
      return false;
    if (value < BaseConstant.NUMBER_ZERO)
      return false;

    GameEffectType effectType = GameEffectType.fromString(typeStr);
    if (effectType == null)
      return false;

    // 타입별 특수 검증
    return switch (effectType) {
      case HEAL_HP_PERCENT, HEAL_MP_PERCENT -> value > BaseConstant.NUMBER_ZERO && value <= BaseConstant.NUMBER_HUNDRED;
      default -> value >= BaseConstant.NUMBER_ZERO;
    };
  }

  /**
   * 효과 리스트 검증
   */
  public static List<String> validateEffects(List<GameEffectData> effectDataList) {
    List<String> errors = new ArrayList<>();

    if (effectDataList == null || effectDataList.isEmpty()) {
      errors.add("효과 데이터가 없습니다");
      return errors;
    }

    for (int i = BaseConstant.NUMBER_ZERO; i < effectDataList.size(); i++) {
      GameEffectData effectData = effectDataList.get(i);
      if (!validateEffect(effectData)) {
        errors.add(String.format("효과 %d: 잘못된 데이터 (타입: %s, 값: %d)", i + 1, effectData.getType(), effectData.getValue()));
      }
    }

    return errors;
  }

  /**
   * 효과 통계 출력
   */
  public static void printEffectStatistics() {
    System.out.println("\n=== 🎭 효과 시스템 통계 ===");

    Set<GameEffectType> allTypes = EnumSet.allOf(GameEffectType.class);
    Set<GameEffectType> implementedTypes = getImplementedEffectTypes();

    System.out.println("총 효과 타입: " + allTypes.size() + "개");
    System.out.println("구현된 효과: " + implementedTypes.size() + "개");
    System.out.println("미구현 효과: " + (allTypes.size() - implementedTypes.size()) + "개");

    System.out.println("\n✅ 구현된 효과:");
    implementedTypes.stream().sorted(Comparator.comparing(GameEffectType::getCategory))
        .forEach(type -> System.out.printf("   %s %s%n", type.getEmoji(), type.getDisplayName()));

    System.out.println("\n🚧 미구현 효과:");
    allTypes.stream().filter(type -> !implementedTypes.contains(type)).sorted(Comparator.comparing(GameEffectType::getCategory))
        .forEach(type -> System.out.printf("   %s %s%n", type.getEmoji(), type.getDisplayName()));

    System.out.println("==========================");
  }

  /**
   * 디버그용 효과 정보 출력
   */
  public static void debugPrintEffect(GameEffect effect) {
    if (effect == null) {
      System.out.println("DEBUG: null 효과");
      return;
    }

    System.out.printf("DEBUG: 효과 - 타입: %s, 값: %d, 퍼센트: %b, 설명: %s%n", effect.getType().getDisplayName(), effect.getValue(), effect.isPercentage(),
        effect.getDescription());
  }
}
