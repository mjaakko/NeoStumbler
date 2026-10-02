package xyz.malkki.neostumbler.di

import org.koin.dsl.module
import xyz.malkki.neostumbler.data.geocoder.AndroidGeocoder
import xyz.malkki.neostumbler.data.geocoder.Geocoder

val geocoderModule = module { single<Geocoder> { AndroidGeocoder(get()) } }
