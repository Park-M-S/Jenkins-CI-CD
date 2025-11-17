package com.hospital.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hospital.entity.YouTubeCategory;

public interface YouTubeCategoryRepository extends JpaRepository<YouTubeCategory, String>{
	
	List<YouTubeCategory> findAll();
	
	Optional<YouTubeCategory> findByMainCategory(String mainCategory);
	
}