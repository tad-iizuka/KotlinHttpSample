package co.ubunifu.kotlinhttpsample

import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

operator fun JSONArray.iterator(): Iterator<JSONObject> =
        (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

val TIMEOUT = 10*1000

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.INVISIBLE

        val button = findViewById<Button>(R.id.buttonGet)
        button.setOnClickListener() {
            progressBar.visibility = View.VISIBLE
            HttpTask( {
                progressBar.visibility = View.INVISIBLE
                if (it == null) {
                    println("connection error")
                    return@HttpTask
                }
                for (json in JSONArray(it)) {
                    println(json)
                }
            } ).execute("GET", "http://192.168.33.10/api/articles")
        }

        val buttonPost = findViewById<Button>(R.id.buttonPost)
        buttonPost.setOnClickListener() {

            val json = JSONObject()
            json.put("title", "TITLE")
            json.put("content", "CONTENT")
            json.put("done", false)

            progressBar.visibility = View.VISIBLE
            HttpTask( {
                progressBar.visibility = View.INVISIBLE
                if (it == null) {
                    println("connection error")
                    return@HttpTask
                }
                println(it)
            } ).execute("POST", "http://192.168.33.10/api/article/1", json.toString())
        }

    }

    class HttpTask(callback: (String?) -> Unit) : AsyncTask<String, Unit, String>()  {

        var callback = callback

        override fun doInBackground(vararg params: String): String? {
            val url = URL(params[1])
            val httpClient = url.openConnection() as HttpURLConnection
            httpClient.setReadTimeout(TIMEOUT)
            httpClient.setConnectTimeout(TIMEOUT)
            httpClient.requestMethod = params[0]

            if (params[0] == "POST") {
                httpClient.instanceFollowRedirects = false
                httpClient.doOutput = true
                httpClient.doInput = true
                httpClient.useCaches = false
                httpClient.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            try {
                if (params[0] == "POST") {
                    httpClient.connect()
                    val os = httpClient.getOutputStream()
                    val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                    writer.write(params[2])
                    writer.flush()
                    writer.close()
                    os.close()
                }
                if (httpClient.responseCode == HttpURLConnection.HTTP_OK) {
                    val stream = BufferedInputStream(httpClient.inputStream)
                    val data: String = readStream(inputStream = stream)
                    return data
                } else {
                    println("ERROR ${httpClient.responseCode}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                httpClient.disconnect()
            }

            return null
        }

        fun readStream(inputStream: BufferedInputStream): String {
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            bufferedReader.forEachLine { stringBuilder.append(it) }
            return stringBuilder.toString()
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            callback(result)
        }
    }

    companion object {
        val TAG = "MainActivity"
    }
}