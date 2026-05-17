package com.moviewatchlist.repository;

import com.moviewatchlist.entity.TvShow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TvShowRepository extends JpaRepository<TvShow, Long> {
    Optional<TvShow> findByTmdbId(Integer tmdbId);
}
