package com.manager.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manager.taskmanager.dto.LoginRequestDTO;
import com.manager.taskmanager.dto.RegisterRequestDTO;
import com.manager.taskmanager.model.Role;
import com.manager.taskmanager.model.User;
import com.manager.taskmanager.repository.UserRepository;
import com.manager.taskmanager.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserDetailsService userDetailsService;

    private User testUser;
    private RegisterRequestDTO registerRequestDTO;
    private LoginRequestDTO loginRequestDTO;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        registerRequestDTO = RegisterRequestDTO.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("Password123")
                .role(Role.USER)
                .build();

        loginRequestDTO = LoginRequestDTO.builder()
                .username("testuser")
                .password("Password123")
                .build();

        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser")
                .password("encodedPassword")
                .roles("USER")
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/register - Should register user successfully")
    void register_Success() throws Exception {
        when(userRepository.findByUsername(registerRequestDTO.getUsername()))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(registerRequestDTO.getEmail()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString()))
                .thenReturn("encodedPassword");
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenReturn(userDetails);
        when(jwtUtil.generateToken(any(UserDetails.class), anyString()))
                .thenReturn("jwt-token");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequestDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.message").value("Registration successful!"));

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 when username already exists")
    void register_UsernameExists() throws Exception {
        when(userRepository.findByUsername(registerRequestDTO.getUsername()))
                .thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequestDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already exists!"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 when email already exists")
    void register_EmailExists() throws Exception {
        when(userRepository.findByUsername(registerRequestDTO.getUsername()))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(registerRequestDTO.getEmail()))
                .thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequestDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists!"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 when password is invalid")
    void register_InvalidPassword() throws Exception {
        RegisterRequestDTO invalidPasswordDTO = RegisterRequestDTO.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("password") // No uppercase or digit
                .role(Role.USER)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPasswordDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("Password must contain at least one uppercase letter, one lowercase letter, and one digit"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 when password is too short")
    void register_PasswordTooShort() throws Exception {
        RegisterRequestDTO shortPasswordDTO = RegisterRequestDTO.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("Pass1") // Less than 8 characters
                .role(Role.USER)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPasswordDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 when username is too short")
    void register_UsernameTooShort() throws Exception {
        RegisterRequestDTO shortUsernameDTO = RegisterRequestDTO.builder()
                .username("ab") // Less than 3 characters
                .email("newuser@example.com")
                .password("Password123")
                .role(Role.USER)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortUsernameDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").value("Username must be between 3 and 50 characters"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 when email is invalid")
    void register_InvalidEmail() throws Exception {
        RegisterRequestDTO invalidEmailDTO = RegisterRequestDTO.builder()
                .username("newuser")
                .email("invalid-email") // Invalid email format
                .password("Password123")
                .role(Role.USER)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmailDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("Email should be valid"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should login successfully")
    void login_Success() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername(loginRequestDTO.getUsername()))
                .thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(any(UserDetails.class), anyString()))
                .thenReturn("jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.message").value("Login successful!"));

        verify(authenticationManager, times(1)).authenticate(any());
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 401 with invalid credentials")
    void login_InvalidCredentials() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestDTO)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password!"));

        verify(authenticationManager, times(1)).authenticate(any());
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 400 when username is blank")
    void login_BlankUsername() throws Exception {
        LoginRequestDTO blankUsernameDTO = LoginRequestDTO.builder()
                .username("")
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankUsernameDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").value("Username is required"));

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 400 when password is blank")
    void login_BlankPassword() throws Exception {
        LoginRequestDTO blankPasswordDTO = LoginRequestDTO.builder()
                .username("testuser")
                .password("")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankPasswordDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("Password is required"));

        verify(authenticationManager, never()).authenticate(any());
    }
}