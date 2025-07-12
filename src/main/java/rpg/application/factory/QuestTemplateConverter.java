package rpg.application.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.domain.item.GameItem;
import rpg.domain.quest.Quest;
import rpg.domain.quest.QuestItemReward;
import rpg.domain.quest.QuestReward;
import rpg.domain.quest.QuestRewardData;
import rpg.domain.quest.QuestTemplateData;
import rpg.domain.quest.VariableQuantity;

public class QuestTemplateConverter {
  private static final Logger logger = LoggerFactory.getLogger(QuestTemplateConverter.class);
  private static final Random random = new Random();

  private final GameItemFactory itemFactory;

  public QuestTemplateConverter(GameItemFactory itemFactory) {
    this.itemFactory = itemFactory;
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
      Quest quest =
          new Quest(template.getId(), template.getTitle(), template.getDescription(), questType, template.getRequiredLevel(), objectives, reward);

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

        quest =
            new Quest(dynamicId, dynamicTitle, dynamicDescription, quest.getType(), quest.getRequiredLevel(), dynamicObjectives, quest.getReward());

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
   * 동적 목표 생성 (일일/주간 퀘스트용)
   */
  private Map<String, Integer> generateDynamicObjectives(QuestTemplateData template) {
    Map<String, Integer> dynamicObjectives = new HashMap<>();

    // 가변 타겟이 있는 경우
    if (template.getVariableTargets() != null && !template.getVariableTargets().isEmpty()) {
      List<String> targets = template.getVariableTargets();
      String selectedTarget = targets.get(random.nextInt(targets.size()));

      // 가변 수량 적용
      int quantity = getRandomQuantity(template.getVariableQuantity());

      // 동적 목표 키 생성
      String objectiveKey = generateObjectiveKey(template.getType(), selectedTarget);
      dynamicObjectives.put(objectiveKey, quantity);

    } else {
      // 가변 타겟이 없으면 기본 목표 사용
      dynamicObjectives.putAll(template.getObjectives());
    }

    return dynamicObjectives;
  }

  /**
   * 목표 키 생성
   */
  private String generateObjectiveKey(String questType, String target) {
    switch (questType.toUpperCase()) {
      case "KILL":
        return "kill_" + target;
      case "COLLECT":
        return "collect_" + target;
      case "EXPLORE":
        return "explore_" + target;
      default:
        return "complete_" + target;
    }
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
}
