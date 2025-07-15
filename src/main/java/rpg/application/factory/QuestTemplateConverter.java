package rpg.application.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.application.manager.LocationManager;
import rpg.application.service.DynamicQuestDataProvider;
import rpg.domain.item.GameItem;
import rpg.domain.item.GameItemData;
import rpg.domain.location.LocationData;
import rpg.domain.monster.MonsterData;
import rpg.domain.quest.Quest;
import rpg.domain.quest.QuestItemReward;
import rpg.domain.quest.QuestReward;
import rpg.domain.quest.QuestRewardData;
import rpg.domain.quest.QuestTemplateData;
import rpg.domain.quest.VariableQuantity;
import rpg.infrastructure.data.loader.ConfigDataLoader;
import rpg.infrastructure.data.loader.MonsterDataLoader;

public class QuestTemplateConverter {
  private static final Logger logger = LoggerFactory.getLogger(QuestTemplateConverter.class);
  private static final Random random = new Random();

  private final GameItemFactory itemFactory;
  private final DynamicQuestDataProvider dataProvider;

  public QuestTemplateConverter(GameItemFactory itemFactory) {
    this.itemFactory = itemFactory;
    this.dataProvider = DynamicQuestDataProvider.getInstance();
  }

  /**
   * 템플릿 데이터를 실제 Quest 객체로 변환
   */
  public Quest convertToQuest(QuestTemplateData template) {
    try {
      // 퀘스트 타입 변환
      Quest.QuestType questType = parseQuestType(template.getType());

      // 목표 설정 (가변 타겟 처리)
      Map<String, Integer> objectives = processObjectives(template);

      // 보상 생성
      QuestReward reward = createReward(template.getReward());

      // 퀘스트 생성
      Quest quest = new Quest(template.getId(), template.getTitle(), template.getDescription(), questType, template.getRequiredLevel(), objectives, reward);

      logger.debug("퀘스트 변환 완료: {} - {}", template.getId(), template.getTitle());
      return quest;

    } catch (Exception e) {
      logger.error("퀘스트 변환 실패: {}", template.getId(), e);
      return null;
    }
  }

  /**
   * 일일/주간 퀘스트의 가변 목표 처리
   */
  public Quest convertToDynamicQuest(QuestTemplateData template) {
    try {
      // 기본 변환
      Quest quest = convertToQuest(template);
      if (quest == null) {
        return null;
      }

      // 일일/주간 퀘스트인 경우 동적 ID 생성
      if ("DAILY".equals(template.getCategory()) || "WEEKLY".equals(template.getCategory())) {
        String dynamicId = generateDynamicId(template);

        // 새로운 퀘스트 생성 (동적 ID와 목표로)
        Map<String, Integer> dynamicObjectives = generateDynamicObjectives(template);
        String dynamicTitle = generateDynamicTitle(template, dynamicObjectives);
        String dynamicDescription = generateDynamicDescription(template, dynamicObjectives);

        quest = new Quest(dynamicId, dynamicTitle, dynamicDescription, quest.getType(), quest.getRequiredLevel(), dynamicObjectives, quest.getReward());

        logger.debug("동적 퀘스트 생성: {} - {}", dynamicId, dynamicTitle);
      }

      return quest;

    } catch (Exception e) {
      logger.error("동적 퀘스트 변환 실패: {}", template.getId(), e);
      return null;
    }
  }

