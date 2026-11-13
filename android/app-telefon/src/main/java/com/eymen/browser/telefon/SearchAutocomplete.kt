package com.eymen.browser.telefon

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Debounced optional search-engine suggestions. No proxy/analytics service,
 * no secret/private history, no URL/host auto-complete sent to the network.
 * Provider endpoint is undocumented; local results still work if unavailable.
 */
internal class SearchAutocomplete {
    private val ui=Handler(Looper.getMainLooper())
    private val worker=Executors.newSingleThreadExecutor()
    private var ticket=0
    private var pending:Runnable?=null
    private var closed=false

    fun request(raw:String,engine:String,privateMode:Boolean,childMode:Boolean,enabled:Boolean,
                onResults:(List<String>)->Unit) {
        cancel()
        val query=SearchSuggestPolicy.eligible(raw,engine,privateMode,childMode,enabled)
        if(query==null || closed){onResults(emptyList());return}
        val localTicket=ticket
        val scheduled=Runnable {
            worker.execute {
                val result=fetch(query)
                ui.post {if(!closed && ticket==localTicket)onResults(result)}
            }
        }
        pending=scheduled
        ui.postDelayed(scheduled,280L)
    }
    fun cancel(){
        ticket++
        pending?.let{ui.removeCallbacks(it)}
        pending=null
    }
    fun close(){closed=true;cancel();worker.shutdownNow()}

    private fun fetch(query:String):List<String> {
        val language=Locale.getDefault().language.take(5).ifBlank{"en"}
        val url="https://suggestqueries.google.com/complete/search?client=firefox&hl="+
            URLEncoder.encode(language,"UTF-8")+"&q="+URLEncoder.encode(query,"UTF-8")
        var connection:HttpURLConnection?=null
        return try {
            connection=(URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout=2200;readTimeout=2200;instanceFollowRedirects=false
                useCaches=false;setRequestProperty("Accept","application/json")
            }
            if(connection.responseCode!=200)return emptyList()
            val payload=connection.inputStream.bufferedReader(Charsets.UTF_8).use{reader ->
                val buffer=CharArray(16384)
                val length=reader.read(buffer)
                if(length<1)"" else String(buffer,0,length)
            }
            val array=JSONArray(payload)
            if(!array.optString(0).equals(query,true))return emptyList()
            val options=array.optJSONArray(1) ?: return emptyList()
            (0 until options.length()).asSequence().map{options.optString(it).trim()}
                .filter{it.isNotBlank() && it.length<=100 && !it.any(Char::isISOControl)}
                .distinct().take(6).toList()
        } catch (_:Exception){emptyList()}
        finally {connection?.disconnect()}
    }
}
