package rpg.domain.shop;

import java.util.Map;

/**
 * 카테고리별 아이템 수를 담는 간단한 데이터 클래스
 */
public class CategoryStats {
  private final Map<ShopItemCategory, Integer> stats;

  public CategoryStats() {
    this.stats = new java.util.HashMap<>();
  }

  public void setCount(ShopItemCategory category, int count) {
    stats.put(category, count);
  }

  public int getCount(ShopItemCategory category) {
    return stats.getOrDefault(category, 0);
  }
}
