package model;

public class Monster {
  private String name;
  private int hp;
  private int attack;
  private int expReward;
  private int goldReward;

  public Monster(String name, int hp, int attack, int expReward, int goldReward) {
    this.name = name;
    this.hp = hp;
    this.attack = attack;
    this.expReward = expReward;
    this.goldReward = goldReward;
  }

  public void takeDamage(int damage) {
    hp -= damage;
    if (hp < 0)
      hp = 0;
  }

  public boolean isAlive() {
    return hp > 0;
  }

  public String getName() {
    return name;
  }

  public int getHp() {
    return hp;
  }

  public int getAttack() {
    return attack;
  }

  public int getExpReward() {
    return expReward;
  }

  public int getGoldReward() {
    return goldReward;
  }
}
