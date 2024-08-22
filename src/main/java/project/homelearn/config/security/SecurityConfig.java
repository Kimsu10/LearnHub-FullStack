package project.homelearn.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import project.homelearn.filter.CustomLoginFilter;
import project.homelearn.filter.CustomLogoutFilter;
import project.homelearn.filter.JwtFilter;
import project.homelearn.repository.curriculum.CurriculumRepository;
import project.homelearn.repository.user.AttendanceRepository;
import project.homelearn.repository.user.LoginHistoryRepository;
import project.homelearn.repository.user.UserRepository;
import project.homelearn.service.jwt.CookieService;
import project.homelearn.service.jwt.JwtService;
import project.homelearn.service.common.RedisService;
import project.homelearn.service.student.BadgeService;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final CookieService cookieService;
    private final RedisService redisService;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final LoginHistoryRepository loginHistoryRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final BadgeService badgeService;
    private final CurriculumRepository curriculumRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // CSRF 설정
//        http.csrf(csrf -> csrf
//                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//        );

        http.csrf(csrf -> csrf.disable());

        http.formLogin(auth -> auth.disable());
        http.httpBasic(auth -> auth.disable());

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register/**", "/code-verify","/reissue", "/csrf-token", "/gs-guide-websocket/**", "/topic/progress/**", "/account/*", "/image/**", "/attach-file/**").permitAll()
                .requestMatchers("/manager/**").hasRole("MANAGER")
                .requestMatchers("/teacher/**").hasRole("TEACHER")
                .requestMatchers("/student/**").hasRole("STUDENT")
                .anyRequest().authenticated()
        );

        // JWT 사용 위해 세션을 무상태로 설정
        http.sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 커스텀 필터 등록 (JwtFilter -> CustomLoginFilter -> CustomLogoutFilter -> LogoutFilter)
        http.addFilterBefore(new JwtFilter(jwtService), CustomLoginFilter.class);
        http.addFilterAt(new CustomLoginFilter(jwtService, cookieService, redisService, authenticationManager(authenticationConfiguration), loginHistoryRepository, userRepository, attendanceRepository, badgeService, curriculumRepository),
                UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new CustomLogoutFilter(jwtService, redisService, cookieService), LogoutFilter.class);

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}