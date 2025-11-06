package br.com.android.calculadorapenal

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import android.text.TextWatcher
import android.text.Editable
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import android.widget.TextView
import android.util.Patterns
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max

class ResultadosActivity : AppCompatActivity() {

    private lateinit var editTextNome: TextInputEditText
    private lateinit var editTextWhatsApp: TextInputEditText
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextProcesso: TextInputEditText
    private lateinit var editTextMensagem: TextInputEditText
    private lateinit var buttonEnviar: MaterialButton
    private lateinit var buttonWhatsApp: MaterialButton
    private lateinit var textInputLayoutNome: TextInputLayout
    private lateinit var textInputLayoutWhatsApp: TextInputLayout
    private lateinit var textInputLayoutEmail: TextInputLayout
    private lateinit var textInputLayoutProcesso: TextInputLayout
    private lateinit var textInputLayoutMensagem: TextInputLayout

    // Formatter para datas no formato DD/MM/AAAA
    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resultados)

        // Processar dados da tela anterior
        processarDadosRecebidos()

        // ... (o resto do seu código permanece EXATAMENTE igual) ...
        // Inicializar views
        editTextNome = findViewById(R.id.editTextNome)
        editTextWhatsApp = findViewById(R.id.editTextWhatsApp)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextProcesso = findViewById(R.id.editTextProcesso)
        editTextMensagem = findViewById(R.id.editTextMensagem)
        buttonEnviar = findViewById(R.id.buttonEnviar)
        buttonWhatsApp = findViewById(R.id.buttonWhatsApp)
        textInputLayoutNome = findViewById(R.id.textInputLayoutNome)
        textInputLayoutWhatsApp = findViewById(R.id.textInputLayoutWhatsApp)
        textInputLayoutEmail = findViewById(R.id.textInputLayoutEmail)
        textInputLayoutProcesso = findViewById(R.id.textInputLayoutProcesso)
        textInputLayoutMensagem = findViewById(R.id.textInputLayoutMensagem)

        // Configurar validação do formato do WhatsApp
        editTextWhatsApp.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private var oldText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                oldText = s.toString()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                val text = s.toString()

                // Remover todos os caracteres não numéricos
                val cleanText = text.replace("[^\\d]".toRegex(), "")

                // Aplicar a máscara (XX) XXXXX-XXXX
                val maskedText = when {
                    cleanText.isEmpty() -> ""
                    cleanText.length <= 2 -> cleanText
                    cleanText.length <= 7 -> "(${cleanText.substring(0, 2)}) ${cleanText.substring(2)}"
                    cleanText.length <= 11 -> "(${cleanText.substring(0, 2)}) ${cleanText.substring(2, 7)}-${cleanText.substring(7)}"
                    else -> "(${cleanText.substring(0, 2)}) ${cleanText.substring(2, 7)}-${cleanText.substring(7, 11)}"
                }

                if (text != maskedText) {
                    isUpdating = true
                    s?.replace(0, s.length, maskedText)
                    isUpdating = false
                }

                // Validar campos obrigatórios
                validarCamposObrigatorios()
                validarFormatoWhatsApp(maskedText)
            }
        })

        // Configurar validação do formato do email
        editTextEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validarFormatoEmail(s.toString())
                validarTodosOsCamposParaHabilitarBotao()
            }
        })

        // Configurar validação do formato do processo
        editTextProcesso.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validarFormatoProcesso(s.toString())
                validarTodosOsCamposParaHabilitarBotao()
            }
        })

        // Configurar validação dos campos obrigatórios
        val camposObrigatoriosWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validarCamposObrigatorios()
                validarTodosOsCamposParaHabilitarBotao()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        editTextNome.addTextChangedListener(camposObrigatoriosWatcher)
        editTextWhatsApp.addTextChangedListener(camposObrigatoriosWatcher)

        // Configurar botão WhatsApp
        buttonWhatsApp.setOnClickListener {
            abrirWhatsApp()
        }

        // Configurar botão Enviar
        buttonEnviar.setOnClickListener {
            if (validarTodosOsCamposParaEnvio()) {
                enviarFormulario()
            }
        }

        // Configurar botão Voltar
        val buttonVoltar = findViewById<MaterialButton>(R.id.buttonVoltar)
        buttonVoltar.setOnClickListener {
            finish()
        }

        // Validar campos inicialmente
        validarCamposObrigatorios()
        validarTodosOsCamposParaHabilitarBotao()
    }

    // ========== FUNÇÕES DE CÁLCULO ADAPTADAS DO SEU CÓDIGO ==========

    @RequiresApi(Build.VERSION_CODES.O)
    private fun processarDadosRecebidos() {
        val extras = intent.extras
        if (extras != null) {
            try {
                // Coletar dados da tela anterior
                val tipoCrime = extras.getString("tipo_crime", "")
                val statusApenado = extras.getString("status", "")
                val regimeAtual = extras.getString("regime", "FECHADO")
                val dataInicioStr = extras.getString("data_inicio", "")

                val penaAnos = extras.getString("pena_anos", "0").toIntOrNull() ?: 0
                val penaMeses = extras.getString("pena_meses", "0").toIntOrNull() ?: 0
                val penaDias = extras.getString("pena_dias", "0").toIntOrNull() ?: 0

                val detracaoAnos = extras.getString("detracao_anos", "0").toIntOrNull() ?: 0
                val detracaoMeses = extras.getString("detracao_meses", "0").toIntOrNull() ?: 0
                val detracaoDias = extras.getString("detracao_dias", "0").toIntOrNull() ?: 0

                val trabalhoDiasTrabalhados = extras.getString("dias_trabalhados", "0").toIntOrNull() ?: 0
                val estudoHoras = extras.getString("horas_estudo", "0").toIntOrNull() ?: 0
                val livrosLidos = extras.getString("livros_lidos", "0").toIntOrNull() ?: 0
                val atividadesComplementaresDias = extras.getString("atividades", "0").toIntOrNull() ?: 0

                // Processar data de início (converter de DD/MM/AAAA para LocalDate)
                val startDate = parseDataInicio(dataInicioStr)
                if (startDate == null) {
                    mostrarErroCalculo("Data de início inválida")
                    return
                }

                // Calcular detração total em dias
                val detractionTotalDays = toTotalDays(detracaoAnos, detracaoMeses, detracaoDias)

                // Calcular data final da pena e total de dias da pena
                val sentenceEndDate = buildSentenceEndDate(startDate, penaAnos, penaMeses, penaDias)
                val totalSentenceDays = daysBetween(startDate, sentenceEndDate)

                if (totalSentenceDays <= 0) {
                    mostrarErroCalculo("Pena total deve ser maior que zero")
                    return
                }

                // Calcular remição
                val remissionDays = calculateRemissionDays(
                    trabalhoDiasTrabalhados,
                    estudoHoras,
                    livrosLidos,
                    atividadesComplementaresDias,
                    regimeAtual
                )

                // Construir detalhes da remição
                val remicaoDetalhes = buildString {
                    if (trabalhoDiasTrabalhados > 0)
                        append("Trabalho: ${trabalhoDiasTrabalhados / 3} dias\n")
                    if (estudoHoras > 0)
                        append("Estudo: ${estudoHoras / 12} dias\n")
                    if (livrosLidos > 0)
                        append("Leitura: ${livrosLidos * 4} dias\n")
                    if (atividadesComplementaresDias > 0)
                        append("Atividades: ${atividadesComplementaresDias} dias")
                }

                // Calcular progressão para semiaberto
                val fractionSemiaberto = getProgressionFraction(tipoCrime, statusApenado)
                val requiredDaysSemi = daysForFraction(totalSentenceDays, fractionSemiaberto)
                val alreadyServed = computeDaysAlreadyServed(startDate, detractionTotalDays, remissionDays)

                val semiResultDate = when {
                    requiredDaysSemi <= 0 -> null
                    alreadyServed >= requiredDaysSemi -> LocalDate.now()
                    else -> {
                        val remaining = requiredDaysSemi - alreadyServed
                        dateAfterDaysFromToday(remaining)
                    }
                }

                // Calcular progressão para aberto
                val fractionAberto = when (normalizeTipoCrime(tipoCrime)) {
                    "HEDIONDO", "HEDIONDO_MORTE" -> 0.5
                    else -> 0.5
                }
                val requiredDaysAberto = daysForFraction(totalSentenceDays, fractionAberto)
                val abertoResultDate = when {
                    requiredDaysAberto <= 0 -> null
                    alreadyServed >= requiredDaysAberto -> LocalDate.now()
                    else -> {
                        val remaining = requiredDaysAberto - alreadyServed
                        dateAfterDaysFromToday(remaining)
                    }
                }

                // Calcular livramento condicional
                val fractionLivramento = getLivramentoFraction(tipoCrime, statusApenado)
                var livramentoDate: LocalDate? = null
                var livramentoMsg = ""

                if (fractionLivramento == 0.0) {
                    livramentoMsg = "Livramento condicional vedado para este tipo de crime."
                } else {
                    val requiredDaysLivramento = daysForFraction(totalSentenceDays, fractionLivramento)
                    livramentoDate = if (alreadyServed >= requiredDaysLivramento) {
                        LocalDate.now()
                    } else {
                        val remaining = requiredDaysLivramento - alreadyServed
                        dateAfterDaysFromToday(remaining)
                    }
                }

                // Exibir resultados na interface
                exibirResultados(
                    semiResultDate,
                    abertoResultDate,
                    livramentoDate,
                    livramentoMsg,
                    remissionDays,
                    remicaoDetalhes
                )

            } catch (e: Exception) {
                mostrarErroCalculo("Erro no cálculo: ${e.message}")
            }
        } else {
            mostrarErroCalculo("Dados não recebidos")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseDataInicio(dataStr: String): LocalDate? {
        return try {
            LocalDate.parse(dataStr, dateFormatter)
        } catch (e: Exception) {
            null
        }
    }

    private fun mostrarErroCalculo(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
        // Preencher com valores padrão em caso de erro
        findViewById<TextView>(R.id.textSemiaberto).text = "Erro no cálculo"
        findViewById<TextView>(R.id.textAberto).text = "Erro no cálculo"
        findViewById<TextView>(R.id.textLivramento).text = "Erro no cálculo"
        findViewById<TextView>(R.id.textDiasRemicao).text = "0 dias"
        findViewById<TextView>(R.id.textDetalhesRemicao).text = "Não foi possível calcular"
        findViewById<TextView>(R.id.textObservacoes).text = "Verifique os dados inseridos"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun exibirResultados(
        semiResultDate: LocalDate?,
        abertoResultDate: LocalDate?,
        livramentoDate: LocalDate?,
        livramentoMsg: String,
        remissionDays: Int,
        remicaoDetalhes: String
    ) {
        // Formatar datas para exibição (DD/MM/AAAA)
        val dateFormatterDisplay = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        findViewById<TextView>(R.id.textSemiaberto).text =
            semiResultDate?.format(dateFormatterDisplay) ?: "Não aplicável"

        findViewById<TextView>(R.id.textAberto).text =
            abertoResultDate?.format(dateFormatterDisplay) ?: "Não aplicável"

        findViewById<TextView>(R.id.textLivramento).text =
            if (livramentoMsg.isNotEmpty()) livramentoMsg
            else livramentoDate?.format(dateFormatterDisplay) ?: "Não aplicável"

        findViewById<TextView>(R.id.textDiasRemicao).text = "$remissionDays dias"
        findViewById<TextView>(R.id.textDetalhesRemicao).text = remicaoDetalhes

        val observacoes = buildString {
            append("Cálculo realizado com base nos dados fornecidos. ")
            append("Valores aproximados - verifique documentos oficiais.")
            if (livramentoMsg.isNotEmpty()) {
                append("\n\n$livramentoMsg")
            }
        }
        findViewById<TextView>(R.id.textObservacoes).text = observacoes
    }

    // ========== FUNÇÕES DO SEU CÓDIGO ORIGINAL (adaptadas) ==========

    // Constrói a data final da pena usando anos/meses/dias
    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildSentenceEndDate(start: LocalDate, years: Int, months: Int, days: Int): LocalDate {
        return start.plusYears(years.toLong()).plusMonths(months.toLong()).plusDays(days.toLong())
    }

    // Dias entre duas datas (calendário real)
    @RequiresApi(Build.VERSION_CODES.O)
    private fun daysBetween(start: LocalDate, end: LocalDate): Int =
        ChronoUnit.DAYS.between(start, end).toInt()

    // Aproximação para converter anos/meses/dias em dias
    private fun toTotalDays(years: Int, months: Int, days: Int): Int {
        return years * 365 + months * 30 + days
    }

    private fun calculateRemissionDays(
        trabalhoDiasTrabalhados: Int,
        estudoHoras: Int,
        livrosLidos: Int,
        atividadesComplementaresDias: Int,
        regimeAtual: String
    ): Int {
        // Trabalho: 1 dia a cada 3 trabalhados (não vale para regime aberto)
        val remTrabalho = if (!regimeAtual.equals("ABERTO", ignoreCase = true)) trabalhoDiasTrabalhados / 3 else 0
        // Estudo: 1 dia a cada 12 horas exatas
        val remEstudo = estudoHoras / 12
        // Leitura: 4 dias por livro lido
        val remLeitura = livrosLidos * 4
        // Atividades complementares (dias creditados)
        val remAtividades = atividadesComplementaresDias
        return remTrabalho + remEstudo + remLeitura + remAtividades
    }

    private fun normalizeTipoCrime(raw: String): String = raw.trim().uppercase()
    private fun normalizeStatus(raw: String): String = raw.trim().uppercase()

    // Frações para progressão conforme regra informada
    private fun getProgressionFraction(tipoCrime: String, status: String): Double {
        val t = normalizeTipoCrime(tipoCrime)
        val s = normalizeStatus(status)

        return when (t) {
            "COMUM" -> if (s == "PRIMARIO") 0.16 else 0.20
            "VIOLENCIA", "VIOLÊNCIA", "GRAVE_AMEAÇA", "GRAVE AMEAÇA" -> if (s == "PRIMARIO") 0.25 else 0.30
            "HEDIONDO" -> if (s == "PRIMARIO") 0.40 else 0.60
            "HEDIONDO_MORTE", "HEDIONDO RESULTADO MORTE" -> if (s == "PRIMARIO") 0.50 else 0.70
            else -> if (s == "PRIMARIO") 0.16 else 0.20
        }
    }

    // Frações para livramento condicional conforme regra informada
    private fun getLivramentoFraction(tipoCrime: String, status: String): Double {
        val t = normalizeTipoCrime(tipoCrime)
        val s = normalizeStatus(status)

        return when (t) {
            "HEDIONDO_MORTE", "HEDIONDO RESULTADO MORTE" -> 0.0 // vedado
            "HEDIONDO" -> 2.0 / 3.0 // 2/3 para hediondo
            else -> if (s == "PRIMARIO") 1.0 / 3.0 else 1.0 / 2.0 // primário 1/3, reincidente 1/2
        }
    }

    private fun daysForFraction(totalDays: Int, fraction: Double): Int =
        ceil(totalDays * fraction).toInt()

    // Computa dias já cumpridos: tempo decorrido desde início + detração + remição
    @RequiresApi(Build.VERSION_CODES.O)
    private fun computeDaysAlreadyServed(
        startDate: LocalDate,
        detractionDays: Int,
        remissionDays: Int
    ): Int {
        val today = LocalDate.now()
        val daysSinceStart = if (!today.isBefore(startDate)) daysBetween(startDate, today) else 0
        return max(0, daysSinceStart + detractionDays + remissionDays)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun dateAfterDaysFromToday(days: Int): LocalDate =
        LocalDate.now().plusDays(days.toLong())

    // ========== FUNÇÕES DE VALIDAÇÃO (mantidas do seu código anterior) ==========

    private fun validarFormatoWhatsApp(whatsapp: String) {
        if (whatsapp.isEmpty()) {
            textInputLayoutWhatsApp.error = null
            return
        }

        // Formato: (XX) XXXXX-XXXX
        val pattern = "^\\(\\d{2}\\)\\s\\d{5}-\\d{4}$"
        if (!whatsapp.matches(pattern.toRegex())) {
            textInputLayoutWhatsApp.error = "Formato inválido. Use: (XX) XXXXX-XXXX"
        } else {
            textInputLayoutWhatsApp.error = null
        }
    }

    private fun validarFormatoEmail(email: String) {
        if (email.isEmpty()) {
            textInputLayoutEmail.error = null
            return
        }

        // Validar formato de email usando Patterns do Android
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            textInputLayoutEmail.error = "Formato de email inválido. Use: exemplo@email.com"
        } else {
            textInputLayoutEmail.error = null
        }
    }

    private fun validarFormatoProcesso(processo: String) {
        if (processo.isEmpty()) {
            textInputLayoutProcesso.error = null
            return
        }

        // Formato: NNNNNNN-DD.AAAA.J.TR.OOOO
        val pattern = "^\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}$"
        if (!processo.matches(pattern.toRegex())) {
            textInputLayoutProcesso.error = "Formato inválido. Use: NNNNNNN-DD.AAAA.J.TR.OOOO"
        } else {
            textInputLayoutProcesso.error = null
        }
    }

    private fun validarCamposObrigatorios() {
        val nome = editTextNome.text.toString().trim()
        val whatsapp = editTextWhatsApp.text.toString().trim()

        // Mostrar/ocultar mensagens de erro
        if (nome.isEmpty()) {
            textInputLayoutNome.error = "Nome é obrigatório"
        } else {
            textInputLayoutNome.error = null
        }

        if (whatsapp.isEmpty()) {
            textInputLayoutWhatsApp.error = "WhatsApp é obrigatório"
        } else {
            textInputLayoutWhatsApp.error = null
        }
    }

    private fun validarTodosOsCamposParaHabilitarBotao() {
        val nome = editTextNome.text.toString().trim()
        val whatsapp = editTextWhatsApp.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val processo = editTextProcesso.text.toString().trim()

        // Verificar se campos obrigatórios estão preenchidos
        val camposObrigatoriosPreenchidos = nome.isNotEmpty() && whatsapp.isNotEmpty()

        // Verificar se há erros de formato nos campos
        val semErrosFormato = textInputLayoutNome.error == null &&
                textInputLayoutWhatsApp.error == null &&
                textInputLayoutEmail.error == null &&
                textInputLayoutProcesso.error == null

        // Habilitar botão apenas se campos obrigatórios preenchidos E sem erros de formato
        buttonEnviar.isEnabled = camposObrigatoriosPreenchidos && semErrosFormato
    }

    private fun validarTodosOsCamposParaEnvio(): Boolean {
        val nome = editTextNome.text.toString().trim()
        val whatsapp = editTextWhatsApp.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val processo = editTextProcesso.text.toString().trim()

        var isValid = true

        // Validar campos obrigatórios
        if (nome.isEmpty()) {
            textInputLayoutNome.error = "Nome é obrigatório"
            isValid = false
        } else {
            textInputLayoutNome.error = null
        }

        if (whatsapp.isEmpty()) {
            textInputLayoutWhatsApp.error = "WhatsApp é obrigatório"
            isValid = false
        } else if (!whatsapp.matches("^\\(\\d{2}\\)\\s\\d{5}-\\d{4}$".toRegex())) {
            textInputLayoutWhatsApp.error = "Formato inválido. Use: (XX) XXXXX-XXXX"
            isValid = false
        } else {
            textInputLayoutWhatsApp.error = null
        }

        // Validar formato do email (se preenchido)
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            textInputLayoutEmail.error = "Formato de email inválido. Use: exemplo@email.com"
            isValid = false
        } else {
            textInputLayoutEmail.error = null
        }

        // Validar formato do processo (se preenchido)
        if (processo.isNotEmpty() && !processo.matches("^\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}$".toRegex())) {
            textInputLayoutProcesso.error = "Formato inválido. Use: NNNNNNN-DD.AAAA.J.TR.OOOO"
            isValid = false
        } else {
            textInputLayoutProcesso.error = null
        }

        return isValid
    }

    private fun abrirWhatsApp() {
        try {
            val url = "https://api.whatsapp.com/send/?phone=5511989498044&text=Ol%C3%A1%2C+sou+advogado%2C+como+posso+ajudar%3F&type=phone_number&app_absent=0"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp não instalado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enviarFormulario() {
        val nome = editTextNome.text.toString().trim()
        val whatsapp = editTextWhatsApp.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val processo = editTextProcesso.text.toString().trim()
        val mensagem = editTextMensagem.text.toString().trim()

        // Aqui você pode enviar os dados para seu backend
        // Por exemplo, enviar por email, salvar em banco de dados, etc.

        // Mostrar dados no Toast para teste
        val dadosEnvio = """
            Nome: $nome
            WhatsApp: $whatsapp
            Email: ${if (email.isEmpty()) "Não informado" else email}
            Processo: ${if (processo.isEmpty()) "Não informado" else processo}
            Mensagem: ${if (mensagem.isEmpty()) "Não informada" else mensagem}
        """.trimIndent()

        Toast.makeText(this, "Formulário enviado com sucesso!\n$dadosEnvio", Toast.LENGTH_LONG).show()

        // Limpar campos após envio (opcional)
        editTextNome.text?.clear()
        editTextWhatsApp.text?.clear()
        editTextEmail.text?.clear()
        editTextProcesso.text?.clear()
        editTextMensagem.text?.clear()

        // Desabilitar botão após envio
        buttonEnviar.isEnabled = false

        // Limpar mensagens de erro
        textInputLayoutNome.error = null
        textInputLayoutWhatsApp.error = null
        textInputLayoutEmail.error = null
        textInputLayoutProcesso.error = null
    }
}