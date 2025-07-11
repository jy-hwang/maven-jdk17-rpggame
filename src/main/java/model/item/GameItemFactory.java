package model.item;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import model.effect.GameEffect;

/**
 * 효과 시스템을 지원하는 개선된 아이템 팩토리
 */
public class GameItemFactory {
    private static final Logger logger = LoggerFactory.getLogger(GameItemFactory.class);
    
    private static final Map<String, GameItemData> itemDatabase = new ConcurrentHashMap<>();
    private static GameItemFactory instance;
    
    private GameItemFactory() {}
    
    public static GameItemFactory getInstance() {
        if (instance == null) {
            synchronized (GameItemFactory.class) {
                if (instance == null) {
                    instance = new GameItemFactory();
                }
            }
        }
        return instance;
    }
    
    /**
     * 아이템 데이터베이스 초기화
     */
    public void initializeItemDatabase(Map<String, GameItemData> items) {
        itemDatabase.clear();
        itemDatabase.putAll(items);
        logger.info("아이템 데이터베이스 초기화 완료: {}개 아이템", items.size());
        
        // 효과가 있는 아이템 개수 로깅
        long effectItemCount = items.values().stream()
                .filter(item -> !item.getEffects().isEmpty())
                .count();
        logger.info("효과가 있는 아이템: {}개", effectItemCount);
    }
    
    /**
     * 아이템 ID로 아이템 생성
     */
    public GameItem createItem(String itemId) {
      GameItemData data = itemDatabase.get(itemId);
        if (data == null) {
            logger.error("알 수 없는 아이템 ID: {}", itemId);
            throw new IllegalArgumentException("알 수 없는 아이템 ID: " + itemId);
        }
        
        return createItemFromData(data);
    }
    
    /**
     * 아이템 데이터로부터 실제 아이템 객체 생성
     */
    private GameItem createItemFromData(GameItemData data) {
      GameItemStats stats = data.getStats();
        
        return switch (data.getType().toUpperCase()) {
            case "WEAPON" -> new GameEquipment(
                data.getName(),
                data.getDescription(),
                data.getValue(),
                data.getRarity(),
                GameEquipment.EquipmentType.WEAPON,
                stats.getAttackBonus(),
                stats.getDefenseBonus(),
                stats.getHpBonus()
            );
            
            case "ARMOR" -> new GameEquipment(
                data.getName(),
                data.getDescription(),
                data.getValue(),
                data.getRarity(),
                GameEquipment.EquipmentType.ARMOR,
                stats.getAttackBonus(),
                stats.getDefenseBonus(),
                stats.getHpBonus()
            );
            
            case "ACCESSORY" -> new GameEquipment(
                data.getName(),
                data.getDescription(),
                data.getValue(),
                data.getRarity(),
                GameEquipment.EquipmentType.ACCESSORY,
                stats.getAttackBonus(),
                stats.getDefenseBonus(),
                stats.getHpBonus()
            );
            
            case "CONSUMABLE" -> createConsumableItem(data);
            
            default -> throw new IllegalArgumentException("알 수 없는 아이템 타입: " + data.getType());
        };
    }
    
    /**
     * 소비 아이템 생성 (효과 시스템 적용)
     */
    private GameConsumable createConsumableItem(GameItemData data) {
        // 효과 리스트 생성
        List<GameEffect> effects = GameEffectFactory.createEffects(data.getEffects());
        
        // 레거시 스탯도 효과로 변환 (하위 호환성)
        GameItemStats stats = data.getStats();
        if (stats.getHpRestore() > 0) {
            effects.add(GameEffectFactory.createSimpleEffect("HEAL_HP", stats.getHpRestore()));
        }
        if (stats.getExpGain() > 0) {
            effects.add(GameEffectFactory.createSimpleEffect("GAIN_EXP", stats.getExpGain()));
        }
        
        return new GameConsumable(
            data.getName(),
            data.getDescription(),
            data.getValue(),
            data.getRarity(),
            effects,
            stats.isStackable(),
            stats.getCooldown()
        );
    }
    
