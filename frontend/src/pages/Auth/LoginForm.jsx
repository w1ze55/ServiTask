import React, { useState } from 'react';
import Icon from '../../utils/iconMapping.jsx';
import apiService from '../../services/api';

export default function LoginForm({ onLogin, onToggleMode }) {
  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    if (error) setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!formData.email.trim() || !formData.password.trim()) {
      setError('Por favor, preencha todos os campos obrigatórios.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const response = await apiService.login(formData.email, formData.password);
      console.log('Login bem-sucedido:', response);
      onLogin();
    } catch (error) {
      console.error('Erro no login:', error);
      setError(error.message || 'Erro ao fazer login. Verifique suas credenciais.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <div className="form-header">
        <h3>Entrar</h3>
        <p>Entre com suas credenciais</p>
      </div>

      <form className="login-form" onSubmit={handleSubmit}>
        {error && (
          <div style={{ 
            background: '#fee2e2', 
            color: '#dc2626', 
            padding: '0.75rem', 
            borderRadius: '8px', 
            marginBottom: '1rem',
            fontSize: '0.9rem',
            border: '1px solid #fecaca'
          }}>
            {error}
          </div>
        )}

        <div className="form-group">
          <label htmlFor="email">E-mail *</label>
          <input
            type="email"
            id="email"
            name="email"
            value={formData.email}
            onChange={handleInputChange}
            placeholder="Digite seu e-mail"
            required
            disabled={loading}
          />
        </div>

        <div className="form-group">
          <label htmlFor="password">Senha *</label>
          <input
            type="password"
            id="password"
            name="password"
            value={formData.password}
            onChange={handleInputChange}
            placeholder="Digite sua senha"
            required
            disabled={loading}
          />
        </div>

        <button type="submit" className="submit-btn" disabled={loading}>
          {loading ? <><Icon emoji="🔄" /> Entrando...</> : <><Icon emoji="🔑" /> Entrar</>}
        </button>

        <div className="forgot-password">
          <a href="#forgot">Esqueceu sua senha?</a>
        </div>

        <div className="toggle-mode">
          <p>
            Não tem uma conta?
            <button type="button" onClick={onToggleMode} className="toggle-btn">
              Criar conta
            </button>
          </p>
        </div>
      </form>
    </>
  );
} 