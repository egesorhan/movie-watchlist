/* ========================================
   WATCHLIST — Frontend Application
   Movies & TV Shows
   ======================================== */

const API = {
    // Search (supports type: movie, tv, multi)
    search: (query, page = 1, type = 'multi') =>
        fetch(`/api/movies/search?query=${encodeURIComponent(query)}&page=${page}&type=${type}`).then(r => r.json()),

    // Discover (supports type: movie, tv)
    discover: (genre, year, page = 1, type = 'movie') => {
        let url = `/api/movies/discover?page=${page}&type=${type}`;
        if (genre) url += `&genre=${genre}`;
        if (year) url += `&year=${year}`;
        return fetch(url).then(r => r.json());
    },

    // Genres (supports type: movie, tv)
    getGenres: (type = 'movie') => fetch(`/api/movies/genres?type=${type}`).then(r => r.json()),

    // Movie Watchlist
    getWatchlist: (status, sortBy) => {
        let url = '/api/watchlist?';
        if (status && status !== 'all') url += `status=${status}&`;
        if (sortBy) url += `sortBy=${sortBy}`;
        return fetch(url).then(r => r.json());
    },
    addToWatchlist: (data) => fetch('/api/watchlist', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    }).then(r => r.json()),
    updateWatchlist: (id, data) => fetch(`/api/watchlist/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    }).then(r => r.json()),
    removeFromWatchlist: (id) => fetch(`/api/watchlist/${id}`, { method: 'DELETE' }),

    // TV Watchlist
    getTvWatchlist: (status, sortBy) => {
        let url = '/api/tv-watchlist?';
        if (status && status !== 'all') url += `status=${status}&`;
        if (sortBy) url += `sortBy=${sortBy}`;
        return fetch(url).then(r => r.json());
    },
    addToTvWatchlist: (data) => fetch('/api/tv-watchlist', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    }).then(r => r.json()),
    updateTvWatchlist: (id, data) => fetch(`/api/tv-watchlist/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    }).then(r => r.json()),
    removeFromTvWatchlist: (id) => fetch(`/api/tv-watchlist/${id}`, { method: 'DELETE' }),

    // Recommendations
    getMovieRecommendations: () => fetch('/api/watchlist/recommendations').then(r => r.json()),
    getTvRecommendations: () => fetch('/api/tv-watchlist/recommendations').then(r => r.json()),

    // Export
    exportMovieCsv: () => window.open('/api/watchlist/export', '_blank'),
    exportTvCsv: () => window.open('/api/tv-watchlist/export', '_blank')
};

// ---- State ----
const state = {
    currentTab: 'search',
    searchType: 'multi',       // multi, movie, tv
    searchResults: [],
    movieWatchlist: [],
    tvWatchlist: [],
    movieWatchlistMap: {},     // tmdbId -> watchlist entry
    tvWatchlistMap: {},        // tmdbId -> tv watchlist entry
    genres: { movie: [], tv: [] },
    genreMap: {},
    currentFilter: 'all',
    currentSort: 'added',
    currentWlType: 'movies',   // movies or tv
    currentRecType: 'movies',  // movies or tv
    selectedMovie: null,
    selectedRating: 0,
    isEditMode: false,
    editEntryId: null
};

const TMDB_IMG = 'https://image.tmdb.org/t/p';

// ---- Init ----
document.addEventListener('DOMContentLoaded', () => {
    initTabs();
    initSearch();
    initMediaToggle();
    initModal();
    initWatchlistFilters();
    initWatchlistMediaToggle();
    initRecMediaToggle();
    initExport();
    loadGenres('movie');
    loadGenres('tv');
    loadWatchlist();
    populateYearFilter();
});

// ---- Tab Navigation ----
function initTabs() {
    document.querySelectorAll('.nav-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            const tabId = tab.dataset.tab;
            switchTab(tabId);
        });
    });
}

function switchTab(tabId) {
    state.currentTab = tabId;

    document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
    document.querySelector(`[data-tab="${tabId}"]`).classList.add('active');

    document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
    document.getElementById(`section-${tabId}`).classList.add('active');

    if (tabId === 'watchlist') loadWatchlist();
    if (tabId === 'recommendations') loadRecommendations();
}

