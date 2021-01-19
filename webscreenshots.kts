#!/usr/bin/env kscript

@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
@file:DependsOn("org.seleniumhq.selenium:selenium-java:3.141.59")
@file:DependsOn("org.seleniumhq.selenium:selenium-chrome-driver:3.141.59")

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Keys
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.io.File
import java.nio.file.Paths

// import javax.imageio.ImageIO

enum class ElementIdentifier {
    ID, CLASS, CSS_SELECTOR
}

data class ElementId(val elementIdType: ElementIdentifier, val elementIdValue: String, val required: Boolean = false)

data class Resource(val url: String, val desc: String?, val data: ElementId, val previousClicks: List<ElementId>)

object Config {

    const val AEMET_BASE_URL = "http://www.aemet.es/es/eltiempo/prediccion/municipios/"
    const val AEMET_HOURS_BASE_URL = "http://www.aemet.es/es/eltiempo/prediccion/municipios/horas/"
    val AEMET_DATA_ID = ElementId(ElementIdentifier.ID, "tabla_con_scroll")
    val AEMET_HOURS_DATA_ID = ElementId(ElementIdentifier.ID, "contenedor_grafica")

    const val EL_TIEMPO_BASE_URL = "https://www.eltiempo.es"
    val EL_TIEMPO_DATA_ID = ElementId(ElementIdentifier.CLASS, "m_table_weather_day")
    val EL_TIEMPO_HOURS_DATA_ID = ElementId(ElementIdentifier.CLASS, "m_table_weather_hour")
    val EL_TIEMPO_COOKIES_ID = ElementId(ElementIdentifier.ID, "didomi-notice-agree-button")

    const val TU_TIEMPO_BASE_URL = "https://www.tutiempo.net"
    val TU_TIEMPO_DATA_ID = ElementId(ElementIdentifier.CLASS, "tsl")
    val TU_TIEMPO_CONDITIONS_ID = ElementId(ElementIdentifier.CLASS, "fc-cta-consent")
    val TU_TIEMPO_COOKIES_ID = ElementId(ElementIdentifier.CSS_SELECTOR,"div#DivAceptarCookies a[href=\"#\"]")
    // TODO elementId para cookies para TuTiempo


    val queries = listOf(
        Resource(AEMET_BASE_URL + "laguna-de-duero-id47076", "Aemet - Laguna de Duero", AEMET_DATA_ID, emptyList()),
        Resource(AEMET_BASE_URL + "san-roman-de-hornija-id47150", "Aemet - San Roman de Hornija", AEMET_DATA_ID, emptyList()),
        Resource(AEMET_BASE_URL + "ahigal-de-villarino-id37004", "Aemet - Ahigal de Villarino", AEMET_DATA_ID, emptyList()),
        Resource(AEMET_HOURS_BASE_URL + "laguna-de-duero-id47076", "Aemet (horas) - Laguna de Duero", AEMET_HOURS_DATA_ID, emptyList()),
        Resource(AEMET_HOURS_BASE_URL + "san-roman-de-hornija-id47150", "Aemet (horas) - San Roman de Hornija", AEMET_HOURS_DATA_ID, emptyList()),
        Resource(AEMET_HOURS_BASE_URL + "ahigal-de-villarino-id37004", "Aemet (horas) - Ahigal de Villarino", AEMET_HOURS_DATA_ID, emptyList()),
        Resource(EL_TIEMPO_BASE_URL + "/laguna-de-duero.html", "El Tiempo - Laguna de Duero", EL_TIEMPO_DATA_ID, listOf(EL_TIEMPO_COOKIES_ID)),
        Resource(EL_TIEMPO_BASE_URL + "/san-roman-de-hornija.html", "El Tiempo - San Roman de Hornija", EL_TIEMPO_DATA_ID, listOf(EL_TIEMPO_COOKIES_ID)),
        Resource(EL_TIEMPO_BASE_URL + "/ahigal-de-villarino.html", "El Tiempo - Ahigal de Villarino", EL_TIEMPO_DATA_ID, listOf(EL_TIEMPO_COOKIES_ID)),
        Resource(EL_TIEMPO_BASE_URL + "/laguna-de-duero.html?v=por_hora", "El Tiempo (horas) - Laguna de Duero", EL_TIEMPO_HOURS_DATA_ID, listOf(EL_TIEMPO_COOKIES_ID)),
        Resource(EL_TIEMPO_BASE_URL + "/san-roman-de-hornija.html?v=por_hora", "El Tiempo (horas) - San Roman de Hornija", EL_TIEMPO_HOURS_DATA_ID, listOf(EL_TIEMPO_COOKIES_ID)),
        Resource(EL_TIEMPO_BASE_URL + "/ahigal-de-villarino.html?v=por_hora", "El Tiempo (horas) - Ahigal de Villarino", EL_TIEMPO_HOURS_DATA_ID, listOf(EL_TIEMPO_COOKIES_ID)),
        Resource(TU_TIEMPO_BASE_URL + "/valladolid.html?datos=calendario", "Tu Tiempo - Valladolid", TU_TIEMPO_DATA_ID, listOf(TU_TIEMPO_CONDITIONS_ID, TU_TIEMPO_COOKIES_ID))

        // NOTE: To get your weather forecast, just add the ids of you villages/town here

    )
}


