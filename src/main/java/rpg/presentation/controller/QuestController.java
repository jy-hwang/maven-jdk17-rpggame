package rpg.presentation.controller;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.application.service.QuestManager;
import rpg.application.validator.InputValidator;
import rpg.core.engine.GameState;
import rpg.domain.item.GameItem;
import rpg.domain.player.Player;
import rpg.domain.quest.Quest;
import rpg.domain.quest.QuestReward;
import rpg.shared.constant.GameConstants;
import rpg.shared.constant.ItemConstants;

/**
 * 퀘스트 시스템을 전담하는 컨트롤러 QuestManager와 QuestReward 클래스와 연동
 */
public class QuestController {
  private static final Logger logger = LoggerFactory.getLogger(QuestController.class);

  private final QuestManager questManager;
  private final GameState gameState;
  private final Player currentPlayer;

  public QuestController(QuestManager questManager, GameState gameState, Player player) {
    this.questManager = questManager;
    this.gameState = gameState;
    this.currentPlayer = player;
    logger.debug("QuestController 초기화 완료");
  }

  /**
   * 퀘스트 관리 메뉴를 실행합니다.
   * 
   * @param player 플레이어 캐릭터
   */
  public void manageQuests(Player player) {
    while (true) {
      displayQuestMenu();

      int choice = InputValidator.getIntInput("선택: ", 0, 8);

      switch (choice) {
        case 1:
          acceptQuest(player);
          break;
        case 2:
          displayActiveQuests();
          showQuestDetails("active");
          break;
        case 3:
          displayCompletedQuests();
          showQuestDetails("completed");
          break;
        case 4:
          // 일일 퀘스트 새로고침
          // if (InputValidator.getConfirmation("일일 퀘스트를 새로고침하시겠습니까?")) {
          // player.getQuestManager().refreshDailyQuests(player);
          // }
          break;
        case 5:
          // 퀘스트 히스토리
          player.getQuestManager().showQuestHistory(player);
          break;
        case 6:
          // 일일 퀘스트 통계
          player.getQuestManager().showDailyQuestStats();
          player.getQuestManager().simulateDailyQuests(player);
        case 7:
          claimQuestReward(player);
          break;
        case 8:
          displayQuestStatistics(player);
          break;
        case 0:
          return;
      }
    }
  }

  /**
   * 퀘스트 메뉴를 표시합니다.
   */
  private void displayQuestMenu() {
    System.out.println("\n=== 퀘스트 관리 ===");
    System.out.println("1. 📋 수락 가능한 퀘스트");
    System.out.println("2. ⚡ 진행 중인 퀘스트");
    System.out.println("3. ✅ 완료된 퀘스트");
    System.out.println("4. 🔄 일일 퀘스트 새로고침(X)");
    System.out.println("5. 📚 퀘스트 히스토리");
    System.out.println("6. 📊 일일 퀘스트 통계");
    System.out.println("7. 🎁 퀘스트 보상 수령");
    System.out.println("8. 📊 퀘스트 통계");
    System.out.println("0. 🔙 돌아가기");
  }

