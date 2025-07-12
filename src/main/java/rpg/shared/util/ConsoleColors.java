package rpg.shared.util;

/**
 * 콘솔 색상 및 스타일링 유틸리티 클래스 ANSI Escape Codes를 사용하여 터미널에 색상과 스타일을 적용
 */
public class ConsoleColors {

  // === ANSI 색상 코드 ===

  // 텍스트 색상
  public static final String BLACK = "\u001B[30m";
  public static final String RED = "\u001B[31m";
  public static final String GREEN = "\u001B[32m";
  public static final String YELLOW = "\u001B[33m";
  public static final String BLUE = "\u001B[34m";
  public static final String PURPLE = "\u001B[35m";
  public static final String CYAN = "\u001B[36m";
  public static final String WHITE = "\u001B[37m";

  // 밝은 색상
  public static final String BRIGHT_BLACK = "\u001B[90m";
  public static final String BRIGHT_RED = "\u001B[91m";
  public static final String BRIGHT_GREEN = "\u001B[92m";
  public static final String BRIGHT_YELLOW = "\u001B[93m";
  public static final String BRIGHT_BLUE = "\u001B[94m";
  public static final String BRIGHT_PURPLE = "\u001B[95m";
  public static final String BRIGHT_CYAN = "\u001B[96m";
  public static final String BRIGHT_WHITE = "\u001B[97m";

  // 배경색
  public static final String BG_BLACK = "\u001B[40m";
  public static final String BG_RED = "\u001B[41m";
  public static final String BG_GREEN = "\u001B[42m";
  public static final String BG_YELLOW = "\u001B[43m";
  public static final String BG_BLUE = "\u001B[44m";
  public static final String BG_PURPLE = "\u001B[45m";
  public static final String BG_CYAN = "\u001B[46m";
  public static final String BG_WHITE = "\u001B[47m";

  // 스타일
  public static final String RESET = "\u001B[0m";
  public static final String BOLD = "\u001B[1m";
  public static final String DIM = "\u001B[2m";
  public static final String ITALIC = "\u001B[3m";
  public static final String UNDERLINE = "\u001B[4m";
  public static final String BLINK = "\u001B[5m";
  public static final String REVERSE = "\u001B[7m";
  public static final String STRIKETHROUGH = "\u001B[9m";

  // 대체 색상 (RGB 미지원 터미널용)
  public static final String LEGENDARY_FALLBACK = BOLD + YELLOW; // 굵은 노란색
  public static final String WARNING_FALLBACK = RED; // 빨간색으로 대체
  public static final String GOLD_FALLBACK = BOLD + YELLOW; // 굵은 노란색
  
  // === RGB 색상 (True Color 지원 터미널용) ===

  /**
   * RGB 값으로 텍스트 색상 생성
   */
  public static String rgb(int r, int g, int b) {
    return String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
  }

  /**
   * RGB 값으로 배경색 생성
   */
  public static String bgRgb(int r, int g, int b) {
    return String.format("\u001B[48;2;%d;%d;%dm", r, g, b);
  }

  // === 게임 전용 색상 ===

  // 희귀도별 색상
  public static final String COMMON = WHITE;
  public static final String UNCOMMON = BRIGHT_GREEN;
  public static final String RARE = BRIGHT_BLUE;
  public static final String EPIC = BRIGHT_PURPLE;
  public static final String LEGENDARY = BRIGHT_YELLOW;

  // 상태 색상
  public static final String SUCCESS = BRIGHT_GREEN;
  public static final String WARNING = BRIGHT_YELLOW;
  public static final String ERROR = BRIGHT_RED;
  public static final String INFO = BRIGHT_CYAN;

  // HP/MP 색상
  public static final String HP_COLOR = BRIGHT_RED;
  public static final String MP_COLOR = BRIGHT_BLUE;
  public static final String GOLD_COLOR = BRIGHT_YELLOW;
  public static final String EXP_COLOR = BRIGHT_GREEN;

  // === 헬퍼 메서드들 ===

  /**
   * 텍스트에 색상 적용
   */
  public static String colorize(String text, String color) {
    return color + text + RESET;
  }

  /**
   * 희귀도에 따른 색상 적용 (가독성 개선)
   */
  public static String rarityColor(String text, String rarity) {
    String color = switch (rarity.toUpperCase()) {
      case "COMMON", "일반" -> COMMON;
      case "UNCOMMON", "고급" -> UNCOMMON;
      case "RARE", "희귀" -> RARE;
      case "EPIC", "영웅" -> EPIC;
      case "LEGENDARY", "전설" -> isColorSupported() ? LEGENDARY : LEGENDARY_FALLBACK;
      default -> WHITE;
    };
    return colorize(text, color);
  }

  /**
   * 성공 메시지 색상
   */
  public static String success(String text) {
    return colorize("✅ " + text, SUCCESS);
  }

  /**
   * 경고 메시지 색상 (가독성 개선)
   */
  public static String warning(String text) {
    String color = isColorSupported() ? WARNING : WARNING_FALLBACK;
    return colorize("⚠️ " + text, color);
  }

  /**
   * 오류 메시지 색상
   */
  public static String error(String text) {
    return colorize("❌ " + text, ERROR);
  }

  /**
   * 정보 메시지 색상
   */
  public static String info(String text) {
    return colorize("ℹ️ " + text, INFO);
  }

