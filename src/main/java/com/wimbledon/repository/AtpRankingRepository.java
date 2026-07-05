package com.wimbledon.repository;

import com.wimbledon.entity.AtpRanking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AtpRankingRepository extends JpaRepository<AtpRanking, Long> {

    List<AtpRanking> findAllByOrderByPointsDescIdAsc();

    long count();
}