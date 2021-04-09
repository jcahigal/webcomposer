#!/usr/bin/env kscript

@file:DependsOn("org.jsoup:jsoup:1.13.1")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.coroutineScope
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.File
import java.nio.file.Paths

object Config {
    const val AEMET_KEY = "Aemet"
    const val TIEMPO_KEY = "ElTiempo"
    const val TU_TIEMPO_KEY = "TuTiempo"

    val queries = mapOf<String, List<String>>(
        // NOTE: To get your weather forecast, just add the ids of you villages/town here
        AEMET_KEY to listOf("laguna-de-duero-id47076", "san-roman-de-hornija-id47150", "ahigal-de-villarino-id37004"),
        TU_TIEMPO_KEY to listOf("valladolid"),
        TIEMPO_KEY to listOf("laguna-de-duero", "san-roman-de-hornija", "ahigal-de-villarino")
    )
}

data class Iframe(val body: Element, val css: List<Element>, val title: String)

abstract class WebSource {
    abstract fun saveWebData(id: String): String?

    abstract fun getBaseUrl(): String

    fun Document.getHeaders() : List<Element> {
        val headers = mutableListOf<Element>()
        headers.addAll(this.select("head > link[rel=stylesheet]"))
        headers.forEach {
            if(it.attr("href") != "") {
                it.attr("href", it.attr("abs:href")) // complete the whole url
            }
        }
        headers.addAll(this.select("head > style"))

        val jsHeaders = mutableListOf<Element>()
        jsHeaders.addAll(this.select("head > script"))
        jsHeaders.forEach {
            if(it.attr("src") != "") {
                it.attr("src", it.attr("abs:src"))  // complete the whole url
            }
        }
        headers.addAll(jsHeaders)

        val iFrameStyle = Element("style").append(".iframe_title    {padding: 2 0; margin: 0; width: 100%; " +
            "background-color: #95B6E9; color: black; font-weight: bold; text-align: center; display: block}")
        headers.add(iFrameStyle)
        return headers
    }

    fun Element.replaceImgUrls(): Element {
        this.select("img").forEach {
            it.attr("src", getBaseUrl() + it.attr("src"))
        }
        return this
    }

    fun Iframe.save(file: String): String? {
        val iFrameTitle = Element("span").append(this.title).attr("class", "iframe_title")
        val body = Element("body").appendChild(iFrameTitle).appendChild(this.body)
        val head = Element("head")
        this.css.forEach { head.appendChild(it) }

        File(file).printWriter().use { out ->
            out.println(Element("html").appendChild(head).appendChild(body).outerHtml())
        }
        return file
    }

}

class AemetDays : WebSource() {
    override fun saveWebData(id: String): String? {
        val document = Jsoup.connect(FULL_URL + id).get() // OK response or exception
        val body = document.select("div#tabla_con_scroll").first()?.replaceImgUrls() ?: return null
        return Iframe(body, document.getHeaders(), "${Config.AEMET_KEY} $id").save("Aemet-days-" + id + ".html")
    }

    override fun getBaseUrl() = BASE_URL

    companion object {
        const val BASE_URL = "http://www.aemet.es"
        const val FULL_URL = BASE_URL + "/es/eltiempo/prediccion/municipios/"
    }
}

class AemetHours : WebSource() {
    override fun saveWebData(id: String): String? {
        val document = Jsoup.connect(FULL_URL + id).get() // OK response or exception
        val body = document.select("div#contenedor_grafica").first()?.replaceImgUrls() ?: return null
        return Iframe(body, document.getHeaders(), "${Config.AEMET_KEY} $id").save("Aemet-hours-" + id + ".html")
    }

    override fun getBaseUrl() = BASE_URL

    companion object {
        const val BASE_URL = "http://www.aemet.es"
        const val FULL_URL = BASE_URL + "/es/eltiempo/prediccion/municipios/horas/"
    }
}

