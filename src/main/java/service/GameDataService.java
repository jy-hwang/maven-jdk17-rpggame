package service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import model.GameCharacter;

/**
 * 다중 슬롯 JSON 저장 시스템
 */
public class GameDataService {
    private static final Logger logger = LoggerFactory.getLogger(GameDataService.class);
    
    private static final String SAVE_DIRECTORY = "save";
    private static final String SAVE_FILE_PREFIX = "rpg_save_slot";
    private static final String BACKUP_PREFIX = "rpg_save_backup_";
    private static final int MAX_SAVE_SLOTS = 5; // 최대 저장 슬롯 수
    private static final ObjectMapper objectMapper;
    
    static {
        // ObjectMapper 전역 설정
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
        createSaveDirectory();
    }
    
    /**
     * 저장 디렉토리를 생성합니다.
     */
    private static void createSaveDirectory() {
        try {
            Path saveDir = Paths.get(SAVE_DIRECTORY);
            if (!Files.exists(saveDir)) {
                Files.createDirectories(saveDir);
                logger.info("저장 디렉토리 생성: {}", saveDir.toAbsolutePath());
            }
        } catch (Exception e) {
            logger.error("저장 디렉토리 생성 실패", e);
        }
    }
    
    /**
     * 지정된 슬롯에 게임을 저장합니다.
     */
    public static void saveGame(GameCharacter character, GameState gameState, int slotNumber) throws GameDataException {
        if (character == null) {
            throw new GameDataException("저장할 캐릭터 데이터가 없습니다.");
        }
        
        if (slotNumber < 1 || slotNumber > MAX_SAVE_SLOTS) {
            throw new GameDataException("저장 슬롯 번호는 1~" + MAX_SAVE_SLOTS + " 사이여야 합니다.");
        }
        
        String fileName = SAVE_FILE_PREFIX + slotNumber + ".json";
        Path saveFilePath = Paths.get(SAVE_DIRECTORY, fileName);
        
        try {
            // 기존 파일이 있으면 백업 생성
            if (Files.exists(saveFilePath)) {
                createJsonBackup(saveFilePath, slotNumber);
            }
            
            // 저장할 데이터 구성
            SaveData saveData = new SaveData(character, gameState, slotNumber);
            
            // JSON 파일로 저장
            objectMapper.writeValue(saveFilePath.toFile(), saveData);
            
            logger.info("슬롯 {} JSON 게임 저장 완료: {} (캐릭터: {})", 
                       slotNumber, saveFilePath.toAbsolutePath(), character.getName());
            System.out.println("게임이 슬롯 " + slotNumber + "에 저장되었습니다!");
            
        } catch (IOException e) {
            logger.error("슬롯 {} JSON 게임 저장 실패: {}", slotNumber, saveFilePath, e);
            throw new GameDataException("게임 저장 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 지정된 슬롯에서 게임을 불러옵니다.
     */
    public static SaveData loadGame(int slotNumber) throws GameDataException {
        if (slotNumber < 1 || slotNumber > MAX_SAVE_SLOTS) {
            throw new GameDataException("저장 슬롯 번호는 1~" + MAX_SAVE_SLOTS + " 사이여야 합니다.");
        }
        
        String fileName = SAVE_FILE_PREFIX + slotNumber + ".json";
        Path saveFilePath = Paths.get(SAVE_DIRECTORY, fileName);
        
        if (!Files.exists(saveFilePath)) {
            logger.info("슬롯 {} 저장 파일이 존재하지 않음: {}", slotNumber, saveFilePath);
            return null;
        }
        
        try {
            SaveData saveData = objectMapper.readValue(saveFilePath.toFile(), SaveData.class);
            logger.info("슬롯 {} JSON 게임 로드 완료: {} (캐릭터: {})", 
                       slotNumber, saveFilePath, saveData.getCharacter().getName());
            return saveData;
            
        } catch (IOException e) {
            logger.error("슬롯 {} JSON 파일 읽기 오류: {}", slotNumber, saveFilePath, e);
            throw new GameDataException("저장 파일을 읽는 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 모든 저장 슬롯의 정보를 조회합니다.
     */
    public static List<SaveSlotInfo> getAllSaveSlots() {
        List<SaveSlotInfo> slots = new ArrayList<>();
        
        for (int i = 1; i <= MAX_SAVE_SLOTS; i++) {
            String fileName = SAVE_FILE_PREFIX + i + ".json";
            Path saveFilePath = Paths.get(SAVE_DIRECTORY, fileName);
            
            if (Files.exists(saveFilePath)) {
                try {
                    SaveData saveData = objectMapper.readValue(saveFilePath.toFile(), SaveData.class);
                    logger.debug("saveData : ", saveData);
                    SaveSlotInfo slotInfo = new SaveSlotInfo(
                        i,
                        true,
                        saveData.getCharacter().getName(),
                        saveData.getCharacter().getLevel(),
                        saveData.getSaveTime(),
                        saveData.getGameState().getTotalPlayTime()
                    );
                    slots.add(slotInfo);
                } catch (IOException e) {
                    logger.warn("슬롯 {} 정보 읽기 실패: {}", i, e.getMessage());
                    slots.add(new SaveSlotInfo(i, false, "오류", 0, "알 수 없음", 0));
                }
            } else {
                slots.add(new SaveSlotInfo(i, false, null, 0, null, 0));
            }
        }
        
        return slots;
    }
    
    /**
     * 저장 슬롯 정보를 표시합니다.
     */
    public static void displaySaveSlots() {
        List<SaveSlotInfo> slots = getAllSaveSlots();
        
        System.out.println("\n=== 저장 슬롯 목록 ===");
        for (SaveSlotInfo slot : slots) {
            if (slot.isOccupied()) {
                System.out.printf("%d. %s (레벨 %d) - %s (플레이 시간: %d분)%n",
                    slot.getSlotNumber(),
                    slot.getCharacterName(),
                    slot.getCharacterLevel(),
                    slot.getSaveTime(),
                    slot.getPlayTime()
                );
            } else {
                System.out.printf("%d. [빈 슬롯]%n", slot.getSlotNumber());
            }
        }
        System.out.println("==================");
    }
    
    /**
     * 지정된 슬롯을 삭제합니다.
     */
    public static boolean deleteSaveSlot(int slotNumber) {
        if (slotNumber < 1 || slotNumber > MAX_SAVE_SLOTS) {
            System.out.println("잘못된 슬롯 번호입니다.");
            return false;
        }
        
        String fileName = SAVE_FILE_PREFIX + slotNumber + ".json";
        Path saveFilePath = Paths.get(SAVE_DIRECTORY, fileName);
        
        try {
            if (Files.exists(saveFilePath)) {
                // 삭제 전 백업 생성
                createJsonBackup(saveFilePath, slotNumber);
                
                boolean deleted = Files.deleteIfExists(saveFilePath);
                if (deleted) {
                    logger.info("슬롯 {} 저장 파일 삭제 완료: {}", slotNumber, saveFilePath);
                    System.out.println("슬롯 " + slotNumber + "가 삭제되었습니다.");
                } else {
                    logger.warn("슬롯 {} 저장 파일 삭제 실패: {}", slotNumber, saveFilePath);
                }
                return deleted;
            } else {
                System.out.println("슬롯 " + slotNumber + "는 이미 비어있습니다.");
                return true;
            }
        } catch (IOException e) {
            logger.error("슬롯 {} 저장 파일 삭제 중 오류: {}", slotNumber, saveFilePath, e);
            System.out.println("저장 파일 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * JSON 백업 파일을 생성합니다.
     */
    private static void createJsonBackup(Path originalFile, int slotNumber) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = BACKUP_PREFIX + "slot" + slotNumber + "_" + timestamp + ".json";
            Path backupPath = Paths.get(SAVE_DIRECTORY, backupFileName);
            
            Files.copy(originalFile, backupPath);
            logger.debug("슬롯 {} JSON 백업 파일 생성: {}", slotNumber, backupPath);
            
            cleanupOldJsonBackups();
            
        } catch (IOException e) {
            logger.warn("슬롯 {} JSON 백업 파일 생성 실패: {}", slotNumber, originalFile, e);
        }
    }
    
    /**
     * 오래된 JSON 백업 파일들을 정리합니다.
     */
    private static void cleanupOldJsonBackups() {
        try {
            Path saveDir = Paths.get(SAVE_DIRECTORY);
            Files.list(saveDir)
                .filter(path -> path.getFileName().toString().startsWith(BACKUP_PREFIX))
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .sorted((p1, p2) -> p2.getFileName().toString().compareTo(p1.getFileName().toString()))
                .skip(10) // 최신 10개는 유지 (슬롯 5개 × 백업 2개)
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                        logger.debug("오래된 JSON 백업 파일 삭제: {}", path);
                    } catch (IOException e) {
                        logger.warn("JSON 백업 파일 삭제 실패: {}", path, e);
                    }
                });
        } catch (IOException e) {
            logger.warn("JSON 백업 파일 정리 중 오류", e);
        }
    }
    
    /**
     * 저장 슬롯 정보 클래스
     */
    public static class SaveSlotInfo {
        private final int slotNumber;
        private final boolean occupied;
        private final String characterName;
        private final int characterLevel;
        private final String saveTime;
        private final int playTime;
        
        public SaveSlotInfo(int slotNumber, boolean occupied, String characterName, 
                           int characterLevel, String saveTime, int playTime) {
            this.slotNumber = slotNumber;
            this.occupied = occupied;
            this.characterName = characterName;
            this.characterLevel = characterLevel;
            this.saveTime = saveTime;
            this.playTime = playTime;
        }
        
        // Getters
        public int getSlotNumber() { return slotNumber; }
        public boolean isOccupied() { return occupied; }
        public String getCharacterName() { return characterName; }
        public int getCharacterLevel() { return characterLevel; }
        public String getSaveTime() { return saveTime; }
        public int getPlayTime() { return playTime; }
    }
    
    /**
     * 저장 데이터를 래핑하는 클래스 (슬롯 번호 추가)
     */
    public static class SaveData {
        private GameCharacter character;
        private GameState gameState;
        private String saveTime;
        private String version;
        private int slotNumber;
        
        public SaveData() {}
        
        public SaveData(GameCharacter character, GameState gameState, int slotNumber) {
            this.character = character;
            this.gameState = gameState != null ? gameState : new GameState();
            this.saveTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            this.version = "2.0.0";
            this.slotNumber = slotNumber;
        }
        
        // Getters and Setters
        public GameCharacter getCharacter() { return character; }
        public void setCharacter(GameCharacter character) { this.character = character; }
        
        public GameState getGameState() { return gameState; }
        public void setGameState(GameState gameState) { this.gameState = gameState; }
        
        public String getSaveTime() { return saveTime; }
        public void setSaveTime(String saveTime) { this.saveTime = saveTime; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public int getSlotNumber() { return slotNumber; }
        public void setSlotNumber(int slotNumber) { this.slotNumber = slotNumber; }
    }
    
    /**
     * 게임 상태 정보를 담는 클래스
     */
    public static class GameState {
        private int totalPlayTime;
        private int monstersKilled;
        private int questsCompleted;
        private String currentLocation;
        
        public GameState() {
            this.totalPlayTime = 0;
            this.monstersKilled = 0;
            this.questsCompleted = 0;
            this.currentLocation = "마을";
        }
        
        public void addPlayTime(int minutes) {
            this.totalPlayTime += minutes;
        }
        
        public void incrementMonstersKilled() {
            this.monstersKilled++;
        }
        
        public void incrementQuestsCompleted() {
            this.questsCompleted++;
        }
        
        public void displayGameStats() {
            System.out.println("\n=== 게임 통계 ===");
            System.out.println("총 플레이 시간: " + totalPlayTime + "분");
            System.out.println("처치한 몬스터: " + monstersKilled + "마리");
            System.out.println("완료한 퀘스트: " + questsCompleted + "개");
            System.out.println("현재 위치: " + currentLocation);
            System.out.println("================");
        }
        
        // Getters and Setters
        public int getTotalPlayTime() { return totalPlayTime; }
        public void setTotalPlayTime(int totalPlayTime) { this.totalPlayTime = totalPlayTime; }
        
        public int getMonstersKilled() { return monstersKilled; }
        public void setMonstersKilled(int monstersKilled) { this.monstersKilled = monstersKilled; }
        
        public int getQuestsCompleted() { return questsCompleted; }
        public void setQuestsCompleted(int questsCompleted) { this.questsCompleted = questsCompleted; }
        
        public String getCurrentLocation() { return currentLocation; }
        public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }
    }
    
    /**
     * 게임 데이터 관련 예외 클래스
     */
    public static class GameDataException extends Exception {
        public GameDataException(String message) {
            super(message);
        }
        
        public GameDataException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    // 상수 getter
    public static int getMaxSaveSlots() { return MAX_SAVE_SLOTS; }
}