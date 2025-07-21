package rpg.shared.dto.player;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rpg.shared.dto.quest.QuestManagerDto;

public class PlayerDto {
  private String name;
  private int level;
  private int hp;
  private int maxHp;
  private int mp;
  private int maxMp;
  private double restoreHp;
  private double restoreMana;
  private int exp;
  private int baseAttack;
  private int baseDefense;
  private int gold;
  private PlayerInventoryDto inventory;
  private SkillManagerDto skillManager;
  private String playerStatusCondition;
  private QuestManagerDto questManager;

  public PlayerDto() {}

  @JsonCreator
  public PlayerDto(
//@formatter:off
  @JsonProperty("name") String name
, @JsonProperty("level") int level
, @JsonProperty("hp") int hp
, @JsonProperty("maxHp") int maxHp
, @JsonProperty("mp") int mp
, @JsonProperty("maxMp") int maxMp
, @JsonProperty("restoreHp") double restoreHp
, @JsonProperty("restoreMana") double restoreMana
, @JsonProperty("exp") int exp
, @JsonProperty("baseAttack") int baseAttack
, @JsonProperty("baseDefense") int baseDefense
, @JsonProperty("gold") int gold
, @JsonProperty("inventory") PlayerInventoryDto inventory
, @JsonProperty("skillManager") SkillManagerDto skillManager
, @JsonProperty("playerStatusCondition") String playerStatusCondition
, @JsonProperty("questManager") QuestManagerDto questManager
//@formatter:off
  ) {
      this.name = name;
      this.level = level;
      this.hp = hp;
      this.maxHp = maxHp;
      this.mp = mp;
      this.maxMp = maxMp;
      this.restoreHp = restoreHp;
      this.restoreMana = restoreMana;
      this.exp = exp;
      this.baseAttack = baseAttack;
      this.baseDefense = baseDefense;
      this.gold = gold;
      this.inventory = inventory;
      this.skillManager = skillManager;
      this.playerStatusCondition = playerStatusCondition;
      this.questManager = questManager;
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

  public int getMp() {
    return mp;
  }

  public void setMp(int mp) {
    this.mp = mp;
  }

  public int getMaxMp() {
    return maxMp;
  }

  public void setMaxMp(int maxMp) {
    this.maxMp = maxMp;
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

  public PlayerInventoryDto getInventory() {
    return inventory;
  }

  public void setInventory(PlayerInventoryDto inventory) {
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

  public QuestManagerDto getQuestManager() {
    return questManager;
  }

  public void setQuestManager(QuestManagerDto questManager) {
    this.questManager = questManager;
  }
  
}
