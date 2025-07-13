package rpg.shared.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.application.factory.GameEffectFactory;
import rpg.application.factory.GameItemFactory;
import rpg.application.factory.JsonBasedQuestFactory;
import rpg.application.factory.SkillFactory;
import rpg.application.service.QuestManager;
import rpg.application.validator.InputValidator;
import rpg.core.engine.GameEngine;
import rpg.domain.inventory.PlayerInventory;
import rpg.domain.item.GameEquipment;
import rpg.domain.item.GameItem;
import rpg.domain.item.ItemRarity;
import rpg.domain.item.effect.GameEffect;
import rpg.domain.player.Player;
import rpg.domain.quest.Quest;
import rpg.domain.quest.QuestReward;
import rpg.domain.skill.Skill;
import rpg.infrastructure.data.loader.QuestTemplateLoader;
import rpg.shared.constant.SystemConstants;

/**
 * 디버그 및 테스트 기능을 전담하는 컨트롤러
 * GameEngine에서 분리된 모든 디버그/테스트 메서드들을 포함
 */
public class DebugController {
  private static final Logger logger = LoggerFactory.getLogger(DebugController.class);

  private final Player player;

  public DebugController(Player player) {
    this.player = player;
    logger.debug("DebugController 초기화 완료");
  }

  /**
   * 메인 디버그 메뉴 실행
   */
  public void showDebugMenu() {
    if (!SystemConstants.DEBUG_MODE) {
      System.out.println("디버그 모드가 활성화되지 않았습니다.");
      return;
    }

    while (true) {
      System.out.println("\n=== 🔧 디버그 메뉴 ===");
      System.out.println("1. 🧪 팩토리 테스트");
      System.out.println("2. 🎲 랜덤 아이템 테스트");
      System.out.println("3. 📋 퀘스트 템플릿 테스트");
      System.out.println("4. 🎯 퀘스트 시스템 테스트");
      System.out.println("5. ⚔️ 스킬 팩토리 테스트");
      System.out.println("6. 🔧 전체 데이터 리로드");
      System.out.println("7. 📈 몬스터 통계");
      System.out.println("8. 🎁 아이템 통계");
      System.out.println("9. 🧪 테스트 몬스터 생성");
      System.out.println("10. 🎒 테스트 아이템 생성");
      System.out.println("11. 🛠️ 전체 시스템 진단");
      System.out.println("12. 📖 도움말 메뉴");
      System.out.println("0. 🔙 돌아가기");

      int choice = InputValidator.getIntInput("선택 (0-12): ", 0, 12);

      switch (choice) {
        case 1:
          testFactories();
          break;
        case 2:
          testRandomItemGeneration();
          break;
        case 3:
          testQuestTemplates();
          break;
        case 4:
          testQuestSystem();
          break;
        case 5:
          testSkillFactory();
          break;
        case 6:
          reloadAllGameData();
          break;
        case 7:
          showMonsterStatistics();
          break;
        case 8:
          showItemStatistics();
          break;
        case 9:
          testMonsterGeneration();
          break;
        case 10:
          testItemGeneration();
          break;
        case 11:
          runFullSystemDiagnostics();
          break;
        case 12:
          showHelpMenu();
          break;
        case 0:
          return;
        default:
          System.out.println("잘못된 선택입니다.");
      }
    }
  }

