package com.eymen.browser.telefon

import android.text.Html
import android.text.style.URLSpan
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

internal data class ImportedBookmark(val url:String,val title:String,val folder:String?)
internal data class ImportedLibrary(val folders:Map<String,String>,val entries:List<ImportedBookmark>,val skipped:Int)

/** Feature 13. Bounded local files; imported HTML is parsed as text, never executed. */
internal object BookmarkExchange {
    const val MAX_BYTES=8*1024*1024
    fun read(stream:InputStream):String {
        val bytes=stream.use {input ->
            val out=java.io.ByteArrayOutputStream();val block=ByteArray(8192)
            while(true){val size=input.read(block);if(size<0)break
                require(out.size()+size<=MAX_BYTES);out.write(block,0,size)}
            out.toByteArray()
        }
        return Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF")
    }
    fun decode(raw:String):ImportedLibrary {
        if(!raw.trimStart().startsWith("{"))return decodeHtml(raw)
        val obj=JSONObject(raw)
        require(obj.getString("format")=="eymen-bookmarks" && obj.getInt("version")==1)
        val folders=obj.getJSONArray("folders");val items=obj.getJSONArray("bookmarks")
        require(folders.length()<=BookmarkPolicy.MAX_FOLDERS && items.length()<=BookmarkPolicy.MAX_ENTRIES)
        val names=linkedMapOf<String,String>()
        for(i in 0 until folders.length()){
            val folder=folders.getJSONObject(i);val key=folder.getString("key");val name=BookmarkPolicy.name(folder.getString("name"))
            require(key.isNotBlank() && key.length<=120 && key !in names && name.isNotEmpty());names[key]=name
        }
        val entries=buildList {
            for(i in 0 until items.length()){
                val row=items.getJSONObject(i);val url=BookmarkPolicy.url(row.getString("url"))?:error("address")
                val key=if(row.isNull("folder"))null else row.getString("folder").also{require(it in names)}
                add(ImportedBookmark(url,BookmarkPolicy.title(row.getString("title"),url),key))
            }
        }
        return ImportedLibrary(names,entries,0)
    }
    private fun decodeHtml(raw:String):ImportedLibrary {
        require(raw.contains("<a",true))
        val text=Html.fromHtml(raw,Html.FROM_HTML_MODE_LEGACY)
        val links=text.getSpans(0,text.length,URLSpan::class.java)
        require(links.size<=BookmarkPolicy.MAX_ENTRIES)
        var skipped=0
        val entries=links.mapNotNull {span ->
            val url=BookmarkPolicy.url(span.url)
            if(url==null){skipped++;null} else {
                val title=text.subSequence(text.getSpanStart(span),text.getSpanEnd(span)).toString()
                ImportedBookmark(url,BookmarkPolicy.title(title,url),null)
            }
        }
        require(entries.isNotEmpty())
        return ImportedLibrary(emptyMap(),entries,skipped)
    }
    fun encode(entries:List<BookmarkEntry>,folders:List<BookmarkFolder>):String {
        val names=JSONArray();for(folder in folders)names.put(JSONObject().put("key",folder.id.toString()).put("name",folder.name))
        val rows=JSONArray();for(item in entries)rows.put(JSONObject().put("url",item.url).put("title",item.title).put("folder",item.folderId?.toString()?:JSONObject.NULL))
        return JSONObject().put("format","eymen-bookmarks").put("version",1).put("folders",names).put("bookmarks",rows).toString(2)
    }
    fun html(entries:List<BookmarkEntry>,folders:List<BookmarkFolder>):String {
        fun escape(value:String)=Html.escapeHtml(value)
        fun row(item:BookmarkEntry)="<DT><A HREF=\"${escape(item.url)}\">${escape(item.title)}</A>\n"
        return buildString {
            append("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n<TITLE>Bookmarks</TITLE>\n<H1>Bookmarks</H1>\n<DL><p>\n")
            entries.filter{it.folderId==null}.forEach{append(row(it))}
            for(folder in folders){append("<DT><H3>${escape(folder.name)}</H3>\n<DL><p>\n")
                entries.filter{it.folderId==folder.id}.forEach{append(row(it))};append("</DL><p>\n")}
            append("</DL><p>\n")
        }
    }
}
