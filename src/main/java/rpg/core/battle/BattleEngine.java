package rpg.core.battle;

import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.application.service.QuestManager;
import rpg.application.validator.InputValidator;
import rpg.core.engine.GameState;
import rpg.domain.item.GameConsumable;
import rpg.domain.item.GameEquipment;
import rpg.domain.item.GameItem;
import rpg.domain.item.ItemRarity;
import rpg.domain.monster.Monster;
import rpg.domain.player.Player;
import rpg.domain.skill.Skill;
import rpg.shared.constant.BattleConstants;

/**
 * 전투 시스템을 전담하는 컨트롤러
 */
public class BattleEngine {
  private static final Logger logger = LoggerFactory.getLogger(BattleEngine.class);

  private final Random random;
  private final QuestManager questManager;
  private final GameState gameState;

  public BattleEngine(QuestManager questManager, GameState gameState) {
    this.random = new Random();
    this.questManager = questManager;
    this.gameState = gameState;
    logger.debug("BattleController 초기화 완료");
  }

  /**
   * 전투를 시작합니다.
   * 
   * @param player 플레이어 캐릭터
   * @param monster 적 몬스터
   * @return 전투 결과 (승리, 패배, 도망)
   */
  public BattleResult startBattle(Player player, Monster monster) {
    try {
      System.out.println("\n⚔️ 전투 시작!");
      System.out.println("⚔️ " + monster.getName() + "이(가) 나타났습니다!");
      logger.info("전투 시작: {} vs {}", player.getName(), monster.getName());

      while (player.isAlive() && monster.isAlive()) {
        showBattleStatus(player, monster);

        BattleAction action = getBattleAction();
        boolean playerTurnUsed = false;

        switch (action) {
          case ATTACK:
            handlePlayerAttack(player, monster);
            playerTurnUsed = true;
            break;
          case SKILL:
            playerTurnUsed = useSkillInBattle(player, monster);
            break;
          case ITEM:
            playerTurnUsed = useItemInBattle(player);
            break;
          case ESCAPE:
            if (attemptEscape()) {
              player.postBattleRegeneration(); // 도망 성공 시 회복
              return BattleResult.ESCAPED;
            }
            playerTurnUsed = true;
            break;
        }

        // 몬스터가 살아있고 플레이어가 턴을 사용했으면 몬스터 공격
        if (monster.isAlive() && playerTurnUsed) {
          handleMonsterAttack(player, monster);
        }

        // 턴 종료 처리
        player.endTurn();
      }

      if (!monster.isAlive()) {
        handleVictory(player, monster);
        player.postBattleRegeneration(); // 승리 후 회복
        return BattleResult.VICTORY;
      } else {
        return BattleResult.DEFEAT;
      }

    } catch (Exception e) {
      logger.error("전투 중 오류", e);
      System.out.println("전투 중 오류가 발생했습니다.");
      return BattleResult.ERROR;
    }
  }

  /**
   * 전투 행동을 선택받습니다.
   */
  private BattleAction getBattleAction() {
    System.out.println("\n전투 행동:");
    System.out.println("1. 일반 공격");
    System.out.println("2. 스킬 사용");
    System.out.println("3. 아이템 사용");
    System.out.println("4. 도망");

    int choice = InputValidator.getIntInput("선택: ", 1, 4);

    return switch (choice) {
      case 1 -> BattleAction.ATTACK;
      case 2 -> BattleAction.SKILL;
      case 3 -> BattleAction.ITEM;
      case 4 -> BattleAction.ESCAPE;
      default -> BattleAction.ATTACK;
    };
  }

  /**
   * 전투 상태를 표시합니다.
   */
  private void showBattleStatus(Player player, Monster monster) {
    System.out.println("\n--- 전투 상황 ---");
    System.out.printf("🧙 %s: %d/%d HP | %d/%d MP%n", player.getName(), player.getHp(), player.getTotalMaxHp(), player.getMana(),
        player.getMaxMana());
    System.out.printf("👹 %s: %d HP%n", monster.getName(), monster.getHp());
    System.out.println("---------------");
  }

  /**
   * 플레이어 공격을 처리합니다.
   */
  private void handlePlayerAttack(Player player, Monster monster) {
    int damage = player.getAttack() + random.nextInt(5);
    monster.takeDamage(damage);

    System.out.println("💥 " + player.getName() + "이(가) " + monster.getName() + "에게 " + damage + "의 데미지를 입혔습니다!");

    if (!monster.isAlive()) {
      System.out.println("🎯 " + monster.getName() + "을(를) 물리쳤습니다!");
    }

    logger.debug("플레이어 공격: {} -> {} (데미지: {})", player.getName(), monster.getName(), damage);
  }

  /**
   * 몬스터 공격을 처리합니다.
   */
  private void handleMonsterAttack(Player player, Monster monster) {
    int monsterDamage = monster.getAttack() + random.nextInt(3);
    int actualDamage = player.takeDamage(monsterDamage);

    System.out.println("💢 " + monster.getName() + "이(가) " + player.getName() + "에게 " + actualDamage + "의 데미지를 입혔습니다!");
    System.out.printf("현재 체력: %d/%d%n", player.getHp(), player.getTotalMaxHp());

    logger.debug("몬스터 공격: {} -> {} (데미지: {})", monster.getName(), player.getName(), actualDamage);
  }

