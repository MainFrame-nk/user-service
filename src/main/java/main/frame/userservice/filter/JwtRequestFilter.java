package main.frame.userservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.frame.userservice.service.UserDetailsService;
import main.frame.userservice.utils.JwtUtil;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

//@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtRequestFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
//            throws ServletException, IOException {
//        final String authHeader = request.getHeader("Authorization");
//
//        if (authHeader != null && authHeader.startsWith("Bearer ")) {
//            String token = authHeader.substring(7);
//            if (!jwtUtil.validateToken(token)) {
//                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
//                return;
//            }
//        }
//
//        chain.doFilter(request, response);
//    }
@Override
public void doFilterInternal(@Nullable HttpServletRequest request,
                             @Nullable HttpServletResponse response,
                             @Nullable FilterChain filterChain)
        throws ServletException, IOException {
    // Логируем все заголовки
// Проверка на null перед выполнением
    if (request == null || response == null || filterChain == null) {
        System.out.println("Некорректный запрос: request, response или filterChain == null");
        return;
    }

    System.out.println("Фильтр вызван для URI: " + request.getRequestURI());

    Enumeration<String> headerNames = request.getHeaderNames();
    if (headerNames != null) {
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            System.out.println("Header: " + header + " -> " + request.getHeader(header));
        }
    }

    final String authorizationHeader = request.getHeader("Authorization");

    String email = null;
    String jwt = null;

    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
        jwt = authorizationHeader.substring(7);
        email = jwtUtil.extractEmail(jwt);
        System.out.println("JWT: " + jwt);  // Добавь логирование
        System.out.println("Email из токена: " + email);
    } else {
        System.out.println("Authorization header не найден или неправильного формата");
    }

    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(email);
        System.out.println("UserDetails загружен для: " + email);
        if (jwtUtil.validateToken(jwt)) {
            System.out.println("Токен валиден для пользователя: " + email);
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        } else {
            System.out.println("Токен недействителен для пользователя: " + email);
        }
    }

//    final String authHeader = request.getHeader("Authorization");
//    if (authHeader != null && authHeader.startsWith("Bearer ")) {
//        String token = authHeader.substring(7);
//        if (jwtUtil.validateToken(token)) {
//            // Извлекаем email и роли из токена
//            String email = jwtUtil.extractEmail(token);
//            List<String> roles = jwtUtil.extractRoles(token); // Предполагаем, что есть такой метод
//            System.out.println("Email из токена: " + email);
//            System.out.println("Роли из токена: " + roles);
//
//            // Создаем список SimpleGrantedAuthority
//            List<SimpleGrantedAuthority> authorities = roles.stream()
//                    .map(SimpleGrantedAuthority::new)
//                    .toList();
//
//            // Настраиваем SecurityContext
//            UsernamePasswordAuthenticationToken authentication =
//                    new UsernamePasswordAuthenticationToken(email, null, authorities);
//            SecurityContextHolder.getContext().setAuthentication(authentication);
//        }
//    } else {
//        System.out.println("Authorization header не найден или неправильного формата! " + authHeader);
//    }

    filterChain.doFilter(request, response);
}

}