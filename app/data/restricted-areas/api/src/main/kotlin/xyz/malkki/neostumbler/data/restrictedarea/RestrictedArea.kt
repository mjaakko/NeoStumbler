package xyz.malkki.neostumbler.data.restrictedarea

import java.util.UUID
import xyz.malkki.neostumbler.geography.Circle

/** Restricted area where reports will not be created in */
data class RestrictedArea(val id: UUID, val circle: Circle)
