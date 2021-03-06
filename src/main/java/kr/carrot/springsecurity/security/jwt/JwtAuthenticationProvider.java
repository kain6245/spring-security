package kr.carrot.springsecurity.security.jwt;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import kr.carrot.springsecurity.app.entity.SessionEntity;
import kr.carrot.springsecurity.app.repository.SessionRepository;
import kr.carrot.springsecurity.security.exception.InvalidJwtTokenException;
import kr.carrot.springsecurity.security.exception.JwtTokenTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.security.Key;
import java.util.Date;

@Slf4j
public class JwtAuthenticationProvider implements AuthTokenProvider<JwtAuthToken> {

    private final static long ACCESS_TOKEN_VALID_TIME = 1000L * 60 * 30; // 30분
    private final static long REFRESH_TOKEN_VALID_TIME = 1000L * 60 * 60 * 3; // 3시간

    private final Key key;
    private final UserDetailsService userDetailsService;
    private final SessionRepository sessionRepository;

    public JwtAuthenticationProvider(String base64Secret, UserDetailsService userDetailsService, SessionRepository sessionRepository) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.userDetailsService = userDetailsService;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public JwtAuthToken createAuthToken(String sessionId, TokenType tokenType) {

        if (tokenType == TokenType.ACCESS_TOKEN) {
            Date expiredDate = new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALID_TIME);
            return new JwtAuthToken(sessionId, expiredDate, key);
        } //
        else if (tokenType == TokenType.REFRESH_TOKEN) {
            Date expiredDate = new Date(System.currentTimeMillis() + REFRESH_TOKEN_VALID_TIME);
            return new JwtAuthToken(sessionId, expiredDate, key);
        } //
        else {
            throw new JwtTokenTypeException();
        }
    }

    @Override
    public JwtAuthToken convertAuthToken(String token) {
        return new JwtAuthToken(token, key);
    }

    @Override
    public Authentication getAuthentication(JwtAuthToken authToken) {

        if(!authToken.validate()) {
            throw new InvalidJwtTokenException();
        }

        // get user from session
        String sessionId = authToken.getSessionId();
        SessionEntity sessionEntity = sessionRepository.findBySessionId(sessionId) // todo: change session service
                .orElseThrow();
        if (sessionEntity.isExpired()) {
            throw new RuntimeException("session expired"); // todo: change exception type
        }

        // get user details
        String username = sessionEntity.getUsername();
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // set authentication true
        // authentication manager 또는 authentication provider에서 충분한 인증/인가가 이루어졌을때만 사용해야 하는 생성자.
        // method desc 참고
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public JwtAuthToken refreshingAccessToken(String refreshToken, String clientId) {

        // get username
        JwtAuthToken jwtAuthToken = new JwtAuthToken(refreshToken, key);
        String sessionId = jwtAuthToken.getSessionId();

        // todo: check session expiration

        // create new token
        return createAuthToken(sessionId, TokenType.ACCESS_TOKEN);
    }
}
