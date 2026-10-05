package no.nav.utenlandsadresser

import no.nav.utenlandsadresser.application.port.inbound.LesFeed
import no.nav.utenlandsadresser.application.port.inbound.LesUtenlandskIdFeed
import no.nav.utenlandsadresser.application.port.inbound.StartAbonnement
import no.nav.utenlandsadresser.application.port.inbound.StartUtenlandskIdAbonnement
import no.nav.utenlandsadresser.application.port.inbound.StoppAbonnement
import no.nav.utenlandsadresser.application.port.inbound.StoppUtenlandskIdAbonnement

data class Services(
    val startAbonnement: StartAbonnement,
    val stoppAbonnement: StoppAbonnement,
    val lesFeed: LesFeed,
    val startUtenlandskIdAbonnement: StartUtenlandskIdAbonnement,
    val stoppUtenlandskIdAbonnement: StoppUtenlandskIdAbonnement,
    val lesUtenlandskIdFeed: LesUtenlandskIdFeed,
)
