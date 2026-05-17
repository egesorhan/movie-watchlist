package com.moviewatchlist.service;

import com.moviewatchlist.dto.GenreDto;
import com.moviewatchlist.dto.TmdbMovieDto;
import com.moviewatchlist.dto.TmdbSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class TmdbService {

    private final RestTemplate restTemplate;

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.api.base-url}")
    private String baseUrl;

    @Value("${tmdb.api.image-base-url}")
    private String imageBaseUrl;

    // Cache genre maps so we don't keep re-fetching
    private Map<Integer, String> movieGenreMap;
    private Map<Integer, String> tvGenreMap;

    public TmdbService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // =============================================
    // MOVIE ENDPOINTS
    // =============================================

    /**
     * Search movies by title
     */
    public List<TmdbMovieDto> searchMovies(String query, int page) {
        String url = String.format("%s/search/movie?api_key=%s&query=%s&page=%d&language=en-US",
                baseUrl, apiKey, query, page);

        TmdbSearchResponse response = restTemplate.getForObject(url, TmdbSearchResponse.class);
        if (response != null && response.getResults() != null) {
            response.getResults().forEach(m -> m.setMediaType("movie"));
            enrichWithGenreNames(response.getResults(), getMovieGenreMap());
            return response.getResults();
        }
        return Collections.emptyList();
    }

    /**
     * Discover movies by genre and/or year
     */
    public List<TmdbMovieDto> discoverMovies(Integer genreId, Integer year, int page) {
        StringBuilder url = new StringBuilder(
                String.format("%s/discover/movie?api_key=%s&page=%d&language=en-US&sort_by=popularity.desc",
                        baseUrl, apiKey, page));

        if (genreId != null) {
            url.append("&with_genres=").append(genreId);
        }
        if (year != null) {
            url.append("&primary_release_year=").append(year);
        }

        TmdbSearchResponse response = restTemplate.getForObject(url.toString(), TmdbSearchResponse.class);
        if (response != null && response.getResults() != null) {
            response.getResults().forEach(m -> m.setMediaType("movie"));
            enrichWithGenreNames(response.getResults(), getMovieGenreMap());
            return response.getResults();
        }
        return Collections.emptyList();
    }

    /**
     * Get all movie genres from TMDB
     */
    @SuppressWarnings("unchecked")
    public List<GenreDto> getGenres() {
        return fetchGenres("movie");
    }

    // =============================================
    // TV ENDPOINTS
    // =============================================

    /**
     * Search TV shows by name
     */
    public List<TmdbMovieDto> searchTv(String query, int page) {
        String url = String.format("%s/search/tv?api_key=%s&query=%s&page=%d&language=en-US",
                baseUrl, apiKey, query, page);

        TmdbSearchResponse response = restTemplate.getForObject(url, TmdbSearchResponse.class);
        if (response != null && response.getResults() != null) {
            for (TmdbMovieDto dto : response.getResults()) {
                dto.setMediaType("tv");
                // Normalize: copy name -> title if title is null
                if (dto.getTitle() == null && dto.getName() != null) {
                    dto.setTitle(dto.getName());
                }
                // Normalize: copy first_air_date -> releaseDate
                if (dto.getReleaseDate() == null && dto.getFirstAirDate() != null) {
                    dto.setReleaseDate(dto.getFirstAirDate());
                }
            }
            enrichWithGenreNames(response.getResults(), getTvGenreMap());
            return response.getResults();
        }
        return Collections.emptyList();
    }

    /**
     * Discover TV shows by genre and/or year
     */
    public List<TmdbMovieDto> discoverTv(Integer genreId, Integer year, int page) {
        StringBuilder url = new StringBuilder(
                String.format("%s/discover/tv?api_key=%s&page=%d&language=en-US&sort_by=popularity.desc",
                        baseUrl, apiKey, page));

        if (genreId != null) {
            url.append("&with_genres=").append(genreId);
        }
        if (year != null) {
            url.append("&first_air_date_year=").append(year);
        }

        TmdbSearchResponse response = restTemplate.getForObject(url.toString(), TmdbSearchResponse.class);
        if (response != null && response.getResults() != null) {
            for (TmdbMovieDto dto : response.getResults()) {
                dto.setMediaType("tv");
                if (dto.getTitle() == null && dto.getName() != null) {
                    dto.setTitle(dto.getName());
                }
                if (dto.getReleaseDate() == null && dto.getFirstAirDate() != null) {
                    dto.setReleaseDate(dto.getFirstAirDate());
                }
            }
            enrichWithGenreNames(response.getResults(), getTvGenreMap());
            return response.getResults();
        }
        return Collections.emptyList();
    }

    /**
     * Get all TV genres from TMDB
     */
    public List<GenreDto> getTvGenres() {
        return fetchGenres("tv");
    }

    // =============================================
    // MULTI-SEARCH (returns both movies & TV)
    // =============================================

    /**
     * Multi-search: returns both movies and TV shows
     */
    public List<TmdbMovieDto> searchMulti(String query, int page) {
        String url = String.format("%s/search/multi?api_key=%s&query=%s&page=%d&language=en-US",
                baseUrl, apiKey, query, page);

        TmdbSearchResponse response = restTemplate.getForObject(url, TmdbSearchResponse.class);
        if (response != null && response.getResults() != null) {
            List<TmdbMovieDto> filtered = new ArrayList<>();
            for (TmdbMovieDto dto : response.getResults()) {
                String type = dto.getMediaType();
                // Multi search returns "person" results too — skip those
                if (type == null || (!type.equals("movie") && !type.equals("tv"))) {
                    continue;
                }
                // Normalize TV fields
                if ("tv".equals(type)) {
                    if (dto.getTitle() == null && dto.getName() != null) {
                        dto.setTitle(dto.getName());
                    }
                    if (dto.getReleaseDate() == null && dto.getFirstAirDate() != null) {
                        dto.setReleaseDate(dto.getFirstAirDate());
                    }
                }
                filtered.add(dto);
            }
            // Enrich with genre names (need both maps)
            enrichWithGenreNames(filtered, getMergedGenreMap());
            return filtered;
        }
        return Collections.emptyList();
    }

    // =============================================
    // IMAGE HELPERS
    // =============================================

    public String getPosterUrl(String posterPath) {
        if (posterPath == null || posterPath.isEmpty()) return null;
        return imageBaseUrl + "/w500" + posterPath;
    }

    public String getBackdropUrl(String backdropPath) {
        if (backdropPath == null || backdropPath.isEmpty()) return null;
        return imageBaseUrl + "/w1280" + backdropPath;
    }

    // =============================================
    // GENRE HELPERS
    // =============================================

    @SuppressWarnings("unchecked")
    private List<GenreDto> fetchGenres(String type) {
        String url = String.format("%s/genre/%s/list?api_key=%s&language=en-US", baseUrl, type, apiKey);
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        List<GenreDto> genres = new ArrayList<>();
        if (response != null && response.containsKey("genres")) {
            List<Map<String, Object>> genreList = (List<Map<String, Object>>) response.get("genres");
            for (Map<String, Object> g : genreList) {
                GenreDto dto = new GenreDto();
                dto.setId((Integer) g.get("id"));
                dto.setName((String) g.get("name"));
                genres.add(dto);
            }
        }
        return genres;
    }

    private Map<Integer, String> getMovieGenreMap() {
        if (movieGenreMap == null) {
            movieGenreMap = new HashMap<>();
            List<GenreDto> genres = getGenres();
            for (GenreDto g : genres) {
                movieGenreMap.put(g.getId(), g.getName());
            }
        }
        return movieGenreMap;
    }

    private Map<Integer, String> getTvGenreMap() {
        if (tvGenreMap == null) {
            tvGenreMap = new HashMap<>();
            List<GenreDto> genres = getTvGenres();
            for (GenreDto g : genres) {
                tvGenreMap.put(g.getId(), g.getName());
            }
        }
        return tvGenreMap;
    }

    private Map<Integer, String> getMergedGenreMap() {
        Map<Integer, String> merged = new HashMap<>(getMovieGenreMap());
        merged.putAll(getTvGenreMap());
        return merged;
    }

    private void enrichWithGenreNames(List<TmdbMovieDto> items, Map<Integer, String> genreMap) {
        for (TmdbMovieDto item : items) {
            if (item.getGenreIds() != null) {
                List<String> names = new ArrayList<>();
                for (int id : item.getGenreIds()) {
                    String name = genreMap.get(id);
                    if (name != null) names.add(name);
                }
                item.setGenreNames(String.join(", ", names));
            }
        }
    }
}
