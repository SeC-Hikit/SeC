package org.sc.manager

import org.sc.common.rest.TrailDto
import org.sc.common.rest.TrailImportDto
import org.sc.common.rest.TrailRawDto
import org.sc.data.mapper.FileDetailsMapper
import org.sc.data.mapper.PlaceRefMapper
import org.sc.data.mapper.TrailCoordinatesMapper
import org.sc.data.mapper.TrailRawMapper
import org.sc.data.model.*
import org.sc.data.repository.TrailDatasetVersionDao
import org.sc.data.repository.TrailRawDAO
import org.sc.processor.TrailsStatsCalculator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*

@Component
class TrailImporterManager @Autowired constructor(
    private val trailsManager: TrailManager,
    private val trailsStatsCalculator: TrailsStatsCalculator,
    private val trailDatasetVersionDao: TrailDatasetVersionDao,
    private val placeMapper: PlaceRefMapper,
    private val trailCoordinatesMapper: TrailCoordinatesMapper,
    private val trailRawMapper: TrailRawMapper,
    private val fileDetailsMapper: FileDetailsMapper,
    private val trailRawDao: TrailRawDAO
) {

    fun saveRaw(trailRaw: TrailRawDto): TrailRawDto =
        trailRawDao.createRawTrail(trailRawMapper.map(trailRaw)).map { trailRawMapper.map(it) }
                .first()

    fun save(importingTrail: TrailImportDto): List<TrailDto> {
        val statsTrailMetadata = StatsTrailMetadata(
            trailsStatsCalculator.calculateTotRise(importingTrail.coordinates),
            trailsStatsCalculator.calculateTotFall(importingTrail.coordinates),
            trailsStatsCalculator.calculateEta(importingTrail.coordinates),
            trailsStatsCalculator.calculateTrailLength(importingTrail.coordinates),
            trailsStatsCalculator.calculateHighestPlace(importingTrail.coordinates),
            trailsStatsCalculator.calculateLowestPlace(importingTrail.coordinates)
        )

        val createdOn = Date()

        val trail = Trail.builder().name(importingTrail.name)
            .startLocation(importingTrail.locations.map { placeMapper.map(it) }.first())
            .endLocation(importingTrail.locations.map { placeMapper.map(it) }.last())
            .description(importingTrail.description)
            .officialEta(importingTrail.officialEta)
            .code(importingTrail.code)
            .variant(importingTrail.isVariant)
            .locations(getConsistentLocations(importingTrail))
            .classification(importingTrail.classification)
            .country(importingTrail.country)
            .statsTrailMetadata(statsTrailMetadata)
            .coordinates(importingTrail.coordinates.map { trailCoordinatesMapper.map(it) })
            .createdOn(createdOn)
            .lastUpdate(createdOn)
            .maintainingSection(importingTrail.maintainingSection)
            .territorialDivision(importingTrail.territorialDivision)
            .geoLineString(GeoLineString(importingTrail.coordinates.map {
                Coordinates2D(
                    it.longitude,
                    it.latitude
                )
            }))
            .cycloDetails(
                CycloDetails(
                    CycloClassification.UNCLASSIFIED, 0,
                    CycloFeasibility(true, 0),
                    CycloFeasibility(true, 0), ""
                )
            )
            .mediaList(emptyList())
            .fileDetails(fileDetailsMapper.map(importingTrail.fileDetailsDto))
            .status(importingTrail.trailStatus)
            .build()

        val savedTrailDao = trailsManager.save(trail)

        trailDatasetVersionDao.increaseVersion()

        return savedTrailDao
    }

    fun countTrailRaw() = trailRawDao.count()

    private fun getConsistentLocations(importingTrail: TrailImportDto) =
        sortLocationsByTrailCoordinates(importingTrail.locations.map { placeMapper.map(it) })

    private fun sortLocationsByTrailCoordinates(locations: List<PlaceRef>): List<PlaceRef> =
        locations.sortedBy { it.trailCoordinates.distanceFromTrailStart }
}