// ---- Media Type Toggle (Search) ----
function initMediaToggle() {
    document.querySelectorAll('.media-toggle-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.media-toggle-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            state.searchType = btn.dataset.type;

            // Reload genres for the selected type
            const genreType = state.searchType === 'multi' ? 'movie' : state.searchType;
            populateGenreFilter(genreType);

            // Re-trigger search if there's a query
            const query = document.getElementById('search-input').value.trim();
            if (query.length >= 2) {
                triggerSearch(query);
            }
        });
    });
}

// ---- Watchlist Media Toggle ----
function initWatchlistMediaToggle() {
    document.querySelectorAll('.wl-media-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.wl-media-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            state.currentWlType = btn.dataset.wlType;
            loadWatchlist();
        });
    });
}

// ---- Recommendations Media Toggle ----
function initRecMediaToggle() {
    document.querySelectorAll('.rec-media-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.rec-media-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            state.currentRecType = btn.dataset.recType;
            loadRecommendations();
        });
    });
}

// ---- Search ----
let searchTimeout;
function initSearch() {
    const input = document.getElementById('search-input');
    const clearBtn = document.getElementById('search-clear');
    const discoverBtn = document.getElementById('discover-btn');

    input.addEventListener('input', () => {
        clearTimeout(searchTimeout);
        const query = input.value.trim();

        clearBtn.style.display = query ? 'flex' : 'none';

        if (query.length < 2) {
            document.getElementById('search-results').innerHTML = '';
            document.getElementById('search-empty').style.display = query.length === 0 ? 'block' : 'none';
            return;
        }

        document.getElementById('search-empty').style.display = 'none';
        document.getElementById('search-loading').style.display = 'flex';

        searchTimeout = setTimeout(() => triggerSearch(query), 400);
    });

    clearBtn.addEventListener('click', () => {
        input.value = '';
        clearBtn.style.display = 'none';
        document.getElementById('search-results').innerHTML = '';
        document.getElementById('search-empty').style.display = 'block';
        input.focus();
    });

    discoverBtn.addEventListener('click', async () => {
        const genre = document.getElementById('genre-filter').value;
        const year = document.getElementById('year-filter').value;

        if (!genre && !year) {
            showToast('Select a genre or year to discover', 'info');
            return;
        }

        document.getElementById('search-empty').style.display = 'none';
        document.getElementById('search-loading').style.display = 'flex';
        document.getElementById('search-results').innerHTML = '';

        try {
            const discoverType = state.searchType === 'multi' ? 'movie' : state.searchType;
            const results = await API.discover(genre || null, year || null, 1, discoverType);
            state.searchResults = results;
            renderMovieGrid(results, 'search-results');
        } catch (err) {
            console.error('Discover error:', err);
            showToast('Failed to discover content', 'error');
        } finally {
            document.getElementById('search-loading').style.display = 'none';
        }
    });
}

async function triggerSearch(query) {
    try {
        const results = await API.search(query, 1, state.searchType);
        state.searchResults = results;
        renderMovieGrid(results, 'search-results');
    } catch (err) {
        console.error('Search error:', err);
        showToast('Failed to search', 'error');
    } finally {
        document.getElementById('search-loading').style.display = 'none';
    }
}

// ---- Genres ----
async function loadGenres(type) {
    try {
        const genres = await API.getGenres(type);
        state.genres[type] = genres;
        genres.forEach(g => state.genreMap[g.id] = g.name);

        // Populate genre filter initially with movie genres
        if (type === 'movie') {
            populateGenreFilter('movie');
        }
    } catch (err) {
        console.error(`Failed to load ${type} genres:`, err);
    }
}

function populateGenreFilter(type) {
    const select = document.getElementById('genre-filter');
    // Keep first option, remove rest
    while (select.options.length > 1) select.remove(1);
    const genres = state.genres[type] || [];
    genres.forEach(g => {
        const opt = document.createElement('option');
        opt.value = g.id;
        opt.textContent = g.name;
        select.appendChild(opt);
    });
}

