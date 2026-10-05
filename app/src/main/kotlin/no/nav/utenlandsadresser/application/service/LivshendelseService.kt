package no.nav.utenlandsadresser.application.service

import no.nav.utenlandsadresser.application.port.inbound.HåndterLivshendelse
import no.nav.utenlandsadresser.application.port.outbound.AbonnementRepository
import no.nav.utenlandsadresser.application.port.outbound.FeedRepository
import no.nav.utenlandsadresser.domain.AdressebeskyttelseGradering
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
import no.nav.utenlandsadresser.domain.Livshendelse
import no.nav.utenlandsadresser.domain.Livshendelse.Adressebeskyttelse.Gradering
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class LivshendelseService(
    private val abonnementRepository: AbonnementRepository,
    private val feedRepository: FeedRepository,
) : HåndterLivshendelse {
    override suspend fun håndter(livshendelse: Livshendelse) {
        val abonnementer = abonnementRepository.hentAbonnementer(livshendelse.personidenter)
        if (abonnementer.isEmpty()) return

        val hendelsestype = livshendelse.tilHendelsestype()
        val events =
            abonnementer.map {
                FeedEvent.Incoming(
                    identitetsnummer = it.identitetsnummer,
                    abonnementId = it.id,
                    hendelsestype = hendelsestype,
                    organisasjonsnummer = it.organisasjonsnummer,
                )
            }

        /*
        Når en adresse endres, får vi én melding for hvert felt som er endret. Det gir mye støy i feeden.
        Derfor hopper vi over en hendelse hvis en lik hendelse er lagt på feeden innenfor DUPLIKATVINDU.
         */
        feedRepository.opprettUtenDuplikater(events, DUPLIKATVINDU)
    }

    companion object {
        val DUPLIKATVINDU: Duration = 10.seconds
    }
}

/**
 * Alle graderinger unntatt [Gradering.UGRADERT] skjuler adressen. Mottakeren skal da slette den.
 */
private fun Livshendelse.tilHendelsestype(): Hendelsestype =
    when (this) {
        is Livshendelse.Adressebeskyttelse -> {
            when (gradering) {
                Gradering.STRENGT_FORTROLIG_UTLAND,
                Gradering.STRENGT_FORTROLIG,
                Gradering.FORTROLIG,
                -> {
                    Hendelsestype.Adressebeskyttelse(AdressebeskyttelseGradering.GRADERT)
                }

                Gradering.UGRADERT -> {
                    Hendelsestype.Adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT)
                }
            }
        }

        is Livshendelse.Bostedsadresse,
        is Livshendelse.Kontaktadresse,
        -> {
            Hendelsestype.OppdatertAdresse
        }
    }
