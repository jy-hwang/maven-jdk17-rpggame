package controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import model.Character;
import model.Monster;
import service.GameData;
import util.InputValidator;

/**
 * 게임 메인 로직을 담당하는 컨트롤러 클래스
 */
public class Game {
  private static final Logger logger = LoggerFactory.getLogger(Game.class);

  private Random random;
  private Character player;
  private List<Monster> monsters;
  private boolean gameRunning;

  // 게임 상수
  private static final int HEALTH_POTION_PRICE = 20;
  private static final int HEALTH_POTION_HEAL = 50;
  private static final int ESCAPE_CHANCE = 50;

  public Game() {
    this.random = new Random();
    this.gameRunning = true;
    initializeMonsters();
    logger.info("게임 인스턴스 생성 완료");
  }

  /**
   * 몬스터 목록을 초기화합니다.
   */
  private void initializeMonsters() {
    try {
      monsters = new ArrayList<>();
      monsters.add(new Monster("슬라임", 20, 5, 10, 5));
      monsters.add(new Monster("고블린", 30, 8, 15, 10));
      monsters.add(new Monster("오크", 50, 12, 25, 20));
      monsters.add(new Monster("드래곤", 100, 20, 50, 50));

      logger.debug("몬스터 초기화 완료: {}마리", monsters.size());
    } catch (Exception e) {
      logger.error("몬스터 초기화 실패", e);
      throw new RuntimeException("게임 초기화 중 오류가 발생했습니다.", e);
    }
  }

  /**
   * 게임을 시작합니다.
   */
  public void start() {
    try {
      logger.info("게임 시작");
      showWelcomeMessage();

      // 플레이어 초기화
      if (!initializePlayer()) {
        logger.info("플레이어 초기화 실패로 게임 종료");
        return;
      }

      // 메인 게임 루프
      gameLoop();

    } catch (Exception e) {
      logger.error("게임 실행 중 오류 발생", e);
      System.out.println("게임 실행 중 오류가 발생했습니다. 게임을 종료합니다.");
    } finally {
      logger.info("게임 종료");
    }
  }

  /**
   * 환영 메시지를 표시합니다.
   */
  private void showWelcomeMessage() {
    System.out.println("==== 간단한 RPG 게임 ====");
    System.out.println("모험가여, 환영합니다!");
    System.out.println("========================");
  }

  /**
   * 플레이어를 초기화합니다.
   * 
   * @return 초기화 성공 시 true
   */
  private boolean initializePlayer() {
    try {
      int choice = InputValidator.getIntInput("1. 새 게임\n2. 게임 불러오기\n선택: ", 1, 2);

      if (choice == 1) {
        return createNewCharacter();
      } else {
        return loadExistingCharacter();
      }

    } catch (Exception e) {
      logger.error("플레이어 초기화 중 오류", e);
      System.out.println("플레이어 초기화 중 오류가 발생했습니다.");
      return false;
    }
  }

  /**
   * 새 캐릭터를 생성합니다.
   * 
   * @return 생성 성공 시 true
   */
  private boolean createNewCharacter() {
    try {
      String name = InputValidator.getStringInput("캐릭터 이름을 입력하세요: ", 1, 20);
      player = new Character(name);

      System.out.println("새로운 모험가 " + name + "님, 환영합니다!");
      player.displayStats();

      logger.info("새 캐릭터 생성: {}", name);
      return true;

    } catch (Exception e) {
      logger.error("새 캐릭터 생성 실패", e);
      System.out.println("캐릭터 생성 중 오류가 발생했습니다.");
      return false;
    }
  }

