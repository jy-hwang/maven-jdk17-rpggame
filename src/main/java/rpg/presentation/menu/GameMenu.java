package rpg.presentation.menu;

import rpg.shared.constant.SystemConstants;

public class GameMenu {

  /**
   * 확장된 메인 메뉴를 표시합니다.
   */
  public void showInGameMenu() {
    System.out.println("\n=== 🎯 게임 메뉴 ===");

    // 탐험 관련
    System.out.println("1. 🗡️ 탐험하기");
    System.out.println("2. 📊 상태 확인");

    // 관리 메뉴
    System.out.println("3. 🎒 인벤토리");
    System.out.println("4. ⚡ 스킬 관리");
    System.out.println("5. 📋 퀘스트");
    System.out.println("6. 🏪 상점");

    // 정보 메뉴
    System.out.println("7. 🗺️ 지역 정보");
    System.out.println("8. 📚 몬스터 도감");

    // 시스템 메뉴
    System.out.println("9. 📁 저장 관리");
    System.out.println("10. 🚪 게임 종료");
    System.out.println("11. ❓ 도움말");

    // 디버그 모드가 활성화된 경우에만 디버그 메뉴 표시
    if (SystemConstants.DEBUG_MODE) {
      System.out.println("99. 🔧 디버그 메뉴");
    }
    System.out.println("==================");
  }
}
