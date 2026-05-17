# Movie & TV Show Watchlist

A personal full-stack web application I built to discover and track movies and TV shows. It connects to the TMDB API to search for media and uses a Java Spring Boot backend with a MySQL database to save my watchlist. The frontend is built using simple HTML, CSS, and JavaScript.

## Setup

1. Clone the repo:
   ```bash
   git clone https://github.com/egesorhan/movie-watchlist.git
   ```
2. Create a MySQL database and update `src/main/resources/application.properties` with your database credentials.
3. Add your TMDB API key to `application.properties`.
4. Run the Spring Boot application and open `http://localhost:8080` in your browser.