  /**
   * 수락 가능한 퀘스트를 표시하고 수락을 처리합니다.
   */
  private void acceptQuest(Player player) {
    questManager.displayAvailableQuests(player);
    var availableQuests = questManager.getAvailableQuests(player);

    if (availableQuests.isEmpty()) {
      System.out.println("현재 수락 가능한 퀘스트가 없습니다.");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    int questIndex = InputValidator.getIntInput("수락할 퀘스트 번호 (0: 취소): ", 0, availableQuests.size()) - 1;
    if (questIndex < 0)
      return;

    Quest quest = availableQuests.get(questIndex);
    displayQuestDetails(quest);

    if (InputValidator.getConfirmation("이 퀘스트를 수락하시겠습니까?")) {
      if (questManager.acceptQuest(quest.getId(), player)) {
        System.out.println("✅ 퀘스트 '" + quest.getTitle() + "'을(를) 수락했습니다!");
        logger.info("퀘스트 수락: {} -> {}", player.getName(), quest.getTitle());
      } else {
        System.out.println("❌ 퀘스트 수락에 실패했습니다.");
        logger.warn("퀘스트 수락 실패: {} -> {}", player.getName(), quest.getTitle());
      }
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 진행 중인 퀘스트를 표시합니다. (수정된 버전)
   */
  private void displayActiveQuests() {
    // currentPlayer를 사용하여 정확한 진행도 표시
    questManager.displayActiveQuestsWithPlayer(currentPlayer);
  }

  /**
   * 완료된 퀘스트를 표시합니다.
   */
  private void displayCompletedQuests() {
    System.out.println("\n=== 완료된 퀘스트 ===");
    questManager.displayCompletedQuests();
  }

  /**
   * 퀘스트 상세 정보를 표시합니다.
   */
  private void showQuestDetails(String type) {
    List<Quest> quests = type.equals("active") ? questManager.getActiveQuests() : questManager.getCompletedQuests();

    if (quests.isEmpty()) {
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    int questIndex = InputValidator.getIntInput("상세 정보를 볼 퀘스트 번호 (0: 취소): ", 0, quests.size()) - 1;
    if (questIndex < 0)
      return;

    Quest selectedQuest = quests.get(questIndex);
    displayQuestDetails(selectedQuest);

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 퀘스트 상세 정보를 표시합니다.
   */
  private void displayQuestDetails(Quest quest) {
    System.out.println("\n" + "=".repeat(50));
    System.out.println("📋 퀘스트: " + quest.getTitle());
    System.out.println("📝 설명: " + quest.getDescription());
    System.out.println("🎯 목표: " + quest.getObjectiveDescription());
    System.out.println("📊 진행도: " + quest.getProgressDescription());
    System.out.println("🏆 상태: " + getQuestStatusKorean(quest.getStatus()));
    System.out.println("⭐ 필요 레벨: " + quest.getRequiredLevel());
    System.out.println("🏷️ 타입: " + getQuestTypeKorean(quest.getType()));

    // 보상 정보 표시 (QuestReward 클래스 사용)
    QuestReward reward = quest.getReward();
    if (reward != null && !reward.isEmpty()) {
      System.out.println("🎁 보상: " + reward.getRewardDescription());
    } else {
      System.out.println("🎁 보상: 없음");
    }

    System.out.println("=".repeat(50));
  }

  /**
   * 퀘스트 보상을 수령합니다.
   */
  private void claimQuestReward(Player player) {
    var completedQuests = questManager.getCompletedQuests().stream().filter(quest -> quest.getStatus() == Quest.QuestStatus.COMPLETED).toList();

    if (completedQuests.isEmpty()) {
      System.out.println("보상을 수령할 수 있는 퀘스트가 없습니다.");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    System.out.println("\n=== 보상 수령 가능한 퀘스트 ===");
    for (int i = 0; i < completedQuests.size(); i++) {
      Quest quest = completedQuests.get(i);
      System.out.printf("%d. %s%n", i + GameConstants.NUMBER_ONE, quest.getTitle());

      QuestReward reward = quest.getReward();
      if (reward != null) {
        System.out.print("   보상: ");
        if (reward.getExpReward() > 0)
          System.out.print("경험치 " + reward.getExpReward() + " ");
        if (reward.getGoldReward() > 0)
          System.out.print("골드 " + reward.getGoldReward() + " ");
        var itemRewards = reward.getItemRewards();
        if (itemRewards != null && !itemRewards.isEmpty()) {
          String itemsText =
              itemRewards.entrySet().stream().map(entry -> entry.getKey().getName() + " x" + entry.getValue()).collect(Collectors.joining(", "));
          System.out.print(itemsText);
        }
        System.out.println();
      }
    }

    int questIndex = InputValidator.getIntInput("보상을 수령할 퀘스트 번호 (0: 취소): ", 0, completedQuests.size()) - 1;
    if (questIndex < 0)
      return;

    Quest quest = completedQuests.get(questIndex);

    if (InputValidator.getConfirmation("'" + quest.getTitle() + "' 퀘스트의 보상을 수령하시겠습니까?")) {
      if (questManager.claimQuestReward(quest.getId(), player)) {
        System.out.println("🎁 퀘스트 보상을 수령했습니다!");
        gameState.incrementQuestsCompleted();
        logger.info("퀘스트 보상 수령: {} -> {}", player.getName(), quest.getTitle());

        // 보상 내용 상세 표시
        QuestReward reward = quest.getReward();
        if (reward != null) {
          displayRewardDetails(reward);
        }
      } else {
        System.out.println("❌ 보상 수령에 실패했습니다.");
        logger.warn("퀘스트 보상 수령 실패: {} -> {}", player.getName(), quest.getTitle());
      }
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 보상 상세 내용을 표시합니다.
   */
  private void displayRewardDetails(QuestReward reward) {
    if (reward.getExpReward() > GameConstants.NUMBER_ZERO) {
      System.out.println("📈 경험치 +" + reward.getExpReward() + " 획득!");
    }
    if (reward.getGoldReward() > GameConstants.NUMBER_ZERO) {
      System.out.println("💰 골드 +" + reward.getGoldReward() + " 획득!");
    }

    // 아이템 보상들 표시
    var itemRewards = reward.getItemRewards();
    if (!itemRewards.isEmpty()) {
      for (var entry : itemRewards.entrySet()) {
        GameItem item = entry.getKey();
        int quantity = entry.getValue();
        System.out.println("📦 " + item.getName() + " x" + quantity + " 획득!");
      }
    }
  }

  /**
   * 퀘스트 통계를 표시합니다.
   */
  private void displayQuestStatistics(Player player) {
    QuestManager.QuestStatistics stats = questManager.getStatistics(player);

    System.out.println("\n=== 퀘스트 통계 ===");
    System.out.println("📋 수락 가능: " + stats.getAvailableCount() + "개");
    System.out.println("⚡ 진행 중: " + stats.getActiveCount() + "개");
    System.out.println("✅ 완료 (미수령): " + stats.getClaimableCount() + "개");
    System.out.println("🎁 완료 (수령): " + stats.getClaimedCount() + "개");
    System.out.println("📊 총 퀘스트: " + stats.getTotalCount() + "개");

    if (stats.getTotalCount() > GameConstants.NUMBER_ZERO) {
      System.out.printf("🏆 완료율: %.1f%%\n", stats.getCompletionRate());

      // 진행도 바 표시
      displayProgressBar(stats.getCompletionRate());
    }

    // 다음 해금 퀘스트 안내
    if (stats.getAvailableCount() == GameConstants.NUMBER_ZERO && player.getLevel() <= ItemConstants.INTERMEDIATE_LEVEL) {
      System.out.println("\n💡 팁: 레벨을 올리면 새로운 퀘스트가 해금됩니다!");
    }

    System.out.println("==================");
    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 진행도 바를 표시합니다.
   */
  private void displayProgressBar(double percentage) {
    int barLength = GameConstants.NUMBER_TWENTY;
    int filledLength = (int) (barLength * percentage / GameConstants.NUMBER_HUNDRED);

    System.out.print("📊 진행도: [");
    for (int i = GameConstants.NUMBER_ZERO; i < barLength; i++) {
      if (i < filledLength) {
        System.out.print("█");
      } else {
        System.out.print("░");
      }
    }
    System.out.printf("] %.1f%%\n", percentage);
  }

  /**
   * 퀘스트 상태를 한국어로 변환합니다.
   */
  private String getQuestStatusKorean(Quest.QuestStatus status) {
    return switch (status) {
      case AVAILABLE -> "수락 가능";
      case ACTIVE -> "진행 중";
      case COMPLETED -> "완료";
      case CLAIMED -> "보상 수령 완료";
      case FAILED -> "실패";
    };
  }

  /**
   * 퀘스트 타입을 한국어로 변환합니다.
   */
  private String getQuestTypeKorean(Quest.QuestType type) {
    return switch (type) {
      case KILL -> "처치";
      case COLLECT -> "수집";
      case LEVEL -> "레벨 달성";
      case EXPLORE -> "탐험";
      case DELIVERY -> "배달";
    };
  }

  /**
   * 몬스터 처치 퀘스트 진행도 업데이트 - ID 기반으로 수정
   */
  public void updateKillProgress(String monsterId) { // ← 파라미터 이름도 변경
    logger.debug("몬스터 처치 퀘스트 진행도 업데이트 요청: {}", monsterId);
    questManager.updateKillProgress(monsterId);
  }

  /**
   * 아이템 수집 퀘스트 진행도 업데이트 - ID 기반으로 수정
   */
  public void updateCollectionProgress(Player player, String itemId, int quantity) { // ← 파라미터 이름도 변경
    logger.debug("아이템 수집 퀘스트 진행도 업데이트 요청: {} x{}", itemId, quantity);
    questManager.updateCollectionProgress(player, itemId, quantity);
  }

  /**
   * 레벨업 퀘스트 진행도를 업데이트합니다.
   * 
   * @param player 플레이어 캐릭터
   */
  public void updateLevelProgress(Player player) {
    questManager.updateLevelProgress(player);
    logger.debug("레벨업 퀘스트 진행도 업데이트: 레벨 {}", player.getLevel());
  }

  /**
   * 진행 중인 퀘스트가 있는지 확인합니다.
   * 
   * @return 진행 중인 퀘스트 존재 여부
   */
  public boolean hasActiveQuests() {
    return !questManager.getActiveQuests().isEmpty();
  }

  /**
   * 수령 가능한 보상이 있는지 확인합니다.
   * 
   * @return 수령 가능한 보상 존재 여부
   */
  public boolean hasClaimableRewards() {
    return !questManager.getClaimableQuests().isEmpty();
  }

  /**
   * 퀘스트 매니저를 반환합니다. (다른 컨트롤러에서 필요한 경우)
   * 
   * @return QuestManager 인스턴스
   */
  public QuestManager getQuestManager() {
    return questManager;
  }

  /**
   * 퀘스트 완료 알림을 표시합니다.
   */
  public void showQuestCompletionNotification(Quest quest) {
    System.out.println("\n" + "★".repeat(GameConstants.NUMBER_TWENTY));
    System.out.println("🎉 퀘스트 완료! 🎉");
    System.out.println("📋 " + quest.getTitle());

    QuestReward reward = quest.getReward();
    if (reward != null && !reward.isEmpty()) {
      System.out.println("🎁 보상: " + reward.getRewardDescription());
      System.out.println("💡 퀘스트 메뉴에서 보상을 수령하세요!");
    }

    System.out.println("★".repeat(GameConstants.NUMBER_TWENTY));
  }

  /**
   * 퀘스트 힌트를 표시합니다.
   */
  public void showQuestHints(Player player) {
    var activeQuests = questManager.getActiveQuests();

    if (activeQuests.isEmpty()) {
      System.out.println("💡 새로운 퀘스트를 수락해보세요!");
      return;
    }

    System.out.println("\n=== 퀘스트 힌트 ===");
    for (Quest quest : activeQuests) {
      System.out.println("📋 " + quest.getTitle());
      System.out.println("💡 " + getQuestHint(quest));
      System.out.println();
    }
  }

  /**
   * 퀘스트별 힌트를 생성합니다.
   */
  private String getQuestHint(Quest quest) {
    return switch (quest.getType()) {
      case KILL -> "탐험하여 목표 몬스터를 찾아 처치하세요.";
      case COLLECT -> "탐험 중 아이템을 수집하거나 상점에서 구매하세요.";
      case LEVEL -> "몬스터를 처치하여 경험치를 획득하고 레벨업하세요.";
      case EXPLORE -> "다양한 지역을 탐험해보세요.";
      case DELIVERY -> "지정된 NPC에게 아이템을 전달하세요.";
    };
  }

  /**
   * 퀘스트 진행도 요약을 반환합니다.
   */
  public String getQuestProgressSummary(Player player) {
    QuestManager.QuestStatistics stats = questManager.getStatistics(player);

    if (stats.getActiveCount() == GameConstants.NUMBER_ZERO && stats.getClaimableCount() == 0) {
      return "현재 진행 중인 퀘스트가 없습니다.";
    }

    StringBuilder summary = new StringBuilder();
    if (stats.getActiveCount() > GameConstants.NUMBER_ZERO) {
      summary.append("진행 중: ").append(stats.getActiveCount()).append("개");
    }
    if (stats.getClaimableCount() > GameConstants.NUMBER_ZERO) {
      if (summary.length() > GameConstants.NUMBER_ZERO)
        summary.append(", ");
      summary.append("보상 수령 대기: ").append(stats.getClaimableCount()).append("개");
    }

    return summary.toString();
  }

  /**
   * 범용 퀘스트 진행도 업데이트 메서드
   * 
   * @param progressType 진행도 타입 ("treasure", "merchant", "explore" 등)
   * @param amount 진행 수량
   */
  public void updateProgress(String progressType, int amount) {
    updateProgress(progressType, null, amount);
  }

  /**
   * 범용 퀘스트 진행도 업데이트 메서드 (상세 버전)
   * 
   * @param progressType 진행도 타입
   * @param target 대상 (아이템명, 몬스터명 등, null 가능)
   * @param amount 진행 수량
   */
  public void updateProgress(String progressType, String target, int amount) {
    try {
      switch (progressType.toLowerCase()) {
        case "treasure" -> updateTreasureProgress(amount);
        case "merchant" -> updateMerchantProgress(amount);
        case "explore" -> updateExploreProgress(target, amount);
        case "kill" -> {
          if (target != null) {
            updateKillProgress(target);
          }
        }
        case "collect" -> {
          if (target != null) {
            updateCollectionProgress(getCurrentPlayer(), target, amount);
          }
        }
        case "level" -> updateLevelProgress(getCurrentPlayer());
        case "delivery" -> updateDeliveryProgress(target, amount);
        case "craft" -> updateCraftProgress(target, amount);
        case "purchase" -> updatePurchaseProgress(target, amount);
        default -> {
          logger.warn("알 수 없는 퀘스트 진행도 타입: {}", progressType);
          // 커스텀 진행도 처리 시도
          updateCustomProgress(progressType, target, amount);
        }
      }

      logger.debug("퀘스트 진행도 업데이트: {} {} x{}", progressType, target != null ? target : "", amount);

    } catch (Exception e) {
      logger.error("퀘스트 진행도 업데이트 실패: {} {} x{}", progressType, target, amount, e);
    }
  }

  // === 2. 구체적인 진행도 업데이트 메서드들 ===

  /**
   * 보물 발견 퀘스트 진행도 업데이트
   */
  public void updateTreasureProgress(int amount) {
    questManager.updateCustomProgress("find_treasure", amount);
    logger.debug("보물 발견 퀘스트 진행도 업데이트: {}개", amount);
  }

  /**
   * 상인 조우 퀘스트 진행도 업데이트
   */
  public void updateMerchantProgress(int amount) {
    questManager.updateCustomProgress("meet_merchant", amount);
    logger.debug("상인 조우 퀘스트 진행도 업데이트: {}회", amount);
  }

  /**
   * 탐험 퀘스트 진행도 업데이트
   */
  public void updateExploreProgress(String locationName, int amount) {
    if (locationName != null) {
      // 특정 지역 탐험 퀘스트
      questManager.updateCustomProgress("explore_" + locationName.toLowerCase().replace(" ", "_"), amount);
    }
    // 일반 탐험 퀘스트
    questManager.updateCustomProgress("explore_any", amount);
    logger.debug("탐험 퀘스트 진행도 업데이트: {} {}회", locationName != null ? locationName : "일반", amount);
  }

  /**
   * 배달 퀘스트 진행도 업데이트
   */
  public void updateDeliveryProgress(String npcName, int amount) {
    if (npcName != null) {
      questManager.updateCustomProgress("delivery_" + npcName.toLowerCase(), amount);
    }
    questManager.updateCustomProgress("delivery_any", amount);
    logger.debug("배달 퀘스트 진행도 업데이트: {} {}개", npcName != null ? npcName : "일반", amount);
  }

  /**
   * 제작 퀘스트 진행도 업데이트
   */
  public void updateCraftProgress(String itemName, int amount) {
    if (itemName != null) {
      questManager.updateCustomProgress("craft_" + itemName.toLowerCase(), amount);
    }
    questManager.updateCustomProgress("craft_any", amount);
    logger.debug("제작 퀘스트 진행도 업데이트: {} {}개", itemName != null ? itemName : "일반", amount);
  }

  /**
   * 구매 퀘스트 진행도 업데이트
   */
  public void updatePurchaseProgress(String itemName, int amount) {
    if (itemName != null) {
      questManager.updateCustomProgress("purchase_" + itemName.toLowerCase(), amount);
    }
    questManager.updateCustomProgress("purchase_any", amount);
    logger.debug("구매 퀘스트 진행도 업데이트: {} {}개", itemName != null ? itemName : "일반", amount);
  }

  /**
   * 커스텀 퀘스트 진행도 업데이트
   */
  public void updateCustomProgress(String progressKey, String target, int amount) {
    String fullKey = target != null ? progressKey + "_" + target.toLowerCase() : progressKey;
    questManager.updateCustomProgress(fullKey, amount);
    logger.debug("커스텀 퀘스트 진행도 업데이트: {} {}개", fullKey, amount);
  }

  /**
   * 현재 플레이어 반환 (다른 메서드에서 사용)
   */
  private Player getCurrentPlayer() {
    return currentPlayer;
  }

  /**
   * 지역별 탐험 완료 시 호출
   */
  public void onLocationExplored(String locationName) {
    updateExploreProgress(locationName, 1);

    // 특정 지역 관련 특별 퀘스트 체크
    checkSpecialLocationQuests(locationName);
  }

  /**
   * 상인과의 거래 완료 시 호출
   */
  public void onMerchantTradeCompleted(String merchantName, String itemName, int quantity) {
    updateMerchantProgress(1);
    updatePurchaseProgress(itemName, quantity);

    // 특정 상인 관련 퀘스트 체크
    checkMerchantQuests(merchantName);
  }

  /**
   * 보물 발견 시 호출
   */
  public void onTreasureFound(String treasureName, String locationName) {
    updateTreasureProgress(1);

    // 특정 보물이나 지역 관련 퀘스트 체크
    if (treasureName != null) {
      updateCustomProgress("find_specific_treasure", treasureName, 1);
    }
    if (locationName != null) {
      updateCustomProgress("find_treasure_in_location", locationName, 1);
    }
  }

  // === 4. 특별 퀘스트 체크 메서드들 ===

  /**
   * 특정 지역 관련 특별 퀘스트 확인
   */
  private void checkSpecialLocationQuests(String locationName) {
    // 특정 지역 연속 탐험, 지역 정복 등의 퀘스트 체크
    questManager.checkLocationBasedQuests(locationName);
  }

  /**
   * 상인 관련 특별 퀘스트 확인
   */
  private void checkMerchantQuests(String merchantName) {
    // 특정 상인과의 우호도, 거래 횟수 등 관련 퀘스트 체크
    questManager.checkMerchantBasedQuests(merchantName);
  }
}

