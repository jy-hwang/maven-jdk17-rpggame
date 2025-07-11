package controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import config.BaseConstant;
import model.GameCharacter;
import model.Monster;
import model.item.GameConsumable;
import model.item.GameEquipment;
import model.item.GameItem;
import model.item.ItemRarity;
import service.GameDataService;

/**
 * 탐험 시스템을 전담하는 컨트롤러
 */
public class ExploreController {
  private static final Logger logger = LoggerFactory.getLogger(ExploreController.class);

  private final Random random;
  private final List<Monster> monsterTemplates;
  private final BattleController battleController;
  private final QuestController questController;
  private final InventoryController inventoryController;
  private final GameDataService.GameState gameState;

  public ExploreController(BattleController battleController, QuestController questController, InventoryController inventoryController,
      GameDataService.GameState gameState) {
    this.random = new Random();
    this.battleController = battleController;
    this.questController = questController;
    this.inventoryController = inventoryController;
    this.gameState = gameState;
    this.monsterTemplates = new ArrayList<>();

    initializeMonsters();
    logger.debug("ExploreController 초기화 완료");
  }

  /**
   * 몬스터 템플릿을 초기화합니다.
   */
  private void initializeMonsters() {
    monsterTemplates.add(new Monster("슬라임", 20, 5, 10, 5));
    monsterTemplates.add(new Monster("고블린", 30, 8, 15, 10));
    monsterTemplates.add(new Monster("오크", 50, 12, 25, 20));
    monsterTemplates.add(new Monster("스켈레톤", 40, 10, 20, 15));
    monsterTemplates.add(new Monster("트롤", 80, 15, 40, 30));
    monsterTemplates.add(new Monster("드래곤", 120, 25, 60, 50));

    logger.debug("몬스터 템플릿 초기화 완료: {}종류", monsterTemplates.size());
  }

  /**
   * 탐험을 시작합니다.
   * 
   * @param player 플레이어 캐릭터
   * @return 탐험 결과
   */
  public ExploreResult startExploration(GameCharacter player) {
    try {
      System.out.println("\n🌲 탐험을 시작합니다...");
      logger.info("탐험 시작: {}", player.getName());

      // 현재 위치 업데이트
      updatePlayerLocation(player);

      // 랜덤 이벤트 또는 몬스터 조우
      if (random.nextInt(100) < BaseConstant.RANDOM_EVENT_CHANCE) {
        return handleRandomEvent(player);
      } else {
        return handleMonsterEncounter(player);
      }

    } catch (Exception e) {
      logger.error("탐험 중 오류", e);
      System.out.println("탐험 중 오류가 발생했습니다.");
      return new ExploreResult(ExploreResult.ResultType.ERROR, "탐험 중 오류 발생");
    }
  }

  /**
   * 플레이어의 현재 위치를 업데이트합니다.
   */
  private void updatePlayerLocation(GameCharacter player) {
    String[] locations = {"숲속 깊은 곳", "고대 유적", "어두운 동굴", "험준한 산길", "신비한 호수", "폐허가 된 성", "마법의 숲", "용암 동굴"};

    String newLocation = locations[random.nextInt(locations.length)];
    gameState.setCurrentLocation(newLocation);

    System.out.println("📍 현재 위치: " + newLocation);
  }

  /**
   * 랜덤 이벤트를 처리합니다.
   */
  private ExploreResult handleRandomEvent(GameCharacter player) {
    RandomEventType eventType = RandomEventType.values()[random.nextInt(RandomEventType.values().length)];

    return switch (eventType) {
      case TREASURE_FOUND -> handleTreasureEvent(player);
      case HEALING_SPRING -> handleHealingEvent(player);
      case ANCIENT_KNOWLEDGE -> handleKnowledgeEvent(player);
      case MAGIC_CRYSTAL -> handleManaEvent(player);
      case MERCHANT_ENCOUNTER -> handleMerchantEvent(player);
      case MYSTERIOUS_SHRINE -> handleShrineEvent(player);
    };
  }

