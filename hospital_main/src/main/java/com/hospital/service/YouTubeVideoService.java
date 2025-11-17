package com.hospital.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hospital.caller.YouTubeApiCaller;
import com.hospital.entity.YouTubeCategory;
import com.hospital.entity.YouTubeVideo;
import com.hospital.parser.YouTubeVideoParser;
import com.hospital.repository.YouTubeCategoryRepository;
import com.hospital.repository.YouTubeVideoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class YouTubeVideoService {

	private final YouTubeApiCaller youTubeApiCaller;
	private final YouTubeVideoParser youTubeVideoParser;
	private final YouTubeVideoRepository youTubeVideoRepository;
	private final YouTubeCategoryRepository youTubeCategoryRepository;

	// ==================== API 호출 + 저장 ====================

	/**
	 * YouTube API 호출 -> 파싱 -> DB 저장
	 */
	@Transactional
	public int fetchAndSaveVideos(String query, int maxResults, String mainCategory, String detailCategory) {
		try {
			log.info("[{}] 영상 검색 시작 - 검색어: {}, maxResults: {}", mainCategory, query, maxResults);

			// 1. API 호출
			String jsonResponse = youTubeApiCaller.searchMedicalVideos(query, maxResults);

			// 2. 파싱
			List<YouTubeVideo> videos = youTubeVideoParser.parseToEntities(jsonResponse, mainCategory,  detailCategory );

			if (videos.isEmpty()) {
				log.warn("[{}] 파싱된 영상 없음 - 검색어: {}", mainCategory, query);
				return 0;
			}

			// 3. 중복 제거 후 저장
			int savedCount = 0;
			for (YouTubeVideo video : videos) {
				if (!youTubeVideoRepository.existsById(video.getVideoId())) {
					youTubeVideoRepository.save(video);
					savedCount++;
				} else {
					log.debug("중복 영상 스킵: {}", video.getVideoId());
				}
			}

			log.info("[{}] 영상 저장 완료 - 검색어: {}, 저장: {}/{}", mainCategory, query, savedCount, videos.size());

			return savedCount;

		} catch (Exception e) {
			log.error("[{}] 영상 저장 실패 - 검색어: {}, 오류: {}", mainCategory, query, e.getMessage(), e);
			return 0;
		}
	}

	@Transactional(readOnly = true)
	public Page<YouTubeVideo> getVideosByPage(List<String> detailCategory, Pageable pageable) {
		if (detailCategory != null && !detailCategory.isEmpty()) {
			return youTubeVideoRepository.findByDetailCategoryIn(detailCategory, pageable);
		}
		return youTubeVideoRepository.findAll(pageable);
	} 
	
	
	public List<Map<String, Object>> getCategories() {
	    List<YouTubeCategory> list = youTubeCategoryRepository.findAll();

	    return list.stream()
	        .map(cat -> {
	            Map<String, Object> map = new HashMap<>();
	            map.put("mainCategory", cat.getMainCategory());
	            
	            String detail = cat.getDetailCategory();
	            List<String> detailList = (detail != null && !detail.isBlank())
	                    ? Arrays.asList(detail.split(","))
	                    : List.of(); // 빈 리스트 반환

	            map.put("detailCategories", detailList);
	           
	            return map;
	        })
	        .collect(Collectors.toList());
	}
	/**
	 * 모든 영상 삭제
	 */
	@Transactional
	public void deleteAllVideos() {
		youTubeVideoRepository.deleteAll();
		log.info("모든 영상 삭제 완료");
	}

}