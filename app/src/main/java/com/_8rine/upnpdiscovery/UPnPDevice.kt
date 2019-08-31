package com._8rine.upnpdiscovery

import com.stanfy.gsonxml.GsonXmlBuilder
import com.stanfy.gsonxml.XmlParserCreator
import org.xmlpull.v1.XmlPullParserFactory

class UPnPDevice internal constructor(val hostAddress: String, header: String) {
    val location: String
    val server: String
    private val usn: String
    private val st: String

    // XML content
    private var descriptionXML: String? = null

    // From description XML
    private var deviceType: String = ""
    var friendlyName: String = ""
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
        val locationText = "LOCATION: "
        this.location = parseHeader(header, locationText)
        val serverText = "SERVER: "
        this.server = parseHeader(header, serverText)
        val usnText = "USN: "
        this.usn = parseHeader(header, usnText)
        val stText = "ST: "
        this.st = parseHeader(header, stText)
    }

    internal fun update(xml: String) {
        this.descriptionXML = xml
        xmlParse(xml)
    }

    override fun toString(): String {
        return "FriendlyName: " + friendlyName + LINE_END +
                "ModelName: " + modelName + LINE_END +
                "HostAddress: " + hostAddress + LINE_END +
                "Location: " + location + LINE_END +
                "Server: " + server + LINE_END +
                "USN: " + usn + LINE_END +
                "ST: " + st + LINE_END +
                "DeviceType: " + deviceType + LINE_END +
                "PresentationURL: " + presentationURL + LINE_END +
                "SerialNumber: " + serialNumber + LINE_END +
                "ModelURL: " + modelURL + LINE_END +
                "ModelNumber: " + modelNumber + LINE_END +
                "Manufacturer: " + manufacturer + LINE_END +
                "ManufacturerURL: " + manufacturerURL + LINE_END +
                "UDN: " + udn + LINE_END +
                "URLBase: " + urlBase
    }

    private fun parseHeader(mSearchAnswer: String, whatSearch: String): String {
        var result = ""
        var searchLinePos = mSearchAnswer.indexOf(whatSearch)
        if (searchLinePos != -1) {
            searchLinePos += whatSearch.length
            val locColon = mSearchAnswer.indexOf(LINE_END, searchLinePos)
            result = mSearchAnswer.substring(searchLinePos, locColon)
        }
        return result
    }

    private fun xmlParse(xml: String) {
        val parserCreator = XmlParserCreator {
            try {
                return@XmlParserCreator XmlPullParserFactory.newInstance().newPullParser()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        val gsonXml = GsonXmlBuilder()
                .setXmlParserCreator(parserCreator)
                .create()


        val model = gsonXml.fromXml(xml, DescriptionModel::class.java)

        this.friendlyName = model.device!!.friendlyName
        this.deviceType = model.device.deviceType
        this.presentationURL = model.device.presentationURL
        this.serialNumber = model.device.serialNumber
        this.modelName = model.device.modelName
        this.modelNumber = model.device.modelNumber
        this.modelURL = model.device.modelURL
        this.manufacturer = model.device.manufacturer
        this.manufacturerURL = model.device.manufacturerURL
        this.udn = model.device.udn
        this.urlBase = model.urlBase
    }

    private class Device {
        val deviceType: String = ""
        val friendlyName: String = ""
        val presentationURL: String = ""
        val serialNumber: String = ""
        val modelName: String = ""
        val modelNumber: String = ""
        val modelURL: String = ""
        val manufacturer: String = ""
        val manufacturerURL: String = ""
        val udn: String = ""

    }

    private class DescriptionModel {
        val device: Device? = null
        val urlBase: String = ""

    }

    companion object {
        private const val LINE_END = "\r\n"
    }
}