package io.nitro.antlers.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import io.nitro.antlers.views.auth.LoginView;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class SecurityConfiguration extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // ПУБЛИЧНО: корень, логин-роут, наш js-модуль и ВАЖНО — вся статика Vaadin
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/", "/auth", "/login",
                "/firebase-app.js",
                "/images/**", "/icons/**",
                "/manifest.webmanifest", "/sw.js", "/offline.html",
                "/h2-console/**",
                "/VAADIN/**",
                "/frontend/**",
                "/webjars/**",
                "/line-awesome/**"
            ).permitAll()
        );

        // H2 в iframe (не обязательно, но полезно)
        http.headers(h -> h.frameOptions(f -> f.sameOrigin()));

        // Vaadin сам добавит anyRequest().authenticated() и свои фильтры
        super.configure(http);

        // Страница логина — наш Vaadin-роут
        setLoginView(http, LoginView.class);
    }
}
