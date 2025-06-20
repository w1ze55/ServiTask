package com.servitask.servitask.service;

import com.servitask.servitask.dto.AuthResponse;
import com.servitask.servitask.dto.LoginRequest;
import com.servitask.servitask.dto.RegisterRequest;
import com.servitask.servitask.entity.User;
import com.servitask.servitask.repository.UserRepository;
import com.servitask.servitask.util.HashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.servitask.servitask.exception.UserException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.Base64;
import java.io.IOException;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtService jwtService;

    @Autowired
    @Lazy
    private AuthenticationManager authenticationManager;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com email: " + username));
    }

    public AuthResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        String jwtToken = jwtService.generateToken(user);

        return new AuthResponse(jwtToken, user);
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        // Validação de senha coincidente
        if (!registerRequest.isPasswordMatching()) {
            throw new UserException("As senhas não coincidem", HttpStatus.BAD_REQUEST);
        }

        // Validação de email único
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new UserException("Este email já está em uso", HttpStatus.CONFLICT);
        }

        // Validação de nome
        if (registerRequest.getName() == null || registerRequest.getName().trim().length() < 2) {
            throw new UserException("Nome deve ter pelo menos 2 caracteres", HttpStatus.BAD_REQUEST);
        }

        // Validação adicional de senha
        if (!isValidPassword(registerRequest.getPassword())) {
            throw new UserException("Senha deve conter pelo menos: 1 letra minúscula, 1 maiúscula, 1 número e 1 caractere especial", HttpStatus.BAD_REQUEST);
        }

        User user = new User(
                registerRequest.getName().trim(),
                registerRequest.getEmail().toLowerCase().trim(),
                HashUtil.hashPassword(registerRequest.getPassword())
        );

        User savedUser = userRepository.save(user);
        
        String jwtToken = jwtService.generateToken(savedUser);
        
        return new AuthResponse(jwtToken, savedUser);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com id: " + id));
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User getUserFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT não fornecido ou formato inválido");
        }

        String token = authHeader.substring(7);

        String email = jwtService.extractUsername(token);

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
    
    @Transactional
    public User updateProfile(String email, String name) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        user.setName(name);
        return userRepository.save(user);
    }
    
    @Transactional
    public void updatePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException("Usuário não encontrado", HttpStatus.NOT_FOUND));
        
        // Verificar se a senha atual está correta
        if (!HashUtil.verifyPassword(currentPassword, user.getPassword())) {
            throw new UserException("A senha atual não confere. Verifique e tente novamente.", HttpStatus.UNAUTHORIZED);
        }
        
        // Verificar se a nova senha é diferente da atual
        if (HashUtil.verifyPassword(newPassword, user.getPassword())) {
            throw new UserException("A nova senha deve ser diferente da senha atual.", HttpStatus.BAD_REQUEST);
        }
        
        // Validar nova senha com mensagem específica
        String passwordValidationError = getPasswordValidationError(newPassword);
        if (passwordValidationError != null) {
            throw new UserException(passwordValidationError, HttpStatus.BAD_REQUEST);
        }
        
        // Criptografar e salvar a nova senha
        user.setPassword(HashUtil.hashPassword(newPassword));
        userRepository.save(user);
    }
    
    /**
     * Valida se a senha atende aos critérios de segurança
     */
    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        
        return hasLower && hasUpper && hasDigit && hasSpecial;
    }
    
    /**
     * Retorna uma mensagem específica sobre o que está faltando na senha
     */
    private String getPasswordValidationError(String password) {
        if (password == null) {
            return "A nova senha é obrigatória.";
        }
        
        if (password.length() < 8) {
            return "A nova senha deve ter pelo menos 8 caracteres.";
        }
        
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        
        if (!hasLower) {
            return "A nova senha deve conter pelo menos uma letra minúscula (a-z).";
        }
        
        if (!hasUpper) {
            return "A nova senha deve conter pelo menos uma letra maiúscula (A-Z).";
        }
        
        if (!hasDigit) {
            return "A nova senha deve conter pelo menos um número (0-9).";
        }
        
        if (!hasSpecial) {
            return "A nova senha deve conter pelo menos um caractere especial (!@#$%^&*).";
        }
        
        return null; // Senha válida
    }

    @Transactional
    public String uploadPhoto(String email, MultipartFile file) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException("Usuário não encontrado", HttpStatus.NOT_FOUND));
        
        // Converter arquivo para Base64
        byte[] fileBytes = file.getBytes();
        String base64Photo = "data:" + file.getContentType() + ";base64," + Base64.getEncoder().encodeToString(fileBytes);
        
        // Salvar no banco de dados
        user.setPhoto(base64Photo);
        userRepository.save(user);
        
        return base64Photo;
    }
}
