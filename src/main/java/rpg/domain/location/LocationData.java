package rpg.domain.location;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LocationData {
  private final String id;
  private final String nameKo;
  private final String nameEn;
  private final String icon;
  private final String description;
  private final int minLevel;
  private final int maxLevel;
  private final DangerLevel dangerLevel;
  private final int eventChance;
  private final Map<String, Object> properties;

  @JsonCreator
  public LocationData(
  //@formatter:off
      @JsonProperty("id") String id,
      @JsonProperty("nameKo") String nameKo,
      @JsonProperty("nameEn") String nameEn,
      @JsonProperty("icon") String icon,
      @JsonProperty("description") String description,
      @JsonProperty("minLevel") int minLevel,
      @JsonProperty("maxLevel") int maxLevel,
      @JsonProperty("dangerLevel") String dangerLevel,
      @JsonProperty("eventChance") int eventChance,
      @JsonProperty("properties") Map<String, Object> properties
      //@formatter:on
  ) {
    this.id = id;
    this.nameKo = nameKo;
    this.nameEn = nameEn;
    this.icon = icon;
    this.description = description;
    this.minLevel = minLevel;
    this.maxLevel = maxLevel;
    this.dangerLevel = DangerLevel.valueOf(dangerLevel);
    this.eventChance = eventChance;
    this.properties = properties;
  }

  // Getters
  public String getId() {
    return id;
  }

  public String getNameKo() {
    return nameKo;
  }

  public String getNameEn() {
    return nameEn;
  }

  public String getIcon() {
    return icon;
  }

  public String getDescription() {
    return description;
  }

  public int getMinLevel() {
    return minLevel;
  }

  public int getMaxLevel() {
    return maxLevel;
  }

  public DangerLevel getDangerLevel() {
    return dangerLevel;
  }

  public int getEventChance() {
    return eventChance;
  }

  public Map<String, Object> properties() {
    return properties;
  }


}
