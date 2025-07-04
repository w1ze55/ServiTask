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
import com.servitask.repository.BoardRepository;

import java.util.Optional;
import java.util.Map;
import java.util.Random;
import java.util.HashMap;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtService jwtService;

    @Autowired
    @Lazy
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private BoardRepository boardRepository;

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
        String passwordValidationError = getPasswordValidationErrorForRegister(registerRequest.getPassword());
        if (passwordValidationError != null) {
            throw new UserException(passwordValidationError, HttpStatus.BAD_REQUEST);
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

        if (!HashUtil.verifyPassword(currentPassword, user.getPassword())) {
            throw new UserException("A senha atual não confere. Verifique e tente novamente.", HttpStatus.UNAUTHORIZED);
        }

        if (HashUtil.verifyPassword(newPassword, user.getPassword())) {
            throw new UserException("A nova senha deve ser diferente da senha atual.", HttpStatus.BAD_REQUEST);
        }

        String passwordValidationError = getPasswordValidationError(newPassword);
        if (passwordValidationError != null) {
            throw new UserException(passwordValidationError, HttpStatus.BAD_REQUEST);
        }

        user.setPassword(HashUtil.hashPassword(newPassword));
        userRepository.save(user);
    }
    
    private String getPasswordValidationErrorForRegister(String password) {
        if (password == null) {
            return "Senha é obrigatória.";
        }
        
        if (password.length() < 8) {
            return "Senha deve ter pelo menos 8 caracteres.";
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
            return "Senha deve conter pelo menos uma letra minúscula (a-z).";
        }
        
        if (!hasUpper) {
            return "Senha deve conter pelo menos uma letra maiúscula (A-Z).";
        }
        
        if (!hasDigit) {
            return "Senha deve conter pelo menos um número (0-9).";
        }
        
        if (!hasSpecial) {
            return "Senha deve conter pelo menos um caractere especial (!@#$%^&*).";
        }
        
        return null;
    }

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
        
        return null;
    }

    @Transactional
    public void removePhoto(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException("Usuário não encontrado", HttpStatus.NOT_FOUND));

        user.setPhoto(null);
        userRepository.save(user);
    }

    private final Map<String, String> deleteConfirmationMessages = new HashMap<>();
    
    public String generateDeleteConfirmationMessage(String email) {
        String[] messages = {
            "EXCLUIR MINHA CONTA PERMANENTEMENTE",
            "DELETAR TODOS OS MEUS DADOS",
            "REMOVER CONTA DEFINITIVAMENTE",
            "APAGAR PERFIL COMPLETO",
            "ELIMINAR CONTA PARA SEMPRE",
            "DESTRUIR DADOS PESSOAIS",
            "CANCELAR CONTA DEFINITIVO",
            "EXCLUIR PERFIL TOTAL"
        };
        
        Random random = new Random();
        String selectedMessage = messages[random.nextInt(messages.length)];
        
        deleteConfirmationMessages.put(email, selectedMessage);
        
        return selectedMessage;
    }
    
    @Transactional
    public void deleteAccount(String email, String confirmationMessage) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException("Usuário não encontrado", HttpStatus.NOT_FOUND));

        String expectedMessage = deleteConfirmationMessages.get(email);
        
        if (expectedMessage == null) {
            throw new UserException("Sessão de confirmação expirada. Gere uma nova mensagem de confirmação.", HttpStatus.BAD_REQUEST);
        }
        
        if (!expectedMessage.equals(confirmationMessage)) {
            throw new UserException("Mensagem de confirmação incorreta. Digite exatamente como mostrado.", HttpStatus.BAD_REQUEST);
        }

        deleteConfirmationMessages.remove(email);

        boardRepository.deleteAll(boardRepository.findByUserOrderByCreatedAtDesc(user));

        userRepository.delete(user);
    }

    @Transactional
    public String uploadPhotoFromBase64(String email, String base64Photo, String contentType, String filename) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException("Usuário não encontrado", HttpStatus.NOT_FOUND));
        
        String processedPhoto = processBase64Image(base64Photo);
        
        user.setPhoto(processedPhoto);
        userRepository.save(user);
        
        return processedPhoto;
    }
    
    private String processBase64Image(String base64Photo) {
        int maxBase64Length = 1024 * 1024;
        
        if (base64Photo.length() > maxBase64Length) {
            try {
                String[] parts = base64Photo.split(",");
                if (parts.length == 2) {
                    String header = parts[0];
                    String data = parts[1];

                    int maxDataLength = maxBase64Length - header.length() - 1;
                    if (data.length() > maxDataLength) {
                        data = data.substring(0, maxDataLength);
                    }
                    
                    String result = header + "," + data;
                    return result;
                }
            } catch (Exception e) {
                return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
            }
        }

        return base64Photo;
    }
}
