package controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import model.Character;
import model.Monster;
import service.GameData;

public class Game {
  private Scanner scanner;
  private Random random;
  private Character player;
  private List<Monster> monsters;

  public Game() {
    scanner = new Scanner(System.in);
    random = new Random();
    initializeMonsters();
  }

  private void initializeMonsters() {
    monsters = new ArrayList<>();
    monsters.add(new Monster("슬라임", 20, 5, 10, 5));
    monsters.add(new Monster("고블린", 30, 8, 15, 10));
    monsters.add(new Monster("오크", 50, 12, 25, 20));
    monsters.add(new Monster("드래곤", 100, 20, 50, 50));
  }

  public void start() {
    System.out.println("==== 간단한 RPG 게임 ====");
    System.out.println("1. 새 게임");
    System.out.println("2. 게임 불러오기");
    System.out.print("선택: ");

    int choice = scanner.nextInt();
    scanner.nextLine();

    if (choice == 1) {
      System.out.print("캐릭터 이름을 입력하세요: ");
      String name = scanner.nextLine();
      player = new Character(name);
    } else if (choice == 2) {
      player = GameData.loadGame();
      if (player == null) {
        System.out.print("새 캐릭터 이름을 입력하세요: ");
        String name = scanner.nextLine();
        player = new Character(name);
      } else {
        System.out.println("게임을 불러왔습니다!");
      }
    } else {
      System.out.println("잘못된 선택입니다.");
      return;
    }

    gameLoop();
  }

  private void gameLoop() {
    while (player.isAlive()) {
      System.out.println("\n=== 메인 메뉴 ===");
      System.out.println("1. 탐험하기");
      System.out.println("2. 상태 확인");
      System.out.println("3. 상점");
      System.out.println("4. 게임 저장");
      System.out.println("5. 게임 종료");
      System.out.print("선택: ");

      int choice = scanner.nextInt();

      switch (choice) {
        case 1:
          explore();
          break;
        case 2:
          player.displayStats();
          break;
        case 3:
          shop();
          break;
        case 4:
          GameData.saveGame(player);
          break;
        case 5:
          System.out.println("게임을 종료합니다. 고마워요!");
          return;
        default:
          System.out.println("잘못된 선택입니다.");
      }
    }
    System.out.println("게임 오버!");
  }

  private void explore() {
    System.out.println("\n탐험을 시작합니다...");
    Monster monster = monsters.get(random.nextInt(monsters.size()));
    System.out.println(monster.getName() + "을(를) 만났습니다!");

    battle(monster);
  }

  private void battle(Monster monster) {
    System.out.println("\n전투 시작!");

    while (player.isAlive() && monster.isAlive()) {
      System.out.println("\n1. 공격");
      System.out.println("2. 도망");
      System.out.print("선택: ");

      int choice = scanner.nextInt();

      if (choice == 1) {
        int damage = player.getAttack() + random.nextInt(5);
        monster.takeDamage(damage);
        System.out.println(
            player.getName() + "이(가) " + monster.getName() + "에게 " + damage + "의 데미지를 입혔습니다!");

        if (monster.isAlive()) {
          int monsterDamage = monster.getAttack() + random.nextInt(3);
          player.takeDamage(monsterDamage);
          System.out.println(monster.getName() + "이(가) " + player.getName() + "에게 " + monsterDamage
              + "의 데미지를 입혔습니다!");
          System.out.println("현재 체력: " + player.getHp() + "/" + player.getMaxHp());
        }
      } else if (choice == 2) {
        if (random.nextInt(100) < 50) {
          System.out.println("성공적으로 도망쳤습니다!");
          return;
        } else {
          System.out.println("도망치지 못했습니다!");
          int monsterDamage = monster.getAttack() + random.nextInt(3);
          player.takeDamage(monsterDamage);
          System.out.println(monster.getName() + "이(가) " + player.getName() + "에게 " + monsterDamage
              + "의 데미지를 입혔습니다!");
          System.out.println("현재 체력: " + player.getHp() + "/" + player.getMaxHp());
        }
      }
    }

    if (!monster.isAlive()) {
      System.out.println(monster.getName() + "을(를) 물리쳤습니다!");
      boolean levelUp = player.gainExp(monster.getExpReward());
      player.setGold(player.getGold() + monster.getGoldReward());
      System.out.println("경험치 +" + monster.getExpReward() + ", 골드 +" + monster.getGoldReward());

      if (levelUp) {
        System.out.println("축하합니다! 레벨이 올랐습니다!");
      }
    }
  }

  private void shop() {
    System.out.println("\n=== 상점 ===");
    System.out.println("보유 골드: " + player.getGold());
    System.out.println("1. 체력 물약 (20골드) - 50 HP 회복");
    System.out.println("2. 나가기");
    System.out.print("선택: ");

    int choice = scanner.nextInt();

    if (choice == 1) {
      if (player.getGold() >= 20) {
        player.setGold(player.getGold() - 20);
        player.heal(50);
        System.out.println("체력 물약을 구매했습니다! 현재 체력: " + player.getHp() + "/" + player.getMaxHp());
      } else {
        System.out.println("골드가 부족합니다!");
      }
    }
  }
}
