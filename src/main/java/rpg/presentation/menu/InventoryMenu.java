package rpg.presentation.menu;

import rpg.domain.player.Player;

public class InventoryMenu {
  /**
   * 인벤토리 메뉴를 표시합니다.
   */
  public void displayInventoryMenu(Player player) {
    System.out.println("\n=== 인벤토리 관리 ===");
    System.out.println("1. 🧪 아이템 사용");
    System.out.println("2. ⚔️ 장비 관리");
    System.out.println("3. 📋 아이템 정보");
    System.out.println("4. 📦 인벤토리 정렬");
    System.out.println("5. 📊 인벤토리 통계");
    System.out.println("6. 🔍 장비 비교");
    System.out.println("7. 🔙 돌아가기");
  }

  /**
   * 장비 관리 메뉴를 표시합니다.
   */
  public void displayEquipmentMenu(Player player) {
    System.out.println("\n=== 장비 관리 ===");
    System.out.println("1. ⚔️ 장비 착용");
    System.out.println("2. 📤 장비 해제");
    System.out.println("3. 👁️ 현재 장비 보기");
    System.out.println("4. ⚡ 최적 장비 자동 착용");
    System.out.println("5. 🔙 돌아가기");
  }

  /**
   * 장비 해제 메뉴를 표시합니다.
   */
  public void displayUnequipItemMenu(Player player) {
    System.out.println("\n=== 장비 해제 ===");
    System.out.println("1. ⚔️ 무기 해제");
    System.out.println("2. 🛡️ 방어구 해제");
    System.out.println("3. 💍 장신구 해제");
    System.out.println("4. 🔄 모든 장비 해제");
    System.out.println("5. 🔙 취소");
  }

}