  /**
   * 퀘스트 타입 문자열을 enum으로 변환
   */
  private Quest.QuestType parseQuestType(String typeString) {
    try {
      return Quest.QuestType.valueOf(typeString.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("알 수 없는 퀘스트 타입: {}, KILL로 기본 설정", typeString);
      return Quest.QuestType.KILL;
    }
  }

  /**
   * 목표 처리 (기본)
   */
  private Map<String, Integer> processObjectives(QuestTemplateData template) {
    return new HashMap<>(template.getObjectives());
  }

  /**
   * 랜덤 수량 생성
   */
  private int getRandomQuantity(VariableQuantity variableQuantity) {
    if (variableQuantity == null) {
      return 1;
    }

    int min = variableQuantity.getMin();
    int max = variableQuantity.getMax();

    if (min >= max) {
      return min;
    }

    return min + random.nextInt(max - min + 1);
  }

  /**
   * 동적 퀘스트 ID 생성
   */
  private String generateDynamicId(QuestTemplateData template) {
    String category = template.getCategory().toLowerCase();
    String type = template.getType().toLowerCase();
    long timestamp = System.currentTimeMillis();

    return category + "_" + type + "_" + timestamp;
  }

  /**
   * 동적 제목 생성
   */
  private String generateDynamicTitle(QuestTemplateData template, Map<String, Integer> objectives) {
    if (objectives.isEmpty()) {
      return template.getTitle();
    }

    // 첫 번째 목표를 기준으로 제목 생성
    Map.Entry<String, Integer> firstObjective = objectives.entrySet().iterator().next();
    String objectiveKey = firstObjective.getKey();
    int quantity = firstObjective.getValue();

    // 목표에서 타겟 추출
    String target = extractTargetFromObjective(objectiveKey);

    String category = template.getCategory();
    if ("DAILY".equals(category)) {
      return "일일 " + target + " " + getActionName(template.getType()) + " (" + quantity + "개)";
    } else if ("WEEKLY".equals(category)) {
      return "주간 " + target + " " + getActionName(template.getType()) + " (" + quantity + "개)";
    }

    return template.getTitle();
  }

  /**
   * 동적 설명 생성
   */
  private String generateDynamicDescription(QuestTemplateData template, Map<String, Integer> objectives) {
    if (objectives.isEmpty()) {
      return template.getDescription();
    }

    Map.Entry<String, Integer> firstObjective = objectives.entrySet().iterator().next();
    String objectiveKey = firstObjective.getKey();
    int quantity = firstObjective.getValue();

    String target = extractTargetFromObjective(objectiveKey);
    String action = getActionDescription(template.getType());

    return target + "을(를) " + quantity + "개 " + action + "하세요.";
  }

  /**
   * 목표 키에서 타겟 추출
   */
  private String extractTargetFromObjective(String objectiveKey) {
    String[] parts = objectiveKey.split("_", 2);
    if (parts.length >= 2) {
      return parts[1];
    }
    return objectiveKey;
  }

  /**
   * 퀘스트 타입별 액션 이름
   */
  private String getActionName(String questType) {
    switch (questType.toUpperCase()) {
      case "KILL":
        return "사냥";
      case "COLLECT":
        return "수집";
      case "EXPLORE":
        return "탐험";
      default:
        return "완료";
    }
  }

  /**
   * 퀘스트 타입별 액션 설명
   */
  private String getActionDescription(String questType) {
    switch (questType.toUpperCase()) {
      case "KILL":
        return "처치";
      case "COLLECT":
        return "수집";
      case "EXPLORE":
        return "탐험";
      default:
        return "완료";
    }
  }

  /**
   * 템플릿 보상 데이터를 실제 QuestReward로 변환
   */
  private QuestReward createReward(QuestRewardData rewardData) {
    QuestReward reward = new QuestReward(rewardData.getExperience(), rewardData.getGold());

    // 아이템 보상 추가
    if (rewardData.getItems() != null && !rewardData.getItems().isEmpty()) {
      for (QuestItemReward itemReward : rewardData.getItems()) {
        GameItem item = itemFactory.createItem(itemReward.getItemId());
        if (item != null) {
          reward.addItemReward(item, itemReward.getQuantity());
          logger.debug("퀘스트 보상 아이템 추가: {} x{}", item.getName(), itemReward.getQuantity());
        } else {
          logger.warn("퀘스트 보상 아이템 생성 실패: {}", itemReward.getItemId());
          // 보상 실패시 골드로 보상
          reward = new QuestReward(rewardData.getExperience(), rewardData.getGold() + 50);
        }
      }
    }

    return reward;
  }

  /**
   * 동적 목표 생성 (개선된 버전)
   */
  private Map<String, Integer> generateDynamicObjectives(QuestTemplateData template) {
    Map<String, Integer> dynamicObjectives = new HashMap<>();

    // 가변 타겟이 있는 경우 JSON 데이터 기반 선택
    if (template.getVariableTargets() != null && !template.getVariableTargets().isEmpty()) {
      String questType = template.getType().toUpperCase();
      int quantity = getRandomQuantity(template.getVariableQuantity());

      switch (questType) {
        case "KILL" -> {
          // JSON 몬스터 데이터에서 선택
          MonsterData selectedMonster = selectMonsterFromVariableTargets(template.getVariableTargets());
          if (selectedMonster != null) {
            String objectiveKey = "kill_" + selectedMonster.getId();
            dynamicObjectives.put(objectiveKey, quantity);
            logger.debug("동적 KILL 목표 생성: {} x{}", objectiveKey, quantity);
          }
        }
        case "COLLECT" -> {
          // JSON 아이템 데이터에서 선택
          GameItemData selectedItem = selectItemFromVariableTargets(template.getVariableTargets());
          if (selectedItem != null) {
            String objectiveKey = "collect_" + selectedItem.getId();
            dynamicObjectives.put(objectiveKey, quantity);
            logger.debug("동적 COLLECT 목표 생성: {} x{}", objectiveKey, quantity);
          }
        }
        case "EXPLORE" -> {
          // JSON 지역 데이터에서 선택
          LocationData selectedLocation = selectLocationFromVariableTargets(template.getVariableTargets());
          if (selectedLocation != null) {
            String objectiveKey = "explore_" + selectedLocation.getId();
            dynamicObjectives.put(objectiveKey, quantity);
            logger.debug("동적 EXPLORE 목표 생성: {} x{}", objectiveKey, quantity);
          }
        }
      }
    } else {
      // 가변 타겟이 없으면 기본 목표 사용
      dynamicObjectives.putAll(template.getObjectives());
    }

    return dynamicObjectives;
  }

  /**
   * variableTargets에서 실제 몬스터 데이터 선택
   */
  private MonsterData selectMonsterFromVariableTargets(List<String> targets) {
    for (String target : targets) {
      // 영문 ID인 경우 직접 검색
      if (isEnglishId(target)) {
        MonsterData monster = MonsterDataLoader.getMonsterById(target);
        if (monster != null)
          return monster;
      }
    }

    // 적합한 몬스터가 없으면 레벨 기반 선택
    return dataProvider.selectRandomMonsterForLevel(1); // 기본 레벨 1
  }

  /**
   * variableTargets에서 실제 아이템 데이터 선택
   */
  private GameItemData selectItemFromVariableTargets(List<String> targets) {
    Map<String, GameItemData> allItems = ConfigDataLoader.loadAllItems();

    for (String target : targets) {
      // 영문 ID인 경우 직접 검색
      if (isEnglishId(target)) {
        GameItemData item = allItems.get(target);
        if (item != null)
          return item;
      }
    }

    // 적합한 아이템이 없으면 랜덤 선택
    return dataProvider.selectRandomCollectableItem();
  }

  /**
   * variableTargets에서 실제 지역 데이터 선택
   */
  private LocationData selectLocationFromVariableTargets(List<String> targets) {
    for (String target : targets) {
      // 영문 ID인 경우 직접 검색
      if (isEnglishId(target)) {
        LocationData location = LocationManager.getLocation(target);
        if (location != null)
          return location;
      }
    }

    // 적합한 지역이 없으면 랜덤 선택
    return dataProvider.selectRandomLocationForLevel(1); // 기본 레벨 1
  }

  /**
   * 영문 ID 형식 확인 (상세 버전)
   */
  public static boolean isEnglishId(String target) {
    if (target == null || target.trim().isEmpty()) {
      return false;
    }

    String trimmed = target.trim();

    // 패턴 체크: 대문자로 시작, 대문자/숫자/언더스코어만 허용
    if (!trimmed.matches("^[A-Z][A-Z0-9_]*$")) {
      return false;
    }

    // 최소/최대 길이 체크
    return trimmed.length() >= 2 && trimmed.length() <= 50;
  }
}
