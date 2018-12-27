package com.secureskytech.demochatasync;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().antMatchers("/", "/sse-demo/**").permitAll().anyRequest().authenticated().and()
                .formLogin().loginPage("/login").permitAll().and().logout().permitAll();
    }

    /* https://docs.spring.io/spring-security/site/docs/5.1.2.RELEASE/reference/htmlsingle/#hello-web-security-java-configuration
     * などで userDetailsService() を override しているが、
     * 今回は「ログインを試みたユーザ全てを認証OKとする」ため、overrideしない。
     * -> AuthenticationManager.authenticate() が認証すれば誰でも入れるようにしておく。
     * さらに authenticationManager() について無条件に認証OKとする DummyAuthenticationManager 
     * を返すことで、事実上「誰でも好きなIDでログインOK」という状態にする。
     */

    static class DummyAuthenticationManager implements AuthenticationManager {
        static final List<GrantedAuthority> AUTHORITIES = Arrays.asList(new SimpleGrantedAuthority("USER"));

        public Authentication authenticate(Authentication auth) throws AuthenticationException {
            // credential のチェックは行わず、誰でも認証OKのtokenを返す。
            return new UsernamePasswordAuthenticationToken(auth.getName(), auth.getCredentials(), AUTHORITIES);
        }
    }

    @Override
    protected AuthenticationManager authenticationManager() throws Exception {
        return new DummyAuthenticationManager();
    }

}