package io.nitro.antlers.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import io.nitro.antlers.views.login.LoginView;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class SecurityConfiguration extends VaadinWebSecurity {
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    super.configure(http);                      // добавит anyRequest().authenticated()
    setLoginView(http, LoginView.class);
  }
}

