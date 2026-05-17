package com.moviewatchlist.dto;

public class WatchlistRequest {
    private Integer tmdbId;
    private String title;
    private String genres;
    private String posterPath;
    private String backdropPath;
    private String overview;
    private String releaseDate;
    private Double voteAverage;
    private String status;
    private Integer userRating;
    private String notes;

    // TV-specific fields
    private String mediaType; // "movie" or "tv"
    private Integer numberOfSeasons;
    private Integer numberOfEpisodes;
    private Integer currentSeason;
    private Integer currentEpisode;
    private String showStatus; // Returning Series, Ended, etc.

    // Getters and Setters
    public Integer getTmdbId() { return tmdbId; }
    public void setTmdbId(Integer tmdbId) { this.tmdbId = tmdbId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getGenres() { return genres; }
    public void setGenres(String genres) { this.genres = genres; }

    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }

    public String getBackdropPath() { return backdropPath; }
    public void setBackdropPath(String backdropPath) { this.backdropPath = backdropPath; }

    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }

    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }

    public Double getVoteAverage() { return voteAverage; }
    public void setVoteAverage(Double voteAverage) { this.voteAverage = voteAverage; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getUserRating() { return userRating; }
    public void setUserRating(Integer userRating) { this.userRating = userRating; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public Integer getNumberOfSeasons() { return numberOfSeasons; }
    public void setNumberOfSeasons(Integer numberOfSeasons) { this.numberOfSeasons = numberOfSeasons; }

    public Integer getNumberOfEpisodes() { return numberOfEpisodes; }
    public void setNumberOfEpisodes(Integer numberOfEpisodes) { this.numberOfEpisodes = numberOfEpisodes; }

    public Integer getCurrentSeason() { return currentSeason; }
    public void setCurrentSeason(Integer currentSeason) { this.currentSeason = currentSeason; }

    public Integer getCurrentEpisode() { return currentEpisode; }
    public void setCurrentEpisode(Integer currentEpisode) { this.currentEpisode = currentEpisode; }

    public String getShowStatus() { return showStatus; }
    public void setShowStatus(String showStatus) { this.showStatus = showStatus; }
}