  /**
   * 기존 캐릭터를 불러옵니다.
   * 
   * @return 로드 성공 시 true
   */
  private boolean loadExistingCharacter() {
    try {
      player = GameData.loadGame();

      if (player == null) {
        System.out.println("저장된 게임이 없습니다. 새 게임을 시작합니다.");
        return createNewCharacter();
      } else {
        System.out.println("게임을 불러왔습니다!");
        System.out.println("어서오세요, " + player.getName() + "님!");
        player.displayStats();

        logger.info("기존 캐릭터 로드: {}", player.getName());
        return true;
      }

    } catch (GameData.GameDataException e) {
      logger.error("게임 로드 실패", e);
      System.out.println("게임 로드 실패: " + e.getMessage());

      boolean createNew = InputValidator.getConfirmation("새 게임을 시작하시겠습니까?");
      if (createNew) {
        return createNewCharacter();
      } else {
        return false;
      }
    }
  }

  /**
   * 메인 게임 루프를 실행합니다.
   */
  private void gameLoop() {
    while (gameRunning && player.isAlive()) {
      try {
        showMainMenu();
        int choice = InputValidator.getIntInput("선택: ", 1, 6);

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
            saveGame();
            break;
          case 5:
            if (confirmExit()) {
              gameRunning = false;
            }
            break;
          case 6:
            showHelp();
            break;
          default:
            System.out.println("잘못된 선택입니다.");
        }

        if (gameRunning && choice != 2 && choice != 6) { // 상태 확인과 도움말 제외
          InputValidator.waitForAnyKey("\n계속하려면 Enter를 누르세요...");
        }

      } catch (Exception e) {
        logger.error("게임 루프 중 오류", e);
        System.out.println("게임 진행 중 오류가 발생했습니다. 계속 진행합니다.");
      }
    }

