package vergilius;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter
{

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http
            .authorizeRequests()
                .antMatchers("/", "/os/{osname:.+}", "/os/{osname:.+}/type/{name}", "/about", "/kernels", "/kernelsx86", "/kernelsx86/{famname:.+}", "/kernelsx64", "/kernelsx64/{famname:.+}","/arch", "/kernels/{famname:.+}").permitAll()
                .anyRequest().authenticated()
                .and()
            .formLogin()
                .loginPage("/login")
                .permitAll()
                .and()
            .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/");
    }
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/css/*.css", "/images/*.png", "/images/*.gif", "/images/*.jpg", "/images/*.ico");
        web.ignoring().antMatchers("/js/*.js");
    }

    @Bean
    @Override
    public UserDetailsService userDetailsService() {
        UserDetails user =
                User.withUsername("username")
                        .password("password")
                        .roles("USER")
                        .build();

        return new InMemoryUserDetailsManager(Arrays.asList(user));
    }
}