function populateYearFilter() {
    const select = document.getElementById('year-filter');
    const currentYear = new Date().getFullYear();
    for (let y = currentYear; y >= 1950; y--) {
        const opt = document.createElement('option');
        opt.value = y;
        opt.textContent = y;
        select.appendChild(opt);
    }
}

// ---- Render Movie/TV Grid ----
function renderMovieGrid(items, containerId) {
    const container = document.getElementById(containerId);
    if (!items || items.length === 0) {
        container.innerHTML = `
            <div class="empty-state" style="grid-column: 1/-1;">
                <div class="empty-icon">🔍</div>
                <p>No results found</p>
            </div>`;
        return;
    }

    container.innerHTML = items.map((item, i) => {
        const tmdbId = item.id || item.tmdbId;
        const mediaType = item.mediaType || item.media_type || 'movie';
        const pPath = item.posterPath || item.poster_path;
        const posterUrl = pPath ? `${TMDB_IMG}/w500${pPath}` : null;
        const title = item.title || item.name || 'Unknown';
        const date = item.releaseDate || item.release_date || item.firstAirDate || item.first_air_date || '';
        const year = date.substring(0, 4);
        const rating = item.voteAverage || item.vote_average || item.voteAverage === 0 ? (item.voteAverage || item.vote_average || 0) : 0;
        const genres = item.genreNames || '';

        // Check if in watchlist
        const isMovie = mediaType === 'movie';
        const watchlistEntry = isMovie ? state.movieWatchlistMap[tmdbId] : state.tvWatchlistMap[tmdbId];
        let badgeHtml = '';
        if (watchlistEntry) {
            const statusLabels = { plan_to_watch: 'Planned', watching: 'Watching', watched: 'Watched' };
            const label = statusLabels[watchlistEntry.status] || 'Added';
            badgeHtml = `<div class="watchlist-badge status-${watchlistEntry.status}">${label}</div>`;
        }

        // Media type badge
        const typeIcon = mediaType === 'tv' ? '📺' : '🎬';
        const typeBadge = `<div class="media-type-badge type-${mediaType}">${typeIcon}</div>`;

        return `
        <div class="movie-card" onclick="openMovieModal(${JSON.stringify(item).replace(/"/g, '&quot;')})" style="animation-delay: ${i * 0.05}s">
            ${badgeHtml}
            ${typeBadge}
            <div class="poster-container">
                ${posterUrl
                    ? `<img class="poster" src="${posterUrl}" alt="${title}" loading="lazy">`
                    : `<div class="poster-placeholder">${typeIcon}</div>`}
                <div class="poster-overlay">
                    <span class="card-rating">
                        <svg viewBox="0 0 24 24" fill="currentColor"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
                        ${rating.toFixed(1)}
                    </span>
                </div>
            </div>
            <div class="card-info">
                <div class="card-title">${title}</div>
                <div class="card-year">${year}</div>
                ${genres ? `<div class="card-genres">${genres}</div>` : ''}
            </div>
        </div>`;
    }).join('');
}