    /**
     * 특정 효과를 가진 아이템 검색
     */
    public List<String> findItemsByEffect(String effectType) {
        return itemDatabase.values().stream()
                .filter(data -> data.getEffects().stream()
                        .anyMatch(effect -> effect.getType().equalsIgnoreCase(effectType)))
                .map(GameItemData::getId)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 레벨 범위의 회복 아이템 생성
     */
    public GameItem createHealingItemForLevel(int level) {
        List<String> healingItems = findItemsByEffect("HEAL_HP");
        
        return healingItems.stream()
                .map(itemDatabase::get)
                .filter(data -> data.getRequirements().getMinLevel() <= level)
                .filter(data -> data.getRequirements().getMinLevel() >= level - 5)
                .max((a, b) -> Integer.compare(
                    a.getRequirements().getMinLevel(), 
                    b.getRequirements().getMinLevel()))
                .map(this::createItemFromData)
                .orElse(createItem("HEALTH_POTION")); // 기본 아이템
    }
    
    /**
     * 상태이상 치료 아이템 검색
     */
    public List<String> findCureItems(String statusType) {
        return itemDatabase.values().stream()
                .filter(data -> data.getEffects().stream()
                        .anyMatch(effect -> 
                            effect.getType().equals("CURE_STATUS") && 
                            statusType.equalsIgnoreCase(effect.getStatusType())))
                .map(GameItemData::getId)
                .collect(Collectors.toList());
    }
    
    /**
     * 전투 중 사용 가능한 아이템 필터링
     */
    public List<String> getCombatUsableItems() {
        return itemDatabase.values().stream()
                .filter(data -> data.getType().equalsIgnoreCase("CONSUMABLE"))
                .filter(data -> data.getStats().isCombatUsable())
                .map(GameItemData::getId)
                .collect(Collectors.toList());
    }
    
    /**
     * 아이템 효과 정보 출력 (디버깅용)
     */
    public void printItemEffects(String itemId) {
      GameItemData data = itemDatabase.get(itemId);
        if (data != null) {
            System.out.println("=== " + data.getName() + " 효과 정보 ===");
            
            if (data.getEffects().isEmpty()) {
                System.out.println("효과 없음");
            } else {
                for (GameEffectData effect : data.getEffects()) {
                    System.out.printf("• %s", effect.getType());
                    if (effect.getValue() != null) {
                        System.out.printf(" (%d", effect.getValue());
                        if (effect.getIsPercentage() != null && effect.getIsPercentage()) {
                            System.out.print("%");
                        }
                        System.out.print(")");
                    }
                    if (effect.getStatusType() != null) {
                        System.out.printf(" - %s", effect.getStatusType());
                    }
                    System.out.println();
                }
            }
            
            GameItemStats stats = data.getStats();
            if (stats.getCooldown() > 0) {
                System.out.println("쿨다운: " + stats.getCooldown() + "턴");
            }
            if (stats.isCombatUsable()) {
                System.out.println("전투 중 사용 가능");
            }
            
            System.out.println("===================");
        }
    }
    
    // 기존 메서드들...
    public boolean itemExists(String itemId) {
        return itemDatabase.containsKey(itemId);
    }
    
    public java.util.Set<String> getAllItemIds() {
        return itemDatabase.keySet();
    }
    
    public List<String> getItemsByType(String type) {
        return itemDatabase.values().stream()
                .filter(data -> data.getType().equalsIgnoreCase(type))
                .map(GameItemData::getId)
                .toList();
    }
    
    public GameItem createRandomItemForLevel(int level) {
        return itemDatabase.values().stream()
                .filter(data -> data.getRequirements().getMinLevel() <= level)
                .filter(data -> data.getRequirements().getMinLevel() >= level - 5)
                .skip((int) (itemDatabase.size() * Math.random()))
                .findFirst()
                .map(this::createItemFromData)
                .orElse(createItem("HEALTH_POTION"));
    }
    
    public GameItem createRandomItemByRarity(GameItem.ItemRarity rarity) {
        return itemDatabase.values().stream()
                .filter(data -> data.getRarity() == rarity)
                .skip((int) (itemDatabase.values().stream()
                        .filter(data -> data.getRarity() == rarity)
                        .count() * Math.random()))
                .findFirst()
                .map(this::createItemFromData)
                .orElse(createItem("HEALTH_POTION"));
    }
}