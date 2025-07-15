package rpg.application.factory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import rpg.domain.skill.Skill;
import rpg.domain.skill.SkillType;
import rpg.shared.constant.SystemConstants;

/**
 * JSON 파일 기반 스킬 팩토리 클래스
 * - 템플릿 데이터와 인스턴스 분리
 * - 캐싱으로 성능 최적화
 * - 동적 스킬 로딩 지원
 */
public class SkillFactory {
  private static final Logger logger = LoggerFactory.getLogger(SkillFactory.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  // 스킬 템플릿 캐시 (ID -> 스킬 템플릿)
  private static final Map<String, SkillTemplate> skillTemplates = new ConcurrentHashMap<>();

  // 초기화 상태
  private static boolean initialized = false;

  /**
   * 스킬 템플릿 내부 클래스
   */
  private static class SkillTemplate {
    public String id;
    public String name;
    public String description;
    public SkillType type;
    public int requiredLevel;
    public int manaCost;
    public int cooldown;
    public double damageMultiplier;
    public int healAmount;
    public int buffDuration;
    public String category;
    public String rarity;
    public String targetType;
    public Map<String, Object> effect;
  }

  /**
   * 팩토리 초기화 (JSON 파일 로드)
   */
  public static synchronized void initialize() {
    if (initialized) {
      logger.debug("SkillFactory 이미 초기화됨");
      return;
    }

    try {
      loadSkillTemplates();
      initialized = true;
      logger.info("SkillFactory 초기화 완료: {}개 스킬 템플릿 로드", skillTemplates.size());
    } catch (Exception e) {
      logger.error("SkillFactory 초기화 실패", e);
      throw new RuntimeException("스킬 팩토리 초기화 실패", e);
    }
  }

  /**
   * JSON 파일에서 스킬 템플릿 로드
   */
  private static void loadSkillTemplates() throws IOException {
    try (InputStream inputStream = SkillFactory.class.getResourceAsStream(SystemConstants.SKILLS_CONFIG)) {
      if (inputStream == null) {
        throw new IOException("스킬 설정 파일을 찾을 수 없습니다: " + SystemConstants.SKILLS_CONFIG);
      }

      JsonNode rootNode = objectMapper.readTree(inputStream);
      JsonNode skillsNode = rootNode.get("skills");

      if (skillsNode == null || !skillsNode.isArray()) {
        throw new IOException("스킬 설정 파일 형식이 올바르지 않습니다");
      }

      for (JsonNode skillNode : skillsNode) {
        SkillTemplate template = parseSkillTemplate(skillNode);
        if (template != null && template.id != null) {
          skillTemplates.put(template.id, template);
          logger.debug("스킬 템플릿 로드: {} ({})", template.id, template.name);
        }
      }

    } catch (IOException e) {
      logger.error("스킬 템플릿 로드 실패", e);
      throw e;
    }
  }

  /**
   * JSON 노드를 스킬 템플릿으로 파싱
   */
  private static SkillTemplate parseSkillTemplate(JsonNode node) {
    try {
      SkillTemplate template = new SkillTemplate();
      template.id = node.get("id").asText();
      template.name = node.get("name").asText();
      template.description = node.get("description").asText();
      template.type = SkillType.valueOf(node.get("type").asText());
      template.requiredLevel = node.get("requiredLevel").asInt();
      template.manaCost = node.get("manaCost").asInt();
      template.cooldown = node.get("cooldown").asInt();
      template.damageMultiplier = node.get("damageMultiplier").asDouble();
      template.healAmount = node.get("healAmount").asInt();
      template.buffDuration = node.get("buffDuration").asInt();
      template.category = node.has("category") ? node.get("category").asText() : "BASIC";
      template.rarity = node.has("rarity") ? node.get("rarity").asText() : "COMMON";
      template.targetType = node.has("targetType") ? node.get("targetType").asText() : "ENEMY";

      // 효과 정보 파싱
      if (node.has("effect")) {
        template.effect = objectMapper.convertValue(node.get("effect"), new TypeReference<Map<String, Object>>() {});
      }

      return template;
    } catch (Exception e) {
      logger.error("스킬 템플릿 파싱 실패: {}", node, e);
      return null;
    }
  }

  /**
   * ID로 스킬 인스턴스 생성
   */
  public static Skill createSkill(String skillId) {
    if (!initialized) {
      initialize();
    }

    if (skillId == null || skillId.trim().isEmpty()) {
      logger.warn("유효하지 않은 스킬 ID: {}", skillId);
      return null;
    }

    SkillTemplate template = skillTemplates.get(skillId);
    if (template == null) {
      logger.warn("스킬 템플릿을 찾을 수 없음: {}", skillId);
      return null;
    }

    try {
      // 템플릿에서 스킬 인스턴스 생성
      Skill skill = new Skill(template.name, template.description, template.type, template.requiredLevel, template.manaCost, template.cooldown, template.damageMultiplier, template.healAmount,
          template.buffDuration);

      // ID 설정 (Skill 클래스에 ID 필드 추가 필요)
      skill.setId(template.id);

      logger.debug("스킬 생성 성공: {} ({})", skillId, template.name);
      return skill;

    } catch (Exception e) {
      logger.error("스킬 생성 실패: {}", skillId, e);
      return null;
    }
  }

  /**
   * 레벨에 따라 사용 가능한 스킬 ID 목록 반환
   */
  public static List<String> getAvailableSkillIds(int playerLevel) {
    if (!initialized) {
      initialize();
    }

    return skillTemplates.values().stream().filter(template -> template.requiredLevel <= playerLevel).map(template -> template.id).sorted().toList();
  }

  /**
   * 기본 스킬 ID 목록 반환 (레벨 1부터 사용 가능)
   */
  public static List<String> getDefaultSkillIds() {
    return getAvailableSkillIds(1);
  }

  /**
   * 특정 카테고리의 스킬 ID 목록 반환
   */
  public static List<String> getSkillIdsByCategory(String category) {
    if (!initialized) {
      initialize();
    }

    return skillTemplates.values().stream().filter(template -> category.equalsIgnoreCase(template.category)).map(template -> template.id).sorted().toList();
  }

  /**
   * 스킬 존재 여부 확인
   */
  public static boolean hasSkill(String skillId) {
    if (!initialized) {
      initialize();
    }

    return skillTemplates.containsKey(skillId);
  }

  /**
   * 스킬 이름으로 ID 찾기
   */
  public static String getSkillIdByName(String skillName) {
    if (!initialized) {
      initialize();
    }

    return skillTemplates.values().stream().filter(template -> skillName.equals(template.name)).map(template -> template.id).findFirst().orElse(null);
  }

  /**
   * 모든 스킬 ID 목록 반환
   */
  public static List<String> getAllSkillIds() {
    if (!initialized) {
      initialize();
    }

    return skillTemplates.keySet().stream().sorted().toList();
  }

  /**
   * 스킬 정보 가져오기 (생성하지 않고 정보만)
   */
  public static Map<String, Object> getSkillInfo(String skillId) {
    if (!initialized) {
      initialize();
    }

    SkillTemplate template = skillTemplates.get(skillId);
    if (template == null) {
      return null;
    }

    Map<String, Object> info = new HashMap<>();
    info.put("id", template.id);
    info.put("name", template.name);
    info.put("description", template.description);
    info.put("type", template.type.name());
    info.put("requiredLevel", template.requiredLevel);
    info.put("manaCost", template.manaCost);
    info.put("cooldown", template.cooldown);
    info.put("damageMultiplier", template.damageMultiplier); // 추가
    info.put("healAmount", template.healAmount); // 추가
    info.put("buffDuration", template.buffDuration); // 추가
    info.put("category", template.category);
    info.put("rarity", template.rarity);
    info.put("targetType", template.targetType); // 추가

    // effect 정보도 추가 (있는 경우)
    if (template.effect != null) {
      info.put("effect", template.effect);
    }

    return info;
  }

  /**
   * 팩토리 리셋 (테스트용)
   */
  public static synchronized void reset() {
    skillTemplates.clear();
    initialized = false;
    logger.debug("SkillFactory 리셋 완료");
  }

  /**
   * 통계 정보 반환
   */
  public static Map<String, Integer> getStatistics() {
    if (!initialized) {
      initialize();
    }

    Map<String, Integer> stats = new HashMap<>();
    stats.put("totalSkills", skillTemplates.size());

    // 타입별 통계
    Map<SkillType, Long> typeStats = skillTemplates.values().stream().collect(java.util.stream.Collectors.groupingBy(template -> template.type, java.util.stream.Collectors.counting()));

    typeStats.forEach((type, count) -> stats.put(type.name().toLowerCase() + "Skills", count.intValue()));

    return stats;
  }

  /**
   * 초기화 상태 확인
   */
  public static boolean isInitialized() {
    return initialized;
  }

  /**
   * 로드된 스킬 수 반환
   */
  public static int getSkillCount() {
    if (!initialized) {
      initialize();
    }
    return skillTemplates.size();
  }
}