  /**
   * 보물 발견 이벤트를 처리합니다.
   */
  private ExploreResult handleTreasureEvent(GameCharacter player) {
    int foundGold = random.nextInt(50) + 20;
    player.setGold(player.getGold() + foundGold);

    String message = "💰 보물 상자를 발견했습니다! " + foundGold + " 골드를 획득했습니다!";
    System.out.println(message);

    // 추가로 아이템도 발견할 수 있음 (30% 확률)
    if (random.nextInt(100) < 30) {
      GameItem treasureItem = generateRandomTreasureItem();
      if (inventoryController.addItem(player, treasureItem, 1)) {
        message += "\n🎁 추가로 " + treasureItem.getName() + "을(를) 발견했습니다!";
        System.out.println("🎁 추가로 " + treasureItem.getName() + "을(를) 발견했습니다!");
      }
    }

    logger.debug("보물 이벤트: {} (골드: {})", player.getName(), foundGold);
    return new ExploreResult(ExploreResult.ResultType.TREASURE, message);
  }

  /**
   * 치유의 샘 이벤트를 처리합니다.
   */
  private ExploreResult handleHealingEvent(GameCharacter player) {
    int healAmount = random.nextInt(40) + 30;
    int oldHp = player.getHp();
    player.heal(healAmount);
    int actualHeal = player.getHp() - oldHp;

    String message = "💚 신비한 치유의 샘을 발견했습니다! " + actualHeal + " 체력을 회복했습니다!";
    System.out.println(message);

    logger.debug("치유 이벤트: {} (회복량: {})", player.getName(), actualHeal);
    return new ExploreResult(ExploreResult.ResultType.HEALING, message);
  }

  /**
   * 고대 지식 이벤트를 처리합니다.
   */
  private ExploreResult handleKnowledgeEvent(GameCharacter player) {
    int expAmount = random.nextInt(30) + 15;
    boolean levelUp = player.gainExp(expAmount);

    String message = "📚 고대 문서를 발견했습니다! " + expAmount + " 경험치를 획득했습니다!";
    System.out.println(message);

    if (levelUp) {
      message += "\n🎉 깨달음을 얻어 레벨이 올랐습니다!";
      System.out.println("🎉 깨달음을 얻어 레벨이 올랐습니다!");
      questController.updateLevelProgress(player);
    }

    logger.debug("지식 이벤트: {} (경험치: {}, 레벨업: {})", player.getName(), expAmount, levelUp);
    return new ExploreResult(ExploreResult.ResultType.KNOWLEDGE, message);
  }

  /**
   * 마법 크리스탈 이벤트를 처리합니다.
   */
  private ExploreResult handleManaEvent(GameCharacter player) {
    int manaAmount = random.nextInt(25) + 20;
    int oldMana = player.getMana();
    player.restoreMana(manaAmount);
    int actualRestore = player.getMana() - oldMana;

    String message = "✨ 마법의 크리스탈을 발견했습니다! " + actualRestore + " 마나를 회복했습니다!";
    System.out.println(message);

    logger.debug("마나 이벤트: {} (회복량: {})", player.getName(), actualRestore);
    return new ExploreResult(ExploreResult.ResultType.MANA_RESTORE, message);
  }

  /**
   * 떠돌이 상인 이벤트를 처리합니다.
   */
  private ExploreResult handleMerchantEvent(GameCharacter player) {
    String message = "🧙‍♂️ 떠돌이 상인을 만났습니다! 특별한 거래를 제안합니다.";
    System.out.println(message);

    // 골드로 특별한 아이템 구매 기회
    if (player.getGold() >= 50) {
      GameItem specialItem = generateSpecialMerchantItem();
      System.out.println("🛍️ " + specialItem.getName() + "을(를) 50골드에 판매합니다.");
      System.out.println("💰 현재 골드: " + player.getGold());

      // 간단한 자동 구매 로직 (나중에 선택지로 확장 가능)
      if (random.nextBoolean() && inventoryController.addItem(player, specialItem, 1)) {
        player.setGold(player.getGold() - 50);
        message += "\n🎁 " + specialItem.getName() + "을(를) 구매했습니다!";
        System.out.println("🎁 " + specialItem.getName() + "을(를) 구매했습니다!");
      } else {
        message += "\n💭 이번에는 거래하지 않기로 했습니다.";
        System.out.println("💭 이번에는 거래하지 않기로 했습니다.");
      }
    } else {
      message += "\n💸 골드가 부족해 거래할 수 없습니다.";
      System.out.println("💸 골드가 부족해 거래할 수 없습니다.");
    }

    logger.debug("상인 이벤트: {}", player.getName());
    return new ExploreResult(ExploreResult.ResultType.MERCHANT, message);
  }

