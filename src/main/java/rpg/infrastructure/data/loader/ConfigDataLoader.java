package rpg.infrastructure.data.loader;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import rpg.domain.item.GameEffectData;
import rpg.domain.item.GameItemData;
import rpg.shared.constant.SystemConstants;

public class ConfigDataLoader {
  private static final Logger logger = LoggerFactory.getLogger(ConfigDataLoader.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * 모든 아이템 데이터 로드 (통합 메서드)
   */
  public static Map<String, GameItemData> loadAllItems() {
    logger.info("전체 아이템 데이터 로드 시작...");

    Map<String, GameItemData> allItems = new HashMap<>();

    // 1. 소비 아이템 (물약) 로드
    Map<String, GameItemData> potions = loadBasicPotions();
    allItems.putAll(potions);
    logger.info("소비 아이템 로드 완료: {}개", potions.size());

    // 2. 무기 로드
    Map<String, GameItemData> weapons = loadBasicWeapons();
    allItems.putAll(weapons);
    logger.info("무기 로드 완료: {}개", weapons.size());

    // 3. 방어구 로드
    Map<String, GameItemData> armors = loadBasicArmors();
    allItems.putAll(armors);
    logger.info("방어구 로드 완료: {}개", armors.size());

    // 4. 액세서리 로드
    Map<String, GameItemData> accessories = loadBasicAccessories();
    allItems.putAll(accessories);
    logger.info("액세서리 로드 완료: {}개", accessories.size());

    logger.info("전체 아이템 로드 완료: {}개", allItems.size());
    return allItems;
  }

  /**
   * 설정 파일 존재 여부 확인 (범용)
   */
  public static boolean isConfigFileExists(String configPath) {
    InputStream inputStream = ConfigDataLoader.class.getResourceAsStream(configPath);
    boolean exists = inputStream != null;

    if (exists) {
      try {
        inputStream.close();
      } catch (Exception e) {
        logger.debug("InputStream 닫기 실패", e);
      }
    }

    logger.debug("설정 파일 존재 여부: {} ({})", exists, configPath);
    return exists;
  }


  /**
   * 기본 물약 데이터 로드
   */
  public static Map<String, GameItemData> loadBasicPotions() {
    try {
      logger.info("기본 물약 데이터 로드 시작...");

      // 설정 파일 존재 여부 확인 (범용 메서드 사용)
      if (!isConfigFileExists(SystemConstants.BASIC_POTIONS_CONFIG)) {
        logger.warn("물약 설정 파일을 찾을 수 없습니다: {}", SystemConstants.BASIC_POTIONS_CONFIG);
        return createDefaultPotions();
      }

      // JSON 파일 로드
      InputStream inputStream = ConfigDataLoader.class.getResourceAsStream(SystemConstants.BASIC_POTIONS_CONFIG);
      List<GameItemData> potionList = objectMapper.readValue(inputStream, new TypeReference<List<GameItemData>>() {});

      Map<String, GameItemData> potionMap = potionList.stream().collect(Collectors.toMap(GameItemData::getId, item -> item));

      logger.info("기본 물약 데이터 로드 완료: {}개", potionList.size());

      // 로드된 데이터 검증
      validateLoadedData(potionList);

      return potionMap;

    } catch (Exception e) {
      logger.error("기본 물약 데이터 로드 실패", e);
      return createDefaultPotions();
    }
  }

  /**
   * 로드된 데이터 검증
   */
  private static void validateLoadedData(List<GameItemData> potionList) {
    int validCount = 0;
    int invalidCount = 0;

    for (GameItemData potion : potionList) {
      boolean isValid = true;

      // 기본 필드 검증
      if (potion.getId() == null || potion.getId().trim().isEmpty()) {
        logger.warn("물약 ID가 비어있음: {}", potion.getName());
        isValid = false;
      }

      if (potion.getName() == null || potion.getName().trim().isEmpty()) {
        logger.warn("물약 이름이 비어있음: ID {}", potion.getId());
        isValid = false;
      }

      if (potion.getValue() < 0) {
        logger.warn("물약 가격이 음수: {} ({}골드)", potion.getName(), potion.getValue());
        isValid = false;
      }

      // 효과 검증
      if (potion.getEffects().isEmpty()) {
        logger.warn("효과가 없는 물약: {}", potion.getName());
        isValid = false;
      } else {
        for (GameEffectData effect : potion.getEffects()) {
          if (effect.getType() == null || effect.getValue() <= 0) {
            logger.warn("잘못된 효과 데이터: {} - 타입: {}, 값: {}", potion.getName(), effect.getType(), effect.getValue());
            isValid = false;
          }
        }
      }

      if (isValid) {
        validCount++;
        logger.debug("유효한 물약: {} (효과 {}개)", potion.getName(), potion.getEffects().size());
      } else {
        invalidCount++;
      }
    }

    logger.info("데이터 검증 완료: 유효 {}개, 무효 {}개", validCount, invalidCount);

    if (invalidCount > 0) {
      logger.warn("일부 물약 데이터에 문제가 있습니다. 로그를 확인하세요.");
    }
  }

  /**
   * 무기 데이터 로드
   */
  public static Map<String, GameItemData> loadBasicWeapons() {
    try {
      logger.info("무기 데이터 로드 시작...");

      // 설정 파일 존재 여부 확인 (범용 메서드 사용)
      if (!isConfigFileExists(SystemConstants.BASIC_WEAPONS_CONFIG)) {
        logger.warn("무기 설정 파일을 찾을 수 없습니다: {}", SystemConstants.BASIC_WEAPONS_CONFIG);
        return createDefaultWeapons();
      }

      // JSON 파일 로드
      InputStream inputStream = ConfigDataLoader.class.getResourceAsStream(SystemConstants.BASIC_WEAPONS_CONFIG);

      List<GameItemData> weaponList = objectMapper.readValue(inputStream, new TypeReference<List<GameItemData>>() {});
      Map<String, GameItemData> weaponMap = weaponList.stream().collect(Collectors.toMap(GameItemData::getId, item -> item));

      logger.info("무기 데이터 로드 완료: {}개", weaponList.size());
      validateEquipmentData(weaponList, "무기");

      return weaponMap;

    } catch (Exception e) {
      logger.error("무기 데이터 로드 실패", e);
      return createDefaultWeapons();
    }
  }

  /**
   * 방어구 데이터 로드
   */
  public static Map<String, GameItemData> loadBasicArmors() {
    try {
      logger.info("방어구 데이터 로드 시작...");

      // 설정 파일 존재 여부 확인 (범용 메서드 사용)
      if (!isConfigFileExists(SystemConstants.BASIC_ARMORS_CONFIG)) {
        logger.warn("방어구 설정 파일을 찾을 수 없습니다: {}", SystemConstants.BASIC_ARMORS_CONFIG);
        return createDefaultArmors();
      }

      InputStream inputStream = ConfigDataLoader.class.getResourceAsStream(SystemConstants.BASIC_ARMORS_CONFIG);

      List<GameItemData> armorList = objectMapper.readValue(inputStream, new TypeReference<List<GameItemData>>() {});
      Map<String, GameItemData> armorMap = armorList.stream().collect(Collectors.toMap(GameItemData::getId, item -> item));

      logger.info("방어구 데이터 로드 완료: {}개", armorList.size());
      validateEquipmentData(armorList, "방어구");

      return armorMap;

    } catch (Exception e) {
      logger.error("방어구 데이터 로드 실패", e);
      return createDefaultArmors();
    }
  }

  /**
   * 액세서리 데이터 로드
   */
  public static Map<String, GameItemData> loadBasicAccessories() {
    try {
      logger.info("액세서리 데이터 로드 시작...");

      // 설정 파일 존재 여부 확인 (범용 메서드 사용)
      if (!isConfigFileExists(SystemConstants.BASIC_ACCESSORIES_CONFIG)) {
        logger.warn("액세서리 설정 파일을 찾을 수 없습니다: {}", SystemConstants.BASIC_ACCESSORIES_CONFIG);
        return createDefaultAccessories();
      }
      InputStream inputStream = ConfigDataLoader.class.getResourceAsStream(SystemConstants.BASIC_ACCESSORIES_CONFIG);

      List<GameItemData> accessoryList = objectMapper.readValue(inputStream, new TypeReference<List<GameItemData>>() {});
      Map<String, GameItemData> accessoryMap = accessoryList.stream().collect(Collectors.toMap(GameItemData::getId, item -> item));

      logger.info("액세서리 데이터 로드 완료: {}개", accessoryList.size());
      validateEquipmentData(accessoryList, "액세서리");

      return accessoryMap;

    } catch (Exception e) {
      logger.error("액세서리 데이터 로드 실패", e);
      return createDefaultAccessories();
    }
  }
  /**
   * 기본 무기 생성 (JSON 파일이 없을 때) - 수정된 버전
   */
  private static Map<String, GameItemData> createDefaultWeapons() {
    logger.info("기본 무기 데이터를 코드로 생성 중...");

    try {
      // 🆕 stats 맵 생성
      Map<String, Integer> woodenSwordStats = Map.of("attack", 5, "defense", 0, "magic", 0);
      Map<String, Integer> ironSwordStats = Map.of("attack", 12, "defense", 0, "magic", 0);
      Map<String, Integer> steelSwordStats = Map.of("attack", 20, "defense", 0, "magic", 0);

      //@formatter:off
      Map<String, GameItemData> defaultWeapons = Map.of(
          "WOODEN_SWORD", new GameItemData(
              "WOODEN_SWORD", "나무 검", "초보자용 나무 검입니다", "EQUIPMENT", 30, "COMMON", 
              false, null, "WEAPON", 5, 0, 0, 
              null, woodenSwordStats, null
          ),
          "IRON_SWORD", new GameItemData(
              "IRON_SWORD", "철 검", "날카로운 철로 만든 검입니다", "EQUIPMENT", 100, "UNCOMMON", 
              false, null, "WEAPON", 12, 0, 0, 
              null, ironSwordStats, null
          ),
          "STEEL_SWORD", new GameItemData(
              "STEEL_SWORD", "강철 검", "단단한 강철로 제련한 고급 검입니다", "EQUIPMENT", 250, "RARE", 
              false, null, "WEAPON", 20, 0, 0, 
              null, steelSwordStats, null
          )
      );
      //@formatter:on

      logger.info("기본 무기 생성 완료: {}개", defaultWeapons.size());
      return defaultWeapons;
      
    } catch (Exception e) {
      logger.error("기본 무기 생성 실패", e);
      return new HashMap<>();
    }
  }

  /**
   * 기본 방어구 생성 (JSON 파일이 없을 때) - 수정된 버전
   */
  private static Map<String, GameItemData> createDefaultArmors() {
    logger.info("기본 방어구 데이터를 코드로 생성 중...");

    try {
      // 🆕 stats 맵 생성
      Map<String, Integer> leatherArmorStats = Map.of("attack", 0, "defense", 8, "magic", 5);
      Map<String, Integer> chainMailStats = Map.of("attack", 0, "defense", 15, "magic", 8);

      //@formatter:off
      Map<String, GameItemData> defaultArmors = Map.of(
          "LEATHER_ARMOR", new GameItemData(
              "LEATHER_ARMOR", "가죽 갑옷", "질긴 가죽으로 만든 갑옷입니다", "EQUIPMENT", 60, "COMMON", 
              false, null, "ARMOR", 0, 8, 20, 
              null, leatherArmorStats, null
          ),
          "CHAIN_MAIL", new GameItemData(
              "CHAIN_MAIL", "사슬 갑옷", "쇠사슬로 엮어 만든 갑옷입니다", "EQUIPMENT", 150, "UNCOMMON", 
              false, null, "ARMOR", 0, 15, 25, 
              null, chainMailStats, null
          )
      );
      //@formatter:on

      logger.info("기본 방어구 생성 완료: {}개", defaultArmors.size());
      return defaultArmors;
      
    } catch (Exception e) {
      logger.error("기본 방어구 생성 실패", e);
      return new HashMap<>();
    }
  }

  /**
   * 기본 액세서리 생성 (JSON 파일이 없을 때) - 수정된 버전
   */
  private static Map<String, GameItemData> createDefaultAccessories() {
    logger.info("기본 액세서리 데이터를 코드로 생성 중...");

    try {
      // 🆕 stats 맵 생성
      Map<String, Integer> powerRingStats = Map.of("attack", 5, "defense", 0, "magic", 3);

      //@formatter:off
      Map<String, GameItemData> defaultAccessories = Map.of(
          "POWER_RING", new GameItemData(
              "POWER_RING", "힘의 반지", "착용자의 공격력을 높여주는 마법의 반지입니다", "EQUIPMENT", 200, "UNCOMMON", 
              false, null, "ACCESSORY", 5, 0, 0, 
              null, powerRingStats, null
          )
      );
      //@formatter:on

      logger.info("기본 액세서리 생성 완료: {}개", defaultAccessories.size());
      return defaultAccessories;
      
    } catch (Exception e) {
      logger.error("기본 액세서리 생성 실패", e);
      return new HashMap<>();
    }
  }

  /**
   * 기본 물약 생성 (JSON 파일이 없을 때) - 수정된 버전
   */
  private static Map<String, GameItemData> createDefaultPotions() {
    logger.info("기본 물약 데이터를 코드로 생성 중...");

    try {
      // HP 회복 물약
      List<GameEffectData> hpEffect = List.of(new GameEffectData("HEAL_HP", 30));
      // MP 회복 물약
      List<GameEffectData> mpEffect = List.of(new GameEffectData("HEAL_MP", 20));

      //@formatter:off
      Map<String, GameItemData> defaultPotions = Map.of(
          "SMALL_HEALTH_POTION", new GameItemData(
              "HEALTH_POTION", "체력 물약", "HP를 30 회복합니다", "CONSUMABLE", 25, "COMMON", 
              true, hpEffect, null, null, null, null, 
              0, null, null  // 🆕 cooldown 추가
          ),
          "SMALL_MANA_POTION", new GameItemData(
              "MANA_POTION", "마나 물약", "MP를 20 회복합니다", "CONSUMABLE", 30, "COMMON", 
              true, mpEffect, null, null, null, null, 
              0, null, null  // 🆕 cooldown 추가
          )
      );
      //@formatter:on
      
      logger.info("기본 물약 생성 완료: {}개", defaultPotions.size());
      validateLoadedData(List.copyOf(defaultPotions.values()));
      return defaultPotions;

    } catch (Exception e) {
      logger.error("기본 물약 생성 실패", e);
      return Map.of(); // 빈 맵 반환
    }
  }
  
  /**
   * 장비 데이터 검증
   */
  private static void validateEquipmentData(List<GameItemData> equipmentList, String type) {
    int validCount = 0;
    int invalidCount = 0;

    for (GameItemData equipment : equipmentList) {
      boolean isValid = true;

      // 기본 검증
      if (equipment.getId() == null || equipment.getName() == null) {
        logger.warn("{} ID 또는 이름이 비어있음: {}", type, equipment.getId());
        isValid = false;
      }

      // 장비별 스탯 검증
      if ("WEAPON".equals(equipment.getType()) && equipment.getAttackBonus() <= 0) {
        logger.warn("무기의 공격력이 0 이하: {} (공격력: {})", equipment.getName(), equipment.getAttackBonus());
        isValid = false;
      }

      if ("ARMOR".equals(equipment.getType()) && equipment.getDefenseBonus() <= 0) {
        logger.warn("방어구의 방어력이 0 이하: {} (방어력: {})", equipment.getName(), equipment.getDefenseBonus());
        isValid = false;
      }

      if (isValid) {
        validCount++;
        logger.debug("유효한 {}: {} (공격: {}, 방어: {}, 체력: {})", type, equipment.getName(), equipment.getAttackBonus(), equipment.getDefenseBonus(),
            equipment.getHpBonus());
      } else {
        invalidCount++;
      }
    }

    logger.info("{} 데이터 검증 완료: 유효 {}개, 무효 {}개", type, validCount, invalidCount);
  }

  /**
   * 데이터 통계 정보 출력
   */
  private static void printDataStatistics(Map<String, GameItemData> potions) {
    if (potions.isEmpty()) {
      logger.info("데이터 통계: 로드된 아이템이 없습니다.");
      return;
    }

    // 타입별 통계
    Map<String, Long> typeStats = potions.values().stream().collect(Collectors.groupingBy(GameItemData::getType, Collectors.counting()));

    // 등급별 통계
    Map<String, Long> rarityStats =
        potions.values().stream().collect(Collectors.groupingBy(item -> item.getRarity().getDisplayName(), Collectors.counting()));

    // 중첩 가능 통계
    long stackableCount = potions.values().stream().mapToLong(item -> item.isStackable() ? 1 : 0).sum();

    // 효과별 통계
    Map<String, Long> effectStats = potions.values().stream().flatMap(item -> item.getEffects().stream())
        .collect(Collectors.groupingBy(GameEffectData::getType, Collectors.counting()));

    logger.info("=== 데이터 통계 ===");
    logger.info("총 아이템: {}개", potions.size());
    logger.info("타입별: {}", typeStats);
    logger.info("등급별: {}", rarityStats);
    logger.info("중첩 가능: {}개", stackableCount);
    logger.info("효과별: {}", effectStats);
    logger.info("================");
  }

  /**
   * 특정 타입의 아이템만 로드 (확장용)
   */
  public static Map<String, GameItemData> loadItemsByType(String itemType) {
    Map<String, GameItemData> allItems = loadBasicPotions();

    return allItems.entrySet().stream().filter(entry -> entry.getValue().getType().equalsIgnoreCase(itemType))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * 데이터 다시 로드 (런타임 리로드용)
   */
  public static void reloadGameData() {
    logger.info("게임 데이터 리로드 중...");

    // 캐시 무효화 (구현 시)
    // clearCache();

    // 데이터 다시 로드
    loadAllItems();

    logger.info("게임 데이터 리로드 완료");
  }
}