// ---- Render Watchlist Grid ----
function renderWatchlistGrid(entries, type) {
    const container = document.getElementById('watchlist-results');
    const emptyEl = document.getElementById('watchlist-empty');

    if (!entries || entries.length === 0) {
        container.innerHTML = '';
        emptyEl.style.display = 'block';
        return;
    }

    emptyEl.style.display = 'none';

    const isTv = type === 'tv';

    container.innerHTML = entries.map((entry, i) => {
        const media = isTv ? entry.tvShow : entry.movie;
        const title = isTv ? media.name : media.title;
        const posterUrl = media.posterPath ? `${TMDB_IMG}/w500${media.posterPath}` : null;
        const date = isTv ? (media.firstAirDate || '') : (media.releaseDate || '');
        const year = date.substring(0, 4);
        const rating = media.voteAverage || 0;
        const typeIcon = isTv ? '📺' : '🎬';

        const statusLabels = { plan_to_watch: 'Planned', watching: 'Watching', watched: 'Watched' };
        const label = statusLabels[entry.status] || '';

        // User stars
        let starsHtml = '';
        if (entry.userRating) {
            for (let s = 1; s <= 5; s++) {
                starsHtml += `<span class="${s <= entry.userRating ? 'star-filled' : 'star-empty'}">★</span>`;
            }
        }

        // TV progress
        let progressHtml = '';
        if (isTv && entry.currentSeason) {
            progressHtml = `<div class="card-progress">S${entry.currentSeason}${entry.currentEpisode ? ' · E' + entry.currentEpisode : ''}</div>`;
        }

        return `
        <div class="movie-card" onclick='openWatchlistModal(${JSON.stringify(entry).replace(/'/g, "&#39;")}, "${type}")' style="animation-delay: ${i * 0.05}s">
            <div class="watchlist-badge status-${entry.status}">${label}</div>
            <div class="media-type-badge type-${isTv ? 'tv' : 'movie'}">${typeIcon}</div>
            <div class="poster-container">
                ${posterUrl
                    ? `<img class="poster" src="${posterUrl}" alt="${title}" loading="lazy">`
                    : `<div class="poster-placeholder">${typeIcon}</div>`}
                <div class="poster-overlay">
                    <span class="card-rating">
                        <svg viewBox="0 0 24 24" fill="currentColor"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
                        ${rating.toFixed(1)}
                    </span>
                </div>
            </div>
            <div class="card-info">
                <div class="card-title">${title}</div>
                <div class="card-year">${year}</div>
                ${media.genres ? `<div class="card-genres">${media.genres}</div>` : ''}
                ${starsHtml ? `<div class="card-user-rating">${starsHtml}</div>` : ''}
                ${progressHtml}
            </div>
        </div>`;
    }).join('');
}

// ---- Watchlist ----
async function loadWatchlist() {
    const loading = document.getElementById('watchlist-loading');
    loading.style.display = 'flex';

    try {
        if (state.currentWlType === 'movies') {
            const entries = await API.getWatchlist(state.currentFilter, state.currentSort);
            state.movieWatchlist = entries;

            // Build lookup map
            state.movieWatchlistMap = {};
            entries.forEach(e => {
                state.movieWatchlistMap[e.movie.tmdbId] = e;
            });

            // Count
            if (state.currentFilter !== 'all') {
                const all = await API.getWatchlist('all', 'added');
                state.movieWatchlistMap = {};
                all.forEach(e => { state.movieWatchlistMap[e.movie.tmdbId] = e; });
            }

            renderWatchlistGrid(entries, 'movies');
        } else {
            const entries = await API.getTvWatchlist(state.currentFilter, state.currentSort);
            state.tvWatchlist = entries;

            state.tvWatchlistMap = {};
            entries.forEach(e => {
                state.tvWatchlistMap[e.tvShow.tmdbId] = e;
            });

            if (state.currentFilter !== 'all') {
                const all = await API.getTvWatchlist('all', 'added');
                state.tvWatchlistMap = {};
                all.forEach(e => { state.tvWatchlistMap[e.tvShow.tmdbId] = e; });
            }

            renderWatchlistGrid(entries, 'tv');
        }

        // Update total count (both movie + tv)
        await updateWatchlistCount();

    } catch (err) {
        console.error('Failed to load watchlist:', err);
        showToast('Failed to load watchlist', 'error');
    } finally {
        loading.style.display = 'none';
    }
}

async function updateWatchlistCount() {
    try {
        const [movies, tvShows] = await Promise.all([
            API.getWatchlist('all', 'added'),
            API.getTvWatchlist('all', 'added')
        ]);
        state.movieWatchlistMap = {};
        movies.forEach(e => { state.movieWatchlistMap[e.movie.tmdbId] = e; });
        state.tvWatchlistMap = {};
        tvShows.forEach(e => { state.tvWatchlistMap[e.tvShow.tmdbId] = e; });

        document.getElementById('watchlist-count').textContent = movies.length + tvShows.length;
    } catch (err) {
        console.error('Failed to update count:', err);
    }
}

function initWatchlistFilters() {
    document.querySelectorAll('.status-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            document.querySelectorAll('.status-tab').forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            state.currentFilter = tab.dataset.status;
            loadWatchlist();
        });
    });

    document.getElementById('sort-select').addEventListener('change', (e) => {
        state.currentSort = e.target.value;
        loadWatchlist();
    });
}

