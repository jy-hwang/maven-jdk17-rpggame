package model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 소모품 아이템 클래스
 */
public class GameConsumable extends GameItem {
  private int hpRestore;
  private int mpRestore;
  private int expGain;
  private boolean isStackable;

  @JsonCreator
  public GameConsumable(@JsonProperty("name") String name,
      @JsonProperty("description") String description, @JsonProperty("value") int value,
      @JsonProperty("rarity") ItemRarity rarity, @JsonProperty("hpRestore") int hpRestore,
      @JsonProperty("mpRestore") int mpRestore, @JsonProperty("expGain") int expGain, @JsonProperty("isStackable") boolean isStackable) {
    super(name, description, value, rarity);
    this.hpRestore = hpRestore;
    this.mpRestore = mpRestore;
    this.expGain = expGain;
    this.isStackable = isStackable;
  }

  @Override
  public boolean use(GameCharacter character) {
    if (hpRestore > 0) {
      int oldHp = character.getHp();
      character.heal(hpRestore);
      System.out.println(getName() + "을(를) 사용하여 " + (character.getHp() - oldHp) + " HP를 회복했습니다!");
    }
    
    if (mpRestore > 0) {
      int oldMana = character.getMana();
      character.heal(mpRestore);
      System.out.println(getName() + "을(를) 사용하여 " + (character.getMana() - oldMana) + " MP를 회복했습니다!");
    }

    if (expGain > 0) {
      boolean levelUp = character.gainExp(expGain);
      System.out.println(getName() + "을(를) 사용하여 " + expGain + " 경험치를 획득했습니다!");
      if (levelUp) {
        System.out.println("레벨업!");
      }
    }

    return true; // 소모품은 사용 후 소멸
  }

  @Override
  public String getItemInfo() {
    StringBuilder info = new StringBuilder();
    info.append(toString()).append("\n");
    if (hpRestore > 0)
      info.append("체력 회복: ").append(hpRestore).append("\n");
    if (mpRestore > 0)
      info.append("마나 회복: ").append(mpRestore).append("\n");
    if (expGain > 0)
      info.append("경험치 획득: ").append(expGain).append("\n");
    info.append("가격: ").append(getValue()).append(" 골드");
    return info.toString();
  }

  // Getters
  public int getHpRestore() {
    return hpRestore;
  }

  public int getExpGain() {
    return expGain;
  }

  public boolean isStackable() {
    return isStackable;
  }
}