  /**
   * 신비한 제단 이벤트를 처리합니다.
   */
  private ExploreResult handleShrineEvent(GameCharacter player) {
    String message = "🗿 신비한 제단을 발견했습니다!";
    System.out.println(message);

    // 다양한 축복 효과 중 하나
    ShrineBlessing blessing = ShrineBlessing.values()[random.nextInt(ShrineBlessing.values().length)];

    switch (blessing) {
      case STRENGTH -> {
        // 임시 공격력 증가 효과 (실제 구현에서는 버프 시스템 필요)
        message += "\n⚔️ 힘의 축복을 받았습니다! (다음 전투에서 공격력 증가)";
        System.out.println("⚔️ 힘의 축복을 받았습니다!");
      }
      case VITALITY -> {
        int bonusHp = 20;
        player.heal(bonusHp);
        message += "\n❤️ 생명력의 축복을 받았습니다! " + bonusHp + " 체력을 회복했습니다!";
        System.out.println("❤️ 생명력의 축복을 받았습니다!");
      }
      case WISDOM -> {
        int bonusMana = 15;
        player.restoreMana(bonusMana);
        message += "\n🔮 지혜의 축복을 받았습니다! " + bonusMana + " 마나를 회복했습니다!";
        System.out.println("🔮 지혜의 축복을 받았습니다!");
      }
      case FORTUNE -> {
        int bonusGold = 30;
        player.setGold(player.getGold() + bonusGold);
        message += "\n💰 행운의 축복을 받았습니다! " + bonusGold + " 골드를 획득했습니다!";
        System.out.println("💰 행운의 축복을 받았습니다!");
      }
    }

    logger.debug("제단 이벤트: {} (축복: {})", player.getName(), blessing);
    return new ExploreResult(ExploreResult.ResultType.SHRINE, message);
  }

  /**
   * 몬스터 조우를 처리합니다.
   */
  private ExploreResult handleMonsterEncounter(GameCharacter player) {
    Monster monster = getRandomMonster(player.getLevel());
    System.out.println("👹 " + monster.getName() + "을(를) 만났습니다!");

    BattleController.BattleResult result = battleController.startBattle(player, monster);

    String message = switch (result) {
      case VICTORY -> {
        // 퀘스트 진행도 업데이트
        questController.updateKillProgress(monster.getName());

        // 아이템 드롭 처리
        if (random.nextInt(100) < BaseConstant.ITEM_DROP_CHANCE) {
          GameItem droppedItem = generateRandomDropItem();
          if (inventoryController.addItem(player, droppedItem, 1)) {
            System.out.println("🎁 " + droppedItem.getName() + "을(를) 획득했습니다!");
            yield "전투 승리! " + droppedItem.getName() + " 획득!";
          } else {
            yield "전투 승리! (인벤토리 가득참)";
          }
        } else {
          yield "전투 승리!";
        }
      }
      case DEFEAT -> "전투 패배...";
      case ESCAPED -> "성공적으로 도망쳤습니다!";
      case ERROR -> "전투 중 오류 발생";
    };

    ExploreResult.ResultType resultType = switch (result) {
      case VICTORY -> ExploreResult.ResultType.BATTLE_VICTORY;
      case DEFEAT -> ExploreResult.ResultType.BATTLE_DEFEAT;
      case ESCAPED -> ExploreResult.ResultType.BATTLE_ESCAPED;
      case ERROR -> ExploreResult.ResultType.ERROR;
    };

    logger.debug("몬스터 조우: {} vs {} (결과: {})", player.getName(), monster.getName(), result);
    return new ExploreResult(resultType, message);
  }

  /**
   * 플레이어 레벨에 적합한 랜덤 몬스터를 생성합니다.
   */
  public Monster getRandomMonster(int playerLevel) {
    // 플레이어 레벨에 따른 몬스터 선택 로직
    List<Monster> suitableMonsters = monsterTemplates.stream().filter(monster -> {
      int monsterLevel = estimateMonsterLevel(monster);
      return monsterLevel <= playerLevel + 2 && monsterLevel >= Math.max(1, playerLevel - 1);
    }).toList();

    if (suitableMonsters.isEmpty()) {
      suitableMonsters = monsterTemplates; // 적절한 몬스터가 없으면 전체에서 선택
    }

    Monster template = suitableMonsters.get(random.nextInt(suitableMonsters.size()));

    // 몬스터의 새 인스턴스 생성 (원본 데이터 보호)
    return new Monster(template.getName(), template.getHp(), template.getAttack(), template.getExpReward(), template.getGoldReward());
  }

