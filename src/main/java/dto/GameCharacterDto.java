package dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GameCharacterDto {
  private String name;
  private int level;
  private int hp;
  private int maxHp;
  private int mana;
  private int maxMana;
  private double restoreHp;
  private double restoreMana;
  private int exp;
  private int baseAttack;
  private int baseDefense;
  private int gold;
  private GameInventoryDto inventory;
  private SkillManagerDto skillManager;
  private String playerStatusCondition;

  public GameCharacterDto() {}

  @JsonCreator
  public GameCharacterDto(
//@formatter:off
  @JsonProperty("name") String name
, @JsonProperty("level") int level
, @JsonProperty("hp") int hp
, @JsonProperty("maxHp") int maxHp
, @JsonProperty("mana") int mana
, @JsonProperty("maxMana") int maxMana
, @JsonProperty("restoreHp") double restoreHp
, @JsonProperty("restoreMana") double restoreMana
, @JsonProperty("exp") int exp
, @JsonProperty("baseAttack") int baseAttack
, @JsonProperty("baseDefense") int baseDefense
, @JsonProperty("gold") int gold
, @JsonProperty("inventory") GameInventoryDto inventory
, @JsonProperty("skillManager") SkillManagerDto skillManager
, @JsonProperty("playerStatusCondition") String playerStatusCondition
//@formatter:off
  ) {
      this.name = name;
      this.level = level;
      this.hp = hp;
      this.maxHp = maxHp;
      this.mana = mana;
      this.maxMana = maxMana;
      this.restoreHp = restoreHp;
      this.restoreMana = restoreMana;
      this.exp = exp;
      this.baseAttack = baseAttack;
      this.baseDefense = baseDefense;
      this.gold = gold;
      this.inventory = inventory;
      this.skillManager = skillManager;
      this.playerStatusCondition = playerStatusCondition;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public int getHp() {
    return hp;
  }

  public void setHp(int hp) {
    this.hp = hp;
  }

  public int getMaxHp() {
    return maxHp;
  }

  public void setMaxHp(int maxHp) {
    this.maxHp = maxHp;
  }

  public int getMana() {
    return mana;
  }

  public void setMana(int mana) {
    this.mana = mana;
  }

  public int getMaxMana() {
    return maxMana;
  }

  public void setMaxMana(int maxMana) {
    this.maxMana = maxMana;
  }

  public double getRestoreHp() {
    return restoreHp;
  }

  public void setRestoreHp(double restoreHp) {
    this.restoreHp = restoreHp;
  }

  public double getRestoreMana() {
    return restoreMana;
  }

  public void setRestoreMana(double restoreMana) {
    this.restoreMana = restoreMana;
  }

  public int getExp() {
    return exp;
  }

  public void setExp(int exp) {
    this.exp = exp;
  }

  public int getBaseAttack() {
    return baseAttack;
  }

  public void setBaseAttack(int baseAttack) {
    this.baseAttack = baseAttack;
  }

  public int getBaseDefense() {
    return baseDefense;
  }

  public void setBaseDefense(int baseDefense) {
    this.baseDefense = baseDefense;
  }

  public int getGold() {
    return gold;
  }

  public void setGold(int gold) {
    this.gold = gold;
  }

  public GameInventoryDto getInventory() {
    return inventory;
  }

  public void setInventory(GameInventoryDto inventory) {
    this.inventory = inventory;
  }

  public SkillManagerDto getSkillManager() {
    return skillManager;
  }

  public void setSkillManager(SkillManagerDto skillManager) {
    this.skillManager = skillManager;
  }

  public String getPlayerStatusCondition() {
    return playerStatusCondition;
  }

  public void setPlayerStatusCondition(String playerStatusCondition) {
    this.playerStatusCondition = playerStatusCondition;
  }
  
}
