package com.moviewatchlist.controller;

import com.moviewatchlist.dto.TmdbMovieDto;
import com.moviewatchlist.dto.WatchlistRequest;
import com.moviewatchlist.entity.TvWatchlistEntry;
import com.moviewatchlist.service.TvWatchlistService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tv-watchlist")
public class TvWatchlistController {

    private final TvWatchlistService tvWatchlistService;

    public TvWatchlistController(TvWatchlistService tvWatchlistService) {
        this.tvWatchlistService = tvWatchlistService;
    }

    /**
     * Get all TV watchlist entries with optional filtering
     */
    @GetMapping
    public ResponseEntity<List<TvWatchlistEntry>> getWatchlist(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "added") String sortBy) {
        return ResponseEntity.ok(tvWatchlistService.getWatchlist(status, sortBy));
    }

    /**
     * Add a TV show to the watchlist
     */
    @PostMapping
    public ResponseEntity<TvWatchlistEntry> addToWatchlist(@RequestBody WatchlistRequest request) {
        return ResponseEntity.ok(tvWatchlistService.addToWatchlist(request));
    }

    /**
     * Update a TV watchlist entry (status, rating, notes, progress)
     */
    @PutMapping("/{id}")
    public ResponseEntity<TvWatchlistEntry> updateEntry(
            @PathVariable Long id,
            @RequestBody WatchlistRequest request) {
        return ResponseEntity.ok(tvWatchlistService.updateEntry(id, request));
    }

    /**
     * Remove a TV show from the watchlist
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFromWatchlist(@PathVariable Long id) {
        tvWatchlistService.removeFromWatchlist(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Get TV show recommendations
     */
    @GetMapping("/recommendations")
    public ResponseEntity<List<TmdbMovieDto>> getRecommendations() {
        return ResponseEntity.ok(tvWatchlistService.getRecommendations());
    }

    /**
     * Export TV watchlist as CSV
     */
    @GetMapping("/export")
    public ResponseEntity<String> exportCsv() {
        String csv = tvWatchlistService.exportToCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tv-watchlist.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