class TiempoDays : WebSource() {
    // it has dinamyc load so its read in several steps (to get headers and content from ajax request)
    override fun saveWebData(id: String): String? {
        val document = Jsoup.connect(BASE_URL + "/" + id + ".html").get() // OK response or exception

        // sometimes the response is returned on ajax request, others on the main request
        val body = document.select("div.m_table_weather_day").first() ?: Jsoup.connect(BASE_URL + "/" + id + ".html~ROW_NUMBER_4~").get() ?: return null

        return Iframe(body, document.getHeaders(), "${Config.TIEMPO_KEY} $id").save("Tiempo-days-" + id + ".html")
    }

    override fun getBaseUrl() = BASE_URL

    companion object {
        const val BASE_URL = "https://www.eltiempo.es"
    }
}

class TiempoHours : WebSource() {
    override fun saveWebData(id: String): String? {
        val document = Jsoup.connect(BASE_URL + "/" + id + ".html?v=por_hora").get() // OK response or exception
        val body = document.select("ul.m_table_weather_hour").first() ?: return null
        return Iframe(body, document.getHeaders(), "${Config.TIEMPO_KEY} $id").save("Tiempo-hours-" + id + ".html")
    }

    override fun getBaseUrl() = BASE_URL

    companion object {
        const val BASE_URL = "https://www.eltiempo.es"
    }
}

class TuTiempo : WebSource() {
    override fun saveWebData(id: String): String? {
        val document = Jsoup.connect(BASE_URL + "/" + id + ".html?datos=calendario").get() // OK response or exception
        val body = document.select("table.tsl").first() ?: return null
        return Iframe(body, document.getHeaders(), "${Config.TU_TIEMPO_KEY} $id").save("TuTiempo-" + id + ".html")
    }

    override fun getBaseUrl() = BASE_URL

    companion object {
        const val BASE_URL = "https://www.tutiempo.net"
    }
}

class HtmlCreator {
    fun createFile(sections: Array<String>) {
        val outputFile = File(OUTPUT_FILE)
        outputFile.printWriter().use { out ->
            out.println("<html>")
            out.println("<head>\n" +
                "<style>\n" +
                "body   {display: table}\n" +
                "iframe    {padding: 0; margin: 0; width: 100%; height: 100%;}\n" +
                ".result    {width: 50%; height: 600px; display: inline-block; padding: 0; margin: 0;}\n" +
                "</style>\n" +
                "</head>")
            out.println("<body>")
            sections.forEach {
                out.println("<div class=\"result\">" +
                    "<iframe title=\"$it\" src=\"file://${Paths.get("").toAbsolutePath().toString()}/$it\" width=\"100%;\">" +
                    "</iframe></div>")
            }
            out.println("</body></html>")
        }
    }

    companion object {
        const val OUTPUT_FILE = "tiempo.html"
    }
}

val aemetDays = AemetDays()
val aemetHours = AemetHours()
val tiempoDays = TiempoDays()
val tiempoHours = TiempoHours()
val tuTiempo = TuTiempo()
// NOTE: Instantiate your class here

val webloads = mutableListOf<Deferred<String?>>()
val htmlContent = mutableListOf<String>()
runBlocking {  // blocking the main thread waiting for all coroutines results
     coroutineScope { // creating coroutines context
         // creation of Future definitions
         Config.queries[Config.AEMET_KEY]?.forEach {
             webloads.add(async {aemetDays.saveWebData(it) })
             webloads.add(async {aemetHours.saveWebData(it) })
         }
         Config.queries[Config.TIEMPO_KEY]?.forEach {
             webloads.add(async {tiempoDays.saveWebData(it) })
             webloads.add(async {tiempoHours.saveWebData(it) })
         }
         Config.queries[Config.TU_TIEMPO_KEY]?.forEach {
             webloads.add(async {tuTiempo.saveWebData(it) })
         }
         // NOTE: add your Config entry here
         // wating for all results
         htmlContent.addAll(webloads.awaitAll().filterNotNull())
    }
}

HtmlCreator().createFile(htmlContent.toTypedArray())