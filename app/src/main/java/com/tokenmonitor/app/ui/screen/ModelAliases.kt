package com.tokenmonitor.app.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal data class ModelAliasResolver(
    private val explicit: Map<String, String>,
    private val automatic: Map<String, String>
) {
    fun resolve(model: String): String {
        val direct = explicit[modelMatchKey(model)]
        if (direct != null) return direct
        val inferred = automatic[modelMatchKey(model)]
        if (inferred != null) {
            return explicit[modelMatchKey(inferred)] ?: inferred
        }
        return model
    }

    companion object {
        val EMPTY = ModelAliasResolver(emptyMap(), emptyMap())
    }
}

internal object ModelAliasRegistry {
    var resolver by mutableStateOf(ModelAliasResolver.EMPTY)

    fun label(model: String): String = resolver.resolve(model)

    fun groupModels(models: Map<String, Long>?): Map<String, Long> {
        if (models.isNullOrEmpty()) return emptyMap()
        val out = mutableMapOf<String, Long>()
        models.forEach { (model, value) ->
            val key = label(model)
            out[key] = (out[key] ?: 0L) + value
        }
        return out
    }

    fun groupLongs(models: Map<String, Long>?, values: Map<String, Long>?): Map<String, Long> {
        if (models.isNullOrEmpty() || values.isNullOrEmpty()) return emptyMap()
        val out = mutableMapOf<String, Long>()
        models.keys.forEach { model ->
            val key = label(model)
            out[key] = (out[key] ?: 0L) + (values[model] ?: 0L)
        }
        return out
    }

    fun groupCosts(models: Map<String, Long>?, costs: Map<String, Double>?): Map<String, Double> {
        if (models.isNullOrEmpty() || costs.isNullOrEmpty()) return emptyMap()
        val out = mutableMapOf<String, Double>()
        models.keys.forEach { model ->
            val key = label(model)
            out[key] = (out[key] ?: 0.0) + (costs[model] ?: 0.0)
        }
        return out
    }
}

internal fun modelMatchKey(model: String): String = model.trim()
    .lowercase()
    .replace(Regex("[._\\s]+"), "-")
    .replace(Regex("-+"), "-")
    .trim('-')

internal fun modelLeaf(model: String): String =
    model.trim().split('/').filter { it.isNotBlank() }.lastOrNull() ?: model.trim()

internal fun buildModelAliasResolver(
    aliases: Map<String, String>,
    modelIds: Collection<String>,
    grouping: String
): ModelAliasResolver {
    val explicit = mutableMapOf<String, String>()
    aliases.forEach { (source, canonical) ->
        val alias = source.trim()
        val target = canonical.trim()
        if (alias.isNotEmpty() && target.isNotEmpty() && modelMatchKey(alias) != modelMatchKey(target)) {
            explicit[modelMatchKey(alias)] = target
        }
    }

    val mode = grouping.lowercase()
    if (mode != "duplicates" && mode != "prefix") {
        return ModelAliasResolver(explicit, emptyMap())
    }

    val discovered = modelIds.map { it.trim() }
        .filter { it.isNotEmpty() && it.length <= 256 }
        .distinct()
    if (mode == "duplicates" && discovered.size < 2) {
        return ModelAliasResolver(explicit, emptyMap())
    }

    val groups = mutableMapOf<String, MutableList<String>>()
    discovered.forEach { model ->
        val identity = modelMatchKey(modelLeaf(model))
        if (identity.isEmpty()) return@forEach
        groups.getOrPut(identity) { mutableListOf() }.add(model)
    }

    val automatic = mutableMapOf<String, String>()
    groups.forEach { (identity, group) ->
        if (mode == "duplicates" && group.size < 2) return@forEach
        val canonical = group.minByOrNull { candidateSortKey(it, identity) } ?: return@forEach
        val canonicalLeaf = modelLeaf(canonical)
        group.forEach { model ->
            if (model != canonicalLeaf) {
                automatic[modelMatchKey(model)] = canonicalLeaf
            }
        }
    }

    return ModelAliasResolver(explicit, automatic)
}

private fun candidateSortKey(model: String, identity: String): String {
    val leaf = modelLeaf(model)
    val leafKey = modelMatchKey(leaf)
    return listOf(
        if (leafKey == identity) "0" else "1",
        if (model == leaf) "0" else "1",
        if (leaf == leaf.lowercase()) "0" else "1",
        if (leaf.any { it in "._ " }) "1" else "0",
        leaf.length.toString().padStart(4, '0'),
        leaf.lowercase()
    ).joinToString("|")
}
