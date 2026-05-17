package com.moviewatchlist.controller;

import com.moviewatchlist.dto.TmdbMovieDto;
import com.moviewatchlist.dto.WatchlistRequest;
import com.moviewatchlist.entity.WatchlistEntry;
import com.moviewatchlist.service.WatchlistService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    /**
     * Get all watchlist entries with optional filtering
     */
    @GetMapping
    public ResponseEntity<List<WatchlistEntry>> getWatchlist(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "added") String sortBy) {
        return ResponseEntity.ok(watchlistService.getWatchlist(status, sortBy));
    }

    /**
     * Add a movie to the watchlist
     */
    @PostMapping
    public ResponseEntity<WatchlistEntry> addToWatchlist(@RequestBody WatchlistRequest request) {
        return ResponseEntity.ok(watchlistService.addToWatchlist(request));
    }

    /**
     * Update a watchlist entry (status, rating, notes)
     */
    @PutMapping("/{id}")
    public ResponseEntity<WatchlistEntry> updateEntry(
            @PathVariable Long id,
            @RequestBody WatchlistRequest request) {
        return ResponseEntity.ok(watchlistService.updateEntry(id, request));
    }

    /**
     * Remove a movie from the watchlist
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFromWatchlist(@PathVariable Long id) {
        watchlistService.removeFromWatchlist(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Get movie recommendations
     */
    @GetMapping("/recommendations")
    public ResponseEntity<List<TmdbMovieDto>> getRecommendations() {
        return ResponseEntity.ok(watchlistService.getRecommendations());
    }

    /**
     * Export watchlist as CSV
     */
    @GetMapping("/export")
    public ResponseEntity<String> exportCsv() {
        String csv = watchlistService.exportToCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=watchlist.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
