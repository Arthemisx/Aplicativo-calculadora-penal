package br.com.android.calculadorapenal

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import android.widget.ArrayAdapter
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import android.content.Intent
import android.app.DatePickerDialog
import android.widget.Button
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

class DadosCrimeActivity : AppCompatActivity() {

    private lateinit var inputTipoCrime: MaterialAutoCompleteTextView
    private lateinit var inputStatus: MaterialAutoCompleteTextView
    private lateinit var inputRegime: MaterialAutoCompleteTextView

    // Campos de input
    private lateinit var inputPenaAnos: TextInputEditText
    private lateinit var inputPenaMeses: TextInputEditText
    private lateinit var inputPenaDias: TextInputEditText
    private lateinit var inputDataInicio: TextInputEditText
    private lateinit var inputDetracaoAnos: TextInputEditText
    private lateinit var inputDetracaoMeses: TextInputEditText
    private lateinit var inputDetracaoDias: TextInputEditText
    private lateinit var inputDiasTrabalhados: TextInputEditText
    private lateinit var inputHorasEstudo: TextInputEditText
    private lateinit var inputLivrosLidos: TextInputEditText
    private lateinit var inputAtividades: TextInputEditText

    private lateinit var buttonCalcular: MaterialButton
    private lateinit var buttonVoltarCadastro: Button