class WebSource {

    suspend fun saveWebData(resource: Resource): String? {
        val options = ChromeOptions()
        options.addArguments("headless") // to run in background and screenshot works better
        val driver = ChromeDriver(options)
        driver.get(resource.url)

        delay(TIME_TO_LOAD)

        resource.previousClicks.forEach {
            it.getElementWithRetry(driver)?.click()
            delay(TIME_TO_LOAD)
        }

        var ele = resource.data.getElementWithRetry(driver) ?: return null

        val screenshot = ele.takeScreenshot(driver)
        driver.quit()

        // Copy the element screenshot to disk
        val fileName = "${resource.desc}.png"
        val screenshotLocation = File(fileName)
        screenshot.copyTo(screenshotLocation, true)
        return fileName
    }


    suspend private fun WebElement.takeScreenshot(driver: WebDriver): File {
        val ts = driver as TakesScreenshot

        // Get the location of element on the page
        val point = this.getLocation()

        // Get width and height of the element
        val eleSize = this.getSize()

        //Resize current window to the set dimension
        driver.manage().window().setSize(Dimension(eleSize.getWidth() + 2 * MARGIN, eleSize.getHeight() + 2 * MARGIN))

        // scroll to get the whole element on screen
        val js = driver as JavascriptExecutor
        js.executeScript("window.scrollBy(${point.getX() - MARGIN},${point.getY() - MARGIN})")
        delay(TIME_TO_RETRY)

        // Get entire page screenshot
        return ts.getScreenshotAs(OutputType.FILE)
    }

    suspend private fun ElementId.getElementWithRetry(driver: WebDriver) : WebElement? {
        try {
            return driver.findElement(this.getBy())
        } catch (nse: NoSuchElementException) {
            delay(TIME_TO_RETRY)
            try {
                return driver.findElement(this.getBy())
            } catch (nse: NoSuchElementException) {
                println("WARN identifier ${this.elementIdValue} not found")
                return null
            }
        }
        return null
    }

    private fun ElementId.getBy(): By = when (this.elementIdType) {
        ElementIdentifier.ID -> By.id(this.elementIdValue)
        ElementIdentifier.CLASS -> By.className(this.elementIdValue)
        ElementIdentifier.CSS_SELECTOR -> By.cssSelector(this.elementIdValue)
    }

    companion object {
        const val MARGIN = 10 // element point is not exactly
        const val TIME_TO_RETRY = 1000L
        const val TIME_TO_LOAD = 2000L
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
                "img    {padding: 0; margin: 0;}\n" +
                ".result    {width: 50%; height: 600px; display: inline-block; padding: 0; margin: 0;}\n" +
                ".img_title    {padding: 2 0; margin: 0; background-color: #95B6E9; color: black; font-weight: bold; text-align: center; display: block}" +
                "</style>\n" +
                "</head>")
            out.println("<body>")
            sections.forEach {
                out.println("<div class=\"result\">" +
                    "<span class=\"img_title\">$it</span>" +
                    "<img alt=\"$it\" src=\"file://${Paths.get("").toAbsolutePath().toString()}/$it\" width=\"100%;\">" +
                    "</img></div>")
            }
            out.println("</body></html>")
        }
    }

    companion object {
        const val OUTPUT_FILE = "tiempoI.html"
    }
}

val webSource = WebSource()


val webloads = mutableListOf<Deferred<String?>>()
val htmlContent = mutableListOf<String>()

runBlocking {  // blocking the main thread waiting for all coroutines results
    coroutineScope { // creating coroutines context
        // creation of Future definitions
        Config.queries.forEach {
            webloads.add(async { webSource.saveWebData(it) })
        }
        // wating for all results
        htmlContent.addAll(webloads.awaitAll().filterNotNull())
    }
}

HtmlCreator().createFile(htmlContent.toTypedArray())