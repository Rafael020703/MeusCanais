package com.meuscanais.core.data.mapper

import com.meuscanais.data.model.XtreamStream
import com.meuscanais.data.model.XtreamVod
import com.meuscanais.data.model.XtreamSeries
import com.meuscanais.data.local.entities.FavoriteEntity
import com.meuscanais.data.local.entities.IptvStreamEntity
import com.meuscanais.domain.model.ContentType
import com.meuscanais.domain.model.IptvItem

fun XtreamStream.toIptvItem() = IptvItem(
    id = streamId.toString(),
    name = name ?: "",
    icon = streamIcon,
    type = ContentType.LIVE,
    epgId = epgChannelId,
    categoryId = categoryId,
    added = added
)

fun IptvStreamEntity.toIptvItem() = IptvItem(
    id = id.toString(),
    name = name,
    icon = logo,
    type = ContentType.fromString(streamType),
    containerExtension = containerExtension,
    rating = rating,
    releaseDate = releaseDate,
    categoryId = categoryId,
    backdropUrl = backdrop,
    plot = plot,
    genre = genre,
    duration = duration,
    cast = cast,
    director = director,
    added = added
)

fun XtreamVod.toIptvItem() = IptvItem(
    id = streamId.toString(),
    name = name ?: "",
    icon = streamIcon,
    type = ContentType.MOVIE,
    containerExtension = container_extension,
    rating = rating,
    added = added,
    categoryId = categoryId
)

fun XtreamSeries.toIptvItem() = IptvItem(
    id = seriesId.toString(),
    name = name ?: "",
    icon = cover,
    type = ContentType.SERIES,
    rating = rating,
    releaseDate = releaseDate,
    added = lastModified,
    categoryId = categoryId,
    plot = plot,
    genre = genre,
    cast = cast,
    director = director,
    duration = episodeRunTime
)

fun FavoriteEntity.favoriteToIptvItem() = IptvItem(
    id = streamId.toString(),
    name = name,
    icon = logo,
    type = ContentType.fromString(streamType),
    epgId = url,
    containerExtension = containerExtension,
    categoryId = categoryId,
    rating = rating,
    releaseDate = releaseDate
)
