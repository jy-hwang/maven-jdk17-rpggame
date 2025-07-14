package rpg.domain.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GameItemData {
  private final String id;
  private final String name;
  private final String description;
  private final String type;
  private final int value;
  private final ItemRarity rarity;
  private final boolean stackable;
  private final List<GameEffectData> effects;

  // 장비 전용 필드들 (nullable)
  private final String equipmentType; // "WEAPON", "ARMOR", "ACCESSORY"
  private final Integer attackBonus; // null이면 0으로 처리
  private final Integer defenseBonus; // null이면 0으로 처리
  private final Integer hpBonus; // null이면 0으로 처리

  // 🆕 추가된 필드들
  private final Integer cooldown; // 소비 아이템 쿨다운
  private final Map<String, Integer> stats; // 장비 스탯 (attack, defense, magic)

  private final Map<String, Object> properties;

  @JsonCreator
  public GameItemData(
//@formatter:off
  @JsonProperty("id") String id
, @JsonProperty("name") String name
, @JsonProperty("description") String description
, @JsonProperty("type") String type
, @JsonProperty("value") int value
, @JsonProperty("rarity") String rarity
, @JsonProperty("stackable") boolean stackable
, @JsonProperty("effects") List<GameEffectData> effects
// 장비 전용 필드들 (Optional)
, @JsonProperty("equipmentType") String equipmentType
, @JsonProperty("attackBonus") Integer attackBonus
, @JsonProperty("defenseBonus") Integer defenseBonus
, @JsonProperty("hpBonus") Integer hpBonus
// 🆕 새로 추가된 필드들
, @JsonProperty("cooldown") Integer cooldown
, @JsonProperty("stats") Map<String, Integer> stats
, @JsonProperty("properties") Map<String, Object> properties
//@formatter:on
  ) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.type = type;
    this.value = value;
    this.rarity = ItemRarity.valueOf(rarity.toUpperCase());
    this.stackable = stackable;
    this.effects = effects != null ? new ArrayList<>(effects) : new ArrayList<>();
    this.equipmentType = equipmentType;
    this.attackBonus = attackBonus;
    this.defenseBonus = defenseBonus;
    this.hpBonus = hpBonus;

    // 🆕 새 필드들 초기화
    this.cooldown = cooldown;
    this.stats = stats != null ? new HashMap<>(stats) : new HashMap<>();

    this.properties = properties != null ? new HashMap<>(properties) : new HashMap<>();
  }

  // Getters
  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getType() {
    return type;
  }

  public int getValue() {
    return value;
  }

  public ItemRarity getRarity() {
    return rarity;
  }

  public boolean isStackable() {
    return stackable;
  }

  public List<GameEffectData> getEffects() {
    return new ArrayList<>(effects);
  }

  public String getEquipmentType() {
    return equipmentType;
  }

  public int getAttackBonus() {
    // stats 필드에서 우선 확인, 없으면 attackBonus 사용
    if (stats.containsKey("attack")) {
      return stats.get("attack");
    }
    return attackBonus != null ? attackBonus : 0;
  }

  public int getDefenseBonus() {
    // stats 필드에서 우선 확인, 없으면 defenseBonus 사용
    if (stats.containsKey("defense")) {
      return stats.get("defense");
    }
    return defenseBonus != null ? defenseBonus : 0;
  }

  public int getHpBonus() {
    return hpBonus != null ? hpBonus : 0;
  }

  // 🆕 새로 추가된 getter 메서드들
  public int getCooldown() {
    // cooldown 필드가 있으면 사용, 없으면 properties에서 찾기
    if (cooldown != null) {
      return cooldown;
    }

    // properties에서 cooldown 찾기 (기존 방식과 호환성 유지)
    if (properties != null && properties.containsKey("cooldown")) {
      Object cooldownObj = properties.get("cooldown");
      if (cooldownObj instanceof Integer) {
        return (Integer) cooldownObj;
      } else if (cooldownObj instanceof String) {
        try {
          return Integer.parseInt((String) cooldownObj);
        } catch (NumberFormatException e) {
          return 0;
        }
      }
    }

    return 0;
  }

  public Map<String, Integer> getStats() {
    return new HashMap<>(stats);
  }

  public int getMagicBonus() {
    // stats 필드에서 magic 값 반환
    return stats.getOrDefault("magic", 0);
  }

  public Map<String, Object> getProperties() {
    return new HashMap<>(properties);
  }
}
