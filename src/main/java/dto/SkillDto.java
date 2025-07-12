package dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SkillDto {
  private String name;
  private String description;
  private String type; // "ATTACK", "HEAL", "BUFF", "DEBUFF"
  private int requiredLevel;
  private int manaCost;
  private int cooldown;
  private double damageMultiplier;
  private int healAmount;
  private int buffDuration;

  public SkillDto() {}

  @JsonCreator
  public SkillDto(
//@formatter:off
  @JsonProperty("name") String name
, @JsonProperty("description") String description
, @JsonProperty("type") String type
, @JsonProperty("requiredLevel") int requiredLevel
, @JsonProperty("manaCost") int manaCost
, @JsonProperty("cooldown") int cooldown
, @JsonProperty("damageMultiplier") double damageMultiplier
, @JsonProperty("healAmount") int healAmount
, @JsonProperty("buffDuration") int buffDuration
//@formatter:on
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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public int getRequiredLevel() {
    return requiredLevel;
  }

  public void setRequiredLevel(int requiredLevel) {
    this.requiredLevel = requiredLevel;
  }

  public int getManaCost() {
    return manaCost;
  }

  public void setManaCost(int manaCost) {
    this.manaCost = manaCost;
  }

  public int getCooldown() {
    return cooldown;
  }

  public void setCooldown(int cooldown) {
    this.cooldown = cooldown;
  }

  public double getDamageMultiplier() {
    return damageMultiplier;
  }

  public void setDamageMultiplier(double damageMultiplier) {
    this.damageMultiplier = damageMultiplier;
  }

  public int getHealAmount() {
    return healAmount;
  }

  public void setHealAmount(int healAmount) {
    this.healAmount = healAmount;
  }

  public int getBuffDuration() {
    return buffDuration;
  }

  public void setBuffDuration(int buffDuration) {
    this.buffDuration = buffDuration;
  }

}
