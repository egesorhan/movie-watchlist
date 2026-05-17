package com.moviewatchlist.service;

import com.moviewatchlist.dto.TmdbMovieDto;
import com.moviewatchlist.dto.WatchlistRequest;
import com.moviewatchlist.entity.TvShow;
import com.moviewatchlist.entity.TvWatchlistEntry;
import com.moviewatchlist.entity.TvWatchlistEntry.WatchStatus;
import com.moviewatchlist.repository.TvShowRepository;
import com.moviewatchlist.repository.TvWatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TvWatchlistService {

    private final TvWatchlistRepository tvWatchlistRepository;
    private final TvShowRepository tvShowRepository;
    private final TmdbService tmdbService;

    public TvWatchlistService(TvWatchlistRepository tvWatchlistRepository,
                              TvShowRepository tvShowRepository,
                              TmdbService tmdbService) {
        this.tvWatchlistRepository = tvWatchlistRepository;
        this.tvShowRepository = tvShowRepository;
        this.tmdbService = tmdbService;
    }

    /**
     * Add a TV show to the watchlist
     */
    @Transactional
    public TvWatchlistEntry addToWatchlist(WatchlistRequest request) {
        TvShow tvShow = tvShowRepository.findByTmdbId(request.getTmdbId())
                .orElseGet(() -> {
                    TvShow s = new TvShow();
                    s.setTmdbId(request.getTmdbId());
                    s.setName(request.getTitle());
                    s.setGenres(request.getGenres());
                    s.setPosterPath(request.getPosterPath());
                    s.setBackdropPath(request.getBackdropPath());
                    s.setOverview(request.getOverview());
                    s.setFirstAirDate(request.getReleaseDate());
                    s.setVoteAverage(request.getVoteAverage());
                    s.setNumberOfSeasons(request.getNumberOfSeasons());
                    s.setNumberOfEpisodes(request.getNumberOfEpisodes());
                    s.setShowStatus(request.getShowStatus());
                    return tvShowRepository.save(s);
                });

        // Check if already in watchlist
        Optional<TvWatchlistEntry> existing = tvWatchlistRepository.findByTvShowId(tvShow.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        TvWatchlistEntry entry = new TvWatchlistEntry();
        entry.setTvShow(tvShow);
        entry.setStatus(request.getStatus() != null ?
                WatchStatus.valueOf(request.getStatus()) : WatchStatus.plan_to_watch);

        if (request.getUserRating() != null) {
            entry.setUserRating(request.getUserRating());
        }
        if (request.getNotes() != null) {
            entry.setNotes(request.getNotes());
        }
        if (request.getCurrentSeason() != null) {
            entry.setCurrentSeason(request.getCurrentSeason());
        }
        if (request.getCurrentEpisode() != null) {
            entry.setCurrentEpisode(request.getCurrentEpisode());
        }

        return tvWatchlistRepository.save(entry);
    }

    /**
     * Get all TV watchlist entries with optional filtering
     */
    public List<TvWatchlistEntry> getWatchlist(String status, String sortBy) {
        if (status != null && !status.isEmpty() && !status.equals("all")) {
            WatchStatus watchStatus = WatchStatus.valueOf(status);
            if (sortBy != null && !sortBy.isEmpty()) {
                return tvWatchlistRepository.findByStatusWithSort(watchStatus, sortBy);
            }
            return tvWatchlistRepository.findAllByStatusOrderByAddedAtDesc(watchStatus);
        }
        return tvWatchlistRepository.findAllByOrderByAddedAtDesc();
    }

    /**
     * Update a TV watchlist entry
     */
    @Transactional
    public TvWatchlistEntry updateEntry(Long id, WatchlistRequest request) {
        TvWatchlistEntry entry = tvWatchlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TV Watchlist entry not found: " + id));

        if (request.getStatus() != null) {
            entry.setStatus(WatchStatus.valueOf(request.getStatus()));
        }
        if (request.getUserRating() != null) {
            entry.setUserRating(request.getUserRating());
        }
        if (request.getNotes() != null) {
            entry.setNotes(request.getNotes());
        }
        if (request.getCurrentSeason() != null) {
            entry.setCurrentSeason(request.getCurrentSeason());
        }
        if (request.getCurrentEpisode() != null) {
            entry.setCurrentEpisode(request.getCurrentEpisode());
        }

        return tvWatchlistRepository.save(entry);
    }

    /**
     * Remove from TV watchlist
     */
    @Transactional
    public void removeFromWatchlist(Long id) {
        tvWatchlistRepository.deleteById(id);
    }

    /**
     * Get TV recommendations based on genres of highly-rated shows
     */
    public List<TmdbMovieDto> getRecommendations() {
        List<String> genreStrings = tvWatchlistRepository.findGenresOfHighlyRatedShows(4);
        if (genreStrings.isEmpty()) {
            return tmdbService.discoverTv(null, null, 1);
        }

        Set<String> likedGenres = new HashSet<>();
        for (String gs : genreStrings) {
            if (gs != null) {
                for (String g : gs.split(",\\s*")) {
                    likedGenres.add(g.trim());
                }
            }
        }

        var genres = tmdbService.getTvGenres();
        List<Integer> genreIds = genres.stream()
                .filter(g -> likedGenres.contains(g.getName()))
                .map(g -> g.getId())
                .limit(3)
                .collect(Collectors.toList());

        if (genreIds.isEmpty()) {
            return tmdbService.discoverTv(null, null, 1);
        }

        return tmdbService.discoverTv(genreIds.get(0), null, 1);
    }

    /**
     * Export TV watchlist as CSV
     */
    public String exportToCsv() {
        List<TvWatchlistEntry> entries = tvWatchlistRepository.findAllByOrderByAddedAtDesc();
        StringWriter sw = new StringWriter();

        sw.write("Name,Genre,Status,Rating,Notes,First Air Date,TMDB Rating,Seasons,Episodes,Current Season,Current Episode,Added At\n");

        for (TvWatchlistEntry entry : entries) {
            TvShow s = entry.getTvShow();
            sw.write(String.format("\"%s\",\"%s\",\"%s\",%s,\"%s\",\"%s\",%s,%s,%s,%s,%s,\"%s\"\n",
                    escapeCsv(s.getName()),
                    escapeCsv(s.getGenres()),
                    entry.getStatus(),
                    entry.getUserRating() != null ? entry.getUserRating() : "",
                    escapeCsv(entry.getNotes()),
                    s.getFirstAirDate() != null ? s.getFirstAirDate() : "",
                    s.getVoteAverage() != null ? s.getVoteAverage() : "",
                    s.getNumberOfSeasons() != null ? s.getNumberOfSeasons() : "",
                    s.getNumberOfEpisodes() != null ? s.getNumberOfEpisodes() : "",
                    entry.getCurrentSeason() != null ? entry.getCurrentSeason() : "",
                    entry.getCurrentEpisode() != null ? entry.getCurrentEpisode() : "",
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
