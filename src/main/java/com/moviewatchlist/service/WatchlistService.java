package com.moviewatchlist.service;

import com.moviewatchlist.dto.TmdbMovieDto;
import com.moviewatchlist.dto.WatchlistRequest;
import com.moviewatchlist.entity.Movie;
import com.moviewatchlist.entity.WatchlistEntry;
import com.moviewatchlist.entity.WatchlistEntry.WatchStatus;
import com.moviewatchlist.repository.MovieRepository;
import com.moviewatchlist.repository.WatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final MovieRepository movieRepository;
    private final TmdbService tmdbService;

    public WatchlistService(WatchlistRepository watchlistRepository,
                            MovieRepository movieRepository,
                            TmdbService tmdbService) {
        this.watchlistRepository = watchlistRepository;
        this.movieRepository = movieRepository;
        this.tmdbService = tmdbService;
    }

    /**
     * Add a movie to the watchlist
     */
    @Transactional
    public WatchlistEntry addToWatchlist(WatchlistRequest request) {
        // Find or create the movie
        Movie movie = movieRepository.findByTmdbId(request.getTmdbId())
                .orElseGet(() -> {
                    Movie m = new Movie();
                    m.setTmdbId(request.getTmdbId());
                    m.setTitle(request.getTitle());
                    m.setGenres(request.getGenres());
                    m.setPosterPath(request.getPosterPath());
                    m.setBackdropPath(request.getBackdropPath());
                    m.setOverview(request.getOverview());
                    m.setReleaseDate(request.getReleaseDate());
                    m.setVoteAverage(request.getVoteAverage());
                    return movieRepository.save(m);
                });

        // Check if already in watchlist
        Optional<WatchlistEntry> existing = watchlistRepository.findByMovieId(movie.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create watchlist entry
        WatchlistEntry entry = new WatchlistEntry();
        entry.setMovie(movie);
        entry.setStatus(request.getStatus() != null ?
                WatchStatus.valueOf(request.getStatus()) : WatchStatus.plan_to_watch);

        if (request.getUserRating() != null) {
            entry.setUserRating(request.getUserRating());
        }
        if (request.getNotes() != null) {
            entry.setNotes(request.getNotes());
        }

        return watchlistRepository.save(entry);
    }

    /**
     * Get all watchlist entries with optional filtering
     */
    public List<WatchlistEntry> getWatchlist(String status, String sortBy) {
        if (status != null && !status.isEmpty() && !status.equals("all")) {
            WatchStatus watchStatus = WatchStatus.valueOf(status);
            if (sortBy != null && !sortBy.isEmpty()) {
                return watchlistRepository.findByStatusWithSort(watchStatus, sortBy);
            }
            return watchlistRepository.findAllByStatusOrderByAddedAtDesc(watchStatus);
        }
        return watchlistRepository.findAllByOrderByAddedAtDesc();
    }

    /**
     * Update a watchlist entry
     */
    @Transactional
    public WatchlistEntry updateEntry(Long id, WatchlistRequest request) {
        WatchlistEntry entry = watchlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Watchlist entry not found: " + id));

        if (request.getStatus() != null) {
            entry.setStatus(WatchStatus.valueOf(request.getStatus()));
        }
        if (request.getUserRating() != null) {
            entry.setUserRating(request.getUserRating());
        }
        if (request.getNotes() != null) {
            entry.setNotes(request.getNotes());
        }

        return watchlistRepository.save(entry);
    }

    /**
     * Remove from watchlist
     */
    @Transactional
    public void removeFromWatchlist(Long id) {
        watchlistRepository.deleteById(id);
    }

    /**
     * Get movie recommendations based on genres of highly-rated movies
     */
    public List<TmdbMovieDto> getRecommendations() {
        List<String> genreStrings = watchlistRepository.findGenresOfHighlyRatedMovies(4);
        if (genreStrings.isEmpty()) {
            // Fallback: get popular movies
            return tmdbService.discoverMovies(null, null, 1);
        }

        // Extract unique genre names
        Set<String> likedGenres = new HashSet<>();
        for (String gs : genreStrings) {
            if (gs != null) {
                for (String g : gs.split(",\\s*")) {
                    likedGenres.add(g.trim());
                }
            }
        }

        // Map genre names to IDs
        var genres = tmdbService.getGenres();
        List<Integer> genreIds = genres.stream()
                .filter(g -> likedGenres.contains(g.getName()))
                .map(g -> g.getId())
                .limit(3)
                .collect(Collectors.toList());

        if (genreIds.isEmpty()) {
            return tmdbService.discoverMovies(null, null, 1);
        }

        // Get movies from the first liked genre
        return tmdbService.discoverMovies(genreIds.get(0), null, 1);
    }

    /**
     * Export watchlist as CSV
     */
    public String exportToCsv() {
        List<WatchlistEntry> entries = watchlistRepository.findAllByOrderByAddedAtDesc();
        StringWriter sw = new StringWriter();

        sw.write("Title,Genre,Status,Rating,Notes,Release Date,TMDB Rating,Added At\n");

        for (WatchlistEntry entry : entries) {
            Movie m = entry.getMovie();
            sw.write(String.format("\"%s\",\"%s\",\"%s\",%s,\"%s\",\"%s\",%s,\"%s\"\n",
                    escapeCsv(m.getTitle()),
                    escapeCsv(m.getGenres()),
                    entry.getStatus(),
                    entry.getUserRating() != null ? entry.getUserRating() : "",
                    escapeCsv(entry.getNotes()),
                    m.getReleaseDate() != null ? m.getReleaseDate() : "",
                    m.getVoteAverage() != null ? m.getVoteAverage() : "",
                    entry.getAddedAt() != null ? entry.getAddedAt().toString() : ""
            ));
        }

        return sw.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
