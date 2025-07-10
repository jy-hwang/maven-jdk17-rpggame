package model;

public class Character {
  private String name;
  private int level;
  private int hp;
  private int maxHp;
  private int exp;
  private int attack;
  private int defense;
  private int gold;

  public Character(String name) {
    this.name = name;
    this.level = 1;
    this.maxHp = 100;
    this.hp = maxHp;
    this.exp = 0;
    this.attack = 10;
    this.defense = 5;
    this.gold = 50;
  }

  public Character(String name, int level, int hp, int maxHp, int exp, int attack, int defense,
      int gold) {
    this.name = name;
    this.level = level;
    this.hp = hp;
    this.maxHp = maxHp;
    this.exp = exp;
    this.attack = attack;
    this.defense = defense;
    this.gold = gold;
  }

  public boolean gainExp(int expGained) {
    this.exp += expGained;
    if (this.exp >= level * 100) {
      levelUp();
      return true;
    }
    return false;
  }

  private void levelUp() {
    level++;
    exp = 0;
    maxHp += 20;
    attack += 5;
    defense += 3;
    hp = maxHp;
    System.out.println("레벨업! 새로운 레벨: " + level);
  }

  public void heal(int amount) {
    hp = Math.min(hp + amount, maxHp);
  }

  public void takeDamage(int damage) {
    int actualDamage = Math.max(damage - defense, 1);
    hp -= actualDamage;
    if (hp < 0)
      hp = 0;
  }

  public boolean isAlive() {
    return hp > 0;
  }

  public String toCsv() {
    return String.format("%s,%d,%d,%d,%d,%d,%d,%d", name, level, hp, maxHp, exp, attack, defense,
        gold);
  }

  public static Character fromCsv(String csvLine) {
    String[] parts = csvLine.split(",");
    return new Character(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
        Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]),
        Integer.parseInt(parts[6]), Integer.parseInt(parts[7]));
  }

  public void displayStats() {
    System.out.println("========== 캐릭터 정보 ==========");
    System.out.println("이름: " + name);
    System.out.println("레벨: " + level);
    System.out.println("체력: " + hp + "/" + maxHp);
    System.out.println("경험치: " + exp + "/" + (level * 100));
    System.out.println("공격력: " + attack);
    System.out.println("방어력: " + defense);
    System.out.println("골드: " + gold);
    System.out.println("=========================");
  }

  // Getters and Setters
  public String getName() {
    return name;
  }

  public int getLevel() {
    return level;
  }

  public int getHp() {
    return hp;
  }

  public int getMaxHp() {
    return maxHp;
  }

  public int getExp() {
    return exp;
  }

  public int getAttack() {
    return attack;
  }

  public int getDefense() {
    return defense;
  }

  public int getGold() {
    return gold;
  }

  public void setGold(int gold) {
    this.gold = gold;
  }
}
