// ...existing code...
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max

@Serializable
data class SentenceInput(
    val penaYears: Int = 0,
    val penaMonths: Int = 0,
    val penaDays: Int = 0,
    val dataInicio: String,
    // detração desmembrada para corresponder ao front-end
    val detracaoYears: Int = 0,
    val detracaoMonths: Int = 0,
    val detracaoDays: Int = 0,
    val tipoCrime: String,
    val statusApenado: String,
    val trabalhoDiasTrabalhados: Int = 0,
    val estudoHoras: Int = 0,
    val livrosLidos: Int = 0,
    val atividadesComplementaresDias: Int = 0,
    val regimeAtual: String = "FECHADO"
)

@Serializable
data class CalculationResult(
    val progressaoSemiaberto: String?,
    val progressaoAberto: String?,
    val livramentoCondicional: String?,
    val detalhes: String? = null,
    val remicaoDias: Int = 0,
    val remicaoDetalhes: String? = null,
    val notas: String? = null
)

// Constrói a data final da pena usando anos/meses/dias
fun buildSentenceEndDate(start: LocalDate, years: Int, months: Int, days: Int): LocalDate {
    return start.plusYears(years.toLong()).plusMonths(months.toLong()).plusDays(days.toLong())
}

// Dias entre duas datas (calendário real)
fun daysBetween(start: LocalDate, end: LocalDate): Int = ChronoUnit.DAYS.between(start, end).toInt()

// Aproximação para converter anos/meses/dias em dias (usar quando necessário)
fun toTotalDays(years: Int, months: Int, days: Int): Int {
    return years * 365 + months * 30 + days
}

fun calculateRemissionDays(
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

fun normalizeTipoCrime(raw: String): String = raw.trim().uppercase()
fun normalizeStatus(raw: String): String = raw.trim().uppercase()

// Frações para progressão conforme regra informada
fun getProgressionFraction(tipoCrime: String, status: String): Double {
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
fun getLivramentoFraction(tipoCrime: String, status: String): Double {
    val t = normalizeTipoCrime(tipoCrime)
    val s = normalizeStatus(status)

    return when (t) {
        "HEDIONDO_MORTE", "HEDIONDO RESULTADO MORTE" -> 0.0 // vedado
        "HEDIONDO" -> 2.0 / 3.0 // 2/3 para hediondo (aplicável independentemente do primário/reincidente conforme sua regra)
        else -> if (s == "PRIMARIO") 1.0 / 3.0 else 1.0 / 2.0 // primário 1/3, reincidente 1/2
    }
}

fun daysForFraction(totalDays: Int, fraction: Double): Int = ceil(totalDays * fraction).toInt()

// Computa dias já cumpridos: tempo decorrido desde início + detração + remição
fun computeDaysAlreadyServed(
    startDate: LocalDate,
    detractionDays: Int,
    remissionDays: Int
): Int {
    val today = LocalDate.now()
    val daysSinceStart = if (!today.isBefore(startDate)) daysBetween(startDate, today) else 0
    return max(0, daysSinceStart + detractionDays + remissionDays)
}

fun dateAfterDaysFromToday(days: Int): LocalDate = LocalDate.now().plusDays(days.toLong())

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json(Json { prettyPrint = true })
        }
        routing {
            post("/calculate") {
                val input = call.receive<SentenceInput>()

                val startDate = try {
                    LocalDate.parse(input.dataInicio)
                } catch (e: Exception) {
                    call.respond(400, mapOf("error" to "dataInicio inválida. Use yyyy-MM-dd"))
                    return@post
                }

                // converte detração desmembrada para dias totais
                val detractionTotalDays = toTotalDays(input.detracaoYears, input.detracaoMonths, input.detracaoDays)

                // cria data final da pena e calcula total de dias da pena
                val sentenceEndDate = buildSentenceEndDate(startDate, input.penaYears, input.penaMonths, input.penaDays)
                val totalSentenceDays = daysBetween(startDate, sentenceEndDate)
                if (totalSentenceDays <= 0) {
                    call.respond(400, mapOf("error" to "Pena total deve ser maior que zero"))
                    return@post
                }

                // calcula remição (considera regime atual para trabalho)
                val remissionDays = calculateRemissionDays(
                    input.trabalhoDiasTrabalhados,
                    input.estudoHoras,
                    input.livrosLidos,
                    input.atividadesComplementaresDias,
                    input.regimeAtual
                )

                val remicaoDetalhes = buildString {
                    if (input.trabalhoDiasTrabalhados > 0)
                        append("Trabalho: ${input.trabalhoDiasTrabalhados / 3} dias\n")
                    if (input.estudoHoras > 0)
                        append("Estudo: ${input.estudoHoras / 12} dias\n")
                    if (input.livrosLidos > 0)
                        append("Leitura: ${input.livrosLidos * 4} dias\n")
                    if (input.atividadesComplementaresDias > 0)
                        append("Atividades: ${input.atividadesComplementaresDias} dias")
                }

                // debug: valores intermediários
                println("INPUT pena Y/M/D: ${input.penaYears}/${input.penaMonths}/${input.penaDays}")
                println("startDate=$startDate, sentenceEndDate=$sentenceEndDate, totalSentenceDays=$totalSentenceDays")
                println("detractionDays(total)=$detractionTotalDays, remissionDays=$remissionDays")

                // --- PROGRESSÃO PARA SEMIABERTO (ou primeiro degrau aplicável) ---
                val fractionSemiaberto = getProgressionFraction(input.tipoCrime, input.statusApenado)
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

                // --- PROGRESSÃO PARA ABERTO (regra simplificada: depende do tipo de crime) ---
                // Nota: as regras exatas de passagem a aberto variam; aqui consideramos um fracção de exemplo:
                val fractionAberto = when (normalizeTipoCrime(input.tipoCrime)) {
                    "HEDIONDO", "HEDIONDO_MORTE" -> 0.5 // ex.: hediondo -> 50% para aberto (ajuste se desejar)
                    else -> 0.5 // escolha padrão 50% para aberto (ajuste conforme necessidade)
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

                // --- LIVRAMENTO CONDICIONAL ---
                val fractionLivramento = getLivramentoFraction(input.tipoCrime, input.statusApenado)
                var livramentoMsg: String? = null
                var livramentoDate: LocalDate? = null

                if (fractionLivramento == 0.0) {
                    livramentoMsg = "Livramento condicional vedado para este tipo de crime."
                } else {
                    val requiredDaysLivramento = daysForFraction(totalSentenceDays, fractionLivramento)
                    if (alreadyServed >= requiredDaysLivramento) {
                        livramentoDate = LocalDate.now()
                    } else {
                        val remaining = requiredDaysLivramento - alreadyServed
                        livramentoDate = dateAfterDaysFromToday(remaining)
                    }
                }

                val result = CalculationResult(
                    progressaoSemiaberto = semiResultDate?.toString(),
                    progressaoAberto = abertoResultDate?.toString(),
                    livramentoCondicional = livramentoDate?.toString(),
                    detalhes = livramentoMsg,
                    remicaoDias = remissionDays,
                    remicaoDetalhes = remicaoDetalhes,
                    notas = "Valores aproximados. Verifique detalhes legais e documentos oficiais."
                )

                call.respond(result)
            }

            get("/health") {
                call.respond(mapOf("status" to "ok"))
            }
        }
    }.start(wait = true)
}

private fun ApplicationCall.respond(
    message: Int,
    messageType: kotlin.collections.Map<String, String>
) {
}