    if (!player.isAlive()) {
      handleGameOver();
    } else {
      System.out.println("게임을 종료합니다. 안녕히 가세요!");
    }
  }

  /**
   * 메인 메뉴를 표시합니다.
   */
  private void showMainMenu() {
    System.out.println("\n=== 메인 메뉴 ===");
    System.out.println("1. 탐험하기");
    System.out.println("2. 상태 확인");
    System.out.println("3. 상점");
    System.out.println("4. 게임 저장");
    System.out.println("5. 게임 종료");
    System.out.println("6. 도움말");
  }

  /**
   * 탐험을 진행합니다.
   */
  private void explore() {
    try {
      System.out.println("\n🌲 탐험을 시작합니다...");

      // 랜덤 이벤트 (10% 확률로 특별한 일 발생)
      if (random.nextInt(100) < 10) {
        handleRandomEvent();
      } else {
        // 일반적인 몬스터 조우
        Monster monster = getRandomMonster();
        System.out.println("⚔️ " + monster.getName() + "을(를) 만났습니다!");
        battle(monster);
      }

    } catch (Exception e) {
      logger.error("탐험 중 오류", e);
      System.out.println("탐험 중 오류가 발생했습니다.");
    }
  }

  /**
   * 랜덤 이벤트를 처리합니다.
   */
  private void handleRandomEvent() {
    int eventType = random.nextInt(3);

    switch (eventType) {
      case 0: // 골드 발견
        int foundGold = random.nextInt(20) + 5;
        player.setGold(player.getGold() + foundGold);
        System.out.println("💰 길에서 " + foundGold + " 골드를 발견했습니다!");
        logger.debug("랜덤 골드 획득: {}", foundGold);
        break;

      case 1: // 체력 회복
        int healAmount = random.nextInt(30) + 10;
        player.heal(healAmount);
        System.out.println("💚 신비한 샘물을 발견해 " + healAmount + " 체력을 회복했습니다!");
        logger.debug("랜덤 체력 회복: {}", healAmount);
        break;

      case 2: // 경험치 획득
        int expAmount = random.nextInt(15) + 5;
        boolean levelUp = player.gainExp(expAmount);
        System.out.println("📚 고대 문서를 읽어 " + expAmount + " 경험치를 얻었습니다!");
        if (levelUp) {
          System.out.println("🎉 깨달음을 얻어 레벨이 올랐습니다!");
        }
        logger.debug("랜덤 경험치 획득: {}", expAmount);
        break;
    }
  }

  /**
   * 랜덤 몬스터를 선택합니다.
   * 
   * @return 선택된 몬스터
   */
  private Monster getRandomMonster() {
    Monster template = monsters.get(random.nextInt(monsters.size()));
    // 몬스터의 새 인스턴스 생성 (원본 데이터 보호)
    return new Monster(template.getName(), template.getHp(), template.getAttack(),
        template.getExpReward(), template.getGoldReward());
  }

  /**
   * 전투를 진행합니다.
   * 
   * @param monster 전투할 몬스터
   */
  private void battle(Monster monster) {
    try {
      System.out.println("\n⚔️ 전투 시작!");
      logger.info("전투 시작: {} vs {}", player.getName(), monster.getName());

      while (player.isAlive() && monster.isAlive()) {
        showBattleStatus(monster);

        int choice = InputValidator.getIntInput("1. 공격\n2. 도망\n선택: ", 1, 2);

        if (choice == 1) {
          handlePlayerAttack(monster);
          if (monster.isAlive()) {
            handleMonsterAttack(monster);
          }
        } else {
          if (attemptEscape()) {
            return;
          } else {
            handleMonsterAttack(monster);
          }
        }
      }

      if (!monster.isAlive()) {
        handleVictory(monster);
      }

    } catch (Exception e) {
      logger.error("전투 중 오류", e);
      System.out.println("전투 중 오류가 발생했습니다.");
    }
  }

  /**
   * 전투 상태를 표시합니다.
   */
  private void showBattleStatus(Monster monster) {
    System.out.println("\n--- 전투 상황 ---");
    System.out.printf("%s: %d/%d HP%n", player.getName(), player.getHp(), player.getMaxHp());
    System.out.printf("%s: %d HP%n", monster.getName(), monster.getHp());
    System.out.println("---------------");
  }

  /**
   * 플레이어 공격을 처리합니다.
   */
  private void handlePlayerAttack(Monster monster) {
    int damage = player.getAttack() + random.nextInt(5);
    monster.takeDamage(damage);

    System.out.println(
        "💥 " + player.getName() + "이(가) " + monster.getName() + "에게 " + damage + "의 데미지를 입혔습니다!");

    if (!monster.isAlive()) {
      System.out.println("🎯 " + monster.getName() + "을(를) 물리쳤습니다!");
    }

    logger.debug("플레이어 공격: {} -> {} (데미지: {})", player.getName(), monster.getName(), damage);
  }

  /**
   * 몬스터 공격을 처리합니다.
   */
  private void handleMonsterAttack(Monster monster) {
    int monsterDamage = monster.getAttack() + random.nextInt(3);
    player.takeDamage(monsterDamage);

    System.out.println("💢 " + monster.getName() + "이(가) " + player.getName() + "에게 "
        + monsterDamage + "의 데미지를 입혔습니다!");
    System.out.println("현재 체력: " + player.getHp() + "/" + player.getMaxHp());

    logger.debug("몬스터 공격: {} -> {} (데미지: {})", monster.getName(), player.getName(), monsterDamage);
  }

  /**
   * 도망 시도를 처리합니다.
   * 
   * @return 도망 성공 시 true
   */
  private boolean attemptEscape() {
    if (random.nextInt(100) < ESCAPE_CHANCE) {
      System.out.println("🏃 성공적으로 도망쳤습니다!");
      logger.debug("도망 성공");
      return true;
    } else {
      System.out.println("❌ 도망치지 못했습니다!");
      logger.debug("도망 실패");
      return false;
    }
  }

  /**
   * 승리를 처리합니다.
   */
  private void handleVictory(Monster monster) {
    try {
      System.out.println("\n🏆 승리!");

      boolean levelUp = player.gainExp(monster.getExpReward());
      player.setGold(player.getGold() + monster.getGoldReward());

      System.out
          .println("📈 경험치 +" + monster.getExpReward() + ", 💰 골드 +" + monster.getGoldReward());

      if (levelUp) {
        System.out.println("🎉 축하합니다! 레벨이 올랐습니다!");
      }

      logger.info("전투 승리: {} (경험치: {}, 골드: {})", monster.getName(), monster.getExpReward(),
          monster.getGoldReward());

    } catch (Exception e) {
      logger.error("승리 처리 중 오류", e);
      System.out.println("승리 보상 처리 중 오류가 발생했습니다.");
    }
  }

  /**
   * 상점을 운영합니다.
   */
  private void shop() {
    try {
      System.out.println("\n🏪 === 상점 ===");
      System.out.println("💰 보유 골드: " + player.getGold());
      System.out
          .println("1. 체력 물약 (" + HEALTH_POTION_PRICE + "골드) - " + HEALTH_POTION_HEAL + " HP 회복");
      System.out.println("2. 나가기");

      int choice = InputValidator.getIntInput("선택: ", 1, 2);

      if (choice == 1) {
        buyHealthPotion();
      }

    } catch (Exception e) {
      logger.error("상점 이용 중 오류", e);
      System.out.println("상점 이용 중 오류가 발생했습니다.");
    }
  }

  /**
   * 체력 물약 구매를 처리합니다.
   */
  private void buyHealthPotion() {
    if (player.getGold() >= HEALTH_POTION_PRICE) {
      if (player.getHp() == player.getMaxHp()) {
        System.out.println("💚 체력이 이미 가득합니다!");
        return;
      }

      player.setGold(player.getGold() - HEALTH_POTION_PRICE);
      int oldHp = player.getHp();
      player.heal(HEALTH_POTION_HEAL);
      int actualHeal = player.getHp() - oldHp;

      System.out.println("🧪 체력 물약을 구매했습니다! " + actualHeal + " HP 회복");
      System.out.println("현재 체력: " + player.getHp() + "/" + player.getMaxHp());

      logger.debug("체력 물약 구매: {} HP 회복", actualHeal);
    } else {
      System.out.println("❌ 골드가 부족합니다!");
      logger.debug("체력 물약 구매 실패: 골드 부족 (보유: {}, 필요: {})", player.getGold(), HEALTH_POTION_PRICE);
    }
  }

  /**
   * 게임 저장을 처리합니다.
   */
  private void saveGame() {
    try {
      GameData.saveGame(player);
      logger.info("게임 저장 완료: {}", player.getName());
    } catch (GameData.GameDataException e) {
      logger.error("게임 저장 실패", e);
      System.out.println("게임 저장 실패: " + e.getMessage());
    }
  }

  /**
   * 게임 종료 확인을 처리합니다.
   * 
   * @return 종료 확인 시 true
   */
  private boolean confirmExit() {
    boolean shouldSave = InputValidator.getConfirmation("게임을 저장하고 종료하시겠습니까?");

    if (shouldSave) {
      saveGame();
    }

    return InputValidator.getConfirmation("정말로 게임을 종료하시겠습니까?");
  }

  /**
   * 게임 오버를 처리합니다.
   */
  private void handleGameOver() {
    System.out.println("\n💀 게임 오버!");
    System.out.println("모험가 " + player.getName() + "님의 모험이 끝났습니다.");
    System.out.printf("최종 레벨: %d, 획득한 골드: %d%n", player.getLevel(), player.getGold());

    logger.info("게임 오버: {} (레벨: {}, 골드: {})", player.getName(), player.getLevel(),
        player.getGold());

    if (InputValidator.getConfirmation("저장 파일을 삭제하시겠습니까?")) {
      GameData.deleteSaveFile();
    }
  }

  /**
   * 도움말을 표시합니다.
   */
  private void showHelp() {
    System.out.println("\n📖 === 게임 도움말 ===");
    System.out.println("• 탐험하기: 몬스터와 싸우고 경험치와 골드를 획득하세요");
    System.out.println("• 상태 확인: 캐릭터의 현재 상태를 확인하세요");
    System.out.println("• 상점: 골드로 체력 물약을 구매하세요");
    System.out.println("• 게임 저장: 현재 진행 상황을 저장하세요");
    System.out.println("• 레벨업: 경험치가 가득 차면 자동으로 레벨업됩니다");
    System.out.println("• 체력: 0이 되면 게임 오버입니다");
    System.out.println("====================");
  }
}