    // Calendário para seleção de data
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dados_crime)

        setupDropdowns()
        setupInputFields()
        setupCalculateButton()
        setupDatePicker()
        setupBackButton()
    }

    private fun setupDropdowns() {
        // Tipo de Crime
        inputTipoCrime = findViewById(R.id.inputTipoCrime)
        val tiposCrime = arrayOf("COMUM", "VIOLÊNCIA", "HEDIONDO", "HEDIONDO_MORTE")
        val adapterTipoCrime = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tiposCrime)
        inputTipoCrime.setAdapter(adapterTipoCrime)
        inputTipoCrime.threshold = 1

        // Status do Apenado
        inputStatus = findViewById(R.id.inputStatus)
        val statusApenado = arrayOf("PRIMÁRIO", "REINCIDENTE")
        val adapterStatus = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statusApenado)
        inputStatus.setAdapter(adapterStatus)
        inputStatus.threshold = 1

        // Regime Atual
        inputRegime = findViewById(R.id.inputRegime)
        val regimes = arrayOf("FECHADO", "SEMIABERTO", "ABERTO")
        val adapterRegime = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, regimes)
        inputRegime.setAdapter(adapterRegime)
        inputRegime.threshold = 1
    }

    private fun setupInputFields() {
        // Inicializar todos os campos de input
        inputPenaAnos = findViewById(R.id.inputPenaAnos)
        inputPenaMeses = findViewById(R.id.inputPenaMeses)
        inputPenaDias = findViewById(R.id.inputPenaDias)
        inputDataInicio = findViewById(R.id.inputDataInicio)
        inputDetracaoAnos = findViewById(R.id.inputDetracaoAnos)
        inputDetracaoMeses = findViewById(R.id.inputDetracaoMeses)
        inputDetracaoDias = findViewById(R.id.inputDetracaoDias)
        inputDiasTrabalhados = findViewById(R.id.inputDiasTrabalhados)
        inputHorasEstudo = findViewById(R.id.inputHorasEstudo)
        inputLivrosLidos = findViewById(R.id.inputLivrosLidos)
        inputAtividades = findViewById(R.id.inputAtividades)
    }

    private fun setupDatePicker() {
        inputDataInicio = findViewById(R.id.inputDataInicio)

        // Criar o DatePickerDialog
        val datePicker = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateLabel()
        }

        // Configurar clique no campo de data
        inputDataInicio.setOnClickListener {
            DatePickerDialog(
                this,
                datePicker,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateDateLabel() {
        val dateString = dateFormat.format(calendar.time)
        inputDataInicio.setText(dateString)
    }

    private fun setupCalculateButton() {
        buttonCalcular = findViewById(R.id.buttonCalcular)

        buttonCalcular.setOnClickListener {
            if (validarTodosCampos()) {
                // Se todos os campos estão preenchidos, ir para tela de resultados
                irParaTelaResultados()
            } else {
                Toast.makeText(this, "Por favor, preencha todos os campos obrigatórios", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupBackButton() {
        buttonVoltarCadastro = findViewById(R.id.buttonVoltarCadastro)

        buttonVoltarCadastro.setOnClickListener {
            // Voltar para a tela de LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Opcional: remove esta activity da pilha
        }
    }

    private fun validarTodosCampos(): Boolean {
        // Validar campos de pena (pelo menos um deve estar preenchido)
        val penaAnos = inputPenaAnos.text.toString().trim()
        val penaMeses = inputPenaMeses.text.toString().trim()
        val penaDias = inputPenaDias.text.toString().trim()

        if (penaAnos.isEmpty() && penaMeses.isEmpty() && penaDias.isEmpty()) {
            inputPenaAnos.error = "Informe pelo menos um valor para a pena"
            return false
        } else {
            inputPenaAnos.error = null
        }

        // Validar data de início
        val dataInicio = inputDataInicio.text.toString().trim()
        if (dataInicio.isEmpty()) {
            inputDataInicio.error = "Data de início é obrigatória"
            return false
        } else {
            inputDataInicio.error = null
        }

        // Validar campos de detração (pelo menos um deve estar preenchido)
        val detracaoAnos = inputDetracaoAnos.text.toString().trim()
        val detracaoMeses = inputDetracaoMeses.text.toString().trim()
        val detracaoDias = inputDetracaoDias.text.toString().trim()

        if (detracaoAnos.isEmpty() && detracaoMeses.isEmpty() && detracaoDias.isEmpty()) {
            inputDetracaoAnos.error = "Informe pelo menos um valor para detração"
            return false
        } else {
            inputDetracaoAnos.error = null
        }

        // Validar dropdowns
        val tipoCrime = inputTipoCrime.text.toString().trim()
        if (tipoCrime.isEmpty()) {
            inputTipoCrime.error = "Tipo de crime é obrigatório"
            return false
        } else {
            inputTipoCrime.error = null
        }

        val status = inputStatus.text.toString().trim()
        if (status.isEmpty()) {
            inputStatus.error = "Status do apenado é obrigatório"
            return false
        } else {
            inputStatus.error = null
        }

        val regime = inputRegime.text.toString().trim()
        if (regime.isEmpty()) {
            inputRegime.error = "Regime atual é obrigatório"
            return false
        } else {
            inputRegime.error = null
        }

        // Validar campos numéricos (devem ter valor padrão 0 se vazios)
        val diasTrabalhados = inputDiasTrabalhados.text.toString().trim()
        val horasEstudo = inputHorasEstudo.text.toString().trim()
        val livrosLidos = inputLivrosLidos.text.toString().trim()
        val atividades = inputAtividades.text.toString().trim()

        // Definir valor padrão 0 para campos opcionais vazios
        if (diasTrabalhados.isEmpty()) {
            inputDiasTrabalhados.setText("0")
        }
        if (horasEstudo.isEmpty()) {
            inputHorasEstudo.setText("0")
        }
        if (livrosLidos.isEmpty()) {
            inputLivrosLidos.setText("0")
        }
        if (atividades.isEmpty()) {
            inputAtividades.setText("0")
        }

        return true
    }

    private fun irParaTelaResultados() {
        // Coletar todos os dados para passar para a próxima tela
        val dados = Bundle().apply {
            putString("pena_anos", inputPenaAnos.text.toString())
            putString("pena_meses", inputPenaMeses.text.toString())
            putString("pena_dias", inputPenaDias.text.toString())
            putString("data_inicio", inputDataInicio.text.toString())
            putString("detracao_anos", inputDetracaoAnos.text.toString())
            putString("detracao_meses", inputDetracaoMeses.text.toString())
            putString("detracao_dias", inputDetracaoDias.text.toString())
            putString("tipo_crime", inputTipoCrime.text.toString())
            putString("status", inputStatus.text.toString())
            putString("regime", inputRegime.text.toString())
            putString("dias_trabalhados", inputDiasTrabalhados.text.toString())
            putString("horas_estudo", inputHorasEstudo.text.toString())
            putString("livros_lidos", inputLivrosLidos.text.toString())
            putString("atividades", inputAtividades.text.toString())
        }

        val intent = Intent(this, ResultadosActivity::class.java)
        intent.putExtras(dados)
        startActivity(intent)

        // Opcional: adicionar animação de transição
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}