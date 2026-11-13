package com.vennela365.viewer.parser

import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

data class MeshVertex(val x: Float, val y: Float, val z: Float)
data class MeshEdge(val a: Int, val b: Int)

data class ParsedMesh3D(
    val formatLabel: String,
    val vertices: List<MeshVertex>,
    val edges: List<MeshEdge>,
    val faceCount: Int,
    val minBounds: MeshVertex,
    val maxBounds: MeshVertex,
    val isBinaryOrUnsupported: Boolean = false,
    val notice: String? = null
)

data class GeoPoint(val lat: Double, val lon: Double, val ele: Double? = null, val label: String = "")
data class GeoPolyline(val name: String, val points: List<GeoPoint>, val closed: Boolean = false)

data class ParsedGeoVector(
    val formatLabel: String,
    val waypoints: List<GeoPoint>,
    val polylines: List<GeoPolyline>,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val totalDistanceKm: Double,
    val minEleMeters: Double?,
    val maxEleMeters: Double?,
    val notice: String? = null
)

/**
 * Pure-Kotlin 3D Geometry (OBJ, ASCII STL) and Geospatial Vector (GeoJSON, GPX, KML) parsers
 * powering interactive Compose Canvas previews without third-party native bloat.
 */
object GeometryAndGeoParser {

    private const val MAX_RENDER_EDGES = 4500

    fun parseMesh(source: String, extension: String): ParsedMesh3D {
        val ext = extension.lowercase().removePrefix(".")
        if (source.isBlank()) {
            return ParsedMesh3D(ext.uppercase(), emptyList(), emptyList(), 0, MeshVertex(0f, 0f, 0f), MeshVertex(0f, 0f, 0f), true, "No ASCII mesh content found.")
        }
        return when (ext) {
            "obj" -> parseObj(source)
            "stl" -> {
                if (source.trimStart().startsWith("solid", ignoreCase = true) && source.contains("vertex", ignoreCase = true)) {
                    parseAsciiStl(source)
                } else {
                    ParsedMesh3D(
                        formatLabel = "BINARY STL",
                        vertices = emptyList(),
                        edges = emptyList(),
                        faceCount = 0,
                        minBounds = MeshVertex(0f, 0f, 0f),
                        maxBounds = MeshVertex(0f, 0f, 0f),
                        isBinaryOrUnsupported = true,
                        notice = "Binary STL detected. Inspect raw header & triangle count in the Hex inspector, or open an ASCII STL / OBJ file for 3D wireframe orbit."
                    )
                }
            }
            else -> ParsedMesh3D(
                formatLabel = ext.uppercase(),
                vertices = emptyList(),
                edges = emptyList(),
                faceCount = 0,
                minBounds = MeshVertex(0f, 0f, 0f),
                maxBounds = MeshVertex(0f, 0f, 0f),
                isBinaryOrUnsupported = true,
                notice = "${ext.uppercase()} CAD/3D containers require proprietary NURBS/scene engines; header and bytes remain safely inspectable."
            )
        }
    }

