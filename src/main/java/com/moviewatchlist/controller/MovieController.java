package com.moviewatchlist.controller;

import com.moviewatchlist.dto.GenreDto;
import com.moviewatchlist.dto.TmdbMovieDto;
import com.moviewatchlist.service.TmdbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final TmdbService tmdbService;

    public MovieController(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
    }

    /**
     * Search movies, TV shows, or both (multi)
     * type = "movie" (default), "tv", or "multi"
     */
    @GetMapping("/search")
    public ResponseEntity<List<TmdbMovieDto>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "multi") String type) {

        List<TmdbMovieDto> results;
        switch (type) {
            case "tv":
                results = tmdbService.searchTv(query, page);
                break;
            case "multi":
                results = tmdbService.searchMulti(query, page);
                break;
            default:
                results = tmdbService.searchMovies(query, page);
                break;
        }
        return ResponseEntity.ok(results);
    }

    /**
     * Discover movies or TV shows by genre and/or year
     * type = "movie" (default) or "tv"
     */
    @GetMapping("/discover")
    public ResponseEntity<List<TmdbMovieDto>> discover(
            @RequestParam(required = false) Integer genre,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "movie") String type) {

        List<TmdbMovieDto> results;
        if ("tv".equals(type)) {
            results = tmdbService.discoverTv(genre, year, page);
        } else {
            results = tmdbService.discoverMovies(genre, year, page);
        }
        return ResponseEntity.ok(results);
    }

    /**
     * Get genres for movies or TV
     * type = "movie" (default) or "tv"
     */
    @GetMapping("/genres")
    public ResponseEntity<List<GenreDto>> getGenres(
            @RequestParam(defaultValue = "movie") String type) {
        if ("tv".equals(type)) {
            return ResponseEntity.ok(tmdbService.getTvGenres());
        }
        return ResponseEntity.ok(tmdbService.getGenres());
    }
}