function initExport() {
    document.getElementById('export-btn').addEventListener('click', () => {
        if (state.currentWlType === 'tv') {
            API.exportTvCsv();
        } else {
            API.exportMovieCsv();
        }
        showToast('Exporting watchlist...', 'info');
    });
}

// ---- Modal ----
function initModal() {
    const overlay = document.getElementById('modal-overlay');
    const closeBtn = document.getElementById('modal-close');

    closeBtn.addEventListener('click', closeModal);
    overlay.addEventListener('click', (e) => {
        if (e.target === overlay) closeModal();
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') closeModal();
    });

    // Stars
    const stars = document.querySelectorAll('.star-rating .star');
    stars.forEach(star => {
        star.addEventListener('mouseenter', () => {
            highlightStars(parseInt(star.dataset.rating));
        });
        star.addEventListener('click', () => {
            state.selectedRating = parseInt(star.dataset.rating);
            setStars(state.selectedRating);
        });
    });

    document.querySelector('.star-rating').addEventListener('mouseleave', () => {
        setStars(state.selectedRating);
    });

    document.getElementById('modal-add-btn').addEventListener('click', addCurrentItem);
    document.getElementById('modal-update-btn').addEventListener('click', updateCurrentEntry);
    document.getElementById('modal-remove-btn').addEventListener('click', removeCurrentEntry);
}

function openMovieModal(item) {
    state.selectedMovie = item;
    state.selectedRating = 0;
    state.isEditMode = false;
    state.editEntryId = null;

    const tmdbId = item.id || item.tmdbId;
    const mediaType = item.mediaType || item.media_type || 'movie';
    const posterPath = item.posterPath || item.poster_path;
    const backdropPath = item.backdropPath || item.backdrop_path;
    const releaseDate = item.releaseDate || item.release_date || item.firstAirDate || item.first_air_date || '';
    const voteAverage = item.voteAverage || item.vote_average || 0;
    const genres = item.genreNames || item.genres || '';
    const title = item.title || item.name || 'Unknown';

    // Type badge
    const typeBadgeEl = document.getElementById('modal-type-badge');
    if (mediaType === 'tv') {
        typeBadgeEl.innerHTML = '📺 TV Series';
        typeBadgeEl.className = 'modal-type-badge type-tv';
        typeBadgeEl.style.display = 'inline-flex';
    } else {
        typeBadgeEl.innerHTML = '🎬 Movie';
        typeBadgeEl.className = 'modal-type-badge type-movie';
        typeBadgeEl.style.display = 'inline-flex';
    }

    // Show/hide TV progress tracking
    const tvProgressGroup = document.getElementById('tv-progress-group');
    if (mediaType === 'tv') {
        tvProgressGroup.style.display = 'block';
    } else {
        tvProgressGroup.style.display = 'none';
    }

    // Set backdrop
    const backdropEl = document.getElementById('modal-backdrop');
    if (backdropPath) {
        backdropEl.style.backgroundImage = `url(${TMDB_IMG}/w1280${backdropPath})`;
    } else if (posterPath) {
        backdropEl.style.backgroundImage = `url(${TMDB_IMG}/w780${posterPath})`;
    } else {
        backdropEl.style.backgroundImage = 'none';
        backdropEl.style.background = 'var(--gradient-card)';
    }

    // Set poster
    const posterEl = document.getElementById('modal-poster');
    posterEl.innerHTML = posterPath
        ? `<img src="${TMDB_IMG}/w500${posterPath}" alt="${title}">`
        : '<div class="poster-placeholder" style="width:100%;height:100%;display:flex;align-items:center;justify-content:center;font-size:48px;background:var(--gradient-card);">🎬</div>';

    document.getElementById('modal-title').textContent = title;
    document.getElementById('modal-year').textContent = releaseDate.substring(0, 4);
    document.getElementById('modal-rating-value').textContent = voteAverage.toFixed(1);

    // Genres
    const genresEl = document.getElementById('modal-genres');
    if (genres) {
        genresEl.innerHTML = genres.split(',').map(g => `<span class="genre-tag">${g.trim()}</span>`).join('');
    } else {
        genresEl.innerHTML = '';
    }

    document.getElementById('modal-overview').textContent = item.overview || 'No overview available.';

    // Check if in watchlist
    const isMovie = mediaType === 'movie';
    const entry = isMovie ? state.movieWatchlistMap[tmdbId] : state.tvWatchlistMap[tmdbId];

    if (entry) {
        state.isEditMode = true;
        state.editEntryId = entry.id;
        document.getElementById('modal-status').value = entry.status;
        state.selectedRating = entry.userRating || 0;
        setStars(state.selectedRating);
        document.getElementById('modal-notes').value = entry.notes || '';

        if (mediaType === 'tv') {
            document.getElementById('modal-current-season').value = entry.currentSeason || 1;
            document.getElementById('modal-current-episode').value = entry.currentEpisode || 1;
        }

        document.getElementById('modal-add-btn').style.display = 'none';
        document.getElementById('modal-update-btn').style.display = 'inline-flex';
        document.getElementById('modal-remove-btn').style.display = 'inline-flex';
    } else {
        document.getElementById('modal-status').value = 'plan_to_watch';
        setStars(0);
        document.getElementById('modal-notes').value = '';

        if (mediaType === 'tv') {
            document.getElementById('modal-current-season').value = 1;
            document.getElementById('modal-current-episode').value = 1;
        }

        document.getElementById('modal-add-btn').style.display = 'inline-flex';
        document.getElementById('modal-update-btn').style.display = 'none';
        document.getElementById('modal-remove-btn').style.display = 'none';
    }

    document.getElementById('modal-overlay').classList.add('active');
    document.body.style.overflow = 'hidden';
}