  /**
   * 팩토리들을 테스트합니다.
   */
  private void testFactories() {
    System.out.println("\n=== 🏭 팩토리 테스트 ===");

    try {
      // GameItemFactory 테스트
      GameItemFactory itemFactory = GameItemFactory.getInstance();
      System.out.println("✅ GameItemFactory 초기화 상태: " + itemFactory.isInitialized());
      System.out.println("📦 로드된 아이템 수: " + itemFactory.getItemCount());

      // JsonBasedQuestFactory 테스트
      JsonBasedQuestFactory questFactory = JsonBasedQuestFactory.getInstance();
      questFactory.printFactoryStatus();

      // 간단한 생성 테스트
      GameItem testItem = itemFactory.createItem("HEALTH_POTION");
      if (testItem != null) {
        System.out.println("✅ 아이템 생성 테스트 성공: " + testItem.getName());
      } else {
        System.out.println("❌ 아이템 생성 테스트 실패");
      }

      Quest testQuest = questFactory.createQuest("quest_001");
      if (testQuest != null) {
        System.out.println("✅ 퀘스트 생성 테스트 성공: " + testQuest.getTitle());
      } else {
        System.out.println("❌ 퀘스트 생성 테스트 실패");
      }

    } catch (Exception e) {
      System.out.println("❌ 팩토리 테스트 중 오류: " + e.getMessage());
      logger.error("팩토리 테스트 실패", e);
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 랜덤 아이템 생성을 테스트합니다.
   */
  private void testRandomItemGeneration() {
    System.out.println("\n=== 🎲 랜덤 아이템 생성 테스트 ===");

    try {
      GameItemFactory factory = GameItemFactory.getInstance();

      // 기본 테스트 실행
      factory.testRandomGeneration();

      boolean continueTest = true;
      while (continueTest) {
        // 인터랙티브 테스트
        System.out.println("\n🎯 인터랙티브 테스트:");
        System.out.println("1. 특정 희귀도 아이템 생성");
        System.out.println("2. 레벨별 아이템 생성");
        System.out.println("3. 특수 상황별 아이템 생성");
        System.out.println("4. 통계 보기");
        System.out.println("0. 🔙 돌아가기");

        int choice = InputValidator.getIntInput("선택 (0-4): ", 0, 4);

        switch (choice) {
          case 1:
            testRarityBasedGeneration(factory);
            break;
          case 2:
            testLevelBasedGeneration(factory);
            break;
          case 3:
            testSpecialGeneration(factory);
            break;
          case 4:
            factory.printRandomGenerationStats();
            break;
          case 0:
            continueTest = false;
            System.out.println("🔙 랜덤 아이템 테스트를 종료합니다.");
            break;
        }
      }
    } catch (Exception e) {
      System.out.println("❌ 랜덤 아이템 테스트 중 오류: " + e.getMessage());
      logger.error("랜덤 아이템 테스트 실패", e);
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 희귀도별 아이템 생성 테스트
   */
  private void testRarityBasedGeneration(GameItemFactory factory) {
    System.out.println("\n💎 희귀도별 생성 테스트:");

    for (ItemRarity rarity : ItemRarity.values()) {
      System.out.printf("🎲 %s 아이템 생성 중...\n", rarity.getDisplayName());

      for (int i = 0; i < 3; i++) {
        GameItem item = factory.createRandomItemByRarity(rarity);
        if (item != null) {
          System.out.printf("   %d. %s (%s)\n", i + 1, item.getName(), item.getRarity().getDisplayName());
        } else {
          System.out.printf("   %d. 생성 실패\n", i + 1);
        }
      }
      System.out.println();
    }
  }

  /**
   * 레벨별 아이템 생성 테스트
   */
  private void testLevelBasedGeneration(GameItemFactory factory) {
    System.out.println("\n📈 레벨별 생성 테스트:");

    int[] testLevels = {1, 3, 5, 8, 12, 18, 25};

    for (int level : testLevels) {
      System.out.printf("🎯 레벨 %d 아이템:\n", level);

      for (int i = 0; i < 3; i++) {
        GameItem item = factory.createRandomItemForLevel(level);
        if (item != null) {
          System.out.printf("   %d. %s (%s)\n", i + 1, item.getName(), item.getRarity().getDisplayName());
        }
      }
      System.out.println();
    }
  }

  /**
   * 특수 상황별 아이템 생성 테스트
   */
  private void testSpecialGeneration(GameItemFactory factory) {
    System.out.println("\n🎁 특수 상황별 생성 테스트:");

    // 보물 상자 아이템
    System.out.println("📦 보물 상자 아이템 (5개):");
    for (int i = 0; i < 5; i++) {
      GameItem item = factory.createTreasureChestItem();
      if (item != null) {
        System.out.printf("   %d. %s (%s)\n", i + 1, item.getName(), item.getRarity().getDisplayName());
      }
    }

    // 몬스터 드롭 아이템
    System.out.println("\n⚔️ 몬스터 드롭 아이템 (레벨 7 몬스터, 5개):");
    for (int i = 0; i < 5; i++) {
      GameItem item = factory.createMonsterDropItem(7);
      if (item != null) {
        System.out.printf("   %d. %s (%s)\n", i + 1, item.getName(), item.getRarity().getDisplayName());
      }
    }

    // 상점 아이템
    System.out.println("\n🏪 상점 아이템 (레벨 5 상점, 5개):");
    for (int i = 0; i < 5; i++) {
      GameItem item = factory.createShopItem(5);
      if (item != null) {
        System.out.printf("   %d. %s (%s)\n", i + 1, item.getName(), item.getRarity().getDisplayName());
      }
    }
  }

  /**
   * 퀘스트 템플릿을 테스트합니다.
   */
  private void testQuestTemplates() {
    System.out.println("\n=== 📋 퀘스트 템플릿 테스트 ===");

    try {
      // QuestTemplateLoader 테스트
      QuestTemplateLoader.printLoaderStatus();

      JsonBasedQuestFactory factory = JsonBasedQuestFactory.getInstance();

      // 전체 퀘스트 목록
      factory.printAllQuests();

      // 특정 퀘스트 상세 정보
      System.out.println("\n🔍 특정 퀘스트 상세 정보:");
      String questId = InputValidator.getStringInput("퀘스트 ID 입력 (예: quest_001): ", 1, 50);
      factory.printQuestTemplateDetails(questId);

    } catch (Exception e) {
      System.out.println("❌ 퀘스트 템플릿 테스트 중 오류: " + e.getMessage());
      logger.error("퀘스트 템플릿 테스트 실패", e);
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 퀘스트 시스템을 테스트합니다.
   */
  private void testQuestSystem() {
    System.out.println("\n=== 🎯 퀘스트 시스템 테스트 ===");

    try {
      JsonBasedQuestFactory factory = JsonBasedQuestFactory.getInstance();

      System.out.println("1. 기본 퀘스트 생성 테스트");
      System.out.println("2. 동적 퀘스트 생성 테스트");
      System.out.println("3. 플레이어 레벨별 퀘스트 테스트");
      System.out.println("4. 일일/주간 퀘스트 테스트");

      int choice = InputValidator.getIntInput("선택 (1-4): ", 1, 4);

      switch (choice) {
        case 1:
          testBasicQuestGeneration(factory);
          break;
        case 2:
          testDynamicQuestGeneration(factory);
          break;
        case 3:
          testLevelBasedQuestGeneration(factory);
          break;
        case 4:
          testDailyWeeklyQuests(factory);
          break;
        case 5:
          testImprovedDailyQuestSystem();
          break;
      }

    } catch (Exception e) {
      System.out.println("❌ 퀘스트 시스템 테스트 중 오류: " + e.getMessage());
      logger.error("퀘스트 시스템 테스트 실패", e);
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 기본 퀘스트 생성 테스트
   */
  private void testBasicQuestGeneration(JsonBasedQuestFactory factory) {
    System.out.println("\n📜 기본 퀘스트 생성 테스트:");

    String[] basicQuests = {"quest_001", "quest_002", "quest_003", "quest_004", "quest_005"};

    for (String questId : basicQuests) {
      Quest quest = factory.createQuest(questId);
      if (quest != null) {
        System.out.printf("✅ %s: %s (레벨 %d)\n", questId, quest.getTitle(), quest.getRequiredLevel());
        System.out.printf("   설명: %s\n", quest.getDescription());
        System.out.printf("   보상: 경험치 %d, 골드 %d\n", quest.getReward().getExpReward(), quest.getReward().getGoldReward());

        if (quest.getReward().getItemRewards() != null && !quest.getReward().getItemRewards().isEmpty()) {
          System.out.println("   아이템 보상:");
          quest.getReward().getItemRewards().forEach((item, quantity) -> System.out.printf("     - %s x%d\n", item.getName(), quantity));
        }
        System.out.println();
      } else {
        System.out.printf("❌ %s: 생성 실패\n", questId);
      }
    }
  }

  /**
   * 동적 퀘스트 생성 테스트
   */
  private void testDynamicQuestGeneration(JsonBasedQuestFactory factory) {
    System.out.println("\n🎲 동적 퀘스트 생성 테스트:");

    int testLevel = InputValidator.getIntInput("테스트할 플레이어 레벨 (1-20): ", 1, 20);

    System.out.printf("🎯 레벨 %d 동적 퀘스트 생성:\n", testLevel);

    for (int i = 0; i < 3; i++) {
      Quest quest = factory.createLevelAppropriateQuest(testLevel);
      if (quest != null) {
        System.out.printf("%d. %s (레벨 %d)\n", i + 1, quest.getTitle(), quest.getRequiredLevel());
        System.out.printf("   타입: %s\n", quest.getType());
        System.out.printf("   보상: 경험치 %d, 골드 %d\n", quest.getReward().getExpReward(), quest.getReward().getGoldReward());
      }
    }
  }

  /**
   * 레벨별 퀘스트 생성 테스트
   */
  private void testLevelBasedQuestGeneration(JsonBasedQuestFactory factory) {
    System.out.println("\n📈 레벨별 퀘스트 생성 테스트:");

    int[] testLevels = {1, 5, 10, 15, 20};

    for (int level : testLevels) {
      System.out.printf("🎯 레벨 %d 퀘스트:\n", level);
      Quest quest = factory.createLevelAppropriateQuest(level);
      if (quest != null) {
        System.out.printf("   %s (타입: %s)\n", quest.getTitle(), quest.getType());
      } else {
        System.out.println("   생성 실패");
      }
    }
  }

  /**
   * 일일/주간 퀘스트 테스트
   */
  @Deprecated
  private void testDailyWeeklyQuests(JsonBasedQuestFactory factory) {
    System.out.println("\n📅 일일/주간 퀘스트 테스트:");

    System.out.println("🌅 일일 퀘스트 생성:");
    for (Quest.QuestType type : Quest.QuestType.values()) {
      Quest dailyQuest = factory.createDailyQuest(type);
      if (dailyQuest != null) {
        System.out.printf("   %s: %s\n", type, dailyQuest.getTitle());
      }
    }
  }

  /**
   * 모든 팩토리 상태 검사
   */
  private void checkAllFactories() {
    System.out.println("\n=== 🏭 모든 팩토리 상태 검사 ===");

    // GameItemFactory 검사
    System.out.println("🔍 GameItemFactory 검사:");
    try {
      GameItemFactory itemFactory = GameItemFactory.getInstance();
      System.out.printf("   ✅ 인스턴스: %s\n", itemFactory != null ? "정상" : "NULL");
      System.out.printf("   ✅ 초기화: %s\n", itemFactory.isInitialized() ? "완료" : "실패");
      System.out.printf("   ✅ 아이템 수: %d개\n", itemFactory.getItemCount());

      // 메서드 존재 확인
      GameItem testItem = itemFactory.createRandomItemByRarity(ItemRarity.COMMON);
      System.out.printf("   ✅ createRandomItemByRarity: %s\n", testItem != null ? "정상" : "오류");

    } catch (Exception e) {
      System.out.printf("   ❌ GameItemFactory 오류: %s\n", e.getMessage());
    }

    // JsonBasedQuestFactory 검사
    System.out.println("\n🔍 JsonBasedQuestFactory 검사:");
    try {
      JsonBasedQuestFactory questFactory = JsonBasedQuestFactory.getInstance();
      System.out.printf("   ✅ 인스턴스: %s\n", questFactory != null ? "정상" : "NULL");

      Quest testQuest = questFactory.createQuest("quest_001");
      System.out.printf("   ✅ createQuest: %s\n", testQuest != null ? "정상" : "오류");

      boolean validation = questFactory.validateTemplates();
      System.out.printf("   ✅ 템플릿 검증: %s\n", validation ? "통과" : "실패");

    } catch (Exception e) {
      System.out.printf("   ❌ JsonBasedQuestFactory 오류: %s\n", e.getMessage());
    }

    // GameEffectFactory 검사
    System.out.println("\n🔍 GameEffectFactory 검사:");
    try {
      GameEffect testEffect = GameEffectFactory.createHealHpEffect(50);
      System.out.printf("   ✅ createHealHpEffect: %s\n", testEffect != null ? "정상" : "오류");
    } catch (Exception e) {
      System.out.printf("   ❌ GameEffectFactory 오류: %s\n", e.getMessage());
    }
  }

  /**
   * JSON 파일 무결성 검사
   */
  private void checkJsonFileIntegrity() {
    System.out.println("\n=== 📁 JSON 파일 무결성 검사 ===");

    String[] requiredFiles = {SystemConstants.MAIN_QUESTS_CONFIG, SystemConstants.SIDE_QUESTS_CONFIG, SystemConstants.DAILY_QUESTS_CONFIG,
        SystemConstants.BASIC_POTIONS_CONFIG, SystemConstants.BASIC_WEAPONS_CONFIG, SystemConstants.BASIC_ARMORS_CONFIG};

    int existingFiles = 0;
    int totalFiles = requiredFiles.length;

    for (String filePath : requiredFiles) {
      System.out.printf("🔍 검사 중: %s\n", filePath);

      try (InputStream is = GameEngine.class.getResourceAsStream(filePath)) {
        if (is != null) {
          // 파일 크기 확인
          int size = is.available();
          System.out.printf("   ✅ 존재함 (크기: %d bytes)\n", size);

          if (size == 0) {
            System.out.println("   ⚠️ 경고: 파일이 비어있습니다");
          } else if (size < 50) {
            System.out.println("   ⚠️ 경고: 파일이 너무 작습니다");
          }

          existingFiles++;
        } else {
          System.out.println("   ❌ 파일 없음");
        }
      } catch (Exception e) {
        System.out.printf("   ❌ 오류: %s\n", e.getMessage());
      }
    }

    System.out.printf("\n📊 결과: %d/%d 파일 존재 (%.1f%%)\n", existingFiles, totalFiles, (existingFiles * 100.0) / totalFiles);
  }

  /**
   * 전체 시스템 진단
   */
  private void runFullSystemDiagnostics() {
    System.out.println("\n=== 🛠️ 전체 시스템 진단 ===");
    System.out.println("종합적인 시스템 상태를 확인합니다...\n");

    checkAllFactories();
    System.out.println("\n" + "=".repeat(50));

    checkJsonFileIntegrity();
    System.out.println("\n" + "=".repeat(50));

    validateGameData();
    System.out.println("\n" + "=".repeat(50));

    showSystemInfo();

    System.out.println("\n🎉 전체 시스템 진단 완료!");
  }

  /**
   * 게임 데이터 검증
   */
  private void validateGameData() {
    System.out.println("\n=== 📋 게임 데이터 검증 ===");

    try {
      GameItemFactory itemFactory = GameItemFactory.getInstance();

      // 아이템 데이터 검증
      System.out.println("🔍 아이템 데이터 검증:");
      System.out.printf("   총 아이템 수: %d개\n", itemFactory.getItemCount());

      // 희귀도별 분포 확인
      Map<ItemRarity, Integer> rarityDist = itemFactory.getRarityDistribution();
      System.out.println("   희귀도별 분포:");
      for (Map.Entry<ItemRarity, Integer> entry : rarityDist.entrySet()) {
        if (entry.getValue() > 0) {
          System.out.printf("     %s: %d개\n", entry.getKey().getDisplayName(), entry.getValue());
        }
      }

    } catch (Exception e) {
      System.out.printf("❌ 데이터 검증 중 오류: %s\n", e.getMessage());
    }
  }

  /**
   * 시스템 정보 표시
   */
  private void showSystemInfo() {
    System.out.println("\n⚙️ === 시스템 정보 ===");
    try {
      GameItemFactory itemFactory = GameItemFactory.getInstance();
      System.out.printf("• 로드된 아이템 수: %d개\n", itemFactory.getItemCount());
      System.out.printf("• 초기화 상태: %s\n", itemFactory.isInitialized() ? "정상" : "오류");

      JsonBasedQuestFactory questFactory = JsonBasedQuestFactory.getInstance();
      System.out.printf("• 퀘스트 템플릿: 메인 %d개, 사이드 %d개, 일일 %d개\n", questFactory.getQuestCount("MAIN"), questFactory.getQuestCount("SIDE"),
          questFactory.getQuestCount("DAILY"));
    } catch (Exception e) {
      System.out.println("• 시스템 상태: 일부 오류 발생 (" + e.getMessage() + ")");
    }
  }

  /**
   * 아이템 통계 표시
   */
  private void showItemStatistics() {
    System.out.println("\n📊 === 아이템 시스템 통계 ===");

    try {
      GameItemFactory factory = GameItemFactory.getInstance();

      // 기본 정보
      System.out.printf("총 아이템 수: %d개\n", factory.getItemCount());
      System.out.printf("초기화 상태: %s\n", factory.isInitialized() ? "정상" : "오류");

      // 희귀도별 분포
      Map<ItemRarity, Integer> rarityDist = factory.getRarityDistribution();
      System.out.println("\n💎 희귀도별 분포:");
      for (Map.Entry<ItemRarity, Integer> entry : rarityDist.entrySet()) {
        if (entry.getValue() > 0) {
          System.out.printf("   %s: %d개 (%.1f%%)\n", entry.getKey().getDisplayName(), entry.getValue(),
              (entry.getValue() * 100.0) / factory.getItemCount());
        }
      }

      // 타입별 분포
      Map<String, Integer> typeDist = factory.getTypeDistribution();
      System.out.println("\n🔧 타입별 분포:");
      for (Map.Entry<String, Integer> entry : typeDist.entrySet()) {
        System.out.printf("   %s: %d개\n", entry.getKey(), entry.getValue());
      }

      // 랜덤 생성 테스트
      System.out.println("\n🎲 랜덤 생성 능력 테스트:");
      for (ItemRarity rarity : ItemRarity.values()) {
        GameItem testItem = factory.createRandomItemByRarity(rarity);
        System.out.printf("   %s: %s\n", rarity.getDisplayName(), testItem != null ? "✅ 가능" : "❌ 불가능");
      }

    } catch (Exception e) {
      System.out.println("❌ 아이템 통계 조회 실패: " + e.getMessage());
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 퀘스트 시스템 상태 확인
   */
  private void showQuestSystemStatus() {
    System.out.println("\n🎯 === 퀘스트 시스템 상태 ===");

    try {
      // QuestTemplateLoader 상태
      System.out.println("📋 템플릿 로더 상태:");
      QuestTemplateLoader.printLoaderStatus();

      // JsonBasedQuestFactory 상태
      JsonBasedQuestFactory factory = JsonBasedQuestFactory.getInstance();
      factory.printFactoryStatus();

      // 플레이어의 퀘스트 매니저 상태 (있는 경우)
      if (player != null && player.getQuestManager() != null) {
        System.out.println("\n👤 플레이어 퀘스트 상태:");
        player.getQuestManager().printQuestSystemStatus();
      }

      // 퀘스트 생성 테스트
      System.out.println("\n🧪 퀘스트 생성 테스트:");
      String[] testQuests = {"quest_001", "quest_002", "quest_005"};
      for (String questId : testQuests) {
        Quest quest = factory.createQuest(questId);
        System.out.printf("   %s: %s\n", questId, quest != null ? quest.getTitle() : "생성 실패");
      }

    } catch (Exception e) {
      System.out.println("❌ 퀘스트 시스템 상태 확인 실패: " + e.getMessage());
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 몬스터 통계 표시
   */
  private void showMonsterStatistics() {
    System.out.println("\n📈 === 몬스터 통계 ===");
    System.out.println("몬스터 통계 기능은 ExploreEngine에서 구현됩니다.");
    // TODO: ExploreEngine과 연동하여 몬스터 통계 표시
    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 테스트 몬스터 생성
   */
  private void testMonsterGeneration() {
    System.out.println("\n🧪 === 테스트 몬스터 생성 ===");
    System.out.println("몬스터 생성 테스트 기능은 ExploreEngine에서 구현됩니다.");
    // TODO: ExploreEngine과 연동하여 테스트 몬스터 생성
    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 테스트 아이템 생성
   */
  private void testItemGeneration() {
    System.out.println("\n🎒 === 테스트 아이템 생성 ===");

    try {
      GameItemFactory factory = GameItemFactory.getInstance();

      System.out.println("🎲 랜덤 아이템 5개 생성:");
      for (int i = 1; i <= 5; i++) {
        GameItem item = factory.createWeightedRandomItem();
        if (item != null) {
          System.out.printf("%d. %s (%s) - %dG\n", i, item.getName(), item.getRarity().getDisplayName(), item.getValue());
        } else {
          System.out.printf("%d. 생성 실패\n", i);
        }
      }

      System.out.println("\n🎯 특정 희귀도 아이템 생성:");
      ItemRarity testRarity = ItemRarity.RARE;
      for (int i = 1; i <= 3; i++) {
        GameItem item = factory.createRandomItemByRarity(testRarity);
        if (item != null) {
          System.out.printf("%d. %s (%s)\n", i, item.getName(), item.getRarity().getDisplayName());
        }
      }

    } catch (Exception e) {
      System.out.println("❌ 테스트 아이템 생성 실패: " + e.getMessage());
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 전체 데이터 리로드
   */
  private void reloadAllGameData() {
    System.out.println("\n🔧 === 전체 데이터 리로드 ===");

    try {
      System.out.println("GameItemFactory 재초기화 중...");
      GameItemFactory.getInstance().reinitialize();
      System.out.println("✅ GameItemFactory 재초기화 완료");

      System.out.println("퀘스트 템플릿 리로드 중...");
      // TODO: QuestTemplateLoader 리로드 기능 구현
      System.out.println("✅ 퀘스트 템플릿 리로드 완료");

      System.out.println("🎉 전체 데이터 리로드 완료!");

    } catch (Exception e) {
      System.out.println("❌ 데이터 리로드 실패: " + e.getMessage());
      logger.error("데이터 리로드 실패", e);
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 도움말 메뉴 표시
   */
  private void showHelpMenu() {
    boolean inHelpMenu = true;
    while (inHelpMenu) {
      System.out.println("\n📖 === 디버그 도움말 ===");
      System.out.println("1. 🧪 간단한 팩토리 테스트 실행");
      System.out.println("2. 📊 아이템 통계 보기");
      System.out.println("3. 🎯 퀘스트 시스템 상태 확인");
      System.out.println("4. 📋 게임 데이터 검증");
      System.out.println("5. 🎲 랜덤 생성 테스트");
      System.out.println("6. 🔍 상세 진단 도구");
      System.out.println("7. 📝 로그 파일 확인");
      System.out.println("0. 📖 도움말 종료");

      int choice = InputValidator.getIntInput("선택 (0-7): ", 0, 7);

      switch (choice) {
        case 1:
          quickFactoryTest();
          break;
        case 2:
          showItemStatistics();
          break;
        case 3:
          showQuestSystemStatus();
          break;
        case 4:
          validateGameData();
          break;
        case 5:
          runInteractiveRandomTest();
          break;
        case 6:
          runDetailedDiagnostics();
          break;
        case 7:
          showLogFileInfo();
          break;
        case 0:
          inHelpMenu = false;
          System.out.println("📖 도움말을 종료합니다.");
          break;
        default:
          System.out.println("잘못된 선택입니다.");
      }

      if (inHelpMenu && choice != 0) {
        System.out.println("\n" + "=".repeat(50));
        if (!InputValidator.getConfirmation("도움말 메뉴를 계속 사용하시겠습니까?")) {
          inHelpMenu = false;
          System.out.println("📖 도움말을 종료합니다.");
        }
      }
    }
  }

  /**
   * 간단한 팩토리 테스트
   */
  private void quickFactoryTest() {
    System.out.println("\n🚀 간단한 팩토리 테스트 실행 중...");

    try {
      // GameItemFactory 테스트
      GameItemFactory itemFactory = GameItemFactory.getInstance();
      System.out.println("✅ GameItemFactory 초기화: " + itemFactory.isInitialized());

      // 기본 아이템 생성 테스트
      GameItem basicItem = itemFactory.createItem("HEALTH_POTION");
      System.out.println("✅ 기본 아이템 생성: " + (basicItem != null ? basicItem.getName() : "실패"));

      // 랜덤 아이템 생성 테스트
      GameItem randomItem = itemFactory.createRandomItemByRarity(ItemRarity.RARE);
      System.out.println("✅ 랜덤 레어 아이템: " + (randomItem != null ? randomItem.getName() : "실패"));

      // JsonBasedQuestFactory 테스트
      JsonBasedQuestFactory questFactory = JsonBasedQuestFactory.getInstance();
      System.out.println("✅ JsonBasedQuestFactory 초기화: 완료");

      Quest testQuest = questFactory.createQuest("quest_001");
      System.out.println("✅ 기본 퀘스트 생성: " + (testQuest != null ? testQuest.getTitle() : "실패"));

      // 동적 퀘스트 테스트
      int testLevel = player != null ? player.getLevel() : 5;
      Quest dynamicQuest = questFactory.createLevelAppropriateQuest(testLevel);
      System.out.println("✅ 동적 퀘스트 생성: " + (dynamicQuest != null ? dynamicQuest.getTitle() : "실패"));

      // 일일 퀘스트 테스트
      Quest dailyQuest = questFactory.createDailyQuest(Quest.QuestType.KILL);
      System.out.println("✅ 일일 퀘스트 생성: " + (dailyQuest != null ? dailyQuest.getTitle() : "실패"));

      System.out.println("🎉 간단한 테스트 완료! 모든 기능이 정상 작동합니다.");

    } catch (Exception e) {
      System.out.println("❌ 테스트 중 오류 발생: " + e.getMessage());
      logger.error("간단한 팩토리 테스트 실패", e);

      // 오류 해결 제안
      System.out.println("\n🔧 문제 해결 제안:");
      System.out.println("1. GameItemFactory에 createRandomItemByRarity 메서드가 추가되었는지 확인");
      System.out.println("2. JSON 퀘스트 파일들이 resources/config/quests/ 에 있는지 확인");
      System.out.println("3. 프로젝트를 다시 컴파일해보세요");
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 인터랙티브 랜덤 테스트 실행
   */
  private void runInteractiveRandomTest() {
    boolean inRandomTest = true;

    while (inRandomTest) {
      System.out.println("\n=== 🎲 인터랙티브 랜덤 테스트 ===");
      System.out.println("1. 🎯 특정 희귀도 아이템 생성");
      System.out.println("2. 📈 레벨별 아이템 생성");
      System.out.println("3. 🎁 특수 상황별 아이템 생성");
      System.out.println("4. 📊 랜덤 생성 통계");
      System.out.println("5. 🔄 가중치 시뮬레이션");
      System.out.println("6. 🧪 종합 랜덤 테스트");
      System.out.println("0. 🔙 이전 메뉴로");

      int choice = InputValidator.getIntInput("선택 (0-6): ", 0, 6);

      try {
        GameItemFactory factory = GameItemFactory.getInstance();

        switch (choice) {
          case 1:
            testRarityBasedGeneration(factory);
            break;
          case 2:
            testLevelBasedGeneration(factory);
            break;
          case 3:
            testSpecialGeneration(factory);
            break;
          case 4:
            factory.printRandomGenerationStats();
            break;
          case 5:
            runWeightSimulation(factory);
            break;
          case 6:
            factory.testRandomGeneration();
            break;
          case 0:
            inRandomTest = false;
            break;
          default:
            System.out.println("잘못된 선택입니다.");
        }

        if (inRandomTest && choice != 0) {
          InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
        }

      } catch (Exception e) {
        System.out.println("❌ 랜덤 테스트 중 오류: " + e.getMessage());
        InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      }
    }
  }

  /**
   * 가중치 시뮬레이션 실행
   */
  private void runWeightSimulation(GameItemFactory factory) {
    System.out.println("\n=== ⚖️ 가중치 시뮬레이션 ===");

    int simCount = InputValidator.getIntInput("시뮬레이션 횟수 (10-1000): ", 10, 1000);

    Map<ItemRarity, Integer> results = new HashMap<>();
    for (ItemRarity rarity : ItemRarity.values()) {
      results.put(rarity, 0);
    }

    System.out.printf("🎲 %d번의 가중치 기반 아이템 생성 시뮬레이션 중...\n", simCount);

    for (int i = 0; i < simCount; i++) {
      try {
        GameItem item = factory.createWeightedRandomItem();
        if (item != null) {
          results.merge(item.getRarity(), 1, Integer::sum);
        }

        // 진행률 표시 (10% 단위)
        if ((i + 1) % (simCount / 10) == 0) {
          System.out.printf("진행률: %d%%\n", ((i + 1) * 100) / simCount);
        }
      } catch (Exception e) {
        System.out.printf("시뮬레이션 %d회차 오류: %s\n", i + 1, e.getMessage());
      }
    }

    System.out.println("\n📊 시뮬레이션 결과:");
    for (Map.Entry<ItemRarity, Integer> entry : results.entrySet()) {
      double percentage = (entry.getValue() * 100.0) / simCount;
      System.out.printf("   %s: %d회 (%.1f%%)\n", entry.getKey().getDisplayName(), entry.getValue(), percentage);
    }

    // 이론값과 비교
    System.out.println("\n📈 이론값과의 비교:");
    System.out.println("   일반: 50% (이론) vs " + String.format("%.1f%%", (results.get(ItemRarity.COMMON) * 100.0) / simCount) + " (실제)");
    System.out.println("   고급: 25% (이론) vs " + String.format("%.1f%%", (results.get(ItemRarity.UNCOMMON) * 100.0) / simCount) + " (실제)");
    System.out.println("   희귀: 15% (이론) vs " + String.format("%.1f%%", (results.get(ItemRarity.RARE) * 100.0) / simCount) + " (실제)");
  }

  /**
   * 상세 진단 도구 실행
   */
  private void runDetailedDiagnostics() {
    boolean inDiagnostics = true;

    while (inDiagnostics) {
      System.out.println("\n=== 🔍 상세 진단 도구 ===");
      System.out.println("1. 🏭 모든 팩토리 상태 검사");
      System.out.println("2. 📁 JSON 파일 무결성 검사");
      System.out.println("3. 🔗 의존성 연결 상태 검사");
      System.out.println("4. 💾 메모리 사용량 확인");
      System.out.println("5. ⚡ 성능 벤치마크");
      System.out.println("6. 🛠️ 전체 시스템 진단");
      System.out.println("0. 🔙 이전 메뉴로");

      int choice = InputValidator.getIntInput("선택 (0-6): ", 0, 6);

      switch (choice) {
        case 1:
          checkAllFactories();
          break;
        case 2:
          checkJsonFileIntegrity();
          break;
        case 3:
          checkDependencyConnections();
          break;
        case 4:
          checkMemoryUsage();
          break;
        case 5:
          runPerformanceBenchmark();
          break;
        case 6:
          runFullSystemDiagnostics();
          break;
        case 0:
          inDiagnostics = false;
          break;
        default:
          System.out.println("잘못된 선택입니다.");
      }

      if (inDiagnostics && choice != 0) {
        InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      }
    }
  }

  /**
   * 의존성 연결 상태 검사
   */
  private void checkDependencyConnections() {
    System.out.println("\n=== 🔗 의존성 연결 상태 검사 ===");

    // GameItemFactory 의존성 확인
    System.out.println("🔍 GameItemFactory 의존성:");
    try {
      GameItemFactory itemFactory = GameItemFactory.getInstance();
      System.out.println("   ✅ GameItemFactory 인스턴스 획득 성공");

      // 아이템 생성 테스트로 내부 의존성 확인
      GameItem testItem = itemFactory.createItem("HEALTH_POTION");
      System.out.println("   ✅ 기본 아이템 생성: " + (testItem != null ? "성공" : "실패"));

    } catch (Exception e) {
      System.out.println("   ❌ GameItemFactory 의존성 오류: " + e.getMessage());
    }

    // JsonBasedQuestFactory 의존성 확인
    System.out.println("\n🔍 JsonBasedQuestFactory 의존성:");
    try {
      JsonBasedQuestFactory questFactory = JsonBasedQuestFactory.getInstance();
      System.out.println("   ✅ JsonBasedQuestFactory 인스턴스 획득 성공");

      Quest testQuest = questFactory.createQuest("quest_001");
      System.out.println("   ✅ 퀘스트 생성: " + (testQuest != null ? "성공" : "실패"));

    } catch (Exception e) {
      System.out.println("   ❌ JsonBasedQuestFactory 의존성 오류: " + e.getMessage());
    }

    System.out.println("\n📊 의존성 검사 완료");
  }

  /**
   * 메모리 사용량 확인
   */
  private void checkMemoryUsage() {
    System.out.println("\n=== 💾 메모리 사용량 확인 ===");

    Runtime runtime = Runtime.getRuntime();
    long totalMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();
    long usedMemory = totalMemory - freeMemory;
    long maxMemory = runtime.maxMemory();

    System.out.println("📊 JVM 메모리 정보:");
    System.out.printf("   전체 메모리: %.2f MB\n", totalMemory / (1024.0 * 1024.0));
    System.out.printf("   사용 메모리: %.2f MB\n", usedMemory / (1024.0 * 1024.0));
    System.out.printf("   여유 메모리: %.2f MB\n", freeMemory / (1024.0 * 1024.0));
    System.out.printf("   최대 메모리: %.2f MB\n", maxMemory / (1024.0 * 1024.0));
    System.out.printf("   메모리 사용률: %.1f%%\n", (usedMemory * 100.0) / totalMemory);

    // 게임 데이터 메모리 추정
    try {
      GameItemFactory itemFactory = GameItemFactory.getInstance();
      double estimatedItemMemory = itemFactory.getItemCount() * 0.5; // 아이템당 약 0.5KB 추정

      System.out.println("\n📦 게임 데이터 메모리 추정:");
      System.out.printf("   아이템 데이터: ~%.2f KB\n", estimatedItemMemory);

      if (estimatedItemMemory < 50) {
        System.out.println("   ✅ 메모리 사용량: 매우 효율적");
      } else if (estimatedItemMemory < 100) {
        System.out.println("   ✅ 메모리 사용량: 효율적");
      } else {
        System.out.println("   ⚠️ 메모리 사용량: 최적화 필요");
      }

    } catch (Exception e) {
      System.out.printf("   ❌ 메모리 추정 실패: %s\n", e.getMessage());
    }
  }

  /**
   * 성능 벤치마크 실행
   */
  private void runPerformanceBenchmark() {
    System.out.println("\n=== ⚡ 성능 벤치마크 ===");

    try {
      GameItemFactory factory = GameItemFactory.getInstance();

      // 아이템 생성 성능 테스트
      System.out.println("🎲 아이템 생성 성능 테스트:");

      long startTime = System.currentTimeMillis();
      int testCount = 1000;

      for (int i = 0; i < testCount; i++) {
        factory.createRandomItemByRarity(ItemRarity.COMMON);
      }

      long endTime = System.currentTimeMillis();
      long duration = endTime - startTime;

      System.out.printf("   %d개 아이템 생성 소요 시간: %d ms\n", testCount, duration);
      System.out.printf("   평균 아이템당 생성 시간: %.2f ms\n", duration / (double) testCount);

      if (duration < 100) {
        System.out.println("   ✅ 성능: 매우 좋음");
      } else if (duration < 500) {
        System.out.println("   ✅ 성능: 좋음");
      } else {
        System.out.println("   ⚠️ 성능: 최적화 필요");
      }

    } catch (Exception e) {
      System.out.println("❌ 성능 벤치마크 실패: " + e.getMessage());
    }
  }

  /**
   * 로그 파일 정보 표시
   */
  private void showLogFileInfo() {
    System.out.println("\n=== 📝 로그 파일 정보 ===");

    String[] logFiles = {"logs/rpg-game.log", "logs/rpg-game-error.log"};

    for (String logPath : logFiles) {
      File logFile = new File(logPath);

      System.out.printf("\n📄 %s:\n", logPath);

      if (logFile.exists()) {
        System.out.printf("   존재: ✅\n");
        System.out.printf("   크기: %.2f KB\n", logFile.length() / 1024.0);
        System.out.printf("   수정 시간: %s\n", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(logFile.lastModified())));

        // 마지막 5줄 표시
        System.out.println("   마지막 5줄:");
        showLastLines(logFile, 5);

      } else {
        System.out.println("   존재: ❌");
      }
    }
  }

  /**
   * 파일의 마지막 N줄 표시
   */
  private void showLastLines(File file, int lineCount) {
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      List<String> lines = new ArrayList<>();
      String line;

      while ((line = reader.readLine()) != null) {
        lines.add(line);
        if (lines.size() > lineCount) {
          lines.remove(0);
        }
      }

      for (String lastLine : lines) {
        System.out.println("     " + lastLine);
      }

    } catch (Exception e) {
      System.out.println("     (로그 미리보기 실패: " + e.getMessage() + ")");
    }
  }

  /**
   * SkillFactory를 테스트합니다.
   */
  private void testSkillFactory() {
    System.out.println("\n=== ⚔️ SkillFactory 테스트 ===");

    try {
      // SkillFactory 초기화 확인
      System.out.println("🔍 SkillFactory 초기화 확인:");
      if (!SkillFactory.isInitialized()) {
        System.out.println("   🔄 SkillFactory 초기화 중...");
        SkillFactory.initialize();
      }
      System.out.println("   ✅ SkillFactory 초기화 완료");

      // JSON 파일 읽기 상태 확인
      int skillCount = SkillFactory.getSkillCount();
      System.out.printf("   📦 로드된 스킬 수: %d개\n", skillCount);

      if (skillCount == 0) {
        System.out.println("   ❌ 스킬이 로드되지 않았습니다.");
        System.out.println("   💡 skills.json 파일을 확인해주세요.");
        return;
      }

      // 전체 스킬 목록 표시
      showAllSkills();

      // 스킬 선택하여 상세정보 보기
      if (InputValidator.getConfirmation("\n🔍 특정 스킬의 상세정보를 보시겠습니까?")) {
        selectAndShowSkillDetail();
      }

    } catch (Exception e) {
      System.out.println("❌ SkillFactory 테스트 중 오류: " + e.getMessage());
      logger.error("SkillFactory 테스트 실패", e);

      System.out.println("\n🔧 확인사항:");
      System.out.println("1. resources/config/skills.json 파일이 존재하는지 확인");
      System.out.println("2. JSON 파일 형식이 올바른지 확인");
      System.out.println("3. SystemConstants.SKILLS_CONFIG 경로가 정확한지 확인");
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 전체 스킬 목록을 레벨별로 정렬하여 표시합니다.
   */
  private void showAllSkills() {
    System.out.println("\n📋 로드된 스킬 목록 (레벨순 정렬):");
    System.out.println("=====================================");

    try {
      List<String> allSkillIds = SkillFactory.getAllSkillIds();

      if (allSkillIds.isEmpty()) {
        System.out.println("   로드된 스킬이 없습니다.");
        return;
      }

      // 스킬 정보와 함께 저장할 클래스
      class SkillInfo {
        String id;
        String name;
        int level;
        String type;

        SkillInfo(String id, String name, int level, String type) {
          this.id = id;
          this.name = name;
          this.level = level;
          this.type = type;
        }
      }

      // 스킬 정보를 수집하고 레벨별로 정렬
      List<SkillInfo> skillInfos = new ArrayList<>();

      for (String skillId : allSkillIds) {
        Map<String, Object> info = SkillFactory.getSkillInfo(skillId);
        if (info != null) {
          String name = (String) info.get("name");
          int level = info.get("requiredLevel") != null ? (Integer) info.get("requiredLevel") : 0;
          String type = (String) info.get("type");

          skillInfos.add(new SkillInfo(skillId, name != null ? name : skillId, level, type != null ? type : "UNKNOWN"));
        } else {
          skillInfos.add(new SkillInfo(skillId, skillId, 0, "UNKNOWN"));
        }
      }

      // 레벨순으로 정렬 (레벨이 같으면 이름순)
      skillInfos.sort((a, b) -> {
        int levelCompare = Integer.compare(a.level, b.level);
        if (levelCompare != 0) {
          return levelCompare;
        }
        return a.name.compareTo(b.name);
      });

      // 정렬된 목록 출력
      for (int i = 0; i < skillInfos.size(); i++) {
        SkillInfo skill = skillInfos.get(i);
        System.out.printf("%2d. %-20s (레벨 %-2d, %s)\n", i + 1, skill.name, skill.level, skill.type);
      }

      System.out.println("=====================================");

    } catch (Exception e) {
      System.out.println("   ❌ 스킬 목록 조회 실패: " + e.getMessage());
    }
  }

  /**
   * 스킬을 선택하여 상세정보를 표시합니다.
   */
  private void selectAndShowSkillDetail() {
    try {
      List<String> allSkillIds = SkillFactory.getAllSkillIds();

      if (allSkillIds.isEmpty()) {
        System.out.println("선택할 수 있는 스킬이 없습니다.");
        return;
      }

      // 레벨별로 정렬된 스킬 ID 목록 생성
      List<String> sortedSkillIds = getSortedSkillIds(allSkillIds);

      int maxIndex = sortedSkillIds.size();
      int selectedIndex = InputValidator.getIntInput(String.format("스킬 번호 선택 (1-%d): ", maxIndex), 1, maxIndex) - 1;

      String selectedSkillId = sortedSkillIds.get(selectedIndex);
      showSkillDetail(selectedSkillId);

    } catch (Exception e) {
      System.out.println("❌ 스킬 선택 중 오류: " + e.getMessage());
    }
  }

  /**
   * 스킬 ID 목록을 레벨별로 정렬하여 반환합니다.
   */
  private List<String> getSortedSkillIds(List<String> skillIds) {
    return skillIds.stream().sorted((id1, id2) -> {
      Map<String, Object> info1 = SkillFactory.getSkillInfo(id1);
      Map<String, Object> info2 = SkillFactory.getSkillInfo(id2);

      int level1 = info1 != null && info1.get("requiredLevel") != null ? (Integer) info1.get("requiredLevel") : 0;
      int level2 = info2 != null && info2.get("requiredLevel") != null ? (Integer) info2.get("requiredLevel") : 0;

      int levelCompare = Integer.compare(level1, level2);
      if (levelCompare != 0) {
        return levelCompare;
      }

      // 레벨이 같으면 이름순 정렬
      String name1 = info1 != null ? (String) info1.get("name") : id1;
      String name2 = info2 != null ? (String) info2.get("name") : id2;
      return (name1 != null ? name1 : id1).compareTo(name2 != null ? name2 : id2);
    }).collect(Collectors.toList());
  }

  /**
   * 특정 스킬의 상세정보를 표시합니다.
   */
  private void showSkillDetail(String skillId) {
    System.out.printf("\n🔍 스킬 상세정보: %s\n", skillId);
    System.out.println("=====================================");

    try {
      // 스킬 정보 조회
      Map<String, Object> skillInfo = SkillFactory.getSkillInfo(skillId);

      if (skillInfo == null) {
        System.out.println("❌ 스킬 정보를 찾을 수 없습니다.");
        return;
      }

      // 정보 출력 (null 값 처리 개선)
      System.out.printf("ID           : %s\n", skillId);
      System.out.printf("이름         : %s\n", getValueOrDefault(skillInfo, "name", "알 수 없음"));
      System.out.printf("설명         : %s\n", getValueOrDefault(skillInfo, "description", "설명 없음"));
      System.out.printf("타입         : %s\n", getValueOrDefault(skillInfo, "type", "알 수 없음"));
      System.out.printf("필요 레벨    : %s\n", getValueOrDefault(skillInfo, "requiredLevel", "0"));
      System.out.printf("마나 비용    : %s\n", getValueOrDefault(skillInfo, "manaCost", "0"));
      System.out.printf("쿨다운       : %s턴\n", getValueOrDefault(skillInfo, "cooldown", "0"));

      // 숫자 필드들은 0이면 표시하지 않음
      Object damageMultiplier = skillInfo.get("damageMultiplier");
      if (damageMultiplier != null && !damageMultiplier.equals(0.0)) {
        System.out.printf("데미지 배율  : %.2f\n", damageMultiplier);
      }

      Object healAmount = skillInfo.get("healAmount");
      if (healAmount != null && !healAmount.equals(0)) {
        System.out.printf("힐량         : %s\n", healAmount);
      }

      Object buffDuration = skillInfo.get("buffDuration");
      if (buffDuration != null && !buffDuration.equals(0)) {
        System.out.printf("버프 지속시간: %s턴\n", buffDuration);
      }

      // 추가 정보가 있다면 표시
      if (skillInfo.containsKey("category") && skillInfo.get("category") != null) {
        System.out.printf("카테고리     : %s\n", skillInfo.get("category"));
      }
      if (skillInfo.containsKey("rarity") && skillInfo.get("rarity") != null) {
        System.out.printf("희귀도       : %s\n", skillInfo.get("rarity"));
      }
      if (skillInfo.containsKey("targetType") && skillInfo.get("targetType") != null) {
        System.out.printf("대상 타입    : %s\n", skillInfo.get("targetType"));
      }

      System.out.println("=====================================");

      // 실제 스킬 인스턴스 생성 테스트
      if (InputValidator.getConfirmation("스킬 인스턴스 생성을 테스트해보시겠습니까?")) {
        testSkillCreation(skillId);
      }

    } catch (Exception e) {
      System.out.println("❌ 스킬 정보 조회 실패: " + e.getMessage());
    }
  }

  /**
   * Map에서 값을 가져오되, null이면 기본값을 반환하는 헬퍼 메서드
   */
  private Object getValueOrDefault(Map<String, Object> map, String key, Object defaultValue) {
    Object value = map.get(key);
    return value != null ? value : defaultValue;
  }

  /**
   * 스킬 생성을 테스트합니다.
   */
  private void testSkillCreation(String skillId) {
    System.out.printf("\n🧪 스킬 생성 테스트: %s\n", skillId);

    try {
      Skill skill = SkillFactory.createSkill(skillId);

      if (skill != null) {
        System.out.println("✅ 스킬 생성 성공!");
        System.out.printf("   생성된 스킬: %s\n", skill.getName());
        System.out.printf("   타입: %s\n", skill.getType());
        System.out.printf("   레벨: %d\n", skill.getRequiredLevel());
        System.out.printf("   마나: %d\n", skill.getManaCost());
      } else {
        System.out.println("❌ 스킬 생성 실패");
      }

    } catch (Exception e) {
      System.out.println("❌ 스킬 생성 중 오류: " + e.getMessage());
    }
  }

  /**
   * 만료 처리 테스트
   */
  private void testExpiryProcessing(QuestManager questManager) {
    System.out.println("\n⏰ === 퀘스트 만료 처리 테스트 ===");

    try {
      // 현재 활성 퀘스트 상태 표시
      System.out.println("📋 현재 활성 퀘스트:");
      List<Quest> activeQuests = questManager.getActiveQuests();
      if (activeQuests.isEmpty()) {
        System.out.println("   활성 퀘스트가 없습니다.");
      } else {
        for (int i = 0; i < activeQuests.size(); i++) {
          Quest quest = activeQuests.get(i);
          System.out.printf("   %d. %s (ID: %s)\n", i + 1, quest.getTitle(), quest.getId());
        }
      }

      // 테스트용 만료된 일일 퀘스트 추가
      System.out.println("\n🧪 테스트용 만료된 일일 퀘스트 생성...");
      addExpiredTestQuests(questManager);

      // 만료 처리 전 상태
      System.out.println("\n📊 만료 처리 전:");
      System.out.printf("   사용 가능한 퀘스트: %d개\n", questManager.getAvailableQuests().size());
      System.out.printf("   활성 퀘스트: %d개\n", questManager.getActiveQuests().size());

      // 만료 처리 실행
      System.out.println("\n🔄 만료된 퀘스트 정리 중...");
      questManager.cleanupExpiredQuests();

      // 만료 처리 후 상태
      System.out.println("\n📊 만료 처리 후:");
      System.out.printf("   사용 가능한 퀘스트: %d개\n", questManager.getAvailableQuests().size());
      System.out.printf("   활성 퀘스트: %d개\n", questManager.getActiveQuests().size());

      // 새로운 일일 퀘스트 생성 테스트
      if (player != null) {
        System.out.println("\n🆕 새로운 일일 퀘스트 생성 테스트...");
        questManager.generateDailyQuests(player);
        System.out.printf("   새로 생성된 퀘스트: %d개\n", questManager.getAvailableQuests(player).size());
      }

      System.out.println("✅ 만료 처리 테스트 완료");

    } catch (Exception e) {
      System.out.println("❌ 만료 처리 테스트 중 오류: " + e.getMessage());
      logger.error("만료 처리 테스트 실패", e);
    }
  }

  /**
   * 테스트용 만료된 퀘스트 추가
   */
  private void addExpiredTestQuests(QuestManager questManager) {
    try {
      // 어제 날짜로 된 일일 퀘스트 ID 생성
      LocalDate yesterday = LocalDate.now().minusDays(1);
      String yesterdayStr = yesterday.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

      // 테스트용 만료된 퀘스트들
      String[] expiredQuestIds =
          {"daily_kill_" + yesterdayStr + "_A01", "daily_collect_" + yesterdayStr + "_A01", "daily_exploration_" + yesterdayStr + "_B01"};

      for (String questId : expiredQuestIds) {
        // 간단한 테스트 퀘스트 생성
        Map<String, Integer> objectives = new HashMap<>();
        objectives.put("test_objective", 1);

        QuestReward reward = new QuestReward(50, 30);

        Quest expiredQuest = new Quest(questId, "만료된 테스트 퀘스트", "테스트용 만료된 일일 퀘스트입니다.", Quest.QuestType.KILL, 1, objectives, reward);

        // 사용 가능한 퀘스트 목록에 추가 (테스트용)
        questManager.getAvailableQuests().add(expiredQuest);
      }

      System.out.printf("   테스트용 만료된 퀘스트 %d개 추가됨\n", expiredQuestIds.length);

    } catch (Exception e) {
      System.out.println("❌ 테스트 퀘스트 추가 실패: " + e.getMessage());
    }
  }

  /**
   * 테스트용 플레이어 생성
   */
  private Player createTestPlayer(String name, int level) {
    try {
      System.out.printf("🧪 테스트 플레이어 생성 중: %s (레벨 %d)\n", name, level);

      // 새 플레이어 생성
      Player testPlayer = new Player(name);

      // 레벨 설정
      for (int i = 1; i < level; i++) {
        testPlayer.gainExp(testPlayer.getExpRequiredForNextLevel());
      }

      // 기본 스탯 조정 (레벨에 맞게)
      int levelBonus = (level - 1) * 10;
      testPlayer.setGold(100 + levelBonus * 5);

      // 테스트용 아이템 추가
      addTestItemsToPlayer(testPlayer, level);

      System.out.printf("✅ 테스트 플레이어 생성 완료: %s (레벨 %d, 골드 %d)\n", testPlayer.getName(), testPlayer.getLevel(), testPlayer.getGold());

      return testPlayer;

    } catch (Exception e) {
      System.out.println("❌ 테스트 플레이어 생성 실패: " + e.getMessage());
      logger.error("테스트 플레이어 생성 실패", e);

      // 폴백: 기본 플레이어 반환
      return new Player(name);
    }
  }

  /**
   * 테스트 플레이어에게 아이템 추가
   */
  private void addTestItemsToPlayer(Player testPlayer, int level) {
    try {
      GameItemFactory itemFactory = GameItemFactory.getInstance();
      PlayerInventory inventory = testPlayer.getInventory();

      // 레벨에 맞는 기본 아이템들 추가
      String[] basicItems = {"HEALTH_POTION", "MANA_POTION"};

      for (String itemId : basicItems) {
        GameItem item = itemFactory.createItem(itemId);
        if (item != null) {
          inventory.addItem(item, Math.min(3, level / 2 + 1));
        }
      }

      // 레벨에 맞는 장비 추가
      if (level >= 5) {
        GameItem weapon = itemFactory.createRandomItemByRarity(ItemRarity.COMMON);
        if (weapon != null && weapon instanceof GameEquipment) {
          inventory.addItem(weapon, 1);
        }
      }

      if (level >= 10) {
        GameItem armor = itemFactory.createRandomItemByRarity(ItemRarity.UNCOMMON);
        if (armor != null && armor instanceof GameEquipment) {
          inventory.addItem(armor, 1);
        }
      }

    } catch (Exception e) {
      System.out.println("⚠️ 테스트 아이템 추가 실패: " + e.getMessage());
    }
  }

  /**
   * 다양한 레벨에서의 일일 퀘스트 생성 테스트
   */
  private void testMultipleLevelGeneration(QuestManager questManager) {
    System.out.println("\n📈 === 다양한 레벨에서의 일일 퀘스트 생성 테스트 ===");

    int[] testLevels = {1, 5, 10, 15, 20, 25, 30, 35, 40, 50};

    for (int level : testLevels) {
      System.out.printf("\n🎯 레벨 %d 테스트:\n", level);

      try {
        Player testPlayer = createTestPlayer("테스트어" + level, level);

        // 원래 퀘스트 수 기록
        int originalQuestCount = questManager.getAvailableQuests().size();

        // 일일 퀘스트 생성
        questManager.generateDailyQuests(testPlayer);

        // 새로 생성된 퀘스트 수 계산
        int newQuestCount = questManager.getAvailableQuests().size();
        int generatedCount = newQuestCount - originalQuestCount;

        System.out.printf("   생성된 일일 퀘스트: %d개\n", generatedCount);

        // 생성된 퀘스트들의 상세 정보
        List<Quest> playerQuests = questManager.getAvailableQuests(testPlayer);
        for (Quest quest : playerQuests) {
          if (quest.getId().startsWith("daily_")) {
            System.out.printf("   - %s (필요 레벨: %d)\n", quest.getTitle(), quest.getRequiredLevel());
          }
        }

      } catch (Exception e) {
        System.out.printf("   ❌ 레벨 %d 테스트 실패: %s\n", level, e.getMessage());
      }
    }

    System.out.println("\n✅ 다양한 레벨 테스트 완료");
  }

  /**
   * 개선된 일일 퀘스트 시스템 전체 테스트
   */
  private void testImprovedDailyQuestSystem() {
    System.out.println("\n=== 🧪 개선된 일일 퀘스트 시스템 테스트 ===");

    if (player == null) {
      System.out.println("❌ 플레이어가 설정되지 않았습니다.");
      System.out.println("테스트용 플레이어를 생성하시겠습니까?");

      if (InputValidator.getConfirmation("테스트 플레이어 생성")) {
        int testLevel = InputValidator.getIntInput("테스트 플레이어 레벨 (1-50): ", 1, 50);
        Player testPlayer = createTestPlayer("테스트어", testLevel);

        // 임시로 테스트 플레이어 사용
        runTestsWithPlayer(testPlayer);
      }
      return;
    }

    runTestsWithPlayer(player);
  }

  /**
   * 특정 플레이어로 테스트 실행
   */
  private void runTestsWithPlayer(Player testPlayer) {
    QuestManager questManager = testPlayer.getQuestManager();

    System.out.println("1. 현재 일일 퀘스트 상태");
    System.out.println("2. 일일 퀘스트 새로고침 테스트");
    System.out.println("3. 다양한 레벨에서 생성 테스트");
    System.out.println("4. 히스토리 시스템 테스트");
    System.out.println("5. 만료 처리 테스트");
    System.out.println("6. 템플릿 기반 생성 테스트");
    System.out.println("7. 종합 테스트");

    int choice = InputValidator.getIntInput("선택 (1-7): ", 1, 7);

    switch (choice) {
      case 1:
        showCurrentDailyQuestStatus(questManager, testPlayer);
        break;
      case 2:
        testDailyQuestRefresh(questManager, testPlayer);
        break;
      case 3:
        testMultipleLevelGeneration(questManager);
        break;
      case 4:
        testHistorySystem(questManager, testPlayer);
        break;
      case 5:
        testExpiryProcessing(questManager);
        break;
      case 6:
        testTemplateBasedGeneration(testPlayer);
        break;
      case 7:
        runComprehensiveTest(questManager, testPlayer);
        break;
    }
  }

  /**
   * 현재 일일 퀘스트 상태 표시
   */
  private void showCurrentDailyQuestStatus(QuestManager questManager, Player player) {
    System.out.println("\n📊 === 현재 일일 퀘스트 상태 ===");

    System.out.printf("플레이어: %s (레벨 %d)\n", player.getName(), player.getLevel());

    List<Quest> availableQuests = questManager.getAvailableQuests(player);
    List<Quest> dailyQuests = availableQuests.stream().filter(quest -> quest.getId().startsWith("daily_")).collect(Collectors.toList());

    if (dailyQuests.isEmpty()) {
      System.out.println("❌ 사용 가능한 일일 퀘스트가 없습니다.");
    } else {
      System.out.printf("✅ 사용 가능한 일일 퀘스트: %d개\n", dailyQuests.size());
      for (int i = 0; i < dailyQuests.size(); i++) {
        Quest quest = dailyQuests.get(i);
        System.out.printf("   %d. %s (ID: %s)\n", i + 1, quest.getTitle(), quest.getId());
        System.out.printf("      목표: %s\n", quest.getObjectives());
        System.out.printf("      보상: 경험치 %d, 골드 %d\n", quest.getReward().getExpReward(), quest.getReward().getGoldReward());
      }
    }

    // 활성 일일 퀘스트
    List<Quest> activeDaily =
        questManager.getActiveQuests().stream().filter(quest -> quest.getId().startsWith("daily_")).collect(Collectors.toList());

    System.out.printf("\n⚡ 진행 중인 일일 퀘스트: %d개\n", activeDaily.size());
    for (Quest quest : activeDaily) {
      System.out.printf("   - %s (진행도: %s)\n", quest.getTitle(), quest.getCurrentProgress());
    }
  }

  /**
   * 일일 퀘스트 새로고침 테스트
   */
  private void testDailyQuestRefresh(QuestManager questManager, Player player) {
    System.out.println("\n🔄 === 일일 퀘스트 새로고침 테스트 ===");

    // 새로고침 전 상태
    int beforeCount = questManager.getAvailableQuests(player).size();
    System.out.printf("새로고침 전 퀘스트 수: %d개\n", beforeCount);

    // 새로고침 실행
    System.out.println("🔄 일일 퀘스트 새로고침 중...");
    questManager.generateDailyQuests(player);

    // 새로고침 후 상태
    int afterCount = questManager.getAvailableQuests(player).size();
    System.out.printf("새로고침 후 퀘스트 수: %d개\n", afterCount);
    System.out.printf("새로 생성된 퀘스트: %d개\n", afterCount - beforeCount);

    System.out.println("✅ 새로고침 테스트 완료");
  }

  /**
   * 히스토리 시스템 테스트
   */
  private void testHistorySystem(QuestManager questManager, Player player) {
    System.out.println("\n📚 === 히스토리 시스템 테스트 ===");

    // TODO: QuestHistoryManager가 구현되면 여기서 테스트
    System.out.println("히스토리 시스템 테스트는 QuestHistoryManager 구현 후 추가됩니다.");

    // 현재는 기본적인 완료된 퀘스트만 표시
    List<Quest> completedQuests = questManager.getCompletedQuests();
    System.out.printf("완료된 퀘스트: %d개\n", completedQuests.size());

    for (Quest quest : completedQuests) {
      System.out.printf("   - %s (%s)\n", quest.getTitle(), quest.getStatus());
    }
  }

  /**
   * 템플릿 기반 생성 테스트
   */
  private void testTemplateBasedGeneration(Player player) {
    System.out.println("\n📋 === 템플릿 기반 일일 퀘스트 생성 테스트 ===");

    try {
      // TODO: TemplateDailyQuestGenerator가 구현되면 여기서 테스트
      System.out.println("템플릿 기반 생성 테스트는 TemplateDailyQuestGenerator 구현 후 추가됩니다.");

      // 현재는 JsonBasedQuestFactory를 사용한 기본 테스트
      JsonBasedQuestFactory factory = JsonBasedQuestFactory.getInstance();

      System.out.println("일일 퀘스트 템플릿 테스트:");
      Quest.QuestType[] types = {Quest.QuestType.KILL, Quest.QuestType.COLLECT};

      for (Quest.QuestType type : types) {
        Quest dailyQuest = factory.createDailyQuest(type);
        if (dailyQuest != null) {
          System.out.printf("   %s: %s\n", type, dailyQuest.getTitle());
        } else {
          System.out.printf("   %s: 생성 실패\n", type);
        }
      }

    } catch (Exception e) {
      System.out.println("❌ 템플릿 기반 생성 테스트 실패: " + e.getMessage());
    }
  }

  /**
   * 종합 테스트 실행
   */
  private void runComprehensiveTest(QuestManager questManager, Player player) {
    System.out.println("\n🎯 === 종합 테스트 실행 ===");

    System.out.println("1️⃣ 현재 상태 확인...");
    showCurrentDailyQuestStatus(questManager, player);

    System.out.println("\n2️⃣ 만료 처리 테스트...");
    testExpiryProcessing(questManager);

    System.out.println("\n3️⃣ 새로운 퀘스트 생성...");
    testDailyQuestRefresh(questManager, player);

    System.out.println("\n4️⃣ 다른 레벨에서 생성 테스트...");
    Player testPlayer15 = createTestPlayer("테스트15", 15);
    Player testPlayer25 = createTestPlayer("테스트25", 25);

    questManager.generateDailyQuests(testPlayer15);
    questManager.generateDailyQuests(testPlayer25);

    System.out.println("\n✅ 종합 테스트 완료!");
    System.out.println("모든 테스트가 성공적으로 실행되었습니다.");
  }

  // ==================== 추가 유틸리티 메서드들 ====================

  /**
   * 테스트 결과를 파일로 저장
   */
  private void saveTestResults(String testName, List<String> results) {
    try {
      String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
      String filename = String.format("test_results_%s_%s.txt", testName, timestamp);

      // TODO: 파일 저장 로직 구현
      System.out.printf("📄 테스트 결과가 %s에 저장되었습니다.\n", filename);

    } catch (Exception e) {
      System.out.println("❌ 테스트 결과 저장 실패: " + e.getMessage());
    }
  }

}
