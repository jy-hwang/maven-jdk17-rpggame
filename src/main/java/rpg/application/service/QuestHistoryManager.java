/**
 * 퀘스트 히스토리 및 만료 관리 시스템 - 완료된/만료된 일일 퀘스트 추적 - 로드 시 퀘스트 상태 복원 및 검증
 */
package rpg.application.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import rpg.application.service.ImprovedDailyQuestManager.QuestTier;
import rpg.domain.player.Player;
import rpg.domain.quest.Quest;
import rpg.domain.quest.QuestReward;

public class QuestHistoryManager {

  // === 퀘스트 히스토리 엔트리 ===
  public static class QuestHistoryEntry {
    private final String questId;
    private final String title;
    private final String description;
    private final String questType;
    private final QuestTier tier;
    private final String completedDate;
    private final QuestStatus finalStatus;
    private final boolean rewardClaimed;
    private final Map<String, Object> metadata;

    public enum QuestStatus {
      COMPLETED, // 완료됨
      EXPIRED, // 만료됨 (미완료)
      ABANDONED, // 포기됨
      FAILED // 실패함
    }

    public QuestHistoryEntry(String questId, String title, String description, String questType, QuestTier tier, String completedDate, QuestStatus finalStatus, boolean rewardClaimed,
        Map<String, Object> metadata) {
      this.questId = questId;
      this.title = title;
      this.description = description;
      this.questType = questType;
      this.tier = tier;
      this.completedDate = completedDate;
      this.finalStatus = finalStatus;
      this.rewardClaimed = rewardClaimed;
      this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    // Getters
    public String getQuestId() {
      return questId;
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    public String getQuestType() {
      return questType;
    }

    public QuestTier getTier() {
      return tier;
    }

    public String getCompletedDate() {
      return completedDate;
    }

    public QuestStatus getFinalStatus() {
      return finalStatus;
    }

    public boolean isRewardClaimed() {
      return rewardClaimed;
    }

    public Map<String, Object> getMetadata() {
      return metadata;
    }

    public boolean isDailyQuest() {
      return questId.startsWith("daily_");
    }

    public String getQuestDateFromId() {
      if (isDailyQuest()) {
        String[] parts = questId.split("_");
        return parts.length >= 3 ? parts[2] : null;
      }
      return null;
    }
  }

  // === 확장된 저장 데이터 구조 ===
  public static class ExtendedQuestProgress {
    private String questId;
    private String questType;
    private String baseTemplate;
    private QuestTier tier;
    private String generatedDate;
    private String expiryDate;
    private Map<String, Integer> progress;
    private String status;
    private Map<String, Object> questDetails; // 🆕 퀘스트 상세 정보 저장

    public ExtendedQuestProgress(Quest quest) {
      this.questId = quest.getId();
      this.questType = quest.getType().name();
      this.tier = QuestTier.getTierForLevel(quest.getRequiredLevel());
      this.generatedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      this.expiryDate = calculateExpiryDate(quest);
      this.progress = new HashMap<>(quest.getCurrentProgress());
      this.status = quest.getStatus().name();

      // 🆕 퀘스트 상세 정보 저장
      this.questDetails = new HashMap<>();
      this.questDetails.put("title", quest.getTitle());
      this.questDetails.put("description", quest.getDescription());
      this.questDetails.put("requiredLevel", quest.getRequiredLevel());
      this.questDetails.put("objectives", quest.getObjectives());
      if (quest.getReward() != null) {
        Map<String, Object> rewardInfo = new HashMap<>();
        rewardInfo.put("exp", quest.getReward().getExpReward());
        rewardInfo.put("gold", quest.getReward().getGoldReward());
        this.questDetails.put("reward", rewardInfo);
      }
    }

    private String calculateExpiryDate(Quest quest) {
      if (quest.getId().startsWith("daily_")) {
        return LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      } else if (quest.getId().startsWith("weekly_")) {
        return LocalDate.now().plusWeeks(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      }
      return null; // 만료되지 않음
    }

    // Getters & Setters
    public String getQuestId() {
      return questId;
    }

    public String getQuestType() {
      return questType;
    }

    public QuestTier getTier() {
      return tier;
    }

    public String getGeneratedDate() {
      return generatedDate;
    }

    public String getExpiryDate() {
      return expiryDate;
    }

    public Map<String, Integer> getProgress() {
      return progress;
    }

    public String getStatus() {
      return status;
    }

    public Map<String, Object> getQuestDetails() {
      return questDetails;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public void setProgress(Map<String, Integer> progress) {
      this.progress = progress;
    }
  }

  // === 퀘스트 히스토리 관리 ===
  private List<QuestHistoryEntry> questHistory;
  private Map<String, ExtendedQuestProgress> activeQuestDetails;

  public QuestHistoryManager() {
    this.questHistory = new ArrayList<>();
    this.activeQuestDetails = new HashMap<>();
  }

  /**
   * 퀘스트 시작 시 상세 정보 저장
   */
  public void recordQuestStart(Quest quest) {
    ExtendedQuestProgress questProgress = new ExtendedQuestProgress(quest);
    activeQuestDetails.put(quest.getId(), questProgress);
    System.out.println("📝 퀘스트 시작 기록: " + quest.getTitle());
  }

  /**
   * 퀘스트 완료 시 히스토리에 추가
   */
  public void recordQuestCompletion(Quest quest, boolean rewardClaimed) {
    ExtendedQuestProgress questProgress = activeQuestDetails.get(quest.getId());
    if (questProgress != null) {
      QuestHistoryEntry entry = createHistoryEntry(questProgress, QuestHistoryEntry.QuestStatus.COMPLETED, rewardClaimed);
      questHistory.add(entry);
      activeQuestDetails.remove(quest.getId());

      System.out.println("✅ 퀘스트 완료 기록: " + quest.getTitle());
    }
  }

  /**
   * 퀘스트 만료 시 히스토리에 추가
   */
  public void recordQuestExpiry(String questId, String reason) {
    ExtendedQuestProgress questProgress = activeQuestDetails.get(questId);
    if (questProgress != null) {
      QuestHistoryEntry entry = createHistoryEntry(questProgress, QuestHistoryEntry.QuestStatus.EXPIRED, false);
      // 만료 이유 추가
      entry.getMetadata().put("expiryReason", reason);
      entry.getMetadata().put("expiredDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

      questHistory.add(entry);
      activeQuestDetails.remove(questId);

      System.out.println("⏰ 퀘스트 만료 기록: " + questProgress.getQuestDetails().get("title") + " (" + reason + ")");
    }
  }

  private QuestHistoryEntry createHistoryEntry(ExtendedQuestProgress questProgress, QuestHistoryEntry.QuestStatus status, boolean rewardClaimed) {
    Map<String, Object> details = questProgress.getQuestDetails();
    return new QuestHistoryEntry(questProgress.getQuestId(), (String) details.get("title"), (String) details.get("description"), questProgress.getQuestType(), questProgress.getTier(),
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), status, rewardClaimed, new HashMap<>(details));
  }

  /**
   * 로드 시 만료된 퀘스트 처리
   */
  public QuestLoadResult processQuestsOnLoad(List<ExtendedQuestProgress> savedQuests) {
    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    List<Quest> validActiveQuests = new ArrayList<>();
    List<QuestHistoryEntry> newlyExpiredQuests = new ArrayList<>();

    for (ExtendedQuestProgress savedQuest : savedQuests) {
      if (isQuestExpired(savedQuest, today)) {
        // 만료된 퀘스트를 히스토리로 이동
        QuestHistoryEntry expiredEntry = createHistoryEntry(savedQuest, QuestHistoryEntry.QuestStatus.EXPIRED, false);
        expiredEntry.getMetadata().put("expiryReason", "로드 시 자동 만료");
        expiredEntry.getMetadata().put("originalDate", savedQuest.getGeneratedDate());

        newlyExpiredQuests.add(expiredEntry);
        questHistory.add(expiredEntry);

      } else {
        // 유효한 퀘스트는 활성 목록에 유지
        Quest restoredQuest = restoreQuestFromProgress(savedQuest);
        if (restoredQuest != null) {
          validActiveQuests.add(restoredQuest);
          activeQuestDetails.put(restoredQuest.getId(), savedQuest);
        }
      }
    }

    return new QuestLoadResult(validActiveQuests, newlyExpiredQuests);
  }

  private boolean isQuestExpired(ExtendedQuestProgress questProgress, String currentDate) {
    String expiryDate = questProgress.getExpiryDate();
    if (expiryDate == null)
      return false; // 만료되지 않는 퀘스트

    try {
      LocalDate expiry = LocalDate.parse(expiryDate);
      LocalDate current = LocalDate.parse(currentDate);
      return current.isAfter(expiry);
    } catch (Exception e) {
      return false; // 파싱 오류 시 만료되지 않은 것으로 처리
    }
  }

  private Quest restoreQuestFromProgress(ExtendedQuestProgress questProgress) {
    // Factory를 통해 퀘스트 복원 (기존 로직 활용)
    try {
      Map<String, Object> details = questProgress.getQuestDetails();

      // 기본 퀘스트 정보로 새 Quest 생성
      @SuppressWarnings("unchecked")
      Map<String, Integer> objectives = (Map<String, Integer>) details.get("objectives");
      @SuppressWarnings("unchecked")
      Map<String, Object> rewardInfo = (Map<String, Object>) details.get("reward");

      QuestReward reward = new QuestReward((Integer) rewardInfo.get("exp"), (Integer) rewardInfo.get("gold"));

      Quest quest = new Quest(questProgress.getQuestId(), (String) details.get("title"), (String) details.get("description"), Quest.QuestType.valueOf(questProgress.getQuestType()),
          (Integer) details.get("requiredLevel"), objectives, reward);

      // 진행도 복원
      quest.setCurrentProgress(questProgress.getProgress());
      quest.setStatus(Quest.QuestStatus.valueOf(questProgress.getStatus()));

      return quest;

    } catch (Exception e) {
      System.err.println("❌ 퀘스트 복원 실패: " + questProgress.getQuestId() + " - " + e.getMessage());
      return null;
    }
  }

  /**
   * 퀘스트 히스토리 조회 메서드들
   */
  public List<QuestHistoryEntry> getDailyQuestHistory(int days) {
    LocalDate cutoff = LocalDate.now().minusDays(days);

    return questHistory.stream().filter(QuestHistoryEntry::isDailyQuest).filter(entry -> {
      try {
        LocalDate entryDate = LocalDate.parse(entry.getCompletedDate());
        return entryDate.isAfter(cutoff) || entryDate.isEqual(cutoff);
      } catch (Exception e) {
        return false;
      }
    }).sorted((a, b) -> b.getCompletedDate().compareTo(a.getCompletedDate())).collect(Collectors.toList());
  }

  public List<QuestHistoryEntry> getExpiredQuests() {
    return questHistory.stream().filter(entry -> entry.getFinalStatus() == QuestHistoryEntry.QuestStatus.EXPIRED).sorted((a, b) -> b.getCompletedDate().compareTo(a.getCompletedDate()))
        .collect(Collectors.toList());
  }

  public List<QuestHistoryEntry> getCompletedQuests() {
    return questHistory.stream().filter(entry -> entry.getFinalStatus() == QuestHistoryEntry.QuestStatus.COMPLETED).sorted((a, b) -> b.getCompletedDate().compareTo(a.getCompletedDate()))
        .collect(Collectors.toList());
  }

  /**
   * 히스토리 정보 출력
   */
  public void displayQuestHistory(Player player) {
    System.out.println("\n=== 📚 퀘스트 히스토리 ===");

    System.out.println("\n🗓️ 최근 7일 일일 퀘스트:");
    List<QuestHistoryEntry> recentDaily = getDailyQuestHistory(7);
    if (recentDaily.isEmpty()) {
      System.out.println("   기록이 없습니다.");
    } else {
      for (QuestHistoryEntry entry : recentDaily) {
        String statusIcon = entry.getFinalStatus() == QuestHistoryEntry.QuestStatus.COMPLETED ? "✅" : "⏰";
        String rewardText = entry.isRewardClaimed() ? " (보상 수령)" : "";
        System.out.printf("   %s [%s] %s - %s%s\n", statusIcon, entry.getCompletedDate(), entry.getTitle(), entry.getFinalStatus().name(), rewardText);
      }
    }

    System.out.println("\n📈 통계:");
    long completedCount = questHistory.stream().filter(entry -> entry.getFinalStatus() == QuestHistoryEntry.QuestStatus.COMPLETED).count();
    long expiredCount = questHistory.stream().filter(entry -> entry.getFinalStatus() == QuestHistoryEntry.QuestStatus.EXPIRED).count();

    System.out.printf("   완료된 퀘스트: %d개\n", completedCount);
    System.out.printf("   만료된 퀘스트: %d개\n", expiredCount);
    if (completedCount + expiredCount > 0) {
      double successRate = (completedCount * 100.0) / (completedCount + expiredCount);
      System.out.printf("   성공률: %.1f%%\n", successRate);
    }

    System.out.println("=".repeat(30));
  }

  // === 로드 결과 클래스 ===
  public static class QuestLoadResult {
    private final List<Quest> validActiveQuests;
    private final List<QuestHistoryEntry> newlyExpiredQuests;

    public QuestLoadResult(List<Quest> validActiveQuests, List<QuestHistoryEntry> newlyExpiredQuests) {
      this.validActiveQuests = validActiveQuests;
      this.newlyExpiredQuests = newlyExpiredQuests;
    }

    public List<Quest> getValidActiveQuests() {
      return validActiveQuests;
    }

    public List<QuestHistoryEntry> getNewlyExpiredQuests() {
      return newlyExpiredQuests;
    }

    public void printLoadSummary() {
      System.out.println("\n📊 퀘스트 로드 결과:");
      System.out.printf("   유효한 활성 퀘스트: %d개\n", validActiveQuests.size());
      System.out.printf("   새로 만료된 퀘스트: %d개\n", newlyExpiredQuests.size());

      if (!newlyExpiredQuests.isEmpty()) {
        System.out.println("\n⏰ 만료된 퀘스트 목록:");
        for (QuestHistoryEntry expired : newlyExpiredQuests) {
          System.out.printf("   - %s (생성일: %s)\n", expired.getTitle(), expired.getMetadata().get("originalDate"));
        }
      }
    }
  }

  // === 저장/로드용 메서드들 ===
  public List<QuestHistoryEntry> getQuestHistory() {
    return new ArrayList<>(questHistory);
  }

  public void setQuestHistory(List<QuestHistoryEntry> history) {
    this.questHistory = history != null ? new ArrayList<>(history) : new ArrayList<>();
  }

  public Map<String, ExtendedQuestProgress> getActiveQuestDetails() {
    return new HashMap<>(activeQuestDetails);
  }

  public void setActiveQuestDetails(Map<String, ExtendedQuestProgress> details) {
    this.activeQuestDetails = details != null ? new HashMap<>(details) : new HashMap<>();
  }
}
