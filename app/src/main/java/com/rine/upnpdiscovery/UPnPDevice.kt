package com.rine.upnpdiscovery

import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream

class UPnPDevice internal constructor(val hostAddress: String, header: String) {
    internal val location: String
    val server: String

    // XML content
    private var descriptionXML: String = ""

    // From description XML
    var friendlyName: String = ""
    private var deviceType: String = ""
    private var presentationURL: String = ""
    private var serialNumber: String = ""
    private var modelName: String = ""
    private var modelNumber: String = ""
    private var modelURL: String = ""
    private var manufacturer: String = ""
    private var manufacturerURL: String = ""
    private var udn: String = ""
    private var urlBase: String = ""

    init {
        location = parseHeader(header, "LOCATION: ")
        server = parseHeader(header, "SERVER: ")
    }

    internal fun update(xml: String) {
        descriptionXML = xml
        try {
            xmlParse(xml)
        } catch (e: XmlPullParserException) {
            Log.w(UPnPDiscovery.TAG, e.toString())
        }
    }

    override fun toString(): String =
        "FriendlyName: " + friendlyName + LINE_END +
            "ModelName: " + modelName + LINE_END +
            "HostAddress: " + hostAddress + LINE_END +
            "Location: " + location + LINE_END +
            "DeviceType: " + deviceType + LINE_END +
            "PresentationURL: " + presentationURL + LINE_END +
            "SerialNumber: " + serialNumber + LINE_END +
            "ModelURL: " + modelURL + LINE_END +
            "ModelNumber: " + modelNumber + LINE_END +
            "Manufacturer: " + manufacturer + LINE_END +
            "ManufacturerURL: " + manufacturerURL + LINE_END +
            "UDN: " + udn + LINE_END +
            "URLBase: " + urlBase

    private fun parseHeader(
        mSearchAnswer: String,
        whatSearch: String,
    ): String {
        var result = ""
        var searchLinePos = mSearchAnswer.indexOf(whatSearch)
        if (searchLinePos != -1) {
            searchLinePos += whatSearch.length
            val locColon = mSearchAnswer.indexOf(LINE_END, searchLinePos)
            result = mSearchAnswer.substring(searchLinePos, locColon)
        }
        return result
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    private fun xmlParse(xml: String) {
        val xmlFactoryObject = XmlPullParserFactory.newInstance()
        val parser = xmlFactoryObject.newPullParser()
        parser.setInput(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)), null)
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            if (event == XmlPullParser.START_TAG) {
                when (name) {
                    "friendlyName" -> friendlyName = readText(parser)
                    "deviceType" -> deviceType = readText(parser)
                    "presentationURL" -> presentationURL = readText(parser)
                    "serialNumber" -> serialNumber = readText(parser)
                    "modelName" -> modelName = readText(parser)
                    "modelNumber" -> modelNumber = readText(parser)
                    "modelURL" -> modelURL = readText(parser)
                    "manufacturer" -> manufacturer = readText(parser)
                    "manufacturerURL" -> manufacturerURL = readText(parser)
                    "UDN" -> udn = readText(parser)
                    "URLBase" -> urlBase = readText(parser)
                }
            }
            event = parser.next()
        }
    }

    companion object {
        private const val LINE_END = "\r\n"
    }
}
