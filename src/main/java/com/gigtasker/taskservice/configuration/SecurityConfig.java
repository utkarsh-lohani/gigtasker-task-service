package com.gigtasker.taskservice.configuration;

import org.gigtasker.common.security.GigTaskerSecurity;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(GigTaskerSecurity.class)
public class SecurityConfig {
}
