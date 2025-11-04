package br.com.android.backend

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
import kotlinx.datetime.*
import kotlin.math.ceil

@Serializable
data class SentenceInput(
    val penaYears: Int = 0,
    val penaMonths: Int = 0,
    val penaDays: Int = 0,
    val dataInicio: String,
    val detraçãoDias: Int = 0,
    val tipoCrime: String,
    val statusApenado: String,
    val trabalhoDiasTrabalhados: Int = 0,
    val estudoHoras: Int = 0,
    val livrosLidos: Int = 0,
    val atividadesComplementaresDias: Int = 0
)

@Serializable
data class CalculationResult(
    val progressaoSemiaberto: String?,
    val progressaoAberto: String?,
    val livramentoCondicional: String?,
    val detalhes: String? = null
)

@Serializable
data class ContactInput(
    val nomeCompleto: String,
    val whatsapp: String,
    val email: String? = null,
    val numeroProcesso: String? = null
)

@Serializable
data class ContactSaved(val id: Int, val nomeCompleto: String, val whatsapp: String, val email: String?, val numeroProcesso: String?)

fun toTotalDays(years: Int, months: Int, days: Int): Int {
    return years * 365 + months * 30 + days
}

fun calculateRemissionDays(trabalhoDiasTrabalhados: Int, estudoHoras: Int, livrosLidos: Int, atividadesComplementaresDias: Int): Int {
    val remTrabalho = trabalhoDiasTrabalhados / 3
    val remEstudo = estudoHoras / 12
    val remLeitura = livrosLidos * 4
    return remTrabalho + remEstudo + remLeitura + atividadesComplementaresDias
}

fun normalizeTipoCrime(raw: String): String = raw.trim().uppercase()

fun normalizeStatus(raw: String): String = raw.trim().uppercase()

fun getProgressionFraction(tipoCrime: String, status: String): Double {
    val t = normalizeTipoCrime(tipoCrime)
    val s = normalizeStatus(status)
    return when (t) {
        "COMUM", "NÃO HEDIONDO", "NAO HEDIONDO" -> if (s == "PRIMÁRIO" || s == "PRIMARIO") 0.16 else 0.20
        "VIOLENCIA", "VIOLÊNCIA", "GRAVE_AMEAÇA", "GRAVE AMEAÇA" -> if (s == "PRIMÁRIO" || s == "PRIMARIO") 0.25 else 0.30
        "HEDIONDO" -> if (s == "PRIMÁRIO" || s == "PRIMARIO") 0.40 else 0.60
        "HEDIONDO_MORTE", "HEDIONDO_RESULTADO_MORTE", "HEDIONDO (RESULTADO MORTE)" -> if (s == "PRIMÁRIO" || s == "PRIMARIO") 0.50 else 0.70
        else -> if (s == "PRIMÁRIO" || s == "PRIMARIO") 0.16 else 0.20
    }
}

fun daysForFraction(totalDays: Int, fraction: Double): Int = ceil(totalDays * fraction).toInt()

fun applyDetractionAndRemission(daysNeeded: Int, detraction: Int, remission: Int): Int {
    val remaining = daysNeeded - detraction - remission
    return if (remaining < 0) 0 else remaining
}

fun datePlusDays(start: LocalDate, days: Int): LocalDate {
    return start.plus(5, DateTimeUnit.DAY)
}

val contactStore = mutableListOf<ContactSaved>()
var contactIdSeq = 1

fun main() {
    print("oi")
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
                val totalSentenceDays = toTotalDays(input.penaYears, input.penaMonths, input.penaDays)
                val remissionDays = calculateRemissionDays(
                    input.trabalhoDiasTrabalhados,
                    input.estudoHoras,
                    input.livrosLidos,
                    input.atividadesComplementaresDias
                )
                val fractionSemiaberto = getProgressionFraction(input.tipoCrime, input.statusApenado)
                val daysNeededSemiaberto = daysForFraction(totalSentenceDays, fractionSemiaberto)
                val remainingDaysSemiaberto = applyDetractionAndRemission(daysNeededSemiaberto, input.detraçãoDias, remissionDays)
                val dateSemiaberto = if (daysNeededSemiaberto == 0) null else datePlusDays(startDate, remainingDaysSemiaberto)

                val fractionAberto = when (normalizeTipoCrime(input.tipoCrime)) {
                    "HEDIONDO", "HEDIONDO_MORTE" -> 0.60
                    else -> 0.50
                }
                val daysNeededAberto = daysForFraction(totalSentenceDays, fractionAberto)
                val remainingDaysAberto = applyDetractionAndRemission(daysNeededAberto, input.detraçãoDias, remissionDays)
                val dateAberto = if (daysNeededAberto == 0) null else datePlusDays(startDate, remainingDaysAberto)

                val tipo = normalizeTipoCrime(input.tipoCrime)
                val status = normalizeStatus(input.statusApenado)
                var liberdadesMsg: String? = null
                var dateLiberdade: LocalDate? = null
                if (tipo == "HEDIONDO_MORTE") {
                    liberdadesMsg = "Livramento condicional vedado para crimes hediondos com resultado de morte."
                } else {
                    val fractionForConditional = when {
                        tipo == "HEDIONDO" -> 2.0 / 3.0
                        status == "PRIMÁRIO" || status == "PRIMARIO" -> 1.0 / 3.0
                        else -> 1.0 / 2.0
                    }
                    val daysNeededConditional = daysForFraction(totalSentenceDays, fractionForConditional)
                    val remainingDaysConditional = applyDetractionAndRemission(daysNeededConditional, input.detraçãoDias, remissionDays)
                    dateLiberdade = datePlusDays(startDate, remainingDaysConditional)
                }

                val result = CalculationResult(
                    progressaoSemiaberto = dateSemiaberto?.toString(),
                    progressaoAberto = dateAberto?.toString(),
                    livramentoCondicional = dateLiberdade?.toString(),
                    detalhes = liberdadesMsg
                )
                call.respond(result)
            }

            post("/contact") {
                val c = call.receive<ContactInput>()
                if (c.nomeCompleto.isBlank()) {
                    call.respond(400, mapOf("error" to "nomeCompleto é obrigatório"))
                    return@post
                }
                if (c.whatsapp.isBlank()) {
                    call.respond(400, mapOf("error" to "whatsapp é obrigatório"))
                    return@post
                }
                val saved = ContactSaved(contactIdSeq++, c.nomeCompleto, c.whatsapp, c.email, c.numeroProcesso)
                contactStore.add(saved)
                call.respond(saved)
            }

            get("/health") {
                call.respond(mapOf("status" to "ok"))
            }
        }
    }.start(wait = true)
}

private fun ApplicationCall.respond(
    message: Int,
    messageType: Map<String, String>
) {
}


