package dev.projeto5369.core

import kotlinx.serialization.json.*

/** Original schema adapter. No code or runtime dependencies from reference clients. */
object YouTubeParser {
    val json = Json { ignoreUnknownKeys = true }
    fun parse(text: String): JsonObject = try { json.parseToJsonElement(text).jsonObject }
        catch (_: Exception) { throw schema() }
    fun schema() = CatalogException(CatalogException.Kind.SCHEMA, "O formato público do YouTube mudou. Tente abrir o vídeo pelo link.")
    fun string(obj: JsonObject, key: String): String = (obj[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
    fun text(element: JsonElement?): String {
        val obj = element as? JsonObject ?: return ""
        return string(obj,"simpleText").ifBlank {
            (obj["runs"] as? JsonArray)?.joinToString("") { (it as? JsonObject)?.let { r -> string(r,"text") }.orEmpty() }.orEmpty()
        }
    }
    fun objects(root: JsonElement): Sequence<JsonObject> = sequence {
        when(root) {
            is JsonObject -> { yield(root); for (value in root.values) yieldAll(objects(value)) }
            is JsonArray -> for(value in root) yieldAll(objects(value))
            else -> Unit
        }
    }
    /** Balanced scanner handles braces and escaped quotes inside JSON strings. */
    fun embedded(html: String, marker: String): JsonObject? {
        val start = html.indexOf(marker).takeIf { it >= 0 } ?: return null
        val open = html.indexOf('{', start + marker.length).takeIf { it >= 0 && it - start < 100 } ?: return null
        var depth = 0; var quoted = false; var escaped = false
        for(i in open until html.length) {
            val c = html[i]
            if(quoted) { if(escaped) escaped = false else if(c == '\\') escaped = true else if(c == '"') quoted = false }
            else when(c) { '"' -> quoted = true; '{' -> depth++; '}' -> { depth--; if(depth == 0) return parse(html.substring(open,i+1)) } }
        }
        return null
    }
    fun initial(html: String): JsonObject = embedded(html,"var ytInitialData =")
        ?: embedded(html,"window[\"ytInitialData\"] =") ?: embedded(html,"ytInitialData =") ?: throw schema()
    fun context(html: String): JsonObject? = embedded(html,"\"INNERTUBE_CONTEXT\":")
    fun duration(value: String): Long? {
        val parts = value.trim().split(':'); if(parts.size !in 2..3) return null
        val nums = parts.map { it.toLongOrNull() ?: return null }
        if(nums.any { it < 0 } || nums.drop(1).any { it >= 60 }) return null
        return nums.fold(0L) { acc,n -> acc*60+n }
    }
    private fun renderer(obj: JsonObject): Video? {
        val id = string(obj,"videoId").takeIf { Regex("[A-Za-z0-9_-]{11}").matches(it) } ?: return null
        val title = text(obj["title"]).ifBlank { return null }
        val byline = obj["ownerText"] ?: obj["shortBylineText"] ?: obj["longBylineText"]
        val channelId = byline?.let { objects(it).mapNotNull { o -> (o["browseEndpoint"] as? JsonObject)?.let { b -> string(b,"browseId").takeIf { it.startsWith("UC") } } }.firstOrNull() }
        val shorts = objects(obj).any { string(it,"webPageType") == "WEB_PAGE_TYPE_SHORTS" || string(it,"url").startsWith("/shorts/") || string(it,"style") == "SHORTS" }
        val time = text(obj["lengthText"]).ifBlank { objects(obj).map { text(it["text"]) }.firstOrNull { duration(it) != null }.orEmpty() }
        val thumbnail = (obj["thumbnail"] as? JsonObject)?.get("thumbnails")?.let { it as? JsonArray }?.lastOrNull()?.let { it as? JsonObject }?.let { string(it,"url") }.orEmpty()
        return Video(id,title,text(byline),channelId,duration(time),thumbnail,isShort=shorts)
    }
    private fun lockup(obj: JsonObject): Video? {
        if(string(obj,"contentType") != "LOCKUP_CONTENT_TYPE_VIDEO") return null
        val id = string(obj,"contentId").takeIf { Regex("[A-Za-z0-9_-]{11}").matches(it) } ?: return null
        val metadata = objects(obj).firstNotNullOfOrNull { it["lockupMetadataViewModel"] as? JsonObject } ?: return null
        val title = (metadata["title"] as? JsonObject)?.let { string(it,"content") }.orEmpty().ifBlank { return null }
        val rows = objects(metadata).mapNotNull { it["text"] as? JsonObject }.map { string(it,"content") }.filter(String::isNotBlank).toList()
        val channelId = objects(metadata).mapNotNull { (it["browseEndpoint"] as? JsonObject)?.let { b -> string(b,"browseId").takeIf { it.startsWith("UC") } } }.firstOrNull()
        val duration = objects(obj).map { string(it,"text") }.mapNotNull(::duration).firstOrNull()
        val thumbnail = objects(obj).map { string(it,"url") }.firstOrNull { it.startsWith("https://i.ytimg.com/") }.orEmpty()
        val isShort = objects(obj).any { string(it,"url").startsWith("/shorts/") || it.containsKey("reelWatchEndpoint") }
        return Video(id,title,rows.firstOrNull().orEmpty(),channelId,duration,thumbnail,isShort=isShort)
    }
    /** Shorts appear as shortsLockupViewModel / reelItemRenderer, or as regular renderers pointing at /shorts/. */
    private fun shortsLockup(obj: JsonObject): Video? {
        val id = objects(obj).mapNotNull { (it["reelWatchEndpoint"] as? JsonObject)?.let { r -> string(r,"videoId") } }
            .firstOrNull { Regex("[A-Za-z0-9_-]{11}").matches(it) } ?: return null
        val overlay = obj["overlayMetadata"] as? JsonObject
        val title = (overlay?.get("primaryText") as? JsonObject)?.let { string(it,"content") }.orEmpty()
            .ifBlank { string(obj,"accessibilityText").substringBefore(",").trim() }.ifBlank { return null }
        val views = (overlay?.get("secondaryText") as? JsonObject)?.let { string(it,"content") }.orEmpty()
        val thumbnail = objects(obj).map { string(it,"url") }.firstOrNull { it.startsWith("https://i.ytimg.com/") }
            ?: "https://i.ytimg.com/vi/$id/oar2.jpg"
        return Video(id,title,"",null,null,thumbnail,views,isShort=true)
    }
    private fun reel(obj: JsonObject): Video? {
        val id = string(obj,"videoId").takeIf { Regex("[A-Za-z0-9_-]{11}").matches(it) } ?: return null
        val title = text(obj["headline"]).ifBlank { return null }
        val thumbnail = (obj["thumbnail"] as? JsonObject)?.get("thumbnails")?.let { it as? JsonArray }?.lastOrNull()?.let { it as? JsonObject }?.let { string(it,"url") }
            ?: "https://i.ytimg.com/vi/$id/oar2.jpg"
        return Video(id,title,"",null,null,thumbnail,text(obj["viewCountText"]),isShort=true)
    }
    fun shortsPage(root: JsonObject, endpoint: String, context: JsonObject?): Page<Video> {
        val items = objects(root).mapNotNull { obj ->
            (obj["shortsLockupViewModel"] as? JsonObject)?.let(::shortsLockup)
                ?: (obj["reelItemRenderer"] as? JsonObject)?.let(::reel)
                ?: ((obj["videoRenderer"] as? JsonObject)?.let(::renderer) ?: (obj["lockupViewModel"] as? JsonObject)?.let(::lockup))?.takeIf { it.isShort }
        }.distinctBy { it.id }.toList()
        val token = objects(root).mapNotNull { (it["continuationCommand"] as? JsonObject)?.let { cmd -> string(cmd,"token").takeIf(String::isNotBlank) } }.firstOrNull()
        val recognized = objects(root).any { o -> o.keys.any { it in setOf("contents","onResponseReceivedCommands","onResponseReceivedActions","continuationContents") } }
        if(items.isEmpty() && !recognized) throw schema()
        return Page(items, if(token != null && context != null) Cursor(token,endpoint,context.toString()) else null)
    }
    fun page(root: JsonObject, endpoint: String, context: JsonObject?): Page<Video> {
        // Exclude unselected channel tabs and unrelated watch recommendations at the caller.
        val items = objects(root).mapNotNull { obj ->
            (obj["videoRenderer"] ?: obj["gridVideoRenderer"])?.let { it as? JsonObject }?.let(::renderer)
                ?: (obj["lockupViewModel"] as? JsonObject)?.let(::lockup)
        }.filterNot { it.isShort }.distinctBy { it.id }.toList()
        val token = objects(root).mapNotNull { (it["continuationCommand"] as? JsonObject)?.let { cmd -> string(cmd,"token").takeIf(String::isNotBlank) } }.firstOrNull()
        val recognized = objects(root).any { o -> o.keys.any { it in setOf("contents","onResponseReceivedCommands","onResponseReceivedActions","continuationContents") } }
        if(items.isEmpty() && !recognized) throw schema()
        return Page(items, if(token != null && context != null) Cursor(token,endpoint,context.toString()) else null)
    }
    fun channel(root: JsonObject, id: String, context: JsonObject?): ChannelPage {
        val metadata = objects(root).firstNotNullOfOrNull { it["channelMetadataRenderer"] as? JsonObject }
        val channel = Channel(id, metadata?.let { string(it,"title") }.orEmpty().ifBlank { id },metadata?.let { string(it,"description") }.orEmpty())
        val selected = objects(root).firstNotNullOfOrNull { (it["tabRenderer"] as? JsonObject)?.takeIf { t -> (t["selected"] as? JsonPrimitive)?.booleanOrNull == true } }
        val page = page(selected?.get("content") as? JsonObject ?: root,"browse",context)
        return ChannelPage(channel,page.copy(items = page.items.map { it.copy(channel = it.channel.ifBlank { channel.title },channelId = it.channelId ?: id) }))
    }
    fun details(html: String, id: String): Video {
        val response = embedded(html,"var ytInitialPlayerResponse =") ?: embedded(html,"ytInitialPlayerResponse =")
        val d = response?.get("videoDetails") as? JsonObject
        if(d == null) throw CatalogException(CatalogException.Kind.UNAVAILABLE,"Metadados indisponíveis. O vídeo pode exigir autorização ou estar indisponível.")
        return Video(id,string(d,"title"),string(d,"author"),string(d,"channelId"),string(d,"lengthSeconds").toLongOrNull(),
            "https://i.ytimg.com/vi/$id/hqdefault.jpg",string(d,"shortDescription"))
    }
}
