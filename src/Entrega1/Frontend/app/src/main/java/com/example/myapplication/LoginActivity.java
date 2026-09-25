package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.network.ApiClient;
import com.example.myapplication.network.LoginRequest;
import com.example.myapplication.network.LoginResponse;
import com.example.myapplication.network.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    ImageButton btnVoltar;
    Button btnLogin, btnCadastro;
    EditText LoginEmail, LoginSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnVoltar = findViewById(R.id.btnVoltar);
        btnLogin = findViewById(R.id.btnLogin);
        btnCadastro = findViewById(R.id.btnCadastro);
        LoginEmail = findViewById(R.id.LoginEmail);
        LoginSenha = findViewById(R.id.LoginSenha);

        btnVoltar.setOnClickListener(v -> {
            Intent voltar = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(voltar);
        });
        btnCadastro.setOnClickListener(v -> {
            Intent cadastro = new Intent(LoginActivity.this, CadastroActivity.class);
            startActivity(cadastro);
        });
        btnLogin.setOnClickListener(v -> realizarLogin());
    }

    private void realizarLogin() {
        String email = LoginEmail.getText().toString().trim(); // Recebe a string do EditText do campo de email
        String senha = LoginSenha.getText().toString().trim(); // Recebe a string do EditText do campo de senha

        // Caso o usuário não preencha algum dos campos do EditText ele da erro, manda uma mensagem e retorna para não acumular
        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha e-mail e senha!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Faz a requisição para o backend
        LoginRequest request = new LoginRequest(email, senha);
        ApiClient.getApiService().login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    // 1. Salva o Token de acesso no SessionManager
                    SessionManager.saveToken(LoginActivity.this, loginResponse.getAccessToken());

                    Toast.makeText(LoginActivity.this, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show();

                    // 2. Redireciona para a tela inicial ou perfil
                    Intent logar = new Intent(LoginActivity.this, PaginaInicial.class);
                    startActivity(logar);
                    finish(); // Fecha a tela de login
                }
                // Caso o email ou a senha estejam incorretos dá erro
                else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Erro desconhecido";
                        Toast.makeText(LoginActivity.this, "Erro: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this, "E-mail ou senha incorretos.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            // Caso a conexão falhe dá erro
            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Falha na conexão: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}