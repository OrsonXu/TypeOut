package com.aware.smartphoneuse;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;



public class PluginConstants {
    private static List<String> navApps = Arrays.asList("anasun.altimeter", "au.com.fuelmap", "au.gov.wa.pta.transperth", "cab.snapp.passenger.play", "co.findship.FindShip2", "com.alk.copilot.mapviewer", "com.atlogis.camaps", "com.atlogis.northamerica", "com.atlogis.nzmaps", "com.atlogis.sovietmaps", "com.bodunov.GalileoPro", "com.bodunov.galileo", "com.citymapper.app.release", "com.connectiq.r485.mapsr485companion2", "com.coulombtech", "com.crittermap.backcountrynavigator.license", "com.discipleskies.android.polarisnavigation", "com.dot.darbmobile", "com.dream.edge.technologies.gps.map.route.traffic.navigation.path.view.street.live", "com.eclipsim.gpsstatus2", "com.exploroz.traveller", "com.footpath", "com.garmin.android.apps.explore", "com.garmin.android.apps.phonelink", "com.golfcart", "com.google.android.apps.maps", "com.google.android.apps.mapslite", "com.google.android.apps.navlite", "com.google.android.street", "com.gps.route.finder.mobile.location.tracker.maps.navigation", "com.gurtam.gps_trace_orange", "com.gurtam.wialon_client", "com.here.app.maps", "com.indyzalab.transitia", "com.infinum.hak", "com.infolog.pa511", "com.invoxia.track", "com.kaisquare.location", "com.kartatech.karta.gps", "com.mapfactor.navigator", "com.mapquest.android.ace", "com.mapswithme.maps.pro", "com.marineways.android", "com.matchpointgps.app", "com.meridian.vdot", "com.metroview.nav.au", "com.mictale.gpsessentials", "com.myapps.dara.compass", "com.myroutesplus", "com.navfree.android.OSM.ALL", "com.navigation.offlinemaps.gps", "com.navin.app", "com.navitel", "com.nhn.android.nmap", "com.odot.ohgo", "com.onezoneapps.lum.offline", "com.passportparking.mobile.parkchicago", "com.procon.elo", "com.raus.i_m_going_home_v2.pro", "com.skobbler.forevermapngusa", "com.swampsend.maastokartat", "com.sygic.aura", "com.sygic.incar", "com.telenav.app.android.cingular", "com.telenav.app.android.scout4cars", "com.telenav.app.android.scout_us", "com.tomtom.gplay.navapp", "com.total.totalservices", "com.trailbehind.android.gaiagps.pro", "com.transcore.android.iowadot", "com.tranzmate", "com.vecturagames.android.app.gpxviewer", "com.vecturagames.android.app.gpxviewer.pro", "com.verizontelematics.verizonhum", "com.vialsoft.radarbot_free", "com.vialsoft.radars_uk_free", "com.viamichelin.android.viamichelinmobile", "com.voice.navigation.tracker.live.earth.maps", "com.vznavigator.Generic", "com.waze", "com.xatori.Plugshare", "com.zahidcataltas.mgrsharita", "com.zahidcataltas.mgrsutmmappro", "cz.aponia.bor3", "cz.seznam.mapy", "de.hafas.android.rejseplanen", "dk.rdzl.topo.gps", "fi.hsl.app", "fi.rdzl.topo.gps", "gbis.gbandroid", "ge.rdzl.topo.gps", "gov.caltrans.quickmap", "ir.balad", "jp.ne.tour.www.travelko.map", "kr.mappers.AtlanSmart", "menion.android.locus", "menion.android.locus.pro", "net.daum.android.map", "net.easyconn.carman.wws", "net.osmand", "net.osmand.plus", "net.sqrl.nztopo50n", "net.sqrl.nztopo50s", "nl.flitsmeister", "nl.rdzl.topo.gps", "no.rdzl.topo.gps", "nz.rdzl.topo.gps", "org.mapapps.mapyourtown.cuba", "org.mapapps.mapyourtown.philippines", "psyberia.alpinequest.free", "psyberia.alpinequest.full", "ru.dublgis.dgismobile", "ru.yandex.yandexnavi", "se.rdzl.topo.gps", "smartwatchstudios.app.gears3navigation", "streetdirectory.mobile", "taxi.tap30.passenger.play", "uk.co.ordnancesurvey.osmaps", "za.co.tracks4africa.nng.africa");
    private static List<String> excTimeoutAppsOther = Arrays.asList(BuildConfig.APPLICATION_ID, "com.samsung.android.app.cocktailbarservice", "com.android.systemui", "com.android.settings", "com.android.settings.intelligence", "com.google.android.googlequicksearchbox", "com.samsung.android.app.aodservice", "com.google.android.packageinstaller", "com.samsung.android.spay", "com.google.android.googlequicksearchbox", "android", "com.samsung.android.MtpApplication", "com.wssyncmldm", "com.motorola.motodisplay", "com.thetransitapp.droid");
    private static List<String> excCalcApps = Arrays.asList(BuildConfig.APPLICATION_ID, "com.samsung.android.app.cocktailbarservice");
    public static String testApp = "com.aware.testforsmartphoneuse";
    // Getter functions
    public static List<String> getExcTimeoutApps(){
        List<String> excTimeoutApps = new ArrayList<String>();
        excTimeoutApps.addAll(excTimeoutAppsOther);
        excTimeoutApps.addAll(navApps);
        return excTimeoutApps;
    }
    public static List<String> getExcCalcApps(){
        return excCalcApps;
    }
}