  /**
   * HP 표시 색상
   */
  public static String hp(int current, int max) {
    return colorize(String.format("❤️ %d/%d", current, max), HP_COLOR);
  }

  /**
   * MP 표시 색상
   */
  public static String mp(int current, int max) {
    return colorize(String.format("💙 %d/%d", current, max), MP_COLOR);
  }

  /**
   * 골드 표시 색상 (가독성 개선)
   */
  public static String gold(int amount) {
    String color = isColorSupported() ? GOLD_COLOR : GOLD_FALLBACK;
    return colorize(String.format("💰 %d G", amount), color);
  }

  /**
   * 경험치 표시 색상
   */
  public static String exp(int amount) {
    return colorize(String.format("⭐ %d EXP", amount), EXP_COLOR);
  }

  /**
   * 진행률 바 생성 (색상 포함)
   */
  public static String progressBar(int current, int max, int barLength) {
    int filledLength = (int) ((double) current / max * barLength);

    StringBuilder bar = new StringBuilder();
    bar.append("[");

    // 채워진 부분 (초록색)
    bar.append(GREEN);
    for (int i = 0; i < filledLength; i++) {
      bar.append("█");
    }

    // 빈 부분 (회색)
    bar.append(BRIGHT_BLACK);
    for (int i = filledLength; i < barLength; i++) {
      bar.append("░");
    }

    bar.append(RESET);
    bar.append("] ");
    bar.append(String.format("%d/%d", current, max));

    return bar.toString();
  }

  /**
   * 그라데이션 텍스트 생성
   */
  public static String gradient(String text, String startColor, String endColor) {
    // 간단한 그라데이션 (중간 지점만)
    int mid = text.length() / 2;
    return startColor + text.substring(0, mid) + endColor + text.substring(mid) + RESET;
  }

  /**
   * 무지개 텍스트
   */
  public static String rainbow(String text) {
    String[] colors = {RED, YELLOW, GREEN, CYAN, BLUE, PURPLE};
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < text.length(); i++) {
      String color = colors[i % colors.length];
      result.append(color).append(text.charAt(i));
    }

    return result.toString() + RESET;
  }

  /**
   * 터미널 색상 지원 여부 확인
   */
  public static boolean isColorSupported() {
    String term = System.getenv("TERM");
    return term != null && (term.contains("color") || term.contains("xterm") || term.contains("screen"));
  }

  /**
   * 색상 비활성화 모드 (디버그용)
   */
  private static boolean colorDisabled = false;

  public static void disableColors() {
    colorDisabled = true;
  }

  public static void enableColors() {
    colorDisabled = false;
  }

  /**
   * 색상이 비활성화된 경우 색상 코드 제거
   */
  public static String applyColor(String text, String color) {
    if (colorDisabled || !isColorSupported()) {
      return text;
    }
    return color + text + RESET;
  }

  // === 테스트 메서드 ===

  /**
   * 색상 테스트 출력
   */
  public static void testColors() {
    System.out.println("=== 🎨 콘솔 색상 테스트 ===");

    // 기본 색상
    System.out.println("기본 색상:");
    System.out.println(RED + "빨간색 텍스트" + RESET);
    System.out.println(GREEN + "초록색 텍스트" + RESET);
    System.out.println(BLUE + "파란색 텍스트" + RESET);
    System.out.println(YELLOW + "노란색 텍스트" + RESET);

    // 희귀도 색상 (개선된 버전)
    System.out.println("\n희귀도 색상 (개선된 가독성):");
    System.out.println(rarityColor("일반 아이템", "COMMON"));
    System.out.println(rarityColor("고급 아이템", "UNCOMMON"));
    System.out.println(rarityColor("희귀 아이템", "RARE"));
    System.out.println(rarityColor("영웅 아이템", "EPIC"));
    System.out.println(rarityColor("전설 아이템", "LEGENDARY") + " ← 더 진한 골드색");

    // 상태 메시지 (개선된 버전)
    System.out.println("\n상태 메시지 (개선된 가독성):");
    System.out.println(success("성공 메시지"));
    System.out.println(warning("경고 메시지") + " ← 오렌지색으로 변경");
    System.out.println(error("오류 메시지"));
    System.out.println(info("정보 메시지"));

    // 게임 스탯 (개선된 버전)
    System.out.println("\n게임 스탯 (개선된 가독성):");
    System.out.println(hp(150, 200));
    System.out.println(mp(80, 100));
    System.out.println(gold(1500) + " ← 더 진한 골드색");
    System.out.println(exp(2450));

    // 진행률 바
    System.out.println("\n진행률 바:");
    System.out.println("HP: " + progressBar(150, 200, 20));
    System.out.println("MP: " + progressBar(80, 100, 20));
    System.out.println("EXP: " + progressBar(2450, 3000, 20));

    // 스타일
    System.out.println("\n텍스트 스타일:");
    System.out.println(BOLD + "굵은 텍스트" + RESET);
    System.out.println(ITALIC + "기울임 텍스트" + RESET);
    System.out.println(UNDERLINE + "밑줄 텍스트" + RESET);

    // 특수 효과
    System.out.println("\n특수 효과:");
    System.out.println(rainbow("무지개 텍스트"));
    System.out.println(gradient("그라데이션 텍스트", RED, BLUE));

    System.out.println("\n======================");
  }
}