    private fun parseObj(source: String): ParsedMesh3D {
        val rawVerts = mutableListOf<MeshVertex>()
        val edgeSet = LinkedHashSet<Long>()
        val edges = mutableListOf<MeshEdge>()
        var faceCount = 0

        source.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.startsWith("v ")) {
                val parts = line.split(Regex("""\s+"""))
                if (parts.size >= 4) {
                    val x = parts[1].toFloatOrNull() ?: 0f
                    val y = parts[2].toFloatOrNull() ?: 0f
                    val z = parts[3].toFloatOrNull() ?: 0f
                    rawVerts += MeshVertex(x, y, z)
                }
            } else if (line.startsWith("f ") && edges.size < MAX_RENDER_EDGES) {
                faceCount++
                val idxTokens = line.split(Regex("""\s+""")).drop(1)
                val vIndices = idxTokens.mapNotNull { tok ->
                    val rawIdx = tok.substringBefore('/').toIntOrNull() ?: return@mapNotNull null
                    val resolved = if (rawIdx < 0) rawVerts.size + rawIdx else rawIdx - 1
                    resolved.takeIf { it in rawVerts.indices }
                }
                if (vIndices.size >= 2) {
                    for (i in vIndices.indices) {
                        val a = vIndices[i]
                        val b = vIndices[(i + 1) % vIndices.size]
                        if (a != b) {
                            val lo = minOf(a, b).toLong()
                            val hi = maxOf(a, b).toLong()
                            val key = (lo shl 32) or hi
                            if (edgeSet.add(key) && edges.size < MAX_RENDER_EDGES) {
                                edges += MeshEdge(a, b)
                            }
                        }
                    }
                }
            }
        }
        return normalizeMesh("OBJ", rawVerts, edges, faceCount)
    }

    private fun parseAsciiStl(source: String): ParsedMesh3D {
        val verts = mutableListOf<MeshVertex>()
        val edges = mutableListOf<MeshEdge>()
        val triBuffer = ArrayList<Int>(3)
        var faceCount = 0

        source.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.startsWith("vertex ", ignoreCase = true)) {
                val p = line.split(Regex("""\s+"""))
                if (p.size >= 4) {
                    val v = MeshVertex(
                        p[1].toFloatOrNull() ?: 0f,
                        p[2].toFloatOrNull() ?: 0f,
                        p[3].toFloatOrNull() ?: 0f
                    )
                    val idx = verts.size
                    verts += v
                    triBuffer += idx
                    if (triBuffer.size == 3) {
                        faceCount++
                        if (edges.size < MAX_RENDER_EDGES) {
                            edges += MeshEdge(triBuffer[0], triBuffer[1])
                            edges += MeshEdge(triBuffer[1], triBuffer[2])
                            edges += MeshEdge(triBuffer[2], triBuffer[0])
                        }
                        triBuffer.clear()
                    }
                }
            }
        }
        return normalizeMesh("ASCII STL", verts, edges, faceCount)
    }

    private fun normalizeMesh(label: String, verts: List<MeshVertex>, edges: List<MeshEdge>, faces: Int): ParsedMesh3D {
        if (verts.isEmpty()) {
            return ParsedMesh3D(label, emptyList(), emptyList(), 0, MeshVertex(0f, 0f, 0f), MeshVertex(0f, 0f, 0f), true, "No 3D vertices found in file.")
        }
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        verts.forEach {
            if (it.x < minX) minX = it.x; if (it.x > maxX) maxX = it.x
            if (it.y < minY) minY = it.y; if (it.y > maxY) maxY = it.y
            if (it.z < minZ) minZ = it.z; if (it.z > maxZ) maxZ = it.z
        }
        val cx = (minX + maxX) * 0.5f
        val cy = (minY + maxY) * 0.5f
        val cz = (minZ + maxZ) * 0.5f
        val span = max(max(maxX - minX, maxY - minY), maxZ - minZ).coerceAtLeast(1e-4f)
        val norm = verts.map {
            MeshVertex((it.x - cx) / span, (it.y - cy) / span, (it.z - cz) / span)
        }
        return ParsedMesh3D(
            formatLabel = label,
            vertices = norm,
            edges = edges,
            faceCount = faces,
            minBounds = MeshVertex(minX, minY, minZ),
            maxBounds = MeshVertex(maxX, maxY, maxZ),
            isBinaryOrUnsupported = false
        )
    }

    fun parseGeo(source: String, extension: String): ParsedGeoVector {
        val ext = extension.lowercase().removePrefix(".")
        if (source.isBlank()) {
            return emptyGeo(ext.uppercase(), "No vector geometry found.")
        }
        return when (ext) {
            "geojson", "topojson" -> parseGeoJson(source, ext.uppercase())
            "gpx" -> parseGpx(source)
            "kml" -> parseKml(source)
            else -> emptyGeo(ext.uppercase(), "${ext.uppercase()} spatial binary requires GIS desktop tooling; file headers inspect safely.")
        }
    }

    private fun parseGeoJson(source: String, label: String): ParsedGeoVector = runCatching {
        val root = JSONObject(source)
        val points = mutableListOf<GeoPoint>()
        val lines = mutableListOf<GeoPolyline>()

        fun extractGeom(geom: JSONObject?, name: String) {
            if (geom == null) return
            when (geom.optString("type")) {
                "Point" -> {
                    val c = geom.optJSONArray("coordinates") ?: return
                    if (c.length() >= 2) {
                        points += GeoPoint(c.optDouble(1), c.optDouble(0), if (c.length() > 2) c.optDouble(2) else null, name)
                    }
                }
                "LineString" -> {
                    val c = geom.optJSONArray("coordinates") ?: return
                    val pts = (0 until c.length()).mapNotNull { i ->
                        val p = c.optJSONArray(i) ?: return@mapNotNull null
                        if (p.length() >= 2) GeoPoint(p.optDouble(1), p.optDouble(0), if (p.length() > 2) p.optDouble(2) else null) else null
                    }
                    if (pts.isNotEmpty()) lines += GeoPolyline(name.ifBlank { "Track" }, pts, closed = false)
                }
                "Polygon" -> {
                    val rings = geom.optJSONArray("coordinates") ?: return
                    val outer = rings.optJSONArray(0) ?: return
                    val pts = (0 until outer.length()).mapNotNull { i ->
                        val p = outer.optJSONArray(i) ?: return@mapNotNull null
                        if (p.length() >= 2) GeoPoint(p.optDouble(1), p.optDouble(0)) else null
                    }
                    if (pts.isNotEmpty()) lines += GeoPolyline(name.ifBlank { "Polygon" }, pts, closed = true)
                }
                "MultiLineString", "MultiPolygon" -> {
                    val parts = geom.optJSONArray("coordinates") ?: return
                    val isPoly = geom.optString("type") == "MultiPolygon"
                    for (i in 0 until minOf(parts.length(), 100)) {
                        val sub = if (isPoly) parts.optJSONArray(i)?.optJSONArray(0) else parts.optJSONArray(i)
                        if (sub != null) {
                            val pts = (0 until sub.length()).mapNotNull { j ->
                                val p = sub.optJSONArray(j) ?: return@mapNotNull null
                                if (p.length() >= 2) GeoPoint(p.optDouble(1), p.optDouble(0)) else null
                            }
                            if (pts.isNotEmpty()) lines += GeoPolyline(name.ifBlank { "Shape ${i + 1}" }, pts, closed = isPoly)
                        }
                    }
                }
            }
        }

        if (root.optString("type") == "FeatureCollection") {
            val features = root.optJSONArray("features")
            if (features != null) {
                for (i in 0 until minOf(features.length(), 500)) {
                    val feat = features.optJSONObject(i) ?: continue
                    val props = feat.optJSONObject("properties")
                    val name = props?.optString("name")?.takeIf { it.isNotBlank() }
                        ?: props?.optString("title")?.takeIf { it.isNotBlank() }
                        ?: "Feature ${i + 1}"
                    extractGeom(feat.optJSONObject("geometry"), name)
                }
            }
        } else if (root.optString("type") == "Feature") {
            val name = root.optJSONObject("properties")?.optString("name").orEmpty()
            extractGeom(root.optJSONObject("geometry"), name)
        } else {
            extractGeom(root, label)
        }

        finalizeGeo(label, points, lines)
    }.getOrElse { emptyGeo(label, it.message ?: "Invalid GeoJSON syntax.") }

    private fun parseGpx(source: String): ParsedGeoVector {
        val waypoints = mutableListOf<GeoPoint>()
        val trackPts = mutableListOf<GeoPoint>()
        val wptRegex = Regex("""<wpt\b([^>]*)>(.*?)</wpt>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        wptRegex.findAll(source).forEach { m ->
            val attrs = m.groupValues[1]
            val body = m.groupValues[2]
            val lat = Regex("""lat\s*=\s*["']([^"']+)["']""").find(attrs)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
            val lon = Regex("""lon\s*=\s*["']([^"']+)["']""").find(attrs)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
            val name = Regex("""<name[^>]*>(.*?)</name>""").find(body)?.groupValues?.getOrNull(1)?.trim().orEmpty()
            val ele = Regex("""<ele[^>]*>(.*?)</ele>""").find(body)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
            if (lat != null && lon != null) waypoints += GeoPoint(lat, lon, ele, name.ifBlank { "WPT ${waypoints.size + 1}" })
        }

        val trkPtRegex = Regex("""<trkpt\b([^>]*)>(.*?)</trkpt>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        trkPtRegex.findAll(source).forEach { m ->
            val attrs = m.groupValues[1]
            val body = m.groupValues[2]
            val lat = Regex("""lat\s*=\s*["']([^"']+)["']""").find(attrs)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
            val lon = Regex("""lon\s*=\s*["']([^"']+)["']""").find(attrs)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
            val ele = Regex("""<ele[^>]*>(.*?)</ele>""").find(body)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
            if (lat != null && lon != null) trackPts += GeoPoint(lat, lon, ele)
        }
        val lines = if (trackPts.isNotEmpty()) listOf(GeoPolyline("GPX Track", trackPts)) else emptyList()
        return finalizeGeo("GPX ROUTE", waypoints, lines)
    }

    private fun parseKml(source: String): ParsedGeoVector {
        val waypoints = mutableListOf<GeoPoint>()
        val lines = mutableListOf<GeoPolyline>()
        val placemarkRegex = Regex("""<Placemark\b[^>]*>(.*?)</Placemark>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        placemarkRegex.findAll(source).forEach { pm ->
            val body = pm.groupValues[1]
            val name = Regex("""<name[^>]*>(.*?)</name>""").find(body)?.groupValues?.getOrNull(1)?.trim().orEmpty()
            val coordsText = Regex("""<coordinates[^>]*>(.*?)</coordinates>""", RegexOption.DOT_MATCHES_ALL).find(body)?.groupValues?.getOrNull(1).orEmpty()
            val pts = coordsText.trim().split(Regex("""\s+""")).mapNotNull { tok ->
                val parts = tok.split(',')
                if (parts.size >= 2) {
                    val lon = parts[0].toDoubleOrNull()
                    val lat = parts[1].toDoubleOrNull()
                    val ele = parts.getOrNull(2)?.toDoubleOrNull()
                    if (lat != null && lon != null) GeoPoint(lat, lon, ele, name) else null
                } else null
            }
            if (pts.size == 1) waypoints += pts.first()
            else if (pts.size > 1) lines += GeoPolyline(name.ifBlank { "KML Path ${lines.size + 1}" }, pts, closed = body.contains("<Polygon", ignoreCase = true))
        }
        return finalizeGeo("KML", waypoints, lines)
    }

    private fun finalizeGeo(label: String, waypoints: List<GeoPoint>, lines: List<GeoPolyline>): ParsedGeoVector {
        val allPts = waypoints + lines.flatMap { it.points }
        if (allPts.isEmpty()) return emptyGeo(label, "No coordinates parsed in $label.")
        val minLat = allPts.minOf { it.lat }
        val maxLat = allPts.maxOf { it.lat }
        val minLon = allPts.minOf { it.lon }
        val maxLon = allPts.maxOf { it.lon }
        val eles = allPts.mapNotNull { it.ele }
        var distKm = 0.0
        lines.forEach { poly ->
            for (i in 0 until poly.points.size - 1) {
                distKm += haversineKm(poly.points[i], poly.points[i + 1])
            }
        }
        return ParsedGeoVector(
            formatLabel = label,
            waypoints = waypoints,
            polylines = lines,
            minLat = minLat,
            maxLat = maxLat,
            minLon = minLon,
            maxLon = maxLon,
            totalDistanceKm = distKm,
            minEleMeters = eles.minOrNull(),
            maxEleMeters = eles.maxOrNull()
        )
    }

    private fun haversineKm(a: GeoPoint, b: GeoPoint): Double {
        val r = 6371.0
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val sinLat = sin(dLat / 2)
        val sinLon = sin(dLon / 2)
        val c = sinLat * sinLat + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sinLon * sinLon
        return r * 2.0 * atan2(sqrt(c), sqrt(max(0.0, 1.0 - abs(c))))
    }

    private fun emptyGeo(label: String, notice: String) =
        ParsedGeoVector(label, emptyList(), emptyList(), 0.0, 0.0, 0.0, 0.0, 0.0, null, null, notice)
}
