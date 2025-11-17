package com.hospital.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hospital.entity.YouTubeVideo;

@Repository
public interface YouTubeVideoRepository extends JpaRepository<YouTubeVideo, String> {
	
    
 
 

    
    
    /**
     * 제목 또는 설명에 키워드 포함된 영상 검색
     */
    List<YouTubeVideo> findByTitleContainingOrDescriptionContaining(String titleKeyword, String descriptionKeyword);
    
    
    /**
     * 카테고리별 페이지네이션 조회
     */
    Page<YouTubeVideo> findByDetailCategoryIn(List<String> detailCategory, Pageable pageable);
    
}