  /**
   * 몬스터의 추정 레벨을 계산합니다.
   */
  private int estimateMonsterLevel(Monster monster) {
    // HP와 공격력을 기반으로 몬스터 레벨 추정
    return (monster.getHp() + monster.getAttack() * 2) / 15;
  }

  /**
   * 랜덤 보물 아이템을 생성합니다.
   */
  private GameItem generateRandomTreasureItem() {
    GameItem[] treasureItems = {new GameConsumable("고급 체력 물약", "HP를 100 회복합니다", 50, ItemRarity.UNCOMMON, 100, 0, 0, true),
        new GameEquipment("은검", "은으로 만든 아름다운 검", 150, ItemRarity.UNCOMMON, GameEquipment.EquipmentType.WEAPON, 12, 0, 0),
        new GameEquipment("마법사의 로브", "마법사가 입던 로브", 120, ItemRarity.RARE, GameEquipment.EquipmentType.ARMOR, 0, 8, 20)};

    return treasureItems[random.nextInt(treasureItems.length)];
  }

  /**
   * 특별한 상인 아이템을 생성합니다.
   */
  private GameItem generateSpecialMerchantItem() {
    GameItem[] merchantItems = {new GameEquipment("여행자의 부츠", "이동 속도를 증가시키는 부츠", 75, ItemRarity.RARE, GameEquipment.EquipmentType.ACCESSORY, 0, 3, 15),
        new GameConsumable("신비한 물약", "HP와 MP를 100 회복", 80, ItemRarity.RARE, 100, 100, 0, true),
        new GameEquipment("고대의 목걸이", "경험치 획득량을 증가시킴", 100, ItemRarity.EPIC, GameEquipment.EquipmentType.ACCESSORY, 0, 0, 25)};

    return merchantItems[random.nextInt(merchantItems.length)];
  }

  /**
   * 랜덤 드롭 아이템을 생성합니다.
   */
  private GameItem generateRandomDropItem() {
    GameItem[] dropItems = {new GameConsumable("체력 물약", "HP를 50 회복합니다", 20, ItemRarity.COMMON, 50, 0, 0, true),
        new GameConsumable("마나 물약", "마나를 30 회복합니다", 25, ItemRarity.COMMON, 0, 30, 0, true),
        new GameEquipment("낡은 검", "사용감이 있지만 쓸만한 검", 40, ItemRarity.COMMON, GameEquipment.EquipmentType.WEAPON, 5, 0, 0),
        new GameEquipment("가죽 갑옷", "기본적인 가죽 갑옷", 60, ItemRarity.COMMON, GameEquipment.EquipmentType.ARMOR, 0, 4, 5)};

    return dropItems[random.nextInt(dropItems.length)];
  }

  /**
   * 탐험 결과 클래스
   */
  public static class ExploreResult {
    private final ResultType type;
    private final String message;

    public ExploreResult(ResultType type, String message) {
      this.type = type;
      this.message = message;
    }

    public ResultType getType() {
      return type;
    }

    public String getMessage() {
      return message;
    }

    public enum ResultType {
      TREASURE, // 보물 발견
      HEALING, // 치유 이벤트
      KNOWLEDGE, // 지식 획득
      MANA_RESTORE, // 마나 회복
      MERCHANT, // 상인 조우
      SHRINE, // 제단 이벤트
      BATTLE_VICTORY, // 전투 승리
      BATTLE_DEFEAT, // 전투 패배
      BATTLE_ESCAPED, // 전투 도망
      ERROR // 오류
    }
  }

  /**
   * 랜덤 이벤트 타입 열거형
   */
  private enum RandomEventType {
    TREASURE_FOUND, // 보물 발견
    HEALING_SPRING, // 치유의 샘
    ANCIENT_KNOWLEDGE, // 고대 지식
    MAGIC_CRYSTAL, // 마법 크리스탈
    MERCHANT_ENCOUNTER, // 상인 조우
    MYSTERIOUS_SHRINE // 신비한 제단
  }

  /**
   * 제단 축복 타입 열거형
   */
  private enum ShrineBlessing {
    STRENGTH, // 힘의 축복
    VITALITY, // 생명력의 축복
    WISDOM, // 지혜의 축복
    FORTUNE // 행운의 축복
  }
}
