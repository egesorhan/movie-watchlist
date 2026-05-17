package com.moviewatchlist.repository;

import com.moviewatchlist.entity.TvWatchlistEntry;
import com.moviewatchlist.entity.TvWatchlistEntry.WatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TvWatchlistRepository extends JpaRepository<TvWatchlistEntry, Long> {

    Optional<TvWatchlistEntry> findByTvShowId(Long tvShowId);

    List<TvWatchlistEntry> findAllByStatusOrderByAddedAtDesc(WatchStatus status);

    List<TvWatchlistEntry> findAllByOrderByAddedAtDesc();

    @Query("SELECT DISTINCT w.tvShow.genres FROM TvWatchlistEntry w WHERE w.userRating >= :minRating")
    List<String> findGenresOfHighlyRatedShows(@Param("minRating") Integer minRating);

    @Query("SELECT w FROM TvWatchlistEntry w WHERE w.status = :status ORDER BY " +
           "CASE WHEN :sortBy = 'rating' THEN w.userRating END DESC, " +
           "CASE WHEN :sortBy = 'added' THEN w.addedAt END DESC, " +
           "CASE WHEN :sortBy = 'title' THEN w.tvShow.name END ASC")
    List<TvWatchlistEntry> findByStatusWithSort(@Param("status") WatchStatus status, @Param("sortBy") String sortBy);
}
