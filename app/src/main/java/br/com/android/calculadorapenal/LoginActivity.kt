package br.com.android.calculadorapenal

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.text.TextWatcher
import android.text.Editable
import android.util.Patterns
import android.widget.Button
import android.widget.Toast
class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var senhaInput: TextInputEditText
    private lateinit var senhaInputLayout: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar os componentes
        emailInput = findViewById(R.id.emailInput)
        emailInputLayout = findViewById(R.id.emailInputLayout)
        senhaInput = findViewById(R.id.senhaInput)
        senhaInputLayout = findViewById(R.id.senhaInputLayout)

        // Validação em tempo real do email
        emailInput.addTextChangedListener(emailTextWatcher)

        // Validação em tempo real da senha
        senhaInput.addTextChangedListener(senhaTextWatcher)

        // Botão de Cadastro - valida antes de navegar
        val buttonCadastro = findViewById<Button>(R.id.buttonCadastro)
        buttonCadastro.setOnClickListener {
            if (validarCampos()) {
                val intent = Intent(this, DadosCrimeActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Preencha todos os campos corretamente", Toast.LENGTH_SHORT).show()
            }
        }

        // Quando clicar no button de SemCadastro, vai para a página de DadosCrime
        val buttonSemCadastro = findViewById<Button>(R.id.buttonSemCadastro)
        buttonSemCadastro.setOnClickListener {
            val intent = Intent(this, DadosCrimeActivity::class.java)
            startActivity(intent)
        }
    }
    private val emailTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            validateEmail()
        }
    }

    private val senhaTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            validateSenha()
        }
    }

    private fun validateEmail() {
        val email = emailInput.text.toString().trim()

        if (email.isEmpty()) {
            emailInputLayout.error = "E-mail é obrigatório"
            return
        }

        if (!isValidEmail(email)) {
            emailInputLayout.error = "Formato de e-mail inválido"
        } else {
            emailInputLayout.error = null
        }
    }

    private fun validateSenha() {
        val senha = senhaInput.text.toString().trim()

        if (senha.isEmpty()) {
            senhaInputLayout.error = "Senha é obrigatória"
        } else if (senha.length < 6) {
            senhaInputLayout.error = "Senha deve ter pelo menos 6 caracteres"
        } else {
            senhaInputLayout.error = null
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = Patterns.EMAIL_ADDRESS
        return emailPattern.matcher(email).matches()
    }

    private fun validarCampos(): Boolean {
        val email = emailInput.text.toString().trim()
        val senha = senhaInput.text.toString().trim()

        // Valida email
        if (email.isEmpty()) {
            emailInputLayout.error = "E-mail é obrigatório"
            return false
        }

        if (!isValidEmail(email)) {
            emailInputLayout.error = "Formato de e-mail inválido"
            return false
        }

        // Valida senha
        if (senha.isEmpty()) {
            senhaInputLayout.error = "Senha é obrigatória"
            return false
        }

        if (senha.length < 6) {
            senhaInputLayout.error = "Senha deve ter pelo menos 6 caracteres"
            return false
        }

        // Se chegou aqui, todos os campos são válidos
        emailInputLayout.error = null
        senhaInputLayout.error = null
        return true
    }
}