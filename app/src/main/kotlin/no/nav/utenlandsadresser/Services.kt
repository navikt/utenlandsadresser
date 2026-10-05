package no.nav.utenlandsadresser

import no.nav.utenlandsadresser.application.port.inbound.HåndterLivshendelse
import no.nav.utenlandsadresser.application.port.inbound.LesFeed
import no.nav.utenlandsadresser.application.port.inbound.LesUtenlandskIdFeed
import no.nav.utenlandsadresser.application.port.inbound.SkrivSporingslogg
import no.nav.utenlandsadresser.application.port.inbound.SlettSporingslogg
import no.nav.utenlandsadresser.application.port.inbound.StartAbonnement
import no.nav.utenlandsadresser.application.port.inbound.StartUtenlandskIdAbonnement
import no.nav.utenlandsadresser.application.port.inbound.StoppAbonnement
import no.nav.utenlandsadresser.application.port.inbound.StoppUtenlandskIdAbonnement
import no.nav.utenlandsadresser.application.port.inbound.UtenlandskIdTilgjengelig

data class Services(
    val startAbonnement: StartAbonnement,
    val stoppAbonnement: StoppAbonnement,
    val lesFeed: LesFeed,
    val startUtenlandskIdAbonnement: StartUtenlandskIdAbonnement,
    val stoppUtenlandskIdAbonnement: StoppUtenlandskIdAbonnement,
    val lesUtenlandskIdFeed: LesUtenlandskIdFeed,
    val utenlandskIdTilgjengelig: UtenlandskIdTilgjengelig,
    val håndterLivshendelse: HåndterLivshendelse,
    val skrivSporingslogg: SkrivSporingslogg,
    val slettSporingslogg: SlettSporingslogg,
)
