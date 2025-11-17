package com.hospital.initializer;

import org.springframework.stereotype.Component;
import com.hospital.service.YouTubeVideoService;
import com.hospital.repository.YouTubeCategoryRepository;
import com.hospital.entity.YouTubeCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class YouTubeDataInitializer {
    
    private final YouTubeVideoService youTubeVideoService;
    private final YouTubeCategoryRepository categoryRepository;
    
    // 검색어 패턴 정의
    private static final String[] DEPT_PATTERNS = {
        "전문의 질환 설명",
        "의사 진료 항목"
    };
    
    private static final String[] DISEASE_PATTERNS = {
        "의사 설명",
        "전문의 진단"
    };
    
    private static final String[] AGE_PATTERNS = {
        "건강검진 항목",
        "의사 건강 조언"
    };
    
    private static final String[] SEASON_PATTERNS = {
        "의사 추천 건강관리",
        "병원 예방접종"
    };
    
    private static final String[] EMERGENCY_KEYWORDS = {
        "심폐소생술 CPR",
        "하임리히법 기도폐쇄",
        "지혈 방법",
        "화상 응급처치",
        "뇌졸중 골든타임",
        "심근경색 전조증상",
        "골절 응급처치",
        "알레르기 쇼크",
        "119 신고 요령",
        "응급실 가야할 증상"
    };
    
    private static final String[] TRUSTED_CHANNELS = {
        "세브란스병원 공식 채널",
        "서울대학교병원 의학정보",
        "삼성서울병원 건강정보",
        "서울아산병원 질환백과"
    };
    
    /**
     * YouTube 영상 데이터 초기화
     */
    public void initializeVideoData() {
        log.info("========================================");
        log.info("YouTube 영상 데이터 초기화 시작");
        log.info("========================================");
        
        int totalCount = 0;
        
        try {
            // 1. 인증된 채널 수집
            log.info("[1/6] 인증된 채널 수집 시작");
            totalCount += collectTrustedChannels();
            
            // 2. 진료과별 수집
            log.info("[2/6] 진료과별 수집 시작");
            totalCount += collectByDepartments();
            
            // 3. 질환별 수집
            log.info("[3/6] 질환별 수집 시작");
            totalCount += collectByDiseases();
            
            // 4. 연령별 수집
            log.info("[4/6] 연령별 수집 시작");
            totalCount += collectByAgeGroups();
            
            // 5. 계절별 수집
            log.info("[5/6] 계절별 수집 시작");
            totalCount += collectBySeason();
            
            // 6. 응급 상황 수집
            log.info("[6/6] 응급 상황 수집 시작");
            totalCount += collectEmergency();
            
            log.info("========================================");
            log.info("YouTube 영상 데이터 초기화 완료: 총 {}개", totalCount);
            log.info("카테고리별 분포:");
            log.info("  - 진료과: ~150개 (30%)");
            log.info("  - 질환: ~150개 (30%)");
            log.info("  - 연령: ~80개 (16%)");
            log.info("  - 계절: ~50개 (10%)");
            log.info("  - 응급: ~50개 (10%)");
            log.info("  - 인증채널: ~32개 (6%)");
            log.info("========================================");
            
        } catch (Exception e) {
            log.error("초기화 중 오류 발생", e);
            throw new RuntimeException("YouTube 데이터 초기화 실패", e);
        }
    }
    
    /**
     * 1. 인증된 채널 수집 (4개)
     */
    private int collectTrustedChannels() {
        int count = 0;
        
        for (String channelKeyword : TRUSTED_CHANNELS) {
            log.info("  → 수집: {}", channelKeyword);
            count += youTubeVideoService.fetchAndSaveVideos(
                channelKeyword, 
                50,  // 채널당 8개
                "인증된 채널",
                "인증된 채널"
            );
            sleep(1000);
        }
        
        log.info("  ✓ 인증된 채널 수집 완료: {}개", count);
        return count;
    }
    
    /**
     * 2. 진료과별 수집 (50개 × 2패턴 × 5개 = 150개)
     */
    private int collectByDepartments() {
        int count = 0;
        
        YouTubeCategory deptCategory = categoryRepository
            .findByMainCategory("진료과")
            .orElseThrow(() -> new RuntimeException("진료과 카테고리를 찾을 수 없습니다"));
        
        List<String> departments = Arrays.asList(
            deptCategory.getDetailCategory().split(",")
        );
        
        for (String dept : departments) {
            dept = dept.trim();
            log.info("  → 진료과: {}", dept);
            
            for (String pattern : DEPT_PATTERNS) {
                String query = dept + " " + pattern;
                log.info("    - 검색: {}", query);
                
                count += youTubeVideoService.fetchAndSaveVideos(
                    query,
                    50,  // 패턴당 5개
                    "진료과",
                    dept
                );
                sleep(1000);
            }
        }
        
        log.info("  ✓ 진료과별 수집 완료: {}개", count);
        return count;
    }
    
    /**
     * 3. 질환별 수집 (50개 × 2패턴 × 5개 = 150개)
     */
    private int collectByDiseases() {
        int count = 0;
        
        YouTubeCategory diseaseCategory = categoryRepository
            .findByMainCategory("질환")
            .orElseThrow(() -> new RuntimeException("질환 카테고리를 찾을 수 없습니다"));
        
        List<String> diseases = Arrays.asList(
            diseaseCategory.getDetailCategory().split(",")
        );
        
        for (String disease : diseases) {
            disease = disease.trim();
            log.info("  → 질환: {}", disease);
            
            for (String pattern : DISEASE_PATTERNS) {
                String query = disease + " " + pattern;
                log.info("    - 검색: {}", query);
                
                count += youTubeVideoService.fetchAndSaveVideos(
                    query,
                    50,  // 패턴당 5개
                    "질환",
                    disease
                );
                sleep(1000);
            }
        }
        
        log.info("  ✓ 질환별 수집 완료: {}개", count);
        return count;
    }
    
    /**
     * 4. 연령별 수집 (4개 × 2패턴 × 50개 = 80개)
     */
    private int collectByAgeGroups() {
        int count = 0;
        
        YouTubeCategory ageCategory = categoryRepository
            .findByMainCategory("연령")
            .orElseThrow(() -> new RuntimeException("연령 카테고리를 찾을 수 없습니다"));
        
        List<String> ageGroups = Arrays.asList(
            ageCategory.getDetailCategory().split(",")
        );
        
        for (String age : ageGroups) {
            age = age.trim();
            log.info("  → 연령: {}", age);
            
            for (String pattern : AGE_PATTERNS) {
                String query = age + " " + pattern;
                log.info("    - 검색: {}", query);
                
                count += youTubeVideoService.fetchAndSaveVideos(
                    query,
                    50,  // 패턴당 10개
                    "연령",
                    age
                );
                sleep(1000);
            }
        }
        
        log.info("  ✓ 연령별 수집 완료: {}개", count);
        return count;
    }
    
    /**
     * 5. 계절별 수집 (2패턴 × 25개 = 50개)
     */
    private int collectBySeason() {
        int count = 0;
        
        String currentSeason = getCurrentSeason();
        log.info("  → 현재 계절: {}", currentSeason);
        
        for (String pattern : SEASON_PATTERNS) {
            String query = currentSeason + "철 " + pattern;
            log.info("    - 검색: {}", query);
            
            count += youTubeVideoService.fetchAndSaveVideos(
                query,
                50,  // 패턴당 50개
                "계절",
                "계절"
            );
            sleep(1000);
        }
        
        log.info("  ✓ 계절별 수집 완료: {}개", count);
        return count;
    }
    
    /**
     * 6. 응급 상황 수집 (10개 × 5개 = 50개)
     */
    private int collectEmergency() {
        int count = 0;
        
        for (String keyword : EMERGENCY_KEYWORDS) {
            log.info("  → 수집: {}", keyword);
            
            count += youTubeVideoService.fetchAndSaveVideos(
                keyword,
                50,  // 키워드당 5개
                "응급",
                "응급"  
            );
            sleep(1000);
        }
        
        log.info("  ✓ 응급 상황 수집 완료: {}개", count);
        return count;
    }
    
    /**
     * 현재 계절 판단
     */
    private String getCurrentSeason() {
        int month = LocalDate.now().getMonthValue();
        
        if (month >= 3 && month <= 5) return "봄";
        if (month >= 6 && month <= 8) return "여름";
        if (month >= 9 && month <= 11) return "가을";
        return "겨울";
    }
    
    /**
     * 재초기화 (기존 데이터 삭제 후 재수집)
     */
    public void reinitializeVideoData() {
        log.info("========================================");
        log.info("YouTube 영상 데이터 재초기화 시작");
        log.info("기존 데이터를 모두 삭제합니다");
        log.info("========================================");
        
        youTubeVideoService.deleteAllVideos();
        initializeVideoData();
    }
    
    /**
     * API 호출 제한 방지를 위한 대기
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sleep 인터럽트 발생");
        }
    }
}