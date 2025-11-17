package com.hospital.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.async.EmergencyAsyncRunner;
import com.hospital.dto.EmergencyWebResponse;
import com.hospital.entity.HospitalMain;
import com.hospital.repository.HospitalMainApiRepository;
import com.hospital.websocket.EmergencyApiWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class EmergencyApiService {

    private final EmergencyAsyncRunner asyncRunner;
    private final EmergencyApiWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;
    private final HospitalMainApiRepository hospitalMainApiRepository;
    private volatile String latestEmergencyJson = null;
    private final AtomicBoolean schedulerRunning = new AtomicBoolean(false);

    // 이전 응급실 데이터를 hpid(병원코드)로 캐싱
    private final Map<String, EmergencyWebResponse> previousDataMap = new HashMap<>();

    @Autowired
    @Lazy
    public EmergencyApiService(EmergencyAsyncRunner asyncRunner,
                              EmergencyApiWebSocketHandler webSocketHandler,
                              HospitalMainApiRepository hospitalMainApiRepository) {
        this.asyncRunner = asyncRunner;
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = new ObjectMapper();
        // null 값 제외 설정 (Map 내부 포함)
        this.objectMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        this.objectMapper.configOverride(java.util.Map.class)
            .setInclude(com.fasterxml.jackson.annotation.JsonInclude.Value.construct(
                com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS,
                com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL));
        this.hospitalMainApiRepository = hospitalMainApiRepository;
    }

    /**
     * WebSocket 연결 시 호출 - 첫 번째 연결이면 스케줄러 시작
     */
    public void onWebSocketConnected() {
        if (schedulerRunning.compareAndSet(false, true)) {
            asyncRunner.runAsyncForAllCities(this::updateCacheFromAsyncResults);
            System.out.println("✅ 응급실 Async 스케줄러 시작 (첫 번째 연결)");
        }
    }

    /**
     * WebSocket 연결 해제 시 호출 - 마지막 연결이면 스케줄러 중지 및 캐시 삭제
     */
    public void onWebSocketDisconnected() {
        if (webSocketHandler.getConnectedSessionCount() == 0) {
            if (schedulerRunning.compareAndSet(true, false)) {
                asyncRunner.stopAsync();
                latestEmergencyJson = null; // 캐시 삭제 (다음 접속 시 최신 데이터 제공)
                previousDataMap.clear(); // 이전 데이터 캐시 초기화
                System.out.println("✅ 응급실 Async 스케줄러 종료 및 캐시 삭제 (마지막 연결 해제)");
            }
        }
    }

    /**
     * Async에서 처리한 DTO 리스트를 캐시에 저장하고 WebSocket으로 브로드캐스트
     */
    public void updateCacheFromAsyncResults(List<EmergencyWebResponse> dtoList) {
        if (!schedulerRunning.get() || dtoList == null || dtoList.isEmpty()) {
            return;
        }

        try {
            // 배치로 좌표 매핑 (한 번의 쿼리로 처리)
            List<EmergencyWebResponse> mappedList = mapCoordinatesBatch(dtoList);

            // 변경 감지 및 타임스탬프 업데이트
            int changedCount = detectChangesAndUpdateTimestamp(mappedList);

            String newJsonData = objectMapper.writeValueAsString(mappedList);

            // 데이터가 변경된 경우에만 브로드캐스트
            if (!newJsonData.equals(latestEmergencyJson)) {
                latestEmergencyJson = newJsonData;
                webSocketHandler.broadcastEmergencyRoomData(newJsonData);
                System.out.println("✅ 응급실 데이터 업데이트 및 브로드캐스트 완료 (매핑: " + mappedList.size() + "건, 변경: " + changedCount + "건)");
            }
        } catch (Exception e) {
            System.err.println("응급실 데이터 처리 중 오류 발생");
            e.printStackTrace();
        }
    }

    /**
     * 응급실 데이터 수집 및 매핑 (컨트롤러용)
     */
    public List<EmergencyWebResponse> fetchAndMapEmergencyData() {
        List<EmergencyWebResponse> emergencyData = new java.util.ArrayList<>();
        asyncRunner.collectAllCitiesData(emergencyData::addAll);
        return mapCoordinatesBatch(emergencyData);
    }

    /**
     * 캐시 없을 때 WebSocket 초기 연결 시 즉시 fetch하여 전송
     */
    public void fetchAndSendInitialData(org.springframework.web.socket.WebSocketSession session) {
        try {
            List<EmergencyWebResponse> freshData = fetchAndMapEmergencyData();
            String jsonData = objectMapper.writeValueAsString(freshData);

            // 캐시 업데이트
            latestEmergencyJson = jsonData;

            // 세션에 전송
            if (session.isOpen()) {
                session.sendMessage(new org.springframework.web.socket.TextMessage(jsonData));
                System.out.println("✅ 최신 데이터 fetch 및 전송 완료: " + session.getId() + " (" + freshData.size() + "건)");
            }
        } catch (Exception e) {
            System.err.println("최신 데이터 fetch 및 전송 실패: " + session.getId());
            e.printStackTrace();
        }
    }

    /**
     * 이전 데이터와 비교하여 변경된 병원을 찾고 타임스탬프 업데이트
     * @return 변경된 병원 수
     */
    private int detectChangesAndUpdateTimestamp(List<EmergencyWebResponse> newDataList) {
        int changedCount = 0;

        for (EmergencyWebResponse newData : newDataList) {
            String hpid = newData.getHpid();
            if (hpid == null) {
                continue;
            }

            EmergencyWebResponse previousData = previousDataMap.get(hpid);

            if (previousData == null) {
                // 신규 병원 - API의 원본 타임스탬프 유지 (이미 UTC로 변환되어 있음)
                // updateTimestampToNow()를 호출하지 않음
                changedCount++;
            } else if (!previousData.equals(newData)) {
                // 데이터가 변경된 병원 - 타임스탬프를 현재 시각으로 업데이트
                newData.updateTimestampToNow();
                changedCount++;
            } else {
                // 변경 없음 - 이전 타임스탬프 유지
                newData.setHvidate(previousData.getHvidate());
            }

            // 현재 데이터를 previousDataMap에 업데이트 (타임스탬프 제외하고 비교하므로 괜찮음)
            previousDataMap.put(hpid, newData);
        }

        return changedCount;
    }

    /**
     * 배치로 좌표 매핑 (성능 최적화 - IN 쿼리)
     */
    private List<EmergencyWebResponse> mapCoordinatesBatch(List<EmergencyWebResponse> dtoList) {
        long startTime = System.currentTimeMillis();
        System.out.println("🔍 매핑 시작 - 응급실 데이터: " + dtoList.size() + "건");

        // 1. 응급실 병원명 추출 (공백 제거)
        long t1 = System.currentTimeMillis();
        List<String> normalizedEmergencyNames = dtoList.stream()
            .map(dto -> dto.getDutyName().replaceAll("\\s+", ""))
            .distinct()
            .collect(Collectors.toList());
        long extractTime = System.currentTimeMillis() - t1;
        System.out.println("✅ [1단계] 응급실 병원명 추출 완료: " + normalizedEmergencyNames.size() + "건 / " + extractTime + "ms");

        // 2. IN 쿼리로 필요한 병원만 조회 (Projection)
        long t2 = System.currentTimeMillis();
        List<Object[]> hospitalProjections = hospitalMainApiRepository.findByNormalizedNamesForCoordinateMapping(normalizedEmergencyNames);
        long queryTime = System.currentTimeMillis() - t2;
        System.out.println("✅ [2단계] DB IN 쿼리 조회 완료: " + hospitalProjections.size() + "건 / " + queryTime + "ms");

        // 3. 공백 제거한 병원명으로 Map 생성 (메모리 매칭)
        long t3 = System.currentTimeMillis();
        Map<String, HospitalCoordinate> hospitalMap = new HashMap<>();
        for (Object[] row : hospitalProjections) {
            String hospitalName = (String) row[0];
            Double coordinateX = (Double) row[1];
            Double coordinateY = (Double) row[2];
            String hospitalAddress = (String) row[3];

            String normalizedName = hospitalName.replaceAll("\\s+", "");
            hospitalMap.putIfAbsent(normalizedName, new HospitalCoordinate(coordinateX, coordinateY, hospitalAddress));
        }
        long mapCreationTime = System.currentTimeMillis() - t3;
        System.out.println("✅ [3단계] Map 생성 완료: " + hospitalMap.size() + "건 / " + mapCreationTime + "ms");

        // 4. 응급실 데이터와 매칭
        long t4 = System.currentTimeMillis();
        List<EmergencyWebResponse> mappedList = dtoList.stream()
            .filter(dto -> {
                String normalizedName = dto.getDutyName().replaceAll("\\s+", "");
                HospitalCoordinate coord = hospitalMap.get(normalizedName);

                if (coord != null) {
                    dto.setCoordinateX(coord.coordinateX);
                    dto.setCoordinateY(coord.coordinateY);
                    dto.setEmergencyAddress(coord.address);
                    return true; // 매핑 성공
                }
                return false; // 매핑 실패 제외
            })
            .collect(Collectors.toList());
        long matchingTime = System.currentTimeMillis() - t4;
        System.out.println("✅ [4단계] 매칭 완료: " + mappedList.size() + "건 / " + matchingTime + "ms");

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("✅ [전체] 배치 매핑 완료: " + totalTime + "ms (추출:" + extractTime + "ms, DB:" + queryTime + "ms, Map:" + mapCreationTime + "ms, 매칭:" + matchingTime + "ms)");

        return mappedList;
    }

    // 내부 클래스: 좌표 정보를 담는 간단한 DTO
    private static class HospitalCoordinate {
        Double coordinateX;
        Double coordinateY;
        String address;

        HospitalCoordinate(Double coordinateX, Double coordinateY, String address) {
            this.coordinateX = coordinateX;
            this.coordinateY = coordinateY;
            this.address = address;
        }
    }

    /**
     * WebSocket 초기 연결 시 캐시 반환
     */
    public JsonNode getEmergencyRoomData() {
        System.out.println("🔍 getEmergencyRoomData() 호출 - latestEmergencyJson null 여부: " + (latestEmergencyJson == null));

        if (latestEmergencyJson == null) {
            System.out.println("⚠️ 캐시 없음 - 빈 ObjectNode 반환");
            return objectMapper.createObjectNode();
        }

        try {
            JsonNode result = objectMapper.readTree(latestEmergencyJson);
            System.out.println("✅ 캐시 반환 - 타입: " + result.getNodeType() + ", 크기: " + result.size());
            return result;
        } catch (Exception e) {
            System.err.println("응급실 데이터 파싱 중 오류 발생");
            e.printStackTrace();
            return objectMapper.createObjectNode();
        }
    }

    /**
     * 스케줄러 강제 중지
     */
    public void stopScheduler() {
        if (schedulerRunning.compareAndSet(true, false)) {
            asyncRunner.stopAsync();
            previousDataMap.clear(); // 이전 데이터 캐시 초기화
            System.out.println("✅ 응급실 스케줄러 강제 중지 완료");
        } else {
            System.out.println("⚠️ 스케줄러가 이미 중지되어 있습니다.");
        }
    }

    /**
     * 서비스 상태 정보 반환
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("schedulerRunning", schedulerRunning.get());
        stats.put("hasLatestData", latestEmergencyJson != null);
        stats.put("lastDataSize", getEmergencyRoomData().size());
        stats.put("connectedSessions", webSocketHandler.getConnectedSessionCount());

        // AsyncRunner에서 통계 가져오기 (있다면)
        stats.put("completedCount", asyncRunner.getCompletedCount());
        stats.put("failedCount", asyncRunner.getFailedCount());
        stats.put("processedCount", asyncRunner.getProcessedCount());

        return stats;
    }

    /**
     * 스케줄러 상태 확인
     */
    public boolean isSchedulerRunning() {
        return schedulerRunning.get();
    }
}
