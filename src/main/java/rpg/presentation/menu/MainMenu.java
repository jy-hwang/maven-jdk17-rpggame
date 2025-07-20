package rpg.presentation.menu;

import rpg.shared.constant.SystemConstants;

public class MainMenu {
  /**
   * 환영 메시지를 표시합니다.
   */
  public void showWelcomeMessage() {
    System.out.println("====================================");
    System.out.println("   🎮 RPG 게임 v" + SystemConstants.GAME_VERSION + "🎮   ");
    System.out.println("====================================");

    System.out.println("새로운 기능:");
    System.out.println("• 📦 다중 저장 슬롯 시스템 (5개)");
    System.out.println("• 🏗️ 개선된 아키텍처 (Controller 분리)");
    System.out.println("• 🌟 향상된 탐험 시스템(탐험지역별 몬스터추가)");
    System.out.println("• 🛍️ 확장된 상점 시스템(구매 / 판매)");
    System.out.println("• 📋 고도화된 퀘스트 관리");
    System.out.println("====================================");
  }
  
  /**
   * 메인 메뉴를 표시합니다.
   */
  public void showMainMenu() {
    System.out.println("\n=== 🎮 메인 메뉴 ===");
    System.out.println("1. 🆕 새로하기");
    System.out.println("2. 📁 불러오기");
    System.out.println("3. 🚪 종료하기");
    System.out.println("==================");
  }
  
  
}
