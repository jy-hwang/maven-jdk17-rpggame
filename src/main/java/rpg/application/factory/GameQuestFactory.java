// GameQuestFactory.java - 퀘스트 생성을 담당하는 Factory 패턴
package rpg.application.factory;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.domain.item.GameItem;
import rpg.domain.item.ItemRarity;
import rpg.domain.quest.Quest;
import rpg.domain.quest.QuestReward;

/**
 * 퀘스트를 생성하는 Factory 클래스
 * - 다양한 타입의 퀘스트를 표준화된 방식으로 생성
 * - 퀘스트 템플릿 기반 생성 지원
 * - 동적 퀘스트 생성 기능
 */
public class GameQuestFactory {
  private static final Logger logger = LoggerFactory.getLogger(GameQuestFactory.class);
  private static GameQuestFactory instance;

  private final GameItemFactory itemFactory;
  private final GameEffectFactory effectFactory;

  private GameQuestFactory() {
    this.itemFactory = GameItemFactory.getInstance();
    this.effectFactory = new GameEffectFactory();
  }

  public static GameQuestFactory getInstance() {
    if (instance == null) {
      instance = new GameQuestFactory();
    }
    return instance;
  }


  /**
   * 플레이어 레벨에 맞는 동적 퀘스트 생성
   */
  public Quest createLevelAppropriateQuest(int playerLevel) {
    // 레벨에 따른 퀘스트 타입 결정
    Quest.QuestType[] types = Quest.QuestType.values();
    Quest.QuestType selectedType = types[(int) (Math.random() * types.length)];

    return createDynamicQuest(selectedType, playerLevel);
  }

  /**
   * 일일 퀘스트 생성
   */
  public Quest createDailyQuest(Quest.QuestType type) {
    String dailyId = "daily_" + type.name().toLowerCase() + "_" + System.currentTimeMillis();

    switch (type) {
      case KILL:
        return createDailyKillQuest(dailyId);
      case COLLECT:
        return createDailyCollectionQuest(dailyId);
      default:
        logger.warn("지원하지 않는 일일 퀘스트 타입: {}", type);
        return null;
    }
  }


  /**
   * 동적 퀘스트 생성 (레벨에 맞춰 스케일링)
   */
  private Quest createDynamicQuest(Quest.QuestType type, int playerLevel) {
    String questId = "dynamic_" + type.name().toLowerCase() + "_" + playerLevel;
    String title;
    String description;
    Map<String, Integer> objectives = new HashMap<>();
    QuestReward reward;

    // 레벨 기반 난이도 조정
    int difficulty = Math.max(1, playerLevel / 3);
    int baseExp = 50 * playerLevel;
    int baseGold = 30 * playerLevel;

    switch (type) {
      case KILL:
        title = "레벨 " + playerLevel + " 사냥 임무";
        description = "강해진 당신에게 어울리는 사냥 임무입니다.";
        objectives.put("kill_random_monster", difficulty + 2);
        reward = new QuestReward(baseExp, baseGold);
        break;

      case COLLECT:
        title = "레벨 " + playerLevel + " 수집 임무";
        description = "귀중한 아이템들을 수집해주세요.";
        objectives.put("collect_random_item", difficulty + 1);

        // 수집 퀘스트는 더 좋은 보상
        GameItem rewardItem = itemFactory.createRandomItemByRarity(playerLevel > 10 ? ItemRarity.RARE : ItemRarity.UNCOMMON);
        reward = new QuestReward(baseExp, baseGold, rewardItem, 1);
        break;

      case LEVEL:
        title = "성장의 길";
        description = "더 높은 레벨에 도달하세요.";
        objectives.put("reach_level", playerLevel + 1);
        reward = new QuestReward(baseExp * 2, baseGold);
        break;

      default:
        return null;
    }

    return new Quest(questId, title, description, type, playerLevel, objectives, reward);
  }


  // 일일 퀘스트 구체 생성 메서드들
  private Quest createDailyKillQuest(String questId) {
    String[] monsters = {"슬라임", "고블린", "오크", "스켈레톤"};
    String targetMonster = monsters[(int) (Math.random() * monsters.length)];

    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("kill_" + targetMonster, 8 + (int) (Math.random() * 5));

    QuestReward reward = new QuestReward(80, 120);

    return new Quest(questId, "일일 " + targetMonster + " 사냥", "오늘의 목표인 " + targetMonster + "을(를) 처치하세요.", Quest.QuestType.KILL, 1, objectives, reward);
  }

  private Quest createDailyCollectionQuest(String questId) {
    String[] items = {"체력 물약", "마나 물약", "철광석", "허브"};
    String targetItem = items[(int) (Math.random() * items.length)];

    Map<String, Integer> objectives = new HashMap<>();
    objectives.put("collect_" + targetItem, 3 + (int) (Math.random() * 3));

    QuestReward reward = new QuestReward(60, 80);

    return new Quest(questId, "일일 " + targetItem + " 수집", "오늘의 목표인 " + targetItem + "을(를) 수집하세요.", Quest.QuestType.COLLECT, 1, objectives, reward);
  }

  /**
   * 팩토리 상태 정보 출력
   */
  public void printFactoryStatus() {
    System.out.println("\n=== 🏭 GameQuestFactory 상태 ===");
    System.out.println("📋 기능: 동적 퀘스트 생성 전용");
    System.out.println("   - 일일 킬 퀘스트 생성");
    System.out.println("   - 일일 수집 퀘스트 생성");
    System.out.println("   - 랜덤 퀘스트 생성");

    System.out.println("\n🔧 연동된 팩토리:");
    System.out.printf("   GameItemFactory: %s%n", itemFactory != null ? "✅ 연결됨" : "❌ 연결안됨");
    System.out.printf("   GameEffectFactory: %s%n", effectFactory != null ? "✅ 연결됨" : "❌ 연결안됨");

    System.out.println("\n💡 참고:");
    System.out.println("   - 퀘스트 템플릿은 JsonBasedQuestFactory 사용");
    System.out.println("   - 이 팩토리는 동적 생성만 담당");
    System.out.println("===================================");
  }
}