  /**
   * 전투 중 스킬을 사용합니다.
   */
  private boolean useSkillInBattle(Player player, Monster monster) {
    var availableSkills = player.getSkillManager().getAvailableSkills(player);

    if (availableSkills.isEmpty()) {
      System.out.println("사용할 수 있는 스킬이 없습니다.");
      return false;
    }

    System.out.println("\n사용 가능한 스킬:");
    for (int i = 0; i < availableSkills.size(); i++) {
      Skill skill = availableSkills.get(i);
      System.out.printf("%d. %s (마나: %d)%n", i + 1, skill.getName(), skill.getManaCost());
    }

    int skillIndex = InputValidator.getIntInput("사용할 스킬 번호 (0: 취소): ", 0, availableSkills.size()) - 1;
    if (skillIndex < 0)
      return false;

    Skill skill = availableSkills.get(skillIndex);
    Skill.SkillResult result = player.getSkillManager().useSkill(skill.getName(), player, monster);

    System.out.println("✨ " + result.getMessage());

    if (result.isSuccess()) {
      logger.debug("전투 중 스킬 사용 성공: {}", skill.getName());
      return true;
    } else {
      logger.debug("전투 중 스킬 사용 실패: {}", skill.getName());
      return false;
    }
  }

  /**
   * 전투 중 아이템을 사용합니다.
   */
  private boolean useItemInBattle(Player player) {
    var items = player.getInventory().getItems();
    var consumables = items.stream().filter(stack -> stack.getItem() instanceof GameConsumable).toList();

    if (consumables.isEmpty()) {
      System.out.println("사용할 수 있는 아이템이 없습니다.");
      return false;
    }

    System.out.println("\n사용 가능한 아이템:");
    for (int i = 0; i < consumables.size(); i++) {
      var stack = consumables.get(i);
      System.out.printf("%d. %s x%d%n", i + 1, stack.getItem().getName(), stack.getQuantity());
    }

    int itemIndex = InputValidator.getIntInput("사용할 아이템 번호 (0: 취소): ", 0, consumables.size()) - 1;
    if (itemIndex < 0)
      return false;

    var selectedStack = consumables.get(itemIndex);
    boolean used = player.getInventory().useItem(selectedStack.getItem().getName(), player);

    if (used) {
      System.out.println("🧪 아이템을 사용했습니다!");
      return true;
    } else {
      System.out.println("아이템 사용에 실패했습니다.");
      return false;
    }
  }

  /**
   * 도망 시도를 처리합니다.
   */
  private boolean attemptEscape() {
    if (random.nextInt(100) < BattleConstants.ESCAPE_CHANCE) {
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
  private void handleVictory(Player player, Monster monster) {
    try {
      System.out.println("\n🏆 승리!");

      boolean levelUp = player.gainExp(monster.getExpReward());
      player.setGold(player.getGold() + monster.getGoldReward());

      System.out.println("📈 경험치 +" + monster.getExpReward() + ", 💰 골드 +" + monster.getGoldReward());

      if (levelUp) {
        System.out.println("🎉 축하합니다! 레벨이 올랐습니다!");
      }

      // 게임 통계 업데이트
      gameState.incrementMonstersKilled();

      // 퀘스트 진행도 업데이트
      questManager.updateKillProgress(monster.getName());

      // 아이템 드롭 (20% 확률)
      if (random.nextInt(100) < 20) {
        dropRandomItem(player);
      }

      logger.info("전투 승리: {} (경험치: {}, 골드: {})", monster.getName(), monster.getExpReward(), monster.getGoldReward());

    } catch (Exception e) {
      logger.error("승리 처리 중 오류", e);
      System.out.println("승리 보상 처리 중 오류가 발생했습니다.");
    }
  }

  /**
   * 랜덤 아이템을 드롭합니다.
   */
  private void dropRandomItem(Player player) {
    GameItem[] possibleDrops = {new GameConsumable("HEALTH_POTION", "체력 물약", "HP를 50 회복합니다", 20, ItemRarity.COMMON, 50, 0, 0, true),
        new GameConsumable("MANA_POTION", "마나 물약", "마나를 30 회복합니다", 25, ItemRarity.COMMON, 0, 30, 0, true),
        new GameEquipment("IRON_SWORD", "철 검", "날카로운 철검", 80, ItemRarity.UNCOMMON, GameEquipment.EquipmentType.WEAPON, 8, 0, 0),
        new GameEquipment("LEATHER_ARMOR", "가죽 갑옷", "질긴 가죽으로 만든 갑옷", 60, ItemRarity.COMMON, GameEquipment.EquipmentType.ARMOR, 0, 6, 10)};

    GameItem droppedItem = possibleDrops[random.nextInt(possibleDrops.length)];

    if (player.getInventory().addItem(droppedItem, 1)) {
      System.out.println("🎁 " + droppedItem.getName() + "을(를) 획득했습니다!");
    } else {
      System.out.println("💼 인벤토리가 가득 차서 " + droppedItem.getName() + "을(를) 획득할 수 없습니다!");
    }
  }

  /**
   * 전투 행동 열거형
   */
  public enum BattleAction {
    ATTACK, // 일반 공격
    SKILL, // 스킬 사용
    ITEM, // 아이템 사용
    ESCAPE // 도망
  }

  /**
   * 전투 결과 열거형
   */
  public enum BattleResult {
    VICTORY, // 승리
    DEFEAT, // 패배
    ESCAPED, // 도망 성공
    ERROR // 오류 발생
  }
}
