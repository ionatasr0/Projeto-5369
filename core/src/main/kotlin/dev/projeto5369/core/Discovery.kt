package dev.projeto5369.core

/** Public discovery queries, not authenticated YouTube recommendations. */
fun discoveryQueries(defaultQuery: String, mixed: Boolean, edition: Int): List<String> {
    if (!mixed) {
        val alternatives = when(defaultQuery) {
            "documentário completo" -> listOf("documentário natureza completo", "documentário história completo", "documentário viagens completo")
            "palestra ciência" -> listOf("astronomia palestra", "física palestra", "biologia documentário completo")
            "história documentário completo" -> listOf("história do Brasil documentário", "história antiga documentário", "história mundial documentário")
            "show completo ao vivo" -> listOf("concerto completo ao vivo", "festival show completo", "música brasileira show completo")
            "viagem de ônibus" -> listOf("ônibus rodoviário viagem completa", "história dos ônibus documentário", "viagem de ônibus Brasil")
            "tecnologia análise completa" -> listOf("engenharia documentário completo", "história da tecnologia", "tecnologia entrevista completa")
            else -> listOf(defaultQuery)
        }
        return listOf(defaultQuery, alternatives[Math.floorMod(edition,alternatives.size)]).distinct()
    }
    val queries = listOf("documentário completo natureza", "palestra ciência tecnologia", "história documentário completo",
        "show completo ao vivo", "viagem de ônibus", "tecnologia análise completa", "documentário viagens cultura", "astronomia universo documentário")
    val start = Math.floorMod(edition, queries.size)
    return (0..2).map { queries[(start + it) % queries.size] }
}

/** Interleave sources; prefer unseen IDs and avoid a single channel occupying adjacent slots. */
fun discoveryVideos(sources: List<List<Video>>, previousIds: Set<String>, watchedIds: Set<String>): List<Video> {
    val interleaved = (0 until (sources.maxOfOrNull { it.size } ?: 0)).flatMap { index -> sources.mapNotNull { it.getOrNull(index) } }
        .distinctBy(Video::id)
    val pending = interleaved.sortedBy { (if (it.id in previousIds) 2 else 0) + (if (it.id in watchedIds) 1 else 0) }.toMutableList()
    return buildList {
        while (pending.isNotEmpty()) {
            val last = lastOrNull()?.let { it.channelId ?: it.channel }
            val index = pending.indexOfFirst { (it.channelId ?: it.channel).let { key -> key.isBlank() || key != last } }.takeIf { it >= 0 } ?: 0
            add(pending.removeAt(index))
        }
    }
}

fun relatedQuery(video: Video): String = video.title.split(Regex("\\s+")).take(10).joinToString(" ").take(150).ifBlank { video.channel.ifBlank { "vídeos longos" } }
