package au.edu.rmit.sept.webapp.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File-based implementation of PersistentTokenRepository for development profile.
 * Stores remember-me tokens in a JSON file instead of database.
 * Thread-safe and suitable for single-instance development environments.
 * Bean is created manually in SecurityConfig with profile-specific logic.
 */
public class FileBasedTokenRepository implements PersistentTokenRepository {

    private static final String TOKEN_FILE_PATH = "./persistent-tokens.json";
    private final ObjectMapper objectMapper;
    private final Map<String, TokenData> tokens;

    public FileBasedTokenRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.tokens = new ConcurrentHashMap<>();
        loadTokensFromFile();
    }

    @Override
    public synchronized void createNewToken(PersistentRememberMeToken token) {
        tokens.put(token.getSeries(), new TokenData(
                token.getUsername(),
                token.getSeries(),
                token.getTokenValue(),
                token.getDate().getTime()
        ));
        saveTokensToFile();
    }

    @Override
    public synchronized void updateToken(String series, String tokenValue, Date lastUsed) {
        TokenData tokenData = tokens.get(series);
        if (tokenData != null) {
            tokenData.tokenValue = tokenValue;
            tokenData.lastUsed = lastUsed.getTime();
            saveTokensToFile();
        }
    }

    @Override
    public synchronized PersistentRememberMeToken getTokenForSeries(String seriesId) {
        TokenData tokenData = tokens.get(seriesId);
        if (tokenData == null) {
            return null;
        }
        return new PersistentRememberMeToken(
                tokenData.username,
                tokenData.series,
                tokenData.tokenValue,
                new Date(tokenData.lastUsed)
        );
    }

    @Override
    public synchronized void removeUserTokens(String username) {
        tokens.entrySet().removeIf(entry -> entry.getValue().username.equals(username));
        saveTokensToFile();
    }

    private void loadTokensFromFile() {
        File file = new File(TOKEN_FILE_PATH);
        if (file.exists()) {
            try {
                Map<String, TokenData> loadedTokens = objectMapper.readValue(
                        file,
                        new TypeReference<Map<String, TokenData>>() {}
                );
                tokens.putAll(loadedTokens);
            } catch (IOException e) {
                System.err.println("Failed to load tokens from file: " + e.getMessage());
            }
        }
    }

    private void saveTokensToFile() {
        try {
            objectMapper.writeValue(new File(TOKEN_FILE_PATH), tokens);
        } catch (IOException e) {
            System.err.println("Failed to save tokens to file: " + e.getMessage());
        }
    }

    /**
     * Internal data structure for JSON serialization.
     */
    public static class TokenData {
        public String username;
        public String series;
        public String tokenValue;
        public long lastUsed;

        // Default constructor for Jackson
        public TokenData() {
        }

        public TokenData(String username, String series, String tokenValue, long lastUsed) {
            this.username = username;
            this.series = series;
            this.tokenValue = tokenValue;
            this.lastUsed = lastUsed;
        }
    }
}
