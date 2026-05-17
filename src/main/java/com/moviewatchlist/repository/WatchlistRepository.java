package com.moviewatchlist.repository;

import com.moviewatchlist.entity.WatchlistEntry;
import com.moviewatchlist.entity.WatchlistEntry.WatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistRepository extends JpaRepository<WatchlistEntry, Long> {

    Optional<WatchlistEntry> findByMovieId(Long movieId);

    List<WatchlistEntry> findAllByStatusOrderByAddedAtDesc(WatchStatus status);

    List<WatchlistEntry> findAllByOrderByAddedAtDesc();

    List<WatchlistEntry> findAllByUserRatingGreaterThanEqualOrderByUserRatingDesc(Integer rating);

    @Query("SELECT DISTINCT w.movie.genres FROM WatchlistEntry w WHERE w.userRating >= :minRating")
    List<String> findGenresOfHighlyRatedMovies(@Param("minRating") Integer minRating);

    @Query("SELECT w FROM WatchlistEntry w WHERE w.status = :status ORDER BY " +
           "CASE WHEN :sortBy = 'rating' THEN w.userRating END DESC, " +
           "CASE WHEN :sortBy = 'added' THEN w.addedAt END DESC, " +
           "CASE WHEN :sortBy = 'title' THEN w.movie.title END ASC")
    List<WatchlistEntry> findByStatusWithSort(@Param("status") WatchStatus status, @Param("sortBy") String sortBy);
}