function openWatchlistModal(entry, type) {
    const isTv = type === 'tv';
    const media = isTv ? entry.tvShow : entry.movie;

    const itemForModal = {
        id: media.tmdbId,
        tmdbId: media.tmdbId,
        title: isTv ? media.name : media.title,
        name: isTv ? media.name : undefined,
        overview: media.overview,
        posterPath: media.posterPath || media.poster_path,
        backdropPath: media.backdropPath || media.backdrop_path,
        releaseDate: isTv ? (media.firstAirDate || media.first_air_date) : (media.releaseDate || media.release_date),
        voteAverage: media.voteAverage || media.vote_average,
        genreNames: media.genres,
        mediaType: isTv ? 'tv' : 'movie',
        numberOfSeasons: isTv ? media.numberOfSeasons : undefined,
        numberOfEpisodes: isTv ? media.numberOfEpisodes : undefined
    };

    // Make sure the map has this entry
    if (isTv) {
        state.tvWatchlistMap[media.tmdbId] = entry;
    } else {
        state.movieWatchlistMap[media.tmdbId] = entry;
    }

    openMovieModal(itemForModal);
}

function closeModal() {
    document.getElementById('modal-overlay').classList.remove('active');
    document.body.style.overflow = '';
}

function highlightStars(rating) {
    document.querySelectorAll('.star-rating .star').forEach(s => {
        const r = parseInt(s.dataset.rating);
        s.classList.remove('active', 'hovered');
        if (r <= rating) s.classList.add('hovered');
    });
}

function setStars(rating) {
    document.querySelectorAll('.star-rating .star').forEach(s => {
        const r = parseInt(s.dataset.rating);
        s.classList.remove('active', 'hovered');
        if (r <= rating) s.classList.add('active');
    });
}

