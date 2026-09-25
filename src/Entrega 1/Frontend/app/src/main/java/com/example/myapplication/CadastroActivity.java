package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.network.ApiClient;
import com.example.myapplication.network.CadastroRequest;
import com.example.myapplication.network.CadastroResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CadastroActivity extends AppCompatActivity {

    ImageButton btnVoltar;
    Button btnCadastro, btnLogin;
    EditText editNome, editEmail, editSenha;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        btnVoltar = findViewById(R.id.btnVoltar);
        btnCadastro = findViewById(R.id.btnCadastro);
        btnLogin = findViewById(R.id.btnLogin);
        editNome = findViewById(R.id.editNome);
        editEmail = findViewById(R.id.editEmail);
        editSenha = findViewById(R.id.editSenha);

        btnVoltar.setOnClickListener(view -> {
            Intent voltar = new Intent(CadastroActivity.this, MainActivity.class);;
            startActivity(voltar);
        });
        btnLogin.setOnClickListener(v -> {
            Intent login = new Intent(CadastroActivity.this, LoginActivity.class);
            startActivity(login);
        });
        btnCadastro.setOnClickListener(v -> realizarCadastro());
    }
    private void realizarCadastro() {
        // Puxa os dados dos campos de EditText
        String nome = editNome.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String senha = editSenha.getText().toString().trim();

        // Caso o usuário não preencha algum dos campos do EditText ele da erro,
        // manda uma mensagem e retorna para não acumular
        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha nome, e-mail e senha!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Faz a requisição para o backend
        CadastroRequest request = new CadastroRequest(nome, email, senha);
        ApiClient.getApiService().cadastrar(request).enqueue(new Callback<CadastroResponse>() {
            @Override
            public void onResponse(Call<CadastroResponse> call, Response<CadastroResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CadastroActivity.this, "Cadastro realizado! Faça login.", Toast.LENGTH_LONG).show();

                    // Redireciona para a tela de login
                    Intent intent = new Intent(CadastroActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    try {
                        // Tenta extrair a mensagem de erro que veio do backend
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Erro desconhecido";
                        Toast.makeText(CadastroActivity.this, "Erro: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(CadastroActivity.this, "Erro ao realizar cadastro.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<CadastroResponse> call, Throwable t) {
                Toast.makeText(CadastroActivity.this, "Falha de rede: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
