package model.monster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MonsterStats {
  private int hp;
  private int attack;
  private int defense;
  private int speed;
  private double criticalRate;

  @JsonCreator
  public MonsterStats(
//@formatter:off
  @JsonProperty("hp") int hp
, @JsonProperty("attack") int attack
, @JsonProperty("defense") int defense
, @JsonProperty("speed") int speed
, @JsonProperty("criticalRate") double criticalRate
//@formatter:on
  ) {
    this.hp = hp;
    this.attack = attack;
    this.defense = defense;
    this.speed = speed;
    this.criticalRate = criticalRate;
  }

  // Getters
  public int getHp() {
    return hp;
  }

  public int getAttack() {
    return attack;
  }

  public int getDefense() {
    return defense;
  }

  public int getSpeed() {
    return speed;
  }

  public double getCriticalRate() {
    return criticalRate;
  }
}