// ---- CRUD Actions ----
async function addCurrentItem() {
    const item = state.selectedMovie;
    if (!item) return;

    const mediaType = item.mediaType || item.media_type || 'movie';
    const isTV = mediaType === 'tv';
    const title = item.title || item.name || 'Unknown';

    const data = {
        tmdbId: item.id || item.tmdbId,
        title: title,
        genres: item.genreNames || item.genres || '',
        posterPath: item.posterPath || item.poster_path || '',
        backdropPath: item.backdropPath || item.backdrop_path || '',
        overview: item.overview || '',
        releaseDate: item.releaseDate || item.release_date || item.firstAirDate || item.first_air_date || '',
        voteAverage: item.voteAverage || item.vote_average || 0,
        status: document.getElementById('modal-status').value,
        userRating: state.selectedRating > 0 ? state.selectedRating : null,
        notes: document.getElementById('modal-notes').value || null,
        mediaType: mediaType
    };

    if (isTV) {
        data.currentSeason = parseInt(document.getElementById('modal-current-season').value) || null;
        data.currentEpisode = parseInt(document.getElementById('modal-current-episode').value) || null;
        data.numberOfSeasons = item.numberOfSeasons || item.number_of_seasons || null;
        data.numberOfEpisodes = item.numberOfEpisodes || item.number_of_episodes || null;
    }

    try {
        if (isTV) {
            const entry = await API.addToTvWatchlist(data);
            state.tvWatchlistMap[data.tmdbId] = entry;
        } else {
            const entry = await API.addToWatchlist(data);
            state.movieWatchlistMap[data.tmdbId] = entry;
        }
        showToast(`"${title}" added to watchlist!`, 'success');
        closeModal();
        loadWatchlist();

        // Re-render search
        if (state.currentTab === 'search' && state.searchResults.length > 0) {
            renderMovieGrid(state.searchResults, 'search-results');
        }
    } catch (err) {
        console.error('Add error:', err);
        showToast('Failed to add to watchlist', 'error');
    }
}

async function updateCurrentEntry() {
    if (!state.editEntryId) return;

    const item = state.selectedMovie;
    const mediaType = item?.mediaType || item?.media_type || 'movie';
    const isTV = mediaType === 'tv';

    const data = {
        status: document.getElementById('modal-status').value,
        userRating: state.selectedRating > 0 ? state.selectedRating : null,
        notes: document.getElementById('modal-notes').value || null
    };

    if (isTV) {
        data.currentSeason = parseInt(document.getElementById('modal-current-season').value) || null;
        data.currentEpisode = parseInt(document.getElementById('modal-current-episode').value) || null;
    }

    try {
        if (isTV) {
            await API.updateTvWatchlist(state.editEntryId, data);
        } else {
            await API.updateWatchlist(state.editEntryId, data);
        }
        showToast('Watchlist entry updated!', 'success');
        closeModal();
        loadWatchlist();
    } catch (err) {
        console.error('Update error:', err);
        showToast('Failed to update', 'error');
    }
}

async function removeCurrentEntry() {
    if (!state.editEntryId) return;

    const item = state.selectedMovie;
    const mediaType = item?.mediaType || item?.media_type || 'movie';
    const isTV = mediaType === 'tv';
    const title = item?.title || item?.name || 'Item';

    try {
        if (isTV) {
            await API.removeFromTvWatchlist(state.editEntryId);
        } else {
            await API.removeFromWatchlist(state.editEntryId);
        }

        const tmdbId = item?.id || item?.tmdbId;
        if (tmdbId) {
            if (isTV) {
                delete state.tvWatchlistMap[tmdbId];
            } else {
                delete state.movieWatchlistMap[tmdbId];
            }
        }

        showToast(`"${title}" removed from watchlist`, 'success');
        closeModal();
        loadWatchlist();

        if (state.currentTab === 'search' && state.searchResults.length > 0) {
            renderMovieGrid(state.searchResults, 'search-results');
        }
    } catch (err) {
        console.error('Remove error:', err);
        showToast('Failed to remove', 'error');
    }
}

// ---- Recommendations ----
async function loadRecommendations() {
    const loading = document.getElementById('recommendations-loading');
    loading.style.display = 'flex';
    document.getElementById('recommendations-results').innerHTML = '';

    try {
        let results;
        if (state.currentRecType === 'tv') {
            results = await API.getTvRecommendations();
        } else {
            results = await API.getMovieRecommendations();
        }
        renderMovieGrid(results, 'recommendations-results');
    } catch (err) {
        console.error('Recommendations error:', err);
        showToast('Failed to load recommendations', 'error');
    } finally {
        loading.style.display = 'none';
    }
}

// ---- Toast Notifications ----
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const icons = { success: '✅', error: '❌', info: 'ℹ️' };

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `<span class="toast-icon">${icons[type]}</span>${message}`;
    container.appendChild(toast);

    setTimeout(() => {
        if (toast.parentNode) toast.remove();
    }, 3000);
}
